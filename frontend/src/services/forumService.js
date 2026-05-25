import api from "../config/axios.js";

export const forumService = {
  createPost: (roomId, payload) => api.post(`/api/forums/${roomId}/posts`, payload).then((res) => res.data),
  listPosts: (roomId, params = {}) => api.get(`/api/forums/${roomId}/posts`, { params }).then((res) => res.data),
  getPost: (postId) => api.get(`/api/forums/posts/${postId}`).then((res) => res.data),
  updatePost: (postId, payload) => api.patch(`/api/forums/posts/${postId}`, payload).then((res) => res.data),
  removePost: (postId) => api.delete(`/api/forums/posts/${postId}`),
  createComment: (postId, payload) => api.post(`/api/forums/posts/${postId}/comments`, payload).then((res) => res.data),
  listComments: (postId, params = {}) => api.get(`/api/forums/posts/${postId}/comments`, { params }).then((res) => res.data),
  updateComment: (commentId, content) => api.patch(`/api/forums/comments/${commentId}`, { content }).then((res) => res.data),
  removeComment: (commentId) => api.delete(`/api/forums/comments/${commentId}`),
};
