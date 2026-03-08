package org.xorgram.security;

import android.os.Build;
import android.util.Log;

import org.xorgram.core.XORConfig;

import java.util.Random;

/**
 * ClientMasker - Masks XORGram identity from Telegram servers.
 * 
 * Spoofs:
 * - Device model
 * - Android version
 * - Client name
 * - App version
 * 
 * This makes XORGram appear as official Telegram client.
 */
public class ClientMasker {
    
    private static final String TAG = "ClientMasker";
    
    // Popular device models to spoof
    private static final String[] POPULAR_DEVICES = {
        "Samsung Galaxy S23",
        "Samsung Galaxy S22",
        "Samsung Galaxy A54",
        "Google Pixel 8",
        "Google Pixel 7",
        "Xiaomi 13",
        "OnePlus 11",
        "OPPO Find X5"
    };
    
    private final XORConfig config;
    private boolean active = false;
    private String spoofedDevice;
    private String spoofedClient;
    
    public ClientMasker(XORConfig config) {
        this.config = config;
    }
    
    /**
     * Activate client masking.
     */
    public void activate() {
        active = true;
        loadSpoofedValues();
        Log.i(TAG, "Client masking activated");
    }
    
    /**
     * Deactivate client masking.
     */
    public void deactivate() {
        active = false;
        Log.i(TAG, "Client masking deactivated");
    }
    
    /**
     * Check if masking is active.
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Get spoofed device info string.
     */
    public String getDeviceInfo() {
        if (!active) {
            return getRealDeviceInfo();
        }
        
        if (spoofedDevice != null && !spoofedDevice.isEmpty()) {
            return spoofedDevice;
        }
        
        // Generate random device info
        return generateRandomDeviceInfo();
    }
    
    /**
     * Get spoofed client name.
     */
    public String getClientName() {
        if (!active) {
            return "XORGram";
        }
        
        if (spoofedClient != null && !spoofedClient.isEmpty()) {
            return spoofedClient;
        }
        
        // Return official client name
        return "Telegram Android";
    }
    
    /**
     * Get real device info.
     */
    private String getRealDeviceInfo() {
        return Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";
    }
    
    /**
     * Generate random device info.
     */
    private String generateRandomDeviceInfo() {
        Random random = new Random();
        String device = POPULAR_DEVICES[random.nextInt(POPULAR_DEVICES.length)];
        int androidVersion = 12 + random.nextInt(4); // Android 12-15
        return device + " (Android " + androidVersion + ")";
    }
    
    /**
     * Load spoofed values from config.
     */
    private void loadSpoofedValues() {
        spoofedDevice = config.getString("sec_spoof_device", "");
        spoofedClient = config.getString("sec_spoof_client", "");
    }
    
    /**
     * Set custom spoofed device.
     */
    public void setSpoofedDevice(String device) {
        this.spoofedDevice = device;
        config.putString("sec_spoof_device", device);
    }
    
    /**
     * Set custom spoofed client name.
     */
    public void setSpoofedClient(String client) {
        this.spoofedClient = client;
        config.putString("sec_spoof_client", client);
    }
    
    /**
     * Reset to defaults.
     */
    public void reset() {
        active = false;
        spoofedDevice = null;
        spoofedClient = null;
        config.putString("sec_spoof_device", "");
        config.putString("sec_spoof_client", "");
    }
    
    /**
     * Destroy resources.
     */
    public void destroy() {
        active = false;
    }
}
