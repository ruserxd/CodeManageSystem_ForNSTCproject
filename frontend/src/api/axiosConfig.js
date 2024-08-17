import axios from 'axios';

export default axios.create({
    baseURL:'https://78d1-1-174-117-127.ngrok-free.app',
    headers: {"ngrok-skip-browser-warning":"true"}
})