import React, { useEffect, useState } from 'react';
import { Table, Typography } from 'antd';
import axios from 'axios';

function AdminOutlined() {
  const { Title } = Typography;
  const [users, setUsers] = useState([]);

  useEffect(() => {
  axios.get('http://localhost:8080/api/getAllUsers')
    .then(response => {
      console.log('API response:', response.data);
      const data = response.data.map(user => ({
        userId: user.userId,
		userName : user.userName,
        userEmail : user.userEmail,
		userAccount: user.userAccount,
        userAuthority : user.userAuthority,
		userPassword : user.userPassword
      }));
      setUsers(data);
    })
    .catch(error => console.error('Error fetching users:', error));
}, []);
  

  const columns = [
    {
      title: 'ID',
      dataIndex: 'userId',
      key: 'userId',
    },
    {
      title: 'Account',
      dataIndex: 'userAccount',
      key: 'userAccount',
    },
  ];

  return (
    <div>
        <Title level={2}>使用者清單</Title>
        <Table columns={columns} dataSource={users} rowKey="userId" />
    </div>
  );
}

export default AdminOutlined;
