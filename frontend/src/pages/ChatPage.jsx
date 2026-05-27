import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import ChatWindow from "../components/chat/ChatWindow.jsx";
import MainLayout from "../components/layout/MainLayout.jsx";
import { useChat } from "../context/ChatContext.jsx";

export default function ChatPage() {
  const [searchParams] = useSearchParams();
  const { rooms, setActiveRoom } = useChat();
  const roomId = searchParams.get("roomId");

  useEffect(() => {
    if (!roomId || !rooms.length) return;
    const room = rooms.find((item) => item.id === roomId);
    if (room) {
      setActiveRoom(room);
    }
  }, [roomId, rooms, setActiveRoom]);

  return (
    <MainLayout>
      <ChatWindow />
    </MainLayout>
  );
}
