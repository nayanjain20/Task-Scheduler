package com.nayan.scheduler.core.factory;

import com.nayan.scheduler.core.model.DeleteTask;
import com.nayan.scheduler.core.model.PrintTask;
import com.nayan.scheduler.core.model.Task;
import com.nayan.scheduler.core.model.WriteTask;

public class TaskFactory {

    public static Task createPrintTask(String taskName) {
        return new PrintTask(taskName);
    }

    public static Task createWriteTask(String taskName, String filePath, String message) {
        return new WriteTask(taskName, filePath, message);
    }

    public static Task createDeleteTask(String taskName, String filePath) {
        return new DeleteTask(taskName, filePath);
    }

}
