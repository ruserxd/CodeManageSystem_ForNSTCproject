import { useEffect, useState } from 'react';
import api from '../../api/axiosConfig';
import { Link } from 'react-router-dom';
import { Typography, List } from 'antd';
const { Title } = Typography;

function ListCurProject() {
	const [fetchData, setFetchData] = useState([]);
	const [loading, setloading] = useState(false);

	useEffect(() => {
		const getProjectNames = async () => {
			setloading(true);
			try {
				const result = await api.get('/api/getProjectNames');
				console.log('response.data\n', result.data);

				if (result.data && result.data.length > 0) {
					setFetchData(result.data);
				} else {
					setFetchData([]);
				}
			} catch (error) {
				console.error('Error during fetch: ', error);
			} finally {
				setloading(false);
			}
		};
		getProjectNames();
	}, []);

	return (
		<div>
			<Title level={2}>Cur Project Names</Title>

			<List
				size="small"
				bordered
				dataSource={fetchData}
				renderItem={(projectName) => (
					<List.Item>
						<Link to={`/ShowMethodDiff/${projectName}`}>{projectName}</Link>
					</List.Item>
				)}
				loading={loading}
			/>
		</div>
	);
}

export default ListCurProject;
