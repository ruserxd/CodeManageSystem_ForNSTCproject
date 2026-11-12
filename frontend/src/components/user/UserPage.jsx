import PropTypes from 'prop-types';
import CloneGit from './CloneGit';
import ListCurProject from './ListCurProject';
import { Navigate, useNavigate } from 'react-router-dom';
import { useCookies } from 'react-cookie';
import { Alert, Button, Typography } from 'antd';
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

	const isDemo = cookies.user.userAuthority === 'DEMO';

	const handleSubmit = () => {
		onLogout();
		navigate('/login');
	};

	const counterCloneSuccess = () => {
		setTriggerImport((prev) => prev + 1);
	};

	return (
		<div>
			{isDemo && (
				<Alert
					type="info"
					showIcon
					message="訪客模式（唯讀）"
					description="可瀏覽範例專案與方法差異，無法 clone、pull 或刪除專案。"
					style={{ marginBottom: 16 }}
				/>
			)}
			<Title>
				歡迎，
				<UserOutlined />
				{cookies.user.userName}
			</Title>
			<br />
			<ListCurProject trigger={trigger} isDemo={isDemo} />
			<br />
			{!isDemo && <CloneGit setTrigger={counterCloneSuccess} />}
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
