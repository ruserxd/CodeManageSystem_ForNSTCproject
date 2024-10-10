import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Checkbox, Form, Input, Flex, message } from 'antd';
import PropTypes from 'prop-types';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import api from '../../api/axiosConfig';

const Login = ({ onLogin }) => {
	const [form] = Form.useForm();
	const navigate = useNavigate();
	const [loading, setLoading] = useState(false);

	const handleSubmit = async (values) => {
		setLoading(true);
		try {
			const loginUserInfo = {
				userAccount: values.useraccount,
				userPassword: values.password
			};

			const result = await api.post('api/login', loginUserInfo);

			if (result.data.success) {
				onLogin(result.data);
				message.success('登入成功');
				form.resetFields();
				navigate('/UserPage');
			} else {
				message.error(`登入失敗：${result.data.message}`);
			}
		} catch (error) {
			console.error('登入錯誤:', error);
			message.error('登入時發生錯誤，請檢查網路連接或稍後再試');
		} finally {
			setLoading(false);
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
			onFinish={handleSubmit}>
			<Form.Item
				name="useraccount"
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
				<Button block type="primary" htmlType="submit" loading={loading}>
					Log in
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
