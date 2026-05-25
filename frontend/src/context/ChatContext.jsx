import { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import { connectWebSocket, disconnectWebSocket, send, subscribe } from "../config/websocket.js";
import { memberService } from "../services/memberService.js";
import { messageService } from "../services/messageService.js";
import { roomService } from "../services/roomService.js";
import { useAuth } from "./AuthContext.jsx";

const ChatContext = createContext(null);

function orderMessages(messages) {
  return [...messages].sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0));
}

function orderRooms(rooms) {
  return [...rooms].sort((a, b) => {
    const aTime = a.lastMessageAt || a.updatedAt || a.createdAt || 0;
    const bTime = b.lastMessageAt || b.updatedAt || b.createdAt || 0;
    return bTime - aTime;
  });
}

function uniqueMembers(memberList) {
  const byUserId = new Map();
  memberList.forEach((member) => {
    const key = member.userId || member.id;
    const current = byUserId.get(key);
    const rank = roleRank(member.role);
    const currentRank = roleRank(current?.role);
    if (!current || rank < currentRank || (rank === currentRank && (member.updatedAt || 0) > (current.updatedAt || 0))) {
      byUserId.set(key, member);
    }
  });
  return [...byUserId.values()].sort((a, b) => {
    if (Boolean(a.userOnline) !== Boolean(b.userOnline)) return a.userOnline ? -1 : 1;
    const roleDiff = roleRank(a.role) - roleRank(b.role);
    if (roleDiff) return roleDiff;
    return (a.userName || a.userEmail || "").localeCompare(b.userName || b.userEmail || "", "vi");
  });
}

function roleRank(role) {
  if (role === "OWNER") return 0;
  if (role === "ADMIN") return 1;
  return 2;
}

function upsertMessage(messages, nextMessage) {
  if (!nextMessage?.id) return messages;
  const exists = messages.some((message) => message.id === nextMessage.id);
  const next = exists
    ? messages.map((message) => (message.id === nextMessage.id ? nextMessage : message))
    : [...messages, nextMessage];
  return orderMessages(next);
}

function upsertRoom(rooms, nextRoom) {
  if (!nextRoom?.id) return rooms;
  const exists = rooms.some((room) => room.id === nextRoom.id);
  const next = exists
    ? rooms.map((room) => (room.id === nextRoom.id ? { ...room, ...nextRoom } : room))
    : [...rooms, nextRoom];
  return orderRooms(next);
}

function messagePreview(message) {
  if (message?.content?.trim()) return message.content;
  if (message?.attachments?.length) return message.messageType === "IMAGE" ? "Hình ảnh" : "Tệp đính kèm";
  if (message?.attachmentIds?.length) return message.messageType === "IMAGE" ? "Hình ảnh" : "Tệp đính kèm";
  return "";
}

function applyMessagePreviewToRooms(rooms, message) {
  if (!message?.roomId) return rooms;
  return orderRooms(
    rooms.map((room) =>
      room.id === message.roomId
        ? {
            ...room,
            lastMessageId: message.id,
            lastMessageContent: messagePreview(message),
            lastMessageAt: message.createdAt,
            updatedAt: message.updatedAt || message.createdAt || room.updatedAt,
          }
        : room,
    ),
  );
}

