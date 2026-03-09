package org.xorgram.core;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * XORModuleManager - Central manager for all XORGram modules.
 * 
 * Responsibilities:
 * - Module loading and initialization
 * - Event dispatching to modules
 * - Module lifecycle management
 * - Dependency resolution
 * - State persistence
 * 
 * This is the core component that connects XORBridge to all modules.
 */
public class XORModuleManager {
    
    private static final String TAG = "XORModuleManager";
    
    // ═══════════════════════════════════════════════════════════════
    // Singleton Instance
    // ═══════════════════════════════════════════════════════════════
    
    private static volatile XORModuleManager instance;
    private static final Object lock = new Object();
    
    /**
     * Get the global XORModuleManager instance.
     * 
     * @param context Application context
     * @return XORModuleManager instance
     */
    public static XORModuleManager getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new XORModuleManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Get the instance if already initialized.
     * 
     * @return XORModuleManager instance or null
     */
    @Nullable
    public static XORModuleManager getInstance() {
        return instance;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Internal State
    // ═══════════════════════════════════════════════════════════════
    
    private final Context context;
    private final XORConfig config;
    private final XOREventBus eventBus;
    private final Map<String, XORModule> modules;
    private final Map<XOREvent, List<XORModule>> eventSubscriptions;
    private final Map<String, ModuleState> moduleStates;
    private final List<ModuleLifecycleListener> lifecycleListeners;
    private boolean initialized = false;
    
    private XORModuleManager(Context context) {
        this.context = context;
        this.modules = new ConcurrentHashMap<>();
        this.eventSubscriptions = new EnumMap<>(XOREvent.class);
        this.moduleStates = new ConcurrentHashMap<>();
        this.lifecycleListeners = new ArrayList<>();

        // Initialize config and event bus - errors are caught at getInstance level
        this.config = XORConfig.getInstance(context);
        this.eventBus = XOREventBus.getInstance();
    }

    // ═══════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initialize the module manager and load all modules.
     */
    public void init() {
        if (initialized) {
            Log.w(TAG, "XORModuleManager already initialized");
            return;
        }

        Log.i(TAG, "Initializing XORModuleManager...");

        try {
            // Load built-in modules
            loadBuiltinModules();

            // Resolve dependencies
            resolveDependencies();

            // Initialize modules in dependency order
            initializeModules();

            initialized = true;
            Log.i(TAG, "XORModuleManager initialized with " + modules.size() + " modules");
        } catch (Throwable t) {
            Log.e(TAG, "Error during module initialization", t);
            // Mark as initialized anyway to prevent loops
            initialized = true;
        }
    }
    
    /**
     * Load built-in modules.
     */
    private void loadBuiltinModules() {
        // Modules are registered here
        // In production, this could use reflection or a plugin system
        // For now, we'll register modules manually
        
        // Note: Actual module classes will be added when modules are created
        // This is a placeholder for the registration pattern
        
        Log.d(TAG, "Loading built-in modules...");
        
        // Example registration (will be uncommented when modules are created):
        // registerModule(new GhostModule());
        // registerModule(new VaultModule());
        // etc.
    }
    
    /**
     * Register a module.
     * 
     * @param module The module to register
     */
    public void registerModule(@NonNull XORModule module) {
        String id = module.getId();
        if (modules.containsKey(id)) {
            Log.w(TAG, "Module already registered: " + id);
            return;
        }
        
        modules.put(id, module);
        moduleStates.put(id, ModuleState.REGISTERED);
        
        // Subscribe to events
        Set<XOREvent> events = module.getSubscribedEvents();
        for (XOREvent event : events) {
            eventSubscriptions.computeIfAbsent(event, k -> new ArrayList<>()).add(module);
        }
        
        Log.d(TAG, "Registered module: " + id + " (subscribed to " + events.size() + " events)");
    }
    
    /**
     * Unregister a module.
     * 
     * @param moduleId The module ID to unregister
     */
    public void unregisterModule(@NonNull String moduleId) {
        XORModule module = modules.remove(moduleId);
        if (module == null) {
            Log.w(TAG, "Module not found: " + moduleId);
            return;
        }
        
        // Unsubscribe from events
        Set<XOREvent> events = module.getSubscribedEvents();
        for (XOREvent event : events) {
            List<XORModule> subscribers = eventSubscriptions.get(event);
            if (subscribers != null) {
                subscribers.remove(module);
            }
        }
        
        // Destroy module
        module.onDestroy();
        moduleStates.remove(moduleId);
        
        Log.d(TAG, "Unregistered module: " + moduleId);
    }
    
    /**
     * Resolve dependencies between modules.
     */
    private void resolveDependencies() {
        Log.d(TAG, "Resolving module dependencies...");
        
        for (XORModule module : modules.values()) {
            List<String> dependencies = module.getDependencies();
            for (String depId : dependencies) {
                XORModule dep = modules.get(depId);
                if (dep == null) {
                    Log.w(TAG, "Missing dependency: " + depId + " for module: " + module.getId());
                    moduleStates.put(module.getId(), ModuleState.MISSING_DEPENDENCY);
                    continue;
                }
                
                String minVersion = module.getMinimumVersion(depId);
                if (minVersion != null && !isVersionCompatible(dep.getVersion(), minVersion)) {
                    Log.w(TAG, "Incompatible dependency version: " + depId + 
                            " (required: " + minVersion + ", actual: " + dep.getVersion() + ")");
                    moduleStates.put(module.getId(), ModuleState.INCOMPATIBLE_DEPENDENCY);
                }
            }
        }
    }
    
    /**
     * Initialize all modules in dependency order.
     */
    private void initializeModules() {
        Log.d(TAG, "Initializing modules...");
        
        // Sort modules by dependencies
        List<XORModule> sortedModules = topologicalSort();
        
        for (XORModule module : sortedModules) {
            ModuleState state = moduleStates.get(module.getId());
            if (state != ModuleState.REGISTERED) {
                Log.w(TAG, "Skipping initialization for module: " + module.getId() + 
                        " (state: " + state + ")");
                continue;
            }
            
            try {
                // Check if module is enabled in config
                boolean enabled = config.getBoolean("module_" + module.getId().replace("xor-", ""), true);
                
                XORConfig moduleConfig = XORConfig.getNamespaced(context, module.getId());
                module.onInit(context, moduleConfig);
                
                if (enabled && module.isEnabled()) {
                    module.onEnable();
                    moduleStates.put(module.getId(), ModuleState.ENABLED);
                    Log.i(TAG, "Module enabled: " + module.getId());
                } else {
                    moduleStates.put(module.getId(), ModuleState.DISABLED);
                    Log.d(TAG, "Module disabled: " + module.getId());
                }
                
                // Notify listeners
                notifyModuleInitialized(module);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize module: " + module.getId(), e);
                moduleStates.put(module.getId(), ModuleState.ERROR);
            }
        }
    }
    
    /**
     * Topological sort of modules based on dependencies.
     */
    private List<XORModule> topologicalSort() {
        List<XORModule> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (XORModule module : modules.values()) {
            visit(module, visited, visiting, result);
        }
        
        return result;
    }
    
    private void visit(XORModule module, Set<String> visited, Set<String> visiting, List<XORModule> result) {
        String id = module.getId();
        
        if (visited.contains(id)) return;
        if (visiting.contains(id)) {
            Log.w(TAG, "Circular dependency detected: " + id);
            return;
        }
        
        visiting.add(id);
        
        for (String depId : module.getDependencies()) {
            XORModule dep = modules.get(depId);
            if (dep != null) {
                visit(dep, visited, visiting, result);
            }
        }
        
        visiting.remove(id);
        visited.add(id);
        result.add(module);
    }
    
    /**
     * Check if a version is compatible with minimum required version.
     */
    private boolean isVersionCompatible(String actual, String minimum) {
        // Simple version comparison (major.minor.patch)
        String[] actualParts = actual.split("\\.");
        String[] minParts = minimum.split("\\.");
        
        for (int i = 0; i < Math.max(actualParts.length, minParts.length); i++) {
            int a = i < actualParts.length ? Integer.parseInt(actualParts[i]) : 0;
            int m = i < minParts.length ? Integer.parseInt(minParts[i]) : 0;
            
            if (a > m) return true;
            if (a < m) return false;
        }
        
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Event Dispatching
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatch an event to all subscribed modules.
     * 
     * @param event The event to dispatch
     * @param args Event arguments
     * @return true if any module blocked the action
     */
    public boolean dispatch(XOREvent event, Object... args) {
        List<XORModule> subscribers = eventSubscriptions.get(event);
        if (subscribers == null || subscribers.isEmpty()) {
            return false;
        }
        
        // Sort by priority (higher priority first)
        List<XORModule> sortedSubscribers = new ArrayList<>(subscribers);
        Collections.sort(sortedSubscribers, Comparator.comparingInt(XORModule::getPriority).reversed());
        
        boolean blocked = false;
        for (XORModule module : sortedSubscribers) {
            ModuleState state = moduleStates.get(module.getId());
            if (state != ModuleState.ENABLED) {
                continue;
            }
            
            try {
                if (module.onEvent(event, args)) {
                    blocked = true;
                    Log.d(TAG, "Event " + event + " blocked by module: " + module.getId());
                    // Continue dispatching to allow all modules to process the event
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in module " + module.getId() + " handling event " + event, e);
            }
        }
        
        return blocked;
    }
    
    /**
     * Dispatch an event and return a result.
     * 
     * @param event The event to dispatch
     * @param args Event arguments
     * @return The result from the first module that returns non-null
     */
    @Nullable
    public Object dispatchWithResult(XOREvent event, Object... args) {
        List<XORModule> subscribers = eventSubscriptions.get(event);
        if (subscribers == null || subscribers.isEmpty()) {
            return null;
        }
        
        List<XORModule> sortedSubscribers = new ArrayList<>(subscribers);
        Collections.sort(sortedSubscribers, Comparator.comparingInt(XORModule::getPriority).reversed());
        
        for (XORModule module : sortedSubscribers) {
            ModuleState state = moduleStates.get(module.getId());
            if (state != ModuleState.ENABLED) {
                continue;
            }
            
            try {
                Object result = module.onEventWithResult(event, args);
                if (result != null) {
                    Log.d(TAG, "Event " + event + " handled by module: " + module.getId());
                    return result;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in module " + module.getId() + " handling event " + event, e);
            }
        }
        
        return null;
    }
    
    /**
     * Dispatch an event and collect results from all modules.
     * 
     * @param event The event to dispatch
     * @param args Event arguments
     * @return List of results from all modules
     */
    @NonNull
    public List<Object> dispatchCollectResults(XOREvent event, Object... args) {
        List<Object> results = new ArrayList<>();
        List<XORModule> subscribers = eventSubscriptions.get(event);
        
        if (subscribers == null || subscribers.isEmpty()) {
            return results;
        }
        
        for (XORModule module : subscribers) {
            ModuleState state = moduleStates.get(module.getId());
            if (state != ModuleState.ENABLED) {
                continue;
            }
            
            try {
                Object result = module.onEventWithResult(event, args);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in module " + module.getId() + " handling event " + event, e);
            }
        }
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Module Management
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Enable a module.
     * 
     * @param moduleId The module ID
     * @return true if successful
     */
    public boolean enableModule(@NonNull String moduleId) {
        XORModule module = modules.get(moduleId);
        if (module == null) {
            Log.w(TAG, "Module not found: " + moduleId);
            return false;
        }
        
        ModuleState state = moduleStates.get(moduleId);
        if (state == ModuleState.ENABLED) {
            return true;
        }
        
        if (state != ModuleState.DISABLED) {
            Log.w(TAG, "Cannot enable module in state: " + state);
            return false;
        }
        
        try {
            module.onEnable();
            module.setEnabled(true);
            moduleStates.put(moduleId, ModuleState.ENABLED);
            
            // Save to config
            config.putBoolean("module_" + moduleId.replace("xor-", ""), true);
            
            notifyModuleEnabled(module);
            Log.i(TAG, "Module enabled: " + moduleId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable module: " + moduleId, e);
            return false;
        }
    }
    
    /**
     * Disable a module.
     * 
     * @param moduleId The module ID
     * @return true if successful
     */
    public boolean disableModule(@NonNull String moduleId) {
        XORModule module = modules.get(moduleId);
        if (module == null) {
            Log.w(TAG, "Module not found: " + moduleId);
            return false;
        }
        
        ModuleState state = moduleStates.get(moduleId);
        if (state == ModuleState.DISABLED) {
            return true;
        }
        
        if (state != ModuleState.ENABLED) {
            Log.w(TAG, "Cannot disable module in state: " + state);
            return false;
        }
        
        try {
            module.onDisable();
            module.setEnabled(false);
            moduleStates.put(moduleId, ModuleState.DISABLED);
            
            // Save to config
            config.putBoolean("module_" + moduleId.replace("xor-", ""), false);
            
            notifyModuleDisabled(module);
            Log.i(TAG, "Module disabled: " + moduleId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable module: " + moduleId, e);
            return false;
        }
    }
    
    /**
     * Get a module by ID.
     * 
     * @param moduleId The module ID
     * @return The module or null
     */
    @Nullable
    public XORModule getModule(@NonNull String moduleId) {
        return modules.get(moduleId);
    }
    
    /**
     * Get all loaded modules.
     * 
     * @return Collection of modules
     */
    @NonNull
    public Collection<XORModule> getLoadedModules() {
        return Collections.unmodifiableCollection(modules.values());
    }
    
    /**
     * Get all enabled modules.
     * 
     * @return List of enabled modules
     */
    @NonNull
    public List<XORModule> getEnabledModules() {
        List<XORModule> enabled = new ArrayList<>();
        for (XORModule module : modules.values()) {
            if (moduleStates.get(module.getId()) == ModuleState.ENABLED) {
                enabled.add(module);
            }
        }
        return enabled;
    }
    
    /**
     * Get module state.
     * 
     * @param moduleId The module ID
     * @return Module state
     */
    @NonNull
    public ModuleState getModuleState(@NonNull String moduleId) {
        return moduleStates.getOrDefault(moduleId, ModuleState.NOT_FOUND);
    }
    
    /**
     * Check if a module is enabled.
     * 
     * @param moduleId The module ID
     * @return true if enabled
     */
    public boolean isModuleEnabled(@NonNull String moduleId) {
        return moduleStates.get(moduleId) == ModuleState.ENABLED;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Called when app goes to background.
     */
    public void onBackground() {
        for (XORModule module : modules.values()) {
            if (moduleStates.get(module.getId()) == ModuleState.ENABLED) {
                try {
                    module.onBackground();
                } catch (Exception e) {
                    Log.e(TAG, "Error in onBackground for module: " + module.getId(), e);
                }
            }
        }
    }
    
    /**
     * Called when app comes to foreground.
     */
    public void onForeground() {
        for (XORModule module : modules.values()) {
            if (moduleStates.get(module.getId()) == ModuleState.ENABLED) {
                try {
                    module.onForeground();
                } catch (Exception e) {
                    Log.e(TAG, "Error in onForeground for module: " + module.getId(), e);
                }
            }
        }
    }
    
    /**
     * Destroy all modules.
     */
    public void destroy() {
        Log.i(TAG, "Destroying XORModuleManager...");
        
        for (XORModule module : modules.values()) {
            try {
                module.onDestroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying module: " + module.getId(), e);
            }
        }
        
        modules.clear();
        eventSubscriptions.clear();
        moduleStates.clear();
        initialized = false;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Listeners
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Add a lifecycle listener.
     * 
     * @param listener The listener to add
     */
    public void addLifecycleListener(@NonNull ModuleLifecycleListener listener) {
        lifecycleListeners.add(listener);
    }
    
    /**
     * Remove a lifecycle listener.
     * 
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(@NonNull ModuleLifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }
    
    private void notifyModuleInitialized(XORModule module) {
        for (ModuleLifecycleListener listener : lifecycleListeners) {
            listener.onModuleInitialized(module);
        }
    }
    
    private void notifyModuleEnabled(XORModule module) {
        for (ModuleLifecycleListener listener : lifecycleListeners) {
            listener.onModuleEnabled(module);
        }
    }
    
    private void notifyModuleDisabled(XORModule module) {
        for (ModuleLifecycleListener listener : lifecycleListeners) {
            listener.onModuleDisabled(module);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Debug
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get debug information.
     * 
     * @return Debug info string
     */
    @NonNull
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("XORModuleManager Debug Info:\n");
        sb.append("  Initialized: ").append(initialized).append("\n");
        sb.append("  Modules: ").append(modules.size()).append("\n");
        
        for (Map.Entry<String, XORModule> entry : modules.entrySet()) {
            String id = entry.getKey();
            XORModule module = entry.getValue();
            ModuleState state = moduleStates.get(id);
            
            sb.append("    ").append(id).append(": ").append(state);
            sb.append(" (v").append(module.getVersion()).append(")\n");
            
            if (module.getDebugInfo() != null) {
                sb.append("      ").append(module.getDebugInfo()).append("\n");
            }
        }
        
        sb.append("  Event Subscriptions:\n");
        for (Map.Entry<XOREvent, List<XORModule>> entry : eventSubscriptions.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(": ");
            sb.append(entry.getValue().size()).append(" subscribers\n");
        }
        
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Enums and Interfaces
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Module state enum.
     */
    public enum ModuleState {
        NOT_FOUND,
        REGISTERED,
        MISSING_DEPENDENCY,
        INCOMPATIBLE_DEPENDENCY,
        DISABLED,
        ENABLED,
        ERROR
    }
    
    /**
     * Module lifecycle listener interface.
     */
    public interface ModuleLifecycleListener {
        void onModuleInitialized(XORModule module);
        void onModuleEnabled(XORModule module);
        void onModuleDisabled(XORModule module);
    }
}
