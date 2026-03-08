package org.xorgram.ghost;

import org.xorgram.core.XORConfig;

/**
 * GhostProfile - Represents a ghost mode profile with specific settings.
 * 
 * Profiles allow users to quickly switch between different ghost mode configurations.
 * Built-in profiles: full, stealth, custom
 */
public class GhostProfile {
    
    private final String name;
    private final boolean blockRead;
    private final boolean blockTyping;
    private final boolean blockOnline;
    private final boolean blockStoryView;
    private final boolean blockVoice;
    private final boolean enabled;
    
    // ═══════════════════════════════════════════════════════════════
    // Constructors
    // ═══════════════════════════════════════════════════════════════
    
    private GhostProfile(String name, boolean blockRead, boolean blockTyping,
                         boolean blockOnline, boolean blockStoryView, boolean blockVoice, boolean enabled) {
        this.name = name;
        this.blockRead = blockRead;
        this.blockTyping = blockTyping;
        this.blockOnline = blockOnline;
        this.blockStoryView = blockStoryView;
        this.blockVoice = blockVoice;
        this.enabled = enabled;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Factory Methods
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Load a profile by name.
     * 
     * @param config XORConfig instance
     * @param name Profile name (full, stealth, custom)
     * @return GhostProfile instance
     */
    public static GhostProfile load(XORConfig config, String name) {
        switch (name) {
            case "full":
                return createFullProfile();
            case "stealth":
                return createStealthProfile();
            case "custom":
                return createCustomProfile(config);
            default:
                return createFullProfile();
        }
    }
    
    /**
     * Create a full ghost profile (all features enabled).
     */
    public static GhostProfile createFullProfile() {
        return new GhostProfile("full", true, true, true, true, true, true);
    }
    
    /**
     * Create a stealth profile (minimal blocking).
     */
    public static GhostProfile createStealthProfile() {
        return new GhostProfile("stealth", true, false, true, false, false, true);
    }
    
    /**
     * Create a custom profile from config.
     */
    public static GhostProfile createCustomProfile(XORConfig config) {
        return new GhostProfile(
                "custom",
                config.getBoolean("ghost_block_read", true),
                config.getBoolean("ghost_block_typing", true),
                config.getBoolean("ghost_block_online", true),
                config.getBoolean("ghost_block_stories", true),
                config.getBoolean("ghost_block_voice", true),
                config.getBoolean("ghost_enabled", false)
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Getters
    // ═══════════════════════════════════════════════════════════════
    
    public String getName() {
        return name;
    }
    
    public boolean shouldBlockReadFor(long dialogId) {
        return blockRead;
    }
    
    public boolean isBlockTypingEnabled() {
        return blockTyping;
    }
    
    public boolean isBlockOnlineEnabled() {
        return blockOnline;
    }
    
    public boolean isBlockStoryViewEnabled() {
        return blockStoryView;
    }
    
    public boolean isBlockVoiceEnabled() {
        return blockVoice;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Profile Description
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get a human-readable description of this profile.
     */
    public String getDescription() {
        switch (name) {
            case "full":
                return "Complete invisibility - blocks all activity indicators";
            case "stealth":
                return "Minimal mode - only blocks read receipts and online status";
            case "custom":
                return "Custom profile with your own settings";
            default:
                return "Unknown profile";
        }
    }
    
    /**
     * Get a summary of what's blocked.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (blockRead) sb.append("Read receipts, ");
        if (blockTyping) sb.append("Typing, ");
        if (blockOnline) sb.append("Online, ");
        if (blockStoryView) sb.append("Stories, ");
        if (blockVoice) sb.append("Voice");
        
        String result = sb.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }
}
