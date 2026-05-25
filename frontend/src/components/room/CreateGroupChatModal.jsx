import { useState } from "react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import Modal from "../common/Modal.jsx";
import { roomService } from "../../services/roomService.js";
import { useChat } from "../../context/ChatContext.jsx";

export default function CreateGroupChatModal({ open, onClose }) {
  const { refreshRooms, setActiveRoom } = useChat();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  async function submit(event) {
    event.preventDefault();
    const room = await roomService.createGroup({ name, description, memberIds: [], isPublic: false });
    await refreshRooms();
    setActiveRoom(room);
    onClose();
  }

  return (
    <Modal title="Create group chat" open={open} onClose={onClose}>
      <form className="space-y-3" onSubmit={submit}>
        <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="Group name" required />
        <Input value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Description" />
        <Button className="w-full" type="submit">Create</Button>
      </form>
    </Modal>
  );
}
