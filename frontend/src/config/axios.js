import axios from "axios";

export const TOKEN_KEY = "chatapp_access_token";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || import.meta.env.VITE_BACKEND_URL || "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
