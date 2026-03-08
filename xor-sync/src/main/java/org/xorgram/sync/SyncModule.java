package org.xorgram.sync;

import android.content.Context;
import android.util.Log;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;
import org.xorgram.core.XORModule;
import org.xorgram.core.XORSetting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * SyncModule - P2P/Server sync with E2E encryption.
 */
public class SyncModule implements XORModule {
    
    private static final String TAG = "SyncModule";
    private XORConfig config;
    private boolean enabled = false;
    
    @Override public String getId() { return "xor-sync"; }
    @Override public String getName() { return "XORSync"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getDescription() { return "P2P/Server sync with E2E encryption"; }
    @Override public String getIcon() { return "ic_sync"; }
    @Override public String getCategory() { return "sync"; }
    
    @Override
    public Set<XOREvent> getSubscribedEvents() {
        return EnumSet.of(XOREvent.SETTINGS_CHANGED, XOREvent.SYNC_DATA, XOREvent.ON_SYNC_DATA_RECEIVED);
    }
    
    @Override
    public void onInit(Context context, XORConfig config) {
        this.config = config;
        Log.i(TAG, "SyncModule initialized");
    }
    
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { enabled = false; }
    @Override public void onDestroy() { enabled = false; }
    
    @Override
    public boolean onEvent(XOREvent event, Object... args) {
        if (!isEnabled()) return false;
        return false;
    }
    
    @Override
    public List<XORSetting> getSettings() {
        List<XORSetting> settings = new ArrayList<>();
        settings.add(XORSetting.toggle("sync_enabled", "Enable Sync", false));
        settings.add(XORSetting.dropdown("sync_mode", "Mode", 
            java.util.Arrays.asList("disabled", "server", "p2p"), "disabled"));
        settings.add(XORSetting.textInput("sync_server_url", "Server URL", ""));
        settings.add(XORSetting.toggle("sync_e2e", "E2E Encryption", true));
        return settings;
    }
    
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
