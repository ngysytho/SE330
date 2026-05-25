import { Input as AntInput } from "antd";

export default function Input({ className = "", multiline = false, ...props }) {
  const Component = multiline ? AntInput.TextArea : AntInput;
  return <Component className={`app-input ${className}`} {...props} />;
}
