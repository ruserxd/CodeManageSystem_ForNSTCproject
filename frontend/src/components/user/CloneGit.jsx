import { useState } from 'react';
import api from '../../api/axiosConfig';

function CloneGit() {
	const [url, setUrl] = useState('');

	const handleFetchData = async (url) => {
		try {
			const response = await api.post('/api/fetch-repo', new URLSearchParams({ url }));

			const { status, path } = response.data;

			if (status === 'CLONE_SUCCESS' || status === 'PULL_SUCCESS') {
				alert(`Repository processed successfully: ${path}`);
				window.location.reload();
			} else if (status === 'ANALYSIS_FAILED') {
				alert(`Repository cloned but no files were analyzed`);
			} else if (status === 'PULL_FAILED' || status === 'CLONE_FAILED') {
				alert(`Failed to process repository`);
			} else {
				alert(`Unexpected status: ${status}`);
			}
		} catch (error) {
			alert('Error during fetch. Please check the console for more information.');
			console.error('Error during fetch:', error);
		}
	};

	const handleClick = (event) => {
		event.preventDefault();
		if (url.trim()) {
			console.log('Submitting URL:', url);
			handleFetchData(url);
		} else {
			alert('URL is empty');
		}
	};

	return (
		<div>
			<label htmlFor="url">URL:</label>
			<input
				type="text"
				id="url"
				name="url"
				value={url}
				onChange={(e) => setUrl(e.target.value)}
				placeholder="GitHub Repo Url"
				required
			/>
			<button onClick={handleClick}>Fetch Repo</button>
		</div>
	);
}

export default CloneGit;
