import { useEffect, useState } from "react";
import EmptyState from "../common/EmptyState.jsx";
import AttachmentPreview from "../document/AttachmentPreview.jsx";
import ForumCommentInput from "./ForumCommentInput.jsx";
import ForumCommentList from "./ForumCommentList.jsx";
import { subscribe } from "../../config/websocket.js";
import { useChat } from "../../context/ChatContext.jsx";
import { forumService } from "../../services/forumService.js";

export default function ForumPostDetail() {
  const { socketConnected } = useChat();
  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);

  useEffect(() => {
    function handle(event) {
      setPost(event.detail);
    }
    window.addEventListener("forum-active-post", handle);
    return () => window.removeEventListener("forum-active-post", handle);
  }, []);

  useEffect(() => {
    if (post) {
      forumService.listComments(post.id).then(setComments);
    }
  }, [post?.id]);

  useEffect(() => {
    if (!post?.id || !socketConnected) return undefined;
    const subscription = subscribe(`/topic/forums/posts/${post.id}/comments`, (event) => {
      if (event.type === "FORUM_COMMENT_CREATED") {
        setComments((current) => (current.some((comment) => comment.id === event.comment.id) ? current : [...current, event.comment]));
      }
      if (event.type === "FORUM_COMMENT_UPDATED") {
        setComments((current) => current.map((comment) => (comment.id === event.comment.id ? event.comment : comment)));
      }
    });
    return () => subscription?.unsubscribe();
  }, [post?.id, socketConnected]);

  if (!post) return <EmptyState title="Select a post" subtitle="Forum conversations appear here." />;

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="border-b border-discord-border p-5">
        <h2 className="text-xl font-bold">{post.title}</h2>
        <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#dbdee1]">{post.content}</p>
        <AttachmentPreview ids={post.attachmentIds} attachments={post.attachments} />
      </div>
      <ForumCommentList comments={comments} />
      <ForumCommentInput post={post} onCreated={(comment) => setComments((current) => (current.some((item) => item.id === comment.id) ? current : [...current, comment]))} />
    </div>
  );
}
