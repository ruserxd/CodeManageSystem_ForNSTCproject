import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vs } from "react-syntax-highlighter/dist/esm/styles/prism";

const HighlightedCode = ({ language, codeString }) => (
  <SyntaxHighlighter language={language} style={vs}>
    {codeString}
  </SyntaxHighlighter>
);

export default HighlightedCode;
