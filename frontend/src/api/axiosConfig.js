import axios from 'axios';

export default axios.create({
    baseURL:'https://fdce-1-174-102-212.ngrok-free.app',
    headers: {"ngrok-skip-browser-warning":"true"}
})