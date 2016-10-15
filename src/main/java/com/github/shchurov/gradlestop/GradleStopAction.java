package com.github.shchurov.gradlestop;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class GradleStopAction extends AnAction {

    private static final String TITLE = "GradleStop";
    private static final String GITHUB_LINK = "https://github.com/shchurov/GradleStop";

    @Override
    public void actionPerformed(AnActionEvent action) {
        Project project = action.getData(PlatformDataKeys.PROJECT);
        String projectPath = project.getBasePath();
        File gradlewFile = new File(projectPath, isWindows() ? "gradlew.bat" : "gradlew");
        if (gradlewFile.exists()) {
            try {
                execute(gradlewFile.getPath() + " --stop", project);
            } catch (IOException e) {
                handleException(e);
            }
        } else {
            showErrorNotification(gradlewFile.getPath() + " not found");
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private String execute(String command, Project project) throws IOException {
        ProgressManager pm = ProgressManager.getInstance();
        return pm.runProcessWithProgressSynchronously(() -> {
            pm.getProgressIndicator().setIndeterminate(true);
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() != 0) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString().trim();
        }, TITLE, false, project);
    }

    private void handleException(Exception e) {
        String message = "I would really appreciate if you file this issue here: " + GITHUB_LINK + "\n";
        PluginManager.getLogger().error(message, e);
    }

    private void showErrorNotification(String text) {
        Notification n = new Notification("com.github.shchurov.gradlestop", TITLE, text, NotificationType.ERROR);
        Notifications.Bus.notify(n);
    }

}
