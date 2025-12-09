package com.example.webRTC_demo.webRTC_demo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SignalingHandler extends TextWebSocketHandler {

   // sessionId -> session
   private final Map<String, WebSocketSession> clients = new ConcurrentHashMap<>();
   private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

   @Override
   public void afterConnectionEstablished(WebSocketSession session) throws Exception {
      String id = session.getId();
      clients.put(id, session);

      ObjectNode json = OBJECT_MAPPER.createObjectNode();
      json.put("type", "welcome");
      json.put("id", id);
      session.sendMessage(new TextMessage(json.toString()));

      System.out.println("Client connected: " + id);
   }

   @Override
   protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
      JsonNode json = OBJECT_MAPPER.readTree(message.getPayload());

      if (json.has("to")) {
         String to = json.path("to").asText();
         WebSocketSession dest = clients.get(to);
         if (dest != null && dest.isOpen()) {
            dest.sendMessage(new TextMessage(message.getPayload()));
         } else {
            ObjectNode err = OBJECT_MAPPER.createObjectNode();
            err.put("type", "error");
            err.put("message", "destination_not_found");
            session.sendMessage(new TextMessage(err.toString()));
         }
      } else if (json.path("broadcast").asBoolean(false)) {
         // broadcast to all other connected clients
         for (Map.Entry<String, WebSocketSession> entry : clients.entrySet()) {
            if (!entry.getKey().equals(session.getId()) && entry.getValue().isOpen()) {
               entry.getValue().sendMessage(new TextMessage(message.getPayload()));
            }
         }
      }
   }

   @Override
   public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
      clients.remove(session.getId());
      System.out.println("Client disconnected: " + session.getId());
   }
}