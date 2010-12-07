/*
Taken from the LaTeX Lab project: http://latexlab.org
Retrieved on 4/2/2010 from:
  http://latex-lab.googlecode.com/svn-history/r22/trunk/latexlab/eclipse/war/codemirror/js/parselatex.js
*/

var LatexParser = Editor.Parser = (function() {
	var tokenizeLatex = (function() {
    function normal(source, setState) {
    	var ch = source.next();
	    if (ch == "\\") {
		     // if (source.peek() == '(') {
			   //   setState(inMath('\\)'));
			   //   return null;
			   // } 
			   // else if (source.peek() == '[') {
			   //   setState(inMath('\\]'));
			   //   return null;
			   // }
			   // else {
	         source.nextWhileMatches(/[a-zA-Z0-9\$\%]/);
	         return "latex-command";
	       // }
	    }
      else if (ch == "$") {
	      // if (source.peek() == '$') 
	      //   setState(inMath('$$'));
	      // else
             setState(inMath('$'));
  
        return null;
      }
	    else if (ch == "%") {
	      source.nextWhileMatches(/[^\n]/);
	      return 'latex-comment';
	    }
	    else if (/[{}]/.test(ch)) {
	      return "latex-braces";
	    }
	    else {
		    source.nextWhileMatches(/[^\\\$\%\n{}]/);
	      return "latex-text";
	    }
	  }
	
	  function inMath(delimiter) {
      return function(source, setState) {
        var escaped = false;
        while (!source.endOfLine()) {
          if(source.lookAhead(delimiter, false) && !escaped) {
            source.lookAhead(delimiter, true)
            break;
          }
          var ch = source.next();
          escaped = !escaped && ch == "\\";
        }
        if (!escaped)
          setState(normal);
        return "latex-inline-math";
      };
    }
	
	  return function(source, startState) {
      return tokenizer(source, startState || normal);
    };
	})();

  function parseLatex(source) {
    function indentTo(n) {return function() {return n;}}
	  source = tokenizeLatex(source);
    var space = 0;

    var iter = {
      next: function() {
        var tok = source.next();
        if (tok.type == "whitespace") {
          if (tok.value == "\n") tok.indentation = indentTo(space);
          else space = tok.value.length;
        }
        return tok;
      },

      copy: function() {
        var _space = space;
        return function(_source) {
          space = _space;
          source = tokenizeLatex(_source);
          return iter;
        };
      }
    };
    return iter;
  }

  return {make: parseLatex, electricChars: "}"};
})();