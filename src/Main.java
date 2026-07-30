import java.time.Instant;

import model.PrintTask;
import model.ScheduledExecution;
import model.Task;
import model.TaskSchedule;
import service.Executor;
import service.Scheduler;

public class Main{



    static Scheduler scheduler;
    static Executor executor;
    public static void main(String args[]){
        System.out.println("Task Scheduler :)\n");
        executor = new Executor();
        scheduler = new Scheduler(executor);
        Runnable clientRunnable = new ClientProcess(scheduler);
        Runnable schedulerRunnable = new SchedulerProcess(scheduler);
        Thread clientThread = new Thread(clientRunnable);
        Thread schedulerThread = new Thread(schedulerRunnable);
        clientThread.start();
        schedulerThread.start();
    }
}

class SchedulerProcess implements Runnable{
    Scheduler scheduler;
    SchedulerProcess(Scheduler scheduler){
        this.scheduler = scheduler;
    }
    @Override
    public void run() {
        System.out.println("Scheduler started\n");
        while(true){
            // System.out.println("In Loop");
            try{
                scheduler.waitUntilNextExecution();
                scheduler.proceedScheduedExecution();
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println("[SCHEDULER] Interrupted: " + e.getMessage());
            }
        }
    }
}

class ClientProcess implements Runnable{
    Scheduler scheduler;
    ClientProcess(Scheduler scheduler){
        this.scheduler = scheduler;
    }
    @Override
    public void run() {
        System.out.println("Client started\nAdding 2 tasks");
        populateTest1();
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + Instant.now() + "] ClientProcess interrupted");
        }
        System.out.println("Adding 2 more tasks\n");
        populateTest2();
        
    }
    public void populateTest1(){
        Task printLogsTask = new PrintTask("Print logs");
        Instant printStartTime = Instant.now().plusSeconds(5);
        TaskSchedule printLogsTaskSchedule = new TaskSchedule(printLogsTask, printStartTime, true, 1);
        ScheduledExecution printLogsScheduledExecution = new ScheduledExecution(printLogsTaskSchedule, printStartTime);
        scheduler.addScheduledExecution(printLogsScheduledExecution);

        
        Task printHeartBeatTask = new PrintTask("Heartbeat");
        Instant printHeartBeatStartTime = Instant.now().plusSeconds(6);
        TaskSchedule printHeartBeatTaskSchedule = new TaskSchedule(printHeartBeatTask, printHeartBeatStartTime, true, 1);
        ScheduledExecution printHeartBeatScheduledExecution = new ScheduledExecution(printHeartBeatTaskSchedule, printHeartBeatStartTime);
        scheduler.addScheduledExecution(printHeartBeatScheduledExecution);

    }
    public void populateTest2(){
        Task printLogsTask = new PrintTask("Cleaning cache");
        Instant printStartTime = Instant.now().plusSeconds(5);
        TaskSchedule printLogsTaskSchedule = new TaskSchedule(printLogsTask, printStartTime, true, 1);
        ScheduledExecution printLogsScheduledExecution = new ScheduledExecution(printLogsTaskSchedule, printStartTime);
        scheduler.addScheduledExecution(printLogsScheduledExecution);

        
        Task printHeartBeatTask = new PrintTask("Playing Song");
        Instant printHeartBeatStartTime = Instant.now().plusSeconds(6);
        TaskSchedule printHeartBeatTaskSchedule = new TaskSchedule(printHeartBeatTask, printHeartBeatStartTime, true, 1);
        ScheduledExecution printHeartBeatScheduledExecution = new ScheduledExecution(printHeartBeatTaskSchedule, printHeartBeatStartTime);
        scheduler.addScheduledExecution(printHeartBeatScheduledExecution);

    }
}