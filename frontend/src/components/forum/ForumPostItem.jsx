export default function ForumPostItem({ post, active, onClick }) {
  return (
    <button
      className={`mb-2 w-full rounded-md p-3 text-left transition ${active ? "bg-discord-hover" : "bg-discord-app hover:bg-discord-hover"}`}
      onClick={onClick}
    >
      <div className="truncate text-sm font-semibold text-discord-text">{post.title}</div>
      <div className="mt-1 line-clamp-2 text-xs text-discord-muted">{post.content}</div>
      <div className="mt-2 text-xs text-[#b5bac1]">{post.commentCount || 0} comments · {post.status}</div>
    </button>
  );
}
