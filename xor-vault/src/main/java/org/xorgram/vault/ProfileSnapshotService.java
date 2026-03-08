package org.xorgram.vault;

import android.util.Log;

import org.xorgram.core.XORConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProfileSnapshotService - Captures profile changes over time.
 * 
 * Tracks changes to user profiles including:
 * - Name changes
 * - Username changes
 * - Bio changes
 * - Profile photo changes
 * - Phone number changes
 */
public class ProfileSnapshotService {
    
    private static final String TAG = "ProfileSnapshot";
    
    private final XORConfig config;
    private final Map<Long, List<ProfileSnapshot>> snapshots;
    private boolean running = false;
    
    public ProfileSnapshotService(XORConfig config) {
        this.config = config;
        this.snapshots = new HashMap<>();
    }
    
    /**
     * Start the service.
     */
    public void start() {
        running = true;
        Log.i(TAG, "ProfileSnapshotService started");
    }
    
    /**
     * Stop the service.
     */
    public void stop() {
        running = false;
        Log.i(TAG, "ProfileSnapshotService stopped");
    }
    
    /**
     * Destroy the service and free resources.
     */
    public void destroy() {
        stop();
        snapshots.clear();
    }
    
    /**
     * Capture a profile update.
     * 
     * @param userId The user ID
     * @param user The user object
     */
    public void captureProfile(long userId, Object user) {
        if (!running) return;
        
        ProfileSnapshot snapshot = new ProfileSnapshot();
        snapshot.userId = userId;
        snapshot.timestamp = System.currentTimeMillis();
        // In real implementation, extract data from user object
        
        snapshots.computeIfAbsent(userId, k -> new ArrayList<>()).add(snapshot);
        Log.d(TAG, "Captured profile snapshot for user: " + userId);
    }
    
    /**
     * Capture a profile photo change.
     * 
     * @param userId The user ID
     * @param photo The photo object
     */
    public void capturePhoto(long userId, Object photo) {
        if (!running) return;
        
        ProfileSnapshot snapshot = new ProfileSnapshot();
        snapshot.userId = userId;
        snapshot.timestamp = System.currentTimeMillis();
        snapshot.isPhotoChange = true;
        // In real implementation, save photo data
        
        snapshots.computeIfAbsent(userId, k -> new ArrayList<>()).add(snapshot);
        Log.d(TAG, "Captured photo change for user: " + userId);
    }
    
    /**
     * Get snapshots for a user.
     * 
     * @param userId The user ID
     * @return List of snapshots
     */
    public List<ProfileSnapshot> getSnapshots(long userId) {
        return snapshots.getOrDefault(userId, new ArrayList<>());
    }
    
    /**
     * Get total snapshot count.
     */
    public int getSnapshotCount() {
        int count = 0;
        for (List<ProfileSnapshot> list : snapshots.values()) {
            count += list.size();
        }
        return count;
    }
    
    /**
     * Clear all snapshots.
     */
    public void clear() {
        snapshots.clear();
        Log.i(TAG, "Cleared all profile snapshots");
    }
    
    /**
     * Profile snapshot data class.
     */
    public static class ProfileSnapshot {
        public long userId;
        public long timestamp;
        public String firstName;
        public String lastName;
        public String username;
        public String bio;
        public String phone;
        public boolean isPhotoChange;
        public byte[] photoData;
        
        /**
         * Get display name at this snapshot.
         */
        public String getDisplayName() {
            if (firstName == null && lastName == null) return "Unknown";
            if (firstName == null) return lastName;
            if (lastName == null) return firstName;
            return firstName + " " + lastName;
        }
        
        /**
         * Get a summary of changes.
         */
        public String getChangeSummary(ProfileSnapshot previous) {
            if (previous == null) return "Initial snapshot";
            
            List<String> changes = new ArrayList<>();
            
            if (!equals(firstName, previous.firstName) || !equals(lastName, previous.lastName)) {
                changes.add("name");
            }
            if (!equals(username, previous.username)) {
                changes.add("username");
            }
            if (!equals(bio, previous.bio)) {
                changes.add("bio");
            }
            if (!equals(phone, previous.phone)) {
                changes.add("phone");
            }
            if (isPhotoChange) {
                changes.add("photo");
            }
            
            return changes.isEmpty() ? "No changes" : String.join(", ", changes);
        }
        
        private boolean equals(String a, String b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
    }
}
