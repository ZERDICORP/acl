package com.zerdicorp.acl;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.intellij.notification.NotificationType.ERROR;
import static com.intellij.notification.NotificationType.INFORMATION;
import static com.zerdicorp.acl.ACLActivity.*;
import static com.zerdicorp.acl.ACLLogBuilder.build;
import static com.zerdicorp.acl.ACLStateService.*;
import static com.zerdicorp.acl.ACLUtils.*;

public class ACLAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        // don't need it //
    }

    private String preCheck(Project project, String changelogPath) {
        if (changelogPath == null || !new File(changelogPath).exists()) {
            if (!findChangelog(project)) {
                throw new ACLException("File " + CHANGELOG_FILE_NAME + " not found.. " +
                        "Please create the file and try again", List.of(CHOOSE_ANOTHER_ACTION));
            }
            saveChangelogPath(project, CHANGELOG_FILE_PATH);
            return CHANGELOG_FILE_PATH;
        }
        return changelogPath;
    }

    private void _actionPerformed(Project project) {
        String maybeChangelogPath = getChangelogPath(project);
        String changelogPath = preCheck(project, maybeChangelogPath);

        Icon selectVersionIcon = Messages.getQuestionIcon();
        String selectVersionText = "Select the desired version change:";
        String lastVersion;
        try {
            lastVersion = getLastVersion(changelogPath);
        } catch (IOException ex) {
            throw new ACLException("Can't extract last version from " + changelogPath);
        }

        String[] possibleVersions = parseFoundAndGetPossibleVersions(lastVersion);
        if (possibleVersions == null) {
            possibleVersions = new String[]{lastVersion};
            selectVersionIcon = Messages.getWarningIcon();
            selectVersionText = "I don't know how to parse the latest version from the changelog. " +
                    "Please change it yourself:";

        }

        String defaultVersion = possibleVersions[0];
        if (possibleVersions.length > 2) {
            defaultVersion = possibleVersions[1];
        }

        String newVersion = select(
                "ACL Dialog",
                selectVersionText,
                possibleVersions,
                // Set by default minor version change (because the most used)..
                defaultVersion,
                selectVersionIcon
        );

        if (newVersion == null) {
            return;
        }

        String message = textarea(project, "ACL Dialog", "Add log message:");
        if (message == null) {
            return;
        }

        String logMessage;
        try {
            logMessage = build(newVersion, message);
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

        String data = logMessage + "\n" + (description.length() == 0 ? "\n" : ("Description: " + description + "\n\n\n"));
        try {
            appendTop(changelogPath, data);
        } catch (IOException ex) {
            throw new ACLException("Can't write log to " + changelogPath);
        }

        saveLastInsertedData(project, data);
        notification(
                "ACL Info",
                "Log with version \"" + newVersion + "\" added to " + CHANGELOG_FILE_NAME,
                project,
                INFORMATION,
                List.of(OPEN_FILE)
        );
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
