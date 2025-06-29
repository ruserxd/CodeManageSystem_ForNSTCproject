import { useState } from 'react';
import { App, Button, Form, Input } from 'antd';
import api from '@/api/axiosConfig';
import { useNavigate } from 'react-router-dom';

const formItemLayout = {
	labelCol: {
		xs: {
			span: 24
		},
		sm: {
			span: 8
		}
	},
	wrapperCol: {
		xs: {
			span: 24
		},
		sm: {
			span: 16
		}
	}
};
const tailFormItemLayout = {
	wrapperCol: {
		xs: {
			span: 24,
			offset: 0
		},
		sm: {
			span: 16,
			offset: 8
		}
	}
};

const Register = () => {
	const { message } = App.useApp();
	const [form] = Form.useForm();
	const [loading, setloading] = useState(false);
	const navigate = useNavigate();

	const handleSubmit = async (values) => {
		setloading(true);
		try {
			const registerUser = {
				userName: values.username,
				userEmail: values.email,
				userAccount: values.account,
				userPassword: values.password
			};

			const result = await api.post('/api/register', registerUser);
			console.log(result.data);
			if (result.data.success) {
				message.success('註冊成功');
				form.resetFields();

				// 註冊成功後，直接導向 userPage
				navigate('/UserPage');
			} else {
				message.error(`註冊失敗: ${result.data.message || '未知錯誤'}`);
			}
		} catch (error) {
			message.error('註冊失敗，請檢察網路狀態 ', error);
		} finally {
			setloading(false);
		}
	};

	return (
		<Form
			{...formItemLayout}
			form={form}
			name="register"
			onFinish={handleSubmit}
			initialValues={{
				residence: ['zhejiang', 'hangzhou', 'xihu'],
				prefix: '86'
			}}
			style={{
				maxWidth: 600
			}}
			scrollToFirstError>
			<Form.Item
				name="email"
				label="E-mail"
				rules={[
					{
						type: 'email',
						message: 'The input is not valid E-mail!'
					},
					{
						required: true,
						message: 'Please input your E-mail!'
					}
				]}>
				<Input />
			</Form.Item>
			<Form.Item
				name="account"
				label="Account"
				rules={[
					{
						required: true,
						message: 'Please input your Account!'
					}
				]}>
				<Input />
			</Form.Item>

			<Form.Item
				name="password"
				label="Password"
				rules={[
					{
						required: true,
						message: 'Please input your password!'
					}
				]}
				hasFeedback>
				<Input.Password />
			</Form.Item>

			<Form.Item
				name="confirm"
				label="Confirm Password"
				dependencies={['password']}
				hasFeedback
				rules={[
					{
						required: true,
						message: 'Please confirm your password!'
					},
					({ getFieldValue }) => ({
						validator(_, value) {
							if (!value || getFieldValue('password') === value) {
								return Promise.resolve();
							}
							return Promise.reject(new Error('The new password that you entered do not match!'));
						}
					})
				]}>
				<Input.Password />
			</Form.Item>

			<Form.Item
				name="username"
				label="Username"
				tooltip="What do you want others to call you?"
				rules={[
					{
						required: true,
						message: 'Please input your nickname!',
						whitespace: true
					}
				]}>
				<Input />
			</Form.Item>

			<Form.Item {...tailFormItemLayout}>
				<Button type="primary" htmlType="submit" loading={loading}>
					Register
				</Button>
			</Form.Item>
		</Form>
	);
};
export default Register;
