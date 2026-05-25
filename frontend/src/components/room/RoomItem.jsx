import { Avatar, Typography } from "antd";
import { useChat } from "../../context/ChatContext.jsx";

export default function RoomItem({ room }) {
  const { activeRoom, setActiveRoom } = useChat();
  const active = activeRoom?.id === room.id;
  const name = room.displayName || (room.name && room.name !== "Private chat" ? room.name : room.type === "PRIVATE" ? "Tin nhắn riêng" : "Phòng chat");
  const initials = name.slice(0, 2).toUpperCase();
  const timeLabel = room.lastMessageAt
    ? new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit" }).format(new Date(room.lastMessageAt))
    : "";
  const preview = room.lastMessageContent || (room.type === "GROUP" ? "Nhóm chat" : room.type === "FORUM" ? "Phòng chat chung" : room.description || "Đang hoạt động");
  const unreadCount = Number(room.unreadCount) || 0;
  const unreadLabel = unreadCount > 99 ? "99+" : unreadCount;

  return (
    <button
      className={`room-item ${active ? "is-active" : ""}`}
      onClick={() => setActiveRoom(room)}
    >
      <Avatar className="room-avatar" shape="circle" src={room.displayAvatar || room.avatarUrl}>
        {initials}
      </Avatar>
      <div className="room-copy">
        <div className="room-name-line">
          <Typography.Text strong ellipsis>
            {name}
          </Typography.Text>
          <div className="room-meta">
            {timeLabel && <span className="room-time">{timeLabel}</span>}
            {unreadCount > 0 && <span className="room-unread-badge">{unreadLabel}</span>}
          </div>
        </div>
        <Typography.Text type="secondary" ellipsis>
          {preview}
        </Typography.Text>
      </div>
    </button>
  );
}
