package org.xorgram.automation;

import org.xorgram.core.XOREvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Automation Rule definition
 * 
 * A rule consists of:
 * - Trigger event(s)
 * - Conditions (IF)
 * - Actions (THEN)
 * - Priority
 */
public class AutomationRule {

    private final String id;
    private final String name;
    private final String description;
    private final List<XOREvent> triggerEvents;
    private final List<Condition> conditions;
    private final List<Action> actions;
    private Condition.Logic conditionLogic;
    private int priority;
    private boolean enabled;
    private boolean stopProcessing;

    public AutomationRule(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.triggerEvents = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.conditionLogic = Condition.Logic.AND;
        this.priority = 0;
        this.enabled = true;
        this.stopProcessing = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean matchesEvent(XOREvent event) {
        return triggerEvents.isEmpty() || triggerEvents.contains(event);
    }

    public void addTriggerEvent(XOREvent event) {
        triggerEvents.add(event);
    }

    public void addCondition(Condition condition) {
        conditions.add(condition);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Action> getActions() {
        return actions;
    }

    public Condition.Logic getConditionLogic() {
        return conditionLogic;
    }

    public void setConditionLogic(Condition.Logic logic) {
        this.conditionLogic = logic;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean shouldStopProcessing() {
        return stopProcessing;
    }

    public void setStopProcessing(boolean stop) {
        this.stopProcessing = stop;
    }
}
