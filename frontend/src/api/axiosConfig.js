import axios from 'axios';

export default axios.create({
    baseURL:'https://283b-1-174-110-224.ngrok-free.app',
    headers: {"ngrok-skip-browser-warning":"true"}
})