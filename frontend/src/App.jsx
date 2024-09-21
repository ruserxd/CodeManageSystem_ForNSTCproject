import { Route, Routes } from "react-router-dom";
import ShowMethodDiff from "./components/ShowMethodDiff";
import Login from "./components/Login";
import Register from "./components/Register";
import MainPage from "./components/MainPage";
import Layout from "./components/Layout";
import Contact from "./components/Contact";
import About from "./components/About";
import './styles/style.css'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
          <Route index element={<MainPage />} />
          <Route path="/Login" element={<Login />} />
          <Route path="/Register" element={<Register />} />
          <Route path="/About" element={<About />} />
          <Route path="/Contact" element={<Contact />} />
          <Route path="/ShowMethodDiff/*" element={<ShowMethodDiff />} />
      </Route>
    </Routes>
  );
}
export default App;
