self.addEventListener("install", () => {
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(clients.claim());
});

self.addEventListener("push", (event) => {
  const payload = readPushPayload(event);
  const data = payload.data || {};
  const notification = payload.notification || {};
  const title = data.title || notification.title || "Tin nhắn mới";
  const body = data.body || notification.body || "";
  const url = data.url || payload.fcmOptions?.link || "/chat";

  console.info("[push-sw] Push received", {
    roomId: data.roomId,
    messageId: data.messageId || payload.messageId,
    title,
  });

  const options = {
    body,
    icon: notification.icon || "/logo192.png",
    badge: notification.badge || "/logo192.png",
    tag: data.messageId || notification.tag || payload.messageId || data.roomId,
    renotify: true,
    requireInteraction: true,
    silent: false,
    timestamp: Date.now(),
    data: {
      url,
      roomId: data.roomId,
      FCM_MSG: payload,
    },
  };

  event.waitUntil(
    self.registration
      .showNotification(title, options)
      .then(() => self.registration.getNotifications())
      .then((notifications) => {
        console.info("[push-sw] Notification shown", {
          title,
          activeNotifications: notifications.map((item) => item.title),
        });
      })
      .catch((error) => {
        console.error("[push-sw] showNotification failed", error);
      }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const fcmData = event.notification.data?.FCM_MSG?.data || {};
  const url = new URL(event.notification.data?.url || fcmData.url || "/chat", self.location.origin).href;
  event.waitUntil(
    clients.matchAll({ type: "window", includeUncontrolled: true }).then((clientList) => {
      const matchedClient = clientList.find((client) => client.url.includes("/chat"));
      if (matchedClient) {
        matchedClient.focus();
        return matchedClient.navigate(url);
      }
      return clients.openWindow(url);
    }),
  );
});

function readPushPayload(event) {
  if (!event.data) {
    return {};
  }
  try {
    return event.data.json();
  } catch {
    return {
      data: {
        title: "Tin nhắn mới",
        body: event.data.text(),
      },
    };
  }
}
