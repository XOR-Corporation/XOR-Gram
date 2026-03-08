package org.xorgram.automation;

import android.content.Context;

import org.xorgram.core.XORConfig;
import org.xorgram.core.XOREvent;
import org.xorgram.core.XORModule;
import org.xorgram.core.XORSetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Automation Module for XORGram
 * 
 * Features:
 * - Rule-based automation engine
 * - Auto-reply system
 * - Scheduled actions (cron-like)
 * - Trigger-based actions
 * - Conditional workflows
 */
public class AutomationModule implements XORModule {

    private static final String MODULE_ID = "xor-automation";
    private static final String MODULE_NAME = "Automation";
    private static final String MODULE_VERSION = "1.0.0";

    private Context context;
    private XORConfig config;
    private boolean enabled = true;

    // Sub-components
    private RuleEngine ruleEngine;
    private AutoReplyManager autoReplyManager;
    private Scheduler scheduler;
    private TriggerManager triggerManager;

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public String getVersion() {
    	return MODULE_VERSION;
    }
   
    @Override
    public String getDescription() {
    	return "Automation rules, auto-reply, scheduled actions, and trigger-based workflows";
    }
   
    @Override
    public Set<XOREvent> getSubscribedEvents() {
        Set<XOREvent> events = new HashSet<>();
        events.add(XOREvent.MESSAGE_RECEIVED);
        events.add(XOREvent.MESSAGE_SENT);
        events.add(XOREvent.USER_JOINED);
        events.add(XOREvent.USER_LEFT);
        events.add(XOREvent.USER_ONLINE);
        events.add(XOREvent.USER_TYPING);
        events.add(XOREvent.CHAT_OPENED);
        events.add(XOREvent.SCHEDULED_ACTION);
        return events;
    }

    @Override
    public void onInit(Context ctx, XORConfig cfg) {
        this.context = ctx;
        this.config = cfg;
        
        // Initialize sub-components
        ruleEngine = new RuleEngine(context, config);
        autoReplyManager = new AutoReplyManager(context, config);
        scheduler = new Scheduler(context, config);
        triggerManager = new TriggerManager(context, config);
        
        // Load enabled state
        enabled = config.getBoolean(MODULE_ID, "enabled", true);
        
        // Start scheduler
        scheduler.start();
    }

    @Override
    public boolean onEvent(XOREvent event, Object... args) {
        if (!enabled) return false;

        // Process through rule engine
        boolean handled = ruleEngine.processEvent(event, args);
        
        switch (event) {
            case MESSAGE_RECEIVED:
                return handleMessageReceived(args) || handled;
            case USER_JOINED:
                return handleUserJoined(args) || handled;
            case USER_LEFT:
                return handleUserLeft(args) || handled;
            case SCHEDULED_ACTION:
                return handleScheduledAction(args);
            default:
                return handled;
        }
    }

    @Override
    public List<XORSetting> getSettings() {
        List<XORSetting> settings = new ArrayList<>();
        
        settings.add(new XORSetting(
            MODULE_ID,
            "enabled",
            "Enable Automation",
            "Enable or disable all automation features",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "auto_reply_enabled",
            "Auto-Reply",
            "Enable automatic replies to messages",
            false,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "rules_enabled",
            "Rule Engine",
            "Enable rule-based automation",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "scheduler_enabled",
            "Scheduler",
            "Enable scheduled actions",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        settings.add(new XORSetting(
            MODULE_ID,
            "triggers_enabled",
            "Triggers",
            "Enable trigger-based actions",
            true,
            XORSetting.Type.BOOLEAN
        ));
        
        return settings;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.setBoolean(MODULE_ID, "enabled", enabled);
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) {
            scheduler.stop();
        }
        if (ruleEngine != null) {
            ruleEngine.shutdown();
        }
    }

    private boolean handleMessageReceived(Object... args) {
        if (!config.getBoolean(MODULE_ID, "auto_reply_enabled", false)) {
            return false;
        }
        // Process auto-reply
        return autoReplyManager.processMessage(args);
    }

    private boolean handleUserJoined(Object... args) {
        // Process join triggers
        return triggerManager.processTrigger(TriggerType.USER_JOINED, args);
    }

    private boolean handleUserLeft(Object... args) {
        // Process leave triggers
        return triggerManager.processTrigger(TriggerType.USER_LEFT, args);
    }

    private boolean handleScheduledAction(Object... args) {
        // Execute scheduled action
        return scheduler.executeScheduledAction(args);
    }

    // Public API methods

    /**
     * Get the rule engine
     */
    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    /**
     * Get the auto-reply manager
     */
    public AutoReplyManager getAutoReplyManager() {
        return autoReplyManager;
    }

    /**
     * Get the scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Get the trigger manager
     */
    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    /**
     * Add a new automation rule
     */
    public void addRule(AutomationRule rule) {
        ruleEngine.addRule(rule);
    }

    /**
     * Remove an automation rule
     */
    public void removeRule(String ruleId) {
        ruleEngine.removeRule(ruleId);
    }

    /**
     * Schedule an action
     */
    public void scheduleAction(ScheduledAction action) {
        scheduler.schedule(action);
    }
}
