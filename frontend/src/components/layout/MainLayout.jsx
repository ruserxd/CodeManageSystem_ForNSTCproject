import PropTypes from "prop-types";
import { useState, useMemo } from "react";
import { Link, Outlet, useLocation } from "react-router-dom";
import {
  DesktopOutlined,
  PieChartOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Breadcrumb, Layout, Menu, theme } from "antd";

const { Header, Content, Footer, Sider } = Layout;

function getItem(label, key, icon, children) {
  return {
    key,
    icon,
    children,
    label,
  };
}

function MainLayout({ user }) {
  // 以下套用 antD
  const [collapsed, setCollapsed] = useState(false);
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  // 當前的網址
  const curLocation = useLocation();

  // 避免重新渲染太多元件 userMemo
  const items = useMemo(
    () => [
      getItem(<Link to="/">主頁</Link>, "1", <PieChartOutlined />),
      getItem("關於", "sub1", <DesktopOutlined />, [
        getItem(<Link to="/Contact">聯絡</Link>, "2"),
        getItem(<Link to="/About">關於</Link>, "3"),
      ]),
      user
        ? getItem(<Link to="/UserPage">使用者</Link>, "user", <UserOutlined />)
        : getItem("使用者", "sub2", <UserOutlined />, [
            getItem(<Link to="/Login">登入</Link>, "4"),
            getItem(<Link to="/Register">註冊</Link>, "5"),
          ]),
      getItem(<Link to="/test">test</Link>, "test"),
    ],
    [user]
  );

  // 根據當前路徑設置選中的菜單項
  const selectedKey = useMemo(() => {
    const path = curLocation.pathname;
    if (path === "/") return "1";
    if (path === "/Contact") return "2";
    if (path === "/About") return "3";
    if (path === "/Login") return "4";
    if (path === "/Register") return "5";
    if (path === "/UserPage") return "user";
    if (path === "/test") return "test";
    return "1"; // 默認選中首頁
  }, [curLocation.pathname]);

  return (
    <Layout
      style={{
        minHeight: "100vh",
      }}
    >
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={(value) => setCollapsed(value)}
      >
        <div className="demo-logo-vertical" />
        <Menu
          selectedKeys={[selectedKey]}
          mode="inline"
          items={items}
          theme="dark"
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: 0,
            background: colorBgContainer,
          }}
        />
        <Content
          style={{
            margin: "0 16px",
          }}
        >
          <Breadcrumb
            style={{
              margin: "16px 0",
            }}
            items={[{ title: "就這樣" }, { title: "小吳" }]}
          />
          <div
            style={{
              padding: 24,
              minHeight: 360,
              background: colorBgContainer,
              borderRadius: borderRadiusLG,
            }}
          >
            <Outlet />
          </div>
        </Content>

        {/* 底部 */}
        <Footer
          style={{
            textAlign: "center",
          }}
        >
          WuKunWei ©{new Date().getFullYear()} Created by KunWeiWu
        </Footer>
      </Layout>
    </Layout>
  );
}

// user 可能為空因此未設置 required
MainLayout.propTypes = {
  user: PropTypes.shape({
    success: PropTypes.bool.isRequired,
    message: PropTypes.string.isRequired,
    myUser: PropTypes.shape({
      user_id: PropTypes.number.isRequired,
      userName: PropTypes.string.isRequired,
      userEmail: PropTypes.string.isRequired,
      userAccount: PropTypes.string.isRequired,
      userPassword: PropTypes.string.isRequired,
      userAuthority: PropTypes.string.isRequired,
    }).isRequired,
  }),
};
export default MainLayout;
