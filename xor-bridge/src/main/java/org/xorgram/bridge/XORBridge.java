package org.xorgram.bridge;

import android.app.Application;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;
import org.xorgram.core.XORModuleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * XORBridge - The single entry point for all XORGram hooks.
 * 
 * This is the ONLY class that the original Telegram code references.
 * All hook points in Telegram call methods in this class, which then
 * dispatch to the appropriate modules through XORModuleManager.
 * 
 * Design Principles:
 * - Each method is a single line call from Telegram
 * - Methods are static for easy patching
 * - All logic is delegated to modules
 * - No Telegram-specific types in signatures (use Object/primitives)
 * 
 * @see XORModuleManager
 * @see XOREvent
 */
public final class XORBridge {
    
    private static final String TAG = "XORBridge";
    
    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════
    
    private static XORModuleManager moduleManager;
    private static XORConfig config;
    private static boolean initialized = false;
    private static boolean debugMode = false;
    
    // ═══════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Initialize XORGram. Called from ApplicationLoader.onCreate().
     * This is the first hook point (GP-020).
     * 
     * @param app The Application instance
     */
    public static void init(@NonNull Application app) {
        if (initialized) {
            Log.w(TAG, "XORBridge already initialized");
            return;
        }
        
        Log.i(TAG, "═══════════════════════════════════════════════════");
        Log.i(TAG, "  XORGram Bridge Initializing...");
        Log.i(TAG, "═══════════════════════════════════════════════════");
        
        try {
            moduleManager = XORModuleManager.getInstance(app);
            config = XORConfig.getInstance(app);
            
            // Load and initialize all modules
            moduleManager.init();
            
            initialized = true;
            Log.i(TAG, "XORBridge initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize XORBridge", e);
        }
    }
    
    /**
     * Check if XORGram is initialized.
     * 
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Enable or disable debug mode.
     * 
     * @param enabled true to enable debug mode
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GHOST ENGINE HOOKS (GP-001 to GP-005)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: GP-001 - Before marking a dialog as read.
     * Called from MessagesController.markDialogAsRead()
     * 
     * @param dialogId The dialog ID
     * @return true to block the read receipt
     */
    public static boolean onBeforeMarkRead(long dialogId) {
        if (!initialized) return false;
        
        if (debugMode) {
            Log.d(TAG, "onBeforeMarkRead: dialogId=" + dialogId);
        }
        
        return moduleManager.dispatch(XOREvent.BEFORE_MARK_READ, dialogId);
    }
    
    /**
     * Hook: GP-002 - Before sending typing indicator.
     * Called from SendMessagesHelper.sendTyping()
     * 
     * @param dialogId The dialog ID
     * @return true to block the typing indicator
     */
    public static boolean onBeforeSendTyping(long dialogId) {
        if (!initialized) return false;
        
        if (debugMode) {
            Log.d(TAG, "onBeforeSendTyping: dialogId=" + dialogId);
        }
        
        return moduleManager.dispatch(XOREvent.BEFORE_SEND_TYPING, dialogId);
    }
    
    /**
     * Hook: GP-003 - Before updating online status.
     * Called from ConnectionsManager.sendRequest(UpdateStatus)
     * 
     * @param online true if going online, false if going offline
     * @return true to block the status update
     */
    public static boolean onBeforeUpdateOnline(boolean online) {
        if (!initialized) return false;
        
        if (debugMode) {
            Log.d(TAG, "onBeforeUpdateOnline: online=" + online);
        }
        
        return moduleManager.dispatch(XOREvent.BEFORE_UPDATE_ONLINE, online);
    }
    
    /**
     * Hook: GP-004 - Before marking a story as read.
     * Called from StoriesController.markStoryAsRead()
     * 
     * @param storyId The story ID
     * @return true to block the story view notification
     */
    public static boolean onBeforeMarkStoryRead(long storyId) {
        if (!initialized) return false;
        
        if (debugMode) {
            Log.d(TAG, "onBeforeMarkStoryRead: storyId=" + storyId);
        }
        
        return moduleManager.dispatch(XOREvent.BEFORE_MARK_STORY_READ, storyId);
    }
    
