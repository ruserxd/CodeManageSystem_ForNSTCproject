import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Tree, TreeSelect, Typography } from 'antd';
import { FileOutlined, CodeOutlined } from '@ant-design/icons';
import api from '../../api/axiosConfig';
import HighlightedCode from './HighlightedCode';

const { Title } = Typography;

const ShowMethodDiff = () => {
	const { '*': urlParam } = useParams();
	const [treeData, setTreeData] = useState([]);
	const [projectName, setProjectName] = useState('');
	const [error, setError] = useState(null);
	const [value, setValue] = useState();
	const onChange = (newValue) => {
		setValue(newValue);
	};
	const onPopupScroll = (e) => {
		console.log('onPopupScroll', e);
	};

	useEffect(() => {
		const fetchData = async () => {
			try {
				const result = await api.post(
					`/api/getData`,
					new URLSearchParams({ ProjectName: urlParam })
				);
				setTreeData(transformData(result.data));
				console.log(result.data);
				setProjectName(urlParam.substring(urlParam.lastIndexOf('/') + 1));
			} catch (error) {
				setError(error);
				console.error('Error during fetch: ', error);
			}
		};

		if (urlParam) {
			fetchData();
		}
	}, [urlParam]);

	// TODO: 樹狀圖的修改
	const transformData = (data) => {
		return data.map((item) => ({
			title: item.fileName,
			label: item.fileName,
			key: item.filePath,
			value: item.filePath,
			icon: <FileOutlined />,
			children: item.methods.map((method, methodIndex) => ({
				title: method.methodName,
				label: method.methodName,
				key: `${item.filePath}-${methodIndex}`,
				value: `${item.filePath}-${methodIndex}`,
				icon: <CodeOutlined />,
				children: method.diffInfoList.map((diff, diffIndex) => ({
					title: `Diff ${diffIndex + 1}`,
					label: `Diff ${diffIndex + 1}`,
					key: `${item.filePath}-${methodIndex}-${diffIndex}`,
					value: `${item.filePath}-${methodIndex}-${diffIndex}`,
					icon: <CodeOutlined />,
					diffInfo: diff
				}))
			}))
		}));
	};

	const renderDiffInfo = (title, diffInfo) => (
		<div className="diffINFO">
			<Card
				title={title}
				bordered={false}
				style={{
					width: 500,
				}}
			>
				<p>Author: {diffInfo.author}</p>
				<p>Author Email: {diffInfo.authorEmail}</p>
				<p>Commit Message: {diffInfo.commitMessage}</p>
				<p>Commit Time: {diffInfo.commitTime}</p>
				<p>HeadRevstr: {diffInfo.headRevstr}</p>
				<HighlightedCode language="diff" codeString={diffInfo.diffCode} className="diffCode" />
			</Card>
		</div>
	);

	if (error) {
		return <div>Error: {error.message}</div>;
	}

	return (
		<div>
			<Title level={2}>{projectName} 的方法差異資訊</Title>
			<TreeSelect
				showSearch
				style={{
					width: '35%',
				}}
				value={value}
				dropdownStyle={{
					maxHeight: 400,
					overflow: 'auto',
				}}
				placeholder="Please select"
				allowClear
				onChange={onChange}
				treeData={treeData}
				onPopupScroll={onPopupScroll}
			/>
			<Tree
				treeData={treeData}
				defaultExpandAll
				showIcon
				titleRender={(nodeData) => {
					if (nodeData.diffInfo) {
						return (
							<div>
								{renderDiffInfo(nodeData.title, nodeData.diffInfo)}
							</div>
						);
					}
					return <span>{nodeData.title}</span>;
				}}
			/>
		</div>
	);
};

export default ShowMethodDiff;
