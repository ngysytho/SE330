import api from "../config/axios.js";

export const messageService = {
  list: (roomId, params = {}) => api.get(`/api/rooms/${roomId}/messages`, { params }).then((res) => res.data),
  send: (roomId, payload) => api.post(`/api/rooms/${roomId}/messages`, payload).then((res) => res.data),
  update: (messageId, content) => api.patch(`/api/messages/${messageId}`, { messageId, content }).then((res) => res.data),
  remove: (messageId) => api.delete(`/api/messages/${messageId}`),
  read: (roomId, messageId) => api.post(`/api/rooms/${roomId}/read/${messageId}`),
};
