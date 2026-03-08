package org.xorgram.vault;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;
import org.xorgram.core.XORModule;
import org.xorgram.core.XORSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * VaultModule - Data Vault for XORGram.
 * 
 * Features:
 * - Anti-Recall: Keep deleted messages locally
 * - Edit History: Track all message edits
 * - Profile Snapshots: Save profile changes over time
 * - Black Box Logger: Comprehensive activity logging
 * 
 * @see AntiRecallEngine
 * @see EditHistoryTracker
 * @see ProfileSnapshotService
 * @see BlackBoxLogger
 */
public class VaultModule implements XORModule {
    
    private static final String TAG = "VaultModule";
    
    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════
    
    private XORConfig config;
    private AntiRecallEngine antiRecallEngine;
    private EditHistoryTracker editHistoryTracker;
    private ProfileSnapshotService profileSnapshotService;
    private BlackBoxLogger blackBoxLogger;
    private boolean enabled = false;
    private Context context;
    
    // ═══════════════════════════════════════════════════════════════
    // Module Identification
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public String getId() {
        return "xor-vault";
    }
    
    @Override
    public String getName() {
        return "Data Vault";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Keep deleted messages, track edits, and save profile history";
    }
    
    @Override
    public String getIcon() {
        return "ic_vault";
    }
    
