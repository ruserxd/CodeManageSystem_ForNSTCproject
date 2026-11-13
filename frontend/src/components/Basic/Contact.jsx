import { AiFillGithub } from 'react-icons/ai';
import { MdEmail, MdEmojiPeople } from 'react-icons/md';

function Contact() {
	return (
		<div>
			<dl>
				<dt>
					Author
					<MdEmojiPeople />:
				</dt>
				<dd>
					吳堃瑋
					<br />
					目前就讀於陽明交通大學資訊科學與工程研究所
				</dd>
				<dt>
					Mail
					<MdEmail />:
				</dt>
				<dd>
					<a href="mailto:kunweiwu2004@gmail.com">kunweiwu2004@gmail.com</a>
				</dd>
				<dt>
					GitHub
					<AiFillGithub />:
				</dt>
				<dd>
					<a href="https://github.com/ruserxd">ruserxd</a>
					<br />
					專案原始碼：
					<a href="https://github.com/ruserxd/CodeManageSystem_ForNSTCproject">CodeManageSystem_ForNSTCproject</a>
				</dd>
			</dl>
		</div>
	);
}

export default Contact;
