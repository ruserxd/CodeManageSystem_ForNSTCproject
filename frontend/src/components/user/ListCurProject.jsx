import { App, Button } from 'antd';
import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import api from '../../api/axiosConfig';
import { Link } from 'react-router-dom';
import { Typography, List, Spin } from 'antd';
import { useCookies } from 'react-cookie';

function ListCurProject(trigger) {
	const [cookies] = useCookies(['user']);

	const { Title } = Typography;
	const contentStyle = {
		padding: 50,
		background: 'rgba(0, 0, 0, 0.05)',
		borderRadius: 4
	};
	const content = <div style={contentStyle} />;

	const [fetchData, setFetchData] = useState([]);
	const [loading, setLoading] = useState(false);
	const [errorJudge, setErrorJudge] = useState(false);
	const [successDelete, setSuccessDelete] = useState(false);
	const { message } = App.useApp();

	// 獲取目前資料庫有的 ProjectNames
	useEffect(() => {
		const getProjectNames = async () => {
			try {
				setLoading(true);
				console.log("getProjectName by" + JSON.stringify(cookies.user));
				let Id = cookies.user.userId;
				console.log('嘗試獲取 %s 的所有 ProjectName', Id);
				const result = await api.get('/api/getProjectNames', {
					params: {
						userId: Id
					}
				});
				console.log('getProjectNames 獲得 ' + JSON.stringify(result.data));

				if (result.data && result.data.length > 0) {
					setFetchData(result.data);
					setErrorJudge(false);
				} else {
					setFetchData([]);
				}
			} catch (err) {
				message.error('Error during fetch the ProjectNames');
				setErrorJudge(true);

				// 2秒後重新嘗試
				let timer = setTimeout(() => {
					getProjectNames();
				}, 5000);

				// 清理 timeout，防止內存洩漏
				return () => clearTimeout(timer);
			} finally {
				setLoading(false);
			}
		};

		getProjectNames();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [trigger, successDelete]);

	const deleteData = async (projectName) => {
		try {
			setSuccessDelete(false);
			setLoading(true);
			await api.get('/api/deleteData', { params: { projectName: projectName , userId: cookies.user.userId} });
			message.success(`成功刪除 ${projectName}`);
		} catch (err) {
			message.error('Error during delete the ProjectNames: ', err);
			setErrorJudge(true);
		} finally {
			//設置 trigger 當刪除成功呼叫 getProjectName
			setLoading(false);
			setSuccessDelete(true);
		}
	};

	const pullData = async (projectName) => {
		try {
			setSuccessDelete(false);
			setLoading(true);
			const result = await api.get('/api/pullProject', { params: { projectName: projectName } });
			console.log(result);
			message.success(result.data.message);
		} catch (err) {
			message.error('Error during pull the ProjectNames: ', err);
			setErrorJudge(true);
		} finally {
			//設置 trigger 當刪除成功呼叫 getProjectName
			setLoading(false);
			setSuccessDelete(true);
		}
	};

	return (
		<div>
			<Title level={2}>Project Names</Title>
			{errorJudge ? (
				<Spin tip="Loading">{content}</Spin>
			) : (
				<List
					size="small"
					bordered
					dataSource={fetchData}
					renderItem={(projectName) => (
						<List.Item
							actions={[
								<Button key="pull" onClick={() => pullData(projectName)} loading={loading}>
									Pull
								</Button>,
								<Button
									key="delete"
									onClick={() => deleteData(projectName)}
									loading={loading}
									danger>
									Delete
								</Button>
							]}>
							<Link to={`/ShowMethodDiff/${projectName}`}>{projectName}</Link>
						</List.Item>
					)}
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
