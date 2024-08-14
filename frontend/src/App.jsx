import React, {useState} from 'react';
import EnterUrl from './components/EnterUrl';
import Result from './components/Result';

function App() {
    // 運用 useState 來管理目前的頁面狀態
    const [currentPage, setCurrentPage] = useState('enter');
    const [code, setCode] = useState('');
    const [diffs, setDiffs] = useState([]);

    // 處理 fetch 資料的函式
    const handleFetchData = async (url) => {
        try {
            // 接受到的是 URL
            console.log('Submitting URL:', url);
            const response = await fetch('http://localhost:8080/api/fetch-repo', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({url}),
            });

            if (response.ok) {
                const data = await response.json();
                console.log('Received data:', data);
                setCode(data.code);
                setDiffs(data.diffs);
                setCurrentPage('result');
            } else {
                alert('Error fetching data. Please check the URL and try again.');
                console.error('Error fetching data:', response.statusText);
            }
        } catch (error) {
            alert('Error during fetch. Please check the console for more information.');
            console.error('Error during fetch:', error);
        }
    };

    // 回傳 JSX 元件
    return (
        <div>
            {currentPage === 'enter' ? (
                // 透過 props 將 handleFetchData 傳遞給 EnterUrl 元件
                <EnterUrl onSubmit={handleFetchData}/>
            ) : (
                // 透過 props 將 code 和 diffs 傳遞給 Result 元件
                <Result code={code} diffs={diffs}/>
            )}
        </div>
    );
}

export default App;