    /**
     * Hook: GP-005 - Before marking voice message as listened.
     * Called from MediaController.markVoiceAsListened()
     * 
     * @param messageId The message ID
     * @return true to block the listen notification
     */
    public static boolean onBeforeMarkVoiceListened(long messageId) {
        if (!initialized) return false;
        
        if (debugMode) {
            Log.d(TAG, "onBeforeMarkVoiceListened: messageId=" + messageId);
        }
        
        return moduleManager.dispatch(XOREvent.BEFORE_MARK_VOICE_LISTENED, messageId);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DATA VAULT HOOKS (GP-010 to GP-013)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: GP-010 - Before deleting messages.
     * Called from MessagesStorage.deleteMessages()
     * 
     * @param messageIds List of message IDs to delete
     * @param dialogId The dialog ID
     */
    public static void onBeforeDeleteMessages(@NonNull ArrayList<Integer> messageIds, long dialogId) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onBeforeDeleteMessages: dialogId=" + dialogId + ", count=" + messageIds.size());
        }
        
        moduleManager.dispatch(XOREvent.BEFORE_DELETE_MESSAGES, messageIds, dialogId);
    }
    
    /**
     * Hook: GP-011 - When a message is edited.
     * Called from MessagesStorage.updateMessage()
     * 
     * @param oldMessage The original message object
     * @param newMessage The edited message object
     */
    public static void onMessageEdited(@NonNull Object oldMessage, @NonNull Object newMessage) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onMessageEdited");
        }
        
        moduleManager.dispatch(XOREvent.MESSAGE_EDITED, oldMessage, newMessage);
    }
    
    /**
     * Hook: GP-012 - When processing incoming updates.
     * Called from MessagesController.processUpdates()
     * 
     * @param updates The updates object
     */
    public static void onProcessUpdates(@NonNull Object updates) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onProcessUpdates");
        }
        
        moduleManager.dispatch(XOREvent.PROCESS_UPDATES, updates);
    }
    
    /**
     * Hook: GP-013 - When a contact is updated.
     * Called from ContactsController.onContactUpdated()
     * 
     * @param userId The user ID
     * @param user The user object
     */
    public static void onContactUpdated(long userId, @NonNull Object user) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onContactUpdated: userId=" + userId);
        }
        
        moduleManager.dispatch(XOREvent.CONTACT_UPDATED, userId, user);
    }
    
    /**
     * Hook: When profile photo changes.
     * 
     * @param userId The user ID
     * @param photo The photo object
     */
    public static void onProfilePhotoChanged(long userId, @Nullable Object photo) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onProfilePhotoChanged: userId=" + userId);
        }
        
        moduleManager.dispatch(XOREvent.PROFILE_PHOTO_CHANGED, userId, photo);
    }
    
    /**
     * Hook: When user status changes.
     * 
     * @param userId The user ID
     * @param status The status object
     */
    public static void onUserStatusChanged(long userId, @Nullable Object status) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onUserStatusChanged: userId=" + userId);
        }
        
        moduleManager.dispatch(XOREvent.USER_STATUS_CHANGED, userId, status);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UI HOOKS (GP-020 to GP-024)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: GP-020 - When LaunchActivity is created.
     * This is where XORGram initializes.
     * 
     * @param activity The LaunchActivity instance
     */
    public static void onLaunchActivityCreated(@NonNull Object activity) {
        if (debugMode) {
            Log.d(TAG, "onLaunchActivityCreated");
        }
        
        // This is called before init() in some cases
        // The actual initialization happens in init()
        if (initialized) {
            moduleManager.dispatch(XOREvent.LAUNCH_ACTIVITY_CREATED, activity);
        }
    }
    
    /**
     * Hook: GP-021 - When ChatActivity view is created.
     * 
     * @param chatActivity The ChatActivity instance
     * @return A View to add as overlay, or null
     */
    @Nullable
    public static View onCreateChatOverlay(@NonNull Object chatActivity) {
        if (!initialized) return null;
        
        if (debugMode) {
            Log.d(TAG, "onCreateChatOverlay");
        }
        
        Object result = moduleManager.dispatchWithResult(XOREvent.CREATE_CHAT_OVERLAY, chatActivity);
        return result instanceof View ? (View) result : null;
    }
    
    /**
     * Hook: GP-022 - When ProfileActivity view is created.
     * 
     * @param profileActivity The ProfileActivity instance
     * @return A View to add as overlay, or null
     */
    @Nullable
    public static View onCreateProfileOverlay(@NonNull Object profileActivity) {
        if (!initialized) return null;
        
        if (debugMode) {
            Log.d(TAG, "onCreateProfileOverlay");
        }
        
        Object result = moduleManager.dispatchWithResult(XOREvent.CREATE_PROFILE_OVERLAY, profileActivity);
        return result instanceof View ? (View) result : null;
    }
    
    /**
     * Hook: GP-023 - When DialogsActivity view is created.
     * 
     * @param dialogsActivity The DialogsActivity instance
     * @return A View to add as overlay, or null
     */
    @Nullable
    public static View onCreateDialogsOverlay(@NonNull Object dialogsActivity) {
        if (!initialized) return null;
        
        if (debugMode) {
            Log.d(TAG, "onCreateDialogsOverlay");
        }
        
        Object result = moduleManager.dispatchWithResult(XOREvent.CREATE_DIALOGS_OVERLAY, dialogsActivity);
        return result instanceof View ? (View) result : null;
    }
    
    /**
     * Hook: GP-024 - When options menu is created.
     * 
     * @param fragment The BaseFragment instance
     * @param menu The menu object
     * @return List of menu items to add, or null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static List<Object> onCreateMenu(@NonNull Object fragment, @NonNull Object menu) {
        if (!initialized) return null;
        
        if (debugMode) {
            Log.d(TAG, "onCreateMenu");
        }
        
        Object result = moduleManager.dispatchWithResult(XOREvent.CREATE_MENU, fragment, menu);
        return result instanceof List ? (List<Object>) result : null;
    }
    
    /**
     * Hook: When theme is applied.
     * 
     * @param themeName The theme name
     */
    public static void onThemeApplied(@NonNull String themeName) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onThemeApplied: " + themeName);
        }
        
        moduleManager.dispatch(XOREvent.THEME_APPLIED, themeName);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MEDIA HOOKS (GP-030 to GP-032)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: GP-030 - Before loading an image.
     * 
     * @param url The image URL
     * @param receiver The ImageReceiver instance
     * @return true to intercept and provide custom image
     */
    public static boolean onBeforeLoadImage(@NonNull String url, @NonNull Object receiver) {
        if (!initialized) return false;
        
        if (debugMode) {
            Log.d(TAG, "onBeforeLoadImage: " + url);
        }
        
        return moduleManager.dispatch(XOREvent.BEFORE_LOAD_IMAGE, url, receiver);
    }
    
    /**
     * Hook: GP-031 - Before playing audio.
     * 
     * @param message The message object
     * @return true to use custom player
     */
    public static boolean onBeforePlayAudio(@NonNull Object message) {
        if (!initialized) return false;
        
        if (debugMode) {
            Log.d(TAG, "onBeforePlayAudio");
        }
        
        return moduleManager.dispatch(XOREvent.BEFORE_PLAY_AUDIO, message);
    }
    
    /**
     * Hook: GP-032 - Before sending media.
     * 
     * @param message The message object
     * @param media The input media object
     */
    public static void onBeforeSendMedia(@NonNull Object message, @NonNull Object media) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onBeforeSendMedia");
        }
        
        moduleManager.dispatch(XOREvent.BEFORE_SEND_MEDIA, message, media);
    }
    
    /**
     * Hook: When video player is created.
     * 
     * @param player The video player instance
     */
    public static void onVideoPlayerCreated(@NonNull Object player) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onVideoPlayerCreated");
        }
        
        moduleManager.dispatch(XOREvent.VIDEO_PLAYER_CREATED, player);
    }
    
    /**
     * Hook: When audio playback state changes.
     * 
     * @param state The playback state
     * @param message The message object
     */
    public static void onAudioPlaybackStateChanged(int state, @Nullable Object message) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onAudioPlaybackStateChanged: " + state);
        }
        
        moduleManager.dispatch(XOREvent.AUDIO_PLAYBACK_STATE_CHANGED, state, message);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // SECURITY HOOKS (GP-040 to GP-042)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: GP-040 - Get device info string.
     * 
     * @return Custom device info string, or null to use default
     */
    @Nullable
    public static String onGetDeviceInfo() {
        if (!initialized) return null;
        
        if (debugMode) {
            Log.d(TAG, "onGetDeviceInfo");
        }
        
        Object result = moduleManager.dispatchWithResult(XOREvent.GET_DEVICE_INFO);
        return result instanceof String ? (String) result : null;
    }
    
    /**
     * Hook: GP-041 - Get client name.
     * 
     * @return Custom client name, or null to use default
     */
    @Nullable
    public static String onGetClientName() {
        if (!initialized) return null;
        
        if (debugMode) {
            Log.d(TAG, "onGetClientName");
        }
        
        Object result = moduleManager.dispatchWithResult(XOREvent.GET_CLIENT_NAME);
        return result instanceof String ? (String) result : null;
    }
    
    /**
     * Hook: GP-042 - App going to background.
     */
    public static void onAppGoingBackground() {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onAppGoingBackground");
        }
        
        moduleManager.onBackground();
        moduleManager.dispatch(XOREvent.APP_GOING_BACKGROUND);
    }
    
    /**
     * Hook: App coming to foreground.
     */
    public static void onAppComingForeground() {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onAppComingForeground");
        }
        
        moduleManager.onForeground();
        moduleManager.dispatch(XOREvent.APP_COMING_FOREGROUND);
    }
    
    /**
     * Hook: Check if biometric is required.
     * 
     * @return true if biometric lock is active
     */
    public static boolean onRequireBiometric() {
        if (!initialized) return false;
        
        Object result = moduleManager.dispatchWithResult(XOREvent.REQUIRE_BIOMETRIC);
        return result instanceof Boolean ? (Boolean) result : false;
    }
    
    /**
     * Hook: Panic button triggered.
     */
    public static void onPanicTriggered() {
        if (!initialized) return;
        
        Log.w(TAG, "PANIC TRIGGERED!");
        moduleManager.dispatch(XOREvent.PANIC_TRIGGERED);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // SYNC HOOKS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: When settings change.
     * 
     * @param key The setting key
     * @param value The new value
     */
    public static void onSettingsChanged(@NonNull String key, @NonNull Object value) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onSettingsChanged: " + key);
        }
        
        moduleManager.dispatch(XOREvent.SETTINGS_CHANGED, key, value);
    }
    
    /**
     * Hook: Sync data to other devices.
     * 
     * @param dataType The data type
     * @param data The data bytes
     */
    public static void onSyncData(@NonNull String dataType, @NonNull byte[] data) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onSyncData: " + dataType);
        }
        
        moduleManager.dispatch(XOREvent.SYNC_DATA, dataType, data);
    }
    
    /**
     * Hook: Received sync data from another device.
     * 
     * @param dataType The data type
     * @param data The data bytes
     */
    public static void onSyncDataReceived(@NonNull String dataType, @NonNull byte[] data) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onSyncDataReceived: " + dataType);
        }
        
        moduleManager.dispatch(XOREvent.ON_SYNC_DATA_RECEIVED, dataType, data);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // AI HOOKS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: Summarize chat.
     * 
     * @param dialogId The dialog ID
     * @param messageCount Number of messages to summarize
     * @return Summary string, or null
     */
    @Nullable
    public static String onSummarizeChat(long dialogId, int messageCount) {
        if (!initialized) return null;
        
        Object result = moduleManager.dispatchWithResult(XOREvent.SUMMARIZE_CHAT, dialogId, messageCount);
        return result instanceof String ? (String) result : null;
    }
    
    /**
     * Hook: Transcribe voice message.
     * 
     * @param voiceMessage The voice message object
     * @return Transcription string, or null
     */
    @Nullable
    public static String onTranscribeVoice(@NonNull Object voiceMessage) {
        if (!initialized) return null;
        
        Object result = moduleManager.dispatchWithResult(XOREvent.TRANSCRIBE_VOICE, voiceMessage);
        return result instanceof String ? (String) result : null;
    }
    
    /**
     * Hook: Generate smart reply.
     * 
     * @param dialogId The dialog ID
     * @param message The message to reply to
     * @return List of suggested replies, or null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static List<String> onGenerateSmartReply(long dialogId, @NonNull Object message) {
        if (!initialized) return null;
        
        Object result = moduleManager.dispatchWithResult(XOREvent.GENERATE_SMART_REPLY, dialogId, message);
        return result instanceof List ? (List<String>) result : null;
    }
    
    /**
     * Hook: Translate text.
     * 
     * @param text The text to translate
     * @param targetLang The target language code
     * @return Translated text, or null
     */
    @Nullable
    public static String onTranslateText(@NonNull String text, @NonNull String targetLang) {
        if (!initialized) return null;
        
        Object result = moduleManager.dispatchWithResult(XOREvent.TRANSLATE_TEXT, text, targetLang);
        return result instanceof String ? (String) result : null;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ADMIN HOOKS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: Channel stats loaded.
     * 
     * @param channelId The channel ID
     * @param stats The stats object
     */
    public static void onChannelStatsLoaded(long channelId, @NonNull Object stats) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onChannelStatsLoaded: " + channelId);
        }
        
        moduleManager.dispatch(XOREvent.CHANNEL_STATS_LOADED, channelId, stats);
    }
    
    /**
     * Hook: Auto-post to channel.
     * 
     * @param channelId The channel ID
     * @param message The message to post
     * @return true if handled
     */
    public static boolean onAutoPost(long channelId, @NonNull Object message) {
        if (!initialized) return false;
        
        return moduleManager.dispatch(XOREvent.AUTO_POST, channelId, message);
    }
    
    /**
     * Hook: Cross-post from one channel to another.
     * 
     * @param sourceChannelId The source channel ID
     * @param targetChannelId The target channel ID
     * @param message The message to cross-post
     * @return true if handled
     */
    public static boolean onCrossPost(long sourceChannelId, long targetChannelId, @NonNull Object message) {
        if (!initialized) return false;
        
        return moduleManager.dispatch(XOREvent.CROSS_POST, sourceChannelId, targetChannelId, message);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // AUTOMATION HOOKS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: New message received.
     * 
     * @param dialogId The dialog ID
     * @param message The message object
     */
    public static void onMessageReceived(long dialogId, @NonNull Object message) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onMessageReceived: dialogId=" + dialogId);
        }
        
        moduleManager.dispatch(XOREvent.MESSAGE_RECEIVED, dialogId, message);
    }
    
    /**
     * Hook: Scheduled automation.
     * 
     * @param ruleId The rule ID
     */
    public static void onScheduledAutomation(@NonNull String ruleId) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onScheduledAutomation: " + ruleId);
        }
        
        moduleManager.dispatch(XOREvent.SCHEDULED_AUTOMATION, ruleId);
    }
    
    /**
     * Hook: Webhook trigger.
     * 
     * @param webhookUrl The webhook URL
     * @param payload The payload
     */
    public static void onWebhookTrigger(@NonNull String webhookUrl, @NonNull String payload) {
        if (!initialized) return;
        
        if (debugMode) {
            Log.d(TAG, "onWebhookTrigger: " + webhookUrl);
        }
        
        moduleManager.dispatch(XOREvent.WEBHOOK_TRIGGER, webhookUrl, payload);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PLUGIN HOOKS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Hook: Load plugin.
     * 
     * @param pluginId The plugin ID
     * @return true if loaded successfully
     */
    public static boolean onLoadPlugin(@NonNull String pluginId) {
        if (!initialized) return false;
        
        Object result = moduleManager.dispatchWithResult(XOREvent.LOAD_PLUGIN, pluginId);
        return result instanceof Boolean ? (Boolean) result : false;
    }
    
    /**
     * Hook: Unload plugin.
     * 
     * @param pluginId The plugin ID
     */
    public static void onUnloadPlugin(@NonNull String pluginId) {
        if (!initialized) return;
        
        moduleManager.dispatch(XOREvent.UNLOAD_PLUGIN, pluginId);
    }
    
    /**
     * Hook: Plugin permission request.
     * 
     * @param pluginId The plugin ID
     * @param permission The permission requested
     * @return true to grant permission
     */
    public static boolean onPluginPermissionRequest(@NonNull String pluginId, @NonNull String permission) {
        if (!initialized) return false;
        
        Object result = moduleManager.dispatchWithResult(XOREvent.PLUGIN_PERMISSION_REQUEST, pluginId, permission);
        return result instanceof Boolean ? (Boolean) result : false;
    }
    
    /**
     * Hook: Execute plugin script.
     * 
     * @param pluginId The plugin ID
     * @param script The script to execute
     * @return Script result, or null
     */
    @Nullable
    public static Object onExecutePluginScript(@NonNull String pluginId, @NonNull String script) {
        if (!initialized) return null;
        
        return moduleManager.dispatchWithResult(XOREvent.EXECUTE_PLUGIN_SCRIPT, pluginId, script);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Utility Methods
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get the module manager.
     * 
     * @return XORModuleManager instance
     */
    @Nullable
    public static XORModuleManager getModuleManager() {
        return moduleManager;
    }
    
    /**
     * Get the config.
     * 
     * @return XORConfig instance
     */
    @Nullable
    public static XORConfig getConfig() {
        return config;
    }
    
    /**
     * Get debug information.
     * 
     * @return Debug info string
     */
    @NonNull
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("XORBridge Debug Info:\n");
        sb.append("  Initialized: ").append(initialized).append("\n");
        sb.append("  Debug Mode: ").append(debugMode).append("\n");
        
        if (moduleManager != null) {
            sb.append(moduleManager.getDebugInfo());
        }
        
        return sb.toString();
    }
}
