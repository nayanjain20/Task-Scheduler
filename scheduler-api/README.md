# Scheduler API

`scheduler-api` is the Spring Boot HTTP entry point for the task scheduler. It creates tasks and reads scheduler state through the same core service used by the CLI.

## Package Structure

```text
com.nayan.scheduler
|-- SchedulerApiApplication.java  # Spring Boot entry point
|-- config/                       # Store and engine composition
|-- controller/                   # HTTP controller
|-- dto/                          # API request and response models
`-- service/                      # DTO-to-core mapping
```

## Current Endpoints

| Method | Path                         | Current behavior                              |
| ------ | ---------------------------- | --------------------------------------------- |
| `GET`  | `/health`                    | Returns a simple API status message           |
| `POST` | `/tasks`                     | Creates and schedules a task                  |
| `GET`  | `/tasks`                     | Returns all tasks                             |
| `GET`  | `/tasks/{taskId}/executions` | Returns execution history for the given task  |

## Request Model

`CreateTaskRequest` groups three parts of a task submission:

- `type` selects `PRINT`, `WRITE`, or `DELETE`.
- `taskName` identifies the task.
- `schedule` contains the start time, recurrence flag, and interval in seconds.
- `payload` contains the optional message and file path used by concrete task types.

`CreateTaskResponse` currently contains a task ID and status string.

## Core Integration

`StoreConfiguration` creates one shared set of in-memory stores. `EngineConfiguration` creates and starts `TaskSchedulerService` with those stores. `SchedulerApiService` is injected with that shared service and maps API DTOs into core `Task` and `TaskSchedule` objects.

Because persistence is in memory, API state is lost when the process stops. A database-backed implementation can replace the store beans without changing the controller or scheduling engine.

## Build

Compile the API and its core dependency from the repository root:

```bash
mvn -pl scheduler-api -am package
```

The VS Code launch configuration named `SchedulerApiApplication` starts the API from the editor.
