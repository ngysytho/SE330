import { Navigate } from "react-router-dom";
import RegisterForm from "../components/auth/RegisterForm.jsx";
import Loading from "../components/common/Loading.jsx";
import { useAuth } from "../context/AuthContext.jsx";

export default function RegisterPage() {
  const { user, loading } = useAuth();

  if (loading) return <Loading fullScreen />;
  if (user) return <Navigate to="/chat" replace />;

  return <div className="auth-page"><RegisterForm /></div>;
}
