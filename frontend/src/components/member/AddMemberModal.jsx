import { useState } from "react";
import { Alert } from "antd";
import { MagnifyingGlass } from "@phosphor-icons/react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import Modal from "../common/Modal.jsx";
import UserSearchList from "../user/UserSearchList.jsx";
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
  const [searching, setSearching] = useState(false);
  const [addingUserId, setAddingUserId] = useState("");
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
    setSearching(true);
    try {
      const existingIds = new Set(members.map((member) => member.userId));
      const results = await userService.search(term);
      setUsers(results.filter((user) => user.id !== currentUser?.id && !existingIds.has(user.id)));
      setHasSearched(true);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Không tìm kiếm được người dùng");
      setHasSearched(true);
    } finally {
      setSearching(false);
    }
  }

  async function add(user) {
    setAddingUserId(user.id);
    setError("");
    try {
      await memberService.add(activeRoom.id, user.id);
      await refreshMembers();
      closeModal();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Không thêm được thành viên");
    } finally {
      setAddingUserId("");
    }
  }

  return (
    <Modal title="Thêm thành viên" open={open} onClose={closeModal}>
      <form className="user-search-form" onSubmit={search}>
        <Input className="user-search-input" value={keyword} onChange={updateKeyword} placeholder="Tìm theo tên, email hoặc số điện thoại" />
        <Button
          className="user-search-button"
          type="submit"
          icon={<MagnifyingGlass size={17} weight="bold" />}
          loading={searching}
          disabled={searching || !keyword.trim() || Boolean(addingUserId)}
        >
          Tìm
        </Button>
      </form>
      {error && <Alert className="user-search-alert" type="error" showIcon message={error} />}
      <UserSearchList
        users={users}
        keyword={keyword}
        searched={hasSearched}
        loading={searching}
        busyUserId={addingUserId}
        actionLabel="Thêm"
        emptyDescription="Không còn người dùng phù hợp để thêm"
        variant="add"
        onSelect={add}
      />
    </Modal>
  );
}
