import { App } from 'antd';
import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import api from '../../api/axiosConfig';
import { Link } from 'react-router-dom';
import { Typography, List, Spin } from 'antd';

const { Title } = Typography;
const contentStyle = {
	padding: 50,
	background: 'rgba(0, 0, 0, 0.05)',
	borderRadius: 4
};
const content = <div style={contentStyle} />;

function ListCurProject(trigger) {
	const [fetchData, setFetchData] = useState([]);
	const [loading, setloading] = useState(false);
	const [erroorJudge, setErrorJudge] = useState(false);
	const { message } = App.useApp();

	// 獲取目前資料庫有的 ProjectNames
	useEffect(() => {
		const getProjectNames = async () => {
			try {
				setloading(true);
				const result = await api.get('/api/getProjectNames');

				if (result.data && result.data.length > 0) {
					setFetchData(result.data);
					setErrorJudge(false);
				} else {
					setFetchData([]);
				}
			} catch (err) {
				message.error('Error during fetch the ProjectNames: ', err);
				setErrorJudge(true);
			} finally {
				setloading(false);
			}
		};

		getProjectNames();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [trigger]);

	return (
		<div>
			<Title level={2}>Cur Project Names</Title>
			{erroorJudge ? (
				<Spin tip="Loading">{content}</Spin>
			) : (
				<List
					size="small"
					bordered
					dataSource={fetchData}
					renderItem={(projectName) => (
						<List.Item key = {projectName}>
							<Link to={`/ShowMethodDiff/${projectName}`}>{projectName}</Link>
							<a>edit</a>
						</List.Item>
					)}
					// 讓使用者的體驗增加
					loading={loading}
				/>
			)}
		</div>
	);
}

ListCurProject.propTypes = {
	trigger: PropTypes.number.isRequired
};

export default ListCurProject;
