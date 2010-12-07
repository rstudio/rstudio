var SweaveParser = Editor.Parser = (function() {
  if (!(RParser && LatexParser))
    throw new Error("R and LaTeX parsers must be loaded for Sweave mixed mode to work.");

  function parseMixed(stream) {
    var latexParser = LatexParser.make(stream), localParser = null;
    var latexCmd = null;
    var atLineStart = true;
    var iter = {next: top, copy: copy};

    function top() {
      var token = latexParser.next();
      if (/^\<\<.*\>\>=/.test(token.content)) {
        iter.next = local(RParser, '@', true);
        latexCmd = null;
      }
      else if (token.style === "latex-command") {
        latexCmd = token.content;
      }
      else if (!(latexCmd === null)) {
        latexCmd += token.content;
        if (token.style === "latex-braces" && token.content === "{") {
          // \Sexpr{foo(bar, baz)}
          if (/^\\Sexpr\{$/.test(latexCmd)) {
            iter.next = local(RParser, "}", false);
            latexCmd = null;
          }
        }
        else if (token.style === "latex-braces" && token.content === "}") {
          // \begin{Scode}foo(bar, baz)\end{Scode}
          if (/^\\begin\{\s*Scode\s*}$/.test(latexCmd)) {
             iter.next = local(RParser, "\\end{Scode}", false);
          }
          latexCmd = null;
        }
      }
      return token;
    }
    function local(parser, tag, tagIsLineStart) {
      localParser = parser.make(stream);
      atLineStart = false;
      return function() {
        if (stream.lookAhead(tag, false, false, false)) {
          if (!tagIsLineStart || atLineStart) {
            localParser = null;
            iter.next = top;
            return top();
          }
        }

        var token = localParser.next();
        atLineStart = token.content == "\n";
        return token;
      };
    }

    function copy() {
      var _latex = latexParser.copy(), _local = localParser && localParser.copy(),
          _next = iter.next, _latexCmd = latexCmd, _atLineStart = atLineStart;
      return function(_stream) {
        stream = _stream;
        latexParser = _latex(_stream);
        localParser = _local && _local(_stream);
        iter.next = _next;
        latexCmd = _latexCmd;
        atLineStart = _atLineStart;
        return iter;
      };
    }
    return iter;
  }

  return {make: parseMixed, electricChars: "{}"};

})();