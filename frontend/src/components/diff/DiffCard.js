import HighlightedCode from './HighlightedCode';
import { Card } from 'antd';
import { useState } from 'react';
import '../../css/showMethodDiff.css';

// 將 Card 的部分抽離出來，更方便管理每個 Card 的 state
const DiffCard = ({ diff }) => {
	const [activeKey, setActiveKey] = useState(`tab_${diff.diffInfoId}`); // 設定預設值

	const tabList = [
		{
			key: `tab_${diff.diffInfoId}`,
			tab: 'author'
		},
		{
			key: `tab2_${diff.diffInfoId}`,
			tab: 'diffInfo'
		}
	];

	const contentList = {
		[`tab_${diff.diffInfoId}`]: (
			<div>
				<p>作者：{diff.author}</p>
				<p>信箱：{diff.authorEmail}</p>
				<p>提交時間：{diff.commitTime}</p>
			</div>
		),
		[`tab2_${diff.diffInfoId}`]: <HighlightedCode language={'java'} codeString={diff.diffCode} />
	};

	return (
		<Card
			title={diff.headRevstr}
			style={{ width: 800 }}
			tabList={tabList}
			bordered={false}
			activeTabKey={activeKey}
			onTabChange={setActiveKey}
			tabProps={{
				size: 'middle'
			}}>
			{contentList[activeKey]}
		</Card>
	);
};
export default DiffCard;
