export default function EmptyState({ title = "Nothing here yet", subtitle = "Start something new." }) {
  return (
    <div className="grid h-full place-items-center text-center">
      <div>
        <h3 className="text-lg font-semibold text-discord-text">{title}</h3>
        <p className="mt-1 text-sm text-discord-muted">{subtitle}</p>
      </div>
    </div>
  );
}
