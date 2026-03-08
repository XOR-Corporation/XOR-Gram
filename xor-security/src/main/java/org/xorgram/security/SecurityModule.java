package org.xorgram.security;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;
import org.xorgram.core.XORModule;
import org.xorgram.core.XORSetting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * SecurityModule - Security features for XORGram.
 * 
 * Features:
 * - Biometric lock (fingerprint/face)
 * - Panic button (quick wipe)
 * - Fake PIN (decoy mode)
 * - Client masking (hide XORGram identity)
 * - Device spoofing
 * - Stealth mode
 * 
 * @see PanicButton
 * @see FakePINController
 * @see BiometricLock
 * @see ClientMasker
 */
public class SecurityModule implements XORModule {
    
    private static final String TAG = "SecurityModule";
    
    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════
    
    private XORConfig config;
    private PanicButton panicButton;
    private FakePINController fakePINController;
    private BiometricLock biometricLock;
    private ClientMasker clientMasker;
    private boolean enabled = false;
    private Context context;
    
    // ═══════════════════════════════════════════════════════════════
    // Module Identification
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public String getId() {
        return "xor-security";
    }
    
    @Override
    public String getName() {
        return "Security Core";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Biometric lock, panic button, fake PIN, and client masking";
    }
    
    @Override
    public String getIcon() {
        return "ic_security";
    }
    
