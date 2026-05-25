import { App as AntApp, ConfigProvider, theme } from "antd";
import { AuthProvider } from "./context/AuthContext.jsx";
import { ChatProvider } from "./context/ChatContext.jsx";
import AppRoutes from "./routes/AppRoutes.jsx";

export default function App() {
  return (
    <ConfigProvider
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: "#1677ff",
          colorInfo: "#13c2c2",
          colorSuccess: "#22c55e",
          colorWarning: "#f59e0b",
          colorError: "#ef4444",
          colorText: "#172033",
          colorTextSecondary: "#667085",
          colorBgLayout: "#eef2f6",
          colorBgContainer: "#ffffff",
          colorBorder: "#d9e0ea",
          borderRadius: 8,
          fontFamily: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif",
        },
        components: {
          Button: {
            borderRadius: 8,
            controlHeight: 38,
            fontWeight: 600,
          },
          Input: {
            borderRadius: 8,
            controlHeight: 40,
          },
          Layout: {
            bodyBg: "#eef2f6",
            headerBg: "#ffffff",
            siderBg: "#15171a",
          },
          Modal: {
            borderRadiusLG: 8,
          },
        },
      }}
    >
      <AntApp>
        <AuthProvider>
          <ChatProvider>
            <AppRoutes />
          </ChatProvider>
        </AuthProvider>
      </AntApp>
    </ConfigProvider>
  );
}
