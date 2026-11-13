import { Typography } from 'antd';

const { Title, Paragraph } = Typography;

function MainPage() {
	return (
		<div>
			<Title>MethodTracker 程式單元歷程管理系統</Title>
			<Title level={3}>以「方法」為單位，追蹤 Java 程式碼隨 commit 的變化</Title>
			<Paragraph>
				輸入一個 Git repo 網址，系統會走過 commit 歷史，把每個 Java 方法在各次 commit 之間的差異整理出來。
				登入後即可從「使用者」頁面開始分析。
			</Paragraph>
		</div>
	);
}

export default MainPage;
