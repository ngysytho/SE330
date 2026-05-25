import api from "../config/axios.js";

export const roomService = {
  createPrivate: (otherUserId) => api.post("/api/rooms/private", { otherUserId }).then((res) => res.data),
  createGroup: (payload) => api.post("/api/rooms/group", payload).then((res) => res.data),
  createForum: (payload) => api.post("/api/rooms/forum", payload).then((res) => res.data),
  defaultForum: () => api.get("/api/rooms/forum/default").then((res) => res.data),
  my: () => api.get("/api/rooms/my").then((res) => res.data),
  private: () => api.get("/api/rooms/private").then((res) => res.data),
  groups: () => api.get("/api/rooms/groups").then((res) => res.data),
  forums: () => api.get("/api/rooms/forums").then((res) => res.data),
  get: (roomId) => api.get(`/api/rooms/${roomId}`).then((res) => res.data),
  update: (roomId, payload) => api.patch(`/api/rooms/${roomId}`, payload).then((res) => res.data),
  remove: (roomId) => api.delete(`/api/rooms/${roomId}`),
};
