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
					節錄自本專案 repo 的真實紀錄，這個方法共被修改過 {SAMPLE_METHOD.totalCommits} 次。
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
			<Paragraph style={{ fontSize: 16, maxWidth: 680 }}>
				把 Java 專案拆成一個個方法，逐 commit 看每個方法改了什麼。貼上 Git repo
				網址，系統會走過 commit 歷史，整理出每個方法在各次 commit 之間的差異。
			</Paragraph>
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
						<Text type="secondary">
							不用註冊，可瀏覽已分析好的範例專案。想分析自己的 repo 請<Link to="/Login">登入</Link>或
							<Link to="/Register">註冊</Link>。
						</Text>
					</>
				)}
			</Space>

			<Divider />
			<Title level={4}>這個方法是怎麼變成現在這樣的？</Title>
			<Paragraph type="secondary">
				下面是 MethodTracker 自己的登入驗證方法，點左邊的 commit 看那一次改了什麼。
			</Paragraph>
			<SampleHistory />

			<Divider />
			<Row gutter={[32, 24]}>
				<Col xs={24} md={12}>
					<Title level={4}>git diff 不就看得到了？</Title>
					<Paragraph>
						看得到，但 git diff 以「檔案」為單位。上面的 <Text code>17ac658</Text>{' '}
						在同一個檔案裡同時改了 import、新增了一個方法、改了註冊流程，checkUser
						的改動夾在中間。
					</Paragraph>
					<Paragraph>
						想知道「某一個方法」經歷過哪些修改時，得自己一個 commit 一個 commit 翻。MethodTracker
						用 JavaParser 把每個版本拆成方法，再比對同一個方法的前後版本。
					</Paragraph>
				</Col>
				<Col xs={24} md={12}>
					<Title level={4}>怎麼用</Title>
					<ol style={{ paddingLeft: 20 }}>
						<li>登入後，在「使用者」頁面貼上公開的 Git repo 網址。</li>
						<li>系統 clone 專案，從最新的 commit 往回逐一比對相鄰的兩個版本。</li>
						<li>在專案列表選檔案、選方法，逐次檢視該方法的 diff。</li>
					</ol>
				</Col>
			</Row>

			<Divider />
			<Row gutter={[32, 24]}>
				<Col xs={24} md={12}>
					<Title level={4}>目前的限制</Title>
					<ul style={{ paddingLeft: 20 }}>
						<li>只分析 .java 檔。</li>
						<li>一般帳號單次最多分析 500 個 commit，更早的 commit 不會納入。</li>
						<li>每天的分析次數有上限。</li>
						<li>commit 很多的 repo 第一次分析會比較久，頁面可能先顯示逾時，但分析仍在背景進行。</li>
					</ul>
				</Col>
				<Col xs={24} md={12}>
					<Title level={4}>專案背景</Title>
					<Paragraph>
						源自國科會大專學生研究計畫（已結案），目前持續開發中。前端 React + Ant Design，後端
						Spring Boot + JGit + JavaParser，資料存在 MySQL，以 Docker Compose 部署。
					</Paragraph>
					<a href={REPO_URL} target="_blank" rel="noreferrer">
						<GithubOutlined /> 原始碼在 GitHub
					</a>
				</Col>
			</Row>
		</div>
	);
}

MainPage.propTypes = {
	onLogin: PropTypes.func.isRequired
};

export default MainPage;
