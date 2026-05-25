import { useEffect, useMemo, useState } from "react";
import { PencilSimple, Trash } from "@phosphor-icons/react";
import { Avatar, Button, Input, Popconfirm, Tooltip, Typography, message as antdMessage } from "antd";
import AttachmentPreview from "../document/AttachmentPreview.jsx";
import { useAuth } from "../../context/AuthContext.jsx";
import { useChat } from "../../context/ChatContext.jsx";

export default function MessageItem({ message }) {
  const { user } = useAuth();
  const { editMessage, deleteMessage } = useChat();
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(message.content || "");
  const [saving, setSaving] = useState(false);
  const mine = message.senderId === user?.id;
  const senderName = message.senderName || "Người dùng";
  const initials = senderName.slice(0, 2).toUpperCase();
  const timeLabel = useMemo(() => {
    if (!message.createdAt) return "";
    return new Intl.DateTimeFormat("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
    }).format(new Date(message.createdAt));
  }, [message.createdAt]);

  useEffect(() => {
    setContent(message.content || "");
  }, [message.id, message.content]);

  async function save() {
    if (saving) return;
    setSaving(true);
    try {
      await editMessage(message.id, content);
      setEditing(false);
    } catch (err) {
      antdMessage.error(err?.response?.data?.message || err?.message || "Không sửa được tin nhắn");
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    try {
      await deleteMessage(message.id);
    } catch (err) {
      antdMessage.error(err?.response?.data?.message || err?.message || "Không xoá được tin nhắn");
    }
  }

  return (
    <div className={`message-row ${mine ? "mine" : ""}`}>
      {!mine && (
        <Avatar className="message-avatar" size={34} src={message.senderAvatar}>
          {initials}
        </Avatar>
      )}
      <div className="message-body">
        {!mine && (
          <div className="message-meta">
            <Typography.Text strong>{senderName}</Typography.Text>
          </div>
        )}
        <div className="message-line">
          <div className="message-bubble">
            {editing ? (
              <div className="message-edit">
                <Input.TextArea
                  autoSize={{ minRows: 1, maxRows: 4 }}
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" && !event.shiftKey) {
                      event.preventDefault();
                      save();
                    }
                  }}
                />
                <Button type="primary" loading={saving} onClick={save}>
                  Lưu
                </Button>
              </div>
            ) : (
              message.content && <Typography.Paragraph className="message-content">{message.content}</Typography.Paragraph>
            )}
            <AttachmentPreview ids={message.attachmentIds} attachments={message.attachments} />
          </div>
          {mine && (
            <div className="message-actions">
              <Tooltip title="Sửa">
                <Button type="text" size="small" icon={<PencilSimple size={16} weight="bold" />} onClick={() => setEditing((value) => !value)} />
              </Tooltip>
              <Popconfirm title="Xoá tin nhắn?" okText="Xoá" cancelText="Huỷ" okButtonProps={{ danger: true }} onConfirm={remove}>
                <Tooltip title="Xoá">
                  <Button danger type="text" size="small" icon={<Trash size={16} weight="bold" />} />
                </Tooltip>
              </Popconfirm>
            </div>
          )}
        </div>
        <div className="message-footer">
          {mine && <span>Bạn</span>}
          {timeLabel && <span>{timeLabel}</span>}
          {message.editedAt && <span>đã sửa</span>}
        </div>
      </div>
    </div>
  );
}
