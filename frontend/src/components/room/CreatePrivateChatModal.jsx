import { useState } from "react";
import { Alert } from "antd";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import Modal from "../common/Modal.jsx";
import { roomService } from "../../services/roomService.js";
import { userService } from "../../services/userService.js";
import { useChat } from "../../context/ChatContext.jsx";
import { useAuth } from "../../context/AuthContext.jsx";

export default function CreatePrivateChatModal({ open, onClose }) {
  const { refreshRooms, setActiveRoom } = useChat();
  const { user: currentUser } = useAuth();
  const [keyword, setKeyword] = useState("");
  const [users, setUsers] = useState([]);
  const [loadingSearch, setLoadingSearch] = useState(false);
  const [creatingUserId, setCreatingUserId] = useState("");
  const [error, setError] = useState("");

  async function search(event) {
    event?.preventDefault();
    setError("");
    setLoadingSearch(true);
    try {
      const results = await userService.search(keyword);
      setUsers(results.filter((user) => user.id !== currentUser?.id));
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Không tìm kiếm được người dùng");
    } finally {
      setLoadingSearch(false);
    }
  }

  async function create(user) {
    if (creatingUserId) return;
    setCreatingUserId(user.id);
    setError("");
    try {
      const room = await roomService.createPrivate(user.id);
      setActiveRoom({
        ...room,
        displayName: user.name || user.gmail || "Tin nhắn riêng",
        displayAvatar: user.avatarImage,
      });
      onClose();
      refreshRooms().catch(() => {});
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Không tạo được tin nhắn riêng");
    } finally {
      setCreatingUserId("");
    }
  }

  return (
    <Modal title="Create private chat" open={open} onClose={onClose}>
      <form className="flex gap-2" onSubmit={search}>
        <Input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Search users" />
        <Button type="submit" disabled={loadingSearch}>
          {loadingSearch ? "Searching" : "Search"}
        </Button>
      </form>
      {error && <Alert className="mt-3" type="error" showIcon message={error} />}
      <div className="mt-3 space-y-2">
        {users.map((user) => (
          <button
            key={user.id}
            className="flex w-full items-center justify-between rounded-md bg-discord-app px-3 py-2 text-left hover:bg-discord-hover disabled:cursor-wait disabled:opacity-60"
            onClick={() => create(user)}
            disabled={Boolean(creatingUserId)}
          >
            <span>{user.name || user.gmail}</span>
            <span className="text-xs text-discord-muted">{creatingUserId === user.id ? "Opening..." : user.gmail}</span>
          </button>
        ))}
      </div>
    </Modal>
  );
}