export function ChatProvider({ children }) {
  const { user, accessToken } = useAuth() || {};
  const privateRoomDisplayCacheRef = useRef(new Map());
  const activeRoomRef = useRef(null);
  const roomsRef = useRef([]);
  const [rooms, setRooms] = useState([]);
  const [activeRoom, setActiveRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [members, setMembers] = useState([]);
  const [typingUsers, setTypingUsers] = useState({});
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [roomError, setRoomError] = useState("");
  const [socketConnected, setSocketConnected] = useState(false);
  const roomTopicKey = useMemo(() => {
    const ids = new Set(rooms.map((room) => room.id).filter(Boolean));
    if (activeRoom?.id) ids.add(activeRoom.id);
    return [...ids].sort().join("|");
  }, [activeRoom?.id, rooms]);

  useEffect(() => {
    activeRoomRef.current = activeRoom;
  }, [activeRoom]);

  useEffect(() => {
    roomsRef.current = rooms;
  }, [rooms]);

  useEffect(() => {
    if (!user || !accessToken) {
      disconnectWebSocket();
      setSocketConnected(false);
      setRooms([]);
      setActiveRoom(null);
      setMessages([]);
      setMembers([]);
      return;
    }
    connectWebSocket(accessToken, () => setSocketConnected(true), () => setSocketConnected(false));
    refreshRooms();
  }, [user?.id, accessToken]);

  useEffect(() => {
    let cancelled = false;
    if (!activeRoom) {
      setMessages([]);
      setMembers([]);
      setTypingUsers({});
      setRoomError("");
      setLoadingMessages(false);
      return undefined;
    }

    async function loadRoomData(room) {
      setLoadingMessages(true);
      setRoomError("");
      setTypingUsers({});
      try {
        const [roomMessages, roomMembers] = await Promise.all([
          messageService.list(room.id, { pageSize: 50 }),
          memberService.list(room.id),
        ]);
        if (cancelled) return;
        setMessages(orderMessages(roomMessages));
        setMembers(uniqueMembers(roomMembers));
      } catch (err) {
        if (cancelled) return;
        setMessages([]);
        setMembers([]);
        setRoomError(err?.response?.data?.message || err?.message || "Could not load this room");
      } finally {
        if (!cancelled) {
          setLoadingMessages(false);
        }
      }
    }

    loadRoomData(activeRoom);

    return () => {
      cancelled = true;
    };
  }, [activeRoom?.id]);

  useEffect(() => {
    if (!socketConnected || !roomTopicKey) {
      return undefined;
    }

    const subscriptions = roomTopicKey
      .split("|")
      .map((roomId) => subscribe(`/topic/rooms/${roomId}`, (event) => applyRoomEvent(event, false)))
      .filter(Boolean);

    return () => subscriptions.forEach((subscription) => subscription.unsubscribe());
  }, [roomTopicKey, socketConnected]);

  useEffect(() => {
    if (!activeRoom || !socketConnected) {
      return undefined;
    }

    const typingSubscription = subscribe(`/topic/rooms/${activeRoom.id}/typing`, (event) => {
      if (event.userId === user?.id) return;
      setTypingUsers((current) => ({ ...current, [event.userId]: event.typing }));
      if (event.typing) {
        window.setTimeout(() => setTypingUsers((current) => ({ ...current, [event.userId]: false })), 2500);
      }
    });

    return () => {
      typingSubscription?.unsubscribe();
    };
  }, [activeRoom?.id, socketConnected, user?.id]);

  useEffect(() => {
    if (!socketConnected || !user?.id) {
      return undefined;
    }

    const userRoomSubscription = subscribe("/user/queue/rooms", applyRoomEvent);

    return () => userRoomSubscription?.unsubscribe();
  }, [socketConnected, user?.id]);

  useEffect(() => {
    if (!socketConnected) {
      return undefined;
    }

    const presenceSubscription = subscribe("/topic/presence", (event) => {
      if (event.type !== "PRESENCE_CHANGED") return;
      setMembers((current) =>
        uniqueMembers(
          current.map((member) =>
            member.userId === event.userId
              ? { ...member, userOnline: event.online, userLastSeenAt: event.lastSeenAt }
              : member,
          ),
        ),
      );
    });

    return () => presenceSubscription?.unsubscribe();
  }, [socketConnected]);

  useEffect(() => {
    if (socketConnected && activeRoom) {
      refreshMembers(activeRoom);
    }
  }, [socketConnected, activeRoom?.id]);

  async function refreshRooms() {
    setLoadingRooms(true);
    try {
      const data = await roomService.my();
      const roomMap = new Map();
      data.filter(Boolean).forEach((room) => roomMap.set(room.id, room));
      const nextRooms = orderRooms(await hydratePrivateRooms([...roomMap.values()]));
      setRooms(nextRooms);
      setActiveRoom((current) => {
        if (!nextRooms.length) return null;
        if (!current) return nextRooms[0];
        return nextRooms.find((room) => room.id === current.id) || nextRooms[0];
      });
    } finally {
      setLoadingRooms(false);
    }
  }

  async function hydratePrivateRooms(roomList) {
    return Promise.all(
      roomList.map(async (room) => {
        if (room.type !== "PRIVATE" || (room.name && room.name !== "Private chat")) {
          return room;
        }
        const cached = privateRoomDisplayCacheRef.current.get(room.id);
        if (cached) {
          return { ...room, ...cached };
        }
        try {
          const roomMembers = await memberService.list(room.id);
          const otherMember = roomMembers.find((member) => member.userId !== user?.id) || roomMembers[0];
          const display = {
            displayName: otherMember?.userName || otherMember?.userEmail || "Tin nhắn riêng",
            displayAvatar: otherMember?.userAvatar,
          };
          privateRoomDisplayCacheRef.current.set(room.id, display);
          return {
            ...room,
            ...display,
          };
        } catch {
          return room;
        }
      }),
    );
  }

  function applyRoomEvent(event, shouldRefreshRooms = true) {
    if (!event?.type) return;
    const eventRoomId = event.roomId || event.message?.roomId;
    const currentActiveRoom = activeRoomRef.current;

    if (event.type === "ROOM_DELETED") {
      setRooms((current) => current.filter((room) => room.id !== eventRoomId));
      setActiveRoom((current) => (current?.id === eventRoomId ? null : current));
      return;
    }

    if (event.room) {
      setRooms((current) => upsertRoom(current, event.room));
      setActiveRoom((current) => (current?.id === event.room.id ? { ...current, ...event.room } : current));
    }

    if (event.type === "MESSAGE_CREATED") {
      if (eventRoomId === currentActiveRoom?.id) {
        setMessages((current) => upsertMessage(current, event.message));
      }
      if (!roomsRef.current.some((room) => room.id === eventRoomId) && shouldRefreshRooms) {
        refreshRooms();
      }
      setRooms((current) => applyMessagePreviewToRooms(current, event.message));
      setActiveRoom((current) =>
        current?.id === eventRoomId
          ? {
              ...current,
              lastMessageId: event.message?.id,
              lastMessageContent: messagePreview(event.message),
              lastMessageAt: event.message?.createdAt,
              updatedAt: event.message?.updatedAt || event.message?.createdAt || current.updatedAt,
            }
          : current,
      );
      return;
    }

    if (event.type === "MESSAGE_UPDATED" && event.message) {
      if (eventRoomId === currentActiveRoom?.id) {
        setMessages((current) => current.map((message) => (message.id === event.message.id ? event.message : message)));
      }
      setRooms((current) =>
        orderRooms(
          current.map((room) =>
            room.id === eventRoomId && room.lastMessageId === event.message.id
              ? { ...room, lastMessageContent: messagePreview(event.message), updatedAt: event.message.updatedAt || room.updatedAt }
              : room,
          ),
        ),
      );
      return;
    }

    if (event.type === "MESSAGE_DELETED") {
      if (eventRoomId === currentActiveRoom?.id) {
        setMessages((current) => current.filter((message) => message.id !== event.messageId));
      }
      setRooms((current) =>
        current.map((room) =>
          room.id === eventRoomId && room.lastMessageId === event.messageId
            ? { ...room, lastMessageId: null, lastMessageContent: "", lastMessageAt: null }
            : room,
        ),
      );
      return;
    }

    if (event.type === "ROOM_UPSERTED" && shouldRefreshRooms) {
      refreshRooms();
    }
  }

  async function refreshMembers(room = activeRoom) {
    if (!room) return;
    setMembers(uniqueMembers(await memberService.list(room.id)));
  }

  async function sendMessage(content, attachmentIds = [], messageType = "TEXT") {
    if (!activeRoom || (!content.trim() && attachmentIds.length === 0)) return;
    const payload = {
      roomId: activeRoom.id,
      content,
      attachmentIds,
      messageType,
    };
    const message = await messageService.send(activeRoom.id, payload);
    setMessages((current) => upsertMessage(current, message));
    setRooms((current) => applyMessagePreviewToRooms(current, message));
    setActiveRoom((current) =>
      current?.id === activeRoom.id
        ? {
            ...current,
            lastMessageId: message.id,
            lastMessageContent: messagePreview(message),
            lastMessageAt: message.createdAt,
            updatedAt: message.updatedAt || message.createdAt || current.updatedAt,
          }
        : current,
    );
    return message;
  }

  async function editMessage(messageId, content) {
    const message = await messageService.update(messageId, content);
    setMessages((current) => current.map((item) => (item.id === message.id ? message : item)));
    return message;
  }

  async function deleteMessage(messageId) {
    await messageService.remove(messageId);
    setMessages((current) => current.filter((message) => message.id !== messageId));
    refreshRooms();
  }

  function setTyping(typing) {
    if (activeRoom) {
      try {
        send("/app/chat.typing", { roomId: activeRoom.id, typing });
      } catch {
        // Typing is best-effort realtime state.
      }
    }
  }

  const value = useMemo(
    () => ({
      rooms,
      activeRoom,
      messages,
      members,
      typingUsers,
      loadingRooms,
      loadingMessages,
      roomError,
      socketConnected,
      setActiveRoom,
      refreshRooms,
      refreshMembers,
      sendMessage,
      editMessage,
      deleteMessage,
      setTyping,
    }),
    [rooms, activeRoom, messages, members, typingUsers, loadingRooms, loadingMessages, roomError, socketConnected],
  );

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export function useChat() {
  return useContext(ChatContext);
}
