import { useState, type FormEvent } from "react";

type TaskType = "PRINT" | "WRITE" | "DELETE";

export interface CreateTaskProps {
  refreshTasks: () => void;
}

function CreateTask({ refreshTasks }: CreateTaskProps) {
  const [name, setName] = useState<string>("");
  const [type, setType] = useState<TaskType>("PRINT");
  const [recurring, setRecurring] = useState<boolean>(false);
  const [interval, setInterval] = useState<number | null>(null);
  const [message, setMessage] = useState<string>("");
  const [path, setPath] = useState<string>("");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const request = {
      type,
      taskName: name,
      schedule: {
        interval: recurring ? interval : null,
        recurring,
      },
      payload: {
        message,
        filePath: path,
      },
    };

    fetch("http://localhost:8080/tasks", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    })
      .then((response) => response.json())
      .then(() => {
        refreshTasks();
      })
      .catch((error) => {
        console.error("Failed to create task", error);
      });
  }

  return (
    <>
      <form className="create-task-form" onSubmit={handleSubmit}>
        <div className="row">
          <div>Task name</div>
          <input
            type="text"
            onChange={(e) => setName(e.target.value)}
            value={name}
          />
        </div>
        <div className="row">
          <div>Task type</div>
          <select
            onChange={(e) => setType(e.target.value as TaskType)}
            value={type}
          >
            <option value={"PRINT"}>PRINT</option>
            <option value={"WRITE"}>WRITE</option>
            <option value={"DELETE"}>DELETE</option>
          </select>
        </div>
        <div className="row">
          <div>Recurring</div>
          <input
            type="checkbox"
            checked={recurring}
            onChange={(e) => {
              setRecurring(e.target.checked);
            }}
          />
        </div>
        {recurring && (
          <div className="row">
            <div>Interval in seconds</div>
            <input
              type="number"
              value={interval ?? -1}
              onChange={(e) => {
                setInterval(Number(e.target.value));
              }}
            />
          </div>
        )}
        {(type === "DELETE" || type === "WRITE") && (
          <div className="row">
            <div>Path relative</div>
            <input
              type="text"
              value={path}
              onChange={(e) => {
                setPath(e.target.value);
              }}
            />
          </div>
        )}
        {(type === "WRITE" || type === "PRINT") && (
          <div className="row">
            <div>Message</div>
            <input
              type="text"
              value={message}
              onChange={(e) => {
                setMessage(e.target.value);
              }}
            />
          </div>
        )}
        <button type="submit">Create task</button>
      </form>
    </>
  );
}
export default CreateTask;
