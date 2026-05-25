import { Avatar, Empty, Spin, Typography } from "antd";
import { ChatCircleText, Plus, UserCircle } from "@phosphor-icons/react";

function displayName(user) {
  return user.name || user.gmail || user.phoneNumber || "Người dùng";
}

function initials(value) {
  return displayName({ name: value }).slice(0, 2).toUpperCase();
}

export default function UserSearchList({
  users = [],
  keyword = "",
  searched = false,
  loading = false,
  busyUserId = "",
  actionLabel = "Chọn",
  emptyDescription = "Không tìm thấy người dùng phù hợp",
  idleDescription = "Nhập tên, email hoặc số điện thoại để tìm người dùng.",
  variant = "chat",
  onSelect,
}) {
  const ActionIcon = variant === "add" ? Plus : ChatCircleText;

  if (loading) {
    return (
      <div className="user-search-state">
        <Spin size="small" />
        <span>Đang tìm người dùng...</span>
      </div>
    );
  }

  if (!searched || !keyword.trim()) {
    return (
      <div className="user-search-state">
        <UserCircle size={32} weight="fill" />
        <span>{idleDescription}</span>
      </div>
    );
  }

  if (!users.length) {
    return <Empty className="user-search-empty" image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyDescription} />;
  }

  return (
    <div className="user-search-list">
      {users.map((user) => {
        const name = displayName(user);
        const busy = busyUserId === user.id;
        return (
          <button
            key={user.id}
            type="button"
            className="user-result-button"
            onClick={() => onSelect?.(user)}
            disabled={Boolean(busyUserId)}
          >
            <Avatar className="user-result-avatar" size={42} src={user.avatarImage}>
              {initials(name)}
            </Avatar>
            <div className="user-result-copy">
              <Typography.Text strong ellipsis>
                {name}
              </Typography.Text>
              <Typography.Text type="secondary" ellipsis>
                {user.gmail || user.phoneNumber || "Người dùng"}
              </Typography.Text>
            </div>
            <span className="user-result-action">
              {busy ? "Đang xử lý" : actionLabel}
              <ActionIcon size={17} weight="bold" />
            </span>
          </button>
        );
      })}
    </div>
  );
}
