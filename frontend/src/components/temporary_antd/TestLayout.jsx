import React, { useState } from "react";
import { Link, Outlet } from "react-router-dom";

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
const items = [
  getItem(<Link to="/test">首頁</Link>, "1", <PieChartOutlined />),
  getItem("關於", "sub1", <DesktopOutlined />, [
    getItem(<Link to="/test/Contact">聯絡</Link>, "3"),
    getItem(<Link to="/test/About">關於</Link>, "4"),
  ]),
  getItem("使用者", "sub2", <UserOutlined />, [
    getItem(<Link to="/test/Login">登入</Link>, "5"),
    getItem(<Link to="/test/Register">註冊</Link>, "6"),
  ]),
];

const TestLayout = () => {
  const [collapsed, setCollapsed] = useState(false);
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();
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
          theme="light"
          defaultSelectedKeys={["1"]}
          mode="inline"
          items={items}
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
          ></Breadcrumb>
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
};
export default TestLayout;
