import { Link } from 'react-router-dom';
import "../styles/login.css";

function Signup() {
    return (
        <div className="signup_page">
          <div id="container2">
            <div className="signup">
              <h3>註冊帳號</h3>
              <form>
                <input
                  placeholder="使用者名稱"
                  id="sign_name"
                  type="text"
                  required
                ></input>
                <div className="tab"></div>
                <input
                  placeholder="帳號"
                  id="sign_account"
                  type="text"
                  required
                ></input>
                <div className="tab"></div>
                <input
                  placeholder="密碼"
                  id="sign_password"
                  type="text"
                  required
                ></input>
                <div className="tab"></div>
                <button type="submit" className="submit">
                    註冊
                </button>
              </form>
              <p>
                已有帳號嗎?<Link to="/Login">登入帳號</Link>
              </p>
            </div>
          </div>
        </div>
    )
}
export default Signup;