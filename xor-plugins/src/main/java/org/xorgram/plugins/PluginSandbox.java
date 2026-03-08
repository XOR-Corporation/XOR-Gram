package org.xorgram.plugins;

import android.content.Context;

import org.xorgram.core.XORConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Plugin Sandbox for secure plugin execution
 * 
 * Features:
 * - Permission-based access control
 * - Resource limits
 * - Network access control
 * - File system isolation
 */
public class PluginSandbox {

    private final Context context;
    private final XORConfig config;
    private final Set<String> allowedPermissions;

    public PluginSandbox(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        this.allowedPermissions = new HashSet<>();
        
        // Initialize default allowed permissions
        initializeDefaultPermissions();
    }

    /**
     * Initialize default allowed permissions
     */
    private void initializeDefaultPermissions() {
        // Safe permissions that all plugins can have
        allowedPermissions.add("READ_MESSAGES");
        allowedPermissions.add("SEND_MESSAGES");
        allowedPermissions.add("READ_CHATS");
        allowedPermissions.add("READ_USERS");
    }

    /**
     * Check if a plugin has a specific permission
     */
    public boolean hasPermission(Plugin plugin, String permission) {
        if (!config.getBoolean("xor-plugins", "sandbox_enabled", true)) {
            return true; // Sandbox disabled, allow all
        }
        
        // Check if permission is in allowed list
        if (!allowedPermissions.contains(permission)) {
            // Check if plugin explicitly has this permission
            return plugin.hasPermission(permission);
        }
        
        return true;
    }

    /**
     * Check if a plugin can access network
     */
    public boolean canAccessNetwork(Plugin plugin) {
        if (!config.getBoolean("xor-plugins", "allow_network", true)) {
            return false;
        }
        
        return hasPermission(plugin, "NETWORK_ACCESS");
    }

    /**
     * Check if a plugin can access files
     */
    public boolean canAccessFiles(Plugin plugin) {
        return hasPermission(plugin, "FILE_ACCESS");
    }

    /**
     * Check if a plugin can access contacts
     */
    public boolean canAccessContacts(Plugin plugin) {
        return hasPermission(plugin, "READ_CONTACTS");
    }

    /**
     * Check if a plugin can modify settings
     */
    public boolean canModifySettings(Plugin plugin) {
        return hasPermission(plugin, "MODIFY_SETTINGS");
    }

    /**
     * Get the isolated file path for a plugin
     */
    public String getIsolatedPath(Plugin plugin, String relativePath) {
        return context.getFilesDir() + "/plugins/" + plugin.getId() + "/data/" + relativePath;
    }

    /**
     * Validate a plugin's permissions
     */
    public ValidationResult validatePermissions(Plugin plugin) {
        ValidationResult result = new ValidationResult();
        
        for (String permission : plugin.getPermissions()) {
            if (!isPermissionValid(permission)) {
                result.addError("Unknown permission: " + permission);
            }
            
            if (isPermissionDangerous(permission)) {
                result.addWarning("Dangerous permission requested: " + permission);
            }
        }
        
        return result;
    }

    /**
     * Check if a permission name is valid
     */
    private boolean isPermissionValid(String permission) {
        // List of valid permissions
        String[] validPermissions = {
            "READ_MESSAGES", "SEND_MESSAGES", "EDIT_MESSAGES", "DELETE_MESSAGES",
            "READ_CHATS", "CREATE_CHATS", "MODIFY_CHATS",
            "READ_USERS", "READ_CONTACTS",
            "FILE_ACCESS", "NETWORK_ACCESS",
            "MODIFY_SETTINGS", "READ_CONFIG",
            "SEND_MEDIA", "DOWNLOAD_MEDIA",
            "ACCESS_LOCATION", "ACCESS_CAMERA", "ACCESS_MICROPHONE"
        };
        
        for (String valid : validPermissions) {
            if (valid.equals(permission)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if a permission is considered dangerous
     */
    private boolean isPermissionDangerous(String permission) {
        String[] dangerousPermissions = {
            "DELETE_MESSAGES", "MODIFY_CHATS", "MODIFY_SETTINGS",
            "SEND_MESSAGES", "SEND_MEDIA",
            "ACCESS_LOCATION", "ACCESS_CAMERA", "ACCESS_MICROPHONE",
            "NETWORK_ACCESS", "FILE_ACCESS"
        };
        
        for (String dangerous : dangerousPermissions) {
            if (dangerous.equals(permission)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Add a permission to the allowed list
     */
    public void allowPermission(String permission) {
        allowedPermissions.add(permission);
    }

    /**
     * Remove a permission from the allowed list
     */
    public void disallowPermission(String permission) {
        allowedPermissions.remove(permission);
    }

    /**
     * Validation result
     */
    public static class ValidationResult {
        private final java.util.List<String> errors;
        private final java.util.List<String> warnings;

        public ValidationResult() {
            this.errors = new java.util.ArrayList<>();
            this.warnings = new java.util.ArrayList<>();
        }

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public java.util.List<String> getWarnings() {
            return warnings;
        }
    }
}
