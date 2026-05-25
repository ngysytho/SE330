import { Modal as AntModal } from "antd";

export default function Modal({ title, children, open, onClose }) {
  return (
    <AntModal title={title} open={open} onCancel={onClose} footer={null} centered destroyOnHidden>
      {children}
    </AntModal>
  );
}
