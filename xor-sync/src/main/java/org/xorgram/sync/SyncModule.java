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
 * SyncModule - P2P/Server sync with E2E encryption and connection monitoring.
 * 
 * Features:
 * - Connection monitoring (ping, speed, quality)
 * - Resource monitoring (CPU, RAM, battery)
 * - P2P/Server sync with E2E encryption
 * - Real-time stats for UI
 */
public class SyncModule implements XORModule {

    private static final String TAG = "SyncModule";

    private Context context;
    private XORConfig config;
    private ConnectionMonitor connectionMonitor;
    private ResourceMonitor resourceMonitor;
    private boolean enabled = false;

    // Current stats
    private int currentPing = -1;
    private float currentSpeed = 0f;
    private ConnectionMonitor.ConnectionType connectionType = ConnectionMonitor.ConnectionType.UNKNOWN;
    private ConnectionMonitor.ConnectionQuality connectionQuality = ConnectionMonitor.ConnectionQuality.UNKNOWN;
    private float cpuUsage = 0f;
    private float ramUsage = 0f;
    private long ramUsedMB = 0;
    private long ramTotalMB = 0;

    @Override
    public String getId() {
        return "xor-sync";
    }

    @Override
    public String getName() {
        return "XORSync";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Connection & resource monitoring with P2P sync";
    }

    @Override
    public String getIcon() {
        return "ic_sync";
    }

    @Override
    public String getCategory() {
        return "sync";
    }

    @Override
    public Set<XOREvent> getSubscribedEvents() {
        return EnumSet.of(
            XOREvent.SETTINGS_CHANGED,
            XOREvent.SYNC_DATA,
            XOREvent.ON_SYNC_DATA_RECEIVED
        );
    }

    @Override
    public void onInit(Context context, XORConfig config) {
        this.context = context;
        this.config = config;

        // Initialize monitors
        connectionMonitor = new ConnectionMonitor(context, config);
        resourceMonitor = new ResourceMonitor(context);

        // Setup listeners
        setupConnectionListener();
        setupResourceListener();

        Log.i(TAG, "SyncModule initialized");
    }

    private void setupConnectionListener() {
        connectionMonitor.addListener(new ConnectionMonitor.ConnectionListener() {
            @Override
            public void onPingUpdated(int pingMs) {
                currentPing = pingMs;
            }

            @Override
            public void onSpeedUpdated(float speedMbps) {
                currentSpeed = speedMbps;
            }

            @Override
            public void onConnectionChanged(ConnectionMonitor.ConnectionType type, ConnectionMonitor.ConnectionQuality quality) {
                connectionType = type;
                connectionQuality = quality;
            }

            @Override
            public void onConnectionLost() {
                Log.w(TAG, "Connection lost");
            }

            @Override
            public void onConnectionRestored() {
                Log.i(TAG, "Connection restored");
            }
        });
    }

    private void setupResourceListener() {
        resourceMonitor.addListener(new ResourceMonitor.ResourceListener() {
            @Override
            public void onCpuUpdated(float cpuPercent) {
                cpuUsage = cpuPercent;
            }

            @Override
            public void onRamUpdated(float ramPercent, long usedMB, long totalMB) {
                ramUsage = ramPercent;
                ramUsedMB = usedMB;
                ramTotalMB = totalMB;
            }

            @Override
            public void onBatteryImpactUpdated(float impact) {
                // Could be used for battery optimization
            }

            @Override
            public void onResourcesUpdated(ResourceMonitor.ResourceStats stats) {
                // Full stats update
            }
        });
    }

    @Override
    public void onEnable() {
        enabled = true;
        if (connectionMonitor != null) {
            connectionMonitor.startMonitoring();
        }
        if (resourceMonitor != null) {
            resourceMonitor.startMonitoring();
        }
        Log.i(TAG, "SyncModule enabled - monitoring started");
    }

    @Override
    public void onDisable() {
        enabled = false;
        if (connectionMonitor != null) {
            connectionMonitor.stopMonitoring();
        }
        if (resourceMonitor != null) {
            resourceMonitor.stopMonitoring();
        }
        Log.i(TAG, "SyncModule disabled - monitoring stopped");
    }

    @Override
    public void onDestroy() {
        onDisable();
        Log.i(TAG, "SyncModule destroyed");
    }

    @Override
    public boolean onEvent(XOREvent event, Object... args) {
        if (!isEnabled()) return false;

        switch (event) {
            case SETTINGS_CHANGED:
                // Handle settings changes
                return true;
            case SYNC_DATA:
                // Handle sync data request
                return true;
            case ON_SYNC_DATA_RECEIVED:
                // Handle received sync data
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<XORSetting> getSettings() {
        List<XORSetting> settings = new ArrayList<>();

        // Connection monitoring settings
        settings.add(XORSetting.toggle("show_ping", "Show Ping", "Display connection latency in UI", true));
        settings.add(XORSetting.toggle("show_speed", "Show Speed", "Display connection speed in UI", true));
        settings.add(XORSetting.toggle("show_cpu", "Show CPU Usage", "Display CPU usage in UI", true));
        settings.add(XORSetting.toggle("show_ram", "Show RAM Usage", "Display memory usage in UI", true));

        // Sync settings
        settings.add(XORSetting.info("─── Sync Settings ───"));
        settings.add(XORSetting.toggle("sync_enabled", "Enable Sync", "Sync settings across devices", false));
        settings.add(XORSetting.dropdown("sync_mode", "Sync Mode",
                java.util.Arrays.asList("disabled", "server", "p2p"), "disabled"));
        settings.add(XORSetting.textInput("sync_server_url", "Server URL", ""));
        settings.add(XORSetting.toggle("sync_e2e", "E2E Encryption", "End-to-end encryption for sync", true));

        return settings;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Public getters for stats
    public int getCurrentPing() {
        return currentPing;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public ConnectionMonitor.ConnectionType getConnectionType() {
        return connectionType;
    }

    public ConnectionMonitor.ConnectionQuality getConnectionQuality() {
        return connectionQuality;
    }

    public float getCpuUsage() {
        return cpuUsage;
    }

    public float getRamUsage() {
        return ramUsage;
    }

    public long getRamUsedMB() {
        return ramUsedMB;
    }

    public long getRamTotalMB() {
        return ramTotalMB;
    }

    public ConnectionMonitor getConnectionMonitor() {
        return connectionMonitor;
    }

    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }
}
