package com.zerdicorp.acl;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.zerdicorp.acl.ACLActivity.CHANGELOG_FILE_NAME;
import static com.zerdicorp.acl.ACLActivity.CHANGELOG_FILE_PATH;
import static com.zerdicorp.acl.ACLUtils.*;

public class ACLAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    private void _actionPerformed(Project project) {
        String lastVersion;
        try {
            lastVersion = getLastVersion(CHANGELOG_FILE_PATH);
        } catch (IOException ex) {
            throw new ACLException("Can't extract last version from " + CHANGELOG_FILE_PATH);
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

        final String[] possibleVersions = new String[]{
                (major + 1) + ".0.0",
                major + "." + (minor + 1) + ".0",
                major + "." + minor + "." + (patch + 1)
        };

        String newVersion = select(
                "ACL Dialog",
                "Select the desired version change:",
                possibleVersions,
                // Set by default minor version change (because the most used).
                possibleVersions[1]
        );

        if (newVersion == null) {
            return;
        }

        String lastCommit;
        try {
            lastCommit = lastCommit(project.getBasePath());
        } catch (IOException ex) {
            throw new ACLException(
                    "Can't extract last commit message from " + project.getBasePath()
            );
        }

        String[] frequent;
        try {
            frequent = (String[]) ArrayUtils.addAll(
                    new String[]{""},
                    findFrequent(CHANGELOG_FILE_PATH)
            );
        } catch (IOException e) {
            throw new ACLException("Can't find frequent from " + CHANGELOG_FILE_PATH);
        }

        String description = input(
                "ACL Dialog",
                "Add description if you want:",
                frequent,
                frequent[0]
        );
        if (description == null) {
            return;
        }

        try {
            appendTop(
                    CHANGELOG_FILE_PATH,
                    "Version: " + newVersion + "\n" + lastCommit + "\n" + (
                            description.length() == 0 ? "\n" : ("Description: " + description + "\n\n\n")
                    )
            );
        } catch (IOException ex) {
            throw new ACLException("Can't write log to " + CHANGELOG_FILE_PATH);
        }

        notification(
                "ACL Info",
                "Log with version \"" + newVersion + "\" added to " + CHANGELOG_FILE_NAME,
                project,
                INFORMATION
        );

        final int answer = yesno(
                "ACL Addition",
                "Do you want to commit " + CHANGELOG_FILE_NAME + " & squash?"
        );

        if (answer == 1) {
            return;
        }

        String lastCommitMessage = commitMessage(lastCommit);
        try {
            commitAndSquash(
                    project.getBasePath(),
                    CHANGELOG_FILE_PATH,
                    lastCommitMessage
            );
        } catch (IOException e) {
            throw new ACLException("Can't commit & squash in " + project.getBasePath());
        }

        notification(
                "ACL Info",
                "Committed and squashed",
                project,
                INFORMATION
        );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        try {
            _actionPerformed(project);
        } catch (ACLException ex) {
            error(ex.text);
        }
    }

    @Override
    public boolean isDumbAware() {
        return super.isDumbAware();
    }
}
