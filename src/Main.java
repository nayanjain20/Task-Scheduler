
import service.Executor;
import service.Scheduler;

public class Main{



    static Scheduler scheduler;
    static Executor executor;
    public static void main(String args[]){
        
        executor = new Executor();
        scheduler = new Scheduler(executor);
        Runnable clientRunnable = new Client(scheduler);
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
                scheduler.proceedScheduledExecution();
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println("[SCHEDULER] Interrupted: " + e.getMessage());
            }
        }
    }
}
