import React, { useEffect, useState } from 'react';
import { Button, message, Space, Table, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axiosConfig';

function AdminOutlined() {
	const navigate = useNavigate();
	const { Title } = Typography;
	const [users, setUsers] = useState([]);

	useEffect(() => {
		const fetchUsers = async () => {
			try {
				const response = await api.get('/api/getAllUsers');

				if (response.data && Array.isArray(response.data)) {
					const data = response.data.map(user => ({
						userId: user.userId,
						userName: user.userName,
						userEmail: user.userEmail,
						userAccount: user.userAccount,
						userAuthority: user.userAuthority,
						userPassword: user.userPassword
					}));
					setUsers(data);
				} else {
					message.error('獲取使用者資訊錯誤');
				}
			} catch (error) {
				message.error(`發生 ${error}`);
			}
		};

		fetchUsers();
	}, []);

	const handleDetailButton = (user) => {
		// 透過 state 的方式將資料從父頁面傳入子頁面
		navigate(`/AdminPage/${user.userId}`, { state: { userDetails: user } });
	};

	const handleDeleteButton = async (userId) => {
		console.log(`嘗試刪除 ${userId}`);
		const response = await api.post(
			'/api/deleteUser',
			new URLSearchParams({ userId })
		);

		if (response.data === true) {
			message.success('成功刪除', userId);
		} else {
			message.success(`刪除 ${userId}發生錯誤}`);
		}
	};

	const columns = [
		{
			title: 'ID',
			dataIndex: 'userId',
			key: 'userId'
		},
		{
			title: 'Account',
			dataIndex: 'userAccount',
			key: 'userAccount'
		},
		{
			title: 'authority',
			dataIndex: 'userAuthority',
			key: 'userAuthority'
		},
		{
			title: '',
			render: (_, record) => (
				<Space>
					<Button
						type="primary"
						onClick={() => handleDetailButton(record)}
					>
						details
					</Button>
					<Button
						danger
						type="primary"
						onClick={() => handleDeleteButton(record.userId)}
					>
						delete
					</Button>
				</Space>
			)

		}
	];

	return (
		<div>
			<Title level={2}>使用者清單</Title>
			<Table columns={columns} dataSource={users} rowKey="userId" />
		</div>
	);
}

export default AdminOutlined;
