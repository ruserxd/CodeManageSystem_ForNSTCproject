import React from 'react';
import { Route, Routes } from 'react-router-dom';
import ShowMethodDiff from './components/diff/ShowMethodDiff';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import MainPage from './components/Basic/MainPage';
import MainLayout from './components/layout/MainLayout';
import Contact from './components/Basic/Contact';
import About from './components/Basic/About';
import UserPage from './components/user/UserPage';

import 'antd/dist/reset.css';
import { ConfigProvider } from 'antd';
import { useCookies } from 'react-cookie';

function App() {
	// 利用 cookies 將使用者資料存下來
	// 具體的 path 都設置為 '/' 各個網域皆可使用
	const [cookies, setCookies, removeCookie] = useCookies(['user']);

	// 處理登入，設置 user 到 cookie 上去
	const handleLogin = (user) => {
		console.log('success or not ' + JSON.stringify(user.myUser, null, 2));
		setCookies('user', user, { path: '/' });
	};

	// 將 user 移除 cookie
	const handleLogout = () => {
		removeCookie('user', { path: '/' });
	};

	return (
		<div className="App">
			{/* 設置系統顏色，提供全部 antD 的設定*/}
			<ConfigProvider
				theme={{
					token: {
						// Primary
						colorPrimary: '#5680E9',
						borderRadius: 2
					}
				}}>
				<Routes>
					<Route path="/" element={<MainLayout user={cookies.user} />}>
						<Route index element={<MainPage />} />
						<Route path="/Login" element={<Login onLogin={handleLogin} />} />
						<Route path="/Register" element={<Register />} />
						<Route path="/About" element={<About />} />
						<Route path="/Contact" element={<Contact />} />
						<Route path="/ShowMethodDiff/*" element={<ShowMethodDiff />} />
						<Route path="/UserPage" element={<UserPage onLogout={handleLogout} />} />
					</Route>
				</Routes>
			</ConfigProvider>
		</div>
	);
}
export default App;
