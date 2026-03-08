package org.xorgram.ai;

import android.content.Context;

import org.xorgram.core.XORConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Smart Reply Generator for AI-powered reply suggestions
 * 
 * Analyzes incoming messages and generates contextually
 * appropriate reply suggestions.
 */
public class SmartReplyGenerator {

    private final Context context;
    private final XORConfig config;
    private final ExecutorService executor;

    // Common reply templates
    private static final String[][] QUICK_REPLIES = {
        {"ok", "sure", "got it", "understood"},
        {"yes", "yeah", "yep", "sure thing"},
        {"no", "nope", "nah", "not really"},
        {"thanks", "thank you", "thanks!", "appreciate it"},
        {"later", "talk later", "catch you later", "bye"},
        {"haha", "lol", "😂", "haha!"},
        {"nice", "cool", "awesome", "great"},
        {"what?", "really?", "seriously?", "huh?"}
    };

    public SmartReplyGenerator(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Generate smart reply suggestions
     * 
     * @param message The incoming message to reply to
     * @param context Additional context (previous messages, etc.)
     * @return List of suggested replies
     */
    public List<String> generate(String message, String context) {
        List<String> suggestions = new ArrayList<>();
        
        if (message == null || message.isEmpty()) {
            return suggestions;
        }

        // Analyze message and generate appropriate replies
        String lowerMessage = message.toLowerCase().trim();

        // Check for questions
        if (lowerMessage.contains("?")) {
            suggestions.addAll(generateQuestionReplies(lowerMessage));
        }

        // Check for greetings
        if (isGreeting(lowerMessage)) {
            suggestions.addAll(generateGreetingReplies());
        }

        // Check for thanks
        if (isThanks(lowerMessage)) {
            suggestions.addAll(generateThanksReplies());
        }

        // Check for confirmation requests
        if (isConfirmationRequest(lowerMessage)) {
            suggestions.addAll(generateConfirmationReplies());
        }

        // Add contextual replies based on keywords
        suggestions.addAll(generateContextualReplies(lowerMessage));

        // Use OpenAI for more advanced suggestions if available
        String apiKey = config.getString("xor-ai", "api_key", "");
        if (!apiKey.isEmpty() && suggestions.size() < 3) {
            suggestions.addAll(generateAIReplies(message, context, apiKey));
        }

        // Limit to 4 suggestions and remove duplicates
        return deduplicateAndLimit(suggestions, 4);
    }

    private List<String> generateQuestionReplies(String message) {
        List<String> replies = new ArrayList<>();
        
        if (message.contains("what") || message.contains("how") || message.contains("why")) {
            replies.add("Let me think about that...");
            replies.add("Good question!");
        }
        
        if (message.contains("when") || message.contains("where")) {
            replies.add("I'll check and let you know");
        }
        
        if (message.contains("can you") || message.contains("could you")) {
            replies.add("Sure, I can do that");
            replies.add("Let me see what I can do");
        }
        
        return replies;
    }

    private List<String> generateGreetingReplies() {
        List<String> replies = new ArrayList<>();
        replies.add("Hey! 👋");
        replies.add("Hi there!");
        replies.add("Hello!");
        return replies;
    }

    private List<String> generateThanksReplies() {
        List<String> replies = new ArrayList<>();
        replies.add("You're welcome! 😊");
        replies.add("No problem!");
        replies.add("Anytime!");
        return replies;
    }

    private List<String> generateConfirmationReplies() {
        List<String> replies = new ArrayList<>();
        replies.add("Yes, sure");
        replies.add("Sounds good!");
        replies.add("I'm in!");
        replies.add("Let's do it");
        return replies;
    }

    private List<String> generateContextualReplies(String message) {
        List<String> replies = new ArrayList<>();
        
        // Emoji reactions
        if (message.contains("😂") || message.contains("lol") || message.contains("haha")) {
            replies.add("😂😂");
            replies.add("Haha!");
        }
        
        // Agreement
        if (message.contains("right") || message.contains("correct") || message.contains("exactly")) {
            replies.add("Exactly!");
            replies.add("100%");
        }
        
        return replies;
    }

    private List<String> generateAIReplies(String message, String context, String apiKey) {
        // TODO: Implement OpenAI API integration for smart replies
        // This would use the Chat Completions API with a prompt like:
        // "Generate 3 short, casual reply suggestions for: [message]"
        return new ArrayList<>();
    }

    private boolean isGreeting(String message) {
        return message.contains("hi") || message.contains("hello") || 
               message.contains("hey") || message.contains("sup") ||
               message.contains("yo") || message.contains("morning") ||
               message.contains("evening");
    }

    private boolean isThanks(String message) {
        return message.contains("thank") || message.contains("thanks") ||
               message.contains("appreciate") || message.contains("thx");
    }

    private boolean isConfirmationRequest(String message) {
        return message.contains("want to") || message.contains("wanna") ||
               message.contains("should we") || message.contains("let's") ||
               message.contains("up for") || message.contains("interested");
    }

    private List<String> deduplicateAndLimit(List<String> suggestions, int limit) {
        List<String> result = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (result.size() >= limit) break;
            if (!result.contains(suggestion)) {
                result.add(suggestion);
            }
        }
        return result;
    }

    /**
     * Asynchronous reply generation
     */
    public void generateAsync(String message, String context, ReplyCallback callback) {
        executor.execute(() -> {
            List<String> replies = generate(message, context);
            callback.onRepliesReady(replies);
        });
    }

    /**
     * Shutdown the generator
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Callback interface for async reply generation
     */
    public interface ReplyCallback {
        void onRepliesReady(List<String> replies);
    }
}
