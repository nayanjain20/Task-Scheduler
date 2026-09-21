import assert from "node:assert/strict";
import test from "node:test";
import { buildCreateTaskRequest } from "../src/utils/createTask.ts";
import { createTask } from "../src/api.ts";

const draft = {
  name: "  Heartbeat  ",
  type: "PRINT",
  recurring: false,
  interval: "",
  message: "hidden message",
  path: "hidden path",
};

test("one-time PRINT trims name, sends zero interval, and omits hidden payload", () => {
  assert.deepEqual(buildCreateTaskRequest(draft), {
    type: "PRINT",
    taskName: "Heartbeat",
    schedule: { interval: 0, recurring: false },
    payload: {},
  });
});

test("recurring WRITE preserves message whitespace and trims path", () => {
  assert.deepEqual(buildCreateTaskRequest({
    ...draft, type: "WRITE", recurring: true, interval: "60",
    path: "  temp\\test.txt  ", message: "  line one\nline two  ",
  }), {
    type: "WRITE",
    taskName: "Heartbeat",
    schedule: { interval: 60, recurring: true },
    payload: { filePath: "temp\\test.txt", message: "  line one\nline two  " },
  });
});

test("DELETE sends only a file path; WRITE allows an empty message", () => {
  assert.deepEqual(buildCreateTaskRequest({ ...draft, type: "DELETE" }).payload,
    { filePath: "hidden path" });
  assert.equal(buildCreateTaskRequest({ ...draft, type: "WRITE", message: "" }).payload.message, "");
});

test("rejects blank names and file paths", () => {
  assert.throws(() => buildCreateTaskRequest({ ...draft, name: "  " }), /task name/);
  for (const type of ["WRITE", "DELETE"]) {
    assert.throws(() => buildCreateTaskRequest({ ...draft, type, path: " \t " }), /file path/);
  }
});

test("validates recurring intervals including backend integer limits", () => {
  for (const interval of ["", " ", "0", "-1", "1.5", "Infinity", "NaN", "2147483648"]) {
    assert.throws(() => buildCreateTaskRequest({ ...draft, recurring: true, interval }), /interval/);
  }
  for (const interval of ["1", "60", "2147483647"]) {
    assert.equal(buildCreateTaskRequest({ ...draft, recurring: true, interval }).schedule.interval, Number(interval));
  }
});

test("creates via JSON POST and returns the confirmed task ID", async (t) => {
  const request = buildCreateTaskRequest(draft);
  t.mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, "http://localhost:8080/tasks");
    assert.equal(options.method, "POST");
    assert.equal(options.headers["Content-Type"], "application/json");
    assert.deepEqual(JSON.parse(options.body), request);
    return Response.json({ taskId: "created-id", status: "Complete" });
  });
  assert.equal(await createTask(request), "created-id");
});

test("null task IDs and malformed success responses are not treated as created", async (t) => {
  for (const data of [{ taskId: null, status: "Complete" }, {}, null, { taskId: " " }]) {
    const mock = t.mock.method(globalThis, "fetch", async () => Response.json(data));
    await assert.rejects(createTask(buildCreateTaskRequest(draft)), /did not confirm/);
    mock.mock.restore();
  }
});

test("creation surfaces HTTP and network errors", async (t) => {
  const mock = t.mock.method(globalThis, "fetch", async () => new Response("", { status: 500 }));
  await assert.rejects(createTask(buildCreateTaskRequest(draft)), /500/);
  mock.mock.restore();
  t.mock.method(globalThis, "fetch", async () => { throw new TypeError("Failed to fetch"); });
  await assert.rejects(createTask(buildCreateTaskRequest(draft)), /Failed to fetch/);
});
