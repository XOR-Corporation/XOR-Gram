package org.xorgram.ui;

import android.util.Log;

import org.xorgram.core.XORConfig;

/**
 * ThemeEngine - Manages custom themes for XORGram.
 */
public class ThemeEngine {
    
    private static final String TAG = "ThemeEngine";
    
    private final XORConfig config;
    private String currentTheme;
    private boolean active = false;
    
    public ThemeEngine(XORConfig config) {
        this.config = config;
        this.currentTheme = config.getString("ui_theme", "default");
    }
    
    public void activate() {
        active = true;
        Log.i(TAG, "Theme engine activated with theme: " + currentTheme);
    }
    
    public void deactivate() {
        active = false;
        Log.i(TAG, "Theme engine deactivated");
    }
    
    public void applyTheme(String themeName) {
        this.currentTheme = themeName;
        config.putString("ui_theme", themeName);
        Log.i(TAG, "Applied theme: " + themeName);
    }
    
    public String getCurrentTheme() {
        return currentTheme;
    }
    
    public void destroy() {
        active = false;
    }
}
