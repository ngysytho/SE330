package com.example.chatapp.websocket;

import com.example.chatapp.security.CustomUserPrincipal;
import com.example.chatapp.security.JwtService;
import com.example.chatapp.service.PresenceService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    public static final String SESSION_USER_ATTRIBUTE = "chatUser";

    private final JwtService jwtService;
    private final PresenceService presenceService;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

    public WebSocketAuthChannelInterceptor(JwtService jwtService, PresenceService presenceService, ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider) {
        this.jwtService = jwtService;
        this.presenceService = presenceService;
        this.messagingTemplateProvider = messagingTemplateProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing JWT Authorization header");
            }
            try {
                CustomUserPrincipal principal = jwtService.parsePrincipal(header.substring(7));
                bindPrincipal(accessor, principal);
                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put(SESSION_USER_ATTRIBUTE, principal);
                }
                if (presenceService.online(principal.id(), accessor.getSessionId())) {
                    broadcastPresence(principal.id(), true);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JWT Authorization header", e);
            }
        }
        CustomUserPrincipal principal = principal(accessor);
        if (!StompCommand.CONNECT.equals(accessor.getCommand()) && principal != null) {
            bindPrincipal(accessor, principal);
        }
        if (accessor.getCommand() != null
                && accessor.getCommand() != StompCommand.CONNECT
                && accessor.getCommand() != StompCommand.DISCONNECT
                && principal == null) {
            throw new AuthenticationCredentialsNotFoundException("WebSocket authentication required");
        }
        if (StompCommand.DISCONNECT.equals(accessor.getCommand()) && principal != null) {
            if (presenceService.offline(principal.id(), accessor.getSessionId())) {
                broadcastPresence(principal.id(), false);
            }
        }
        return message;
    }

    private CustomUserPrincipal principal(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof CustomUserPrincipal principal) {
            return principal;
        }
        if (accessor.getSessionAttributes() == null) {
            return null;
        }
        Object stored = accessor.getSessionAttributes().get(SESSION_USER_ATTRIBUTE);
        return stored instanceof CustomUserPrincipal principal ? principal : null;
    }

    private void bindPrincipal(StompHeaderAccessor accessor, CustomUserPrincipal principal) {
        accessor.setUser(principal);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private void broadcastPresence(String userId, boolean online) {
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/presence", Map.of(
                "type", "PRESENCE_CHANGED",
                "userId", userId,
                "online", online,
                "lastSeenAt", System.currentTimeMillis()
        ));
    }
}
