package org.xorgram.ui;

import org.xorgram.core.XORConfig;

/**
 * IconPackManager - Manages icon packs for XORGram.
 */
public class IconPackManager {
    
    private final XORConfig config;
    private String currentPack;
    
    public IconPackManager(XORConfig config) {
        this.config = config;
        this.currentPack = config.getString("ui_icon_pack", "default");
    }
    
    public String getCurrentPack() {
        return currentPack;
    }
    
    public void setIconPack(String pack) {
        this.currentPack = pack;
        config.putString("ui_icon_pack", pack);
    }
    
    public void destroy() {
        // Cleanup
    }
}
