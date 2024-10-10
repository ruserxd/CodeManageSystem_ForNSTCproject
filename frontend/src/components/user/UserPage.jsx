import PropTypes from 'prop-types';
import CloneGit from './CloneGit';
import ListCurProject from './ListCurProject';
import { Navigate, useNavigate } from 'react-router-dom';
import { useCookies } from 'react-cookie';

function UserPage({ onLogout }) {
	const navigate = useNavigate();
	const [cookies] = useCookies(['user']);

	if (!cookies.user) {
		return <Navigate to="/login" replace />;
	}

	const handleSubmit = () => {
		console.log('登出' + cookies.user);
		onLogout();
		navigate('/');
	};

	return (
		<div>
			<h1>歡迎，{cookies.user.myUser.userName}</h1>
			<ListCurProject />
			<CloneGit />
			<button className="btn" onClick={handleSubmit}>
				登出
			</button>
		</div>
	);
}

UserPage.propTypes = {
	onLogout: PropTypes.func.isRequired
};

export default UserPage;
