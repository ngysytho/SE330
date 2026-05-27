import api from "../config/axios.js";

export const userService = {
  me: () => api.get("/api/users/me").then((res) => res.data),
  updateMe: (payload) => api.patch("/api/users/me", payload).then((res) => res.data),
  registerFcmToken: (token) => api.post("/api/users/me/fcm-token", { token }).then((res) => res.data),
  removeFcmToken: (token) => api.delete("/api/users/me/fcm-token", { data: { token } }),
  get: (id) => api.get(`/api/users/${id}`).then((res) => res.data),
  search: (keyword) => api.get("/api/users/search", { params: { keyword } }).then((res) => res.data),
};
