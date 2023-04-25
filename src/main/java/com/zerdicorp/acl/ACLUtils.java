package com.zerdicorp.acl;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.zerdicorp.acl.ACLActivity.CHANGELOG_FILE_NAME;
import static com.zerdicorp.acl.ACLStateService.*;

public interface ACLUtils {
    AnAction OPEN_FILE = new NotificationAction("Check it out") {
        @Override
        public boolean isDumbAware() {
            return super.isDumbAware();
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
            String changelogPath = getChangelogPath(e.getProject());
            VirtualFile file = VfsUtil.findFile(Paths.get(changelogPath), true);
            if (file == null) {
                error("Can't open " + CHANGELOG_FILE_NAME + "..");
                return;
            }

            String lastInsertedData = getLastInsertedData(e.getProject());
            if (lastInsertedData == null) {
                return;
            }

            OpenFileDescriptor desc = new OpenFileDescriptor(e.getProject(), file, 0, 0);
            desc.navigate(true);

            Editor editor = FileEditorManager.getInstance(e.getProject()).getSelectedTextEditor();
            int startOffset = editor.getCaretModel().getOffset();
            int endOffset = startOffset + lastInsertedData.length();

            for (RangeHighlighter highlighter : editor.getMarkupModel().getAllHighlighters()) {
                highlighter.dispose();
            }

            editor.getMarkupModel().addRangeHighlighter(
                    startOffset,
                    endOffset,
                    HighlighterLayer.SELECTION,
                    new TextAttributes(
                            null,
                            new JBColor(
                                    new Color(126, 234, 122, 50),
                                    new Color(126, 234, 122, 50)
                            ),
                            null,
                            null,
                            Font.PLAIN
                    ),
                    HighlighterTargetArea.EXACT_RANGE
            );
        }
    };

    AnAction CHOOSE_ANOTHER_ACTION = new NotificationAction("Choose another") {
        @Override
        public boolean isDumbAware() {
            return super.isDumbAware();
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
            String newChangelogPath = chooseFile(e.getProject());
            if (newChangelogPath != null) {
                saveChangelogPath(e.getProject(), newChangelogPath);
                changelogFileFoundNotification(e.getProject(), newChangelogPath);
            }
        }
    };

    static void changelogFileFoundNotification(Project project, String changelogPath) {
        notification(
                "ACL Welcome",
                "Changelog file found:<br><br><i style='font-weight: bold;'>" + changelogPath +
                        "</i><br><br>Ready to work!",
                project,
                INFORMATION,
                List.of(CHOOSE_ANOTHER_ACTION)
        );
    }

    static String shortPath(String path, String projectPath) {
        String[] components = path.replace(projectPath, "").split("/");
        int len = components.length;
        if (len > 3) {
            return components[0] + "/.../" + components[len - 2] + "/" + components[len - 1];
        }
        return String.join("/", components);
    }

    static void notification(
            String title,
            String text,
            Project project,
            NotificationType notificationType
    ) {
        notification(title, text, project, notificationType, List.of());
    }

