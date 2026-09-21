import { formatIst, newestExecutionsFirst, type Execution } from "../utils/executions";

export interface ExecutionListProps {
  executions: Execution[];
}
function ExecutionList({ executions }: ExecutionListProps) {
  return (
    <div className="execution-history">
      <h4>Execution history - newest first</h4>
      {executions.length === 0 ? <p>No executions recorded.</p> : (
        <ul>
          {newestExecutionsFirst(executions).map((execution) => (
            <li key={execution.id}>
              <time dateTime={execution.time}>{formatIst(execution.time)}</time>
              <span>{execution.status}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
export default ExecutionList;
