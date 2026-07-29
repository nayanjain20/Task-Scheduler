import java.time.Instant;

import model.PrintTask;
import model.ScheduledExecution;
import model.Task;
import model.TaskSchedule;
import service.Scheduler;

public class Main{

    // static Scheduler scheduler = new Scheduler();
    public static void main(String args[]){
        System.out.println("Task Scheduler :)\n");
        Scheduler scheduler = new Scheduler();
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();
        Runnable clientRunnable = new ClientProcess(scheduler, schedulerThread);
        Thread clienThread = new Thread(clientRunnable);
        clienThread.start();
    }
}

class ClientProcess implements Runnable{
    Scheduler scheduler;
    Thread schedulerThread;
    ClientProcess(Scheduler scheduler,  Thread schedulerThread){
        this.scheduler = scheduler;
        this.schedulerThread = schedulerThread;
    }
    @Override
    public void run() {
        System.out.println("Client started\nAdding 2 tasks");
        populateTest1(schedulerThread);
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + Instant.now() + "] ClientProcess interrupted");
        }
        System.out.println("Adding 2 more tasks\n");
        populateTest2(schedulerThread);
        
    }
    public void populateTest1(Thread schedulerThread){
        Task printLogsTask = new PrintTask("Print logs");
        Instant printStartTime = Instant.now().plusSeconds(5);
        TaskSchedule printLogsTaskSchedule = new TaskSchedule(printLogsTask, printStartTime, true, 5);
        ScheduledExecution printLogsScheduledExecution = new ScheduledExecution(printLogsTaskSchedule, printStartTime);
        scheduler.addScheduledExecution(printLogsScheduledExecution,schedulerThread);

        
        Task printHeartBeatTask = new PrintTask("Heartbeat");
        Instant printHeartBeatStartTime = Instant.now().plusSeconds(6);
        TaskSchedule printHeartBeatTaskSchedule = new TaskSchedule(printHeartBeatTask, printHeartBeatStartTime, true, 2);
        ScheduledExecution printHeartBeatScheduledExecution = new ScheduledExecution(printHeartBeatTaskSchedule, printHeartBeatStartTime);
        scheduler.addScheduledExecution(printHeartBeatScheduledExecution, schedulerThread);

    }
    public void populateTest2(Thread schedulerThread){
        Task printLogsTask = new PrintTask("Cleaning cache");
        Instant printStartTime = Instant.now().plusSeconds(5);
        TaskSchedule printLogsTaskSchedule = new TaskSchedule(printLogsTask, printStartTime, true, 5);
        ScheduledExecution printLogsScheduledExecution = new ScheduledExecution(printLogsTaskSchedule, printStartTime);
        scheduler.addScheduledExecution(printLogsScheduledExecution, schedulerThread);

        
        Task printHeartBeatTask = new PrintTask("Playing Song");
        Instant printHeartBeatStartTime = Instant.now().plusSeconds(6);
        TaskSchedule printHeartBeatTaskSchedule = new TaskSchedule(printHeartBeatTask, printHeartBeatStartTime, true, 2);
        ScheduledExecution printHeartBeatScheduledExecution = new ScheduledExecution(printHeartBeatTaskSchedule, printHeartBeatStartTime);
        scheduler.addScheduledExecution(printHeartBeatScheduledExecution, schedulerThread);

    }
}