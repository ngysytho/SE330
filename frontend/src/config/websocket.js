import { Client } from "@stomp/stompjs";

const API_URL = import.meta.env.VITE_API_URL || import.meta.env.VITE_BACKEND_URL || "http://localhost:8080";

let client;
let activeToken;

function websocketUrl() {
  return API_URL.replace(/^http/, "ws") + "/ws";
}

export function connectWebSocket(token, onConnect, onDisconnect) {
  if (client?.active && activeToken === token) {
    return client;
  }
  if (client?.active) {
    client.deactivate();
  }
  activeToken = token;

  client = new Client({
    brokerURL: websocketUrl(),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: (message) => {
      if (import.meta.env.DEV) {
        console.debug("[stomp]", message);
      }
    },
    onConnect: () => onConnect?.(),
    onDisconnect: () => onDisconnect?.(),
    onWebSocketClose: () => onDisconnect?.(),
    onStompError: (frame) => console.error("[stomp]", frame.headers.message, frame.body),
    onWebSocketError: (event) => console.error("[websocket]", event),
  });
  client.activate();
  return client;
}

export function isWebSocketConnected() {
  return Boolean(client?.connected);
}

export function subscribe(destination, callback) {
  if (!client?.connected) {
    return undefined;
  }
  return client.subscribe(destination, (message) => {
    try {
      callback(JSON.parse(message.body));
    } catch (error) {
      console.error("[stomp] Could not parse message", error, message.body);
    }
  });
}

export function send(destination, body) {
  if (!client?.connected) {
    throw new Error("WebSocket is not connected");
  }
  client.publish({ destination, body: JSON.stringify(body) });
}

export function disconnectWebSocket() {
  if (client) {
    client.deactivate();
    client = undefined;
    activeToken = undefined;
  }
}
