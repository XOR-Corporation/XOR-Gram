package org.xorgram.security;

import android.util.Log;

import org.xorgram.core.XORConfig;

/**
 * FakePINController - Decoy interface for plausible deniability.
 * 
 * When a fake PIN is entered, shows a decoy interface with
 * fake/limited data instead of real data.
 */
public class FakePINController {
    
    private static final String TAG = "FakePINController";
    
    private final XORConfig config;
    private boolean fakeModeActive = false;
    
    public FakePINController(XORConfig config) {
        this.config = config;
    }
    
    /**
     * Check if a PIN is the fake PIN.
     * 
     * @param pin The entered PIN
     * @return true if this is the fake PIN
     */
    public boolean isFakePIN(String pin) {
        String fakePIN = config.getString("sec_fake_pin", "");
        return !fakePIN.isEmpty() && fakePIN.equals(pin);
    }
    
    /**
     * Activate fake mode.
     */
    public void activateFakeMode() {
        fakeModeActive = true;
        Log.i(TAG, "Fake mode activated");
    }
    
    /**
     * Deactivate fake mode.
     */
    public void deactivateFakeMode() {
        fakeModeActive = false;
        Log.i(TAG, "Fake mode deactivated");
    }
    
    /**
     * Check if fake mode is active.
     */
    public boolean isFakeModeActive() {
        return fakeModeActive;
    }
    
    /**
     * Set the fake PIN.
     */
    public void setFakePIN(String pin) {
        config.putString("sec_fake_pin", pin);
    }
    
    /**
     * Reset the controller.
     */
    public void reset() {
        fakeModeActive = false;
        config.putString("sec_fake_pin", "");
    }
    
    /**
     * Destroy resources.
     */
    public void destroy() {
        fakeModeActive = false;
    }
}
