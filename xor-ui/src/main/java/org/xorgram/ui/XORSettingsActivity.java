package org.xorgram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XORModuleManager;
import org.xorgram.sync.ConnectionMonitor;
import org.xorgram.sync.ResourceMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * XORSettingsActivity - Main settings dashboard for XORGram.
 * 
 * Features:
 * - AMOLED-optimized true black design with white accents
 * - Pulsing glow animations
 * - Connection speed monitoring
 * - Ping display
 * - Resource usage (CPU, RAM, Battery)
 * - Module configuration
 */
public class XORSettingsActivity extends Activity {

    // AMOLED Color Palette
    private static final String AMOLED_BLACK = "#000000";
    private static final String AMOLED_DARK = "#0D0D0D";
    private static final String AMOLED_CARD = "#1A1A1A";
    private static final String ACCENT_WHITE = "#FFFFFF";
    private static final String ACCENT_DIM = "#80FFFFFF";
    private static final String ACCENT_GLOW = "#33FFFFFF";
    private static final String STATUS_GOOD = "#4CAF50";
    private static final String STATUS_WARNING = "#FF9800";
    private static final String STATUS_BAD = "#F44336";

    private static final String TAG = "XORSettings";

    private LinearLayout rootLayout;
    private ScrollView scrollView;
    private LinearLayout contentLayout;

    // Header views
    private TextView titleView;
    private TextView versionView;
    private ImageView logoView;

    // Stats views
    private PulsingGlowView pingView;
    private PulsingGlowView speedView;
    private PulsingGlowView cpuView;
    private PulsingGlowView ramView;

    // Stats values
    private int currentPing = 42;
    private float currentSpeed = 0f; // Mbps
    private float cpuUsage = 0f;
    private float ramUsage = 0f;

    // Real monitors
    private ConnectionMonitor connectionMonitor;
    private ResourceMonitor resourceMonitor;
    private ConnectionMonitor.ConnectionListener connectionListener;
    private ResourceMonitor.ResourceListener resourceListener;

    private Handler statsHandler;
    private Runnable statsUpdater;
    private boolean isUpdating = true;
    private boolean useRealMonitors = true; // Set to false to use simulated data

    private XORConfig config;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        config = XORConfig.getInstance(this);

        // Setup AMOLED fullscreen
        setupAmoledWindow();

        // Create UI
        createUI();

