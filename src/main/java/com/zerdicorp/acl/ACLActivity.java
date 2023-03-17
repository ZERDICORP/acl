package com.zerdicorp.acl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.notification.NotificationType.WARNING;
import static com.zerdicorp.acl.ACLUtils.notification;

public class ACLActivity implements StartupActivity {
    final public static String CHANGELOG_FILE_NAME = "changelog.txt";
    public static String CHANGELOG_FILE_PATH;

    @Override
    public void runActivity(@NotNull Project project) {
        final String projectPath = project.getBasePath();

        final Optional<Path> pathOpt = ACLUtils.findFile(projectPath, CHANGELOG_FILE_NAME);

        if (pathOpt.isPresent()) {
            CHANGELOG_FILE_PATH = pathOpt.get().toString();
            notification("ACL Welcome", "Changelog file found. Ready to work!", project, INFORMATION);
        } else {
            notification("ACL Skip", "Can't find " + CHANGELOG_FILE_NAME + ".. Skip!", project, WARNING);
        }
    }
}
