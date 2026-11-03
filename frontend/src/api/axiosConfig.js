import axios from 'axios';

export const TOKEN_KEY = 'token';

// 後端位址由環境變數 VITE_API_URL 決定（瀏覽器要連得到的位址），未設定時預設本機 8080
const api = axios.create({
	baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
	headers: { 'ngrok-skip-browser-warning': 'true' }
});

// 每個請求自動帶上登入後取得的 JWT
api.interceptors.request.use((config) => {
	const token = localStorage.getItem(TOKEN_KEY);
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});

// token 失效或過期時，清除登入狀態並導回登入頁
api.interceptors.response.use(
	(response) => response,
	(error) => {
		if (error.response?.status === 401) {
			localStorage.removeItem(TOKEN_KEY);
			document.cookie = 'user=; Max-Age=0; path=/';
			window.location.href = '/Login';
		}
		return Promise.reject(error);
	}
);

export default api;
