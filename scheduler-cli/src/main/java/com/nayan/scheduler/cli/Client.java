package com.nayan.scheduler.cli;

import java.time.Instant;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import com.nayan.scheduler.core.factory.TaskFactory;
import com.nayan.scheduler.core.model.TaskExecution;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.Task.TaskStatus;
import com.nayan.scheduler.core.model.TaskSchedule;
import com.nayan.scheduler.core.service.Scheduler;
import com.nayan.scheduler.core.service.Worker;
import com.nayan.scheduler.core.store.TaskExecutionStore;
import com.nayan.scheduler.core.store.TaskScheduleStore;
import com.nayan.scheduler.core.store.TaskStore;
import com.nayan.scheduler.core.util.Logger;

/**
 * Interactive CLI client for managing tasks in the scheduler.
 * Supports adding, cancelling, pausing, and resuming tasks.
 */
public class Client implements Runnable {

    private final Scheduler scheduler;
    private final Scanner scanner = new Scanner(System.in);
    private TaskStore taskStore;
    private TaskExecutionStore taskExecutionStore;
    private TaskScheduleStore taskScheduleStore;

    public Client(Scheduler scheduler, TaskStore taskStore, TaskScheduleStore taskScheduleStore,
            TaskExecutionStore taskExecutionStore) {
        this.scheduler = scheduler;
        this.taskStore = taskStore;
        this.taskScheduleStore = taskScheduleStore;
        this.taskExecutionStore = taskExecutionStore;
    }

