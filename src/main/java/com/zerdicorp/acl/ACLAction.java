package com.zerdicorp.acl;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.intellij.notification.NotificationType.ERROR;
import static com.intellij.notification.NotificationType.INFORMATION;
import static com.zerdicorp.acl.ACLActivity.CHANGELOG_FILE_NAME;
import static com.zerdicorp.acl.ACLActivity.findChangelog;
import static com.zerdicorp.acl.ACLStateService.getChangelogPath;
import static com.zerdicorp.acl.ACLStateService.saveLastInsertedData;
import static com.zerdicorp.acl.ACLUtils.*;

public class ACLAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        // don't need it //
    }

    private void preCheck(Project project, String changelogPath) {
        if (changelogPath == null || !new File(changelogPath).exists()) {
            if (!findChangelog(project)) {
                throw new ACLException("File " + CHANGELOG_FILE_NAME + " not found.. " + "Please specify the file in the backend folder and try again", List.of(CHOOSE_ANOTHER_ACTION));
            }
        }
    }

    private void _actionPerformed(Project project) {
        String changelogPath = getChangelogPath(project);

        preCheck(project, changelogPath);

        String lastVersion;
        try {
            lastVersion = getLastVersion(changelogPath);
        } catch (IOException ex) {
            throw new ACLException("Can't extract last version from " + changelogPath);
        }

        // Validating last version and extracting version parts.
        int major;
        int minor;
        int patch;
        try {
            final String[] versionParts = lastVersion.split("\\.");

            major = Integer.parseInt(versionParts[0]);
            minor = Integer.parseInt(versionParts[1]);
            patch = Integer.parseInt(versionParts[2]);
        } catch (Exception ex) {
            throw new ACLException("Invalid last version in " + CHANGELOG_FILE_NAME);
        }

        final String[] possibleVersions = new String[]{(major + 1) + ".0.0", major + "." + (minor + 1) + ".0", major + "." + minor + "." + (patch + 1)};

        String newVersion = select("ACL Dialog", "Select the desired version change:", possibleVersions,
                // Set by default minor version change (because the most used).
                possibleVersions[1]);

        if (newVersion == null) {
            return;
        }

        String lastCommit;
        try {
            lastCommit = lastCommit(project.getBasePath());
        } catch (IOException ex) {
            throw new ACLException("Can't extract last commit message from " + project.getBasePath());
        }

        String[] frequent;
        try {
            frequent = (String[]) ArrayUtils.addAll(new String[]{""}, findFrequent(changelogPath));
        } catch (IOException e) {
            throw new ACLException("Can't find frequent from " + changelogPath);
        }

        String description = input("ACL Dialog", "Add description if you want:", frequent, frequent[0]);
        if (description == null) {
            return;
        }

        String data = "Version: " + newVersion + "\n" + lastCommit + "\n" + (description.length() == 0 ? "\n" : ("Description: " + description + "\n\n\n"));
        try {
            appendTop(changelogPath, data);
        } catch (IOException ex) {
            throw new ACLException("Can't write log to " + changelogPath);
        }

        saveLastInsertedData(project, data);

        notification("ACL Info", "Log with version \"" + newVersion + "\" added to " + CHANGELOG_FILE_NAME, project, INFORMATION, List.of(OPEN_FILE));

        final int answer = yesno("ACL Addition", "Do you want to commit " + CHANGELOG_FILE_NAME + " & squash?");

        if (answer == 1) {
            return;
        }

        String lastCommitMessage = commitMessage(lastCommit);
        try {
            commitAndSquash(project.getBasePath(), changelogPath, lastCommitMessage);
        } catch (IOException e) {
            throw new ACLException("Can't commit & squash in " + project.getBasePath());
        }

        notification("ACL Info", "Committed and squashed", project, INFORMATION);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        try {
            _actionPerformed(project);
        } catch (ACLException ex) {
            notification("ACL Error", ex.text, project, ERROR, ex.actions);
        }
    }

    @Override
    public boolean isDumbAware() {
        return super.isDumbAware();
    }
}
