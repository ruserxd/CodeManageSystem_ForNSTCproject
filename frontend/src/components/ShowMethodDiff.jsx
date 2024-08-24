import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import api from "../api/axiosConfig";

function ShowMethodDiff() {
  const { "*": urlParam } = useParams();

  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const getTheMethodDiff = async () => {
      try {
        const response = await api.post(
          `/api/fetch-repo/categorize`,
          new URLSearchParams({ Path: urlParam })
        );
        setData(response.data);
        console.log("response.data", response.data);
      } catch (error) {
        setError(error);
        console.error("Error during fetch:", error);
      }
    };
    if (urlParam) {
      getTheMethodDiff();
    }
  }, [urlParam]);

  if (error) {
    return <div>Error: {error.message}</div>;
  }
  
  /*src/cloneCode/JavaSpringBootLearning*/
  return (
    <div>
      <h1>{urlParam.substring(urlParam.lastIndexOf("/") + 1)} 差異資訊: </h1>
      {data &&
        data.map((item) => (
          <div key={item.filePath}>
            <h2>檔案名稱: {item.fileName}</h2>
            <h2>檔案路徑: {item.filePath}</h2>
            <h2>方法:</h2>
            <div className="method">
              {/*把物件轉成創立成一個新的陣列*/}
              {Object.entries(item.methods).map(([methodName, diffs], methodIndex) => (
                  <div key={methodIndex}>
                    <h4>方法名稱: {methodName}</h4>
                    {diffs.map((diff, diffIndex) => (
                      <div key={diffIndex}>
                        <h4>作者: {diff.author}</h4>
                        <h4>Email: {diff.authorEmail}</h4>
                        <h4>commit訊息: {diff.commitMessage}</h4>
                        <h4>
                          timestamp:{" "}
                          {new Date(diff.timestamp * 1000).toLocaleString()}
                        </h4>
                        <h4>
                          commitTime:{" "}
                          {new Date(diff.commitTime).toLocaleString()}
                        </h4>
                        <pre>diff資訊: {diff.diffCode}</pre>
                      </div>
                    ))}
                  </div>
                )
              )}
            </div>
          </div>
        ))}
    </div>
  );
}
export default ShowMethodDiff;
