// src/components/HighlightedCode.js

import React from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'; // 使用 Prism 风格系列
import { vs } from 'react-syntax-highlighter/dist/esm/styles/prism'; // 选择一个可用的高对比度主题

const HighlightedCode = ({ language, codeString }) => (
    <SyntaxHighlighter language={language} style={vs}>
        {codeString}
    </SyntaxHighlighter>
);

export default HighlightedCode;
