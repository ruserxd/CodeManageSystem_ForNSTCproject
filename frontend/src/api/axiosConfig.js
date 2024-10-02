import axios from 'axios';

export default axios.create({
    baseURL: 'https://8012-123-110-54-203.ngrok-free.app',
    headers: {"ngrok-skip-browser-warning":"true"}
})