import CloneGit from "./CloneGit";
import ListCurProject from "./ListCurProject";
import '../styles/style.css'

function MainPage() {
  return (
    <div>
      <ListCurProject />
      <CloneGit />
    </div>

  );
}
export default MainPage;
