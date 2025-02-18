import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { DesktopOutlined, HomeOutlined, PieChartOutlined, UserOutlined, ToolOutlined } from '@ant-design/icons';
import { Breadcrumb, Layout, Menu, theme } from 'antd';

const { Content, Footer, Sider } = Layout;

function getItem(label, key, icon, children) {
	return { key, icon, children, label, type: 'item' };
}

function MainLayout({ user }) {
	const [collapsed, setCollapsed] = useState(false);
	const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();
	let curLocation = useLocation();

	const items = useMemo(() => [
		getItem(<Link to="/">主頁</Link>, '主頁', <PieChartOutlined />),
		getItem('關於', 'sub1', <DesktopOutlined />, [
			getItem(<Link to="/Contact">聯絡</Link>, '聯絡'),
			getItem(<Link to="/About">關於</Link>, '關於')
		]),
		user ? getItem(<Link to="/UserPage">使用者</Link>, '使用者', <UserOutlined />) :
		getItem('使用者', 'sub2', <UserOutlined />, [
			getItem(<Link to="/Login">登入</Link>, '登入'),
			getItem(<Link to="/Register">註冊</Link>, '註冊')
		]),
		user && user.userAuthority === 'ADMIN' ?
		getItem(<Link to="/AdminPage">管理者</Link>, '管理者', <ToolOutlined />) : null
	], [user]);

	const selectedKey = useMemo(() => {
		const path = curLocation.pathname;
		if (path === '/') return '主頁';
		if (path === '/Contact') return '聯絡';
		if (path === '/About') return '關於';
		if (path === '/Login') return '登入';
		if (path === '/Register') return '註冊';
		if (path === '/UserPage') return '使用者';
		if (path === '/AdminPage') return '管理者';
		return '主頁';
	}, [curLocation.pathname]);

	return (
		<Layout style={{ minHeight: '100vh' }}>
			<Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
				<div className="demo-logo-vertical" />
				<Menu selectedKeys={[selectedKey]} mode="inline" items={items} theme="dark" />
			</Sider>
			<Layout>
				<Content style={{ margin: '0 16px' }}>
					<Breadcrumb style={{ margin: '16px 0' }} items={[
						{ title: <HomeOutlined /> },
						{ title: '程式單元歷程管理系統' },
						{ title: selectedKey }
					]} />
					<div style={{ padding: 24, minHeight: 360, background: colorBgContainer, borderRadius: borderRadiusLG }}>
						<Outlet />
					</div>
				</Content>
				<Footer style={{ textAlign: 'center' }}>WuKunWei ©{new Date().getFullYear()} Created by KunWeiWu</Footer>
			</Layout>
		</Layout>
	);
}

MainLayout.propTypes = {
	user: PropTypes.shape({
		userId: PropTypes.number.isRequired,
		userName: PropTypes.string.isRequired,
		userEmail: PropTypes.string.isRequired,
		userAccount: PropTypes.string.isRequired,
		userPassword: PropTypes.string.isRequired,
		userAuthority: PropTypes.string.isRequired
	}).isRequired
};

export default MainLayout;
