import api from "../config/axios.js";

export const memberService = {
  add: (roomId, userId) => api.post(`/api/rooms/${roomId}/members`, { userId }).then((res) => res.data),
  list: (roomId) => api.get(`/api/rooms/${roomId}/members`).then((res) => res.data),
  updateRole: (roomId, userId, role) => api.patch(`/api/rooms/${roomId}/members/${userId}/role`, { role }).then((res) => res.data),
  updateStatus: (roomId, userId, status) => api.patch(`/api/rooms/${roomId}/members/${userId}/status`, { status }).then((res) => res.data),
  remove: (roomId, userId) => api.delete(`/api/rooms/${roomId}/members/${userId}`),
};
