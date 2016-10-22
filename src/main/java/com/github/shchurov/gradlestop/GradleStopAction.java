package com.github.shchurov.gradlestop;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GradleStopAction extends AnAction {

    private static final String TITLE = "GradleStop";
    private static final String GITHUB_LINK = "https://github.com/shchurov/GradleStop";

    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private Future stopTask;
    private Process process;

    @Override
    public void actionPerformed(AnActionEvent action) {
        Project project = action.getData(PlatformDataKeys.PROJECT);
        String projectPath = project.getBasePath();
        File gradlewFile = new File(projectPath, isWindows() ? "gradlew.bat" : "gradlew");
        if (gradlewFile.exists()) {
            runStopTask(gradlewFile.getPath());
        } else {
            showNotification(gradlewFile.getPath() + " not found", true);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private void runStopTask(String gradlewPath) {
        if (stopTask != null && !stopTask.isDone()) {
            return;
        }
        stopTask = executor.submit(() -> {
            boolean cancelled = destroyProcessIfRunning(process);
            if (cancelled) {
                showNotification("Re-executing stop command...", false);
            } else {
                showNotification("Executing stop command...", false);
            }
            try {
                process = Runtime.getRuntime().exec(gradlewPath + " --stop");
                runResultListenerTask(process);
            } catch (IOException e) {
                handleUnexpectedException(e);
            }
        });
    }

    private boolean destroyProcessIfRunning(Process p) {
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
            try {
                p.waitFor();
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void runResultListenerTask(final Process p) {
        executor.execute(() -> {
            try {
                p.waitFor();
                if (p.exitValue() == 0) {
                    showNotification("Done", false);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleUnexpectedException(Exception e) {
        String message = "I would really appreciate if you file this issue here: " + GITHUB_LINK + "\n";
        PluginManager.getLogger().error(message, e);
    }

    private void showNotification(String text, boolean error) {
        Notification n = new Notification("com.github.shchurov.gradlestop", TITLE, text,
                error ? NotificationType.ERROR : NotificationType.INFORMATION);
        Notifications.Bus.notify(n);
    }

}
