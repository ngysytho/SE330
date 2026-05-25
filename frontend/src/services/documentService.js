import api from "../config/axios.js";

export const documentService = {
  upload: (file) => {
    const body = new FormData();
    body.append("file", file);
    return api.post("/api/documents/upload", body).then((res) => res.data);
  },
  my: () => api.get("/api/documents/my").then((res) => res.data),
  get: (id) => api.get(`/api/documents/${id}`).then((res) => res.data),
  remove: (id) => api.delete(`/api/documents/${id}`),
};
