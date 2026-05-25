import api, { TOKEN_KEY } from "../config/axios.js";

const USER_KEY = "chatapp_user";

function clearLegacyAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

function storeAuth(data) {
  clearLegacyAuth();
  sessionStorage.setItem(TOKEN_KEY, data.accessToken);
  sessionStorage.setItem(USER_KEY, JSON.stringify(data.user));
  return data;
}

export async function register(payload) {
  const { data } = await api.post("/api/auth/register", payload);
  return storeAuth(data);
}

export async function login(payload) {
  const { data } = await api.post("/api/auth/login", payload);
  return storeAuth(data);
}

export async function logout() {
  try {
    await api.post("/api/auth/logout");
  } finally {
    clearStoredAuth();
  }
}

export async function getMe() {
  const { data } = await api.get("/api/auth/me");
  sessionStorage.setItem(USER_KEY, JSON.stringify(data));
  return data;
}

export function getStoredAuth() {
  clearLegacyAuth();
  const accessToken = sessionStorage.getItem(TOKEN_KEY);
  const rawUser = sessionStorage.getItem(USER_KEY);
  return {
    accessToken,
    user: rawUser ? JSON.parse(rawUser) : null,
  };
}

export function clearStoredAuth() {
  clearLegacyAuth();
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}
