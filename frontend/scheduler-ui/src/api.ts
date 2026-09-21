import type { Execution } from "./utils/executions";
import type { CreateTaskRequest } from "./utils/createTask";

export type TaskAction = "pause" | "resume" | "cancel";

export interface Task {
  id: string;
  name: string;
  type: string;
  status: string;
  executions: Execution[] | null;
  executionError: string | null;
}

interface TaskResponseItem {
  taskId: string;
  taskName: string;
  taskType: string;
  taskStatus: string;
}

interface ExecutionResponseItem {
  taskExecutionId: string;
  executionTime: string;
  executionStatus: string;
}

const apiUrl = "http://localhost:8080";

async function request(path: string, method = "GET", body?: CreateTaskRequest) {
  const response = await fetch(`${apiUrl}${path}`, {
    method,
    ...(body === undefined ? {} : {
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
  });
  if (!response.ok) {
    throw new Error(`Request failed (${response.status} ${response.statusText}).`);
  }
  return response;
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "An unexpected error occurred.";
}

export async function createTask(task: CreateTaskRequest): Promise<string> {
  const response = await request("/tasks", "POST", task);
  const data: unknown = await response.json();
  if (typeof data !== "object" || data === null || !("taskId" in data) ||
      typeof data.taskId !== "string" || !data.taskId.trim()) {
    throw new Error("The server did not confirm task creation. Refresh the task list before retrying.");
  }
  return data.taskId;
}

export async function loadTasks(): Promise<Task[]> {
  const response = await request("/tasks");
  const data: { tasks: TaskResponseItem[] } = await response.json();
  if (!Array.isArray(data.tasks)) {
    throw new Error("The API returned an invalid task list.");
  }

  return Promise.all(data.tasks.map(async (task) => {
    let executions: Execution[] | null = null;
    let executionError: string | null = null;
    try {
      const response = await request(`/tasks/${task.taskId}/executions`);
      const data: { executions: ExecutionResponseItem[] } = await response.json();
      if (!Array.isArray(data.executions) ||
          data.executions.some((execution) =>
            typeof execution.executionTime !== "string" ||
            !Number.isFinite(Date.parse(execution.executionTime)))) {
        throw new Error("The API returned invalid execution data.");
      }
      executions = data.executions.map((execution) => ({
        id: execution.taskExecutionId,
        time: execution.executionTime,
        status: execution.executionStatus,
      }));
    } catch (error) {
      executionError = `Could not load executions. ${errorMessage(error)}`;
    }
    return {
      id: task.taskId,
      name: task.taskName,
      type: task.taskType,
      status: task.taskStatus,
      executions,
      executionError,
    };
  }));
}

export async function performTaskAction(taskId: string, action: TaskAction) {
  const response = await request(`/tasks/${taskId}/${action}`, "POST");
  const accepted: unknown = await response.json();
  if (accepted !== true) {
    throw new Error(`The server did not accept the ${action} action. Refresh and try again.`);
  }
}
