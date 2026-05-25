import api from "../config/axios.js";

export const userService = {
  me: () => api.get("/api/users/me").then((res) => res.data),
  updateMe: (payload) => api.patch("/api/users/me", payload).then((res) => res.data),
  get: (id) => api.get(`/api/users/${id}`).then((res) => res.data),
  search: (keyword) => api.get("/api/users/search", { params: { keyword } }).then((res) => res.data),
};
