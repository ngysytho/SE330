import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { clearStoredAuth, getMe, getStoredAuth, login as loginService, logout as logoutService, register as registerService, storeUser } from "../services/authService.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const stored = getStoredAuth();
  const [accessToken, setAccessToken] = useState(stored.accessToken);
  const [user, setUserState] = useState(stored.user);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function hydrate() {
      if (!stored.accessToken) {
        setLoading(false);
        return;
      }
      try {
        setUserState(await getMe());
      } catch {
        clearStoredAuth();
        setAccessToken(null);
        setUserState(null);
      } finally {
        setLoading(false);
      }
    }
    hydrate();
  }, []);

  async function login(payload) {
    setLoading(true);
    setError("");
    try {
      const data = await loginService(payload);
      setAccessToken(data.accessToken);
      setUserState(data.user);
      return data;
    } catch (err) {
      const message = err?.response?.data?.message || err?.message || "Login failed";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }

  async function register(payload) {
    setLoading(true);
    setError("");
    try {
      const data = await registerService(payload);
      setAccessToken(data.accessToken);
      setUserState(data.user);
      return data;
    } catch (err) {
      const message = err?.response?.data?.message || err?.message || "Registration failed";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }

  async function logout() {
    await logoutService();
    setAccessToken(null);
    setUserState(null);
  }

  function setUser(nextUser) {
    setUserState(storeUser(nextUser));
  }

  const value = useMemo(
    () => ({ accessToken, user, loading, error, login, register, logout, setUser }),
    [accessToken, user, loading, error],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
