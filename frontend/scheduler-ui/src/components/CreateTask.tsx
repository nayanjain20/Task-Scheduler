import { useId, useRef, useState, type FormEvent } from "react";
import { createTask, errorMessage } from "../api";
import { buildCreateTaskRequest, type TaskDraft } from "../utils/createTask";

export interface CreateTaskProps {
  refreshTasks: () => void;
}

const emptyDraft: TaskDraft = {
  name: "",
  type: "PRINT",
  recurring: false,
  interval: "60",
  message: "",
  path: "",
};

function CreateTask({ refreshTasks }: CreateTaskProps) {
  const [draft, setDraft] = useState<TaskDraft>(emptyDraft);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const inFlight = useRef(false);
  const id = useId();
  const typeDescription = {
    PRINT: "Prints the task name to the backend console. No custom message is needed.",
    WRITE: "Writes a message to a file on the backend machine.",
    DELETE: "Deletes a file on the backend machine. Use a test file you can safely remove.",
  }[draft.type];

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (inFlight.current) return;
    setError(null);
    setSuccess(null);
    let request;
    try {
      request = buildCreateTaskRequest(draft);
    } catch (error) {
      setError(errorMessage(error));
      return;
    }

    inFlight.current = true;
    setSubmitting(true);
    try {
      await createTask(request);
      setSuccess(`Created "${request.taskName}". It will appear in your tasks below.`);
      setDraft(emptyDraft);
      refreshTasks();
    } catch (error) {
      setError(`Could not create task. ${errorMessage(error)}`);
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  function resetForm() {
    setDraft(emptyDraft);
    setError(null);
    setSuccess(null);
  }

  return (
    <section className="create-task-panel" aria-labelledby={`${id}-heading`}>
      <header className="create-task-header">
        <h2 id={`${id}-heading`}>Create a task</h2>
        <p>Choose an action and how often to run it.</p>
      </header>
      <form className="create-task-form" onSubmit={handleSubmit} aria-busy={submitting}>
        <fieldset className="create-task-fields" disabled={submitting}>
          <legend className="visually-hidden">Task details</legend>
          <div className="create-task-grid">
            <div className="form-field">
              <label htmlFor={`${id}-name`}>Task name <span className="required-label">(required)</span></label>
              <input id={`${id}-name`} name="taskName" required placeholder="e.g. Print a heartbeat"
                value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} />
            </div>
            <div className="form-field">
              <label htmlFor={`${id}-type`}>Task action</label>
              <select id={`${id}-type`} name="type" value={draft.type} aria-describedby={`${id}-type-help`}
                onChange={(event) => {
                  const type = event.target.value;
                  if (type === "PRINT" || type === "WRITE" || type === "DELETE") {
                    setDraft({ ...draft, type });
                  }
                }}>
                <option value="PRINT">Print to console</option>
                <option value="WRITE">Write to file</option>
                <option value="DELETE">Delete file</option>
              </select>
              <p id={`${id}-type-help`} className={draft.type === "DELETE" ? "delete-warning" : "field-hint"}>{typeDescription}</p>
            </div>
          </div>

          <fieldset className="schedule-fields">
            <legend>Schedule</legend>
            <div className="schedule-options">
              <label className={`schedule-option ${!draft.recurring ? "selected" : ""}`}>
                <input type="radio" name={`${id}-schedule`} value="once" checked={!draft.recurring}
                  onChange={() => setDraft({ ...draft, recurring: false })} />
                <span><strong>Run once</strong><span className="field-hint">Queue a single execution</span></span>
              </label>
              <label className={`schedule-option ${draft.recurring ? "selected" : ""}`}>
                <input type="radio" name={`${id}-schedule`} value="recurring" checked={draft.recurring}
                  onChange={() => setDraft({ ...draft, recurring: true })} />
                <span><strong>Recurring</strong><span className="field-hint">Repeat at a regular interval</span></span>
              </label>
            </div>
            {draft.recurring && (
              <div className="form-field interval-field">
                <label htmlFor={`${id}-interval`}>Repeat every (seconds)</label>
                <input id={`${id}-interval`} name="interval" type="number" min="1" max="2147483647" step="1" required
                  aria-describedby={`${id}-interval-help`} value={draft.interval}
                  onChange={(event) => setDraft({ ...draft, interval: event.target.value })} />
                <p id={`${id}-interval-help`} className="field-hint">Use a positive whole number, for example 60 for every minute.</p>
              </div>
            )}
          </fieldset>

          {draft.type !== "PRINT" && (
            <div className="form-field">
              <label htmlFor={`${id}-path`}>File path <span className="required-label">(required)</span></label>
              <input id={`${id}-path`} name="filePath" type="text" required placeholder="e.g. temp\report.txt"
                aria-describedby={`${id}-path-help`} value={draft.path}
                onChange={(event) => setDraft({ ...draft, path: event.target.value })} />
              <p id={`${id}-path-help`} className="field-hint">Relative paths are resolved from the backend's working directory, not your browser.</p>
            </div>
          )}
          {draft.type === "WRITE" && (
            <div className="form-field">
              <label htmlFor={`${id}-message`}>Message <span className="required-label">(optional)</span></label>
              <textarea id={`${id}-message`} name="message" rows={4} placeholder="Text to write to the file"
                value={draft.message} onChange={(event) => setDraft({ ...draft, message: event.target.value })} />
            </div>
          )}
          <div className="create-task-footer">
            <button type="button" onClick={resetForm}>Clear form</button>
            <button type="submit" className="primary-button">{submitting ? "Creating task..." : "Create task"}</button>
          </div>
        </fieldset>
        {error && <p className="error-message" role="alert">{error}</p>}
        {success && <p className="success-message" role="status">{success}</p>}
      </form>
    </section>
  );
}

export default CreateTask;
