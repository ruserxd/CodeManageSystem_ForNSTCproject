import { Typography } from 'antd';

const { Title, Paragraph } = Typography;

function About() {
	return (
		<Typography>
			<Title level={2}>關於 MethodTracker</Title>
			<Paragraph>
				Git 的 diff
				以「檔案」和「行」為單位，當一個檔案裡有很多方法時，很難看出某個方法本身經歷了哪些修改。MethodTracker
				把 Java 程式碼拆成一個個方法，讓你直接看到單一方法在各次 commit 之間的變化歷程。
			</Paragraph>

			<Title level={3}>使用流程</Title>
			<ol>
				<li>
					登入後，在「使用者」頁面輸入公開的 Git repo 網址（訪客帳號只能瀏覽已分析好的範例專案）。
				</li>
				<li>
					系統 clone 該 repo，從最新的 commit 往回逐一比對相鄰的兩個 commit；單次分析的 commit
					數有上限，超過的較舊 commit 不會納入。
				</li>
				<li>每個 .java 檔會被解析成方法，並記錄每個方法在各次 commit 的差異。</li>
				<li>在專案列表中選擇檔案與方法，即可逐次檢視該方法的 diff。</li>
			</ol>

			<Title level={3}>目前的限制</Title>
			<ul>
				<li>只分析 .java 檔。</li>
				<li>一般帳號單次最多分析 500 個 commit，更早的 commit 不會納入。</li>
				<li>每天的分析次數有上限。</li>
				<li>commit 很多的 repo 第一次分析會比較久，頁面可能先顯示逾時，但分析仍在背景進行。</li>
			</ul>

			<Title level={3}>技術架構</Title>
			<ul>
				<li>前端：React、Vite、Ant Design</li>
				<li>後端：Spring Boot、JGit、JavaParser、java-diff-utils</li>
				<li>資料庫：MySQL</li>
				<li>部署：Docker Compose、Nginx、CloudFlare</li>
			</ul>

			<Title level={3}>專案背景</Title>
			<Paragraph>本專案源自國科會大專學生研究計畫（已結案），2025 年台灣軟體工程研討會（TCSE 2025） 的展示論文（Demo Paper），目前持續開發中。</Paragraph>
		</Typography>
	);
}

export default About;
