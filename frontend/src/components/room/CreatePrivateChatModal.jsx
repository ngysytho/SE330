import { useState } from "react";
import { Alert } from "antd";
import { MagnifyingGlass } from "@phosphor-icons/react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import Modal from "../common/Modal.jsx";
import UserSearchList from "../user/UserSearchList.jsx";
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
  const [hasSearched, setHasSearched] = useState(false);

  function closeModal() {
    setKeyword("");
    setUsers([]);
    setError("");
    setHasSearched(false);
    onClose();
  }

  function updateKeyword(event) {
    setKeyword(event.target.value);
    setUsers([]);
    setError("");
    setHasSearched(false);
  }

  async function search(event) {
    event?.preventDefault();
    const term = keyword.trim();
    if (!term) {
      setUsers([]);
      setHasSearched(false);
      return;
    }
    setError("");
    setLoadingSearch(true);
    try {
      const results = await userService.search(term);
      setUsers(results.filter((user) => user.id !== currentUser?.id));
      setHasSearched(true);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Không tìm kiếm được người dùng");
      setHasSearched(true);
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
      closeModal();
      refreshRooms().catch(() => {});
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Không tạo được tin nhắn riêng");
    } finally {
      setCreatingUserId("");
    }
  }

  return (
    <Modal title="Tạo chat riêng" open={open} onClose={closeModal}>
      <form className="user-search-form" onSubmit={search}>
        <Input className="user-search-input" value={keyword} onChange={updateKeyword} placeholder="Tìm theo tên, email hoặc số điện thoại" />
        <Button
          className="user-search-button"
          type="submit"
          icon={<MagnifyingGlass size={17} weight="bold" />}
          loading={loadingSearch}
          disabled={loadingSearch || !keyword.trim() || Boolean(creatingUserId)}
        >
          Tìm
        </Button>
      </form>
      {error && <Alert className="user-search-alert" type="error" showIcon message={error} />}
      <UserSearchList
        users={users}
        keyword={keyword}
        searched={hasSearched}
        loading={loadingSearch}
        busyUserId={creatingUserId}
        actionLabel="Mở chat"
        emptyDescription="Không tìm thấy người dùng phù hợp"
        onSelect={create}
      />
    </Modal>
  );
}
