import {
  connectWebSocket,
  disconnectWebSocket,
  send,
  subscribe,
} from "../config/websocket.js";

export { connectWebSocket, disconnectWebSocket };

export const chatRealtimeTopics = {
  room: (roomId) => `/topic/rooms/${roomId}`,
  typing: (roomId) => `/topic/rooms/${roomId}/typing`,
  userRooms: "/user/queue/rooms",
  presence: "/topic/presence",
};

export function subscribeRoom(roomId, callback) {
  return subscribe(chatRealtimeTopics.room(roomId), callback);
}

export function subscribeRoomTyping(roomId, callback) {
  return subscribe(chatRealtimeTopics.typing(roomId), callback);
}

export function subscribeUserRooms(callback) {
  return subscribe(chatRealtimeTopics.userRooms, callback);
}

export function subscribePresence(callback) {
  return subscribe(chatRealtimeTopics.presence, callback);
}

export function sendTyping(roomId, typing) {
  send("/app/chat.typing", { roomId, typing });
}
