import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import api from "../../api/axiosConfig";
import HighlightedCode from "./HighlightedCode";

function ShowMethodDiff() {
  const { "*": urlParam } = useParams();

  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [projectName, setProjectName] = useState(null);

  useEffect(() => {
    const getTheMethodDiff = async () => {
      try {
        // 等待 post 成立後，再執行下一行
        const result = await api.post(
          `/api/getData`,
          new URLSearchParams({ ProjectName: urlParam })
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
            {item.methods &&
              item.methods.map((method, Index) => (
                <div key={Index}>
                  <h2>方法 {method.methodName}</h2>
                  {method.diffInfoList &&
                    method.diffInfoList.map((diff, Index) => (
                      <div key={Index}>
                        <h4>Author: {diff.author}</h4>
                        <h4>AuthorEmail: {diff.authorEmail}</h4>
                        <h4>CommitMessage: {diff.commitMessage}</h4>
                        <h4>AuthorEmail: {diff.authorEmail}</h4>
                        <h4>CommitTime: {diff.commitTime}</h4>
                        <HighlightedCode
                          language="diff"
                          codeString={diff.diffCode}
                        />
                      </div>
                    ))}
                </div>
              ))}
          </div>
        ))}
    </div>
  );
}
export default ShowMethodDiff;
