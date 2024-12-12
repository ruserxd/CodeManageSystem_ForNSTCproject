import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { TreeSelect, Typography } from 'antd';
import api from '../../api/axiosConfig';
import PropTypes from 'prop-types';

const { Title } = Typography;

const ShowMethodDiff = () => {
	const { '*': urlParam } = useParams();
	const [treeData, setTreeData] = useState([]);
	const [projectName, setProjectName] = useState('');
	const [error, setError] = useState(null);
	const [value, setValue] = useState();

	useEffect(() => {
		const fetchData = async () => {
			try {
				console.log(`嘗試 fetch ${urlParam}`);
				const response = await api.post(
					`/api/getData`,
					new URLSearchParams({ ProjectName: urlParam })
				);

				const tmp = transformData(response.data);
				setTreeData(tmp);
				// 設定 Project 為網域的最後一個 Param
				setProjectName(urlParam.substring(urlParam.lastIndexOf('/') + 1));
			} catch (error) {
				setError(error);
				console.error('Error during fetch: ', error);
			}
		};
		void fetchData();
	}, [urlParam]);

	// 日後當透過 selectTree 選擇是直接將資料修該為該路徑
	const treeSelectOnChange = (newValue) => {
		console.log(newValue);
	};

	// 將 JSon 格式修改成 antd treeSelect 需求
	const transformData = (data) => {
		return [
			{
				value: `project_${data.projectId}`,
				title: data.projectName,
				label: data.projectName,
				children: data.files.map((file, index) => ({
					value: `project_${data.projectId}_${file.filesId}`,
					title: file.fileName,
					label: file.fileName,
					children: file.methods.map((method, methodIndex) => ({
						value: `project_${data.projectId}_${file.filesId}_${method.methodId}`,
						title: method.methodName,
						label: method.methodName,
						children: method.diffInfoList.map((diff, diffIndex) => ({
							value: `project_${data.projectId}_${file.filesId}_${method.methodId}_${diff.diffInfoId}`,
							title: diff.commitMessage,
							label: diff.commitMessage
						}))
					}))
				}))
			}
		];
	};

	return (
		<>
			{error ? (
				<p> Error: error </p>
			) : (
				<div>
					<Title level={2}>{projectName} 的方法差異資訊</Title>
					<TreeSelect
						showSearch
						style={{
							width: '25%'
						}}
						value={value}
						dropdownStyle={{
							maxHeight: 400,
							overflow: 'auto'
						}}
						placeholder="Select"
						allowClear
						onChange={treeSelectOnChange}
						treeData={treeData}
					/>
				</div>
			)}
		</>
	);
};

ShowMethodDiff.propTypes = {
	gotData: PropTypes.shape({
		projectId: PropTypes.number.isRequired,
		projectName: PropTypes.string.isRequired,
		headRevstr: PropTypes.string.isRequired,
		files: PropTypes.arrayOf(
			PropTypes.shape({
				filesId: PropTypes.number.isRequired,
				fileName: PropTypes.string.isRequired,
				filePath: PropTypes.string.isRequired,
				methods: PropTypes.arrayOf(
					PropTypes.shape({
						methodId: PropTypes.number.isRequired,
						methodName: PropTypes.string.isRequired,
						diffInfoList: PropTypes.arrayOf(
							PropTypes.shape({
								diffInfoId: PropTypes.number.isRequired,
								author: PropTypes.string.isRequired,
								authorEmail: PropTypes.string.isRequired,
								commitMessage: PropTypes.string.isRequired,
								timestamp: PropTypes.string.isRequired,
								commitTime: PropTypes.number.isRequired,
								diffCode: PropTypes.string.isRequired,
								headRevstr: PropTypes.string
							})
						)
					})
				)
			})
		)
	})
};
export default ShowMethodDiff;
