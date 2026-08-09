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

- **Pause** changes the task status to `PAUSE` and marks pending execution records as discarded.
- **Resume** accepts only paused tasks, changes the status back to `ACTIVE`, and schedules a new execution.
- **Cancel** changes the task status to `CANCEL` and marks pending execution records as discarded.

## Core Integration

The three JPA store adapters implement the core persistence contracts and are discovered as Spring repositories. `EngineConfiguration` injects those stores into `TaskSchedulerService`, configures 10 worker threads, and starts the scheduler. `SchedulerApiService` maps API DTOs into core `Task` and `TaskSchedule` objects.

`TaskMapper`, `TaskScheduleMapper`, and `TaskExecutionMapper` keep JPA annotations out of the core domain. Task subtype fields such as write messages and file paths are stored in `TaskEntity` and restored through `TaskFactory`.

## Database

The default datasource is an in-memory H2 database named `schedulerdb`. Hibernate updates the schema at startup, and the H2 console is available at `/h2-console`. Data is lost when the API process stops; change the datasource properties to use a file-backed H2 database or another JPA-supported database for durable storage.

## Tests

`SchedulerApiJpaE2ETest` starts the Spring application with a test H2 database and sends requests through MockMvc. It verifies task, schedule, and execution rows for create, list, pause, resume, and cancel flows, including WRITE and DELETE payload persistence.

Manual HTTP testing can use the same flows against the running API. Recurring tasks create a new execution after each interval until they are paused or cancelled; cancelling marks pending execution records as discarded.

## Build

Compile the API and its core dependency from the repository root:

```bash
mvn -pl scheduler-api -am package
```

Run the API persistence tests with:

```bash
mvn -pl scheduler-api -am test
```

The VS Code launch configuration named `SchedulerApiApplication` starts the API from the editor.
