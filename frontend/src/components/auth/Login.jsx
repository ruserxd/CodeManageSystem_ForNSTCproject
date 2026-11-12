import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { App, Button, Checkbox, Flex, Form, Input } from 'antd';
import PropTypes from 'prop-types';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import api from '@/api/axiosConfig';

const Login = ({ onLogin }) => {
	const [form] = Form.useForm();
	const navigate = useNavigate();
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);

	const [demoLoading, setDemoLoading] = useState(false);

	// 一般登入與訪客登入共用的結果處理
	const handleLoginResult = (data, failPrefix) => {
		if (data.success) {
			onLogin(data.myUser, data.token);
			message.success(data.myUser.userName + '登入成功');
			form.resetFields();
			navigate('/UserPage');
		} else {
			message.error(`${failPrefix}：${data.message}`);
		}
	};

	const handleSubmit = async (values) => {
		setLoading(true);
		try {
			const loginUserInfo = {
				userAccount: values.userAccount,
				userPassword: values.password
			};

			const result = await api.post('api/login', loginUserInfo);
			console.log('登入 ' + JSON.stringify(result.data.myUser));
			handleLoginResult(result.data, '登入失敗');
		} catch (error) {
			console.error('登入錯誤:', error);
			message.error('登入時發生錯誤，請檢查網路連接或稍後再試');
		} finally {
			setLoading(false);
		}
	};

	const handleDemoLogin = async () => {
		setDemoLoading(true);
		try {
			const result = await api.post('api/demo-login');
			handleLoginResult(result.data, '訪客登入失敗');
		} catch (error) {
			console.error('訪客登入錯誤:', error);
			message.error('訪客登入時發生錯誤，請檢查網路連接或稍後再試');
		} finally {
			setDemoLoading(false);
		}
	};

	return (
		<Form
			name="login"
			initialValues={{
				remember: true
			}}
			style={{
				maxWidth: 360
			}}
			onFinish={handleSubmit}
			form={form}>
			<Form.Item
				name="userAccount"
				rules={[
					{
						required: true,
						message: '請輸入使用者的名稱!'
					}
				]}>
				<Input prefix={<UserOutlined />} placeholder="UserAccount" />
			</Form.Item>

			<Form.Item
				name="password"
				rules={[
					{
						required: true,
						message: '請輸入您的密碼'
					}
				]}>
				<Input prefix={<LockOutlined />} type="password" placeholder="Password" />
			</Form.Item>
			<Form.Item>
				<Flex justify="space-between" align="center">
					<Form.Item name="remember" valuePropName="checked" noStyle>
						<Checkbox>Remember me</Checkbox>
					</Form.Item>
					<Link to="/Register">忘記帳號</Link>
				</Flex>
			</Form.Item>

			<Form.Item>
				<Button block type="primary" htmlType="submit" loading={loading} disabled={demoLoading}>
					Log in
				</Button>
				<Button
					block
					style={{ marginTop: 8 }}
					onClick={handleDemoLogin}
					loading={demoLoading}
					disabled={loading}>
					訪客體驗
				</Button>
				沒有帳號嗎?<Link to="/Register">註冊帳號</Link>
			</Form.Item>
		</Form>
	);
};

Login.propTypes = {
	onLogin: PropTypes.func.isRequired
};

export default Login;
