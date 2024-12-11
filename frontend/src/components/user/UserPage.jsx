import PropTypes from 'prop-types';
import CloneGit from './CloneGit';
import ListCurProject from './ListCurProject';
import { Navigate, useNavigate } from 'react-router-dom';
import { useCookies } from 'react-cookie';
import { Button, Typography } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { useState } from 'react';
const { Title } = Typography;

function UserPage({ onLogout }) {
	const navigate = useNavigate();
	const [cookies] = useCookies(['user']);
	const [trigger, setTriggerImport] = useState(0);

	if (!cookies.user) {
		return <Navigate to="/login" replace />;
	}

	const handleSubmit = () => {
		onLogout();
		navigate('/login');
	};

	const counterCloneSuccess = () => {
		setTriggerImport((prev) => prev + 1);
	};

	return (
		<div>
			<Title>
				歡迎，
				<UserOutlined />
				{cookies.user.userName}
			</Title>
			<br />
			<ListCurProject trigger={trigger} />
			<br />
			<CloneGit setTrigger={counterCloneSuccess} />
			<Button type="primary" onClick={handleSubmit}>
				登出
			</Button>
		</div>
	);
}

UserPage.propTypes = {
	onLogout: PropTypes.func.isRequired
};

export default UserPage;
