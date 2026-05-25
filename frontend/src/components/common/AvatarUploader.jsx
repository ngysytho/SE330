import { useRef, useState } from "react";
import { Camera, Trash } from "@phosphor-icons/react";
import { Avatar, Button, Typography } from "antd";
import { documentService } from "../../services/documentService.js";

function initialsFrom(value) {
  const text = (value || "").trim();
  if (!text) return "AV";
  return text.slice(0, 2).toUpperCase();
}

export default function AvatarUploader({
  value,
  onChange,
  name,
  description = "Ảnh đại diện",
  size = 96,
  className = "",
  disabled = false,
}) {
  const inputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");

  async function handleFile(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setError("Chỉ chọn file ảnh.");
      event.target.value = "";
      return;
    }

    setUploading(true);
    setError("");
    try {
      const document = await documentService.upload(file);
      onChange(document.fileUrl);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Upload ảnh thất bại.");
    } finally {
      setUploading(false);
      event.target.value = "";
    }
  }

  return (
    <div className={`avatar-uploader ${className}`}>
      <div className="avatar-uploader-preview">
        <Avatar className="avatar-uploader-avatar" size={size} src={value}>
          {initialsFrom(name)}
        </Avatar>
        <button
          className="avatar-uploader-camera"
          type="button"
          disabled={disabled || uploading}
          onClick={() => inputRef.current?.click()}
          aria-label="Chọn ảnh đại diện"
        >
          <Camera size={18} weight="fill" />
        </button>
      </div>
      <div className="avatar-uploader-copy">
        <Typography.Text strong ellipsis>
          {name || description}
        </Typography.Text>
        <Typography.Text type="secondary">{uploading ? "Đang upload..." : description}</Typography.Text>
        {error && <span className="avatar-uploader-error">{error}</span>}
        <div className="avatar-uploader-actions">
          <Button size="small" loading={uploading} disabled={disabled} onClick={() => inputRef.current?.click()}>
            Chọn ảnh
          </Button>
          {value && (
            <Button size="small" type="text" danger disabled={disabled || uploading} icon={<Trash size={15} />} onClick={() => onChange("")}>
              Xóa
            </Button>
          )}
        </div>
      </div>
      <input ref={inputRef} type="file" accept="image/*" hidden onChange={handleFile} />
    </div>
  );
}
