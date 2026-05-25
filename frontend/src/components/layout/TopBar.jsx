import { Link } from "react-router-dom";
import { Info, Phone, SignOut, UserCircle, VideoCamera } from "@phosphor-icons/react";
import { Avatar, Badge, Button, Layout, Tooltip, Typography } from "antd";
import { useAuth } from "../../context/AuthContext.jsx";
import { useChat } from "../../context/ChatContext.jsx";

export default function TopBar() {
  const { activeRoom, members, socketConnected } = useChat();
  const { logout, user } = useAuth();
  const otherMember = activeRoom?.type === "PRIVATE" ? members.find((member) => member.userId !== user?.id) : null;
  const onlineCount = members.filter((member) => member.userOnline).length;
  const roomName =
    activeRoom?.displayName ||
    otherMember?.userName ||
    (activeRoom?.name && activeRoom.name !== "Private chat" ? activeRoom.name : activeRoom?.type === "PRIVATE" ? "Tin nhắn riêng" : "Chọn phòng");
  const initials = roomName.slice(0, 2).toUpperCase();
  const avatar = activeRoom?.displayAvatar || otherMember?.userAvatar || activeRoom?.avatarUrl;
  const statusText = activeRoom ? (socketConnected ? "Đang hoạt động" : "Mất kết nối") : "Chọn một cuộc trò chuyện";
  const subtitle = activeRoom?.type === "FORUM"
    ? `${members.length} thành viên • ${onlineCount} online`
    : activeRoom?.description || statusText;

  return (
    <Layout.Header className="topbar">
      <div className="topbar-room">
        <Badge dot status={socketConnected ? "success" : "default"} offset={[-4, 28]}>
          <Avatar className="topbar-avatar" size={42} src={avatar}>
            {initials}
          </Avatar>
        </Badge>
        <div className="topbar-copy">
          <Typography.Text strong ellipsis>
            {roomName}
          </Typography.Text>
          <Typography.Text type="secondary" ellipsis>
            {subtitle}
          </Typography.Text>
        </div>
      </div>
      <div className="topbar-actions">
        <Tooltip title="Gọi thoại">
          <Button className="topbar-icon-button" type="text" shape="circle" icon={<Phone size={22} weight="fill" />} />
        </Tooltip>
        <Tooltip title="Gọi video">
          <Button className="topbar-icon-button" type="text" shape="circle" icon={<VideoCamera size={23} weight="fill" />} />
        </Tooltip>
        <Tooltip title="Thông tin">
          <Button className="topbar-icon-button" type="text" shape="circle" icon={<Info size={24} weight="fill" />} />
        </Tooltip>
        <Link className="profile-link" to="/profile">
          <Avatar size={30} src={user?.avatarImage} icon={!user?.avatarImage && <UserCircle size={19} weight="bold" />} />
        </Link>
        <Tooltip title="Đăng xuất">
          <Button className="topbar-logout" type="text" shape="circle" icon={<SignOut size={20} weight="bold" />} onClick={logout} />
        </Tooltip>
      </div>
    </Layout.Header>
  );
}
