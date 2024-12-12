import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Tree, TreeSelect, Typography } from 'antd';
import { CodeOutlined, FileOutlined } from '@ant-design/icons';
import api from '../../api/axiosConfig';
import HighlightedCode from './HighlightedCode';

const { Title } = Typography;

const ShowMethodDiff = () => {
	const { '*': urlParam } = useParams();
	const [treeData, setTreeData] = useState([]);
	const [projectName, setProjectName] = useState('');
	const [error, setError] = useState(null);
	const [value, setValue] = useState();
	const [showMethodDiff, setShowMethodDiff] = useState([]);
	const onPopupScroll = (e) => {
		// console.log('onPopupScroll', e);
	};

	useEffect(() => {
		const fetchData = async () => {
			try {
				console.log(`嘗試 fetch ${urlParam}`);
				const result = await api.post(
					`/api/getData`,
					new URLSearchParams({ ProjectName: urlParam })
				);
				console.log('轉成樹狀圖 result.data\n' + JSON.stringify(result.data));

				const transformedData = setTreeData(transformData(result.data));
				console.log('轉成樹狀圖 treeData\n' + JSON.stringify(transformedData));

				// 設定 Project 為網域的最後一個 Param
				setProjectName(urlParam.substring(urlParam.lastIndexOf('/') + 1));
			} catch (error) {
				setError(error);
				console.error('Error during fetch: ', error);
			}
		};
		void fetchData();
	}, [urlParam]);

	// (避免異步狀態的處理) 當 treeData 更改時，才去執行 setShowMethodDiff
	useEffect(() => {
		console.log('轉成樹狀圖 showMethodDiff\n' + JSON.stringify(treeData));
		setShowMethodDiff(treeData);
	}, [treeData]);

	const treeSelectOnChange = (newValue) => {
		setValue(newValue);
		console.log(newValue);
	};

	// TODO: 樹狀圖的修改
	// 進入時會執行一遍
	const transformData = (data) => {
		return data.map((item) => ({
			title: item.fileName,
			label: item.fileName,
			key: item.fileName,
			value: item.fileName,
			icon: <FileOutlined />,
			children: item.methods.map((method, methodIndex) => ({
				title: method.methodName,
				label: method.methodName,
				key: `${item.fileName}_${methodIndex}`,
				value: `${item.fileName}_${methodIndex}`,
				icon: <CodeOutlined />,
				children: method.diffInfoList.map((diff, diffIndex) => ({
					title: `Diff ${diffIndex + 1}`,
					label: `Diff ${diffIndex + 1}`,
					key: `${item.fileName}_${methodIndex}_${diffIndex}`,
					value: `${item.fileName}_${methodIndex}_${diffIndex}`,
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
					width: 500
				}}>
				<p>Author: {diffInfo.author}</p>
				<p>Author Email: {diffInfo.authorEmail}</p>
				<p>Commit Message: {diffInfo.commitMessage}</p>
				<p>Commit Time: {diffInfo.commitTime}</p>
				<p>HeadRevstr: {diffInfo.headRevstr}</p>
				<HighlightedCode language="diff" codeString={diffInfo.diffCode} className="diffCode" />
			</Card>
		</div>
	);

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
						onPopupScroll={onPopupScroll}
					/>
					<Tree
						treeData={showMethodDiff}
						defaultExpandAll
						showIcon
						titleRender={(nodeData) => {
							if (nodeData.diffInfo) {
								return <div>{renderDiffInfo(nodeData.title, nodeData.diffInfo)}</div>;
							}
							return <span>{nodeData.title}</span>;
						}}
					/>
				</div>
			)}
		</>
	);
};

export default ShowMethodDiff;
