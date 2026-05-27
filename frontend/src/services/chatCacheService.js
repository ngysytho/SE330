const CACHE_PREFIX = "chatapp_cache_v2";
const MAX_CACHED_MESSAGES = 80;

function cacheKey(userId, suffix) {
  return `${CACHE_PREFIX}:${userId}:${suffix}`;
}

function readJsonCache(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function writeJsonCache(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Cache only speeds up first render; storage failures should not block chat.
  }
}

export function orderMessages(messages) {
  return [...messages].sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0));
}

export function orderRooms(rooms) {
  return [...rooms].sort((a, b) => {
    const aTime = a.lastMessageAt || a.updatedAt || a.createdAt || 0;
    const bTime = b.lastMessageAt || b.updatedAt || b.createdAt || 0;
    return bTime - aTime;
  });
}

export function readCachedRooms(userId) {
  if (!userId) return [];
  return orderRooms(readJsonCache(cacheKey(userId, "rooms"), []));
}

export function writeCachedRooms(userId, rooms) {
  if (!userId) return;
  writeJsonCache(cacheKey(userId, "rooms"), orderRooms(rooms || []));
}

export function readCachedMessages(userId, roomId) {
  if (!userId || !roomId) return [];
  return orderMessages(readJsonCache(cacheKey(userId, `messages:${roomId}`), []));
}

export function writeCachedMessages(userId, roomId, roomMessages) {
  if (!userId || !roomId) return;
  writeJsonCache(cacheKey(userId, `messages:${roomId}`), orderMessages(roomMessages || []).slice(-MAX_CACHED_MESSAGES));
}