    @Override
    public void run() {

        Logger.initialize();

        System.out.println("==================================");
        System.out.println("      TASK SCHEDULER");
        System.out.println("==================================");

        // Preload demo tasks
        loadDemoTasks();

        while (true) {

            printMenu();

            String option = scanner.nextLine();
            UUID taskId;

            switch (option) {

                case "1":
                    addTaskMenu();
                    break;

                case "2":
                    printTaskSummary();
                    System.out.print("Enter Task Id to Cancel: ");
                    taskId = UUID.fromString(scanner.nextLine());
                    cancelTask(taskId);
                    break;

                case "3":
                    printTaskSummary();
                    System.out.print("Enter Task Id to Pause: ");
                    taskId = UUID.fromString(scanner.nextLine());
                    pauseTask(taskId);
                    break;

                case "4":
                    printTaskSummary();
                    System.out.print("Enter Task Id to Resume: ");
                    taskId = UUID.fromString(scanner.nextLine());
                    resumeTask(taskId);
                    break;

                case "5":
                    listTask(false);
                    break;

                case "6":
                    listTask(true);
                    break;

                case "7":
                    loadDemoTasks();
                    System.out.println("Demo tasks added.");
                    break;

                case "8":
                    System.out.println("Bye!");
                    return;

                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n================ MENU ================");
        System.out.println("1. Add Task");
        System.out.println("2. Cancel Task");
        System.out.println("3. Pause Task");
        System.out.println("4. Resume Task");
        System.out.println("5. List Tasks");
        System.out.println("6. List Tasks + Executions");
        System.out.println("7. Load Demo Tasks");
        System.out.println("8. Exit");
        System.out.print("> ");
    }

    private void printTaskSummary() {
        System.out.println("\nAvailable Tasks:");
        List<Task> tasks = taskStore.getAllTasks();
        for (Task task : tasks) {
            System.out.printf("%-38s %-30s %-10s%n",
                    task.getTaskId(),
                    task.getTaskName(),
                    task.getTaskStatus());
        }
    }

    private void addTaskMenu() {

        System.out.println("\nSelect Task Type:");
        System.out.println("1. Print Task");
        System.out.println("2. Write Task");
        System.out.println("3. Delete Task");
        System.out.print("> ");

        int taskType = Integer.parseInt(scanner.nextLine());

        System.out.print("Task Name: ");
        String taskName = scanner.nextLine();

        System.out.print("Start After (seconds): ");
        int delaySeconds = Integer.parseInt(scanner.nextLine());

        System.out.print("Recurring (y/n): ");
        boolean recurring = scanner.nextLine().equalsIgnoreCase("y");

        int intervalSeconds = 0;
        if (recurring) {
            System.out.print("Interval (seconds): ");
            intervalSeconds = Integer.parseInt(scanner.nextLine());
        }

        Task task = null;

        switch (taskType) {

            case 1: {
                task = TaskFactory.createPrintTask(taskName);
                break;
            }

            case 2: {
                System.out.print("Target File Path: ");
                String filePath = scanner.nextLine();

                System.out.print("Message: ");
                String message = scanner.nextLine();

                task = TaskFactory.createWriteTask(

                        taskName,
                        filePath,
                        message);
                break;
            }

            case 3: {
                System.out.print("Target File Path: ");
                String filePath = scanner.nextLine();

                task = TaskFactory.createDeleteTask(

                        taskName,
                        filePath);
                break;
            }

            default:
                System.out.println("Invalid task type.");
                return;
        }

        scheduleTask(
                task,
                delaySeconds,
                recurring,
                intervalSeconds);

        System.out.println("[CLIENT] Task added successfully.");
    }

    private void loadDemoTasks() {
        loadWriteDemoTasks();
        loadDeleteDemoTasks();
        loadPrintDemoTasks();
        printTaskSummary();
    }

    private void loadWriteDemoTasks() {
        Task writeTaskA = TaskFactory.createWriteTask("Write A", "temp/a.txt", "Hi");
        taskStore.addTask(writeTaskA);
        scheduleTask(writeTaskA, 5, true, 1);

    }

    private void loadDeleteDemoTasks() {
        Task deleteTaskA = TaskFactory.createDeleteTask("Delete A", "temp/a.txt");
        taskStore.addTask(deleteTaskA);
        scheduleTask(deleteTaskA, 10, true, 5);
    }

    private void loadPrintDemoTasks() {
        Task prinTask = TaskFactory.createPrintTask("Heart beat");
        taskStore.addTask(prinTask);
        scheduleTask(prinTask, 0, true, 5);
        Task oneTimePrintTask = TaskFactory.createPrintTask("One time task [Hello]");
        taskStore.addTask(oneTimePrintTask);
        scheduleTask(oneTimePrintTask, 0, false, 10);
    }

    public void listTask(boolean showExecutions) {
        System.out.println("\n=========== TASKS ===========");
        List<Task> tasks = taskStore.getAllTasks();
        for (Task task : tasks) {
            printTask(task);
            if (showExecutions) {
                listTaskExecutions(task);
            }
            System.out.println();
        }
    }

    public void printTask(Task task) {
        System.out.println("[" + task.getTaskId() + "] "
                + task.getTaskName()
                // + " | Recurring: " + Schedule
                + " | Status: " + task.getTaskStatus());
    }

    public void listTaskExecutions(Task task) {
        System.out.println("Executions:");
        List<TaskExecution> executions = taskExecutionStore.getTaskExecutionsForTask(task.getTaskId());
        for (TaskExecution execution : executions) {
            Worker worker = execution.getWorker();
            String workerId = (worker == null) ? "PENDING" : String.valueOf(worker.getWorkerId());

            System.out.println("  " + execution.getExecutionTime()
                    + " | Worker: " + workerId + " | Status: " + execution.getExecutionStatus());
        }
    }

    public void scheduleTask(Task task,
            int delaySeconds,
            boolean recurring,
            int intervalSeconds) {

        Instant startTime = Instant.now().plusSeconds(delaySeconds);

        TaskSchedule schedule = new TaskSchedule(
                task.getTaskId(),
                startTime,
                recurring,
                intervalSeconds);
        taskScheduleStore.addTaskSchedule(schedule);
        task.setTaskScheduleId(schedule.getTaskScheduleId());
        taskStore.updateTask(task);

        TaskExecution execution = new TaskExecution(task.getTaskId(), schedule.getTaskScheduleId(), startTime);

        scheduler.addScheduledExecution(execution);
    }

    public void cancelTask(UUID taskId) {
        Task task = taskStore.getTask(taskId);
        if (task != null) {
            task.setTaskStatus(TaskStatus.DEACTIVE);
            taskStore.updateTask(task);
            System.out.println("[CLIENT] Task cancelled: " + task.getTaskName());
        }
    }

    public void pauseTask(UUID taskId) {
        Task task = taskStore.getTask(taskId);
        if (task != null) {
            task.setTaskStatus(TaskStatus.PAUSE);
            taskStore.updateTask(task);
            System.out.println("[CLIENT] Task paused: " + task.getTaskName());
        }
    }

    public void resumeTask(UUID taskId) {

        Task task = taskStore.getTask(taskId);

        if (task == null)
            return;

        task.setTaskStatus(TaskStatus.ACTIVE);
        taskStore.updateTask(task);

        Instant taskStartTime = taskScheduleStore.getTaskSchedule(task.getTaskScheduleId()).getStartTime();

        Instant executionTime = Instant.now();
        if (executionTime.isBefore(taskStartTime)) {
            return;
        }

        TaskExecution execution = new TaskExecution(task.getTaskId(), task.getTaskScheduleId(),
                executionTime);

        scheduler.addScheduledExecution(execution);
        System.out.println("[CLIENT] Task resumed: " + task.getTaskName());
    }
}
