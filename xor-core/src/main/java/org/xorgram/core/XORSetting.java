package org.xorgram.core;

import android.view.View;

import java.util.List;

/**
 * XORSetting - Represents a setting item for module configuration.
 * 
 * Settings are displayed in the XORGram settings screen and can be
 * various types: toggles, dropdowns, text inputs, schedules, etc.
 */
public class XORSetting {
    
    public enum Type {
        TOGGLE, // Boolean switch
        BOOLEAN, // Boolean (alias for TOGGLE)
        DROPDOWN, // Single selection from list
        MULTI_SELECT, // Multiple selection from list
        TEXT_INPUT, // Free text input
        STRING, // String input (alias for TEXT_INPUT)
        NUMBER_INPUT, // Numeric input
        INTEGER, // Integer input (alias for NUMBER_INPUT)
        SCHEDULE, // Time/schedule picker
        COLOR_PICKER, // Color selection
        SLIDER, // Range slider
        BUTTON, // Action button
        HEADER, // Section header
        INFO // Informational text
    }
    
    private final String key;
    private final String title;
    private final String description;
    private final Type type;
    private Object defaultValue;
    private Object currentValue;
    private List<String> options;       // For dropdown/multi-select
    private int minValue;               // For slider/number
    private int maxValue;               // For slider/number
    private String icon;                // Icon resource name
    private boolean requiresRestart;    // Requires app restart to apply
    private boolean premium;            // Requires premium
    private boolean visible = true;     // Visibility state
    private boolean enabled = true;     // Enabled state
    private OnChangeListener onChangeListener;
    private View customView;            // Custom view for complex settings
    
    private XORSetting(Builder builder) {
    	this.key = builder.key;
    	this.title = builder.title;
    	this.description = builder.description;
    	this.type = builder.type;
    	this.defaultValue = builder.defaultValue;
    	this.currentValue = builder.currentValue;
    	this.options = builder.options;
    	this.minValue = builder.minValue;
    	this.maxValue = builder.maxValue;
    	this.icon = builder.icon;
    	this.requiresRestart = builder.requiresRestart;
    	this.premium = builder.premium;
    	this.visible = builder.visible;
    	this.enabled = builder.enabled;
    	this.onChangeListener = builder.onChangeListener;
    	this.customView = builder.customView;
    }
   
