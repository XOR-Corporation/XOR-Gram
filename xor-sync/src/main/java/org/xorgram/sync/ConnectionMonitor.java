package org.xorgram.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.xorgram.core.XORConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ConnectionMonitor - Real-time connection monitoring for XORGram.
 * 
 * Features:
 * - Ping measurement to Telegram servers
 * - Connection speed estimation
 * - Network type detection
 * - Connection quality scoring
 */
public class ConnectionMonitor {

    private static final String TAG = "ConnectionMonitor";

    // Telegram server endpoints for ping testing
    private static final String[] TELEGRAM_ENDPOINTS = {
        "149.154.167.50",  // Telegram DC1
        "149.154.167.51",  // Telegram DC2
        "149.154.175.50",  // Telegram DC3
        "149.154.175.100", // Telegram DC4
        "149.154.166.120", // Telegram DC5
    };

    private static final int PING_TIMEOUT_MS = 3000;
    private static final int SPEED_TEST_BYTES = 1024 * 100; // 100KB test

    private final Context context;
    private final XORConfig config;
    private final ConnectivityManager connectivityManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private final List<ConnectionListener> listeners = new ArrayList<>();
    private NetworkCallback networkCallback;

    // Current stats
    private int currentPing = -1;
    private float currentSpeed = 0f; // Mbps
    private ConnectionType connectionType = ConnectionType.UNKNOWN;
    private ConnectionQuality quality = ConnectionQuality.UNKNOWN;
    private boolean isConnected = false;

    // Monitoring state
    private boolean isMonitoring = false;
    private Handler monitoringHandler;
    private Runnable monitoringRunnable;

    public enum ConnectionType {
        WIFI("WiFi"),
        MOBILE_4G("4G/LTE"),
        MOBILE_3G("3G"),
        MOBILE_2G("2G"),
        ETHERNET("Ethernet"),
        UNKNOWN("Unknown");

        private final String displayName;
        ConnectionType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ConnectionQuality {
        EXCELLENT(100, "Excellent"),
        GOOD(70, "Good"),
        FAIR(40, "Fair"),
        POOR(20, "Poor"),
        DISCONNECTED(0, "Disconnected"),
        UNKNOWN(-1, "Unknown");

        private final int score;
        private final String displayName;
        ConnectionQuality(int score, String displayName) {
            this.score = score;
            this.displayName = displayName;
        }
        public int getScore() { return score; }
        public String getDisplayName() { return displayName; }
    }

    public interface ConnectionListener {
        void onPingUpdated(int pingMs);
        void onSpeedUpdated(float speedMbps);
        void onConnectionChanged(ConnectionType type, ConnectionQuality quality);
        void onConnectionLost();
        void onConnectionRestored();
    }

    public ConnectionMonitor(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.executor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startMonitoring() {
        if (isMonitoring) return;
        isMonitoring = true;

        // Register network callback
        if (connectivityManager != null) {
            NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

            networkCallback = new NetworkCallback();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        }

        // Start periodic monitoring
        monitoringHandler = new Handler(Looper.getMainLooper());
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) return;

                executor.execute(() -> {
                    measurePing();
                    estimateSpeed();
                    updateQuality();
                });

                // Schedule next update (every 5 seconds)
                monitoringHandler.postDelayed(this, 5000);
            }
        };
        monitoringHandler.post(monitoringRunnable);

