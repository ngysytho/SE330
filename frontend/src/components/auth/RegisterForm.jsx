import { Link } from "react-router-dom";
import { useState } from "react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import { useAuth } from "../../context/AuthContext.jsx";

export default function RegisterForm() {
  const { register, loading, error } = useAuth();
  const [form, setForm] = useState({ gmail: "", password: "", name: "" });

  async function submit(event) {
    event.preventDefault();
    await register(form).catch(() => {});
  }

  return (
    <form className="auth-card" autoComplete="off" onSubmit={submit}>
      <h1>Tạo tài khoản</h1>
      <p>Điền thông tin cơ bản để bắt đầu.</p>
      <div className="auth-fields">
        <Input
          type="text"
          name="chat_register_name"
          autoComplete="off"
          value={form.name}
          onChange={(event) => setForm({ ...form, name: event.target.value })}
          placeholder="Tên hiển thị"
          required
        />
        <Input
          type="text"
          name="chat_register_account"
          autoComplete="off"
          spellCheck={false}
          value={form.gmail}
          onChange={(event) => setForm({ ...form, gmail: event.target.value })}
          placeholder="Tài khoản"
          required
        />
        <Input
          type="password"
          name="chat_register_password"
          autoComplete="new-password"
          value={form.password}
          onChange={(event) => setForm({ ...form, password: event.target.value })}
          placeholder="Mật khẩu"
          required
        />
      </div>
      {error && <div className="auth-error">{error}</div>}
      <Button className="auth-submit" type="submit" disabled={loading}>
        Đăng ký
      </Button>
      <p className="auth-switch">
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </p>
    </form>
  );
}
