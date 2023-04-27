package com.zerdicorp.acl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static com.intellij.notification.NotificationType.WARNING;
import static com.zerdicorp.acl.ACLStateService.getChangelogPath;
import static com.zerdicorp.acl.ACLStateService.saveChangelogPath;
import static com.zerdicorp.acl.ACLUtils.*;

public class ACLActivity implements StartupActivity {
    final public static String CHANGELOG_FILE_NAME = "changelog.txt";
    static String CHANGELOG_FILE_PATH;

    public static boolean findChangelog(Project project) {
        final String projectPath = project.getBasePath();
        final List<Path> foundChangelogs = ACLUtils.findFile(projectPath, CHANGELOG_FILE_NAME);

        System.out.println(foundChangelogs);

        if (foundChangelogs.size() == 1) {
            CHANGELOG_FILE_PATH = foundChangelogs.get(0).toString();
            return true;
        } else if (foundChangelogs.size() > 1) {
            String[] variants = foundChangelogs
                    .stream()
                    .map(Path::toString)
                    .toArray(String[]::new);
            CHANGELOG_FILE_PATH = select(
                    "ACL Dialog",
                    "I found more than one changelog file. Please choose one:",
                    variants,
                    variants[0],
                    Messages.getWarningIcon()
            );
            return CHANGELOG_FILE_PATH != null;
        }
        return false;
    }

    @Override
    public void runActivity(@NotNull Project project) {
        String maybeChangelogPath = getChangelogPath(project);
        if (maybeChangelogPath == null || !new File(maybeChangelogPath).exists()) {
            if (findChangelog(project)) {
                changelogFileFoundNotification(project, CHANGELOG_FILE_PATH);
            } else {
                notification(
                        "ACL Skip",
                        "Can't find " + CHANGELOG_FILE_NAME + ".. Skip!",
                        project,
                        WARNING,
                        List.of(CHOOSE_ANOTHER_ACTION)
                );
            }
            saveChangelogPath(project, CHANGELOG_FILE_PATH);
        } else {
            changelogFileFoundNotification(project, maybeChangelogPath);
        }
    }
}
