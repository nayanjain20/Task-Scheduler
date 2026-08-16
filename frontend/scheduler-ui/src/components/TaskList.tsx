import TaskCard from "./TaskCard";

export interface Task {
  id: string;
  name: string;
  type: string;
  status: string;
}
interface TaskListProps {
  tasks: Task[];
}
function TaskList({ tasks }: TaskListProps) {
  return (
    <>
      <div className="task-list">
        {tasks.map((task) => (
          <TaskCard key={task.id} task={task} />
        ))}
      </div>
    </>
  );
}

export default TaskList;
