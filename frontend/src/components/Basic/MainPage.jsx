import PropTypes from 'prop-types';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCookies } from 'react-cookie';
import { App, Button, Space, Typography, theme } from 'antd';
import { GithubOutlined } from '@ant-design/icons';
import api from '@/api/axiosConfig';
import HighlightedCode from '../diff/HighlightedCode';
import { SAMPLE_HISTORY, SAMPLE_METHOD } from './landingSample';

const { Title, Paragraph, Text } = Typography;

const REPO_URL = 'https://github.com/ruserxd/CodeManageSystem_ForNSTCproject';

// 只有 hover／focus-visible／RWD 需要 CSS，其餘用 inline style；class 一律加 mt-home- 前綴避免影響其他頁面
function scopedCss(token) {
	return `
.mt-home-body {
	display: grid;
	grid-template-columns: 280px minmax(0, 1fr);
}
.mt-home-list {
	border-right: 1px solid ${token.colorBorderSecondary};
}
@media (max-width: 767px) {
	.mt-home-body { grid-template-columns: minmax(0, 1fr); }
	.mt-home-list { border-right: none; border-bottom: 1px solid ${token.colorBorderSecondary}; }
}
.mt-home-commit {
	display: block;
	width: 100%;
	min-height: 44px;
	padding: 10px 16px 10px 13px;
	border: none;
	border-left: 3px solid transparent;
	background: transparent;
	color: ${token.colorText};
	font: inherit;
	text-align: left;
	cursor: pointer;
	transition: background ${token.motionDurationMid};
}
.mt-home-commit:hover { background: ${token.colorFillTertiary}; }
.mt-home-commit:focus-visible {
	outline: 2px solid ${token.colorPrimary};
	outline-offset: -2px;
}
.mt-home-commit[aria-pressed='true'] {
	border-left-color: ${token.colorPrimary};
	background: ${token.colorPrimaryBg};
}
.mt-home-code { flex: 1; min-width: 0; display: flex; }
.mt-home-code pre {
	flex: 1;
	margin: 0 !important;
	border-radius: 0 !important;
	padding: 16px !important;
	font-size: 13px;
	line-height: 1.6;
}
`;
}

// 範例方法的修改歷程：左邊選 commit，右邊顯示該次 diff，上方一條 header 讓兩邊看起來是同一個面板
function SampleHistory() {
	const { token } = theme.useToken();
	const [index, setIndex] = useState(SAMPLE_HISTORY.length - 1);
	const cur = SAMPLE_HISTORY[index];
	const mono = { fontFamily: token.fontFamilyCode };

	return (
		<>
			<style>{scopedCss(token)}</style>
			<div
				style={{
					border: `1px solid ${token.colorBorderSecondary}`,
					borderRadius: token.borderRadiusLG,
					overflow: 'hidden',
					background: token.colorBgContainer
				}}>
				<div
					style={{
						display: 'flex',
						flexWrap: 'wrap',
						alignItems: 'baseline',
						justifyContent: 'space-between',
						gap: '4px 16px',
						padding: '10px 16px',
						background: token.colorFillTertiary,
						borderBottom: `1px solid ${token.colorBorderSecondary}`,
						fontSize: 13
					}}>
					<span style={{ ...mono, overflowWrap: 'anywhere' }}>
						<Text type="secondary">{SAMPLE_METHOD.file}</Text>
						<Text type="secondary"> · </Text>
						<Text strong>{SAMPLE_METHOD.signature}</Text>
					</span>
					<Text type="secondary" style={mono}>
						{cur.sha}
					</Text>
				</div>
				<div className="mt-home-body">
					<div className="mt-home-list" role="group" aria-label="選擇 commit">
						{SAMPLE_HISTORY.map((c, i) => (
							<button
								key={c.sha}
								type="button"
								className="mt-home-commit"
								onClick={() => setIndex(i)}
								aria-pressed={i === index}>
								<div style={{ ...mono, fontSize: 12, color: token.colorTextSecondary }}>
									{c.sha} · {c.date}
								</div>
								<div style={{ marginTop: 2, fontWeight: i === index ? 500 : 400 }}>{c.summary}</div>
							</button>
						))}
					</div>
					<div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, minHeight: 320 }}>
						<div
							style={{
								padding: '10px 16px',
								fontSize: 13,
								color: token.colorTextSecondary,
								borderBottom: `1px solid ${token.colorBorderSecondary}`
							}}>
							{cur.note}
						</div>
						<div className="mt-home-code">
							<HighlightedCode language="diff" codeString={cur.diff} />
						</div>
					</div>
				</div>
			</div>
			<Text type="secondary" style={{ display: 'block', marginTop: 8, fontSize: 12 }}>
				取自本專案 repo，共 {SAMPLE_METHOD.totalCommits} 次修改中的 {SAMPLE_HISTORY.length} 次。
			</Text>
		</>
	);
}

function MainPage({ onLogin }) {
	const navigate = useNavigate();
	const { message } = App.useApp();
	const [cookies] = useCookies(['user']);
	const [demoLoading, setDemoLoading] = useState(false);
	const { token } = theme.useToken();

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
			<Title level={2} style={{ marginTop: 8, marginBottom: 8 }}>
				MethodTracker
			</Title>
			<Paragraph style={{ fontSize: 16, color: token.colorTextSecondary, marginBottom: 24 }}>
				把 Java 專案拆成方法，逐 commit 看每個方法改了什麼。
			</Paragraph>
			<Space size={16} align="center" wrap>
				{cookies.user ? (
					<Button type="primary" size="large" onClick={() => navigate('/UserPage')}>
						前往我的專案
					</Button>
				) : (
					<>
						<Button type="primary" size="large" loading={demoLoading} onClick={handleDemoLogin}>
							以訪客身分試用
						</Button>
						<Link to="/Login" style={{ fontSize: 15 }}>
							登入
						</Link>
					</>
				)}
			</Space>

			<section style={{ marginTop: 56 }}>
				<Title level={4} style={{ marginBottom: 4 }}>
					這個方法是怎麼變成現在這樣的？
				</Title>
				<Paragraph type="secondary" style={{ marginBottom: 16 }}>
					git diff 以檔案為單位；這裡只看單一方法。
				</Paragraph>
				<SampleHistory />
			</section>

			<footer
				style={{
					marginTop: 56,
					paddingTop: 16,
					borderTop: `1px solid ${token.colorBorderSecondary}`,
					fontSize: 13,
					color: token.colorTextTertiary
				}}>
				<Link to="/About" style={{ color: 'inherit' }}>
					關於本專案
				</Link>
				<span style={{ margin: '0 8px' }}>·</span>
				<a href={REPO_URL} target="_blank" rel="noreferrer" style={{ color: 'inherit' }}>
					<GithubOutlined /> GitHub
				</a>
			</footer>
		</div>
	);
}

MainPage.propTypes = {
	onLogin: PropTypes.func.isRequired
};

export default MainPage;
