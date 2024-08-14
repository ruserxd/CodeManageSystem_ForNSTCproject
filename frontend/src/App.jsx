import React, {useState} from 'react';
import EnterUrl from './components/EnterUrl';
import Result from './components/Result';
import api from './api/axiosConfig';
import { Route , Routes, useNavigate} from 'react-router-dom';

function App() {
    // 使用 useState 存獲取資料的狀態
    const [code, setCode] = useState('');
    const [diffs, setDiffs] = useState([]);
    const navigate = useNavigate();

    // 處理 fetch 資料  
    const handleFetchData = async (url) => {
        try {
            // 接受到的是 URL
            console.log('Submitting URL:', url);
            const response = await api.post('/api/fetch-repo',  new URLSearchParams({ url }));
            
            console.log('Received data:', response.data);
            setCode(response.data.code);
            setDiffs(response.data.diffs);
            
            navigate('/result');
        } catch (error) {
            alert('Error during fetch. Please check the console for more information.');
            console.error('Error during fetch:', error);
        }
    };

    // API
    return (
        <Routes>
            <Route path="/" element={<EnterUrl onSubmit={handleFetchData}/>}/>
            <Route path="/result" element={<Result code={code} diffs={diffs}/>}/>
        </Routes>
    );
}

export default App;
