import { Route, Routes } from "react-router-dom";
import ShowMethodDiff from "./components/ShowMethodDiff";
import Login from "./components/Login";
import Signup from "./components/Signup";
import MainPage from "./components/MainPage";
import Layout from "./components/Layout";
import './styles/style.css'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
          <Route index element={<MainPage />} />
          <Route path="/Login" element={<Login />} />
          <Route path="/Signup" element={<Signup />} />
          <Route path="/ShowMethodDiff/*" element={<ShowMethodDiff />} />
      </Route>
    </Routes>
  );
}
export default App;