    @Override
    public String getCategory() {
        return "security";
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Event Subscription
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public Set<XOREvent> getSubscribedEvents() {
        return EnumSet.of(
                XOREvent.GET_DEVICE_INFO,
                XOREvent.GET_CLIENT_NAME,
                XOREvent.APP_GOING_BACKGROUND,
                XOREvent.APP_COMING_FOREGROUND,
                XOREvent.PANIC_TRIGGERED,
                XOREvent.REQUIRE_BIOMETRIC
        );
    }
    
    @Override
    public int getPriority() {
        return 110; // Highest priority for security
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public void onInit(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        
        // Initialize components
        panicButton = new PanicButton(config);
        fakePINController = new FakePINController(config);
        biometricLock = new BiometricLock(config);
        clientMasker = new ClientMasker(config);
        
        Log.i(TAG, "SecurityModule initialized");
    }
    
    @Override
    public void onEnable() {
        enabled = true;
        
        if (config.getBoolean("sec_biometric_lock", false)) {
            biometricLock.activate();
        }
        
        if (config.getBoolean("sec_mask_client", true)) {
            clientMasker.activate();
        }
        
        Log.i(TAG, "SecurityModule enabled");
    }
    
    @Override
    public void onDisable() {
        enabled = false;
        
        biometricLock.deactivate();
        clientMasker.deactivate();
        
        Log.i(TAG, "SecurityModule disabled");
    }
    
    @Override
    public void onDestroy() {
        enabled = false;
        
        panicButton.destroy();
        fakePINController.destroy();
        biometricLock.destroy();
        clientMasker.destroy();
        
        Log.i(TAG, "SecurityModule destroyed");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Event Handling
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public boolean onEvent(XOREvent event, Object... args) {
        if (!isEnabled()) {
            return false;
        }
        
        switch (event) {
            case GET_DEVICE_INFO:
                return false; // Handled in onEventWithResult
                
            case GET_CLIENT_NAME:
                return false; // Handled in onEventWithResult
                
            case APP_GOING_BACKGROUND:
                handleAppBackground();
                return false;
                
            case APP_COMING_FOREGROUND:
                handleAppForeground();
                return false;
                
            case PANIC_TRIGGERED:
                handlePanic();
                return false;
                
            case REQUIRE_BIOMETRIC:
                return biometricLock.isRequired();
                
            default:
                return false;
        }
    }
    
    @Nullable
    @Override
    public Object onEventWithResult(XOREvent event, Object... args) {
        switch (event) {
            case GET_DEVICE_INFO:
                if (config.getBoolean("sec_mask_client", true)) {
                    return clientMasker.getDeviceInfo();
                }
                return null;
                
            case GET_CLIENT_NAME:
                if (config.getBoolean("sec_mask_client", true)) {
                    return clientMasker.getClientName();
                }
                return null;
                
            default:
                return null;
        }
    }
    
    private void handleAppBackground() {
        if (config.getBoolean("sec_biometric_lock", false)) {
            biometricLock.lock();
        }
    }
    
    private void handleAppForeground() {
        if (config.getBoolean("sec_biometric_lock", false)) {
            biometricLock.unlock();
        }
    }
    
    private void handlePanic() {
        Log.w(TAG, "Panic triggered!");
        panicButton.execute();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Settings
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public List<XORSetting> getSettings() {
        List<XORSetting> settings = new ArrayList<>();
        
        // Main toggle
        settings.add(XORSetting.toggle(
                "sec_enabled",
                "Security Features",
                "Enable security features",
                true
        ));
        
        settings.add(XORSetting.header("Biometric Lock"));
        
        settings.add(XORSetting.toggle(
                "sec_biometric_lock",
                "Biometric Lock",
                "Require fingerprint/face to open app",
                false
        ));
        
        settings.add(XORSetting.info(
                "Biometric lock activates when app goes to background"
        ));
        
        settings.add(XORSetting.header("Panic Button"));
        
        settings.add(XORSetting.textInput(
                "sec_panic_keyword",
                "Panic Keyword",
                ""
        ));
        
        settings.add(XORSetting.info(
                "Type this keyword in any chat to trigger panic mode"
        ));
        
        settings.add(XORSetting.header("Fake PIN"));
        
        settings.add(XORSetting.textInput(
                "sec_fake_pin",
                "Fake PIN",
                ""
        ));
        
        settings.add(XORSetting.info(
                "Enter this PIN to show a decoy interface instead of real data"
        ));
        
        settings.add(XORSetting.header("Client Masking"));
        
        settings.add(XORSetting.toggle(
                "sec_mask_client",
                "Mask Client Identity",
                "Hide XORGram from Telegram servers",
                true
        ));
        
        settings.add(XORSetting.toggle(
                "sec_stealth_mode",
                "Stealth Mode",
                "Hide app icon from launcher (dial code to open)",
                false
        ));
        
        settings.add(XORSetting.header("Device Spoofing"));
        
        settings.add(XORSetting.textInput(
                "sec_spoof_device",
                "Device Model",
                ""
        ));
        
        settings.add(XORSetting.info(
                "Leave empty to use your real device model"
        ));
        
        return settings;
    }
    
    @Override
    public void onSettingChanged(String key, Object value) {
        switch (key) {
            case "sec_biometric_lock":
                if ((boolean) value) {
                    biometricLock.activate();
                } else {
                    biometricLock.deactivate();
                }
                break;
                
            case "sec_mask_client":
                if ((boolean) value) {
                    clientMasker.activate();
                } else {
                    clientMasker.deactivate();
                }
                break;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get the panic button controller.
     */
    public PanicButton getPanicButton() {
        return panicButton;
    }
    
    /**
     * Get the fake PIN controller.
     */
    public FakePINController getFakePINController() {
        return fakePINController;
    }
    
    /**
     * Get the biometric lock controller.
     */
    public BiometricLock getBiometricLock() {
        return biometricLock;
    }
    
    /**
     * Get the client masker.
     */
    public ClientMasker getClientMasker() {
        return clientMasker;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Debug
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("enabled=").append(enabled).append(", ");
        sb.append("biometric=").append(biometricLock.isActive()).append(", ");
        sb.append("masking=").append(clientMasker.isActive());
        return sb.toString();
    }
    
    @Override
    public void reset() {
        panicButton.reset();
        fakePINController.reset();
        biometricLock.reset();
        clientMasker.reset();
    }
}
