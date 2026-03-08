package org.xorgram.automation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Trigger definition
 * 
 * A trigger is activated by specific events and executes actions
 */
public class Trigger {

    private final String id;
    private final TriggerType type;
    private final String name;
    private final List<Action> actions;
    private String condition;
    private boolean enabled;

    public Trigger(TriggerType type, String name) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.name = name;
        this.actions = new ArrayList<>();
        this.condition = null;
        this.enabled = true;
    }

    public String getId() {
        return id;
    }

    public TriggerType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if this trigger matches the provided arguments
     */
    public boolean matches(Object... args) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        // TODO: Implement condition matching
        // This would check if the args match the trigger condition
        return true;
    }
}
