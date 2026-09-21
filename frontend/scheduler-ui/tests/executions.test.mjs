import assert from "node:assert/strict";
import test from "node:test";
import { formatIst, newestExecutionsFirst, summarizeExecutions } from "../src/utils/executions.ts";
import { loadTasks, performTaskAction } from "../src/api.ts";

test("empty and unfinished history has no finished runs", () => {
  assert.deepEqual(summarizeExecutions([]), { count: 0, lastRun: null });
  assert.deepEqual(summarizeExecutions(
    ["PENDING", "DISCARDED", "SKIPPED"].map((status) => ({
      id: status, status, time: "2026-09-18T23:00:00Z",
    })),
  ), { count: 0, lastRun: null });
});

test("counts only finished executions and selects latest timestamp, not list order", () => {
  const latest = { id: "failed", status: "FAILED", time: "2026-09-18T18:45:00Z" };
  const executions = [
    latest,
    { id: "pending", status: "PENDING", time: "2099-01-01T00:00:00Z" },
    { id: "completed", status: "COMPLETED", time: "2026-09-18T18:00:00Z" },
    { id: "skipped", status: "SKIPPED", time: "2026-09-18T20:00:00Z" },
    { id: "discarded", status: "DISCARDED", time: "2026-09-18T21:00:00Z" },
  ];
  assert.deepEqual(summarizeExecutions(executions), { count: 2, lastRun: latest });
  assert.equal(executions[0], latest);
});

test("IST formatting handles midnight rollover and offset input", () => {
  const result = formatIst("2026-09-18T18:45:00Z");
  assert.match(result, /19 Sept? 2026/);
  assert.match(result, /12:15:00 am IST$/i);
  assert.equal(result, formatIst("2026-09-19T00:15:00+05:30"));
});

test("history sorts newest to oldest across dates and timezone offsets without mutating input", () => {
  const executions = [
    { id: "oldest", status: "COMPLETED", time: "2026-09-17T23:59:00Z" },
    { id: "newest", status: "PENDING", time: "2026-09-20T00:00:00Z" },
    { id: "middle", status: "FAILED", time: "2026-09-19T00:15:00+05:30" },
  ];
  assert.deepEqual(newestExecutionsFirst(executions).map((execution) => execution.id),
    ["newest", "middle", "oldest"]);
  assert.equal(executions[0].id, "oldest");
  assert.deepEqual(newestExecutionsFirst([]), []);
});

const tasks = [{ taskId: "task-1", taskName: "Example", taskType: "PRINT", taskStatus: "ACTIVE" }];

test("loads tasks with execution summaries", async (t) => {
  t.mock.method(globalThis, "fetch", async (url) => Response.json(
    url.endsWith("/executions")
      ? { executions: [{ taskExecutionId: "e1", executionTime: "2026-09-18T10:00:00Z", executionStatus: "COMPLETED" }] }
      : { tasks },
  ));
  const result = await loadTasks();
  assert.equal(result[0].name, "Example");
  assert.equal(result[0].executionError, null);
  assert.equal(summarizeExecutions(result[0].executions).count, 1);
});

test("failed or malformed execution responses are unavailable, never zero", async (t) => {
  for (const response of [
    new Response("Unavailable", { status: 503 }),
    Response.json({ executions: [{ executionTime: "invalid" }] }),
    Response.json({}),
  ]) {
    const mock = t.mock.method(globalThis, "fetch", async (url) =>
      url.endsWith("/executions") ? response : Response.json({ tasks }),
    );
    const result = await loadTasks();
    assert.equal(result[0].executions, null);
    assert.match(result[0].executionError, /Could not load executions/);
    mock.mock.restore();
  }
});

test("task list HTTP failure is surfaced", async (t) => {
  t.mock.method(globalThis, "fetch", async () => new Response("", { status: 500 }));
  await assert.rejects(loadTasks(), /500/);
});

test("actions POST to correct endpoint and require true acknowledgement", async (t) => {
  const calls = [];
  const mock = t.mock.method(globalThis, "fetch", async (url, options) => {
    calls.push([url, options.method]);
    return Response.json(true);
  });
  for (const action of ["pause", "resume", "cancel"]) {
    await performTaskAction("task-1", action);
  }
  assert.deepEqual(calls, ["pause", "resume", "cancel"].map((action) =>
    [`http://localhost:8080/tasks/task-1/${action}`, "POST"],
  ));
  mock.mock.restore();
  t.mock.method(globalThis, "fetch", async () => Response.json(false));
  await assert.rejects(performTaskAction("task-1", "pause"), /did not accept/);
});
