import { useState } from "react";
import { PaperPlaneTilt, X } from "@phosphor-icons/react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import AttachmentPreview from "../document/AttachmentPreview.jsx";
import UploadFileButton from "../document/UploadFileButton.jsx";
import { forumService } from "../../services/forumService.js";

export default function ForumCommentInput({ post, onCreated }) {
  const [content, setContent] = useState("");
  const [attachments, setAttachments] = useState([]);

  async function submit(event) {
    event.preventDefault();
    if (!content.trim() && attachments.length === 0) return;
    onCreated(await forumService.createComment(post.id, { content, attachmentIds: attachments.map((item) => item.id) }));
    setContent("");
    setAttachments([]);
  }

  return (
    <form className="border-t border-discord-border p-4" onSubmit={submit}>
      {attachments.length > 0 && (
        <div className="mb-3 rounded-md bg-discord-app p-2">
          <div className="mb-2 flex items-center justify-between text-xs text-discord-muted">
            <span>{attachments.length} attachment ready</span>
            <Button type="button" variant="ghost" className="h-7 w-7 px-0" onClick={() => setAttachments([])} title="Clear attachments">
              <X size={14} weight="bold" />
            </Button>
          </div>
          <AttachmentPreview attachments={attachments} />
        </div>
      )}
      <div className="flex gap-2 rounded-lg bg-discord-elevated p-2">
        <UploadFileButton onUploaded={(document) => setAttachments((items) => [...items, document])} />
        <Input value={content} onChange={(event) => setContent(event.target.value)} placeholder="Reply" className="border-0 bg-transparent focus:border-0" />
        <Button type="submit" className="h-10 w-10 shrink-0 px-0" disabled={!content.trim() && attachments.length === 0} title="Reply">
          <PaperPlaneTilt size={19} weight="fill" />
        </Button>
      </div>
    </form>
  );
}
