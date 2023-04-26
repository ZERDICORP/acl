package com.zerdicorp.acl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(name = "ACLState", storages = {@Storage("ACLState.xml")})
public class ACLStateService implements PersistentStateComponent<ACLStateService.ACLState> {
    static class ACLStateItem {
        public String changelog;
        public String lastInsertedData;

        public ACLStateItem() {
            changelog = "";
            lastInsertedData = "";
        }

        public ACLStateItem(String changelog) {
            this.changelog = changelog;
        }
    }

    static class ACLState {
        public Map<String, ACLStateItem> map = new HashMap<>();
    }

    private ACLState ACLState = new ACLState();

    @Nullable
    @Override
    public ACLStateService.ACLState getState() {
        return ACLState;
    }

    @Override
    public void loadState(@NotNull ACLStateService.ACLState ACLState) {
        this.ACLState = ACLState;
    }

    public static String getLastInsertedData(Project project) {
        ACLStateService service = project.getService(ACLStateService.class);
        ACLStateService.ACLState state = service.getState();
        ACLStateService.ACLStateItem current = state.map.get(project.getBasePath());
        if (current == null) {
            return null;
        }
        return current.lastInsertedData;
    }

    public static void saveLastInsertedData(Project project, String lastInsertedData) {
        ACLStateService service = project.getService(ACLStateService.class);
        ACLStateService.ACLState state = service.getState();
        ACLStateService.ACLStateItem current = state.map.get(project.getBasePath());
        current.lastInsertedData = lastInsertedData;
        state.map.put(project.getBasePath(), current);
    }

    public static void saveChangelogPath(Project project, String changelogPath) {
        ACLStateService service = project.getService(ACLStateService.class);
        ACLStateService.ACLState state = service.getState();
        ACLStateService.ACLStateItem current = state.map.get(project.getBasePath());
        if (current == null) {
            state.map.put(project.getBasePath(), new ACLStateItem(changelogPath));
            return;
        }
        if (changelogPath == null || current.changelog == null || !current.changelog.equals(changelogPath)) {
            current.changelog = changelogPath;
            state.map.put(project.getBasePath(), current);
        }
    }

    public static String getChangelogPath(Project project) {
        ACLStateService service = project.getService(ACLStateService.class);
        ACLStateService.ACLState state = service.getState();
        ACLStateService.ACLStateItem current = state.map.get(project.getBasePath());
        if (current == null) {
            return null;
        }
        return current.changelog;
    }
}
