import React, { useState } from 'react';
import EnterUrl from './components/EnterUrl';
import Result from './components/Result';

function App() {
  const [currentPage, setCurrentPage] = useState('enter');
  const [code, setCode] = useState('');
  const [diffs, setDiffs] = useState([]);

  const handleFetchData = async (url) => {
    try {
      console.log('Submitting URL:', url); // Debugging log
      const response = await fetch('http://localhost:8080/api/fetch-repo', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({ url }),
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Received data:', data); // Debugging log
        setCode(data.code);
        setDiffs(data.diffs);
        setCurrentPage('result');
      } else {
        console.error('Error fetching data:', response.statusText);
      }
    } catch (error) {
      console.error('Error during fetch:', error);
    }
  };

  return (
      <div>
        {currentPage === 'enter' ? (
            <EnterUrl onSubmit={handleFetchData} />
        ) : (
            <Result code={code} diffs={diffs} />
        )}
      </div>
  );
}

export default App;
