import { useRef, useState } from "react";
import { Paperclip } from "@phosphor-icons/react";
import { Button, Tooltip, message as antdMessage } from "antd";
import { documentService } from "../../services/documentService.js";

export default function UploadFileButton({ onUploaded }) {
  const inputRef = useRef(null);
  const [loading, setLoading] = useState(false);

  async function upload(event) {
    const files = Array.from(event.target.files || []);
    if (!files.length) return;
    setLoading(true);
    try {
      const uploaded = await Promise.all(files.map((file) => documentService.upload(file)));
      uploaded.forEach((document) => onUploaded(document));
      antdMessage.success(`Đã tải ${uploaded.length} tệp lên`);
    } catch (err) {
      antdMessage.error(err?.response?.data?.message || err?.message || "Không tải được tệp");
    } finally {
      setLoading(false);
      event.target.value = "";
    }
  }

  return (
    <>
      <input ref={inputRef} className="hidden" type="file" multiple onChange={upload} />
      <Tooltip title="Đính kèm tệp">
        <Button
          className="upload-trigger"
          type="text"
          icon={<Paperclip size={20} weight="bold" />}
          onClick={() => inputRef.current?.click()}
          disabled={loading}
          loading={loading}
          title="Upload file"
        />
      </Tooltip>
    </>
  );
}
