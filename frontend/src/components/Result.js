// src/components/Result.js

import React, { useEffect } from 'react'; // 确保导入 useEffect
import '../styles/result.css'; // 你自己的样式文件
import HighlightedCode from './HighlightedCode'; // 引入刚刚创建的组件

function Result({ code, diffs }) {
    useEffect(() => {
        console.log('Diffs:', diffs);
    }, [diffs]);

    return (
        <div>
            <h2>Java 程式碼</h2>
            <HighlightedCode language="java" codeString={code} />

            <h2>Commit 差異</h2>
            {diffs.map((diff, index) => (
                <div key={index}>
                    <p>Author: {diff.author}</p>
                    <p>File: {diff.filename}</p>
                    <p>CommitID: {diff.commitId}</p>
                    <p>Time: {new Date(diff.timestamp * 1000).toLocaleString()}</p>
                    <h4>原始程式碼:</h4>
                    <HighlightedCode language="java" codeString={diff.originalCode} />
                    <h4>修改後的程式碼:</h4>
                    <HighlightedCode language="diff" codeString={diff.diff} />
                </div>
            ))}
        </div>
    );
}

export default Result;
