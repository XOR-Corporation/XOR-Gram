package org.xorgram.security;

import android.util.Log;

import org.xorgram.core.XORConfig;

/**
 * PanicButton - Emergency wipe functionality.
 * 
 * When triggered, performs configurable actions:
 * - Clear all XORGram data
 * - Clear Telegram cache
 * - Reset to default settings
 * - Optionally uninstall app
 */
public class PanicButton {
    
    private static final String TAG = "PanicButton";
    
    private final XORConfig config;
    private PanicAction action = PanicAction.CLEAR_DATA;
    
    public PanicButton(XORConfig config) {
        this.config = config;
    }
    
    /**
     * Set the panic action.
     */
    public void setAction(PanicAction action) {
        this.action = action;
    }
    
    /**
     * Execute the panic action.
     */
    public void execute() {
        Log.w(TAG, "Executing panic action: " + action);
        
        switch (action) {
            case CLEAR_DATA:
                clearXORGramData();
                break;
                
            case CLEAR_ALL:
                clearXORGramData();
                clearTelegramCache();
                break;
                
            case NUKE:
                clearXORGramData();
                clearTelegramCache();
                clearTelegramData();
                break;
        }
    }
    
    /**
     * Clear all XORGram data.
     */
    private void clearXORGramData() {
        Log.i(TAG, "Clearing XORGram data...");
        config.clear();
    }
    
    /**
     * Clear Telegram cache.
     */
    private void clearTelegramCache() {
        Log.i(TAG, "Clearing Telegram cache...");
        // In real implementation, would clear Telegram cache directory
    }
    
    /**
     * Clear all Telegram data (nuclear option).
     */
    private void clearTelegramData() {
        Log.w(TAG, "Clearing all Telegram data...");
        // In real implementation, would clear all Telegram data
        // and optionally trigger uninstall
    }
    
    /**
     * Reset panic button state.
     */
    public void reset() {
        action = PanicAction.CLEAR_DATA;
    }
    
    /**
     * Destroy resources.
     */
    public void destroy() {
        // Nothing to clean up
    }
    
    /**
     * Panic action types.
     */
    public enum PanicAction {
        CLEAR_DATA,     // Clear XORGram data only
        CLEAR_ALL,      // Clear XORGram + Telegram cache
        NUKE            // Clear everything
    }
}
