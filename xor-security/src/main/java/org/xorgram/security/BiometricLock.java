package org.xorgram.security;

import android.util.Log;

import org.xorgram.core.XORConfig;

/**
 * BiometricLock - Fingerprint/Face authentication lock.
 * 
 * Requires biometric authentication when app comes to foreground
 * after being in background.
 */
public class BiometricLock {
    
    private static final String TAG = "BiometricLock";
    
    private final XORConfig config;
    private boolean active = false;
    private boolean locked = false;
    private long lockTime = 0;
    
    public BiometricLock(XORConfig config) {
        this.config = config;
    }
    
    /**
     * Activate biometric lock.
     */
    public void activate() {
        active = true;
        Log.i(TAG, "Biometric lock activated");
    }
    
    /**
     * Deactivate biometric lock.
     */
    public void deactivate() {
        active = false;
        locked = false;
        Log.i(TAG, "Biometric lock deactivated");
    }
    
    /**
     * Check if biometric lock is active.
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Check if biometric is required.
     */
    public boolean isRequired() {
        return active && locked;
    }
    
    /**
     * Lock the app (called when going to background).
     */
    public void lock() {
        if (!active) return;
        
        locked = true;
        lockTime = System.currentTimeMillis();
        Log.d(TAG, "App locked");
    }
    
    /**
     * Unlock the app (called after successful auth).
     */
    public void unlock() {
        locked = false;
        Log.d(TAG, "App unlocked");
    }
    
    /**
     * Get the time since lock was engaged.
     */
    public long getLockDuration() {
        if (!locked) return 0;
        return System.currentTimeMillis() - lockTime;
    }
    
    /**
     * Reset the lock.
     */
    public void reset() {
        active = false;
        locked = false;
        lockTime = 0;
    }
    
    /**
     * Destroy resources.
     */
    public void destroy() {
        active = false;
        locked = false;
    }
}
