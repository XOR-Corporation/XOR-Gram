package org.xorgram.vault;

import android.util.Log;

import org.xorgram.core.XORConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BlackBoxLogger - Comprehensive activity logging.
 * 
 * Logs all activity for debugging and analysis:
 * - Message events
 * - Status changes
 * - User actions
 * - System events
 * 
 * Logs are stored locally and never sent to any server.
 */
public class BlackBoxLogger {
    
    private static final String TAG = "BlackBoxLogger";
    private static final int MAX_LOG_SIZE = 10000;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    private final XORConfig config;
    private final List<LogEntry> logBuffer;
    private final SimpleDateFormat dateFormat;
    private File logFile;
    private FileWriter fileWriter;
    private boolean running = false;
    
    public BlackBoxLogger(XORConfig config) {
        this.config = config;
        this.logBuffer = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    }
    
    /**
     * Start the logger.
     */
    public void start() {
        running = true;
        Log.i(TAG, "BlackBoxLogger started");
    }
    
    /**
     * Stop the logger.
     */
    public void stop() {
        running = false;
        flush();
        closeFile();
        Log.i(TAG, "BlackBoxLogger stopped");
    }
    
    /**
     * Destroy the logger and free resources.
     */
    public void destroy() {
        stop();
        logBuffer.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Logging Methods
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Log a status change event.
     */
    public void logStatusChange(long userId, Object status) {
        log("STATUS", "User " + userId + " status changed: " + (status != null ? status.getClass().getSimpleName() : "offline"));
    }
    
    /**
     * Log a message event.
     */
    public void logMessageEvent(String event, long dialogId, int messageId) {
        log("MSG", event + " dialog=" + dialogId + " msg=" + messageId);
    }
    
    /**
     * Log a user action.
     */
    public void logUserAction(String action, String details) {
        log("USER", action + " " + details);
    }
    
    /**
     * Log a system event.
     */
    public void logSystemEvent(String event, String details) {
        log("SYS", event + " " + details);
    }
    
    /**
     * Log an error.
     */
    public void logError(String source, Throwable error) {
        log("ERR", source + ": " + error.getMessage());
    }
    
    /**
     * Log debug information.
     */
    public void logDebug(String tag, String message) {
        log("DBG", tag + ": " + message);
    }
    
    /**
     * Generic log method.
     */
    private void log(String category, String message) {
        if (!running) return;
        
        LogEntry entry = new LogEntry();
        entry.timestamp = System.currentTimeMillis();
        entry.category = category;
        entry.message = message;
        
        synchronized (logBuffer) {
            logBuffer.add(entry);
            
            // Limit buffer size
            if (logBuffer.size() > MAX_LOG_SIZE) {
                flush();
            }
        }
        
        // Also log to Android logcat
        Log.d(TAG, "[" + category + "] " + message);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // File Operations
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Flush buffer to file.
     */
    public void flush() {
        if (logBuffer.isEmpty()) return;
        
        try {
            ensureFileOpen();
            
            synchronized (logBuffer) {
                for (LogEntry entry : logBuffer) {
                    String line = formatEntry(entry) + "\n";
                    fileWriter.write(line);
                }
                logBuffer.clear();
            }
            
            fileWriter.flush();
            
            // Check file size
            if (logFile != null && logFile.length() > MAX_FILE_SIZE) {
                rotateLog();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to flush log", e);
        }
    }
    
    private void ensureFileOpen() throws IOException {
        if (fileWriter == null) {
            // In real implementation, get file from config context
            // logFile = new File(context.getFilesDir(), "xorgram_blackbox.log");
            // fileWriter = new FileWriter(logFile, true);
        }
    }
    
    private void closeFile() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close log file", e);
            }
            fileWriter = null;
        }
    }
    
    private void rotateLog() {
        closeFile();
        
        // Rename current log to .old
        if (logFile != null && logFile.exists()) {
            File oldFile = new File(logFile.getParent(), "xorgram_blackbox.log.old");
            if (oldFile.exists()) {
                oldFile.delete();
            }
            logFile.renameTo(oldFile);
        }
    }
    
    private String formatEntry(LogEntry entry) {
        return dateFormat.format(new Date(entry.timestamp)) + 
               " [" + entry.category + "] " + 
               entry.message;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Query Methods
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get recent log entries.
     * 
     * @param count Number of entries to return
     * @return List of log entries
     */
    public List<LogEntry> getRecentEntries(int count) {
        List<LogEntry> result = new ArrayList<>();
        synchronized (logBuffer) {
            int start = Math.max(0, logBuffer.size() - count);
            for (int i = start; i < logBuffer.size(); i++) {
                result.add(logBuffer.get(i));
            }
        }
        return result;
    }
    
    /**
     * Search log entries by category.
     */
    public List<LogEntry> searchByCategory(String category) {
        List<LogEntry> result = new ArrayList<>();
        synchronized (logBuffer) {
            for (LogEntry entry : logBuffer) {
                if (entry.category.equals(category)) {
                    result.add(entry);
                }
            }
        }
        return result;
    }
    
    /**
     * Clear all logs.
     */
    public void clear() {
        synchronized (logBuffer) {
            logBuffer.clear();
        }
        
        if (logFile != null && logFile.exists()) {
            logFile.delete();
        }
        
        Log.i(TAG, "Cleared all logs");
    }
    
    /**
     * Get log file path.
     */
    public File getLogFile() {
        return logFile;
    }
    
    /**
     * Log entry data class.
     */
    public static class LogEntry {
        public long timestamp;
        public String category;
        public String message;
        
        @Override
        public String toString() {
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.US);
            return df.format(new Date(timestamp)) + " [" + category + "] " + message;
        }
    }
}
