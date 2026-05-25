import { Navigate } from "react-router-dom";
import Loading from "../components/common/Loading.jsx";
import { useAuth } from "../context/AuthContext.jsx";

export default function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) return <Loading fullScreen />;
  if (!user) return <Navigate to="/login" replace />;
  return children;
}
