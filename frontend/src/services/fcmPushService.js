import { deleteToken, getMessaging, getToken, isSupported, onMessage } from "firebase/messaging";
import { app } from "../config/firebase.js";
import { userService } from "./userService.js";

const FCM_TOKEN_KEY = "chatapp_fcm_token";
const FCM_TOKEN_CONFIG_KEY = "chatapp_fcm_token_config";
let foregroundUnsubscribe = null;
let registrationPromise = null;

function firebaseConfig() {
  return {
    apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
    authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
    projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
    storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
    messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
    appId: import.meta.env.VITE_FIREBASE_APP_ID,
    measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID,
  };
}

function canUseNotifications() {
  return typeof window !== "undefined" && "Notification" in window && "serviceWorker" in navigator;
}

export async function registerPushNotifications() {
  if (registrationPromise) {
    return registrationPromise;
  }
  registrationPromise = registerPushNotificationsOnce().finally(() => {
    registrationPromise = null;
  });
  return registrationPromise;
}

export async function unregisterPushNotifications() {
  const token = localStorage.getItem(FCM_TOKEN_KEY);
  if (token) {
    await userService.removeFcmToken(token).catch(() => {});
    localStorage.removeItem(FCM_TOKEN_KEY);
  }
  localStorage.removeItem(FCM_TOKEN_CONFIG_KEY);
  if (foregroundUnsubscribe) {
    foregroundUnsubscribe();
    foregroundUnsubscribe = null;
  }
  if (await messagingSupported()) {
    await deleteToken(getMessaging(app)).catch(() => {});
  }
}

async function registerPushNotificationsOnce() {
  if (!canUseNotifications() || !(await messagingSupported())) {
    console.warn("[push] Trình duyệt không hỗ trợ Notification/Service Worker/Firebase Messaging.");
    return null;
  }
  if (Notification.permission === "default") {
    const permission = await Notification.requestPermission();
    if (permission !== "granted") {
      return null;
    }
  }
  if (Notification.permission !== "granted") {
    return null;
  }

  const serviceWorkerRegistration = await registerMessagingServiceWorker();
  const messaging = getMessaging(app);
  const options = { serviceWorkerRegistration };
  const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY;
  if (vapidKey) {
    options.vapidKey = vapidKey;
  } else {
    console.warn("[push] Thiếu VITE_FIREBASE_VAPID_KEY. Notification khi đóng hẳn tab có thể không hoạt động.");
  }

  const configSignature = pushConfigSignature(vapidKey);
  let storedToken = localStorage.getItem(FCM_TOKEN_KEY);
  const storedConfigSignature = localStorage.getItem(FCM_TOKEN_CONFIG_KEY);
  if (storedToken && storedConfigSignature !== configSignature) {
    console.info("[push] Cấu hình push đã đổi, xoá FCM token cũ để lấy token mới.");
    await userService.removeFcmToken(storedToken).catch(() => {});
    localStorage.removeItem(FCM_TOKEN_KEY);
    localStorage.removeItem(FCM_TOKEN_CONFIG_KEY);
    await deleteToken(messaging).catch(() => {});
    storedToken = null;
  }

  const token = await getToken(messaging, options);
  if (!token) {
    console.warn("[push] Firebase không trả về FCM token.");
    return null;
  }

  if (storedToken && storedToken !== token) {
    await userService.removeFcmToken(storedToken).catch(() => {});
  }
  await userService.registerFcmToken(token);
  localStorage.setItem(FCM_TOKEN_KEY, token);
  localStorage.setItem(FCM_TOKEN_CONFIG_KEY, configSignature);
  console.info(`[push] Đã đăng ký FCM token ${maskToken(token)}.`);
  bindForegroundMessages(messaging);
  return token;
}

async function messagingSupported() {
  return isSupported().catch(() => false);
}

async function registerMessagingServiceWorker() {
  const config = encodeURIComponent(JSON.stringify(firebaseConfig()));
  const signature = encodeURIComponent(pushConfigSignature(import.meta.env.VITE_FIREBASE_VAPID_KEY));
  const registration = await navigator.serviceWorker.register(
    `/firebase-messaging-sw.js?firebaseConfig=${config}&pushConfig=${signature}`,
    { updateViaCache: "none" },
  );
  await registration.update().catch(() => {});
  return registration;
}

function bindForegroundMessages(messaging) {
  if (foregroundUnsubscribe) {
    return;
  }
  foregroundUnsubscribe = onMessage(messaging, (payload) => {
    showLocalNotification(payload);
  });
}

function showLocalNotification(payload) {
  if (!canUseNotifications() || Notification.permission !== "granted") {
    return;
  }
  const data = payload?.data || {};
  const title = data.title || payload?.notification?.title || "Tin nhắn mới";
  const body = data.body || payload?.notification?.body || "";
  const notification = new Notification(title, {
    body,
    icon: "/logo192.png",
    badge: "/logo192.png",
    data: {
      url: data.url || "/chat",
      roomId: data.roomId,
    },
  });
  notification.onclick = () => {
    window.focus();
    window.location.assign(notification.data?.url || "/chat");
    notification.close();
  };
}

function pushConfigSignature(vapidKey) {
  return JSON.stringify({
    projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "",
    messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "",
    appId: import.meta.env.VITE_FIREBASE_APP_ID || "",
    vapidKey: vapidKey || "",
  });
}

function maskToken(token) {
  if (!token) {
    return "<empty>";
  }
  if (token.length <= 16) {
    return `${token[0]}***${token[token.length - 1]}`;
  }
  return `${token.slice(0, 8)}...${token.slice(-6)}`;
}
