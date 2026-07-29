package model;

import java.time.Instant;

public class PrintTask implements Task{

    String printJob;

    public PrintTask(String printJob){
        this.printJob = printJob;
    }
    void print(){
        System.out.println("[" + Instant.now() + "] "+printJob);
    }
    @Override
    public void execute() {
        print();  
    }
}
