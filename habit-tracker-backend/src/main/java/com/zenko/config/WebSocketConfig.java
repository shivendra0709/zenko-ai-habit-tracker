package com.zenko.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time habit updates.
 * Enables STOMP messaging for live habit completions, streaks, and notifications.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the message broker for handling WebSocket connections.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker with destination prefixes
        config.enableSimpleBroker("/topic", "/queue");
        // Configure the prefix for sending messages to specific users
        config.setUserDestinationPrefix("/user");
        // Prefix for client-to-server messages
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoint for WebSocket connections.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/habits")
                .setAllowedOrigins("*")
                .withSockJS(); // Fallback for browsers without WebSocket support
    }
}
