export type TaskType = "PRINT" | "WRITE" | "DELETE";

export interface TaskDraft {
  name: string;
  type: TaskType;
  recurring: boolean;
  interval: string;
  message: string;
  path: string;
}

export interface CreateTaskRequest {
  type: TaskType;
  taskName: string;
  schedule: { interval: number; recurring: boolean };
  payload: { message?: string; filePath?: string };
}

export function buildCreateTaskRequest(draft: TaskDraft): CreateTaskRequest {
  const taskName = draft.name.trim();
  if (!taskName) {
    throw new Error("Enter a task name.");
  }
  const interval = draft.recurring ? Number(draft.interval) : 0;
  if (draft.recurring &&
      (!Number.isInteger(interval) || interval < 1 || interval > 2147483647)) {
    throw new Error("Enter a whole-number interval between 1 and 2,147,483,647 seconds.");
  }
  const filePath = draft.path.trim();
  if (draft.type !== "PRINT" && !filePath) {
    throw new Error("Enter a file path for this task.");
  }
  return {
    type: draft.type,
    taskName,
    schedule: { interval, recurring: draft.recurring },
    payload: draft.type === "PRINT" ? {} : draft.type === "WRITE"
      ? { filePath, message: draft.message }
      : { filePath },
  };
}
