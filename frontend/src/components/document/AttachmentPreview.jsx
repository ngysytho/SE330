import { useState } from "react";
import { File, ImageSquare, WarningCircle } from "@phosphor-icons/react";
import { Tag, Typography } from "antd";

const IMAGE_EXTENSION_PATTERN = /\.(avif|bmp|gif|heic|heif|jpe?g|png|svg|webp)(?=$|[?#])/i;

function formatSize(size) {
  if (!size) return "";
  if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function isImageAttachment(attachment) {
  const documentType = attachment.documentType?.toLowerCase() || "";
  if (documentType.startsWith("image/")) return true;
  const fileName = attachment.fileName || "";
  const fileUrl = attachment.fileUrl || "";
  try {
    return IMAGE_EXTENSION_PATTERN.test(fileName) || IMAGE_EXTENSION_PATTERN.test(decodeURIComponent(fileUrl));
  } catch {
    return IMAGE_EXTENSION_PATTERN.test(fileName) || IMAGE_EXTENSION_PATTERN.test(fileUrl);
  }
}

function AttachmentCard({ attachment }) {
  const [imageFailed, setImageFailed] = useState(false);
  const isImage = isImageAttachment(attachment);
  const canPreviewImage = isImage && attachment.fileUrl && !imageFailed;
  const href = attachment.fileUrl || undefined;

  return (
    <a
      className={`attachment-card ${canPreviewImage ? "image" : "file"}`}
      href={href}
      target="_blank"
      rel="noreferrer"
      onClick={(event) => {
        if (!href) event.preventDefault();
      }}
      aria-label={attachment.fileName || "Attachment"}
    >
      {canPreviewImage ? (
        <>
          <div className="attachment-image-wrap">
            <img
              className="attachment-image"
              src={attachment.fileUrl}
              alt={attachment.fileName || "attachment"}
              loading="lazy"
              onError={() => setImageFailed(true)}
            />
          </div>
          <div className="attachment-caption">
            <ImageSquare size={14} weight="bold" />
            <Typography.Text type="secondary" ellipsis>
              {attachment.fileName || "Hình ảnh"}
            </Typography.Text>
          </div>
        </>
      ) : (
        <div className="attachment-file">
          <div className="attachment-file-icon">
            {isImage && imageFailed ? <WarningCircle size={24} weight="fill" /> : <File size={24} weight="fill" />}
          </div>
          <div className="attachment-file-text">
            <Typography.Text strong ellipsis>
              {attachment.fileName || "File"}
            </Typography.Text>
            <Typography.Text type="secondary">
              {isImage && imageFailed ? "Không xem trước được ảnh" : formatSize(attachment.size)}
            </Typography.Text>
          </div>
        </div>
      )}
    </a>
  );
}

export default function AttachmentPreview({ ids = [], attachments = [] }) {
  const fallbackIds = ids?.filter((id) => !attachments?.some((item) => item.id === id)) || [];
  if (!attachments?.length && !fallbackIds.length) return null;

  return (
    <div className="attachment-grid">
      {attachments.map((attachment) => (
        <AttachmentCard key={attachment.id || attachment.fileUrl || attachment.fileName} attachment={attachment} />
      ))}
      {fallbackIds.map((id) => (
        <Tag key={id}>Attachment {id.slice(0, 6)}</Tag>
      ))}
    </div>
  );
}
