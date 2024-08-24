import React, { useState } from "react";
import api from "../api/axiosConfig";

function CloneGit() {
  const [url, setUrl] = useState("");

  const handleFetchData = async (url) => {
    try {
      /*接受到的是 URL*/
      console.log("Submitting URL:", url);
      await api.post("/api/fetch-repo", new URLSearchParams({ url }));
      alert("Data fetched successfully");
    } catch (error) {
      alert(
        "Error during fetch. Please check the console for more information."
      );
      console.error("Error during fetch:", error);
    }
  };

  const handleClick = (event) => {
    event.preventDefault();
    if (url.trim()) {
      console.log("Submitting URL:", url);
      handleFetchData(url);
    } else {
      console.warn("URL is empty");
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
        required
      />
      <button onClick={handleClick}>Fetch Repo</button>
    </div>
  );
}

export default CloneGit;
