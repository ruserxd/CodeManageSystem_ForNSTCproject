import React, {useEffect} from 'react';
import PropTypes from 'prop-types'; // Import PropTypes
import '../styles/result.css';
import HighlightedCode from './HighlightedCode';

function Result({code, diffs}) {
    useEffect(() => {
        console.log('Diffs:', diffs);
    }, [diffs]);

    return (
        <div>
            <h2>Java 程式碼</h2>
            <HighlightedCode language="java" codeString={code}/>

            <h2>Commit 差異</h2>
            {diffs.map((diff, index) => (
                <div key={index}>
                    <p>Author: {diff.author}</p>
                    <p>File: {diff.filename}</p>
                    <p>CommitID: {diff.commitId}</p>
                    <p>Time: {new Date(diff.timestamp * 1000).toLocaleString()}</p>
                    <h4>原始程式碼:</h4>
                    <HighlightedCode language="java" codeString={diff.originalCode}/>
                    <h4>新增的程式碼:</h4>
                    <HighlightedCode language="java" codeString={diff.addedLines}/>
                    <h4>刪減的程式碼:</h4>
                    <HighlightedCode language="java" codeString={diff.removedLines}/>
                    <h4>原本的diff</h4>
                    <HighlightedCode language="diff" codeString={diff.diff}/>
                </div>
            ))}
        </div>
    );
}

// 使用 PropTypes 進行型別檢查
Result.propTypes = {
    code: PropTypes.string.isRequired,
    diffs: PropTypes.arrayOf(PropTypes.shape({
        author: PropTypes.string.isRequired,
        filename: PropTypes.string.isRequired,
        commitId: PropTypes.string.isRequired,
        timestamp: PropTypes.number.isRequired,
        originalCode: PropTypes.string.isRequired,
        addedLines: PropTypes.string.isRequired,
        removedLines: PropTypes.string.isRequired,
        diff: PropTypes.string.isRequired,
    })).isRequired,
};

export default Result;
