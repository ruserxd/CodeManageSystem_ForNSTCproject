import { Route, Routes } from "react-router-dom";
import ShowMethodDiff from "./components/ShowMethodDiff";
import Login from "./components/Login";
import Register from "./components/Register";
import MainPage from "./components/MainPage";
import Layout from "./components/Layout";
import Contact from "./components/Contact";
import About from "./components/About";
import UserPage from "./components/UserPage";

import './styles/style.css'
import { useCookies } from "react-cookie";

function App() {
  // 利用 cookies 將使用者資料存下來
  const [cookies, setCookies, removeCookie] = useCookies(['user']);

  const handleLogin = (user) => {
    setCookies('user', user, { path: '/' })
  }

  const handleLogout = () => {
    removeCookie('user', { path: '/' });
  }

  return (
    <Routes>
      <Route path="/" element={<Layout user={cookies.user}/>}>
          <Route index element={<MainPage />} />
          <Route path="/Login" element={<Login onLogin={handleLogin}/>} />
          <Route path="/Register" element={<Register />} />
          <Route path="/About" element={<About />} />
          <Route path="/Contact" element={<Contact />} />
          <Route path="/ShowMethodDiff/*" element={<ShowMethodDiff />} />
          <Route path="/UserPage" element={<UserPage onLogout={handleLogout}/>} />
      </Route>
    </Routes>
  );
}
export default App;