        // Start stats monitoring
        startStatsMonitoring();
    }

    private void setupAmoledWindow() {
        // True black background
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor(AMOLED_BLACK));
        window.setNavigationBarColor(Color.parseColor(AMOLED_BLACK));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        // Light status bar icons (white on black)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int flags = window.getDecorView().getSystemUiVisibility();
            window.getDecorView().setSystemUiVisibility(flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void createUI() {
        // Root layout
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor(AMOLED_BLACK));
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // ScrollView
        scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        scrollView.setFillViewport(true);

        // Content layout
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(16), dp(48), dp(16), dp(32));
        scrollView.addView(contentLayout, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Create header
        createHeader();

        // Create stats row
        createStatsRow();

        // Create modules section
        createModulesSection();

        // Create settings sections
        createSettingsSections();

        rootLayout.addView(scrollView);
        setContentView(rootLayout);
    }

    private void createHeader() {
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setGravity(Gravity.CENTER);
        headerLayout.setPadding(0, dp(16), 0, dp(32));

        // Logo with glow effect
        logoView = new ImageView(this);
        logoView.setLayoutParams(new LinearLayout.LayoutParams(dp(80), dp(80)));
        logoView.setImageResource(android.R.drawable.ic_menu_send); // Placeholder
        startLogoGlowAnimation();

        // Title
        titleView = new TextView(this);
        titleView.setText("XORGram");
        titleView.setTextColor(Color.parseColor(ACCENT_WHITE));
        titleView.setTextSize(28);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, dp(16), 0, 0);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);

        // Version
        versionView = new TextView(this);
        versionView.setText("v1.0.0 • Modular Overlay");
        versionView.setTextColor(Color.parseColor(ACCENT_DIM));
        versionView.setTextSize(12);
        versionView.setGravity(Gravity.CENTER);
        versionView.setPadding(0, dp(4), 0, 0);

        headerLayout.addView(logoView);
        headerLayout.addView(titleView);
        headerLayout.addView(versionView);

        contentLayout.addView(headerLayout);
    }

    private void createStatsRow() {
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(4);
        statsRow.setPadding(0, dp(8), 0, dp(16));

        // Create 4 stat cards
        pingView = new PulsingGlowView(this);
        speedView = new PulsingGlowView(this);
        cpuView = new PulsingGlowView(this);
        ramView = new PulsingGlowView(this);

        LinearLayout.LayoutParams statParams = new LinearLayout.LayoutParams(0, dp(120));
        statParams.weight = 1;
        statParams.setMargins(dp(4), 0, dp(4), 0);

        statsRow.addView(pingView, statParams);
        statsRow.addView(speedView, statParams);
        statsRow.addView(cpuView, statParams);
        statsRow.addView(ramView, statParams);

        contentLayout.addView(statsRow);
    }

    private void createModulesSection() {
        // Section title
        TextView sectionTitle = createSectionTitle("MODULES");
        contentLayout.addView(sectionTitle);

        // Modules card
        LinearLayout modulesCard = createCard();
        
        // Add module entries
        addModuleEntry(modulesCard, "Ghost Mode", "Hide online status & read receipts", true);
        addModuleEntry(modulesCard, "Data Vault", "Anti-recall & edit history", true);
        addModuleEntry(modulesCard, "Security", "Panic button & fake PIN", false);
        addModuleEntry(modulesCard, "Automation", "Auto-reply & scheduled actions", false);
        addModuleEntry(modulesCard, "AI Features", "Smart replies & summarization", true);
        addModuleEntry(modulesCard, "Admin Tools", "Analytics & bulk operations", false);

        contentLayout.addView(modulesCard);
    }

    private void createSettingsSections() {
        // Appearance section
        TextView appearanceTitle = createSectionTitle("APPEARANCE");
        contentLayout.addView(appearanceTitle);

        LinearLayout appearanceCard = createCard();
        addSettingEntry(appearanceCard, "AMOLED Theme", "True black background", true, true);
        addSettingEntry(appearanceCard, "Pulsing Glow", "Animated accent effects", true, true);
        addSettingEntry(appearanceCard, "Custom Bubbles", "Message bubble styling", false, false);
        addSettingEntry(appearanceCard, "Icon Pack", "Custom app icons", false, false);
        contentLayout.addView(appearanceCard);

        // Privacy section
        TextView privacyTitle = createSectionTitle("PRIVACY");
        contentLayout.addView(privacyTitle);

        LinearLayout privacyCard = createCard();
        addSettingEntry(privacyCard, "Hide Typing", "Don't show typing status", true, true);
        addSettingEntry(privacyCard, "Hide Forward Tag", "Remove forwarded message tag", false, false);
        addSettingEntry(privacyCard, "Anti-Screenshot", "Prevent chat screenshots", false, false);
        addSettingEntry(privacyCard, "Secure Keyboard", "Use secure keyboard input", true, true);
        contentLayout.addView(privacyCard);

        // Connection section
        TextView connectionTitle = createSectionTitle("CONNECTION");
        contentLayout.addView(connectionTitle);

        LinearLayout connectionCard = createCard();
        addSettingEntry(connectionCard, "Show Ping", "Display connection latency", true, true);
        addSettingEntry(connectionCard, "Show Speed", "Display transfer speed", true, true);
        addSettingEntry(connectionCard, "Auto Reconnect", "Reconnect on disconnect", true, true);
        addSettingEntry(connectionCard, "Proxy Settings", "Configure connection proxy", false, false);
        contentLayout.addView(connectionCard);

        // Performance section
        TextView performanceTitle = createSectionTitle("PERFORMANCE");
        contentLayout.addView(performanceTitle);

        LinearLayout performanceCard = createCard();
        addSettingEntry(performanceCard, "Show CPU Usage", "Display CPU statistics", true, true);
        addSettingEntry(performanceCard, "Show RAM Usage", "Display memory usage", true, true);
        addSettingEntry(performanceCard, "Battery Saver", "Reduce animations", false, false);
        addSettingEntry(performanceCard, "Clear Cache", "Free up storage space", false, false);
        contentLayout.addView(performanceCard);
    }

    private TextView createSectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(Color.parseColor(ACCENT_DIM));
        title.setTextSize(12);
        title.setPadding(0, dp(24), 0, dp(8));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        return title;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor(AMOLED_CARD));
        card.setPadding(dp(16), dp(8), dp(16), dp(8));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor(AMOLED_CARD));
        background.setCornerRadius(dp(12));
        card.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);

        return card;
    }

    private void addModuleEntry(LinearLayout parent, String name, String description, boolean enabled) {
        LinearLayout entry = new LinearLayout(this);
        entry.setOrientation(LinearLayout.HORIZONTAL);
        entry.setGravity(Gravity.CENTER_VERTICAL);
        entry.setPadding(0, dp(12), 0, dp(12));

        // Text container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(Color.parseColor(ACCENT_WHITE));
        nameView.setTextSize(16);

        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextColor(Color.parseColor(ACCENT_DIM));
        descView.setTextSize(12);

        textContainer.addView(nameView);
        textContainer.addView(descView);

        // Status indicator
        View statusDot = new View(this);
        statusDot.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(8)));
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(enabled ? Color.parseColor(STATUS_GOOD) : Color.parseColor(ACCENT_DIM));
        statusDot.setBackground(dotDrawable);

        entry.addView(textContainer);
        entry.addView(statusDot);

        // Add divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor(AMOLED_DARK));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1
        ));

        parent.addView(entry);
        parent.addView(divider);
    }

    private void addSettingEntry(LinearLayout parent, String name, String description, boolean enabled, boolean hasSwitch) {
        LinearLayout entry = new LinearLayout(this);
        entry.setOrientation(LinearLayout.HORIZONTAL);
        entry.setGravity(Gravity.CENTER_VERTICAL);
        entry.setPadding(0, dp(12), 0, dp(12));

        // Text container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(Color.parseColor(ACCENT_WHITE));
        nameView.setTextSize(16);

        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextColor(Color.parseColor(ACCENT_DIM));
        descView.setTextSize(12);

        textContainer.addView(nameView);
        textContainer.addView(descView);

        entry.addView(textContainer);

        if (hasSwitch) {
            Switch toggle = new Switch(this);
            toggle.setChecked(enabled);
            toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(ACCENT_WHITE)));
            toggle.setTrackTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(ACCENT_DIM)));
            entry.addView(toggle);
        } else {
            ImageView arrow = new ImageView(this);
            arrow.setImageResource(android.R.drawable.ic_menu_more);
            arrow.setColorFilter(Color.parseColor(ACCENT_DIM));
            entry.addView(arrow);
        }

        // Add divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor(AMOLED_DARK));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1
        ));

        parent.addView(entry);
        parent.addView(divider);
    }

    private void startStatsMonitoring() {
        // Initialize real monitors
        initRealMonitors();

        statsHandler = new Handler(Looper.getMainLooper());
        statsUpdater = new Runnable() {
            @Override
            public void run() {
                if (!isUpdating) return;

                if (useRealMonitors && connectionMonitor != null && resourceMonitor != null) {
                    // Use real monitor data - listeners will update the views
                    // Just trigger a refresh here
                    updateStatsFromMonitors();
                } else {
                    // Simulate stats (fallback)
                    currentPing = 30 + (int)(Math.random() * 50);
                    currentSpeed = 10 + (float)(Math.random() * 90);
                    cpuUsage = 10 + (float)(Math.random() * 40);
                    ramUsage = 40 + (float)(Math.random() * 30);

                    // Update views
                    pingView.setData("PING", String.valueOf(currentPing), "ms", Math.min(currentPing / 100f, 1f));
                    speedView.setData("SPEED", String.format("%.1f", currentSpeed), "Mbps", currentSpeed / 100f);
                    cpuView.setData("CPU", String.format("%.0f", cpuUsage), "%", cpuUsage / 100f);
                    ramView.setData("RAM", String.format("%.0f", ramUsage), "%", ramUsage / 100f);
                }

                statsHandler.postDelayed(this, 2000);
            }
        };
        statsHandler.post(statsUpdater);
    }

    private void initRealMonitors() {
        try {
            // Initialize connection monitor
            connectionMonitor = new ConnectionMonitor(this, config);
            connectionListener = new ConnectionMonitor.ConnectionListener() {
                @Override
                public void onPingUpdated(int pingMs) {
                    runOnUiThread(() -> {
                        currentPing = pingMs;
                        float progress = Math.min(pingMs / 200f, 1f); // 0-200ms range
                        pingView.setData("PING", String.valueOf(pingMs), "ms", progress);
                    });
                }

                @Override
                public void onSpeedUpdated(float speedMbps) {
                    runOnUiThread(() -> {
                        currentSpeed = speedMbps;
                        float progress = Math.min(speedMbps / 100f, 1f); // 0-100 Mbps range
                        speedView.setData("SPEED", String.format("%.1f", speedMbps), "Mbps", progress);
                    });
                }

                @Override
                public void onConnectionChanged(ConnectionMonitor.ConnectionType type, ConnectionMonitor.ConnectionQuality quality) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Connection changed: " + type.getDisplayName() + " - " + quality.getDisplayName());
                    });
                }

                @Override
                public void onConnectionLost() {
                    runOnUiThread(() -> {
                        pingView.setData("PING", "--", "ms", 0f);
                        speedView.setData("SPEED", "--", "Mbps", 0f);
                    });
                }

                @Override
                public void onConnectionRestored() {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Connection restored");
                    });
                }
            };
            connectionMonitor.addListener(connectionListener);
            connectionMonitor.startMonitoring();

            // Initialize resource monitor
            resourceMonitor = new ResourceMonitor(this);
            resourceListener = new ResourceMonitor.ResourceListener() {
                @Override
                public void onCpuUpdated(float cpuPercent) {
                    runOnUiThread(() -> {
                        cpuUsage = cpuPercent;
                        cpuView.setData("CPU", String.format("%.0f", cpuPercent), "%", cpuPercent / 100f);
                    });
                }

                @Override
                public void onRamUpdated(float ramPercent, long usedMB, long totalMB) {
                    runOnUiThread(() -> {
                        ramUsage = ramPercent;
                        ramView.setData("RAM", String.format("%.0f", ramPercent), "%", ramPercent / 100f);
                    });
                }

                @Override
                public void onBatteryImpactUpdated(float impact) {
                    // Could display battery impact in a separate view
                    Log.d(TAG, "Battery impact: " + impact);
                }

                @Override
                public void onResourcesUpdated(ResourceMonitor.ResourceStats stats) {
                    // Combined update - already handled by individual callbacks
                }
            };
            resourceMonitor.addListener(resourceListener);
            resourceMonitor.startMonitoring();

            useRealMonitors = true;
            Log.d(TAG, "Real monitors initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize real monitors, using simulated data", e);
            useRealMonitors = false;
        }
    }

    private void updateStatsFromMonitors() {
        // Get current stats from monitors if available
        if (connectionMonitor != null) {
            int ping = connectionMonitor.getCurrentPing();
            float speed = connectionMonitor.getCurrentSpeed();
            if (ping > 0) {
                pingView.setData("PING", String.valueOf(ping), "ms", Math.min(ping / 200f, 1f));
            }
            if (speed > 0) {
                speedView.setData("SPEED", String.format("%.1f", speed), "Mbps", Math.min(speed / 100f, 1f));
            }
        }
        if (resourceMonitor != null) {
            ResourceMonitor.ResourceStats stats = resourceMonitor.getCurrentStats();
            if (stats != null) {
                cpuView.setData("CPU", String.format("%.0f", stats.cpuUsage), "%", stats.cpuUsage / 100f);
                ramView.setData("RAM", String.format("%.0f", stats.ramUsage), "%", stats.ramUsage / 100f);
            }
        }
    }

    private void startLogoGlowAnimation() {
        ValueAnimator glowAnimator = ValueAnimator.ofFloat(0.3f, 1f, 0.3f);
        glowAnimator.setDuration(2000);
        glowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            logoView.setAlpha(alpha);
        });
        glowAnimator.start();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isUpdating = false;
        if (statsHandler != null) {
            statsHandler.removeCallbacks(statsUpdater);
        }
        
        // Stop pulsing animations
        if (pingView != null) pingView.stopPulsing();
        if (speedView != null) speedView.stopPulsing();
        if (cpuView != null) cpuView.stopPulsing();
        if (ramView != null) ramView.stopPulsing();
        
        // Cleanup real monitors
        if (connectionMonitor != null) {
            connectionMonitor.removeListener(connectionListener);
            connectionMonitor.stopMonitoring();
        }
        if (resourceMonitor != null) {
            resourceMonitor.removeListener(resourceListener);
            resourceMonitor.stopMonitoring();
        }
    }
}
