package org.xorgram.plugins;

import org.xorgram.core.XOREvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Plugin definition
 * 
 * Represents a loaded plugin with its metadata and state
 */
public class Plugin {

    private final String id;
    private final String name;
    private final String version;
    private final Type type;
    private String path;
    private String description;
    private String author;
    private State state;
    private boolean enabled;
    private final List<String> dependencies;
    private final Set<XOREvent> subscribedEvents;
    private final List<String> permissions;

    public Plugin(String id, String name, String version, Type type) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.type = type;
        this.state = State.UNLOADED;
        this.enabled = true;
        this.dependencies = new ArrayList<>();
        this.subscribedEvents = new HashSet<>();
        this.permissions = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Type getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void addDependency(String pluginId) {
        dependencies.add(pluginId);
    }

    public Set<XOREvent> getSubscribedEvents() {
        return subscribedEvents;
    }

    public void subscribeEvent(XOREvent event) {
        subscribedEvents.add(event);
    }

    public boolean handlesEvent(XOREvent event) {
        return subscribedEvents.contains(event);
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void addPermission(String permission) {
        permissions.add(permission);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * Handle an event
     */
    public boolean onEvent(XOREvent event, Object... args) {
        // This is overridden by actual plugin implementations
        return false;
    }

    /**
     * Plugin types
     */
    public enum Type {
        JAVASCRIPT,
        PYTHON,
        NATIVE
    }

    /**
     * Plugin states
     */
    public enum State {
        UNLOADED,
        LOADING,
        LOADED,
        RUNNING,
        PAUSED,
        ERROR,
        UNLOADING
    }
}
