import { Empty, Skeleton, Typography } from "antd";
import RoomItem from "./RoomItem.jsx";

export default function RoomList({ title, rooms, icon, loading = false }) {
  return (
    <section className="room-section">
      {title && (
        <div className="room-section-title">
          {icon}
          <Typography.Text>{title}</Typography.Text>
        </div>
      )}
      <div className="room-list">
        {loading && rooms.length === 0 && (
          <div className="room-list-loading">
            <Skeleton avatar active paragraph={{ rows: 1 }} />
            <Skeleton avatar active paragraph={{ rows: 1 }} />
            <Skeleton avatar active paragraph={{ rows: 1 }} />
          </div>
        )}
        {rooms.map((room) => (
          <RoomItem key={room.id} room={room} />
        ))}
        {!loading && rooms.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không có cuộc trò chuyện" />}
      </div>
    </section>
  );
}
