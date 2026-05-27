package com.example.chatapp.websocket;

import com.example.chatapp.service.PresenceService;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceEventListener {
    private final PresenceService presenceService;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

    public WebSocketPresenceEventListener(PresenceService presenceService, ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider) {
        this.presenceService = presenceService;
        this.messagingTemplateProvider = messagingTemplateProvider;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String userId = presenceService.userIdForSession(event.getSessionId());
        if (presenceService.offlineSession(event.getSessionId())) {
            broadcastPresence(userId);
        }
    }

    private void broadcastPresence(String userId) {
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/presence", Map.of(
                "type", "PRESENCE_CHANGED",
                "userId", userId,
                "online", false,
                "lastSeenAt", System.currentTimeMillis()
        ));
    }
}
