import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import api from "../api/axiosConfig";
import HighlightedCode from "./HighlightedCode";

function ShowMethodDiff() {
  const { "*": urlParam } = useParams();

  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [projectName, setProjectName] = useState(null);

  useEffect(() => {
    const getTheMethodDiff = async () => {
      try {
        const result = await api.post(
          `/api/fetch-repo/categorize`,
          new URLSearchParams({ Path: urlParam })
        );
        setData(result.data);
        console.log("response.data\n", result.data);
      } catch (error) {
        setError(error);
        console.error("Error during fetch: ", error);
      }
    };
    if (urlParam) {
      setProjectName(urlParam.substring(urlParam.lastIndexOf("/") + 1));
      getTheMethodDiff();
    }
  }, [urlParam]);

  if (error) {
    return <div>Error: {error.message}</div>;
  }

  return (
    <div>
      <h1>{projectName} 的方法差異資訊 </h1>
      {data &&
        data.map((item) => (
          <div key={item.filePath}>
            <h2>檔案名稱: {item.fileName}</h2>
            <h2>檔案路徑: {item.filePath}</h2>
            <h2>方法:</h2>
            <div className="method">
              {/*把物件轉成創立成一個新的陣列*/}
              {Object.entries(item.methods).map(
                ([methodName, diffs], methodIndex) => (
                  <div key={methodIndex}>
                    <h3>方法名稱: {methodName}</h3>
                    {diffs.map((diff, diffIndex) => (
                      <div key={diffIndex}>
                        <h4>
                          commitTime:{" "}
                          {new Date(diff.commitTime).toLocaleString()}
                        </h4>
                        <h4>commit訊息: {diff.commitMessage}</h4>
                        <h4>作者: {diff.author}</h4>
                        <h4>Email: {diff.authorEmail}</h4>
                        <HighlightedCode
                          language="diff"
                          codeString={diff.diffCode}
                        />
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
