# Scheduler API

`scheduler-api` is the Spring Boot HTTP entry point for the task scheduler. It depends on `scheduler-core` so that HTTP requests can eventually create tasks and submit executions to the same engine used by the CLI.

## Package Structure

```text
com.nayan.scheduler
|-- SchedulerApiApplication.java  # Spring Boot entry point
|-- controler/                    # HTTP controller
`-- dto/                          # API request and response models
```

The package name `controler` reflects the current source layout.

## Current Endpoints

| Method | Path | Current behavior |
| --- | --- | --- |
| `GET` | `/health` | Returns a simple API status message |
| `POST` | `/tasks` | Accepts a task request and currently returns a placeholder response |

`POST /tasks` does not yet persist or schedule the submitted task.

## Request Model

`CreateTaskRequest` groups three parts of a task submission:

- `type` selects `PRINT`, `WRITE`, or `DELETE`.
- `taskName` identifies the task.
- `schedule` contains the requested start time.
- `payload` contains the optional message and file path used by concrete task types.

`CreateTaskResponse` currently contains a task ID and status string.

## Core Integration

`SchedulerController` declares a dependency on the core `Scheduler`. To make the API independently runnable, the Spring application still needs a composition configuration that provides:

1. Implementations of `TaskStore`, `TaskScheduleStore`, and `TaskExecutionStore`.
2. An `Executor` configured with those stores and a worker count.
3. A `Scheduler` configured with the executor and stores.
4. A background scheduling loop equivalent to the CLI's `SchedulerProcess`.
5. Controller logic that converts DTOs into core tasks, schedules, and executions.

Those pieces are intentionally not documented as existing behavior because they are not implemented in the current module.

## Build

Compile the API and its core dependency from the repository root:

```bash
mvn -pl scheduler-api -am package
```

The module compiles, but starting the Spring context requires a `Scheduler` bean. Once the composition configuration is added, the Spring Boot Maven plugin can run the application.