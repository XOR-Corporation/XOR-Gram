package org.xorgram.automation;

import android.content.Context;

import org.xorgram.core.XORConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trigger Manager for trigger-based actions
 * 
 * Features:
 * - User join/leave triggers
 * - Keyword triggers
 * - Time-based triggers
 * - Location-based triggers
 * - Custom event triggers
 */
public class TriggerManager {

    private final Context context;
    private final XORConfig config;
    private final Map<TriggerType, List<Trigger>> triggers;

    public TriggerManager(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        this.triggers = new ConcurrentHashMap<>();
        
        // Initialize trigger lists
        for (TriggerType type : TriggerType.values()) {
            triggers.put(type, new ArrayList<>());
        }
        
        // Load saved triggers
        loadTriggers();
    }

    /**
     * Process a trigger event
     * 
     * @return true if any trigger was executed
     */
    public boolean processTrigger(TriggerType type, Object... args) {
        if (!config.getBoolean("xor-automation", "triggers_enabled", true)) {
            return false;
        }

        List<Trigger> typeTriggers = triggers.get(type);
        boolean anyExecuted = false;
        
        for (Trigger trigger : typeTriggers) {
            if (trigger.isEnabled() && trigger.matches(args)) {
                executeTrigger(trigger, args);
                anyExecuted = true;
            }
        }
        
        return anyExecuted;
    }

    /**
     * Add a trigger
     */
    public void addTrigger(Trigger trigger) {
        List<Trigger> typeTriggers = triggers.get(trigger.getType());
        if (typeTriggers != null) {
            typeTriggers.add(trigger);
            saveTriggers();
        }
    }

    /**
     * Remove a trigger
     */
    public void removeTrigger(String triggerId) {
        for (List<Trigger> typeTriggers : triggers.values()) {
            typeTriggers.removeIf(t -> t.getId().equals(triggerId));
        }
        saveTriggers();
    }

    /**
     * Get all triggers
     */
    public List<Trigger> getAllTriggers() {
        List<Trigger> all = new ArrayList<>();
        for (List<Trigger> typeTriggers : triggers.values()) {
            all.addAll(typeTriggers);
        }
        return all;
    }

    /**
     * Get triggers by type
     */
    public List<Trigger> getTriggers(TriggerType type) {
        return new ArrayList<>(triggers.get(type));
    }

    /**
     * Enable/disable a trigger
     */
    public void setTriggerEnabled(String triggerId, boolean enabled) {
        for (List<Trigger> typeTriggers : triggers.values()) {
            for (Trigger trigger : typeTriggers) {
                if (trigger.getId().equals(triggerId)) {
                    trigger.setEnabled(enabled);
                    saveTriggers();
                    return;
                }
            }
        }
    }

    /**
     * Execute a trigger's actions
     */
    private void executeTrigger(Trigger trigger, Object... args) {
        // Execute all actions associated with this trigger
        for (Action action : trigger.getActions()) {
            try {
                action.execute(context, args);
            } catch (Exception e) {
                // Log error but continue
            }
        }
    }

    /**
     * Load triggers from storage
     */
    private void loadTriggers() {
        // TODO: Load from config storage
    }

    /**
     * Save triggers to storage
     */
    private void saveTriggers() {
        // TODO: Save to config storage
    }
}
