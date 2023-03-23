package com.zerdicorp.acl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.notification.NotificationType.WARNING;
import static com.zerdicorp.acl.ACLStateService.saveChangelogPath;
import static com.zerdicorp.acl.ACLUtils.*;

public class ACLActivity implements StartupActivity {
    final public static String CHANGELOG_FILE_NAME = "changelog.txt";
    private static String CHANGELOG_FILE_PATH;

    public static boolean findChangelog(Project project) {
        final String projectPath = project.getBasePath();
        final Optional<Path> pathOpt = ACLUtils.findFile(projectPath, CHANGELOG_FILE_NAME);

        if (pathOpt.isPresent()) {
            String tempPath = pathOpt.get().toString();
            if (Arrays.asList(tempPath.split("/")).contains("backend")) {
                CHANGELOG_FILE_PATH = tempPath;
                return true;
            }
        }
        return false;
    }

    @Override
    public void runActivity(@NotNull Project project) {
        if (findChangelog(project)) {
            saveChangelogPath(project, CHANGELOG_FILE_PATH);

            String path = shortPath(CHANGELOG_FILE_PATH, project.getBasePath());
            notification(
                    "ACL Welcome",
                    "Changelog file found:<br><br><i style='font-weight: bold;'>" + path + "</i><br><br>Ready to work!",
                    project,
                    INFORMATION,
                    List.of(CHOOSE_ANOTHER_ACTION)
            );
        } else {
            notification(
                    "ACL Skip",
                    "Can't find " + CHANGELOG_FILE_NAME + ".. Skip!",
                    project,
                    WARNING,
                    List.of(CHOOSE_ANOTHER_ACTION)
            );
        }
    }
}
