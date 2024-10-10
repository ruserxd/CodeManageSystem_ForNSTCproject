import PropTypes from 'prop-types';
import CloneGit from './CloneGit';
import ListCurProject from './ListCurProject';
import { Navigate, useNavigate } from 'react-router-dom';
import { useCookies } from 'react-cookie';
import { Button, Typography } from 'antd';
import { UserOutlined } from '@ant-design/icons';
const { Title } = Typography;

function UserPage({ onLogout }) {
	const navigate = useNavigate();
	const [cookies] = useCookies(['user']);

	if (!cookies.user) {
		return <Navigate to="/login" replace />;
	}

	const handleSubmit = () => {
		console.log('登出' + cookies.user);
		onLogout();
		navigate('/login');
	};

	return (
		<div>
			<Title>
				歡迎，
				<UserOutlined />
				{cookies.user.myUser.userName}
			</Title>
			<br />
			<ListCurProject />
			<br />
			<CloneGit />
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
