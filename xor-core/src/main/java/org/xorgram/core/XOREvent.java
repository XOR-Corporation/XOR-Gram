package org.xorgram.core;

/**
 * XOREvent - Enumeration of all events that can be dispatched through the XORBridge.
 * 
 * Each event corresponds to a specific hook point in the Telegram client.
 * Modules subscribe to events they want to handle.
 * 
 * @see XORModule#getSubscribedEvents()
 */
public enum XOREvent {
    
    // ═══════════════════════════════════════════════════════════════
    // GHOST ENGINE EVENTS (GP-001 to GP-005)
    // ═══════════════════════════════════════════════════════════════
    
    /** 
     * Dispatched before marking a dialog as read.
     * Return true to block the read receipt.
     * Args: long dialogId
     */
    BEFORE_MARK_READ,
    
    /**
     * Dispatched before sending typing indicator.
     * Return true to block the typing notification.
     * Args: long dialogId
     */
    BEFORE_SEND_TYPING,
    
    /**
     * Dispatched before updating online status.
     * Return true to block the online status update.
     * Args: boolean online
     */
    BEFORE_UPDATE_ONLINE,
    
    /**
     * Dispatched before marking a story as read.
     * Return true to block the story view notification.
     * Args: long storyId
     */
    BEFORE_MARK_STORY_READ,
    
    /**
     * Dispatched before marking voice message as listened.
     * Return true to block the listen notification.
     * Args: long messageId
     */
    BEFORE_MARK_VOICE_LISTENED,
    
    // ═══════════════════════════════════════════════════════════════
    // DATA VAULT EVENTS (GP-010 to GP-013)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched before deleting messages.
     * Allows capturing deleted messages for anti-recall.
     * Args: ArrayList<Integer> msgIds, long dialogId
     */
    BEFORE_DELETE_MESSAGES,
    
    /**
     * Dispatched when a message is edited.
     * Allows capturing edit history.
     * Args: Message oldMsg, Message newMsg
     */
    MESSAGE_EDITED,
    
    /**
     * Dispatched when processing incoming updates.
     * Allows intercepting deleted/edited messages from server.
     * Args: TLRPC.Updates updates
     */
    PROCESS_UPDATES,
    
    /**
     * Dispatched when a contact is updated.
     * Allows capturing profile snapshots.
     * Args: long userId, TLRPC.User user
     */
    CONTACT_UPDATED,
    
    /**
     * Dispatched when a user profile photo changes.
     * Args: long userId, TLRPC.Photo photo
     */
    PROFILE_PHOTO_CHANGED,
    
    /**
     * Dispatched when user status changes.
     * Args: long userId, TLRPC.UserStatus status
     */
    USER_STATUS_CHANGED,
    
    // ═══════════════════════════════════════════════════════════════
    // UI EVENTS (GP-020 to GP-024)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched when LaunchActivity is created.
     * Use for XORGram initialization.
     * Args: LaunchActivity activity
     */
    LAUNCH_ACTIVITY_CREATED,
    
    /**
     * Dispatched when ChatActivity view is created.
     * Return a View to add overlay to chat.
     * Args: ChatActivity chat
     */
    CREATE_CHAT_OVERLAY,
    
    /**
     * Dispatched when ProfileActivity view is created.
     * Return a View to add overlay to profile.
     * Args: ProfileActivity profile
     */
    CREATE_PROFILE_OVERLAY,
    
    /**
     * Dispatched when DialogsActivity view is created.
     * Return a View to add overlay to dialog list.
     * Args: DialogsActivity dialogs
     */
    CREATE_DIALOGS_OVERLAY,
    
    /**
     * Dispatched when options menu is created.
     * Return List<MenuItem> to add menu items.
     * Args: BaseFragment fragment, Menu menu
     */
    CREATE_MENU,
    
    /**
     * Dispatched when chat bubble is being drawn.
     * Allows customizing bubble appearance.
     * Args: MessageObject message, View bubbleView
     */
    DRAW_BUBBLE,
    
    /**
     * Dispatched when theme is being applied.
     * Args: String themeName
     */
    THEME_APPLIED,
    
    /**
     * Dispatched to get custom theme resources.
     * Args: String resourceName
     */
    GET_THEME_RESOURCE,
    
