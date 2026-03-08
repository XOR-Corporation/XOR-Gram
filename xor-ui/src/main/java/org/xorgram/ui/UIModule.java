package org.xorgram.ui;

import android.content.Context;
import android.util.Log;
import android.view.View;

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
 * UIModule - UI/UX customization for XORGram.
 * 
 * Features:
 * - Custom themes
 * - Bubble customization
 * - Icon packs
 * - Font customization
 * - Theme store
 */
public class UIModule implements XORModule {
    
    private static final String TAG = "UIModule";
    
    private XORConfig config;
    private ThemeEngine themeEngine;
    private BubbleCustomizer bubbleCustomizer;
    private IconPackManager iconPackManager;
    private boolean enabled = false;
    private Context context;
    
    @Override
    public String getId() {
        return "xor-ui";
    }
    
    @Override
    public String getName() {
        return "UI/UX Engine";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Custom themes, bubbles, icons, and fonts";
    }
    
    @Override
    public String getIcon() {
        return "ic_theme";
    }
    
    @Override
    public String getCategory() {
        return "appearance";
    }
    
    @Override
    public Set<XOREvent> getSubscribedEvents() {
        return EnumSet.of(
                XOREvent.CREATE_CHAT_OVERLAY,
                XOREvent.CREATE_PROFILE_OVERLAY,
                XOREvent.CREATE_DIALOGS_OVERLAY,
                XOREvent.CREATE_MENU,
                XOREvent.THEME_APPLIED,
                XOREvent.DRAW_BUBBLE
        );
    }
    
    @Override
    public void onInit(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        
        themeEngine = new ThemeEngine(config);
        bubbleCustomizer = new BubbleCustomizer(config);
        iconPackManager = new IconPackManager(config);
        
        Log.i(TAG, "UIModule initialized");
    }
    
    @Override
    public void onEnable() {
        enabled = true;
        themeEngine.activate();
        Log.i(TAG, "UIModule enabled");
    }
    
    @Override
    public void onDisable() {
        enabled = false;
        themeEngine.deactivate();
        Log.i(TAG, "UIModule disabled");
    }
    
    @Override
    public void onDestroy() {
        enabled = false;
        themeEngine.destroy();
        bubbleCustomizer.destroy();
        iconPackManager.destroy();
        Log.i(TAG, "UIModule destroyed");
    }
    
    @Override
    public boolean onEvent(XOREvent event, Object... args) {
        if (!isEnabled()) return false;
        
        switch (event) {
            case THEME_APPLIED:
                themeEngine.applyTheme((String) args[0]);
                return false;
                
            case DRAW_BUBBLE:
                bubbleCustomizer.customizeBubble(args[0], args[1]);
                return false;
                
            default:
                return false;
        }
    }
    
    @Nullable
    @Override
    public View onCreateOverlay(XOREvent event, Object... args) {
        if (!isEnabled()) return null;
        
        switch (event) {
            case CREATE_CHAT_OVERLAY:
                return null; // Add chat overlay if needed
                
            case CREATE_PROFILE_OVERLAY:
                return null; // Add profile overlay if needed
                
            case CREATE_DIALOGS_OVERLAY:
                return null; // Add dialogs overlay if needed
                
            default:
                return null;
        }
    }
    
    @Override
    public List<XORSetting> getSettings() {
        List<XORSetting> settings = new ArrayList<>();
        
        settings.add(XORSetting.header("Themes"));
        
        settings.add(XORSetting.dropdown(
                "ui_theme",
                "Theme",
                java.util.Arrays.asList("default", "dark", "amoled", "custom"),
                config.getString("ui_theme", "default")
        ));
        
        settings.add(XORSetting.header("Bubbles"));
        
        settings.add(XORSetting.toggle(
                "ui_custom_bubbles",
                "Custom Bubbles",
                "Enable custom bubble styles",
                false
        ));
        
        settings.add(XORSetting.slider(
                "ui_bubble_radius",
                "Bubble Radius",
                config.getInt("ui_bubble_radius", 18),
                0, 30
        ));
        
        settings.add(XORSetting.header("Icons"));
        
        settings.add(XORSetting.dropdown(
                "ui_icon_pack",
                "Icon Pack",
                java.util.Arrays.asList("default", "material", "filled", "outline"),
                config.getString("ui_icon_pack", "default")
        ));
        
        settings.add(XORSetting.header("Fonts"));
        
        settings.add(XORSetting.dropdown(
                "ui_font",
                "Font",
                java.util.Arrays.asList("default", "roboto", "inter", "system"),
                config.getString("ui_font", "default")
        ));
        
        settings.add(XORSetting.header("Other"));
        
        settings.add(XORSetting.toggle(
                "ui_hide_stories",
                "Hide Stories Bar",
                "Hide the stories bar from chat list",
                false
        ));
        
        settings.add(XORSetting.toggle(
                "ui_message_timestamps",
                "Always Show Timestamps",
                "Show timestamps on all messages",
                true
        ));
        
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
    
    @Override
    public String getDebugInfo() {
        return "enabled=" + enabled + ", theme=" + config.getString("ui_theme", "default");
    }
    
    @Override
    public void reset() {
        config.putString("ui_theme", "default");
        config.putString("ui_icon_pack", "default");
        config.putString("ui_font", "default");
        config.putInt("ui_bubble_radius", 18);
        config.putBoolean("ui_custom_bubbles", false);
        config.putBoolean("ui_hide_stories", false);
    }
}
