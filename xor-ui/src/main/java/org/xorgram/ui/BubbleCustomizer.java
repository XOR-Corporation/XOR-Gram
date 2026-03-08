package org.xorgram.ui;

import org.xorgram.core.XORConfig;

/**
 * BubbleCustomizer - Customizes chat bubble appearance.
 */
public class BubbleCustomizer {
    
    private final XORConfig config;
    private int bubbleRadius;
    
    public BubbleCustomizer(XORConfig config) {
        this.config = config;
        this.bubbleRadius = config.getInt("ui_bubble_radius", 18);
    }
    
    public void customizeBubble(Object message, Object bubbleView) {
        // Apply bubble customization
    }
    
    public int getBubbleRadius() {
        return bubbleRadius;
    }
    
    public void setBubbleRadius(int radius) {
        this.bubbleRadius = radius;
        config.putInt("ui_bubble_radius", radius);
    }
    
    public void destroy() {
        // Cleanup
    }
}
