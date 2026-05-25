import { Layout } from "antd";
import MemberList from "../member/MemberList.jsx";
import Sidebar from "./Sidebar.jsx";
import TopBar from "./TopBar.jsx";

export default function MainLayout({ children }) {
  return (
    <Layout className="app-shell">
      <Sidebar />
      <Layout className="app-main">
        <TopBar />
        <Layout.Content className="app-content">{children}</Layout.Content>
      </Layout>
      <MemberList />
    </Layout>
  );
}
