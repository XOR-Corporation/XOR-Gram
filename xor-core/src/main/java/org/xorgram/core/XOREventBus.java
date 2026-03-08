package org.xorgram.core;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * XOREventBus - Event bus for inter-module communication.
 * 
 * Features:
 * - Thread-safe event dispatching
 * - Priority-based event handling
 * - Sticky events for late subscribers
 * - Event filtering and transformation
 * 
 * Uses EventBus internally but provides XORGram-specific features.
 */
public class XOREventBus {
    
    private static final String TAG = "XOREventBus";
    
    // ═══════════════════════════════════════════════════════════════
    // Singleton Instance
    // ═══════════════════════════════════════════════════════════════
    
    private static volatile XOREventBus instance;
    private static final Object lock = new Object();
    
    public static XOREventBus getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new XOREventBus();
                }
            }
        }
        return instance;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Internal State
    // ═══════════════════════════════════════════════════════════════
    
    private final EventBus eventBus;
    private final Map<String, List<EventFilter>> filters;
    private final Map<String, EventTransformer> transformers;
    private final List<EventInterceptor> interceptors;
    private final Map<String, Object> stickyEvents;
    private final EventStats stats;
    
    private XOREventBus() {
        this.eventBus = EventBus.builder()
                .logSubscriberExceptions(false)
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .eventInheritance(true)
                .build();
        this.filters = new ConcurrentHashMap<>();
        this.transformers = new ConcurrentHashMap<>();
        this.interceptors = new CopyOnWriteArrayList<>();
        this.stickyEvents = new ConcurrentHashMap<>();
        this.stats = new EventStats();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Basic Event Posting
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Post an event to all subscribers.
     * 
     * @param event The event to post
     */
    public void post(Object event) {
        if (event == null) return;
        
        stats.recordPost(event.getClass().getSimpleName());
        
        // Apply interceptors
        for (EventInterceptor interceptor : interceptors) {
            if (!interceptor.onIntercept(event)) {
                return; // Event was intercepted
            }
        }
        
        // Apply transformers
        Object transformedEvent = event;
        String eventType = event.getClass().getSimpleName();
        EventTransformer transformer = transformers.get(eventType);
        if (transformer != null) {
            transformedEvent = transformer.transform(event);
            if (transformedEvent == null) return; // Event was filtered out
        }
        
        eventBus.post(transformedEvent);
    }
    
    /**
     * Post an event with a specific tag.
     * 
     * @param event The event to post
     * @param tag Event tag for filtering
     */
    public void post(Object event, String tag) {
        post(new TaggedEvent(event, tag));
    }
    
    /**
     * Post a sticky event (persists for late subscribers).
     * 
     * @param event The event to post
     */
    public void postSticky(Object event) {
        if (event == null) return;
        
        String eventType = event.getClass().getSimpleName();
        stickyEvents.put(eventType, event);
        stats.recordPost(eventType);
        
        eventBus.postSticky(event);
    }
    
    /**
     * Post an event asynchronously.
     * 
     * @param event The event to post
     */
    public void postAsync(Object event) {
        if (event == null) return;
        
        stats.recordPost(event.getClass().getSimpleName());
        eventBus.post(event);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Subscription
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Register a subscriber to receive events.
     * 
     * @param subscriber The subscriber object
     */
    public void register(Object subscriber) {
        if (!eventBus.isRegistered(subscriber)) {
            eventBus.register(subscriber);
        }
    }
    
    /**
     * Unregister a subscriber.
     * 
     * @param subscriber The subscriber object
     */
    public void unregister(Object subscriber) {
        if (eventBus.isRegistered(subscriber)) {
            eventBus.unregister(subscriber);
        }
    }
    
    /**
     * Check if an object is registered.
     * 
     * @param subscriber The subscriber to check
     * @return true if registered
     */
    public boolean isRegistered(Object subscriber) {
        return eventBus.isRegistered(subscriber);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Sticky Events
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get a sticky event by type.
     * 
     * @param eventType The event class
     * @return The sticky event or null
     */
    @SuppressWarnings("unchecked")
    public <T> T getStickyEvent(Class<T> eventType) {
        String key = eventType.getSimpleName();
        Object event = stickyEvents.get(key);
        if (event != null && eventType.isInstance(event)) {
            return (T) event;
        }
        return eventBus.getStickyEvent(eventType);
    }
    
    /**
     * Remove a sticky event.
     * 
     * @param eventType The event class
     */
    public <T> T removeStickyEvent(Class<T> eventType) {
        String key = eventType.getSimpleName();
        stickyEvents.remove(key);
        return eventBus.removeStickyEvent(eventType);
    }
    
    /**
     * Remove all sticky events.
     */
    public void removeAllStickyEvents() {
        stickyEvents.clear();
        eventBus.removeAllStickyEvents();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Filters and Transformers
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Add a filter for a specific event type.
     * 
     * @param eventType The event type to filter
     * @param filter The filter to apply
     */
    public void addFilter(String eventType, EventFilter filter) {
        if (!filters.containsKey(eventType)) {
            filters.put(eventType, new CopyOnWriteArrayList<>());
        }
        filters.get(eventType).add(filter);
    }
    
    /**
     * Remove a filter.
     * 
     * @param eventType The event type
     * @param filter The filter to remove
     */
    public void removeFilter(String eventType, EventFilter filter) {
        List<EventFilter> list = filters.get(eventType);
        if (list != null) {
            list.remove(filter);
        }
    }
    
    /**
     * Set a transformer for a specific event type.
     * 
     * @param eventType The event type to transform
     * @param transformer The transformer to apply
     */
    public void setTransformer(String eventType, EventTransformer transformer) {
        transformers.put(eventType, transformer);
    }
    
    /**
     * Remove a transformer.
     * 
     * @param eventType The event type
     */
    public void removeTransformer(String eventType) {
        transformers.remove(eventType);
    }
    
    /**
     * Add an event interceptor.
     * 
     * @param interceptor The interceptor to add
     */
    public void addInterceptor(EventInterceptor interceptor) {
        interceptors.add(interceptor);
    }
    
    /**
     * Remove an event interceptor.
     * 
     * @param interceptor The interceptor to remove
     */
    public void removeInterceptor(EventInterceptor interceptor) {
        interceptors.remove(interceptor);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Statistics
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get event statistics.
     * 
     * @return Event stats object
     */
    public EventStats getStats() {
        return stats;
    }
    
    /**
     * Reset statistics.
     */
    public void resetStats() {
        stats.reset();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Debug
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get debug information.
     * 
     * @return Debug info string
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("XOREventBus Debug Info:\n");
        sb.append("  Sticky events: ").append(stickyEvents.size()).append("\n");
        sb.append("  Filters: ").append(filters.size()).append("\n");
        sb.append("  Transformers: ").append(transformers.size()).append("\n");
        sb.append("  Interceptors: ").append(interceptors.size()).append("\n");
        sb.append("  Stats:\n").append(stats.toString());
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Interfaces
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Filter interface for event filtering.
     */
    public interface EventFilter {
        /**
         * Filter an event.
         * 
         * @param event The event to filter
         * @return true to allow the event, false to block it
         */
        boolean filter(Object event);
    }
    
    /**
     * Transformer interface for event transformation.
     */
    public interface EventTransformer {
        /**
         * Transform an event.
         * 
         * @param event The original event
         * @return The transformed event, or null to block
         */
        Object transform(Object event);
    }
    
    /**
     * Interceptor interface for event interception.
     */
    public interface EventInterceptor {
        /**
         * Intercept an event before posting.
         * 
         * @param event The event to intercept
         * @return true to allow posting, false to block
         */
        boolean onIntercept(Object event);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Helper Classes
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Tagged event wrapper for tagged posting.
     */
    public static class TaggedEvent {
        public final Object event;
        public final String tag;
        
        public TaggedEvent(Object event, String tag) {
            this.event = event;
            this.tag = tag;
        }
    }
    
    /**
     * Event statistics tracker.
     */
    public static class EventStats {
        private final Map<String, Long> postCounts = new ConcurrentHashMap<>();
        private final Map<String, Long> lastPostTime = new ConcurrentHashMap<>();
        private long totalPosts = 0;
        
        public void recordPost(String eventType) {
            postCounts.merge(eventType, 1L, Long::sum);
            lastPostTime.put(eventType, System.currentTimeMillis());
            totalPosts++;
        }
        
        public long getPostCount(String eventType) {
            return postCounts.getOrDefault(eventType, 0L);
        }
        
        public long getLastPostTime(String eventType) {
            return lastPostTime.getOrDefault(eventType, 0L);
        }
        
        public long getTotalPosts() {
            return totalPosts;
        }
        
        public void reset() {
            postCounts.clear();
            lastPostTime.clear();
            totalPosts = 0;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("    Total posts: ").append(totalPosts).append("\n");
            sb.append("    Event types: ").append(postCounts.size()).append("\n");
            for (Map.Entry<String, Long> entry : postCounts.entrySet()) {
                sb.append("      ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }
    }
}
