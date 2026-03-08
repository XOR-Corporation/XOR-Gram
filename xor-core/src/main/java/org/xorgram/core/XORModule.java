package org.xorgram.core;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * XORModule - Interface that all XORGram modules must implement.
 * 
 * Each module is a self-contained unit of functionality that:
 * - Subscribes to specific events from the Telegram client
 * - Can block or modify Telegram's default behavior
 * - Provides its own settings UI
 * - Can be enabled/disabled independently
 * 
 * @see XOREvent
 * @see XORSetting
 */
public interface XORModule {
    
    // ═══════════════════════════════════════════════════════════════
    // Module Identification
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get the unique identifier for this module.
     * Format: "xor-{name}" (e.g., "xor-ghost", "xor-vault")
     * 
     * @return Module ID string
     */
    String getId();
    
    /**
     * Get the human-readable name of this module.
     * Used in UI and logs.
     * 
     * @return Module display name
     */
    String getName();
    
    /**
     * Get the module version string.
     * Format: "major.minor.patch" (e.g., "1.0.0")
     * 
     * @return Version string
     */
    String getVersion();
    
    /**
     * Get a brief description of what this module does.
     * Used in module list UI.
     * 
     * @return Description string
     */
    String getDescription();
    
    /**
     * Get the module icon resource name.
     * 
     * @return Icon resource name or null for default
     */
    @Nullable
    default String getIcon() {
        return null;
    }
    
    /**
     * Get the module category for grouping in UI.
     * 
     * @return Category string
     */
    default String getCategory() {
        return "other";
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Event Subscription
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get the set of events this module wants to handle.
     * Only events in this set will be dispatched to onEvent().
     * 
     * @return Set of XOREvent to subscribe to
     */
    Set<XOREvent> getSubscribedEvents();
    
    /**
     * Get the priority for event handling.
     * Higher priority modules are called first.
     * Default is 0 (normal priority).
     * 
     * @return Priority value (-100 to 100)
     */
    default int getPriority() {
        return 0;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Called when the module is first initialized.
     * Use this to set up any required resources, register listeners, etc.
     * 
     * @param context Application context
     * @param config XORConfig instance for reading/writing settings
     */
    void onInit(Context context, XORConfig config);
    
    /**
     * Called when the module is enabled.
     * Use this to start any active functionality.
     */
    default void onEnable() {}
    
    /**
     * Called when the module is disabled.
     * Use this to pause functionality but keep resources.
     */
    default void onDisable() {}
    
    /**
     * Called when the module is being destroyed.
     * Clean up all resources here.
     */
    void onDestroy();
    
    /**
     * Called when app is going to background.
     * Use for cleanup or state saving.
     */
    default void onBackground() {}
    
    /**
     * Called when app is coming to foreground.
     * Use for restoring state.
     */
    default void onForeground() {}
    
    // ═══════════════════════════════════════════════════════════════
    // Event Handling
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Handle an event from the Telegram client.
     * 
     * @param event The event type
     * @param args Event arguments (varies by event type)
     * @return true to block the default Telegram behavior, false to allow it
     */
    boolean onEvent(XOREvent event, Object... args);
    
    /**
     * Handle an event and return a result.
     * Used for events that expect a return value.
     * 
     * @param event The event type
     * @param args Event arguments
     * @return Result object (type depends on event)
     */
    @Nullable
    default Object onEventWithResult(XOREvent event, Object... args) {
        onEvent(event, args);
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UI Extensions
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Create an overlay view for a specific UI event.
     * Called when ChatActivity, ProfileActivity, etc. are created.
     * 
     * @param event The UI event (CREATE_CHAT_OVERLAY, etc.)
     * @param args Context arguments (activity, fragment, etc.)
     * @return A View to add as overlay, or null for no overlay
     */
    @Nullable
    default View onCreateOverlay(XOREvent event, Object... args) {
        return null;
    }
    
    /**
     * Get the settings for this module.
     * These are displayed in the XORGram settings screen.
     * 
     * @return List of XORSetting objects
     */
    default List<XORSetting> getSettings() {
        return Collections.emptyList();
    }
    
    /**
     * Called when a setting value changes.
     * 
     * @param key Setting key
     * @param value New value
     */
    default void onSettingChanged(String key, Object value) {}
    
    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Check if this module is currently enabled.
     * 
     * @return true if enabled
     */
    boolean isEnabled();
    
    /**
     * Set the enabled state of this module.
     * 
     * @param enabled true to enable
     */
    void setEnabled(boolean enabled);
    
    /**
     * Check if this module requires a restart to apply changes.
     * 
     * @return true if restart required
     */
    default boolean requiresRestart() {
        return false;
    }
    
    /**
     * Check if this module is available (dependencies met, etc.).
     * 
     * @return true if available
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * Get the reason why this module is unavailable.
     * 
     * @return Unavailability reason or null if available
     */
    @Nullable
    default String getUnavailableReason() {
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Dependencies
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get the list of module IDs this module depends on.
     * 
     * @return List of dependency module IDs
     */
    default List<String> getDependencies() {
        return Collections.emptyList();
    }
    
    /**
     * Get the minimum version required for a dependency.
     * 
     * @param moduleId Dependency module ID
     * @return Minimum version string or null if any version
     */
    @Nullable
    default String getMinimumVersion(String moduleId) {
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Debug / Logging
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get debug information about this module's state.
     * Used for troubleshooting.
     * 
     * @return Debug info string
     */
    @Nullable
    default String getDebugInfo() {
        return null;
    }
    
    /**
     * Called to reset this module to default state.
     */
    default void reset() {}
}
