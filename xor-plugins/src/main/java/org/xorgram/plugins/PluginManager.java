package org.xorgram.plugins;

import android.content.Context;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin Manager for loading and managing plugins
 * 
 * Features:
 * - Plugin loading from files
 * - Plugin lifecycle management
 * - Plugin dependency resolution
 * - Plugin hot-reloading
 */
public class PluginManager {

    private final Context context;
    private final XORConfig config;
    private final Map<String, Plugin> loadedPlugins;
    private final List<PluginLoadListener> listeners;

    public PluginManager(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        this.loadedPlugins = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }

    /**
     * Load all installed plugins
     */
    public void loadInstalledPlugins() {
        File pluginsDir = new File(context.getFilesDir(), "plugins");
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs();
            return;
        }
        
        File[] pluginDirs = pluginsDir.listFiles(File::isDirectory);
        if (pluginDirs == null) return;
        
        for (File pluginDir : pluginDirs) {
            try {
                loadPluginFromDir(pluginDir);
            } catch (Exception e) {
                // Log error but continue loading other plugins
            }
        }
    }

    /**
     * Install a plugin from a file
     */
    public boolean installPlugin(String path) {
        File pluginFile = new File(path);
        if (!pluginFile.exists()) {
            return false;
        }
        
        // TODO: Implement plugin installation
        // 1. Extract plugin archive
        // 2. Validate plugin manifest
        // 3. Check dependencies
        // 4. Copy to plugins directory
        // 5. Load the plugin
        
        return false;
    }

    /**
     * Uninstall a plugin
     */
    public boolean uninstallPlugin(String pluginId) {
        Plugin plugin = loadedPlugins.get(pluginId);
        if (plugin == null) {
            return false;
        }
        
        // Unload first
        unloadPlugin(pluginId);
        
        // Delete plugin directory
        File pluginDir = new File(context.getFilesDir(), "plugins/" + pluginId);
        if (pluginDir.exists()) {
            deleteRecursive(pluginDir);
        }
        
        return true;
    }

    /**
     * Load a plugin from its directory
     */
    private void loadPluginFromDir(File pluginDir) {
        // Look for manifest.json
        File manifestFile = new File(pluginDir, "manifest.json");
        if (!manifestFile.exists()) {
            return;
        }
        
        // Parse manifest
        // TODO: Implement manifest parsing
        
        // Create plugin instance
        Plugin plugin = new Plugin(
            pluginDir.getName(),
            pluginDir.getName(),
            "1.0.0",
            Plugin.Type.JAVASCRIPT
        );
        plugin.setPath(pluginDir.getAbsolutePath());
        
        // Load the plugin
        loadPlugin(plugin);
    }

    /**
     * Load a plugin
     */
    public void loadPlugin(Plugin plugin) {
        if (loadedPlugins.containsKey(plugin.getId())) {
            return;
        }
        
        // Check dependencies
        if (!checkDependencies(plugin)) {
            return;
        }
        
        // Initialize plugin
        plugin.setState(Plugin.State.LOADING);
        
        try {
            // Execute plugin initialization script
            // TODO: Run plugin init script
            
            plugin.setState(Plugin.State.LOADED);
            loadedPlugins.put(plugin.getId(), plugin);
            
            // Notify listeners
            notifyPluginLoaded(plugin);
        } catch (Exception e) {
            plugin.setState(Plugin.State.ERROR);
        }
    }

    /**
     * Unload a plugin
     */
    public void unloadPlugin(String pluginId) {
        Plugin plugin = loadedPlugins.remove(pluginId);
        if (plugin == null) {
            return;
        }
        
        plugin.setState(Plugin.State.UNLOADING);
        
        try {
            // Execute plugin cleanup script
            // TODO: Run plugin cleanup script
            
            plugin.setState(Plugin.State.UNLOADED);
            notifyPluginUnloaded(plugin);
        } catch (Exception e) {
            plugin.setState(Plugin.State.ERROR);
        }
    }

    /**
     * Unload all plugins
     */
    public void unloadAllPlugins() {
        for (String pluginId : new ArrayList<>(loadedPlugins.keySet())) {
            unloadPlugin(pluginId);
        }
    }

    /**
     * Get a loaded plugin
     */
    public Plugin getPlugin(String pluginId) {
        return loadedPlugins.get(pluginId);
    }

    /**
     * Get all loaded plugins
     */
    public List<Plugin> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins.values());
    }

    /**
     * Check if a plugin's dependencies are satisfied
     */
    private boolean checkDependencies(Plugin plugin) {
        for (String dependency : plugin.getDependencies()) {
            if (!loadedPlugins.containsKey(dependency)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Dispatch an event to all plugins
     */
    public boolean dispatchEvent(XOREvent event, Object... args) {
        boolean handled = false;
        
        for (Plugin plugin : loadedPlugins.values()) {
            if (plugin.isEnabled() && plugin.handlesEvent(event)) {
                try {
                    if (plugin.onEvent(event, args)) {
                        handled = true;
                    }
                } catch (Exception e) {
                    // Log error but continue
                }
            }
        }
        
        return handled;
    }

    /**
     * Add a plugin load listener
     */
    public void addListener(PluginLoadListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a plugin load listener
     */
    public void removeListener(PluginLoadListener listener) {
        listeners.remove(listener);
    }

    private void notifyPluginLoaded(Plugin plugin) {
        for (PluginLoadListener listener : listeners) {
            listener.onPluginLoaded(plugin);
        }
    }

    private void notifyPluginUnloaded(Plugin plugin) {
        for (PluginLoadListener listener : listeners) {
            listener.onPluginUnloaded(plugin);
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    /**
     * Plugin load listener interface
     */
    public interface PluginLoadListener {
        void onPluginLoaded(Plugin plugin);
        void onPluginUnloaded(Plugin plugin);
    }
}
