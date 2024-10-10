import { useEffect, useState } from 'react';
import api from '../../api/axiosConfig';
import { Link } from 'react-router-dom';

function ListCurProject() {
	const [data, setData] = useState([]);

	useEffect(() => {
		const getProjectNames = async () => {
			try {
				const result = await api.get('/api/getProjectNames');
				console.log('response.data\n', result.data);

				if (result.data && result.data.length > 0) {
					setData(result.data);
				} else {
					setData([]);
				}
			} catch (error) {
				console.error('Error during fetch: ', error);
			}
		};
		getProjectNames();
	}, []);

	return (
		<div>
			<h2>Cur Project Names</h2>

			<ul>
				{data.length > 0 ? (
					data.map((projectName, id) => (
						<li key={id}>
							<Link to={`/ShowMethodDiff/${projectName}`}>{projectName}</Link>
						</li>
					))
				) : (
					<p>No projects in SQL</p>
				)}
			</ul>
		</div>
	);
}

export default ListCurProject;
