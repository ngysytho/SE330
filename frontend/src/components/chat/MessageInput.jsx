import { useRef, useState } from "react";
import { PaperPlaneTilt, X } from "@phosphor-icons/react";
import { Alert, Button, Input, Tooltip } from "antd";
import AttachmentPreview from "../document/AttachmentPreview.jsx";
import UploadFileButton from "../document/UploadFileButton.jsx";
import { useChat } from "../../context/ChatContext.jsx";

export default function MessageInput() {
  const { sendMessage, setTyping } = useChat();
  const [content, setContent] = useState("");
  const [attachments, setAttachments] = useState([]);
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const sendingRef = useRef(false);

  async function doSend() {
    if (sendingRef.current || (!content.trim() && attachments.length === 0)) return;
    sendingRef.current = true;
    setSending(true);
    setError("");
    try {
      const attachmentIds = attachments.map((attachment) => attachment.id);
      const messageType = attachments.length
        ? attachments.every((attachment) => attachment.documentType?.startsWith("image/"))
          ? "IMAGE"
          : "FILE"
        : "TEXT";
      await sendMessage(content, attachmentIds, messageType);
      setContent("");
      setAttachments([]);
      setTyping(false);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Could not send message");
    } finally {
      sendingRef.current = false;
      setSending(false);
    }
  }

  async function submit(event) {
    event?.preventDefault();
    await doSend();
  }

  function handleKeyDown(event) {
    if (event.nativeEvent?.isComposing) return;
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      doSend();
    }
  }

  return (
    <form className="message-composer" onSubmit={submit}>
      {attachments.length > 0 && (
        <div className="composer-attachments">
          <div className="composer-attachment-head">
            <span>{attachments.length} tệp sẵn sàng gửi</span>
            <Tooltip title="Bỏ tệp đính kèm">
              <Button type="text" size="small" icon={<X size={14} weight="bold" />} onClick={() => setAttachments([])} />
            </Tooltip>
          </div>
          <AttachmentPreview attachments={attachments} />
        </div>
      )}
      {error && <Alert className="composer-alert" type="error" showIcon message={error} />}
      <div className="composer-box">
        <UploadFileButton onUploaded={(document) => setAttachments((items) => [...items, document])} />
        <Input.TextArea
          className="composer-input"
          autoSize={{ minRows: 1, maxRows: 5 }}
          value={content}
          onChange={(event) => {
            setContent(event.target.value);
            setTyping(true);
          }}
          onBlur={() => setTyping(false)}
          onKeyDown={handleKeyDown}
          placeholder="Aa"
        />
        <Tooltip title="Gửi tin nhắn">
          <Button
            type="primary"
            htmlType="submit"
            className="composer-send"
            loading={sending}
            disabled={!content.trim() && attachments.length === 0}
            icon={!sending && <PaperPlaneTilt size={20} weight="fill" />}
            shape="circle"
            aria-label="Gửi tin nhắn"
          />
        </Tooltip>
      </div>
    </form>
  );
}
