import { useMemo, useState } from "react";
import { MagnifyingGlass, NotePencil, UsersThree } from "@phosphor-icons/react";
import { Button, Input, Layout, Segmented, Tooltip } from "antd";
import CreateGroupChatModal from "../room/CreateGroupChatModal.jsx";
import CreatePrivateChatModal from "../room/CreatePrivateChatModal.jsx";
import RoomList from "../room/RoomList.jsx";
import { useChat } from "../../context/ChatContext.jsx";

export default function Sidebar() {
  const { rooms, loadingRooms } = useChat();
  const [modal, setModal] = useState(null);
  const [query, setQuery] = useState("");
  const [tab, setTab] = useState("all");
  const filteredRooms = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return rooms.filter((room) => {
      const matchesTab =
        tab === "all" ||
        (tab === "groups" && room.type === "GROUP") ||
        (tab === "forums" && room.type === "FORUM") ||
        (tab === "private" && room.type === "PRIVATE");
      const displayName = room.displayName || room.name || "";
      const matchesSearch = !keyword || `${displayName} ${room.description || ""} ${room.lastMessageContent || ""}`.toLowerCase().includes(keyword);
      return matchesTab && matchesSearch;
    });
  }, [query, rooms, tab]);

  return (
    <Layout.Sider width={300} className="app-sidebar">
      <div className="sidebar-head">
        <div className="sidebar-titlebar">
          <h1>Chats</h1>
          <div className="sidebar-head-actions">
            <Tooltip title="Tạo nhóm">
              <Button className="sidebar-icon-button" type="text" shape="circle" icon={<UsersThree size={21} weight="bold" />} onClick={() => setModal("group")} />
            </Tooltip>
            <Tooltip title="Tin nhắn mới">
              <Button className="sidebar-icon-button" type="text" shape="circle" icon={<NotePencil size={21} weight="bold" />} onClick={() => setModal("private")} />
            </Tooltip>
          </div>
        </div>
        <Input
          className="sidebar-search"
          prefix={<MagnifyingGlass size={22} weight="bold" />}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search Messenger"
          allowClear
        />
        <Segmented
          className="sidebar-tabs"
          value={tab}
          onChange={setTab}
          options={[
            { label: "All", value: "all" },
            { label: "Chats", value: "private" },
            { label: "Groups", value: "groups" },
            { label: "Server", value: "forums" },
          ]}
        />
      </div>
      <div className="sidebar-scroll">
        <RoomList rooms={filteredRooms} loading={loadingRooms && rooms.length === 0} />
      </div>
      <CreatePrivateChatModal open={modal === "private"} onClose={() => setModal(null)} />
      <CreateGroupChatModal open={modal === "group"} onClose={() => setModal(null)} />
    </Layout.Sider>
  );
}