    // ═══════════════════════════════════════════════════════════════
    // MEDIA EVENTS (GP-030 to GP-032)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched before loading an image.
     * Return true to intercept and provide custom image.
     * Args: String url, ImageReceiver receiver
     */
    BEFORE_LOAD_IMAGE,
    
    /**
     * Dispatched before playing audio.
     * Return true to use custom player.
     * Args: MessageObject message
     */
    BEFORE_PLAY_AUDIO,
    
    /**
     * Dispatched before sending media.
     * Allows modifying media quality/settings.
     * Args: MessageObject message, TLRPC.InputMedia media
     */
    BEFORE_SEND_MEDIA,
    
    /**
     * Dispatched when video player is created.
     * Args: VideoPlayer player
     */
    VIDEO_PLAYER_CREATED,
    
    /**
     * Dispatched when audio playback state changes.
     * Args: int state, MessageObject message
     */
    AUDIO_PLAYBACK_STATE_CHANGED,
    
    // ═══════════════════════════════════════════════════════════════
    // SECURITY EVENTS (GP-040 to GP-042)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched to get device info string.
     * Return custom device info for spoofing.
     * Args: none
     */
    GET_DEVICE_INFO,
    
    /**
     * Dispatched to get client name.
     * Return custom client name for masking.
     * Args: none
     */
    GET_CLIENT_NAME,
    
    /**
     * Dispatched when app is going to background.
     * Use for biometric lock activation.
     * Args: none
     */
    APP_GOING_BACKGROUND,
    
    /**
     * Dispatched when app is coming to foreground.
     * Use for biometric unlock check.
     * Args: none
     */
    APP_COMING_FOREGROUND,
    
    /**
     * Dispatched when panic button triggered.
     * Args: none
     */
    PANIC_TRIGGERED,
    
    /**
     * Dispatched to check if biometric is required.
     * Return true to require biometric.
     * Args: none
     */
    REQUIRE_BIOMETRIC,
    
    // ═══════════════════════════════════════════════════════════════
    // SYNC EVENTS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched when settings change.
     * Use for syncing to other devices.
     * Args: String key, Object value
     */
    SETTINGS_CHANGED,
    
    /**
     * Dispatched to sync data.
     * Args: String dataType, byte[] data
     */
    SYNC_DATA,
    
    /**
     * Dispatched when receiving sync data.
     * Args: String dataType, byte[] data
     */
    ON_SYNC_DATA_RECEIVED,
    
    // ═══════════════════════════════════════════════════════════════
    // AI EVENTS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched to summarize chat.
     * Args: long dialogId, int messageCount
     */
    SUMMARIZE_CHAT,
    
    /**
     * Dispatched to transcribe voice message.
     * Args: MessageObject voiceMessage
     */
    TRANSCRIBE_VOICE,
    
    /**
     * Dispatched to generate smart reply.
     * Args: long dialogId, MessageObject message
     */
    GENERATE_SMART_REPLY,
    
    /**
     * Dispatched to translate text.
     * Args: String text, String targetLang
     */
    TRANSLATE_TEXT,
    
    // ═══════════════════════════════════════════════════════════════
    // ADMIN EVENTS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched when channel stats are loaded.
     * Args: long channelId, TLRPC.StatsURL stats
     */
    CHANNEL_STATS_LOADED,
    
    /**
     * Dispatched for auto-posting.
     * Args: long channelId, MessageObject message
     */
    AUTO_POST,
    
    /**
     * Dispatched for cross-posting.
     * Args: long sourceChannelId, long targetChannelId, MessageObject message
     */
    CROSS_POST,
    
    // ═══════════════════════════════════════════════════════════════
    // AUTOMATION EVENTS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched when new message received.
     * Use for automation rules.
     * Args: long dialogId, MessageObject message
     */
    MESSAGE_RECEIVED,
    
    /**
     * Dispatched for scheduled automation.
     * Args: String ruleId
     */
    SCHEDULED_AUTOMATION,
    
    /**
     * Dispatched for webhook trigger.
     * Args: String webhookUrl, String payload
     */
    WEBHOOK_TRIGGER,
    
