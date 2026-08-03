package service;

import model.DeleteTask;
import model.PrintTask;
import model.Task;
import model.WriteTask;

public class TaskFactory {

    public static Task creatPrintTask(int taskId, String taskName){
        return new PrintTask(taskName, taskId);
    }
    public static Task createWriteTask(int taskId, String taskName, String filePath, String message){
        return new WriteTask(taskId, taskName, filePath, message);
    }
    public static Task creatDeleteTask(int taskId, String taskName, String filePath){
        return new DeleteTask(taskId, taskName, filePath);
    }
    
}
