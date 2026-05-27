import { Empty, Skeleton } from "antd";
import MessageInput from "./MessageInput.jsx";
import MessageList from "./MessageList.jsx";
import TypingIndicator from "./TypingIndicator.jsx";
import { useChat } from "../../context/ChatContext.jsx";

export default function ChatWindow() {
  const { activeRoom, loadingRooms } = useChat();

  if (!activeRoom) {
    if (loadingRooms) {
      return (
        <div className="chat-shell">
          <div className="message-skeletons">
            <Skeleton avatar active paragraph={{ rows: 2 }} />
            <Skeleton avatar active paragraph={{ rows: 2 }} />
            <Skeleton avatar active paragraph={{ rows: 2 }} />
          </div>
        </div>
      );
    }
    return (
      <div className="chat-empty">
        <Empty description="Chọn một phòng chat để bắt đầu" />
      </div>
    );
  }

  return (
    <div className="chat-shell">
      <MessageList />
      <TypingIndicator />
      <MessageInput />
    </div>
  );
}
