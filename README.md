# Task Scheduler

A learning project that implements a multi-threaded task scheduler in Java. It supports one-time and recurring tasks, a manually managed worker pool, pluggable persistence contracts, an interactive CLI, and an early Spring Boot API.

## Modules

| Module | Responsibility |
| --- | --- |
| `scheduler-core` | Domain models, scheduling logic, worker execution, and persistence interfaces |
| `scheduler-cli` | Interactive terminal client and in-memory implementations of the persistence interfaces |
| `scheduler-api` | Spring Boot HTTP entry point and request/response DTOs |

Each module has its own README with its internal structure and behavior.

## Architecture

```mermaid
flowchart LR
    CLI[scheduler-cli] --> Core[scheduler-core]
    API[scheduler-api] --> Core
    CLI --> Memory[(In-memory stores)]
    Core --> Ports[Store interfaces]
    Memory -. implements .-> Ports
```

The core module does not choose a database or storage technology. Applications create implementations of `TaskStore`, `TaskScheduleStore`, and `TaskExecutionStore`, then inject them into the scheduler and executor.

## Execution Flow

1. A client creates a `Task` and its `TaskSchedule`.
2. A `TaskExecution` is added to the scheduler's time-ordered queue.
3. The scheduler waits until the earliest execution is due.
4. Due executions are passed to the executor queue.
5. A worker loads the task from `TaskStore`, runs it, and records the result through `TaskExecutionStore`.
6. For a recurring schedule, the scheduler creates the next execution using the configured interval.

## Project Structure

```text
task-scheduler/
|-- scheduler-core/   # Scheduling engine and storage contracts
|-- scheduler-cli/    # CLI application and in-memory stores
|-- scheduler-api/    # Spring Boot API scaffold
`-- pom.xml            # Parent Maven reactor
```

## Build

Requirements:

- JDK 21
- Maven 3.9 or newer

Build and test all modules from the repository root:

```bash
mvn clean test
```

The project currently has no automated test sources, so this command validates dependency resolution and compilation for all modules.

## Current Status

The CLI is the complete runnable composition of the scheduler. The API currently contains the Spring Boot entry point, health endpoint, task DTOs, and a placeholder task endpoint. It still needs store implementations and Spring configuration for the core services before it can run the scheduling flow independently.
