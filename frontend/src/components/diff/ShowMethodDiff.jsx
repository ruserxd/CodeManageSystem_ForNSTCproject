import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Tree, TreeSelect, Typography, Spin, Space, Tag } from 'antd';
import api from '@/api/axiosConfig';
import PropTypes from 'prop-types';
import DiffCard from './DiffCard';
import { DownOutlined, LoadingOutlined, FileOutlined, FunctionOutlined } from '@ant-design/icons';
import '@/css/showMethodDiff.css';

const { Title } = Typography;

// ShowMethodDiff.jsx (最終修訂後的 buildNestedTree 函式 - 消除警告)
const buildNestedTree = (files, projectId, isTreeSelect) => {
	// 確保每次呼叫 buildNestedTree (用於 Tree 和 TreeSelect) 時，目錄計數器都從 0 開始
	let tempCounter = 0;
	const root = {};

	files.forEach((file) => {
		const parts = file.filePath.split('/').filter(p => p.length > 0);
		let current = root;

		parts.forEach((part, index) => {
			const isFile = index === parts.length - 1;
			const key = part;

			if (!current[key]) {
				const newNode = {
					title: part,
					label: part,
				};

				if (isFile) {
					// 檔案節點 (File)：可選
					const fileNodeId = `project_${projectId}_${file.filesId}`;

					newNode.value = fileNodeId;
					newNode.key = fileNodeId;
					newNode.isLeaf = isTreeSelect;

					newNode.methods = file.methods.map((method) => {
						const methodKey = `${fileNodeId}_${method.methodId}`;
						const methodNode = {
							// 方法節點：可選
							value: methodKey,
							title: method.methodName,
							label: method.methodName,
							key: methodKey,
							methodId: method.methodId,
						};

						if (isTreeSelect) {
							methodNode.label = `${method.methodName} (${method.diffCount} diffs)`;
						} else {
							methodNode.isLeaf = false;
						}
						return methodNode;
					});

				} else {
					// 目錄節點 (Directory)：不可選，但 key 和 value 必須一致
					tempCounter++;
					const dirKey = `dir_${tempCounter}`;

					// *** 關鍵修正：將 key 和 value 設為相同的 dirKey 字串 ***
					newNode.key = dirKey;
					newNode.value = dirKey;

					newNode.children = {};
					newNode.isLeaf = false;
				}

				current[key] = newNode;
			}

			// 只有當當前節點是目錄（有 children 物件）時，才繼續遍歷
			if (!isFile && current[key].children) {
				current = current[key].children;
			}
		});
	});

	// 將 Map 結構轉換為 Array 結構
	const mapToArray = (map) => {
		return Object.values(map).map(node => {
			if (node.methods) {
				// 如果是檔案節點，使用 methods 替換 children
				return { ...node, children: node.methods };
			}

			if (node.children && typeof node.children === 'object' && !Array.isArray(node.children)) {
				// 遞迴處理目錄節點
				return { ...node, children: mapToArray(node.children) };
			}

			return node;
		});
	};

	return mapToArray(root);
};
// -------------------------------------------------------------------


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
	const [statistics, setStatistics] = useState({ fileCount: 0, methodCount: 0 });

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

				// 計算統計數據
				const fileCount = structure.files?.length || 0;
				const methodCount = structure.files?.reduce((total, file) => {
					return total + (file.methods?.length || 0);
				}, 0) || 0;
				setStatistics({ fileCount, methodCount });

				// 生成 TreeSelect 數據
				const selectedData = transformToSelectedTreeData(structure);
				setSelectedTreeData(selectedData);

				// 生成 Tree 數據（method 層級暫時沒有 children）
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

	// 生成 TreeSelect 數據 (使用新的巢狀建構邏輯)
	const transformToSelectedTreeData = (data) => {
		const nestedFiles = buildNestedTree(data.files, data.projectId, true);
		const projectIdKey = `project_${data.projectId}`;
		return [
			{
				value: projectIdKey,
				title: data.projectName,
				label: data.projectName,
				// 為 TreeSelect 根節點設定 key = value
				key: projectIdKey,
				children: nestedFiles
			}
		];
	};

	// 初始化 TreeData（使用新的巢狀建構邏輯）
	const initialTreeData = (data) => {
		const nestedFiles = buildNestedTree(data.files, data.projectId, false);
		const projectIdKey = `project_${data.projectId}`;
		return [
			{
				value: projectIdKey,
				title: data.projectName,
				label: data.projectName,
				key: projectIdKey,
				children: nestedFiles
			}
		];
	};

	// 當 Tree 節點展開時，動態載入 DiffInfo
	const onExpand = async (expandedKeysValue) => {
		setExpandedKeys(expandedKeysValue);

		// 找出新展開的 method 節點
		// method 節點現在的 key 格式為 project_projectId_filesId_methodId (4 個部分)
		const newExpandedKeys = expandedKeysValue.filter(
			(key) => !expandedKeys.includes(key) && key.split('_').length === 4
		);

		for (const key of newExpandedKeys) {
			await loadMethodDiffInfo(key);
		}
	};

	// 載入指定 method 的 DiffInfo
	const loadMethodDiffInfo = async (key) => {
		const parts = key.split('_');
		// methodId 是 key 的第四個部分
		const methodId = parseInt(parts[3]);

		// 如果已經載入過，跳過
		if (loadedMethodIds.has(methodId)) return;

		try {
			// 先顯示載入中狀態
			setTreeData((prevTreeData) => {
				return updateTreeDataWithLoading(prevTreeData, key);
			});

			console.log(`載入 Method ${methodId} 的 DiffInfo`);
			const response = await api.get('/api/getDiffInfoByMethod', {
				params: { methodId }
			});

			const diffInfoList = response.data;

			// 更新 TreeData，將 loading 替換為實際的 DiffCard
			setTreeData((prevTreeData) => {
				return updateTreeDataWithDiffInfo(
					prevTreeData,
					key,
					diffInfoList,
					parts[1], // projectId
					parts[2], // fileId
					methodId
				);
			});

			// 標記已載入
			setLoadedMethodIds((prev) => new Set(prev).add(methodId));
		} catch (error) {
			console.error(`載入 Method ${methodId} 的 DiffInfo 失敗:`, error);
			// 載入失敗時顯示錯誤訊息
			setTreeData((prevTreeData) => {
				return updateTreeDataWithError(prevTreeData, key);
			});
		}
	};

	// 更新為載入中狀態
	const updateTreeDataWithLoading = (data, targetKey) => {
		return data.map((node) => {
			if (node.key === targetKey) {
				return {
					...node,
					children: [
						{
							key: `loading_${targetKey}`,
							title: (
								<span style={{ color: '#1890ff' }}>
          <LoadingOutlined /> 載入中...
         </span>
							),
							isLeaf: true
						}
					]
				};
			}

			if (node.children) {
				return {
					...node,
					children: updateTreeDataWithLoading(node.children, targetKey)
				};
			}

			return node;
		});
	};

	// 更新為錯誤狀態
	const updateTreeDataWithError = (data, targetKey) => {
		return data.map((node) => {
			if (node.key === targetKey) {
				return {
					...node,
					children: [
						{
							key: `error_${targetKey}`,
							title: <span style={{ color: '#ff4d4f' }}>載入失敗，請重試</span>,
							isLeaf: true
						}
					]
				};
			}

			if (node.children) {
				return {
					...node,
					children: updateTreeDataWithError(node.children, targetKey)
				};
			}

			return node;
		});
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

	// 根據 TreeSelect 的選擇值，找出所有需要展開的父節點 key
	const findKeysToExpand = (data, targetValue, currentPathKeys = []) => {
		for (const node of data) {

			if (node.value === targetValue) {
				// 找到目標，返回路徑上的所有 key 和目標 key
				return [...currentPathKeys, node.key];
			}

			if (node.children) {
				// 將當前節點的 key 加入路徑中
				const foundKeys = findKeysToExpand(node.children, targetValue, [...currentPathKeys, node.key]);

				if (foundKeys.length > 0) {
					// 如果在子節點中找到目標，則返回該路徑
					return foundKeys;
				}
			}
		}
		return [];
	};

	// 透過 selectTree 選擇直接將資料修改為該路徑
	const treeSelectOnChange = async (newValue) => {
		console.log(newValue);
		setValue(newValue);

		if (newValue) {
			// 1. 找出所有需要展開的 key (包含目錄節點)
			const projectNode = treeData[0];
			const keysToExpand = findKeysToExpand(projectNode.children, newValue, [projectNode.key]);

			// 移除重複和空值
			const uniqueKeysToExpand = [...new Set(keysToExpand.filter(Boolean))];
			setExpandedKeys(uniqueKeysToExpand);

			// 2. 如果選擇的是方法層級（4個部分），自動載入差異資訊
			const parts = newValue.split('_');
			if (parts.length === 4) {
				const methodKey = newValue;
				// 延遲一下確保 expandedKeys 已更新
				setTimeout(async () => {
					await loadMethodDiffInfo(methodKey);
				}, 100);
			}
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
					<Space direction="vertical" size="large" style={{ width: '100%' }}>
						<div>
							<Title level={2}>{projectName} 的方法差異資訊</Title>
							<Space size="middle" style={{ marginTop: 8 }}>
								<Tag icon={<FileOutlined />} color="blue">
									{statistics.fileCount} 個檔案
								</Tag>
								<Tag icon={<FunctionOutlined />} color="green">
									{statistics.methodCount} 個方法
								</Tag>
							</Space>
						</div>

						<TreeSelect
							showSearch
							style={{
								width: '35%'
							}}
							value={value}
							placeholder="請輸入檔案名稱或方法名稱搜尋"
							allowClear
							onChange={treeSelectOnChange}
							treeData={selectedTreeData}
							treeNodeFilterProp="title"
							filterTreeNode={(input, treeNode) => {
								const title = treeNode.title || treeNode.label || '';
								return title.toLowerCase().includes(input.toLowerCase());
							}}
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
					</Space>
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