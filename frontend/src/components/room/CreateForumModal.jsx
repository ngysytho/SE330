import { useState } from "react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import Modal from "../common/Modal.jsx";
import { roomService } from "../../services/roomService.js";
import { useChat } from "../../context/ChatContext.jsx";

export default function CreateForumModal({ open, onClose }) {
  const { refreshRooms, setActiveRoom } = useChat();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  async function submit(event) {
    event.preventDefault();
    const room = await roomService.createForum({ name, description, memberIds: [], isPublic: false });
    await refreshRooms();
    setActiveRoom(room);
    onClose();
  }

  return (
    <Modal title="Create forum" open={open} onClose={onClose}>
      <form className="space-y-3" onSubmit={submit}>
        <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="Forum name" required />
        <Input value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Description" />
        <Button className="w-full" type="submit">Create</Button>
      </form>
    </Modal>
  );
}
