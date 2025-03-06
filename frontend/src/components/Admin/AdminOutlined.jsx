import React, { useEffect, useState } from 'react';
import { Button, Space, Table, Typography } from 'antd';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

function AdminOutlined() {
	const navigate = useNavigate();
	const { Title } = Typography;
	const [users, setUsers] = useState([]);

	useEffect(() => {
		axios.get('http://localhost:8080/api/getAllUsers')
			.then(response => {
				console.log('API response:', response.data);
				const data = response.data.map(user => ({
					userId: user.userId,
					userName: user.userName,
					userEmail: user.userEmail,
					userAccount: user.userAccount,
					userAuthority: user.userAuthority,
					userPassword: user.userPassword
				}));
				setUsers(data);
			})
			.catch(error => console.error('Error fetching users:', error));
	}, []);

	const handleDetailButton = (user) => {
		// 透過 state 的方式將資料從父頁面傳入子頁面
		navigate(`/AdminPage/${user.userId}`, { state: { userDetails: user } });
	};

	const handleDeleteButton = (user) => {
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
						onClick={() => handleDeleteButton()}
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
