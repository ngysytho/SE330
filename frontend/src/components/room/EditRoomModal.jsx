import { useEffect, useState } from "react";
import { Alert } from "antd";
import AvatarUploader from "../common/AvatarUploader.jsx";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import Modal from "../common/Modal.jsx";
import { useChat } from "../../context/ChatContext.jsx";
import { roomService } from "../../services/roomService.js";

export default function EditRoomModal({ open, onClose, room }) {
  const { refreshRooms, setActiveRoom } = useChat();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [avatarUrl, setAvatarUrl] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open || !room) return;
    setName(room.name || "");
    setDescription(room.description || "");
    setAvatarUrl(room.avatarUrl || "");
    setError("");
  }, [open, room?.id]);

  async function submit(event) {
    event.preventDefault();
    if (!room) return;
    setSaving(true);
    setError("");
    try {
      const updated = await roomService.update(room.id, {
        name,
        description,
        avatarUrl,
        isPublic: room.isPublic,
      });
      setActiveRoom((current) => (current?.id === updated.id ? { ...current, ...updated } : current));
      await refreshRooms();
      onClose();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Không cập nhật được nhóm.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Thông tin nhóm" open={open} onClose={onClose}>
      <form className="room-form" onSubmit={submit}>
        <AvatarUploader value={avatarUrl} name={name} description="Avatar nhóm" onChange={setAvatarUrl} disabled={saving} />
        <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="Tên nhóm" required />
        <Input multiline rows={3} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Mô tả nhóm" />
        {error && <Alert type="error" showIcon message={error} />}
        <Button className="w-full" type="submit" loading={saving}>Lưu thông tin</Button>
      </form>
    </Modal>
  );
}
