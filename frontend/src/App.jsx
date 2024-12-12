import React, { useEffect } from 'react';
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
import { App as AntApp, ConfigProvider, message } from 'antd';
import { useCookies } from 'react-cookie';

function App() {
	// 利用 cookies 將使用者資料存下來
	// 具體的 path 都設置為 '/' 各個網域皆可使用
	// cookies.user.
	const [cookies, setCookie, removeCookie] = useCookies(['user']);

	// 處理登入，設置 user 到 cookie 上去
	const handleLogin = (user) => {
		console.log('success or not ' + JSON.stringify(user, null, 2));
		setCookie('user', user, {
			path: '/'
		});
	};

	// 將 user 移除 cookie
	const handleLogout = () => {
		console.log('登出 ' + cookies.user.userName);
		removeCookie('user', { path: '/' });
	};

	// Global 配置 message
	useEffect(() => {
		message.config({
			duration: 1,
			maxCount: 2
		});
	}, []);

	return (
		<div className="App">
			{/* 可以直接使用靜態的 message, notification, modal -> useApp */}
			<AntApp>
				{/* 設置系統顏色，提供全部 antD 的設定*/}
				<ConfigProvider
					theme={{
						token: {
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
			</AntApp>
		</div>
	);
}

export default App;
