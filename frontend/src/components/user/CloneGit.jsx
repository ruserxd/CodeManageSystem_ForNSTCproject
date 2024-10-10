import { useState } from 'react';
import api from '../../api/axiosConfig';
import { Button, Form, Input, message, Space } from 'antd';

function CloneGit() {
	const [form] = Form.useForm();
	const [loading, setloading] = useState(false);

	const handleFetchData = async (url) => {
		setloading(true);
		try {
			const response = await api.post('/api/fetch-repo', new URLSearchParams({ url }));

			const { status, path } = response.data;

			if (status === 'CLONE_SUCCESS' || status === 'PULL_SUCCESS') {
				alert(`Repository processed successfully: ${path}`);
				window.location.reload();
				form.resetFields();
			} else if (status === 'ANALYSIS_FAILED') {
				alert(`Repository cloned but no files were analyzed`);
			} else if (status === 'PULL_FAILED' || status === 'CLONE_FAILED') {
				alert(`Failed to process repository`);
			} else {
				alert(`Unexpected status: ${status}`);
			}
		} catch (error) {
			alert('Error during fetch. Please check the console for more information.');
			console.error('Error during fetch:', error);
		} finally {
			setloading(false);
		}
	};

	const handleClick = (values) => {
		console.log(values.url);
		if (values.url.trim()) {
			console.log('Submitting URL:', values.url);
			handleFetchData(values.url);
		} else {
			message.error('URL is empty');
		}
	};

	return (
		<div>
			<Form
				onFinish={handleClick}
				name="wrap"
				labelCol={{
					flex: '110px'
				}}
				labelAlign="left"
				labelWrap
				wrapperCol={{
					flex: 1
				}}
				colon={false}
				style={{
					maxWidth: 600
				}}>
				{' '}
				<Form.Item
					name="url"
					label="URL"
					rules={[
						{
							required: true
						},
						{
							type: 'url',
							warningOnly: true
						},
						{
							type: 'string',
							min: 6
						}
					]}>
					<Input placeholder="GitHub repository url" />
				</Form.Item>
				<Form.Item>
					<Space>
						<Button htmlType="submit" loading={loading}>
							Submit
						</Button>
					</Space>
				</Form.Item>
			</Form>
		</div>
	);
}

export default CloneGit;
