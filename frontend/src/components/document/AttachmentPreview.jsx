import { File, ImageSquare } from "@phosphor-icons/react";
import { Image, Space, Tag, Typography } from "antd";

function formatSize(size) {
  if (!size) return "";
  if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

export default function AttachmentPreview({ ids = [], attachments = [] }) {
  const fallbackIds = ids?.filter((id) => !attachments?.some((item) => item.id === id)) || [];
  if (!attachments?.length && !fallbackIds.length) return null;

  return (
    <div className="attachment-grid">
      {attachments.map((attachment) => {
        const isImage = attachment.documentType?.startsWith("image/");
        return (
          <a
            key={attachment.id}
            className={`attachment-card ${isImage ? "image" : "file"}`}
            href={attachment.fileUrl}
            target="_blank"
            rel="noreferrer"
          >
            {isImage ? (
              <Image className="attachment-image" src={attachment.fileUrl} alt={attachment.fileName || "attachment"} preview={false} />
            ) : (
              <Space className="attachment-file" size={12}>
                <File size={24} weight="fill" />
                <div className="attachment-file-text">
                  <Typography.Text strong ellipsis>
                    {attachment.fileName || "File"}
                  </Typography.Text>
                  <Typography.Text type="secondary">{formatSize(attachment.size)}</Typography.Text>
                </div>
              </Space>
            )}
            {isImage && (
              <div className="attachment-caption">
                <ImageSquare size={14} weight="bold" />
                <Typography.Text type="secondary" ellipsis>
                  {attachment.fileName}
                </Typography.Text>
              </div>
            )}
          </a>
        );
      })}
      {fallbackIds.map((id) => (
        <Tag key={id}>Attachment {id.slice(0, 6)}</Tag>
      ))}
    </div>
  );
}
