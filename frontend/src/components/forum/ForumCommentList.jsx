import AttachmentPreview from "../document/AttachmentPreview.jsx";

export default function ForumCommentList({ comments }) {
  return (
    <div className="min-h-0 flex-1 overflow-y-auto p-5">
      {comments.map((comment) => (
        <div key={comment.id} className="mb-3 rounded-md bg-discord-app p-3">
          <div className="text-sm font-semibold text-discord-text">{comment.authorName}</div>
          {comment.content && <p className="mt-1 whitespace-pre-wrap text-sm text-[#dbdee1]">{comment.content}</p>}
          <AttachmentPreview ids={comment.attachmentIds} attachments={comment.attachments} />
        </div>
      ))}
    </div>
  );
}
