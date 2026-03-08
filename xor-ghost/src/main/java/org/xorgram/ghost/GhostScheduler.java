package org.xorgram.ghost;

import android.util.Log;

import org.xorgram.core.XORConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GhostScheduler - Manages scheduled ghost mode activation.
 * 
 * Supports cron-like schedule format:
 * - "always" - Always active
 * - "never" - Never active (manual only)
 * - "HH:mm-HH:mm" - Time range (e.g., "09:00-17:00" for work hours)
 * - "HH:mm-HH:mm,HH:mm-HH:mm" - Multiple time ranges
 * - "HH:mm-HH:mm:days" - Time range with specific days (e.g., "09:00-17:00:1-5" for weekdays)
 * 
 * Day numbers: 1=Monday, 7=Sunday
 */
public class GhostScheduler {
    
    private static final String TAG = "GhostScheduler";
    
    private final XORConfig config;
    private String schedule;
    private List<TimeRange> timeRanges;
    private boolean active;
    
    // ═══════════════════════════════════════════════════════════════
    // Constructor
    // ═══════════════════════════════════════════════════════════════
    
    public GhostScheduler(XORConfig config) {
        this.config = config;
        this.timeRanges = new ArrayList<>();
        this.schedule = config.getString("ghost_schedule", "");
        parseSchedule();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Schedule Management
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Update the schedule.
     * 
     * @param schedule New schedule string
     */
    public void updateSchedule(String schedule) {
        this.schedule = schedule;
        config.putString("ghost_schedule", schedule);
        parseSchedule();
    }
    
    /**
     * Parse the schedule string into time ranges.
     */
    private void parseSchedule() {
        timeRanges.clear();
        
        if (schedule == null || schedule.isEmpty() || "never".equals(schedule)) {
            active = false;
            return;
        }
        
        if ("always".equals(schedule)) {
            active = true;
            return;
        }
        
        // Parse time ranges
        String[] ranges = schedule.split(",");
        for (String range : ranges) {
            TimeRange tr = parseTimeRange(range.trim());
            if (tr != null) {
                timeRanges.add(tr);
            }
        }
        
        active = !timeRanges.isEmpty();
    }
    
    /**
     * Parse a single time range.
     */
    private TimeRange parseTimeRange(String range) {
        // Format: HH:mm-HH:mm or HH:mm-HH:mm:days
        Pattern pattern = Pattern.compile(
                "^(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})(?::(\\d+(?:-\\d+)*))?$"
        );
        
        Matcher matcher = pattern.matcher(range);
        if (!matcher.matches()) {
            Log.w(TAG, "Invalid time range format: " + range);
            return null;
        }
        
        try {
            int startHour = Integer.parseInt(matcher.group(1));
            int startMin = Integer.parseInt(matcher.group(2));
            int endHour = Integer.parseInt(matcher.group(3));
            int endMin = Integer.parseInt(matcher.group(4));
            
            List<Integer> days = null;
            if (matcher.group(5) != null) {
                days = parseDays(matcher.group(5));
            }
            
            return new TimeRange(startHour, startMin, endHour, endMin, days);
            
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid time range numbers: " + range);
            return null;
        }
    }
    
    /**
     * Parse day specification.
     */
    private List<Integer> parseDays(String daysStr) {
        List<Integer> days = new ArrayList<>();
        String[] parts = daysStr.split("-");
        
        if (parts.length == 1) {
            // Single day
            days.add(Integer.parseInt(parts[0]));
        } else if (parts.length == 2) {
            // Day range
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            for (int i = start; i <= end; i++) {
                days.add(i);
            }
        }
        
        return days;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Status Check
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Check if ghost mode should be active right now.
     * 
     * @return true if ghost mode should be active
     */
    public boolean isGhostActive() {
        if (!active) {
            return false;
        }
        
        if ("always".equals(schedule)) {
            return true;
        }
        
        if (timeRanges.isEmpty()) {
            return false;
        }
        
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMin = now.get(Calendar.MINUTE);
        int currentDay = now.get(Calendar.DAY_OF_WEEK);
        
        // Convert Calendar.DAY_OF_WEEK to our format (1=Monday, 7=Sunday)
        int dayOfWeek = currentDay == Calendar.SUNDAY ? 7 : currentDay - 1;
        
        for (TimeRange range : timeRanges) {
            if (range.contains(currentHour, currentMin, dayOfWeek)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the current schedule string.
     */
    public String getSchedule() {
        return schedule;
    }
    
    /**
     * Get a human-readable description of the schedule.
     */
    public String getDescription() {
        if (schedule == null || schedule.isEmpty() || "never".equals(schedule)) {
            return "Ghost mode is disabled";
        }
        
        if ("always".equals(schedule)) {
            return "Ghost mode is always active";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Ghost mode active: ");
        
        for (int i = 0; i < timeRanges.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(timeRanges.get(i).toString());
        }
        
        return sb.toString();
    }
    
    /**
     * Stop the scheduler.
     */
    public void stop() {
        active = false;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // TimeRange Helper Class
    // ═══════════════════════════════════════════════════════════════
    
    private static class TimeRange {
        final int startHour;
        final int startMin;
        final int endHour;
        final int endMin;
        final List<Integer> days;
        
        TimeRange(int startHour, int startMin, int endHour, int endMin, List<Integer> days) {
            this.startHour = startHour;
            this.startMin = startMin;
            this.endHour = endHour;
            this.endMin = endMin;
            this.days = days;
        }
        
        boolean contains(int hour, int min, int dayOfWeek) {
            // Check day if specified
            if (days != null && !days.isEmpty() && !days.contains(dayOfWeek)) {
                return false;
            }
            
            // Convert to minutes for easier comparison
            int current = hour * 60 + min;
            int start = startHour * 60 + startMin;
            int end = endHour * 60 + endMin;
            
            // Handle overnight ranges (e.g., 22:00-06:00)
            if (start > end) {
                return current >= start || current <= end;
            }
            
            return current >= start && current <= end;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%02d:%02d", startHour, startMin));
            sb.append("-");
            sb.append(String.format("%02d:%02d", endHour, endMin));
            
            if (days != null && !days.isEmpty()) {
                sb.append(" on ");
                if (days.size() == 7) {
                    sb.append("every day");
                } else if (days.size() == 5 && !days.contains(6) && !days.contains(7)) {
                    sb.append("weekdays");
                } else if (days.size() == 2 && days.contains(6) && days.contains(7)) {
                    sb.append("weekends");
                } else {
                    String[] dayNames = {"", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                    for (int i = 0; i < days.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(dayNames[days.get(i)]);
                    }
                }
            }
            
            return sb.toString();
        }
    }
}
