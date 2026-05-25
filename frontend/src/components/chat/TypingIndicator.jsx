import { Typography } from "antd";
import { useChat } from "../../context/ChatContext.jsx";

export default function TypingIndicator() {
  const { typingUsers, members } = useChat();
  const names = Object.entries(typingUsers)
    .filter(([, typing]) => typing)
    .map(([userId]) => members.find((member) => member.userId === userId)?.userName || "Ai đó");

  return (
    <div className="typing-indicator">
      {names.length > 0 && (
        <Typography.Text type="secondary">
          {names.slice(0, 2).join(", ")}
          {names.length > 2 ? ` và ${names.length - 2} người khác` : ""} đang nhập...
        </Typography.Text>
      )}
    </div>
  );
}
