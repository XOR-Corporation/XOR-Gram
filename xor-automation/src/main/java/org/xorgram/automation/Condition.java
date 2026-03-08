package org.xorgram.automation;

/**
 * Condition for automation rules
 * 
 * Types:
 * - MESSAGE_CONTAINS: Message contains specific text
 * - MESSAGE_MATCHES: Message matches regex pattern
 * - SENDER_IS: Message from specific user
 * - CHAT_IS: Message in specific chat
 * - TIME_BETWEEN: Current time is in range
 * - CUSTOM: Custom condition
 */
public class Condition {

    private final Type type;
    private final String value;
    private final String extra;
    private boolean caseSensitive;

    public Condition(Type type, String value) {
        this.type = type;
        this.value = value;
        this.extra = null;
        this.caseSensitive = false;
    }

    public Condition(Type type, String value, String extra) {
        this.type = type;
        this.value = value;
        this.extra = extra;
        this.caseSensitive = false;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getExtra() {
        return extra;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Evaluate the condition against provided arguments
     */
    public boolean evaluate(Object... args) {
        switch (type) {
            case MESSAGE_CONTAINS:
                return evaluateMessageContains(args);
            case MESSAGE_MATCHES:
                return evaluateMessageMatches(args);
            case SENDER_IS:
                return evaluateSenderIs(args);
            case CHAT_IS:
                return evaluateChatIs(args);
            case TIME_BETWEEN:
                return evaluateTimeBetween();
            case CUSTOM:
                return evaluateCustom(args);
            default:
                return false;
        }
    }

    private boolean evaluateMessageContains(Object... args) {
        if (args.length == 0 || !(args[0] instanceof String)) {
            return false;
        }
        String message = (String) args[0];
        String searchValue = caseSensitive ? value : value.toLowerCase();
        String searchMessage = caseSensitive ? message : message.toLowerCase();
        return searchMessage.contains(searchValue);
    }

    private boolean evaluateMessageMatches(Object... args) {
        if (args.length == 0 || !(args[0] instanceof String)) {
            return false;
        }
        String message = (String) args[0];
        try {
            return message.matches(value);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean evaluateSenderIs(Object... args) {
        // TODO: Parse sender ID from args
        return false;
    }

    private boolean evaluateChatIs(Object... args) {
        // TODO: Parse chat ID from args
        return false;
    }

    private boolean evaluateTimeBetween() {
        if (extra == null) return false;
        
        try {
            String[] parts = extra.split("-");
            int startHour = Integer.parseInt(parts[0]);
            int endHour = Integer.parseInt(parts[1]);
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            
            if (startHour <= endHour) {
                return currentHour >= startHour && currentHour <= endHour;
            } else {
                // Spans midnight
                return currentHour >= startHour || currentHour <= endHour;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean evaluateCustom(Object... args) {
        // TODO: Implement custom condition evaluation
        return false;
    }

    /**
     * Condition types
     */
    public enum Type {
        MESSAGE_CONTAINS,
        MESSAGE_MATCHES,
        SENDER_IS,
        CHAT_IS,
        TIME_BETWEEN,
        CUSTOM
    }

    /**
     * Logic for combining conditions
     */
    public enum Logic {
        AND,
        OR
    }
}
