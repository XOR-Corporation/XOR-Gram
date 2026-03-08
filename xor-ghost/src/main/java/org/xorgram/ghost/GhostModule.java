package org.xorgram.ghost;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;
import org.xorgram.core.XORModule;
import org.xorgram.core.XORSetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GhostModule - Ghost Engine for XORGram.
 * 
 * Features:
 * - Hide read receipts (messages appear unread)
 * - Hide typing indicators
 * - Hide online status
 * - Hide story views
 * - Hide voice message listen status
 * - Per-chat whitelist/blacklist
 * - Scheduled ghost mode
 * - Multiple ghost profiles
 * 
 * @see GhostProfile
 * @see GhostScheduler
 */
public class GhostModule implements XORModule {
    
    private static final String TAG = "GhostModule";
    
    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════
    
    private XORConfig config;
    private GhostProfile activeProfile;
    private GhostScheduler scheduler;
    private Set<Long> whitelist;
    private Set<Long> blacklist;
    private boolean enabled = false;
    private Context context;
    
    // ═══════════════════════════════════════════════════════════════
    // Module Identification
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public String getId() {
        return "xor-ghost";
    }
    
    @Override
    public String getName() {
        return "Ghost Engine";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Hide your online presence, read receipts, and typing indicators";
    }
    
    @Override
    public String getIcon() {
        return "ic_ghost";
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
                XOREvent.BEFORE_MARK_READ,
                XOREvent.BEFORE_SEND_TYPING,
                XOREvent.BEFORE_UPDATE_ONLINE,
                XOREvent.BEFORE_MARK_STORY_READ,
                XOREvent.BEFORE_MARK_VOICE_LISTENED
        );
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority - should be processed first
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public void onInit(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        
        // Load whitelist/blacklist
        loadWhitelistBlacklist();
        
        // Initialize scheduler
        scheduler = new GhostScheduler(config);
        
        // Load active profile
        loadActiveProfile();
        
        Log.i(TAG, "GhostModule initialized");
    }
    
    @Override
    public void onEnable() {
        enabled = true;
        Log.i(TAG, "GhostModule enabled");
    }
    
    @Override
    public void onDisable() {
        enabled = false;
        Log.i(TAG, "GhostModule disabled");
    }
    
