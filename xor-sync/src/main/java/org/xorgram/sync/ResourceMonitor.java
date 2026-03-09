package org.xorgram.sync;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * ResourceMonitor - Real-time system resource monitoring for XORGram.
 * 
 * Features:
 * - CPU usage tracking
 * - RAM usage tracking
 * - Battery impact estimation
 * - Process-specific monitoring
 */
public class ResourceMonitor {

    private static final String TAG = "ResourceMonitor";

    private final Context context;
    private final ActivityManager activityManager;
    private final Handler mainHandler;

    private final List<ResourceListener> listeners = new ArrayList<>();

    // Current stats
    private float cpuUsage = 0f;
    private float ramUsage = 0f;
    private long ramUsedMB = 0;
    private long ramTotalMB = 0;
    private float batteryImpact = 0f;

    // CPU tracking
    private long lastCpuTime = 0;
    private long lastAppCpuTime = 0;
    private long lastUpdateTime = 0;

    // Monitoring state
    private boolean isMonitoring = false;
    private Handler monitoringHandler;
    private Runnable monitoringRunnable;

    public interface ResourceListener {
        void onCpuUpdated(float cpuPercent);
        void onRamUpdated(float ramPercent, long usedMB, long totalMB);
        void onBatteryImpactUpdated(float impact);
        void onResourcesUpdated(ResourceStats stats);
    }

    public static class ResourceStats {
        public final float cpuUsage;
        public final float ramUsage;
        public final long ramUsedMB;
        public final long ramTotalMB;
        public final float batteryImpact;
        public final long timestamp;

        public ResourceStats(float cpuUsage, float ramUsage, long ramUsedMB, long ramTotalMB, float batteryImpact) {
            this.cpuUsage = cpuUsage;
            this.ramUsage = ramUsage;
            this.ramUsedMB = ramUsedMB;
            this.ramTotalMB = ramTotalMB;
            this.batteryImpact = batteryImpact;
            this.timestamp = System.currentTimeMillis();
        }

        public String getCpuFormatted() {
            return String.format("%.1f%%", cpuUsage);
        }

        public String getRamFormatted() {
            return String.format("%.1f%%", ramUsage);
        }

        public String getRamMBFormatted() {
            return String.format("%d/%d MB", ramUsedMB, ramTotalMB);
        }

        public String getBatteryImpactFormatted() {
            if (batteryImpact < 0.3f) return "Low";
            if (batteryImpact < 0.7f) return "Medium";
            return "High";
        }
    }

