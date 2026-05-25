import { Empty, Typography } from "antd";
import RoomItem from "./RoomItem.jsx";

export default function RoomList({ title, rooms, icon }) {
  return (
    <section className="room-section">
      {title && (
        <div className="room-section-title">
          {icon}
          <Typography.Text>{title}</Typography.Text>
        </div>
      )}
      <div className="room-list">
        {rooms.map((room) => (
          <RoomItem key={room.id} room={room} />
        ))}
        {rooms.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không có cuộc trò chuyện" />}
      </div>
    </section>
  );
}
