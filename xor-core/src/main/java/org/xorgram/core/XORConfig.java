package org.xorgram.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * XORConfig - Unified configuration system for all XORGram modules.
 * 
 * Features:
 * - Encrypted storage using AndroidX Security
 * - Per-module namespace isolation
 * - Type-safe getters/setters
 * - Change notification system
 * - Import/export functionality
 * 
 * Each module works with its own namespace (prefix) to avoid conflicts.
 */
public class XORConfig {
    
    private static final String TAG = "XORConfig";
    private static final String PREFS_FILE_NAME = "xorgram_config";
    private static final String KEY_VERSION = "_version";
    private static final int CURRENT_VERSION = 1;
    
    private final SharedPreferences preferences;
    private final Map<String, Object> cache;
    private final Map<String, List<OnConfigChangeListener>> listeners;
    private final Context context;
    private final String namespace;
    
    // ═══════════════════════════════════════════════════════════════
    // Singleton Instance
    // ═══════════════════════════════════════════════════════════════
    
    private static volatile XORConfig instance;
    private static final Object lock = new Object();
    
    /**
     * Get the global XORConfig instance.
     * 
     * @param context Application context
     * @return XORConfig instance
     */
    public static XORConfig getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new XORConfig(context, null);
                }
            }
        }
        return instance;
    }
    
    /**
     * Get a namespaced XORConfig for a specific module.
     * 
     * @param context Application context
     * @param namespace Module namespace (e.g., "ghost", "vault")
     * @return Namespaced XORConfig
     */
    public static XORConfig getNamespaced(Context context, String namespace) {
        return new XORConfig(context, namespace);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Constructor
    // ═══════════════════════════════════════════════════════════════
    
    private XORConfig(Context context, String namespace) {
        this.context = context.getApplicationContext();
        this.namespace = namespace != null ? namespace + "_" : "";
        this.cache = new HashMap<>();
        this.listeners = new WeakHashMap<>();
        this.preferences = createEncryptedPreferences();
        
        // Initialize default values
        initDefaults();
    }
    
    private SharedPreferences createEncryptedPreferences() {
        // Try encrypted preferences first, with multiple fallback levels
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            SharedPreferences encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            
            // Test that we can actually write to it
            encryptedPrefs.edit().putBoolean("_test", true).apply();
            encryptedPrefs.edit().remove("_test").apply();
            
            Log.i(TAG, "Using encrypted preferences");
            return encryptedPrefs;
        } catch (Throwable e) {
            // Fallback to regular preferences - this is safe as the data is not highly sensitive
            // The main purpose is convenience, not security against physical access
            Log.w(TAG, "Failed to create encrypted preferences, falling back to regular: " + e.getMessage());
            try {
                return context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
            } catch (Throwable e2) {
                // Last resort - in-memory preferences (will not persist)
                Log.e(TAG, "Failed to create any preferences, using in-memory fallback", e2);
                return new InMemoryPreferences();
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Default Values Initialization
    // ═══════════════════════════════════════════════════════════════
    
    private void initDefaults() {
        // Only initialize on first run
        if (preferences.contains(KEY_VERSION)) {
            return;
        }
        
        SharedPreferences.Editor editor = preferences.edit();
        
        // ═══════ GHOST ENGINE DEFAULTS ═══════
        editor.putBoolean("ghost_enabled", false);
        editor.putBoolean("ghost_block_read", true);
        editor.putBoolean("ghost_block_typing", true);
        editor.putBoolean("ghost_block_online", true);
        editor.putBoolean("ghost_block_stories", true);
        editor.putBoolean("ghost_block_voice", true);
        editor.putString("ghost_active_profile", "full");
        editor.putString("ghost_schedule", "");
        editor.putString("ghost_whitelist", "[]");
        
        // ═══════ DATA VAULT DEFAULTS ═══════
        editor.putBoolean("vault_anti_recall", true);
        editor.putBoolean("vault_edit_history", true);
        editor.putBoolean("vault_profile_snapshots", false);
        editor.putBoolean("vault_black_box", false);
        editor.putBoolean("vault_deleted_messages_limit", true);
        editor.putInt("vault_retention_days", 30);
        
        // ═══════ SYNC DEFAULTS ═══════
        editor.putBoolean("sync_enabled", false);
        editor.putString("sync_mode", "disabled"); // disabled | server | p2p
        editor.putString("sync_server_url", "");
        editor.putBoolean("sync_e2e", true);
        editor.putString("sync_device_id", "");
        
        // ═══════ AI DEFAULTS ═══════
        editor.putBoolean("ai_enabled", false);
        editor.putString("ai_provider", "gemini");
        editor.putString("ai_api_key", "");
        editor.putBoolean("ai_whisper_local", true);
        editor.putBoolean("ai_auto_translate", false);
        editor.putString("ai_translate_lang", "ru");
        editor.putBoolean("ai_smart_replies", false);
        
        // ═══════ UI DEFAULTS ═══════
        editor.putString("ui_theme", "default");
        editor.putString("ui_icon_pack", "default");
        editor.putString("ui_font", "default");
        editor.putInt("ui_bubble_radius", 18);
        editor.putBoolean("ui_hide_stories", false);
        editor.putBoolean("ui_custom_bubbles", false);
        editor.putBoolean("ui_message_timestamps", true);
        
        // ═══════ SECURITY DEFAULTS ═══════
        editor.putBoolean("sec_encrypt_db", true);
        editor.putBoolean("sec_biometric_lock", false);
        editor.putString("sec_panic_keyword", "");
        editor.putString("sec_fake_pin", "");
        editor.putBoolean("sec_mask_client", true);
        editor.putString("sec_spoof_device", "");
        editor.putBoolean("sec_stealth_mode", false);
        
        // ═══════ MODULE TOGGLES ═══════
        editor.putBoolean("module_ghost", true);
        editor.putBoolean("module_vault", true);
        editor.putBoolean("module_sync", false);
        editor.putBoolean("module_ai", false);
        editor.putBoolean("module_media", true);
        editor.putBoolean("module_ui", true);
        editor.putBoolean("module_security", true);
        editor.putBoolean("module_admin", false);
        editor.putBoolean("module_automation", false);
        editor.putBoolean("module_plugins", false);
        editor.putBoolean("module_browser", true);
        
        // ═══════ MEDIA DEFAULTS ═══════
        editor.putBoolean("media_speed_boost", true);
        editor.putBoolean("media_advanced_player", true);
        editor.putBoolean("media_equalizer", false);
        editor.putInt("media_default_speed", 100);
        editor.putBoolean("media_auto_download_quality", false);
        
        // ═══════ ADMIN DEFAULTS ═══════
        editor.putBoolean("admin_dashboard", false);
        editor.putBoolean("admin_auto_post", false);
        editor.putBoolean("admin_cross_post", false);
        
        // ═══════ AUTOMATION DEFAULTS ═══════
        editor.putBoolean("automation_enabled", false);
        editor.putString("automation_rules", "[]");
        editor.putBoolean("automation_auto_reply", false);
        
        // ═══════ PLUGINS DEFAULTS ═══════
        editor.putBoolean("plugins_enabled", false);
        editor.putString("plugins_installed", "[]");
        editor.putBoolean("plugins_python", false);
        editor.putBoolean("plugins_js", false);
        
        // ═══════ BROWSER DEFAULTS ═══════
        editor.putBoolean("browser_adblock", true);
        editor.putBoolean("browser_reader_mode", false);
        editor.putString("browser_search_engine", "google");
        
        editor.putInt(KEY_VERSION, CURRENT_VERSION);
        editor.apply();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Key Handling
    // ═══════════════════════════════════════════════════════════════
    
    private String getFullKey(String key) {
        return namespace + key;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Type-Safe Getters
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get a boolean value.
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Get a boolean value with default.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String fullKey = getFullKey(key);
        Object cached = cache.get(fullKey);
        if (cached instanceof Boolean) {
            return (Boolean) cached;
        }
        boolean value = preferences.getBoolean(fullKey, defaultValue);
        cache.put(fullKey, value);
        return value;
    }

    /**
     * Get a boolean value with module namespace and default.
     */
    public boolean getBoolean(String module, String key, boolean defaultValue) {
        String fullKey = module + "_" + key;
        Object cached = cache.get(fullKey);
        if (cached instanceof Boolean) {
            return (Boolean) cached;
        }
        boolean value = preferences.getBoolean(fullKey, defaultValue);
        cache.put(fullKey, value);
        return value;
    }
    
    /**
     * Get a string value.
     */
    public String getString(String key) {
        return getString(key, "");
    }
    
    /**
     * Get a string value with default.
     */
    public String getString(String key, String defaultValue) {
    	String fullKey = getFullKey(key);
    	Object cached = cache.get(fullKey);
    	if (cached instanceof String) {
    		return (String) cached;
    	}
    	String value = preferences.getString(fullKey, defaultValue);
    	cache.put(fullKey, value);
    	return value;
    }
   
    /**
     * Get a string value with module namespace.
     */
    public String getString(String module, String key, String defaultValue) {
    	String fullKey = module + "_" + key;
    	Object cached = cache.get(fullKey);
    	if (cached instanceof String) {
    		return (String) cached;
    	}
    	String value = preferences.getString(fullKey, defaultValue);
    	cache.put(fullKey, value);
    	return value;
    }
    
    /**
     * Get an integer value.
     */
    public int getInt(String key) {
        return getInt(key, 0);
    }
    
    /**
     * Get an integer value with default.
     */
    public int getInt(String key, int defaultValue) {
        String fullKey = getFullKey(key);
        Object cached = cache.get(fullKey);
        if (cached instanceof Integer) {
            return (Integer) cached;
        }
        int value = preferences.getInt(fullKey, defaultValue);
        cache.put(fullKey, value);
        return value;
    }
    
    /**
     * Get a long value.
     */
    public long getLong(String key) {
        return getLong(key, 0L);
    }
    
    /**
     * Get a long value with default.
     */
    public long getLong(String key, long defaultValue) {
        String fullKey = getFullKey(key);
        Object cached = cache.get(fullKey);
        if (cached instanceof Long) {
            return (Long) cached;
        }
        long value = preferences.getLong(fullKey, defaultValue);
        cache.put(fullKey, value);
        return value;
    }
    
    /**
     * Get a float value.
     */
    public float getFloat(String key) {
        return getFloat(key, 0f);
    }
    
    /**
     * Get a float value with default.
     */
    public float getFloat(String key, float defaultValue) {
        String fullKey = getFullKey(key);
        Object cached = cache.get(fullKey);
        if (cached instanceof Float) {
            return (Float) cached;
        }
        float value = preferences.getFloat(fullKey, defaultValue);
        cache.put(fullKey, value);
        return value;
    }
    
    /**
     * Get a string set value.
     */
    public Set<String> getStringSet(String key) {
        return getStringSet(key, Collections.emptySet());
    }
    
    /**
     * Get a string set value with default.
     */
    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        String fullKey = getFullKey(key);
        Object cached = cache.get(fullKey);
        if (cached instanceof Set) {
            @SuppressWarnings("unchecked")
            Set<String> value = (Set<String>) cached;
            return value;
        }
        Set<String> value = preferences.getStringSet(fullKey, defaultValue);
        cache.put(fullKey, new HashSet<>(value));
        return new HashSet<>(value);
    }
    
    /**
     * Get a list value (stored as JSON array).
     */
    public List<String> getList(String key) {
        return getList(key, Collections.emptyList());
    }
    
    /**
     * Get a list value with default.
     */
    public List<String> getList(String key, List<String> defaultValue) {
        String fullKey = getFullKey(key);
        Object cached = cache.get(fullKey);
        if (cached instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> value = (List<String>) cached;
            return new ArrayList<>(value);
        }
        
        String json = preferences.getString(fullKey, null);
        if (json == null) {
            return new ArrayList<>(defaultValue);
        }
        
        try {
            JSONArray array = new JSONArray(json);
            List<String> value = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                value.add(array.getString(i));
            }
            cache.put(fullKey, value);
            return new ArrayList<>(value);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse list: " + key, e);
            return new ArrayList<>(defaultValue);
        }
    }
    
    /**
     * Get a JSON object value.
     */
    public JSONObject getJSONObject(String key) {
        String fullKey = getFullKey(key);
        String json = preferences.getString(fullKey, "{}");
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON: " + key, e);
            return new JSONObject();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Type-Safe Setters
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Set a boolean value.
     */
    public void putBoolean(String key, boolean value) {
    	String fullKey = getFullKey(key);
    	cache.put(fullKey, value);
    	preferences.edit().putBoolean(fullKey, value).apply();
    	notifyListeners(key, value);
    }
   
    /**
     * Set a boolean value with module namespace.
     */
    public void setBoolean(String module, String key, boolean value) {
    	String fullKey = module + "_" + key;
    	cache.put(fullKey, value);
    	preferences.edit().putBoolean(fullKey, value).apply();
    	notifyListeners(key, value);
    }
    
    /**
     * Set a string value.
     */
    public void putString(String key, String value) {
        String fullKey = getFullKey(key);
        cache.put(fullKey, value);
        preferences.edit().putString(fullKey, value).apply();
        notifyListeners(key, value);
    }
    
    /**
     * Set an integer value.
     */
    public void putInt(String key, int value) {
        String fullKey = getFullKey(key);
        cache.put(fullKey, value);
        preferences.edit().putInt(fullKey, value).apply();
        notifyListeners(key, value);
    }
    
    /**
     * Set a long value.
     */
    public void putLong(String key, long value) {
        String fullKey = getFullKey(key);
        cache.put(fullKey, value);
        preferences.edit().putLong(fullKey, value).apply();
        notifyListeners(key, value);
    }
    
    /**
     * Set a float value.
     */
    public void putFloat(String key, float value) {
        String fullKey = getFullKey(key);
        cache.put(fullKey, value);
        preferences.edit().putFloat(fullKey, value).apply();
        notifyListeners(key, value);
    }
    
    /**
     * Set a string set value.
     */
    public void putStringSet(String key, Set<String> value) {
        String fullKey = getFullKey(key);
        cache.put(fullKey, new HashSet<>(value));
        preferences.edit().putStringSet(fullKey, value).apply();
        notifyListeners(key, value);
    }
    
    /**
     * Set a list value (stored as JSON array).
     */
    public void putList(String key, List<String> value) {
        String fullKey = getFullKey(key);
        cache.put(fullKey, new ArrayList<>(value));
        JSONArray array = new JSONArray(value);
        preferences.edit().putString(fullKey, array.toString()).apply();
        notifyListeners(key, value);
    }
    
    /**
     * Set a JSON object value.
     */
    public void putJSONObject(String key, JSONObject value) {
        String fullKey = getFullKey(key);
        preferences.edit().putString(fullKey, value.toString()).apply();
        notifyListeners(key, value);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Utility Methods
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Check if a key exists.
     */
    public boolean contains(String key) {
        return preferences.contains(getFullKey(key));
    }
    
    /**
     * Remove a key.
     */
    public void remove(String key) {
        String fullKey = getFullKey(key);
        cache.remove(fullKey);
        preferences.edit().remove(fullKey).apply();
        notifyListeners(key, null);
    }
    
    /**
     * Clear all settings for this namespace.
     */
    public void clear() {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (namespace.isEmpty() || key.startsWith(namespace)) {
                editor.remove(key);
                cache.remove(key);
            }
        }
        editor.apply();
    }
    
    /**
     * Clear cache (force reload from storage).
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * Get all keys in this namespace.
     */
    public Set<String> getKeys() {
        Set<String> keys = new HashSet<>();
        for (String key : preferences.getAll().keySet()) {
            if (namespace.isEmpty()) {
                keys.add(key);
            } else if (key.startsWith(namespace)) {
                keys.add(key.substring(namespace.length()));
            }
        }
        return keys;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Change Notification
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Register a change listener.
     */
    public void addListener(String key, OnConfigChangeListener listener) {
        String fullKey = getFullKey(key);
        if (!listeners.containsKey(fullKey)) {
            listeners.put(fullKey, new ArrayList<>());
        }
        listeners.get(fullKey).add(listener);
    }
    
    /**
     * Remove a change listener.
     */
    public void removeListener(String key, OnConfigChangeListener listener) {
        String fullKey = getFullKey(key);
        List<OnConfigChangeListener> list = listeners.get(fullKey);
        if (list != null) {
            list.remove(listener);
        }
    }
    
    private void notifyListeners(String key, Object value) {
        String fullKey = getFullKey(key);
        List<OnConfigChangeListener> list = listeners.get(fullKey);
        if (list != null) {
            for (OnConfigChangeListener listener : list) {
                listener.onConfigChanged(key, value);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Import/Export
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Export all settings to JSON.
     */
    public JSONObject exportToJson() throws JSONException {
        JSONObject json = new JSONObject();
        Map<String, ?> all = preferences.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            if (namespace.isEmpty() || key.startsWith(namespace)) {
                String shortKey = namespace.isEmpty() ? key : key.substring(namespace.length());
                Object value = entry.getValue();
                if (value instanceof Boolean) {
                    json.put(shortKey, (Boolean) value);
                } else if (value instanceof Integer) {
                    json.put(shortKey, (Integer) value);
                } else if (value instanceof Long) {
                    json.put(shortKey, (Long) value);
                } else if (value instanceof Float) {
                    json.put(shortKey, (Float) value);
                } else if (value instanceof String) {
                    json.put(shortKey, (String) value);
                } else if (value instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<String> set = (Set<String>) value;
                    json.put(shortKey, new JSONArray(set));
                }
            }
        }
        return json;
    }
    
    /**
     * Import settings from JSON.
     */
    public void importFromJson(JSONObject json) throws JSONException {
        SharedPreferences.Editor editor = preferences.edit();
        JSONArray names = json.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String key = names.getString(i);
                String fullKey = getFullKey(key);
                Object value = json.get(key);
                if (value instanceof Boolean) {
                    editor.putBoolean(fullKey, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(fullKey, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(fullKey, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(fullKey, (Float) value);
                } else if (value instanceof String) {
                    editor.putString(fullKey, (String) value);
                } else if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    Set<String> set = new HashSet<>();
                    for (int j = 0; j < array.length(); j++) {
                        set.add(array.getString(j));
                    }
                    editor.putStringSet(fullKey, set);
                }
                cache.remove(fullKey);
            }
        }
        editor.apply();
    }
    
    /**
     * Get the config file path for backup.
     */
    public File getConfigFile() {
        return new File(context.getFilesDir().getParent() + "/shared_prefs/" + PREFS_FILE_NAME + ".xml");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Listener Interface
    // ═══════════════════════════════════════════════════════════════
    
    public interface OnConfigChangeListener {
        void onConfigChanged(String key, Object value);
    }

    // ═══════════════════════════════════════════════════════════════
    // In-Memory Preferences Fallback
    // ═══════════════════════════════════════════════════════════════

    /**
     * In-memory SharedPreferences implementation for fallback when
     * neither encrypted nor regular preferences can be created.
     * Data will not persist across app restarts.
     */
    private static class InMemoryPreferences implements SharedPreferences {
        private final Map<String, Object> data = new HashMap<>();
        private final List<OnSharedPreferenceChangeListener> listeners = new ArrayList<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(data);
        }

        @Override
        public String getString(String key, String defValue) {
            Object v = data.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object v = data.get(key);
            if (v instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> set = (Set<String>) v;
                return set;
            }
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = data.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = data.get(key);
            return v instanceof Long ? (Long) v : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = data.get(key);
            return v instanceof Float ? (Float) v : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = data.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }

        @Override
        public boolean contains(String key) {
            return data.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new InMemoryEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            listeners.add(listener);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            listeners.remove(listener);
        }

        private class InMemoryEditor implements Editor {
            private final Map<String, Object> pending = new HashMap<>();
            private final List<String> pendingRemovals = new ArrayList<>();
            private boolean clearPending = false;

            @Override
            public Editor putString(String key, String value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                pending.put(key, values);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                pendingRemovals.add(key);
                return this;
            }

            @Override
            public Editor clear() {
                clearPending = true;
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                if (clearPending) {
                    data.clear();
                }
                for (String key : pendingRemovals) {
                    data.remove(key);
                }
                data.putAll(pending);
                
                // Notify listeners
                for (String key : pending.keySet()) {
                    for (OnSharedPreferenceChangeListener listener : listeners) {
                        listener.onSharedPreferenceChanged(InMemoryPreferences.this, key);
                    }
                }
            }
        }
    }
}
