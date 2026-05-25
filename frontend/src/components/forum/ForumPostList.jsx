import { useEffect, useState } from "react";
import Button from "../common/Button.jsx";
import EmptyState from "../common/EmptyState.jsx";
import CreateForumPostModal from "./CreateForumPostModal.jsx";
import ForumPostItem from "./ForumPostItem.jsx";
import { subscribe } from "../../config/websocket.js";
import { useChat } from "../../context/ChatContext.jsx";
import { forumService } from "../../services/forumService.js";
import { roomService } from "../../services/roomService.js";

export default function ForumPostList() {
  const { activeRoom, socketConnected } = useChat();
  const [posts, setPosts] = useState([]);
  const [activePost, setActivePost] = useState(null);
  const [creating, setCreating] = useState(false);
  const [forumRoom, setForumRoom] = useState(null);

  useEffect(() => {
    let mounted = true;
    async function load() {
      if (!activeRoom) return;
      let room = activeRoom;
      if (activeRoom.type !== "FORUM") {
        room = await roomService.defaultForum();
      }
      const data = await forumService.listPosts(room.id);
      if (!mounted) return;
      setForumRoom(room);
      setPosts(data);
      setActivePost(data[0] || null);
    }
    load();
    return () => {
      mounted = false;
    };
  }, [activeRoom?.id]);

  useEffect(() => {
    if (!forumRoom?.id || !socketConnected) return undefined;
    const subscription = subscribe(`/topic/forums/${forumRoom.id}/posts`, (event) => {
      if (event.type === "FORUM_POST_CREATED") {
        setPosts((current) => (current.some((post) => post.id === event.post.id) ? current : [event.post, ...current]));
        setActivePost((current) => current || event.post);
      }
      if (event.type === "FORUM_POST_UPDATED") {
        setPosts((current) => current.map((post) => (post.id === event.post.id ? event.post : post)));
        setActivePost((current) => (current?.id === event.post.id ? event.post : current));
      }
      if (event.type === "FORUM_POST_DELETED") {
        setPosts((current) => current.filter((post) => post.id !== event.postId));
        setActivePost((current) => (current?.id === event.postId ? null : current));
      }
    });
    return () => subscription?.unsubscribe();
  }, [forumRoom?.id, socketConnected]);

  useEffect(() => {
    window.dispatchEvent(new CustomEvent("forum-active-post", { detail: activePost }));
  }, [activePost]);

  return (
    <div className="min-h-0 border-r border-discord-border bg-discord-sidebar">
      <div className="flex h-14 items-center justify-between border-b border-discord-border px-4">
        <div className="text-sm font-semibold">Posts</div>
        <Button variant="secondary" onClick={() => setCreating(true)}>New</Button>
      </div>
      <div className="h-[calc(100%-3.5rem)] overflow-y-auto p-3">
        {posts.length === 0 ? (
          <EmptyState title="No posts" subtitle="Create the first thread." />
        ) : (
          posts.map((post) => <ForumPostItem key={post.id} post={post} active={activePost?.id === post.id} onClick={() => setActivePost(post)} />)
        )}
      </div>
      <CreateForumPostModal
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={(post) => {
          setPosts((current) => (current.some((item) => item.id === post.id) ? current : [post, ...current]));
          setActivePost(post);
        }}
      />
    </div>
  );
}
