import { useEffect, useState } from "react";
import type { Task } from "./TaskList";
import ExecutionList from "./ExecutionList";

export interface Execution {
  id: string;
  time: string;
  status: string;
}

interface ExecutionResponse {
  taskExecutionId: string;
  executionTime: string;
  executionStatus: string;
}

interface TaskCardProps {
  task: Task;
}

function TaskCard({ task }: TaskCardProps) {
  const [showExecutions, setShowExecutions] = useState(false);
  const [executions, setExecutions] = useState<Execution[]>([]);

  useEffect(() => {
    if (!showExecutions) {
      return;
    }

    fetch(`http://localhost:8080/tasks/${task.id}/executions`)
      .then((response) => response.json())
      .then((data: { executions?: ExecutionResponse[] }) => {
        setExecutions(
          data.executions?.map((execution) => ({
            id: execution.taskExecutionId,
            time: execution.executionTime,
            status: execution.executionStatus,
          })) ?? [],
        );
      })
      .catch((error) => console.error("Failed to load executions", error));
  }, [showExecutions, task.id]);

  return (
    <div className="task-card">
      <div className="task-info">
        <div>{task.name}</div>
        <div>{task.status}</div>
      </div>
      <div className="task-type">
        <div>Task type: {task.type}</div>
      </div>
      <div className="task-button">
        <button onClick={() => setShowExecutions((visible) => !visible)}>
          {showExecutions ? "Hide executions" : "Show executions"}
        </button>
      </div>
      {showExecutions && <ExecutionList executions={executions} />}
    </div>
  );
}

export default TaskCard;
