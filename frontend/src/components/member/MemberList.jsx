import { useMemo, useState } from "react";
import { ChatCircleDots, LockSimple, MagnifyingGlass, PencilSimple, UserCircle, UserPlus, UsersThree } from "@phosphor-icons/react";
import { Avatar, Button, Empty, Layout, Tooltip } from "antd";
import AddMemberModal from "./AddMemberModal.jsx";
import MemberItem from "./MemberItem.jsx";
import { useAuth } from "../../context/AuthContext.jsx";
import { useChat } from "../../context/ChatContext.jsx";
import EditRoomModal from "../room/EditRoomModal.jsx";

export default function MemberList() {
  const { members, activeRoom } = useChat();
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const visibleMembers = useMemo(() => {
    const byUserId = new Map();
    members.forEach((member) => {
      const key = member.userId || member.id;
      if (!byUserId.has(key)) byUserId.set(key, member);
    });
    return [...byUserId.values()].sort((a, b) => {
      if (Boolean(a.userOnline) !== Boolean(b.userOnline)) return a.userOnline ? -1 : 1;
      if (activeRoom?.type !== "PRIVATE") {
        if (a.role === "OWNER" && b.role !== "OWNER") return -1;
        if (a.role !== "OWNER" && b.role === "OWNER") return 1;
      }
      return (a.userName || a.userEmail || "").localeCompare(b.userName || b.userEmail || "", "vi");
    });
  }, [activeRoom?.type, members]);
  const onlineCount = visibleMembers.filter((member) => member.userOnline).length;
  const otherMember = activeRoom?.type === "PRIVATE" ? members.find((member) => member.userId !== user?.id) : null;
  const displayName =
    activeRoom?.displayName ||
    otherMember?.userName ||
    (activeRoom?.name && activeRoom.name !== "Private chat" ? activeRoom.name : activeRoom?.type === "PRIVATE" ? "Tin nhắn riêng" : "Chat info");
  const avatar = activeRoom?.displayAvatar || otherMember?.userAvatar || activeRoom?.avatarUrl;
  const initials = displayName.slice(0, 2).toUpperCase();
  const canEditRoom = ["GROUP", "FORUM"].includes(activeRoom?.type);
  const roomDescription = activeRoom?.description || (activeRoom?.type === "FORUM" ? "Phòng chat chung của server" : "Đang hoạt động");

  return (
    <Layout.Sider width={300} className="member-panel">
      <div className="member-scroll">
        {activeRoom ? (
          <>
            <div className="chat-info-profile">
              <Avatar className="chat-info-avatar" size={96} src={avatar}>
                {initials}
              </Avatar>
              <h2>{displayName}</h2>
              <p>{roomDescription}</p>
              <div className="chat-info-pill">
                <LockSimple size={15} weight="fill" />
                <span>Kết nối realtime</span>
              </div>
              <div className="chat-info-actions">
                <button className="chat-info-action" type="button">
                  <span><MagnifyingGlass size={24} weight="bold" /></span>
                  <strong>Search</strong>
                </button>
                {canEditRoom && (
                  <button className="chat-info-action" type="button" onClick={() => setEditOpen(true)}>
                    <span><PencilSimple size={24} weight="fill" /></span>
                    <strong>Edit</strong>
                  </button>
                )}
              </div>
            </div>

            <section className="chat-info-section">
              <div className="chat-info-row static">
                <ChatCircleDots size={23} weight="fill" />
                <span>{activeRoom.type === "FORUM" ? "Server chung" : activeRoom.type === "GROUP" ? "Nhóm chat" : "Tin nhắn riêng"}</span>
              </div>
              <div className="chat-info-row static">
                <UsersThree size={23} weight="fill" />
                <span>{visibleMembers.length} thành viên</span>
              </div>
              <div className="chat-info-row static">
                <UserCircle size={23} weight="fill" />
                <span>{onlineCount} online</span>
              </div>
            </section>

            <section className="chat-info-section">
              <div className="member-head">
                <span>Thành viên</span>
                {activeRoom.type !== "FORUM" && (
                  <Tooltip title="Thêm thành viên">
                    <Button className="member-add-button" type="text" shape="circle" icon={<UserPlus size={20} weight="bold" />} onClick={() => setOpen(true)} />
                  </Tooltip>
                )}
              </div>
              {visibleMembers.map((member) => (
                <MemberItem key={member.id} member={member} showRole={activeRoom.type !== "PRIVATE"} />
              ))}
              {visibleMembers.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có thành viên" />}
            </section>
          </>
        ) : (
          <div className="chat-empty compact">
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chọn một cuộc trò chuyện" />
          </div>
        )}
      </div>
      <AddMemberModal open={open} onClose={() => setOpen(false)} />
      <EditRoomModal open={editOpen} onClose={() => setEditOpen(false)} room={activeRoom} />
    </Layout.Sider>
  );
}
