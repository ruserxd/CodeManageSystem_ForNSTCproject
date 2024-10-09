import PropTypes from "prop-types";
import { Outlet, Link } from "react-router-dom";
import Footer from "./Footer";

function Layout({ user }) {
  return (
    <div className="layout">
      <header className="header">
        <div className="topBar">
          <h2 className="mainTitle">程式單元開發管理系統</h2>
          <nav className="navigation">
            <ul>
              <li>
                <Link to="/">首頁</Link>
              </li>
              <li>
                <Link to="/About">關於我們</Link>
              </li>
              <li>
                <Link to="/Contact">聯絡我們</Link>
              </li>
              <li>
                <Link to="/test">testing web</Link>
              </li>
              {user ? (
                <li>
                  <Link to="/UserPage">Welcome {user.myUser.userName}</Link>
                </li>
              ) : (
                <li>
                  <Link to="/Login">登入</Link>
                </li>
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

Layout.propTypes = {
  user: PropTypes.shape({
    success: PropTypes.bool.isRequired,
    message: PropTypes.string.isRequired,
    myUser: PropTypes.shape({
      user_id: PropTypes.number.isRequired,
      userName: PropTypes.string.isRequired,
      userEmail: PropTypes.string.isRequired,
      userAccount: PropTypes.string.isRequired,
      userPassword: PropTypes.string.isRequired,
      userAuthority: PropTypes.string.isRequired,
    }).isRequired,
  }).isRequired,
};
export default Layout;
