import { Outlet, Link } from "react-router-dom";
import Footer from "./Footer";

function Layout({user}) {
  return (
    <div className="layout">
      <header className="header">
        <div className="topBar">
          <h2 className="mainTitle">程式單元開發管理系統</h2>
          <nav className="navigation">
            <ul>
              <li><Link to="/">首頁</Link></li>
              <li><Link to="/About">關於我們</Link></li>
              <li><Link to="/Contact">聯絡我們</Link></li>
              {user ? (
                <li><Link to="/UserPage">Welcome {user.myUser.userName}</Link></li>
              ):(
                <li><Link to="/Login">登入</Link></li>
              )}
            </ul>
          </nav>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

export default Layout;