    // ═══════════════════════════════════════════════════════════════
    // PLUGIN EVENTS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispatched to load plugin.
     * Args: String pluginId
     */
    LOAD_PLUGIN,
    
    /**
     * Dispatched to unload plugin.
     * Args: String pluginId
     */
    UNLOAD_PLUGIN,
    
    /**
     * Dispatched when plugin requests permission.
     * Args: String pluginId, String permission
     */
    PLUGIN_PERMISSION_REQUEST,
    
    /**
     * Dispatched to execute plugin script.
     * Args: String pluginId, String script
     */
    EXECUTE_PLUGIN_SCRIPT,
   
    // ═══════════════════════════════════════════════════════════════
    // ADDITIONAL AI EVENTS
    // ═══════════════════════════════════════════════════════════════
   
    /**
     * Dispatched when voice message received.
     * Args: long dialogId, MessageObject message
     */
    VOICE_MESSAGE_RECEIVED,
   
    /**
     * Dispatched for OpenAI API request.
     * Args: String prompt, String model
     */
    OPENAI_REQUEST,
   
    /**
     * Dispatched to get summary.
     * Args: long dialogId, int messageCount
     */
    GET_SUMMARY,
   
    // ═══════════════════════════════════════════════════════════════
    // ADDITIONAL ADMIN EVENTS
    // ═══════════════════════════════════════════════════════════════
   
    /**
     * Dispatched when chat is opened.
     * Args: long dialogId
     */
    CHAT_OPENED,
   
    /**
     * Dispatched when message is sent.
     * Args: long dialogId, MessageObject message
     */
    MESSAGE_SENT,
   
    /**
     * Dispatched when user joins chat.
     * Args: long chatId, long userId
     */
    USER_JOINED,
   
    /**
     * Dispatched when user leaves chat.
     * Args: long chatId, long userId
     */
    USER_LEFT,
   
    /**
     * Dispatched for admin command.
     * Args: long chatId, String command, String[] args
     */
    ADMIN_COMMAND,
   
    // ═══════════════════════════════════════════════════════════════
    // ADDITIONAL AUTOMATION EVENTS
    // ═══════════════════════════════════════════════════════════════
   
    /**
     * Dispatched when user online status changes.
     * Args: long userId, boolean online
     */
    USER_ONLINE,
   
    /**
     * Dispatched when user is typing.
     * Args: long dialogId, long userId
     */
    USER_TYPING,
   
    /**
     * Dispatched for scheduled action.
     * Args: String actionId, Object data
     */
    SCHEDULED_ACTION,
   
    // ═══════════════════════════════════════════════════════════════
    // ADDITIONAL PLUGIN EVENTS
    // ═══════════════════════════════════════════════════════════════
   
    /**
     * Dispatched when plugin is loaded.
     * Args: String pluginId
     */
    PLUGIN_LOADED,
   
    /**
     * Dispatched when plugin is unloaded.
     * Args: String pluginId
     */
    PLUGIN_UNLOADED,
   
    /**
     * Dispatched for plugin command.
     * Args: String pluginId, String command, String[] args
     */
    PLUGIN_COMMAND;
   
    /**
     * Get the category of this event.
     * @return Category name
     */
    public String getCategory() {
        if (this.ordinal() < BEFORE_DELETE_MESSAGES.ordinal()) {
            return "GHOST_ENGINE";
        } else if (this.ordinal() < LAUNCH_ACTIVITY_CREATED.ordinal()) {
            return "DATA_VAULT";
        } else if (this.ordinal() < BEFORE_LOAD_IMAGE.ordinal()) {
            return "UI";
        } else if (this.ordinal() < GET_DEVICE_INFO.ordinal()) {
            return "MEDIA";
        } else if (this.ordinal() < SETTINGS_CHANGED.ordinal()) {
            return "SECURITY";
        } else if (this.ordinal() < SUMMARIZE_CHAT.ordinal()) {
            return "SYNC";
        } else if (this.ordinal() < CHANNEL_STATS_LOADED.ordinal()) {
            return "AI";
        } else if (this.ordinal() < MESSAGE_RECEIVED.ordinal()) {
            return "ADMIN";
        } else if (this.ordinal() < LOAD_PLUGIN.ordinal()) {
            return "AUTOMATION";
        } else {
            return "PLUGIN";
        }
    }
}
