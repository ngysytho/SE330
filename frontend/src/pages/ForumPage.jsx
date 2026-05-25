import ForumPostDetail from "../components/forum/ForumPostDetail.jsx";
import ForumPostList from "../components/forum/ForumPostList.jsx";

export default function ForumPage() {
  return (
    <div className="grid h-full min-h-0 grid-cols-[360px_minmax(0,1fr)]">
      <ForumPostList />
      <ForumPostDetail />
    </div>
  );
}
