import PropTypes from "prop-types";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { dracula } from "react-syntax-highlighter/dist/esm/styles/prism";

const HighlightedCode = ({ language, codeString }) => (
  <SyntaxHighlighter language={language} style={dracula}>
    {codeString}
  </SyntaxHighlighter>
);

HighlightedCode.propTypes = {
  language: PropTypes.string,
  codeString: PropTypes.string,
};
export default HighlightedCode;
