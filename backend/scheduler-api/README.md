# Scheduler API

`scheduler-api` is the Spring Boot HTTP entry point for the task scheduler. It creates tasks and reads scheduler state through the same core service used by the CLI.

## Package Structure

```text
com.nayan.scheduler
|-- SchedulerApiApplication.java  # Spring Boot entry point
|-- config/                       # Store and engine composition
|-- controller/                   # HTTP controller
|-- dto/                          # API request and response models
|-- persistence/                  # JPA entities, repositories, mappers, and stores
`-- service/                      # DTO-to-core mapping
```

## Current Endpoints

| Method | Path                         | Current behavior                             |
| ------ | ---------------------------- | -------------------------------------------- |
| `GET`  | `/health`                    | Returns a simple API status message          |
| `POST` | `/tasks`                     | Creates and schedules a task                 |
| `GET`  | `/tasks`                     | Returns all tasks                            |
| `GET`  | `/tasks/{taskId}/executions` | Returns execution history for the given task |
| `POST` | `/tasks/{taskId}/pause`      | Pauses a task                                |
| `POST` | `/tasks/{taskId}/resume`     | Resumes a paused task                        |
| `POST` | `/tasks/{taskId}/cancel`     | Cancels a task                               |

Task and execution list responses include both the result list and its `count`. State-action endpoints return `true` when the requested transition is accepted and `false` otherwise.

## Request Model

`CreateTaskRequest` groups three parts of a task submission:

- `type` selects `PRINT`, `WRITE`, or `DELETE`.
- `taskName` identifies the task.
- `schedule` contains the start time, recurrence flag, and interval in seconds.
- `payload` contains the optional message and file path used by concrete task types.

`CreateTaskResponse` currently contains a task ID and status string.

## Task State Operations

- **Pause** changes the task status to `PAUSE` and marks PENDING and IN_QUEUE execution records as discarded.
- **Resume** accepts only paused tasks, changes the status back to `ACTIVE`, and schedules a new execution.
- **Cancel** changes the task status to `CANCEL` and marks PENDING and IN_QUEUE execution records as discarded.

## Core Integration

The three JPA store adapters implement the core persistence contracts and are discovered as Spring repositories. `EngineConfiguration` injects those stores into `TaskSchedulerService`, configures 10 worker threads, and starts the scheduler. `SchedulerApiService` maps API DTOs into core `Task` and `TaskSchedule` objects.

The scheduler polls `claimDueExecutions()` every second. The JPA store uses one
native H2 `SELECT ... FROM FINAL TABLE (UPDATE ...)` statement to claim up to 100
PENDING executions scheduled strictly before `Instant.now()`. A subquery chooses
the earliest eligible IDs; the UPDATE changes them to IN_QUEUE, clears worker ID
to -1, and refreshes the timestamp. FINAL TABLE returns only rows changed by that
statement, ordered by execution time then execution ID. This is not a separate
read of all IN_QUEUE rows, and there is no per-execution UPDATE loop.
The scheduler only forwards that committed batch to the executor. The worker then
claims IN_QUEUE as ASSIGNED. No scheduler-owned priority queue is maintained.
The in-memory implementation owns its own priority queue behind the same contract.
The store flushes pending managed changes and clears the persistence context
before the native statement, so its returned entities cannot be stale cached
copies. They are mapped directly into core models without another query or
manually patching their fields. The native query returns rows, so it intentionally
does not use `@Modifying`; the store supplies the transaction.

This claim SQL targets the configured H2 database. Switching databases requires
an equivalent atomic update-and-return query for that database's dialect.
The in-memory store applies the same 100-execution limit under its lock. Remaining
PENDING work waits for subsequent polls. No scheduler ID or separate queue-filling
poller is used.

`TaskMapper`, `TaskScheduleMapper`, and `TaskExecutionMapper` keep JPA annotations out of the core domain. Task subtype fields such as write messages and file paths are stored in `TaskEntity` and restored through `TaskFactory`.

## Database

The default datasource is file-backed H2 at `jdbc:h2:file:./data/schedulerdb`,
relative to the process working directory. Data survives API restarts. Hibernate
updates the schema at startup, and the H2 console is available at `/h2-console`.
Tests use a separate in-memory H2 database.

### Existing H2 database: add IN_QUEUE before upgrading

Hibernate `ddl-auto=update` does **not** expand an existing H2 native ENUM when a
Java enum gains a value. Old databases may reject IN_QUEUE, including queries
that compare against it. A regression test verifies this with an isolated
file-backed database.

For a legacy native ENUM `task_executions.execution_status` column:

1. Stop the API and any other process using the database.
2. Back up the database files. Do not delete them or switch to `create-drop`.
3. Open that same file database in a compatible standalone H2 client and run:

   ```sql
   ALTER TABLE task_executions ALTER COLUMN execution_status
   ENUM ('ASSIGNED', 'COMPLETED', 'DISCARDED', 'FAILED', 'IN_QUEUE', 'PENDING', 'SKIPPED');
   ```

4. Close the client and restart the API from its usual working directory.

This one-time change preserves existing execution rows and statuses. Fresh
databases already include IN_QUEUE and need no manual change. For VARCHAR columns
with CHECK constraints, inspect and update the actual constraint instead of
applying the native ENUM statement. No automatic migration of existing user data
is performed by this refactor.

## Worker Assignment and Execution Timestamps

The execution model supports `ASSIGNED` status and an `updatedAt` timestamp.
Both stores set a UTC `updatedAt` timestamp on creation and refresh it on every
store update, including claims, worker assignments, completion, and discards.

`assignTaskExecutionToWorker(taskExecutionId, workerId)` assigns a specific IN_QUEUE
execution, setting its worker ID, status to ASSIGNED, and `updatedAt` using
`Instant.now()`. The execution ID is a UUID and worker IDs are nonnegative integers.
Invalid arguments throw `IllegalArgumentException`; missing or non-IN_QUEUE
executions throw `IllegalStateException`. JPA uses a conditional update inside a
transaction so competing assignment calls cannot both claim the same IN_QUEUE row.
Call this before executing work; the method itself does not enqueue or execute it.

Historical records have no update timestamp until modified through the store.
Existing databases must support the nullable `updated_at` column
and the `ASSIGNED` and `IN_QUEUE` statuses (including any status constraint).

## Deferred Recovery

Automatic recovery is not included in the current scheduler/persistence changes.
Startup begins polling PENDING executions without resetting existing IN_QUEUE or
ASSIGNED rows. There is no periodic stale-worker recovery job.

Database claiming and enqueueing into the in-process executor are not one
transaction. If dispatch fails after a claim, the error is logged and the rest of
the batch is attempted, but the undelivered IN_QUEUE row is not retried
automatically, even on restart. Work interrupted while ASSIGNED is likewise not
automatically requeued. File-backed persistence retains these rows; it does not
itself recover interrupted work. A safe recovery policy remains future work.

## Tests

Store contract and concurrency tests verify strict due-time boundaries, ordering,
no duplicate claims, transaction rollback, and cancellation racing
worker assignment. Core tests cover memory-queue consistency, snapshot isolation,
one-second polling, startup without resets, worker claim rejection, and recurring
dispatch behavior.

`SchedulerApiJpaE2ETest` starts the Spring application with a test H2 database and
sends requests through MockMvc. It verifies create, list, pause, resume, and cancel
flows, including WRITE and DELETE payload persistence. A short PRINT task also
passes through the real poller and worker to a persisted COMPLETED execution with
a worker ID. `JpaTaskExecutionSchemaUpdateTest` verifies the one-time legacy H2
ENUM migration without opening the application's database.

Manual HTTP testing can use the same flows against the running API. Recurring
tasks create a new execution at dispatch until they are paused or cancelled;
cancelling marks PENDING and IN_QUEUE records as discarded.

## Build

Compile the API and its core dependency from the `backend` directory:

```bash
mvn -pl scheduler-api -am package
```

Run the API persistence tests with:

```bash
mvn -pl scheduler-api -am test
```

### Run in VS Code

Open the repository root, let VS Code finish importing the Maven projects, then
select **SchedulerApiApplication** in **Run and Debug** and press **F5**.
The [launch configuration](../../.vscode/launch.json) selects the `scheduler-api`
Maven project so the Java debugger resolves Spring and the core module on its
classpath. Its working directory is the repository root, preserving the default
database location at `data/schedulerdb`.

Do not add another API launch entry with an empty `projectName`. A launch using
only `jdt.ls-java-project/bin` is using the generic Java project rather than the
Maven module and can fail with `SpringApplication cannot be resolved`.
The workspace stops launches on build failures instead of running broken classes.

If `scheduler-api` is missing from the **Java Projects** view, run **Java: Import
Java Projects in Workspace** from the Command Palette. If the imported project
still has stale dependency errors, run **Java: Clean Java Language Server
Workspace**, allow the reload, and wait for Maven import to finish before retrying.

If Maven builds successfully but Java project import fails, check the Java
language server log. Missing or locked JARs under the Red Hat Java extension's
`globalStorage` directory indicate an extension runtime/cache problem, not a
missing application dependency. Close other VS Code windows and restart VS Code,
then retry the import. If the same extension-runtime errors persist, reinstall
**Language Support for Java by Red Hat**. Do not delete shared extension caches
while other VS Code windows are using them.