    @Override
    public String getCategory() {
        return "privacy";
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Event Subscription
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public Set<XOREvent> getSubscribedEvents() {
        return EnumSet.of(
                XOREvent.BEFORE_DELETE_MESSAGES,
                XOREvent.MESSAGE_EDITED,
                XOREvent.PROCESS_UPDATES,
                XOREvent.CONTACT_UPDATED,
                XOREvent.PROFILE_PHOTO_CHANGED,
                XOREvent.USER_STATUS_CHANGED
        );
    }
    
    @Override
    public int getPriority() {
        return 90; // High priority for data capture
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public void onInit(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        
        // Initialize engines
        antiRecallEngine = new AntiRecallEngine(config);
        editHistoryTracker = new EditHistoryTracker(config);
        profileSnapshotService = new ProfileSnapshotService(config);
        blackBoxLogger = new BlackBoxLogger(config);
        
        Log.i(TAG, "VaultModule initialized");
    }
    
    @Override
    public void onEnable() {
        enabled = true;
        
        if (config.getBoolean("vault_anti_recall", true)) {
            antiRecallEngine.start();
        }
        
        if (config.getBoolean("vault_edit_history", true)) {
            editHistoryTracker.start();
        }
        
        if (config.getBoolean("vault_profile_snapshots", false)) {
            profileSnapshotService.start();
        }
        
        if (config.getBoolean("vault_black_box", false)) {
            blackBoxLogger.start();
        }
        
        Log.i(TAG, "VaultModule enabled");
    }
    
    @Override
    public void onDisable() {
        enabled = false;
        
        antiRecallEngine.stop();
        editHistoryTracker.stop();
        profileSnapshotService.stop();
        blackBoxLogger.stop();
        
        Log.i(TAG, "VaultModule disabled");
    }
    
    @Override
    public void onDestroy() {
        enabled = false;
        
        antiRecallEngine.destroy();
        editHistoryTracker.destroy();
        profileSnapshotService.destroy();
        blackBoxLogger.destroy();
        
        Log.i(TAG, "VaultModule destroyed");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Event Handling
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public boolean onEvent(XOREvent event, Object... args) {
        if (!isEnabled()) {
            return false;
        }
        
        switch (event) {
            case BEFORE_DELETE_MESSAGES:
                handleDeleteMessages(args);
                return false; // Don't block deletion, just capture
                
            case MESSAGE_EDITED:
                handleMessageEdited(args);
                return false;
                
            case PROCESS_UPDATES:
                handleProcessUpdates(args);
                return false;
                
            case CONTACT_UPDATED:
                handleContactUpdated(args);
                return false;
                
            case PROFILE_PHOTO_CHANGED:
                handleProfilePhotoChanged(args);
                return false;
                
            case USER_STATUS_CHANGED:
                handleUserStatusChanged(args);
                return false;
                
            default:
                return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleDeleteMessages(Object... args) {
        if (!config.getBoolean("vault_anti_recall", true)) {
            return;
        }
        
        ArrayList<Integer> messageIds = (ArrayList<Integer>) args[0];
        long dialogId = (long) args[1];
        
        Log.d(TAG, "Capturing deleted messages: " + messageIds.size() + " from " + dialogId);
        antiRecallEngine.captureMessages(messageIds, dialogId);
    }
    
    private void handleMessageEdited(Object... args) {
        if (!config.getBoolean("vault_edit_history", true)) {
            return;
        }
        
        Object oldMessage = args[0];
        Object newMessage = args[1];
        
        Log.d(TAG, "Capturing edited message");
        editHistoryTracker.captureEdit(oldMessage, newMessage);
    }
    
    private void handleProcessUpdates(Object... args) {
        if (!config.getBoolean("vault_anti_recall", true)) {
            return;
        }
        
        Object updates = args[0];
        antiRecallEngine.processUpdates(updates);
    }
    
    private void handleContactUpdated(Object... args) {
        if (!config.getBoolean("vault_profile_snapshots", false)) {
            return;
        }
        
        long userId = (long) args[0];
        Object user = args[1];
        
        Log.d(TAG, "Capturing profile update: " + userId);
        profileSnapshotService.captureProfile(userId, user);
    }
    
    private void handleProfilePhotoChanged(Object... args) {
        if (!config.getBoolean("vault_profile_snapshots", false)) {
            return;
        }
        
        long userId = (long) args[0];
        Object photo = args[1];
        
        Log.d(TAG, "Capturing profile photo: " + userId);
        profileSnapshotService.capturePhoto(userId, photo);
    }
    
    private void handleUserStatusChanged(Object... args) {
        if (!config.getBoolean("vault_black_box", false)) {
            return;
        }
        
        long userId = (long) args[0];
        Object status = args[1];
        
        blackBoxLogger.logStatusChange(userId, status);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Settings
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public List<XORSetting> getSettings() {
        List<XORSetting> settings = new ArrayList<>();
        
        // Main toggle
        settings.add(XORSetting.toggle(
                "vault_enabled",
                "Data Vault",
                "Enable data vault features",
                true
        ));
        
        settings.add(XORSetting.header("Anti-Recall"));
        
        settings.add(XORSetting.toggle(
                "vault_anti_recall",
                "Keep Deleted Messages",
                "Messages deleted by others will be kept locally",
                true
        ));
        
        settings.add(XORSetting.toggle(
                "vault_deleted_messages_limit",
                "Limit Storage",
                "Limit how long deleted messages are kept",
                true
        ));
        
        settings.add(XORSetting.numberInput(
                "vault_retention_days",
                "Retention Days",
                30, 1, 365
        ));
        
        settings.add(XORSetting.header("Edit History"));
        
        settings.add(XORSetting.toggle(
                "vault_edit_history",
                "Track Message Edits",
                "Keep history of all message edits",
                true
        ));
        
        settings.add(XORSetting.header("Profile Snapshots"));
        
        settings.add(XORSetting.toggle(
                "vault_profile_snapshots",
                "Save Profile Changes",
                "Keep snapshots of profile changes over time",
                false
        ));
        
        settings.add(XORSetting.header("Black Box Logger"));
        
        settings.add(XORSetting.toggle(
                "vault_black_box",
                "Activity Logger",
                "Log all activity for debugging and analysis",
                false
        ));
        
        settings.add(XORSetting.info(
                "Black Box logs are stored locally and never sent to any server"
        ));
        
        return settings;
    }
    
    @Override
    public void onSettingChanged(String key, Object value) {
        switch (key) {
            case "vault_anti_recall":
                if ((boolean) value) {
                    antiRecallEngine.start();
                } else {
                    antiRecallEngine.stop();
                }
                break;
                
            case "vault_edit_history":
                if ((boolean) value) {
                    editHistoryTracker.start();
                } else {
                    editHistoryTracker.stop();
                }
                break;
                
            case "vault_profile_snapshots":
                if ((boolean) value) {
                    profileSnapshotService.start();
                } else {
                    profileSnapshotService.stop();
                }
                break;
                
            case "vault_black_box":
                if ((boolean) value) {
                    blackBoxLogger.start();
                } else {
                    blackBoxLogger.stop();
                }
                break;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get the anti-recall engine.
     */
    public AntiRecallEngine getAntiRecallEngine() {
        return antiRecallEngine;
    }
    
    /**
     * Get the edit history tracker.
     */
    public EditHistoryTracker getEditHistoryTracker() {
        return editHistoryTracker;
    }
    
    /**
     * Get the profile snapshot service.
     */
    public ProfileSnapshotService getProfileSnapshotService() {
        return profileSnapshotService;
    }
    
    /**
     * Get the black box logger.
     */
    public BlackBoxLogger getBlackBoxLogger() {
        return blackBoxLogger;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Debug
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("enabled=").append(enabled).append(", ");
        sb.append("antiRecall=").append(antiRecallEngine.getMessageCount()).append(" msgs, ");
        sb.append("editHistory=").append(editHistoryTracker.getEditCount()).append(" edits, ");
        sb.append("snapshots=").append(profileSnapshotService.getSnapshotCount());
        return sb.toString();
    }
    
    @Override
    public void reset() {
        antiRecallEngine.clear();
        editHistoryTracker.clear();
        profileSnapshotService.clear();
        blackBoxLogger.clear();
    }
}
