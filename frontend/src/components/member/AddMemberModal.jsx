import { useState } from "react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import Modal from "../common/Modal.jsx";
import { useChat } from "../../context/ChatContext.jsx";
import { useAuth } from "../../context/AuthContext.jsx";
import { memberService } from "../../services/memberService.js";
import { userService } from "../../services/userService.js";

export default function AddMemberModal({ open, onClose }) {
  const { activeRoom, members, refreshMembers } = useChat();
  const { user: currentUser } = useAuth();
  const [keyword, setKeyword] = useState("");
  const [users, setUsers] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function search(event) {
    event?.preventDefault();
    setError("");
    const existingIds = new Set(members.map((member) => member.userId));
    const results = await userService.search(keyword);
    setUsers(results.filter((user) => user.id !== currentUser?.id && !existingIds.has(user.id)));
  }

  async function add(userId) {
    setLoading(true);
    setError("");
    try {
      await memberService.add(activeRoom.id, userId);
      await refreshMembers();
      setKeyword("");
      setUsers([]);
      onClose();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Could not add member");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal title="Add member" open={open} onClose={onClose}>
      <form className="flex gap-2" onSubmit={search}>
        <Input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Search users" />
        <Button type="submit" disabled={loading}>Search</Button>
      </form>
      {error && <div className="mt-3 rounded-md border border-rose-800 bg-rose-950/40 p-3 text-sm text-rose-200">{error}</div>}
      <div className="mt-3 space-y-2">
        {users.map((user) => (
          <button key={user.id} className="flex w-full items-center justify-between rounded-md bg-discord-app px-3 py-2 text-left hover:bg-discord-hover disabled:opacity-50" onClick={() => add(user.id)} disabled={loading}>
            <span>{user.name || user.gmail}</span>
            <span className="text-xs text-discord-muted">Add</span>
          </button>
        ))}
        {keyword && users.length === 0 && !error && <div className="rounded-md bg-discord-app px-3 py-2 text-sm text-discord-muted">No users found to add</div>}
      </div>
    </Modal>
  );
}
