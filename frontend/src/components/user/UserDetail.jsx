import { Button, Descriptions } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';

function UserDetail() {
	const navigate = useNavigate();
	const location = useLocation();
	const userDetails = location.state?.userDetails;
	const generateItem = (userDetails) => {
		return [
			{
				label: 'UserId',
				span: 'filled',
				children: `${userDetails.userId}`
			},
			{
				label: 'UserName',
				span: 'filled',
				children: `${userDetails.userName}`
			},
			{
				label: 'UserAccount',
				span: 'filled',
				children: `${userDetails.userAccount}`
			},
			{
				label: 'UserEmail',
				span: 'filled',
				children: `${userDetails.userEmail}`
			},
			{
				label: 'UserPassword',
				children: `${userDetails.userPassword}`
			}
		];
	};
	const backToPage = () => {
		navigate(-1);
	};
	return (
		<div>
			<Descriptions bordered title="User Info" items={generateItem(userDetails)} />
			<br />
			<Button type={'primary'} onClick={backToPage}>Back</Button>
		</div>);
}

export default UserDetail;