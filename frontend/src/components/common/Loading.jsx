export default function Loading({ fullScreen = false }) {
  return (
    <div className={`${fullScreen ? "grid h-screen place-items-center" : "grid h-full place-items-center"} bg-discord-app text-discord-muted`}>
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-discord-border border-t-brand" />
    </div>
  );
}
