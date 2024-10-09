import { Route, Routes } from "react-router-dom";
import ShowMethodDiff from "./components/diff/ShowMethodDiff";
import Login from "./components/auth/Login";
import Register from "./components/auth/Register";
import MainPage from "./components/Basic/MainPage";
import Layout from "./components/layout/Layout";
import Contact from "./components/Basic/Contact";
import About from "./components/Basic/About";
import UserPage from "./components/user/UserPage";
import "./styles/style.css";
import { useCookies } from "react-cookie";

import TestLayout from "./components/temporary_antd/TestLayout";

function App() {
  // 利用 cookies 將使用者資料存下來
  const [cookies, setCookies, removeCookie] = useCookies(["user"]);

  const handleLogin = (user) => {
    console.log("success or not " + JSON.stringify(user.myUser, null, 2));
    setCookies("user", user, { path: "/" });
  };

  const handleLogout = () => {
    removeCookie("user", { path: "/" });
  };

  return (
    // theme={{ token: { colorPrimary: "#5AB9EA" } }}
    <Routes>
      <Route path="/" element={<Layout user={cookies.user} />}>
        <Route index element={<MainPage />} />
        <Route path="/Login" element={<Login onLogin={handleLogin} />} />
        <Route path="/Register" element={<Register />} />
        <Route path="/About" element={<About />} />
        <Route path="/Contact" element={<Contact />} />
        <Route path="/ShowMethodDiff/*" element={<ShowMethodDiff />} />
        <Route
          path="/UserPage"
          element={<UserPage onLogout={handleLogout} />}
        />
      </Route>
      <Route path="/test" element={<TestLayout />}>
        <Route index element={<MainPage />} />
        <Route path="Login" element={<Login onLogin={handleLogin} />} />
        <Route path="Register" element={<Register />} />
        <Route path="About" element={<About />} />
        <Route path="Contact" element={<Contact />} />
        <Route path="ShowMethodDiff/*" element={<ShowMethodDiff />} />
        {/* <Route
          path="/UserPage"
          element={<UserPage onLogout={handleLogout} />}
        /> */}
      </Route>
    </Routes>
  );
}
export default App;
