import { Link } from "react-router-dom";
import "../styles/login.css";

const preventRefresh = (e) => {
  e.preventDefault();
};

function Login() {
  return (
    <div className="login_signup">
      <div className="login_page">
        <div id="container1">
          <div className="login">
            <h3>登入系統</h3>
            <form>
              <input
                type="text"
                id="user_account"
                placeholder="帳號"
                required
              ></input>
              <div className="tab"></div>
              <input
                type="text"
                id="user_password"
                placeholder="密碼"
                required
              ></input>
              <div className="tab"></div>
              <button type="submit" value="登入" className="submit" onClick={preventRefresh}>
                登入
              </button>
            </form>
            <p>
              沒有帳號嗎?<Link to="/Signup">註冊帳號</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
