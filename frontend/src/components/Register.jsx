import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import "../styles/login.css";
import api from "../api/axiosConfig";
import validator from 'validator'

function Register() {
  const [userName, setUserName] = useState("");
  const [userEmail, setUserEmail] = useState("");
  const [userAccount, setUserAccount] = useState("");
  const [userPassword, setUserPassword] = useState("");
  const [successRegister, setSuccessRegister] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const registerUser = {
      userName,
      userEmail,
      userAccount,
      userPassword,
    };

    const result = await api.post(
      "/api/register",
      registerUser
    );
    console.log(result.data);
    setSuccessRegister(true);
    // if (result.data.success === true) {
    //   setSuccessRegister(true);
    // }
    console.log(registerUser);
  };

  // 負責確定 email 格式
  const validateEmail = (e) => {
    var email = e.target.value
  
    if (validator.isEmail(email)) {
      setUserEmail(email);
    }
  }

  // 傳送完後，將資料清空
  useEffect(() => {
    if (successRegister) {
      setUserName("");
      setUserEmail("");
      setUserAccount("");
      setUserPassword("");
      
      setSuccessRegister(false);

      alert("註冊成功");
    }
  }
  ,[successRegister]);


  return (
    <div className="signup_page">
      <div id="container2">
        <div className="signup">
          <h3>註冊帳號</h3>
          <form onSubmit={handleSubmit}>
            <input
              placeholder="使用者名稱"
              id="sign_name"
              type="text"
              value={userName}
              onChange={(e) => {
                setUserName(e.target.value);
              }}
              required
            ></input>
            <div className="tab"></div>
            <input
              placeholder="電子信箱"
              id="sign_email"
              type="email"
              value={userEmail}
              onChange={validateEmail}
              required
            ></input>
            <div className="tab"></div>
            <input
              placeholder="帳號"
              id="sign_account"
              type="text"
              value={userAccount}
              onChange={(e) => {
                setUserAccount(e.target.value);
              }}
              required
            ></input>
            <div className="tab"></div>
            <input
              placeholder="密碼"
              id="sign_password"
              type="password"
              value={userPassword}
              onChange={(e) => {
                setUserPassword(e.target.value);
              }}
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
  );
}
export default Register;