        Log.i(TAG, "Connection monitoring started");
    }

    public void stopMonitoring() {
        isMonitoring = false;

        if (networkCallback != null && connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }

        if (monitoringHandler != null) {
            monitoringHandler.removeCallbacks(monitoringRunnable);
            monitoringHandler = null;
        }

        Log.i(TAG, "Connection monitoring stopped");
    }

    private void measurePing() {
        int bestPing = Integer.MAX_VALUE;

        for (String endpoint : TELEGRAM_ENDPOINTS) {
            try {
                long startTime = System.currentTimeMillis();
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(endpoint, 443), PING_TIMEOUT_MS);
                socket.close();
                int pingTime = (int) (System.currentTimeMillis() - startTime);

                if (pingTime < bestPing) {
                    bestPing = pingTime;
                }
            } catch (IOException e) {
                // Endpoint unreachable, try next
            }
        }

        if (bestPing == Integer.MAX_VALUE) {
            // All endpoints failed, try DNS ping
            try {
                long startTime = System.currentTimeMillis();
                InetAddress.getByName("telegram.org");
                bestPing = (int) (System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                bestPing = -1;
            }
        }

        currentPing = bestPing;
        mainHandler.post(() -> {
            for (ConnectionListener listener : listeners) {
                listener.onPingUpdated(currentPing);
            }
        });
    }

    private void estimateSpeed() {
        // Estimate based on connection type and ping
        float estimatedSpeed = 0f;

        switch (connectionType) {
            case WIFI:
                // WiFi: estimate based on ping (rough approximation)
                if (currentPing > 0) {
                    estimatedSpeed = Math.max(10, 150 - currentPing);
                }
                break;
            case MOBILE_4G:
                estimatedSpeed = currentPing > 0 ? Math.max(5, 80 - currentPing) : 20;
                break;
            case MOBILE_3G:
                estimatedSpeed = 2f;
                break;
            case MOBILE_2G:
                estimatedSpeed = 0.2f;
                break;
            case ETHERNET:
                estimatedSpeed = currentPing > 0 ? Math.max(50, 200 - currentPing) : 100;
                break;
            default:
                estimatedSpeed = 0f;
        }

        currentSpeed = estimatedSpeed;
        mainHandler.post(() -> {
            for (ConnectionListener listener : listeners) {
                listener.onSpeedUpdated(currentSpeed);
            }
        });
    }

    private void updateQuality() {
        ConnectionQuality newQuality;

        if (!isConnected) {
            newQuality = ConnectionQuality.DISCONNECTED;
        } else if (currentPing < 0) {
            newQuality = ConnectionQuality.UNKNOWN;
        } else if (currentPing < 50 && currentSpeed > 50) {
            newQuality = ConnectionQuality.EXCELLENT;
        } else if (currentPing < 100 && currentSpeed > 20) {
            newQuality = ConnectionQuality.GOOD;
        } else if (currentPing < 200 && currentSpeed > 5) {
            newQuality = ConnectionQuality.FAIR;
        } else {
            newQuality = ConnectionQuality.POOR;
        }

        if (newQuality != quality) {
            quality = newQuality;
            mainHandler.post(() -> {
                for (ConnectionListener listener : listeners) {
                    listener.onConnectionChanged(connectionType, quality);
                }
            });
        }
    }

    private void detectConnectionType(Network network) {
        if (connectivityManager == null) return;

        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
        if (caps == null) {
            connectionType = ConnectionType.UNKNOWN;
            return;
        }

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            connectionType = ConnectionType.WIFI;
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            connectionType = ConnectionType.ETHERNET;
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // Determine cellular generation
            int downSpeed = caps.getLinkDownstreamBandwidthKbps();
            int upSpeed = caps.getLinkUpstreamBandwidthKbps();

            if (downSpeed >= 10000) { // 10+ Mbps
                connectionType = ConnectionType.MOBILE_4G;
            } else if (downSpeed >= 1000) { // 1+ Mbps
                connectionType = ConnectionType.MOBILE_3G;
            } else {
                connectionType = ConnectionType.MOBILE_2G;
            }
        } else {
            connectionType = ConnectionType.UNKNOWN;
        }
    }

    public void addListener(ConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    public int getCurrentPing() {
        return currentPing;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public ConnectionQuality getQuality() {
        return quality;
    }

    public boolean isConnected() {
        return isConnected;
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            isConnected = true;
            detectConnectionType(network);
            mainHandler.post(() -> {
                for (ConnectionListener listener : listeners) {
                    listener.onConnectionRestored();
                }
            });
            Log.i(TAG, "Connection available: " + connectionType.getDisplayName());
        }

        @Override
        public void onLost(Network network) {
            isConnected = false;
            quality = ConnectionQuality.DISCONNECTED;
            mainHandler.post(() -> {
                for (ConnectionListener listener : listeners) {
                    listener.onConnectionLost();
                }
            });
            Log.i(TAG, "Connection lost");
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
            detectConnectionType(network);
            mainHandler.post(() -> {
                for (ConnectionListener listener : listeners) {
                    listener.onConnectionChanged(connectionType, quality);
                }
            });
        }
    }
}
