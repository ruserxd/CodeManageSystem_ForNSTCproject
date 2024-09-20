import { Outlet, Link } from "react-router-dom";
import Footer from "./Footer";

function Layout() {
  return (
    <div>
      <header className="header">
        <div className="topBar">
          <h1 className="mainTitle">程式單元開發管理系統</h1>
          <nav className="navigation">
            <ul>
              <li><Link to="/">首頁</Link></li>
              <li><Link to="/About">關於我們</Link></li>
              <li><Link to="/Contact">聯絡我們</Link></li>
              <li><Link to="/Login">登入</Link></li>
            </ul>
          </nav>
        </div>
      </header>
      
      <Outlet />
      <Footer />
    </div>
  );
}

export default Layout;