    static void notification(
            String title,
            String text,
            Project project,
            NotificationType notificationType,
            Collection<? extends AnAction> actions
    ) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification n = NotificationGroupManager.getInstance()
                    .getNotificationGroup("ACL Notification Group")
                    .createNotification(
                            title,
                            text,
                            notificationType
                    );
            n.addActions(actions);
            n.notify(project);
        });
    }

    static String chooseFile(Project project) {
        VirtualFile[] files = FileChooserFactory.getInstance()
                .createFileChooser(FileChooserDescriptorFactory.createSingleFileDescriptor(), project, null)
                .choose(project);

        if (files.length == 0) {
            return null;
        }

        return files[0].getPath();
    }

    static void info(String text) {
        Messages.showInfoMessage(text, "ACL Info");
    }

    static void error(String text) {
        Messages.showErrorDialog(text, "ACL Error");
    }

    static String textarea(Project project, String title, String text) {
        return Messages.showMultilineInputDialog(
                project,
                text,
                title,
                "",
                Messages.getQuestionIcon(),
                null
        );
    }

    static String input(String title, String text, String[] values, String initialValue) {
        return Messages.showEditableChooseDialog(
                text,
                title,
                Messages.getQuestionIcon(),
                values,
                initialValue,
                null
        );
    }

    static String select(String title, String text, String[] values, String initialValue) {
        return select(title, text, values, initialValue, Messages.getQuestionIcon());
    }

    static String select(String title, String text, String[] values, String initialValue, Icon icon) {
        return Messages.showEditableChooseDialog(
                text,
                title,
                icon,
                values,
                initialValue,
                null
        );
    }

    static int yesno(String title, String text) {
        return Messages.showYesNoDialog(text, title, Messages.getQuestionIcon());
    }

    private static String[] parseFoundNormalAndGetPossibleVersions(String foundNormalVersion) {
        int major;
        int minor;
        int patch;
        try {
            final String[] versionParts = foundNormalVersion.split("\\.");

            major = Integer.parseInt(versionParts[0]);
            minor = Integer.parseInt(versionParts[1]);
            patch = Integer.parseInt(versionParts[2]);
        } catch (Exception ex) {
            return null;
        }

        return new String[]{
                (major + 1) + ".0.0",
                major + "." + (minor + 1) + ".0",
                major + "." + minor + "." + (patch + 1)
        };
    }

    private static String[] parseFoundXXAndGetPossibleVersions(String foundVersion, String prefix) {
        String version;
        int n;
        try {
            final String[] versionAndXX = foundVersion.split("-" + prefix + "\\.");
            version = versionAndXX[0];
            n = Integer.parseInt(versionAndXX[1]);
        } catch (Exception ex) {
            return null;
        }

        return new String[]{
                version + "-" + prefix + "." + (n + 1)
        };
    }

    static String[] parseFoundAndGetPossibleVersions(String foundVersion) {
        final String[] normal = parseFoundNormalAndGetPossibleVersions(foundVersion);
        if (normal != null) {
            return normal;
        }

        final String[] xxs = new String[]{"RC", "HF"};
        for (int i = 0; i < xxs.length; i++) {
            final String[] possible = parseFoundXXAndGetPossibleVersions(foundVersion, xxs[i]);
            if (possible != null) {
                return possible;
            }
        }

        return null;
    }

    static String getLastVersion(String pathToChangelog) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(pathToChangelog));
        String line;
        // by default, it's having invalid version
        String version = "_._._";
        while ((line = br.readLine()) != null) {
            if (line.contains("Version")) {
                version = line.split(" ")[1];
                break;
            }
        }
        return version;
    }

    static void appendTop(String filePath, String data) throws IOException {
        File file = new File(filePath);
        LineIterator li = FileUtils.lineIterator(file);
        File tempFile = File.createTempFile("prependPrefix", ".tmp");
        BufferedWriter w = new BufferedWriter(new FileWriter(tempFile));
        try {
            w.write(data);
            while (li.hasNext()) {
                w.write(li.next());
                w.write("\n");
            }
        } finally {
            IOUtils.closeQuietly(w);
            IOUtils.closeQuietly(li);
        }
        FileUtils.deleteQuietly(file);
        FileUtils.moveFile(tempFile, file);
    }

    static List<Path> findFile(String path, String targetFileName) {
        try (Stream<Path> stream = Files.walk(Paths.get(path))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p
                            .getFileName()
                            .toString()
                            .equals(targetFileName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static List<String> intersection(String[] arr1, String[] arr2) {
        int length = arr1.length;
        if (arr2.length < arr1.length) {
            length = arr2.length;
        }

        final List<String> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            if (!arr1[i].equalsIgnoreCase(arr2[i])) {
                return result;
            }
            result.add(arr1[i]);
        }

        return result;
    }

    static Map<String, Integer> sortByKeyLengthAndValue(Map<String, Integer> hm) {
        final List<Map.Entry<String, Integer>> list = new LinkedList<>(hm.entrySet());
        list.sort(Comparator.comparingInt(c -> c.getValue() * c.getKey().length()));
        Collections.reverse(list);

        final Map<String, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    String fileExtR =
            "\\.(doc|docx|log|odt|pages|rtf|txt|csv|key|pps|ppt|pptx|tar|xml|json|pdf|xls|xlsx|db|sql|rar|gz|zip)";
    String plainR = "[^a-zA-Zа-яА-Я0-9 ]";

    static String prepare(String s) {
        return s
                .replaceAll(fileExtR, "")
                .replaceAll(plainR, "");
    }

    static String[] findFrequent(String pathToChangelog) throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(pathToChangelog));
        final List<String> descriptions = new ArrayList<>();

        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains("Description")) {
                descriptions.add(line.split("Description:")[1].trim());
            }
        }

        final Map<String, Integer> frequent = new HashMap<>();

        for (int i = 0; i < descriptions.size(); i++) {
            final String preparedMain = prepare(descriptions.get(i));
            final String[] main = preparedMain.split(" ");
            final Map<String, String> origin = new HashMap<>();
            final Map<String, Integer> top = new HashMap<>();

            for (int j = i + 1; j < descriptions.size(); j++) {
                final String preparedWard = prepare(descriptions.get(j));
                final String[] ward = preparedWard.split(" ");

                final List<String> ion = intersection(main, ward);
                if (ion.size() == 0) {
                    continue;
                }

                final String candidate = String.join(" ", ion);
                final String skinnyCandidate = candidate.toLowerCase();

                final Integer count = top.get(skinnyCandidate);
                if (count != null) {
                    top.put(skinnyCandidate, count + 1);
                } else {
                    top.put(skinnyCandidate, 0);
                }

                origin.putIfAbsent(skinnyCandidate, candidate);
            }

            if (top.size() == 0) {
                continue;
            }

            for (Map.Entry<String, Integer> bestOf : top.entrySet()) {
                final String originBestOf = origin.get(bestOf.getKey());

                final Integer count = frequent.get(originBestOf);
                if (count != null) {
                    if (bestOf.getValue() > count) {
                        frequent.put(originBestOf, bestOf.getValue());
                    }
                } else {
                    frequent.put(originBestOf, bestOf.getValue());
                }
            }
        }

        return sortByKeyLengthAndValue(frequent).keySet().toArray(new String[0]);
    }
}
