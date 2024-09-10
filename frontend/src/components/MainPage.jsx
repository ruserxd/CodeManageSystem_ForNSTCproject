import CloneGit from "./CloneGit";
import ListCurProject from "./ListCurProject";
import Header from "./Header";
import '../styles/style.css'

function MainPage() {
  return (
    <div>
      <Header/>
      <ListCurProject/>
      <CloneGit />
    </div>
  );
}
export default MainPage;
