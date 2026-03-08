package org.xorgram.automation;

import java.util.UUID;

/**
 * Scheduled Action definition
 * 
 * Represents an action scheduled to execute at a specific time
 */
public class ScheduledAction {

    private final String id;
    private final long chatId;
    private final Action action;
    private long nextExecutionTime;
    private Recurrence recurrence;
    private boolean enabled;

    public ScheduledAction(long chatId, Action action, long executionTime) {
        this.id = UUID.randomUUID().toString();
        this.chatId = chatId;
        this.action = action;
        this.nextExecutionTime = executionTime;
        this.recurrence = Recurrence.NONE;
        this.enabled = true;
    }

    public String getId() {
        return id;
    }

    public long getChatId() {
        return chatId;
    }

    public Action getAction() {
        return action;
    }

    public long getNextExecutionTime() {
        return nextExecutionTime;
    }

    public boolean isDue(long currentTime) {
        return enabled && currentTime >= nextExecutionTime;
    }

    public boolean isRecurring() {
        return recurrence != Recurrence.NONE;
    }

    public void updateNextExecution() {
        switch (recurrence) {
            case HOURLY:
                nextExecutionTime += 3600000L;
                break;
            case DAILY:
                nextExecutionTime += 86400000L;
                break;
            case WEEKLY:
                nextExecutionTime += 604800000L;
                break;
            case MONTHLY:
                nextExecutionTime += 2592000000L;
                break;
            default:
                break;
        }
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Recurrence types
     */
    public enum Recurrence {
        NONE,
        HOURLY,
        DAILY,
        WEEKLY,
        MONTHLY,
        CUSTOM
    }
}
