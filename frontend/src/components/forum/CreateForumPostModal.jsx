import { useState } from "react";
import Button from "../common/Button.jsx";
import Input from "../common/Input.jsx";
import Modal from "../common/Modal.jsx";
import UploadFileButton from "../document/UploadFileButton.jsx";
import { useChat } from "../../context/ChatContext.jsx";
import { forumService } from "../../services/forumService.js";
import { roomService } from "../../services/roomService.js";

export default function CreateForumPostModal({ open, onClose, onCreated }) {
  const { activeRoom } = useChat();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [attachmentIds, setAttachmentIds] = useState([]);

  async function submit(event) {
    event.preventDefault();
    let targetRoomId = activeRoom?.id;
    if (!targetRoomId || activeRoom?.type !== "FORUM") {
      const created = await roomService.defaultForum();
      targetRoomId = created.id;
    }
    const post = await forumService.createPost(targetRoomId, { title, content, attachmentIds });
    onCreated(post);
    setTitle("");
    setContent("");
    setAttachmentIds([]);
    onClose();
  }

  return (
    <Modal title="Create forum post" open={open} onClose={onClose}>
      <form className="space-y-3" onSubmit={submit}>
        <Input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Title" required />
        <Input multiline rows={5} value={content} onChange={(event) => setContent(event.target.value)} placeholder="Content" />
        <div className="flex items-center gap-2">
          <UploadFileButton onUploaded={(document) => setAttachmentIds((ids) => [...ids, document.id])} />
          <span className="text-xs text-discord-muted">{attachmentIds.length} attachments</span>
        </div>
        <Button className="w-full" type="submit">Post</Button>
      </form>
    </Modal>
  );
}
