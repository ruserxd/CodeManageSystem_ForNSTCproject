import { Button, Form, Input } from 'antd';
import { nameValidator } from '../validators/nameValidators';

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
const TestRegister = () => {
	const [form] = Form.useForm();
	const onFinish = (values) => {
		console.log('Received values of form: ', values);
	};
	const validateName = (_, value) => {
		const result = nameValidator(value);
		if (result.error) {
			return Promise.reject(new Error(result.errorMsg));
		}
		return Promise.resolve();
	};

	return (
		<Form
			{...formItemLayout}
			form={form}
			name="register"
			onFinish={onFinish}
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
					},
					{ validator: validateName }
				]}>
				<Input />
			</Form.Item>

			<Form.Item {...tailFormItemLayout}>
				<Button type="primary" htmlType="submit">
					Register
				</Button>
			</Form.Item>
		</Form>
	);
};
export default TestRegister;
