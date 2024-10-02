import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { dracula } from "react-syntax-highlighter/dist/esm/styles/prism";

const HighlightedCode = ({ language, codeString }) => (
  <SyntaxHighlighter language={language} style={dracula}>
    {codeString}
  </SyntaxHighlighter>
);

export default HighlightedCode;
