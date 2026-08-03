
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import model.ScheduledExecution;
import model.Task;
import model.Task.TaskStatus;
import model.TaskSchedule;
import service.Scheduler;
import factory.TaskFactory;
import service.Worker;
import util.Logger;

/**
 * Interactive CLI client for managing tasks in the scheduler.
 * Supports adding, cancelling, pausing, and resuming tasks.
 */
public class Client implements Runnable {

    private final Scheduler scheduler;
    private final Map<Integer, Task> taskMap = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);

    private int currentTaskId = 0;

    public Client(Scheduler scheduler) {
        this.scheduler = scheduler;
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

            switch (option) {

                case "1":
                    addTaskMenu();
                    break;

                case "2":
                    printTaskSummary();
                    System.out.print("Enter Task Id to Cancel: ");
                    cancelTask(Integer.parseInt(scanner.nextLine()));
                    break;

                case "3":
                    printTaskSummary();
                    System.out.print("Enter Task Id to Pause: ");
                    pauseTask(Integer.parseInt(scanner.nextLine()));
                    break;

                case "4":
                    printTaskSummary();
                    System.out.print("Enter Task Id to Resume: ");
                    resumeTask(Integer.parseInt(scanner.nextLine()));
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
        for (Task task : taskMap.values()) {
            System.out.printf("[%d] %-30s %-10s%n",
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
                task = TaskFactory.creatPrintTask(currentTaskId++, taskName);
                break;
            }

            case 2: {
                System.out.print("Target File Path: ");
                String filePath = scanner.nextLine();

                System.out.print("Message: ");
                String message = scanner.nextLine();

                task = TaskFactory.createWriteTask(
                        currentTaskId++,
                        taskName,
                        filePath,
                        message);
                break;
            }

            case 3: {
                System.out.print("Target File Path: ");
                String filePath = scanner.nextLine();

                task = TaskFactory.creatDeleteTask(
                        currentTaskId++,
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

    }

    private void loadWriteDemoTasks() {
        Task writeTaskA = TaskFactory.createWriteTask(currentTaskId++, "Write A", "temp/a.txt", "Hi");
        scheduleTask(writeTaskA, 5, true, 1);

    }

    private void loadDeleteDemoTasks() {
        Task deleteTaskA = TaskFactory.creatDeleteTask(currentTaskId++, "Delete A", "temp/a.txt");
        scheduleTask(deleteTaskA, 10, true, 5);
    }

    private void loadPrintDemoTasks() {
        Task prinTask = TaskFactory.creatPrintTask(currentTaskId++, "Heart beat");
        scheduleTask(prinTask, 0, true, 5);
    }

    public void listTask(boolean showExecutions) {
        System.out.println("\n=========== TASKS ===========");
        for (Task task : taskMap.values()) {
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
                + " | Recurring: " + task.getTaskSchedule().isRecurring()
                + " | Status: " + task.getTaskStatus());
    }

    public void listTaskExecutions(Task task) {
        System.out.println("Executions:");
        for (ScheduledExecution execution : task.getScheduledExecutions()) {
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
                task,
                startTime,
                recurring,
                intervalSeconds);

        ScheduledExecution execution = new ScheduledExecution(schedule, startTime);

        task.setTaskSchedule(schedule);
        task.getScheduledExecutions().add(execution);

        taskMap.put(task.getTaskId(), task);

        scheduler.addScheduledExecution(execution);
    }

    public void cancelTask(int taskId) {
        Task task = taskMap.get(taskId);
        if (task != null) {
            task.setTaskStatus(TaskStatus.DEACTIVE);
            System.out.println("[CLIENT] Task cancelled: " + task.getTaskName());
        }
    }

    public void pauseTask(int taskId) {
        Task task = taskMap.get(taskId);
        if (task != null) {
            task.setTaskStatus(TaskStatus.PAUSE);
            System.out.println("[CLIENT] Task paused: " + task.getTaskName());
        }
    }

    public void resumeTask(int taskId) {

        Task task = taskMap.get(taskId);

        if (task == null)
            return;

        task.setTaskStatus(TaskStatus.ACTIVE);

        Instant executionTime = Instant.now();

        if (executionTime.isBefore(task.getTaskSchedule().getStartTime())) {
            executionTime = task.getTaskSchedule().getStartTime();
        }

        ScheduledExecution execution = new ScheduledExecution(task.getTaskSchedule(), executionTime);

        task.getScheduledExecutions().add(execution);

        scheduler.addScheduledExecution(execution);
        System.out.println("[CLIENT] Task resumed: " + task.getTaskName());
    }
}
