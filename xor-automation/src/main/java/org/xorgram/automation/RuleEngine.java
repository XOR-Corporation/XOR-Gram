package org.xorgram.automation;

import android.content.Context;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Rule Engine for automation rules
 * 
 * Supports:
 * - IF-THEN rules with conditions
 * - Multiple conditions (AND, OR)
 * - Action execution
 * - Rule priorities
 */
public class RuleEngine {

    private final Context context;
    private final XORConfig config;
    private final Map<String, AutomationRule> rules;
    private final ExecutorService executor;

    public RuleEngine(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        this.rules = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadExecutor();
        
        // Load saved rules
        loadRules();
    }

    /**
     * Process an event through the rule engine
     * 
     * @return true if any rule was triggered and executed
     */
    public boolean processEvent(XOREvent event, Object... args) {
        if (!config.getBoolean("xor-automation", "rules_enabled", true)) {
            return false;
        }

        List<AutomationRule> matchingRules = new ArrayList<>();
        
        // Find all rules that match this event
        for (AutomationRule rule : rules.values()) {
            if (rule.isEnabled() && rule.matchesEvent(event)) {
                matchingRules.add(rule);
            }
        }
        
        // Sort by priority (higher priority first)
        matchingRules.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        // Execute matching rules
        boolean anyExecuted = false;
        for (AutomationRule rule : matchingRules) {
            if (evaluateConditions(rule, args)) {
                executeActions(rule, args);
                anyExecuted = true;
                
                // Stop if rule says to stop processing
                if (rule.shouldStopProcessing()) {
                    break;
                }
            }
        }
        
        return anyExecuted;
    }

    /**
     * Add a new rule
     */
    public void addRule(AutomationRule rule) {
        rules.put(rule.getId(), rule);
        saveRules();
    }

    /**
     * Remove a rule
     */
    public void removeRule(String ruleId) {
        rules.remove(ruleId);
        saveRules();
    }

    /**
     * Get all rules
     */
    public List<AutomationRule> getRules() {
        return new ArrayList<>(rules.values());
    }

    /**
     * Get a specific rule
     */
    public AutomationRule getRule(String ruleId) {
        return rules.get(ruleId);
    }

    /**
     * Enable/disable a rule
     */
    public void setRuleEnabled(String ruleId, boolean enabled) {
        AutomationRule rule = rules.get(ruleId);
        if (rule != null) {
            rule.setEnabled(enabled);
            saveRules();
        }
    }

    /**
     * Evaluate rule conditions
     */
    private boolean evaluateConditions(AutomationRule rule, Object... args) {
        List<Condition> conditions = rule.getConditions();
        
        if (conditions.isEmpty()) {
            return true;
        }
        
        Condition.Logic logic = rule.getConditionLogic();
        
        if (logic == Condition.Logic.OR) {
            // OR: any condition must be true
            for (Condition condition : conditions) {
                if (evaluateCondition(condition, args)) {
                    return true;
                }
            }
            return false;
        } else {
            // AND: all conditions must be true
            for (Condition condition : conditions) {
                if (!evaluateCondition(condition, args)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Evaluate a single condition
     */
    private boolean evaluateCondition(Condition condition, Object... args) {
        // TODO: Implement condition evaluation based on type
        // This would check message content, sender, time, etc.
        return condition.evaluate(args);
    }

    /**
     * Execute rule actions
     */
    private void executeActions(AutomationRule rule, Object... args) {
        executor.execute(() -> {
            for (Action action : rule.getActions()) {
                try {
                    action.execute(context, args);
                } catch (Exception e) {
                    // Log error but continue with other actions
                }
            }
        });
    }

    /**
     * Load rules from config
     */
    private void loadRules() {
        // TODO: Load rules from config storage
    }

    /**
     * Save rules to config
     */
    private void saveRules() {
        // TODO: Save rules to config storage
    }

    /**
     * Shutdown the engine
     */
    public void shutdown() {
        executor.shutdown();
    }
}
