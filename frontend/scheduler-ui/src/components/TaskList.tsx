import TaskCard from "./TaskCard";
import type { Task, TaskAction } from "../api";

interface TaskListProps {
  tasks: Task[];
  onActionComplete: (taskId: string, action: TaskAction) => void;
}
function TaskList({ tasks, onActionComplete }: TaskListProps) {
  return (
    <>
      <div className="task-list">
        {tasks.map((task) => (
          <TaskCard key={task.id} task={task} onActionComplete={onActionComplete} />
        ))}
      </div>
    </>
  );
}

export default TaskList;
