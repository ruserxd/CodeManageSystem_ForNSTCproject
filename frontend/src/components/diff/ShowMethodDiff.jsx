import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Tree, TreeSelect, Typography } from 'antd';
import api from '../../api/axiosConfig';
import PropTypes from 'prop-types';
import DiffCard from './DiffCard';
import { DownOutlined } from '@ant-design/icons';
import '../../css/showMethodDiff.css';

const { Title } = Typography;

const ShowMethodDiff = () => {
	const { '*': urlParam } = useParams();
	const [selectedTreeData, setSelectedTreeData] = useState([]);
	const [treeData, setTreeData] = useState([]);
	const [projectName, setProjectName] = useState('');
	const [error, setError] = useState(null);
	const [value, setValue] = useState();
	const [expandedKeys, setExpandedKeys] = useState([]);

	// 獲取此專案的 DiffInfo
	useEffect(() => {
		const fetchData = async () => {
			try {
				console.log(`嘗試 fetch ${urlParam}`);
				const response = await api.post(
					`/api/getData`,
					new URLSearchParams({ ProjectName: urlParam })
				);

				const tmp = transformToSelectedTreeData(response.data);
				setSelectedTreeData(tmp);

				initialTreeData(response.data);
				// 設定 Project 為網域的最後一個 Param
				setProjectName(urlParam.substring(urlParam.lastIndexOf('/') + 1));
			} catch (error) {
				setError(error);
				console.error('Error during fetch: ', error);
			}
		};
		void fetchData();
	}, [urlParam]);

	// 產生 selected，將 JSon 格式修改成 antd treeSelect 需求
	const transformToSelectedTreeData = (data) => {
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

	// 初始化 TreeData
	const initialTreeData = (data) => {
		const tmp = [
			{
				value: `project_${data.projectId}`,
				title: data.projectName,
				label: data.projectName,
				key: `project_${data.projectId}`,
				children: data.files.map((file, index) => ({
					value: `project_${data.projectId}_${file.filesId}`,
					title: file.fileName,
					label: file.fileName,
					key: `project_${data.projectId}_${file.filesId}`,
					children: file.methods.map((method, methodIndex) => ({
						value: `project_${data.projectId}_${file.filesId}_${method.methodId}`,
						title: method.methodName,
						label: method.methodName,
						key: `project_${data.projectId}_${file.filesId}_${method.methodId}`,
						children: method.diffInfoList.map((diff, diffIndex) => ({
							value: `project_${data.projectId}_${file.filesId}_${method.methodId}_${diff.diffInfoId}`,
							title: <DiffCard diff={diff} />,
							label: diff.commitMessage,
							key: `project_${data.projectId}_${file.filesId}_${method.methodId}_${diff.diffInfoId}`
						}))
					}))
				}))
			}
		];
		setTreeData(tmp);
	};

	// 根據選擇值生成需要展開的所有父節點鍵值
	const generateExpandedKeys = (selectedValue) => {
		if (!selectedValue) return [];

		const parts = selectedValue.split('_');
		const keys = [];

		// 從選擇的完整路徑構建出所有父級路徑
		for (let i = 1; i <= parts.length; i++) {
			keys.push(parts.slice(0, i).join('_'));
		}

		return keys;
	};

	// 透過 selectTree 選擇直接將資料修該為該路徑
	const treeSelectOnChange = (newValue) => {
		console.log(newValue);
		setValue(newValue);

		// 根據選擇的值，設置需要展開的樹節點
		if (newValue) {
			const keysToExpand = generateExpandedKeys(newValue);
			setExpandedKeys(keysToExpand);
		}
	};

	return (
		<>
			{error ? (
				<p> Error: {error.message} </p>
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
						treeData={selectedTreeData}
					/>
					<Tree
						treeData={treeData}
						showLine
						switcherIcon={<DownOutlined />}
						expandedKeys={expandedKeys}
						onExpand={setExpandedKeys}
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