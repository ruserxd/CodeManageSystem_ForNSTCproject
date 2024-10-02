import { useEffect, useState, useCallback } from "react";
import { useInput, validate } from "canathus";
import { nameValidator } from "../validators/nameValidators";
import { emailValidator } from "../validators/emailValidators";
import { accountValidator } from "../validators/accountValidators";
import { passwordValidator } from "../validators/passwordValidators";
import { Link } from "react-router-dom";
import "../../styles/login.css";
import api from "../../api/axiosConfig";

function Register() {
  const [userName, setUserName] = useInput("", nameValidator);
  const [userEmail, setUserEmail] = useInput("", emailValidator);
  const [userAccount, setUserAccount] = useInput("", accountValidator);
  const [userPassword, setUserPassword] = useInput("", passwordValidator);

  const [successRegister, setSuccessRegister] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const isValid = validate({
      userName,
      userEmail,
      userAccount,
      userPassword,
    });

    if (!isValid) {
      return;
    }

    const registerUser = {
      userName: userName.value,
      userEmail: userEmail.value,
      userAccount: userAccount.value,
      userPassword: userPassword.value,
    };

    const result = await api.post("/api/register", registerUser);
    console.log(result.data);
    setSuccessRegister(true);
  };

  // 傳送完後，將資料清空
  const resetForm = useCallback(() => {
    setUserName({ value: "", error: null });
    setUserEmail({ value: "", error: null });
    setUserAccount({ value: "", error: null });
    setUserPassword({ value: "", error: null });
  }, [setUserName, setUserEmail, setUserAccount, setUserPassword]);

  useEffect(() => {
    if (successRegister) {
      resetForm();
      setSuccessRegister(false);
      alert("註冊成功");
    }
  }, [successRegister, resetForm]);

  return (
    <div className="signup_page">
      <div id="container2">
        <div className="signup">
          <h3>註冊帳號</h3>
          <form onSubmit={handleSubmit} noValidate>
            <input
              placeholder="使用者名稱"
              id="sign_name"
              type="text"
              value={userName.value}
              onChange={(e) => setUserName(e.target.value)}
              required
            ></input>
            <span>{userName.error && userName.errorMsg}</span>
            <div className="tab"></div>

            <input
              placeholder="電子信箱"
              id="sign_email"
              type="email"
              value={userEmail.value}
              onChange={(e) => setUserEmail(e.target.value)}
              required
            ></input>
            <span>{userEmail.error && userEmail.errorMsg}</span>
            <div className="tab"></div>

            <input
              placeholder="帳號"
              id="sign_account"
              type="text"
              value={userAccount.value}
              onChange={(e) => setUserAccount(e.target.value)}
              required
            ></input>
            <span>{userAccount.error && userAccount.errorMsg}</span>
            <div className="tab"></div>

            <input
              placeholder="密碼"
              id="sign_password"
              type="password"
              value={userPassword.value}
              onChange={(e) => setUserPassword(e.target.value)}
              required
            ></input>
            <span>{userPassword.error && userPassword.errorMsg}</span>
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
