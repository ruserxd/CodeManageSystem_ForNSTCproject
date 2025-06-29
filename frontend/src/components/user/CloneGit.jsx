import { App, Button, Form, Input, Space } from 'antd';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import api from '@/api/axiosConfig';
import { useCookies } from 'react-cookie';

function CloneGit({ setTrigger }) {
	const [form] = Form.useForm();
	const [loading, setLoading] = useState(false);
	const [cookies] = useCookies(['user']);
	const { notification, message } = App.useApp();

	const showNotification = (status, message) => {
		notification.info({
			message: `${status}`,
			description: `${cookies.user.userName}  ${message}`,
			placement: 'bottomLeft',
			showProgress: true
		});
	};

	const handleFetchData = async (url, commitId) => {
		setLoading(true);
		try {
			const userId = cookies.user.userId;
			console.log('Submitting UserID: ', userId);
			console.log(`透過 ${url} and ${commitId} 獲得`);
			const response = await api.post(
				'/api/fetch-repo',
				new URLSearchParams({ url, commitId, userId })
			);
			const { status, message } = response.data;
			console.log(response.data);
			console.log(status);
			console.log(message);
			if (status === 'CLONE_SUCCESS' || status === 'PULL_SUCCESS') {
				showNotification(status, message);
				form.resetFields();

				if (setTrigger) {
					setTrigger();
				}
			} else if (status === 'ANALYSIS_FAILED') {
				showNotification(status, message);
			} else if (status === 'PULL_FAILED' || status === 'CLONE_FAILED') {
				showNotification(status, message);
			} else {
				showNotification(status, message);
			}
		} catch (error) {
			showNotification('FetchError', error);
			console.error('Error during fetch:', error);
		} finally {
			setLoading(false);
		}
	};

	const handleClick = (values) => {
		console.log(values.url, values.commitId);
		if (values.url.trim()) {
			console.log('Submitting URL:', values.url);
			if (values.commitId === undefined) {
				console.log('Submitting CommitID: Head');
				handleFetchData(values.url, 'HEAD');
			} else {
				console.log('Submitting CommitID: ', values.commitId);
				handleFetchData(values.url, values.commitId);
			}
		} else {
			message.error('URL is empty');
		}
	};

	return (
		<div>
			<Form
				form={form}
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
				// 不要有冒號
				colon={false}
				style={{
					maxWidth: 600
				}}>
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
						}
					]}>
					<Input placeholder="GitHub repository url" />
				</Form.Item>
				<Form.Item
					name="commitId"
					label="COMMITID"
					rules={[
						{
							required: false
						},
						{
							type: 'string',
							// git commitId 最少也要 8 個
							min: 8
						}
					]}>
					<Input placeholder="GitHub commitId" />
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

CloneGit.propTypes = {
	setTrigger: PropTypes.func.isRequired
};

export default CloneGit;
