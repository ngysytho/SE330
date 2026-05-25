import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { CaretLeft, FloppyDisk } from "@phosphor-icons/react";
import { Alert } from "antd";
import AvatarUploader from "../components/common/AvatarUploader.jsx";
import Button from "../components/common/Button.jsx";
import Input from "../components/common/Input.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { userService } from "../services/userService.js";

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState(() => ({
    name: user?.name || "",
    phoneNumber: user?.phoneNumber || "",
    address: user?.address || "",
    birthday: user?.birthday || "",
    note: user?.note || "",
    avatarImage: user?.avatarImage || "",
  }));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    setForm({
      name: user?.name || "",
      phoneNumber: user?.phoneNumber || "",
      address: user?.address || "",
      birthday: user?.birthday || "",
      note: user?.note || "",
      avatarImage: user?.avatarImage || "",
    });
  }, [user?.id]);

  function update(field, value) {
    setSaved(false);
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSaved(false);
    try {
      setUser(await userService.updateMe(form));
      setSaved(true);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Không lưu được profile.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="profile-page">
      <div className="profile-shell">
        <Link className="profile-back" to="/chat">
          <CaretLeft size={18} weight="bold" />
          <span>Quay lại chat</span>
        </Link>
        <form className="profile-card" onSubmit={submit}>
          <div className="profile-card-head">
            <div>
              <h1>Profile</h1>
              <p>Thông tin cá nhân hiển thị trong chat</p>
            </div>
            <Button type="submit" loading={saving} icon={<FloppyDisk size={18} weight="bold" />}>
              Lưu
            </Button>
          </div>

          <AvatarUploader
            className="profile-avatar-upload"
            value={form.avatarImage}
            name={form.name || user?.gmail}
            description="Ảnh đại diện"
            onChange={(value) => update("avatarImage", value)}
            disabled={saving}
          />

          <div className="profile-grid">
            <label>
              <span>Tên</span>
              <Input value={form.name} onChange={(event) => update("name", event.target.value)} placeholder="Tên hiển thị" />
            </label>
            <label>
              <span>Số điện thoại</span>
              <Input value={form.phoneNumber} onChange={(event) => update("phoneNumber", event.target.value)} placeholder="Số điện thoại" />
            </label>
            <label>
              <span>Địa chỉ</span>
              <Input value={form.address} onChange={(event) => update("address", event.target.value)} placeholder="Địa chỉ" />
            </label>
            <label>
              <span>Sinh nhật</span>
              <Input type="date" value={form.birthday} onChange={(event) => update("birthday", event.target.value)} />
            </label>
          </div>

          <label className="profile-note">
            <span>Ghi chú</span>
            <Input multiline rows={4} value={form.note} onChange={(event) => update("note", event.target.value)} placeholder="Ghi chú" />
          </label>

          {error && <Alert type="error" showIcon message={error} />}
          {saved && <Alert type="success" showIcon message="Đã lưu profile." />}
        </form>
      </div>
    </div>
  );
}
