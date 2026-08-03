package model;

import java.time.Instant;

import util.Logger;

public class PrintTask extends Task{

    public PrintTask(String taskName, int taskId){
        super(taskId, taskName, TaskStatus.ACTIVE);
    }
    void print(){
        Logger.log("[" + Instant.now() + "] "+taskName);
    }
    @Override
    public void execute() {
        print();  
    }
    
}
