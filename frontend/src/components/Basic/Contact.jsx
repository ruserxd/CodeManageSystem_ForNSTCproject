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
					目前就讀於逢甲大學_資訊工程
				</dd>
				<dt>
					Mail
					<MdEmail />:
				</dt>
				<dd>
					<a href="mailto:D1149580@o365.fcu.edu.tw">D1149580@o365.fcu.edu.tw</a>
				</dd>
				<dt>
					GitHub
					<AiFillGithub />:
				</dt>
				<dd>
					<a href="https://github.com/ruserxd">ruserxd</a>
				</dd>
			</dl>
		</div>
	);
}
export default Contact;
