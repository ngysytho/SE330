import { Button as AntButton } from "antd";

export default function Button({ children, className = "", variant = "primary", type, htmlType, ...props }) {
  const buttonType = variant === "primary" ? "primary" : "default";
  const isNativeType = ["button", "submit", "reset"].includes(type);
  const resolvedHtmlType = htmlType || (isNativeType ? type : undefined);

  return (
    <AntButton
      type={buttonType}
      htmlType={resolvedHtmlType}
      danger={variant === "danger"}
      className={`app-button app-button-${variant} ${className}`}
      {...props}
    >
      {children}
    </AntButton>
  );
}