    @Override
    public void onDestroy() {
        enabled = false;
        if (scheduler != null) {
            scheduler.stop();
        }
        Log.i(TAG, "GhostModule destroyed");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Event Handling
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public boolean onEvent(XOREvent event, Object... args) {
        if (!isEnabled()) {
            return false;
        }
        
        // Check if ghost mode is enabled
        if (!config.getBoolean("ghost_enabled", false)) {
            return false;
        }
        
        // Check scheduler
        if (scheduler != null && !scheduler.isGhostActive()) {
            return false;
        }
        
        switch (event) {
            case BEFORE_MARK_READ:
                return handleMarkRead(args);
                
            case BEFORE_SEND_TYPING:
                return handleSendTyping(args);
                
            case BEFORE_UPDATE_ONLINE:
                return handleUpdateOnline(args);
                
            case BEFORE_MARK_STORY_READ:
                return handleMarkStoryRead(args);
                
            case BEFORE_MARK_VOICE_LISTENED:
                return handleMarkVoiceListened(args);
                
            default:
                return false;
        }
    }
    
    private boolean handleMarkRead(Object... args) {
        if (!config.getBoolean("ghost_block_read", true)) {
            return false;
        }
        
        long dialogId = (long) args[0];
        
        // Check whitelist/blacklist
        if (isWhitelisted(dialogId)) {
            Log.d(TAG, "Dialog whitelisted, allowing read: " + dialogId);
            return false;
        }
        
        if (isBlacklisted(dialogId)) {
            Log.d(TAG, "Dialog blacklisted, blocking read: " + dialogId);
            return true;
        }
        
        // Check profile settings
        if (activeProfile != null && !activeProfile.shouldBlockReadFor(dialogId)) {
            return false;
        }
        
        Log.d(TAG, "Blocking read receipt for dialog: " + dialogId);
        return true;
    }
    
    private boolean handleSendTyping(Object... args) {
        if (!config.getBoolean("ghost_block_typing", true)) {
            return false;
        }
        
        long dialogId = (long) args[0];
        
        if (isWhitelisted(dialogId)) {
            return false;
        }
        
        if (activeProfile != null && !activeProfile.isBlockTypingEnabled()) {
            return false;
        }
        
        Log.d(TAG, "Blocking typing indicator for dialog: " + dialogId);
        return true;
    }
    
    private boolean handleUpdateOnline(Object... args) {
        if (!config.getBoolean("ghost_block_online", true)) {
            return false;
        }
        
        boolean online = (boolean) args[0];
        
        if (activeProfile != null && !activeProfile.isBlockOnlineEnabled()) {
            return false;
        }
        
        Log.d(TAG, "Blocking online status update: " + (online ? "online" : "offline"));
        return true;
    }
    
    private boolean handleMarkStoryRead(Object... args) {
        if (!config.getBoolean("ghost_block_stories", true)) {
            return false;
        }
        
        long storyId = (long) args[0];
        
        if (activeProfile != null && !activeProfile.isBlockStoryViewEnabled()) {
            return false;
        }
        
        Log.d(TAG, "Blocking story view: " + storyId);
        return true;
    }
    
    private boolean handleMarkVoiceListened(Object... args) {
        if (!config.getBoolean("ghost_block_voice", true)) {
            return false;
        }
        
        long messageId = (long) args[0];
        
        Log.d(TAG, "Blocking voice listen: " + messageId);
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Settings
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public List<XORSetting> getSettings() {
        List<XORSetting> settings = new ArrayList<>();
        
        // Main toggle
        settings.add(XORSetting.toggle(
                "ghost_enabled",
                "Ghost Mode",
                "Enable ghost mode to hide your activity",
                false
        ));
        
        settings.add(XORSetting.header("Read Receipts"));
        
        settings.add(XORSetting.toggle(
                "ghost_block_read",
                "Hide Read Receipts",
                "Messages will appear unread to others",
                true
        ));
        
        settings.add(XORSetting.toggle(
                "ghost_block_stories",
                "Hide Story Views",
                "Stories you view won't be marked as seen",
                true
        ));
        
        settings.add(XORSetting.toggle(
                "ghost_block_voice",
                "Hide Voice Listen",
                "Voice messages won't be marked as listened",
                true
        ));
        
        settings.add(XORSetting.header("Activity Status"));
        
        settings.add(XORSetting.toggle(
                "ghost_block_typing",
                "Hide Typing Indicator",
                "Others won't see when you're typing",
                true
        ));
        
        settings.add(XORSetting.toggle(
                "ghost_block_online",
                "Hide Online Status",
                "You'll always appear offline",
                true
        ));
        
        settings.add(XORSetting.header("Scheduling"));
        
        settings.add(XORSetting.schedule(
                "ghost_schedule",
                "Schedule",
                config.getString("ghost_schedule", "")
        ));
        
        settings.add(XORSetting.header("Exceptions"));
        
        settings.add(XORSetting.info(
                "Add chats to whitelist to allow read receipts, or blacklist to always block"
        ));
        
        return settings;
    }
    
    @Override
    public void onSettingChanged(String key, Object value) {
        switch (key) {
            case "ghost_enabled":
                enabled = (boolean) value;
                break;
                
            case "ghost_schedule":
                if (scheduler != null) {
                    scheduler.updateSchedule((String) value);
                }
                break;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public boolean isEnabled() {
        return enabled && config.getBoolean("ghost_enabled", false);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Whitelist/Blacklist Management
    // ═══════════════════════════════════════════════════════════════
    
    private void loadWhitelistBlacklist() {
        whitelist = new HashSet<>();
        blacklist = new HashSet<>();
        
        // Load from config
        List<String> whitelistStr = config.getList("ghost_whitelist", Collections.emptyList());
        for (String id : whitelistStr) {
            try {
                whitelist.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid whitelist entry: " + id);
            }
        }
        
        List<String> blacklistStr = config.getList("ghost_blacklist", Collections.emptyList());
        for (String id : blacklistStr) {
            try {
                blacklist.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid blacklist entry: " + id);
            }
        }
    }
    
    public void addToWhitelist(long dialogId) {
        whitelist.add(dialogId);
        saveWhitelist();
    }
    
    public void removeFromWhitelist(long dialogId) {
        whitelist.remove(dialogId);
        saveWhitelist();
    }
    
    public void addToBlacklist(long dialogId) {
        blacklist.add(dialogId);
        saveBlacklist();
    }
    
    public void removeFromBlacklist(long dialogId) {
        blacklist.remove(dialogId);
        saveBlacklist();
    }
    
    public boolean isWhitelisted(long dialogId) {
        return whitelist.contains(dialogId);
    }
    
    public boolean isBlacklisted(long dialogId) {
        return blacklist.contains(dialogId);
    }
    
    private void saveWhitelist() {
        List<String> list = new ArrayList<>();
        for (Long id : whitelist) {
            list.add(String.valueOf(id));
        }
        config.putList("ghost_whitelist", list);
    }
    
    private void saveBlacklist() {
        List<String> list = new ArrayList<>();
        for (Long id : blacklist) {
            list.add(String.valueOf(id));
        }
        config.putList("ghost_blacklist", list);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Profile Management
    // ═══════════════════════════════════════════════════════════════
    
    private void loadActiveProfile() {
        String profileName = config.getString("ghost_active_profile", "full");
        activeProfile = GhostProfile.load(config, profileName);
    }
    
    public void setActiveProfile(String profileName) {
        config.putString("ghost_active_profile", profileName);
        activeProfile = GhostProfile.load(config, profileName);
    }
    
    public GhostProfile getActiveProfile() {
        return activeProfile;
    }
    
    public List<String> getAvailableProfiles() {
        return Arrays.asList("full", "stealth", "custom");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Debug
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("enabled=").append(enabled).append(", ");
        sb.append("whitelist=").append(whitelist.size()).append(", ");
        sb.append("blacklist=").append(blacklist.size());
        return sb.toString();
    }
    
    @Override
    public void reset() {
        whitelist.clear();
        blacklist.clear();
        config.putList("ghost_whitelist", Collections.emptyList());
        config.putList("ghost_blacklist", Collections.emptyList());
        config.putString("ghost_schedule", "");
        config.putBoolean("ghost_enabled", false);
        loadActiveProfile();
    }
}
