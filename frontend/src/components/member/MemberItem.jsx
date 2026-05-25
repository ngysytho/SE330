import { Avatar, Badge, Tag, Typography } from "antd";

export default function MemberItem({ member, showRole = true }) {
  const displayName = member.userName || member.userEmail || "Người dùng";
  const initials = displayName.slice(0, 2).toUpperCase();
  const isOnline = Boolean(member.userOnline);
  const statusLabel = member.status !== "ACTIVE" ? member.status : isOnline ? "ONLINE" : "OFFLINE";

  return (
    <div className="member-item">
      <Badge dot color={isOnline ? "#22c55e" : "#98a2b3"} offset={[-2, 30]}>
        <Avatar size={36} src={member.userAvatar}>
          {initials}
        </Avatar>
      </Badge>
      <div className="member-copy">
        <Typography.Text strong ellipsis>
          {displayName}
        </Typography.Text>
        <div className="member-meta">
          {showRole && <Tag color={member.role === "OWNER" ? "gold" : member.role === "ADMIN" ? "cyan" : "default"}>{member.role}</Tag>}
          <Typography.Text type="secondary">{statusLabel}</Typography.Text>
        </div>
      </div>
    </div>
  );
}
