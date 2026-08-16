import type { Execution } from "./TaskCard";

export interface ExecutionListProps {
  executions: Execution[];
}
function ExecutionList({ executions }: ExecutionListProps) {
  return (
    <>
      {executions.map((execution) => (
        <div key={execution.id}>
          {execution.time} | {execution.status}
        </div>
      ))}
    </>
  );
}
export default ExecutionList;