    public ResourceMonitor(Context context) {
        this.context = context;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startMonitoring() {
        if (isMonitoring) return;
        isMonitoring = true;

        // Initialize CPU tracking
        lastCpuTime = getTotalCpuTime();
        lastAppCpuTime = getAppCpuTime();
        lastUpdateTime = System.currentTimeMillis();

        // Start periodic monitoring
        monitoringHandler = new Handler(Looper.getMainLooper());
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) return;

                updateStats();

                // Schedule next update (every 2 seconds)
                monitoringHandler.postDelayed(this, 2000);
            }
        };
        monitoringHandler.post(monitoringRunnable);

        Log.i(TAG, "Resource monitoring started");
    }

    public void stopMonitoring() {
        isMonitoring = false;

        if (monitoringHandler != null) {
            monitoringHandler.removeCallbacks(monitoringRunnable);
            monitoringHandler = null;
        }

        Log.i(TAG, "Resource monitoring stopped");
    }

    private void updateStats() {
        updateCpuUsage();
        updateRamUsage();
        updateBatteryImpact();

        ResourceStats stats = new ResourceStats(cpuUsage, ramUsage, ramUsedMB, ramTotalMB, batteryImpact);

        mainHandler.post(() -> {
            for (ResourceListener listener : listeners) {
                listener.onCpuUpdated(cpuUsage);
                listener.onRamUpdated(ramUsage, ramUsedMB, ramTotalMB);
                listener.onBatteryImpactUpdated(batteryImpact);
                listener.onResourcesUpdated(stats);
            }
        });
    }

    private void updateCpuUsage() {
        try {
            long currentTime = System.currentTimeMillis();
            long currentCpuTime = getTotalCpuTime();
            long currentAppCpuTime = getAppCpuTime();

            if (lastCpuTime > 0 && lastUpdateTime > 0) {
                long timeDiff = currentTime - lastUpdateTime;
                long cpuDiff = currentCpuTime - lastCpuTime;
                long appCpuDiff = currentAppCpuTime - lastAppCpuTime;

                if (cpuDiff > 0 && timeDiff > 0) {
                    // Calculate app's CPU usage percentage
                    cpuUsage = (appCpuDiff * 100f) / cpuDiff;
                    // Clamp to reasonable range
                    cpuUsage = Math.max(0, Math.min(100, cpuUsage));
                }
            }

            lastCpuTime = currentCpuTime;
            lastAppCpuTime = currentAppCpuTime;
            lastUpdateTime = currentTime;

        } catch (Exception e) {
            Log.e(TAG, "Error reading CPU stats", e);
            // Fallback: use ActivityManager for approximate CPU
            if (activityManager != null) {
                try {
                    android.app.ActivityManager.MemoryInfo memInfo = new android.app.ActivityManager.MemoryInfo();
                    activityManager.getMemoryInfo(memInfo);
                    // Approximate CPU based on available memory pressure
                    cpuUsage = memInfo.availMem < memInfo.totalMem / 4 ? 60f : 30f;
                } catch (Exception e2) {
                    cpuUsage = 0f;
                }
            }
        }
    }

    private void updateRamUsage() {
        if (activityManager == null) return;

        try {
            // Get memory info
            android.app.ActivityManager.MemoryInfo memInfo = new android.app.ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);

            ramTotalMB = memInfo.totalMem / (1024 * 1024);
            long availableMB = memInfo.availMem / (1024 * 1024);
            ramUsedMB = ramTotalMB - availableMB;
            ramUsage = ((float) ramUsedMB / ramTotalMB) * 100f;

            // Get app-specific memory usage using getProcessMemoryInfo
            int pid = Process.myPid();
            int[] pids = new int[]{pid};
            android.os.Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(pids);
            
            if (memoryInfos != null && memoryInfos.length > 0) {
                // Get total PSS (Proportional Set Size) for the app
                // This gives app-specific memory usage
                long totalPss = memoryInfos[0].getTotalPss() * 1024; // Convert KB to bytes
                // Could be used for more detailed app memory tracking
            }

        } catch (Exception e) {
            Log.e(TAG, "Error reading RAM stats", e);
        }
    }

    private void updateBatteryImpact() {
        // Estimate battery impact based on CPU and memory usage
        // Higher CPU and memory usage = higher battery drain
        float cpuWeight = 0.6f;
        float ramWeight = 0.4f;

        batteryImpact = (cpuUsage * cpuWeight + ramUsage * ramWeight) / 100f;
        batteryImpact = Math.max(0, Math.min(1, batteryImpact));
    }

    private long getTotalCpuTime() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String line = reader.readLine();
            reader.close();

            if (line != null) {
                String[] parts = line.split("\\s+");
                long total = 0;
                // Skip "cpu" prefix, sum all time values
                for (int i = 1; i < parts.length && i <= 8; i++) {
                    total += Long.parseLong(parts[i]);
                }
                return total;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading total CPU time", e);
        }
        return 0;
    }

    private long getAppCpuTime() {
        try {
            int pid = Process.myPid();
            RandomAccessFile reader = new RandomAccessFile("/proc/" + pid + "/stat", "r");
            String line = reader.readLine();
            reader.close();

            if (line != null) {
                String[] parts = line.split("\\s+");
                // utime + stime (positions 13 and 14 in /proc/[pid]/stat)
                long utime = Long.parseLong(parts[13]);
                long stime = Long.parseLong(parts[14]);
                return utime + stime;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading app CPU time", e);
        }
        return 0;
    }

    public void addListener(ResourceListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ResourceListener listener) {
        listeners.remove(listener);
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

    public float getBatteryImpact() {
        return batteryImpact;
    }

    public ResourceStats getCurrentStats() {
        return new ResourceStats(cpuUsage, ramUsage, ramUsedMB, ramTotalMB, batteryImpact);
    }
}
