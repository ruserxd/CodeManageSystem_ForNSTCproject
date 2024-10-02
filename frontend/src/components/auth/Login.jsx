import { useState } from "react";

import { Link, useNavigate } from "react-router-dom";
import api from "../../api/axiosConfig";
import "../../styles/login.css";

function Login({ onLogin }) {
  const [userAccount, setUserAccount] = useState("");
  const [userPassword, setUserPassword] = useState("");
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const LoginUserINFO = {
        userAccount,
        userPassword,
      };
      console.log(LoginUserINFO);

      const result = await api.post("api/login", LoginUserINFO);
      console.log(result.data);

      // 輸入格重置
      setUserAccount("");
      setUserPassword("");

      if (result.data.success) {
        onLogin(result.data);
        alert("登入成功");

        navigate("/UserPage");
      } else {
        alert("登入失敗，帳號密碼請確定" + result.data.message);
      }
    } catch (e) {
      console.error(e);
      alert("登入發生錯誤, 請確定網路狀態");
    }
  };
  return (
    <div className="login_signup">
      <div className="login_page">
        <div id="container1">
          <div className="login">
            <h3>登入系統</h3>
            <form onSubmit={handleSubmit}>
              <input
                type="text"
                id="user_Account"
                placeholder="帳號"
                value={userAccount}
                onChange={(e) => {
                  setUserAccount(e.target.value);
                }}
                required
              ></input>
              <div className="tab"></div>
              <input
                type="password"
                id="user_password"
                placeholder="密碼"
                value={userPassword}
                onChange={(e) => {
                  setUserPassword(e.target.value);
                }}
                required
              ></input>
              <div className="tab"></div>
              <button type="submit" value="登入" className="submit">
                登入
              </button>
            </form>
            <p>
              沒有帳號嗎?<Link to="/Register">註冊帳號</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
