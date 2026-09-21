import { useId, useRef, useState } from "react";
import { errorMessage, performTaskAction, type Task, type TaskAction } from "../api";
import { formatIst, summarizeExecutions } from "../utils/executions";
import ExecutionList from "./ExecutionList";

interface TaskCardProps {
  task: Task;
  onActionComplete: (taskId: string, action: TaskAction) => void;
}

function TaskCard({ task, onActionComplete }: TaskCardProps) {
  const [showExecutions, setShowExecutions] = useState(false);
  const [pendingAction, setPendingAction] = useState<TaskAction | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const actionInFlight = useRef(false);
  const historyId = useId();
  const headingId = useId();
  const summary = task.executions === null ? null : summarizeExecutions(task.executions);

  async function handleAction(action: TaskAction) {
    if (actionInFlight.current) return;
    if (action === "cancel" &&
        !window.confirm(`Cancel "${task.name}"? Pending executions will be discarded. This cannot be resumed.`)) {
      return;
    }
    actionInFlight.current = true;
    setPendingAction(action);
    setActionError(null);
    setActionMessage(null);
    try {
      await performTaskAction(task.id, action);
      onActionComplete(task.id, action);
      setActionMessage({ pause: "Task paused.", resume: "Task resumed.", cancel: "Task cancelled." }[action]);
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      actionInFlight.current = false;
      setPendingAction(null);
    }
  }

  return (
    <article className="task-card" aria-labelledby={headingId}>
      <div className="task-info">
        <h3 id={headingId}>{task.name}</h3>
        <span className={`task-status status-${task.status.toLowerCase()}`}>{task.status}</span>
      </div>
      <p>Task type: {task.type}</p>
      <dl className="execution-summary">
        <div>
          <dt>Execution count</dt>
          <dd>{summary === null ? "Unavailable" : summary.count}</dd>
        </div>
        <div>
          <dt>Last finished run - scheduled time (IST)</dt>
          <dd>{summary === null ? "Unavailable" : summary.lastRun ? formatIst(summary.lastRun.time) : "Not run yet"}</dd>
        </div>
      </dl>
      <p className="summary-note">
        Counts completed and failed runs only. Times are scheduled times, not actual start or finish times.
      </p>
      {task.executionError && <p className="error-message" role="alert">{task.executionError}</p>}
      <div className="task-actions" aria-label={`Actions for ${task.name}`}>
        <button disabled={pendingAction !== null || task.status !== "ACTIVE"} onClick={() => void handleAction("pause")}>
          {pendingAction === "pause" ? "Pausing..." : "Pause"}
        </button>
        <button disabled={pendingAction !== null || task.status !== "PAUSE"} onClick={() => void handleAction("resume")}>
          {pendingAction === "resume" ? "Resuming..." : "Resume"}
        </button>
        <button className="cancel-button" disabled={pendingAction !== null || !["ACTIVE", "PAUSE"].includes(task.status)} onClick={() => void handleAction("cancel")}>
          {pendingAction === "cancel" ? "Cancelling..." : "Cancel"}
        </button>
      </div>
      <p className="summary-note">Pause and cancel do not interrupt a run already in progress.</p>
      {actionError && <p className="error-message" role="alert">{actionError}</p>}
      {actionMessage && <p role="status">{actionMessage}</p>}
      <button className="history-button" aria-expanded={showExecutions} aria-controls={historyId}
        onClick={() => setShowExecutions((visible) => !visible)}>
        {showExecutions ? "Hide executions" : "Show executions"}
      </button>
      <div id={historyId} hidden={!showExecutions}>
        {showExecutions && task.executions !== null && <ExecutionList executions={task.executions} />}
      </div>
    </article>
  );
}

export default TaskCard;