    /**
     * Convenience constructor for module settings.
     * Used by modules to create settings with module namespace.
     *
     * @param module Module ID (used as prefix for the key)
     * @param key Setting key
     * @param title Display title
     * @param description Description text
     * @param defaultValue Default value (Boolean, String, Integer, etc.)
     * @param type Setting type
     */
    public XORSetting(String module, String key, String title, String description, Object defaultValue, Type type) {
    	this.key = module + "_" + key;
    	this.title = title;
    	this.description = description;
    	this.type = type;
    	this.defaultValue = defaultValue;
    	this.currentValue = defaultValue;
    	this.options = null;
    	this.minValue = 0;
    	this.maxValue = 0;
    	this.icon = null;
    	this.requiresRestart = false;
    	this.premium = false;
    	this.visible = true;
    	this.enabled = true;
    	this.onChangeListener = null;
    	this.customView = null;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Static Factory Methods
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Create a toggle setting.
     */
    public static XORSetting toggle(String key, String title, boolean defaultValue) {
        return new Builder(key, title, Type.TOGGLE)
                .defaultValue(defaultValue)
                .currentValue(defaultValue)
                .build();
    }
    
    /**
     * Create a toggle setting with description.
     */
    public static XORSetting toggle(String key, String title, String description, boolean defaultValue) {
        return new Builder(key, title, Type.TOGGLE)
                .description(description)
                .defaultValue(defaultValue)
                .currentValue(defaultValue)
                .build();
    }
    
    /**
     * Create a dropdown setting.
     */
    public static XORSetting dropdown(String key, String title, List<String> options, String defaultValue) {
        return new Builder(key, title, Type.DROPDOWN)
                .options(options)
                .defaultValue(defaultValue)
                .currentValue(defaultValue)
                .build();
    }
    
    /**
     * Create a multi-select setting.
     */
    public static XORSetting multiSelect(String key, String title, List<String> options, List<String> defaultValues) {
        return new Builder(key, title, Type.MULTI_SELECT)
                .options(options)
                .defaultValue(defaultValues)
                .currentValue(defaultValues)
                .build();
    }
    
    /**
     * Create a text input setting.
     */
    public static XORSetting textInput(String key, String title, String defaultValue) {
        return new Builder(key, title, Type.TEXT_INPUT)
                .defaultValue(defaultValue)
                .currentValue(defaultValue)
                .build();
    }
    
    /**
     * Create a number input setting.
     */
    public static XORSetting numberInput(String key, String title, int defaultValue, int min, int max) {
        return new Builder(key, title, Type.NUMBER_INPUT)
                .defaultValue(defaultValue)
                .currentValue(defaultValue)
                .minValue(min)
                .maxValue(max)
                .build();
    }
    
    /**
     * Create a schedule setting.
     */
    public static XORSetting schedule(String key, String title, String defaultValue) {
        return new Builder(key, title, Type.SCHEDULE)
                .defaultValue(defaultValue)
                .currentValue(defaultValue)
                .build();
    }
    
    /**
     * Create a color picker setting.
     */
    public static XORSetting colorPicker(String key, String title, int defaultColor) {
        return new Builder(key, title, Type.COLOR_PICKER)
                .defaultValue(defaultColor)
                .currentValue(defaultColor)
                .build();
    }
    
    /**
     * Create a slider setting.
     */
    public static XORSetting slider(String key, String title, int defaultValue, int min, int max) {
        return new Builder(key, title, Type.SLIDER)
                .defaultValue(defaultValue)
                .currentValue(defaultValue)
                .minValue(min)
                .maxValue(max)
                .build();
    }
    
    /**
     * Create an action button.
     */
    public static XORSetting button(String key, String title) {
        return new Builder(key, title, Type.BUTTON).build();
    }
    
    /**
     * Create a section header.
     */
    public static XORSetting header(String title) {
        return new Builder("", title, Type.HEADER).build();
    }
    
    /**
     * Create an info text.
     */
    public static XORSetting info(String description) {
        return new Builder("", "", Type.INFO)
                .description(description)
                .build();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Getters
    // ═══════════════════════════════════════════════════════════════
    
    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Type getType() { return type; }
    public Object getDefaultValue() { return defaultValue; }
    public Object getCurrentValue() { return currentValue; }
    public List<String> getOptions() { return options; }
    public int getMinValue() { return minValue; }
    public int getMaxValue() { return maxValue; }
    public String getIcon() { return icon; }
    public boolean requiresRestart() { return requiresRestart; }
    public boolean isPremium() { return premium; }
    public boolean isVisible() { return visible; }
    public boolean isEnabled() { return enabled; }
    public View getCustomView() { return customView; }
    
    // ═══════════════════════════════════════════════════════════════
    // Setters
    // ═══════════════════════════════════════════════════════════════
    
    public void setCurrentValue(Object value) {
        this.currentValue = value;
        if (onChangeListener != null) {
            onChangeListener.onChange(this, value);
        }
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setOnChangeListener(OnChangeListener listener) {
        this.onChangeListener = listener;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Type-specific getters
    // ═══════════════════════════════════════════════════════════════
    
    public boolean getBooleanValue() {
        if (currentValue instanceof Boolean) {
            return (Boolean) currentValue;
        }
        return defaultValue instanceof Boolean ? (Boolean) defaultValue : false;
    }
    
    public String getStringValue() {
        if (currentValue instanceof String) {
            return (String) currentValue;
        }
        return defaultValue instanceof String ? (String) defaultValue : "";
    }
    
    public int getIntValue() {
        if (currentValue instanceof Integer) {
            return (Integer) currentValue;
        }
        return defaultValue instanceof Integer ? (Integer) defaultValue : 0;
    }
    
    public int getColorValue() {
        if (currentValue instanceof Integer) {
            return (Integer) currentValue;
        }
        return defaultValue instanceof Integer ? (Integer) defaultValue : 0;
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getListValue() {
        if (currentValue instanceof List) {
            return (List<String>) currentValue;
        }
        return defaultValue instanceof List ? (List<String>) defaultValue : null;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Listener Interface
    // ═══════════════════════════════════════════════════════════════
    
    public interface OnChangeListener {
        void onChange(XORSetting setting, Object newValue);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════
    
    public static class Builder {
        private final String key;
        private final String title;
        private final Type type;
        private String description;
        private Object defaultValue;
        private Object currentValue;
        private List<String> options;
        private int minValue;
        private int maxValue;
        private String icon;
        private boolean requiresRestart;
        private boolean premium;
        private boolean visible = true;
        private boolean enabled = true;
        private OnChangeListener onChangeListener;
        private View customView;
        
        public Builder(String key, String title, Type type) {
            this.key = key;
            this.title = title;
            this.type = type;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
        
        public Builder currentValue(Object currentValue) {
            this.currentValue = currentValue;
            return this;
        }
        
        public Builder options(List<String> options) {
            this.options = options;
            return this;
        }
        
        public Builder minValue(int minValue) {
            this.minValue = minValue;
            return this;
        }
        
        public Builder maxValue(int maxValue) {
            this.maxValue = maxValue;
            return this;
        }
        
        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }
        
        public Builder requiresRestart(boolean requiresRestart) {
            this.requiresRestart = requiresRestart;
            return this;
        }
        
        public Builder premium(boolean premium) {
            this.premium = premium;
            return this;
        }
        
        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder onChangeListener(OnChangeListener listener) {
            this.onChangeListener = listener;
            return this;
        }
        
        public Builder customView(View view) {
            this.customView = view;
            return this;
        }
        
        public XORSetting build() {
            return new XORSetting(this);
        }
    }
}
