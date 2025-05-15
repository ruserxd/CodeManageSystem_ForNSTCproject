import { Typography } from 'antd';

const { Title } = Typography;

function MainPage() {
	return (
		<div>
			<Title> 程式單元歷程管理系統</Title>
			<Title level={2}>此系統投稿於大專學生研究計畫 by 吳堃瑋 (已結案)</Title>
		</div>
	);
}

export default MainPage;
