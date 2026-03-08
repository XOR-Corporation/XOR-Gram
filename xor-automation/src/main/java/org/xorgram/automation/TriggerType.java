package org.xorgram.automation;

/**
 * Trigger types for automation
 */
public enum TriggerType {
    // User events
    USER_JOINED,
    USER_LEFT,
    USER_ONLINE,
    USER_TYPING,
    USER_SEEN,
    
    // Message events
    MESSAGE_RECEIVED,
    MESSAGE_SENT,
    MESSAGE_EDITED,
    MESSAGE_DELETED,
    MESSAGE_PINNED,
    
    // Chat events
    CHAT_OPENED,
    CHAT_CLOSED,
    CHAT_CREATED,
    CHAT_MUTED,
    CHAT_UNMUTED,
    
    // Media events
    MEDIA_RECEIVED,
    MEDIA_SENT,
    VOICE_RECEIVED,
    
    // Call events
    CALL_INCOMING,
    CALL_OUTGOING,
    CALL_MISSED,
    CALL_ENDED,
    
    // System events
    APP_STARTED,
    APP_FOREGROUND,
    APP_BACKGROUND,
    NETWORK_CHANGED,
    BATTERY_LOW,
    
    // Time events
    TIME_SCHEDULED,
    DATE_CHANGED,
    
    // Custom
    CUSTOM
}
