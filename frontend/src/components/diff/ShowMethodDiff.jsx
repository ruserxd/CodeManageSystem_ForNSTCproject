import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Tree, TreeSelect, Typography, Spin } from 'antd';
import api from '@/api/axiosConfig';
import PropTypes from 'prop-types';
import DiffCard from './DiffCard';
import { DownOutlined, LoadingOutlined } from '@ant-design/icons';
import '@/css/showMethodDiff.css';

const { Title } = Typography;

const ShowMethodDiff = () => {
	const { '*': urlParam } = useParams();
	const [selectedTreeData, setSelectedTreeData] = useState([]);
	const [treeData, setTreeData] = useState([]);
	const [projectName, setProjectName] = useState('');
	const [error, setError] = useState(null);
	const [value, setValue] = useState();
	const [expandedKeys, setExpandedKeys] = useState([]);
	const [loadedMethodIds, setLoadedMethodIds] = useState(new Set());
	const [loading, setLoading] = useState(true);

	// 初次載入：獲取專案結構（不包含 DiffInfo）
	useEffect(() => {
		const fetchStructure = async () => {
			try {
				setLoading(true);
				console.log(`嘗試 fetch 專案結構 ${urlParam}`);
				const response = await api.get('/api/getProjectStructure', {
					params: { projectName: urlParam }
				});

				const structure = response.data;

				// 生成 TreeSelect 數據
				const selectedData = transformToSelectedTreeData(structure);
				setSelectedTreeData(selectedData);

				// 生成 Tree 數據（DiffInfo 用 placeholder）
				const treeDataInit = initialTreeData(structure);
				setTreeData(treeDataInit);

				setProjectName(urlParam.substring(urlParam.lastIndexOf('/') + 1));
			} catch (error) {
				setError(error);
				console.error('Error during fetch structure: ', error);
			} finally {
				setLoading(false);
			}
		};
		void fetchStructure();
	}, [urlParam]);

	// 生成 TreeSelect 數據
	const transformToSelectedTreeData = (data) => {
		return [
			{
				value: `project_${data.projectId}`,
				title: data.projectName,
				label: data.projectName,
				children: data.files.map((file) => ({
					value: `project_${data.projectId}_${file.filesId}`,
					title: file.fileName,
					label: file.fileName,
					children: file.methods.map((method) => ({
						value: `project_${data.projectId}_${file.filesId}_${method.methodId}`,
						title: method.methodName,
						label: `${method.methodName} (${method.diffCount} diffs)`
					}))
				}))
			}
		];
	};

	// 初始化 TreeData（DiffInfo 顯示為 placeholder）
	const initialTreeData = (data) => {
		return [
			{
				value: `project_${data.projectId}`,
				title: data.projectName,
				label: data.projectName,
				key: `project_${data.projectId}`,
				children: data.files.map((file) => ({
					value: `project_${data.projectId}_${file.filesId}`,
					title: file.fileName,
					label: file.fileName,
					key: `project_${data.projectId}_${file.filesId}`,
					children: file.methods.map((method) => ({
						value: `project_${data.projectId}_${file.filesId}_${method.methodId}`,
						title: method.methodName,
						label: method.methodName,
						key: `project_${data.projectId}_${file.filesId}_${method.methodId}`,
						methodId: method.methodId,
						children: [
							{
								key: `loading_${method.methodId}`,
								title: <span style={{ color: '#999' }}>點擊載入差異信息...</span>,
								isLeaf: true
							}
						]
					}))
				}))
			}
		];
	};

	// 當 Tree 節點展開時，動態載入 DiffInfo
	const onExpand = async (expandedKeysValue) => {
		setExpandedKeys(expandedKeysValue);

		// 找出新展開的 method 節點
		const newExpandedKeys = expandedKeysValue.filter(
			(key) => !expandedKeys.includes(key) && key.split('_').length === 4
		);

		for (const key of newExpandedKeys) {
			const parts = key.split('_');
			const methodId = parseInt(parts[3]);

			// 如果已經載入過，跳過
			if (loadedMethodIds.has(methodId)) continue;

			try {
				console.log(`載入 Method ${methodId} 的 DiffInfo`);
				const response = await api.get('/api/getDiffInfoByMethod', {
					params: { methodId }
				});

				const diffInfoList = response.data;

				// 更新 TreeData，將 placeholder 替換為實際的 DiffCard
				setTreeData((prevTreeData) => {
					return updateTreeDataWithDiffInfo(
						prevTreeData,
						key,
						diffInfoList,
						parts[1],
						parts[2],
						methodId
					);
				});

				// 標記已載入
				setLoadedMethodIds((prev) => new Set(prev).add(methodId));
			} catch (error) {
				console.error(`載入 Method ${methodId} 的 DiffInfo 失敗:`, error);
			}
		}
	};

	// 遞迴更新 TreeData
	const updateTreeDataWithDiffInfo = (
		data,
		targetKey,
		diffInfoList,
		projectId,
		fileId,
		methodId
	) => {
		return data.map((node) => {
			if (node.key === targetKey) {
				// 找到目標節點，更新其 children
				return {
					...node,
					children: diffInfoList.map((diff) => ({
						value: `project_${projectId}_${fileId}_${methodId}_${diff.diffInfoId}`,
						title: <DiffCard diff={diff} />,
						label: diff.commitMessage,
						key: `project_${projectId}_${fileId}_${methodId}_${diff.diffInfoId}`,
						isLeaf: true
					}))
				};
			}

			if (node.children) {
				// 遞迴處理子節點
				return {
					...node,
					children: updateTreeDataWithDiffInfo(
						node.children,
						targetKey,
						diffInfoList,
						projectId,
						fileId,
						methodId
					)
				};
			}

			return node;
		});
	};

	// 根據選擇值生成需要展開的所有父節點鍵值
	const generateExpandedKeys = (selectedValue) => {
		if (!selectedValue) return [];

		const parts = selectedValue.split('_');
		const keys = [];

		for (let i = 1; i <= parts.length; i++) {
			keys.push(parts.slice(0, i).join('_'));
		}

		return keys;
	};

	// 透過 selectTree 選擇直接將資料修改為該路徑
	const treeSelectOnChange = (newValue) => {
		console.log(newValue);
		setValue(newValue);

		if (newValue) {
			const keysToExpand = generateExpandedKeys(newValue);
			setExpandedKeys(keysToExpand);
		}
	};

	if (loading) {
		return (
			<div style={{ textAlign: 'center', padding: '50px' }}>
				<Spin indicator={<LoadingOutlined style={{ fontSize: 48 }} spin />} />
				<p style={{ marginTop: 20 }}>載入專案結構中...</p>
			</div>
		);
	}

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
						onExpand={onExpand}
						virtual
						height={600}
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
						diffCount: PropTypes.number
					})
				)
			})
		)
	})
};

export default ShowMethodDiff;