import { Link } from "react-router-dom";
import { useState } from "react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import { useAuth } from "../../context/AuthContext.jsx";

export default function LoginForm() {
  const { login, loading, error } = useAuth();
  const [form, setForm] = useState({ gmail: "", password: "" });

  async function submit(event) {
    event.preventDefault();
    await login(form).catch(() => {});
  }

  return (
    <form className="auth-card" autoComplete="off" onSubmit={submit}>
      <h1>BaoAnhPhat</h1>
      <p>Đăng nhập để tiếp tục chat.</p>
      <div className="auth-fields">
        <Input
          type="text"
          name="chat_login_account"
          autoComplete="off"
          spellCheck={false}
          value={form.gmail}
          onChange={(event) => setForm({ ...form, gmail: event.target.value })}
          placeholder="Tài khoản"
          required
        />
        <Input
          type="password"
          name="chat_login_password"
          autoComplete="new-password"
          value={form.password}
          onChange={(event) => setForm({ ...form, password: event.target.value })}
          placeholder="Mật khẩu"
          required
        />
      </div>
      {error && <div className="auth-error">{error}</div>}
      <Button className="auth-submit" type="submit" disabled={loading}>
        Đăng nhập
      </Button>
      <p className="auth-switch">
        Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
      </p>
    </form>
  );
}
