import { useEffect, useRef } from "react";
import { Alert, Empty, Skeleton } from "antd";
import MessageItem from "./MessageItem.jsx";
import { useChat } from "../../context/ChatContext.jsx";

export default function MessageList() {
  const { messages, loadingMessages, roomError } = useChat();
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length]);

  return (
    <div className="messages-scroll">
      {roomError && <Alert className="message-alert" type="error" showIcon message={roomError} />}
      {loadingMessages ? (
        <div className="message-skeletons">
          <Skeleton avatar active paragraph={{ rows: 2 }} />
          <Skeleton avatar active paragraph={{ rows: 2 }} />
          <Skeleton avatar active paragraph={{ rows: 2 }} />
        </div>
      ) : messages.length === 0 && !roomError ? (
        <div className="chat-empty compact">
          <Empty description="Chưa có tin nhắn nào" />
        </div>
      ) : (
        messages.map((message) => <MessageItem key={message.id} message={message} />)
      )}
      <div ref={bottomRef} />
    </div>
  );
}
