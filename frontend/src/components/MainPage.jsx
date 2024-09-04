import CloneGit from "./CloneGit";
import ListCurProject from "./ListCurProject";
import '../styles/style.css'

function MainPage() {
  return (
    <div>
      <h1>Weclome Main Page</h1>
      <ListCurProject/>
      <CloneGit />
    </div>
  );
}
export default MainPage;
