import { useCallback, useEffect, useRef, useState } from "react";
import "./App.css";
import TaskList from "./components/TaskList";
import CreateTask from "./components/CreateTask";
import { errorMessage, loadTasks, type Task, type TaskAction } from "./api";

function App() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const requestVersion = useRef(0);

  const refreshTasks = useCallback(async () => {
    const version = ++requestVersion.current;
    try {
      const tasks = await loadTasks();
      if (version === requestVersion.current) {
        setTasks(tasks);
        setError(null);
      }
    } catch (error) {
      if (version === requestVersion.current) {
        setError(`Could not refresh tasks. Displayed data may be out of date. ${errorMessage(error)}`);
      }
    } finally {
      if (version === requestVersion.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    const requests = requestVersion;
    let stopped = false;
    let timer: ReturnType<typeof setTimeout>;
    async function poll() {
      await refreshTasks();
      if (!stopped) {
        timer = setTimeout(poll, 5000);
      }
    }
    void poll();
    return () => {
      stopped = true;
      clearTimeout(timer);
      ++requests.current;
    };
  }, [refreshTasks]);

  function onActionComplete(taskId: string, action: TaskAction) {
    ++requestVersion.current;
    const status = { pause: "PAUSE", resume: "ACTIVE", cancel: "CANCEL" }[action];
    setTasks((tasks) => tasks.map((task) =>
      task.id === taskId ? { ...task, status } : task,
    ));
    void refreshTasks();
  }

  return (
    <>
      <h1>Task scheduler</h1>
      <CreateTask refreshTasks={refreshTasks} />
      <section className="tasks-section" aria-label="Tasks">
        <div className="tasks-toolbar">
          <h2>Your tasks</h2>
          <button onClick={() => void refreshTasks()}>Refresh</button>
        </div>
        <p className="summary-note">Refreshes every 5 seconds. All execution times are in India Standard Time (IST).</p>
        {error && <p className="error-message" role="alert">{error}</p>}
        {loading && <p role="status">Loading tasks...</p>}
        {!loading && !error && tasks.length === 0 && <p>No tasks yet. Create one above.</p>}
        <TaskList tasks={tasks} onActionComplete={onActionComplete} />
      </section>
    </>
  );
}

export default App;
