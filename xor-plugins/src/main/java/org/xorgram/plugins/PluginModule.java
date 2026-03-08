package org.xorgram.plugins;

import android.content.Context;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;
import org.xorgram.core.XORModule;
import org.xorgram.core.XORSetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Plugin Module for XORGram
 * 
 * Features:
 * - Plugin loading and management
 * - Python runtime support
 * - JavaScript runtime support
 * - Plugin sandboxing
 * - Plugin marketplace
 */
public class PluginModule implements XORModule {

    private static final String MODULE_ID = "xor-plugins";
    private static final String MODULE_NAME = "Plugins";
    private static final String MODULE_VERSION = "1.0.0";

    private Context context;
    private XORConfig config;
    private boolean enabled = true;

    // Sub-components
    private PluginManager pluginManager;
    private PythonRuntime pythonRuntime;
    private JavaScriptRuntime jsRuntime;
    private PluginSandbox sandbox;
    private PluginMarketplace marketplace;

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public String getVersion() {
    	return MODULE_VERSION;
    }
   
    @Override
    public String getDescription() {
    	return "Plugin system with Python and JavaScript runtime, sandboxing, and marketplace support";
    }
   
    @Override
    public Set<XOREvent> getSubscribedEvents() {
        Set<XOREvent> events = new HashSet<>();
        events.add(XOREvent.PLUGIN_LOADED);
        events.add(XOREvent.PLUGIN_UNLOADED);
        events.add(XOREvent.PLUGIN_COMMAND);
        events.add(XOREvent.MESSAGE_RECEIVED);
        events.add(XOREvent.MESSAGE_SENT);
        return events;
    }

    @Override
    public void onInit(Context ctx, XORConfig cfg) {
        this.context = ctx;
        this.config = cfg;
        
        // Initialize sub-components
        pluginManager = new PluginManager(context, config);
        pythonRuntime = new PythonRuntime(context, config);
        jsRuntime = new JavaScriptRuntime(context, config);
        sandbox = new PluginSandbox(context, config);
        marketplace = new PluginMarketplace(context, config);
        
        // Load enabled state
        enabled = config.getBoolean(MODULE_ID, "enabled", true);
        
        // Load installed plugins
        pluginManager.loadInstalledPlugins();
    }

    @Override
    public boolean onEvent(XOREvent event, Object... args) {
        if (!enabled) return false;

        switch (event) {
            case PLUGIN_LOADED:
                return handlePluginLoaded(args);
            case PLUGIN_UNLOADED:
                return handlePluginUnloaded(args);
            case PLUGIN_COMMAND:
                return handlePluginCommand(args);
            case MESSAGE_RECEIVED:
            case MESSAGE_SENT:
                return dispatchToPlugins(event, args);
            default:
                return false;
        }
    }

    @Override
    public List<XORSetting> getSettings() {
        List<XORSetting> settings = new ArrayList<>();
        
        settings.add(new XORSetting(
            MODULE_ID,
            "enabled",
            "Enable Plugins",
            "Enable or disable the plugin system",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "python_enabled",
            "Python Runtime",
            "Enable Python plugin support",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "javascript_enabled",
            "JavaScript Runtime",
            "Enable JavaScript plugin support",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "sandbox_enabled",
            "Plugin Sandbox",
            "Run plugins in sandboxed environment",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "auto_update",
            "Auto-Update Plugins",
            "Automatically update plugins",
            false,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "allow_network",
            "Allow Network Access",
            "Allow plugins to access network",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        return settings;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.setBoolean(MODULE_ID, "enabled", enabled);
    }

    @Override
    public void onDestroy() {
        if (pluginManager != null) {
            pluginManager.unloadAllPlugins();
        }
        if (pythonRuntime != null) {
            pythonRuntime.shutdown();
        }
        if (jsRuntime != null) {
            jsRuntime.shutdown();
        }
    }

    private boolean handlePluginLoaded(Object... args) {
        // Handle plugin loaded event
        return false;
    }

    private boolean handlePluginUnloaded(Object... args) {
        // Handle plugin unloaded event
        return false;
    }

    private boolean handlePluginCommand(Object... args) {
        // Handle plugin command
        return false;
    }

    private boolean dispatchToPlugins(XOREvent event, Object... args) {
        // Dispatch event to all loaded plugins
        return pluginManager.dispatchEvent(event, args);
    }

    // Public API methods

    /**
     * Get the plugin manager
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Get Python runtime
     */
    public PythonRuntime getPythonRuntime() {
        return pythonRuntime;
    }

    /**
     * Get JavaScript runtime
     */
    public JavaScriptRuntime getJSRuntime() {
        return jsRuntime;
    }

    /**
     * Get plugin marketplace
     */
    public PluginMarketplace getMarketplace() {
        return marketplace;
    }

    /**
     * Install a plugin from file
     */
    public boolean installPlugin(String path) {
        return pluginManager.installPlugin(path);
    }

    /**
     * Uninstall a plugin
     */
    public boolean uninstallPlugin(String pluginId) {
        return pluginManager.uninstallPlugin(pluginId);
    }

    /**
     * Execute a plugin script
     */
    public Object executeScript(String pluginId, String script, Object... args) {
        Plugin plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null) return null;
        
        switch (plugin.getType()) {
            case PYTHON:
                return pythonRuntime.execute(plugin, script, args);
            case JAVASCRIPT:
                return jsRuntime.execute(plugin, script, args);
            default:
                return null;
        }
    }
}
