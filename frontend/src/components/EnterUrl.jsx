import React, {useState} from 'react';
import '../styles/enter.css';

function EnterUrl({onSubmit}) {
    const [url, setUrl] = useState('');

    const handleSubmit = (event) => {
        event.preventDefault();
        if (url.trim()) {
            console.log('Submitting URL:', url); // Debugging log
            onSubmit(url);
        } else {
            console.warn('URL is empty');
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            <label htmlFor="url">URL:</label>
            <input
                type="text"
                id="url"
                name="url"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                required
            />
            <button type="submit">Submit</button>
        </form>
    );
}

export default EnterUrl;
