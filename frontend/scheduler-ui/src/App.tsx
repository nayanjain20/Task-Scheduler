import { useEffect, useState } from "react";
import "./App.css";
import TaskList, { type Task } from "./components/TaskList";
import CreateTask from "./components/CreateTask";

interface TaskResponseItem {
  taskId: string;
  taskName: string;
  taskType: string;
  taskStatus: string;
}

interface TasksResponse {
  tasks?: TaskResponseItem[];
  count: number;
}

function App() {
  const [tasks, setTasks] = useState<Task[]>([]);

  useEffect(() => {
    refreshTasks();
  }, []);

  function refreshTasks() {
    fetch("http://localhost:8080/tasks")
      .then((response) => response.json())
      .then((data: TasksResponse) => {
        setTasks(
          data.tasks?.map((task) => ({
            id: task.taskId,
            name: task.taskName,
            type: task.taskType,
            status: task.taskStatus,
          })) ?? [],
        );
      })
      .catch((error) => {
        console.error("Failed to load tasks", error);
      });
  }

  return (
    <>
      <h1>Task scheduler</h1>
      <CreateTask refreshTasks={refreshTasks} />
      <TaskList tasks={tasks} />
    </>
  );
}

export default App;
