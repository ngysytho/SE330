import { Link } from "react-router-dom";
import { useState } from "react";
import Button from "../components/common/Button.jsx";
import Input from "../components/common/Input.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { userService } from "../services/userService.js";

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({
    name: user?.name || "",
    phoneNumber: user?.phoneNumber || "",
    address: user?.address || "",
    birthday: user?.birthday || "",
    note: user?.note || "",
    avatarImage: user?.avatarImage || "",
  });

  async function submit(event) {
    event.preventDefault();
    setUser(await userService.updateMe(form));
  }

  return (
    <div className="min-h-screen bg-discord-app p-6 text-discord-text">
      <div className="mx-auto max-w-2xl">
        <Link className="text-sm text-discord-muted hover:text-white" to="/chat">Back to chat</Link>
        <form className="mt-5 space-y-3 rounded-lg border border-discord-border bg-discord-panel p-5" onSubmit={submit}>
          <h1 className="text-xl font-bold">Profile</h1>
          <Input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Name" />
          <Input value={form.phoneNumber} onChange={(event) => setForm({ ...form, phoneNumber: event.target.value })} placeholder="Phone number" />
          <Input value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} placeholder="Address" />
          <Input value={form.birthday} onChange={(event) => setForm({ ...form, birthday: event.target.value })} placeholder="Birthday" />
          <Input value={form.avatarImage} onChange={(event) => setForm({ ...form, avatarImage: event.target.value })} placeholder="Avatar URL" />
          <Input multiline rows={4} value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} placeholder="Note" />
          <Button type="submit">Save profile</Button>
        </form>
      </div>
    </div>
  );
}
