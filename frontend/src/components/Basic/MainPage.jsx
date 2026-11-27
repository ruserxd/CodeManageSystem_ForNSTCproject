import PropTypes from 'prop-types';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCookies } from 'react-cookie';
import { App, Button, Col, Divider, Row, Space, Typography } from 'antd';
import { GithubOutlined } from '@ant-design/icons';
import api from '@/api/axiosConfig';
import HighlightedCode from '../diff/HighlightedCode';
import { SAMPLE_HISTORY, SAMPLE_METHOD } from './landingSample';

const { Title, Paragraph, Text } = Typography;

const REPO_URL = 'https://github.com/ruserxd/CodeManageSystem_ForNSTCproject';

// 範例方法的修改歷程：左邊選 commit，右邊顯示該次 diff
function SampleHistory() {
	const [index, setIndex] = useState(SAMPLE_HISTORY.length - 1);
	const cur = SAMPLE_HISTORY[index];

	return (
		<Row gutter={[24, 16]}>
			<Col xs={24} md={8}>
				<Text type="secondary">
					{SAMPLE_METHOD.file} · <Text code>{SAMPLE_METHOD.signature}</Text>
				</Text>
				<div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 4 }}>
					{SAMPLE_HISTORY.map((c, i) => (
						<button
							key={c.sha}
							type="button"
							onClick={() => setIndex(i)}
							aria-pressed={i === index}
							style={{
								textAlign: 'left',
								padding: '10px 12px',
								border: 'none',
								borderLeft: `3px solid ${i === index ? '#5680E9' : '#e5e7eb'}`,
								background: i === index ? '#f0f4fd' : 'transparent',
								cursor: 'pointer',
								font: 'inherit'
							}}>
							<div style={{ fontFamily: 'monospace', fontSize: 12, color: '#6b7280' }}>
								{c.sha} · {c.date}
							</div>
							<div>{c.summary}</div>
						</button>
					))}
				</div>
				<Paragraph type="secondary" style={{ marginTop: 12, fontSize: 13 }}>
					取自本專案 repo，共 {SAMPLE_METHOD.totalCommits} 次修改中的 {SAMPLE_HISTORY.length} 次。
				</Paragraph>
			</Col>
			<Col xs={24} md={16}>
				<Paragraph style={{ marginBottom: 8 }}>{cur.note}</Paragraph>
				<div style={{ overflowX: 'auto', fontSize: 13 }}>
					<HighlightedCode language="diff" codeString={cur.diff} />
				</div>
			</Col>
		</Row>
	);
}

function MainPage({ onLogin }) {
	const navigate = useNavigate();
	const { message } = App.useApp();
	const [cookies] = useCookies(['user']);
	const [demoLoading, setDemoLoading] = useState(false);

	const handleDemoLogin = async () => {
		setDemoLoading(true);
		try {
			const { data } = await api.post('api/demo-login');
			if (data.success) {
				onLogin(data.myUser, data.token);
				navigate('/UserPage');
			} else {
				message.error(`訪客登入失敗：${data.message}`);
			}
		} catch (error) {
			console.error('訪客登入錯誤:', error);
			message.error('訪客登入時發生錯誤，請稍後再試');
		} finally {
			setDemoLoading(false);
		}
	};

	return (
		<div style={{ maxWidth: 1080 }}>
			<Title level={2} style={{ marginBottom: 8 }}>
				MethodTracker
			</Title>
			<Paragraph style={{ fontSize: 16 }}>把 Java 專案拆成方法，逐 commit 看每個方法改了什麼。</Paragraph>
			<Space wrap>
				{cookies.user ? (
					<Button type="primary" size="large" onClick={() => navigate('/UserPage')}>
						前往我的專案
					</Button>
				) : (
					<>
						<Button type="primary" size="large" loading={demoLoading} onClick={handleDemoLogin}>
							以訪客身分試用
						</Button>
						<Link to="/Login">登入</Link>
					</>
				)}
			</Space>

			<Divider />
			<Title level={4}>這個方法是怎麼變成現在這樣的？</Title>
			<Paragraph type="secondary">git diff 以檔案為單位；這裡只看單一方法。</Paragraph>
			<SampleHistory />

			<Divider />
			<Text type="secondary">
				<Link to="/About">關於本專案</Link> ·{' '}
				<a href={REPO_URL} target="_blank" rel="noreferrer">
					<GithubOutlined /> GitHub
				</a>
			</Text>
		</div>
	);
}

MainPage.propTypes = {
	onLogin: PropTypes.func.isRequired
};

export default MainPage;
