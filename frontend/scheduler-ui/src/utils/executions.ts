export interface Execution {
  id: string;
  time: string;
  status: string;
}

const istFormatter = new Intl.DateTimeFormat("en-IN", {
  timeZone: "Asia/Kolkata",
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: true,
});

export function formatIst(time: string): string {
  return `${istFormatter.format(new Date(time))} IST`;
}

export function newestExecutionsFirst(executions: Execution[]): Execution[] {
  return [...executions].sort((a, b) => Date.parse(b.time) - Date.parse(a.time));
}

export function summarizeExecutions(executions: Execution[]) {
  const finished = executions.filter(
    (execution) =>
      execution.status === "COMPLETED" || execution.status === "FAILED",
  );
  const lastRun = finished.reduce<Execution | null>(
    (latest, execution) =>
      latest === null ||
      Date.parse(execution.time) > Date.parse(latest.time)
        ? execution
        : latest,
    null,
  );
  return { count: finished.length, lastRun };
}
