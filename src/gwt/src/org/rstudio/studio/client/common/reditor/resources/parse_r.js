var RParser = Editor.Parser = (function() {

   var INDENT_WIDTH = 2;

   // from https://svn.r-project.org/R/trunk/src/main/gram.y
   var keywords = ["NULL", "NA", "TRUE", "FALSE", "T", "F", "Inf", "NaN",
      "NA_integer_", "NA_real_", "NA_character_", "NA_complex_", "function",
      "while", "repeat", "for", "if", "in", "else", "next",
      "break", "..."];

   function parseR(source) {
      // can be null, "'", or '"'
      var activeQuotChar = null;

      function tokenizeR(source)
      {
         var ch = source.next();

         // Quoted strings
         if (ch == '"' || ch == "'")
         {
            if (activeQuotChar == null)
               activeQuotChar = ch;
            else if (activeQuotChar == ch)
               activeQuotChar = null;

            // The quote is always considered part of the quoted string

            var start = activeQuotChar != null;
            var single = ch == "'";
            return "str"
                  + "-" + (start ? "start" : "end")
                  + "-" + (single ? "single" : "double");
         }
         else if (activeQuotChar != null)
         {
            // Test cases:
            // "\\" "test" (should be two separate strings)
            // "foo\[ENTER]test" (should be one string)
            // "test\"test" (should be one string)

            var escaping = ch == '\\';
            
            while (!source.endOfLine())
            {
               // Backslash starts an escape sequence. Note that escape sequences
               // can be longer than two characters, but we only care about ones
               // that happen to be two characters (\" and \').

               ch = source.peek();
               if (!escaping && (ch == '"' || ch == "'"))
                  break;

               if (escaping)
                  escaping = false;
               else if (ch == '\\')
                  escaping = true;

               source.next();
            }
            return "str";
         }

         // Comment
         if (ch == '#')
         {
            while (!source.endOfLine())
            {
               source.next();
            }
            return "comment";
         }

         // Numbers
         if (/[0-9]/.test(ch) || (ch == '.' && source.matches(/[0-9]/)))
         {
            /* NOTE: This is not quite correct. + and - are only part of a
               number when they are immediately preceded by [eE]. Otherwise,
               they should be treated as operators. */
            source.nextWhileMatches(/[0-9xXa-fA-F+\-L.i]/);
            return "number";
         }

         // Identifiers and Reserved Words
         /* NOTE: We do not handle accented letters in Western European
            locales--R uses isalnum(c) which can be locale specific */
         if (/[a-zA-Z.]/.test(ch))
         {
            var id = "";
            id = id + ch;
            while (source.matches(/[a-zA-Z0-9._]/))
               id = id + source.next();

            var i = keywords.length;
            while (--i >= 0)
               if (keywords[i] == id)
                  return "reserved";

            return "identifier";
         }
         
         if (ch == '`')
         {
            source.nextWhileMatches(/[^`\n]/);
            if (source.peek() == '`')
               source.next();
            return "identifier";
         }

         // Brackets
         if (/[\[\]\{\}\(\)]/.test(ch))
         {
            // double-brackets
            if ((ch == '[' || ch == ']') && source.peek() == ch)
               source.next();
            return "bracket";
         }

         // Do we need special case for ..., ..1, ..2, etc.??

         // Operators
         if (/[+\-*\/^><!&|~$:=]/.test(ch))
            return "operator";

         // User-defined infix operators
         if (ch == '%')
         {
            source.nextWhileMatches(/[^%\n]/);
            if (source.peek() == '%')
               source.next();
            return "operator";
         }

         return "text";
      }
      function indentTo(n) {
         var compactIf = compactIfInEffect;
         return function(nextToken, currIndent, direction) {
            if (direction == null) {
               if (compactIf && nextToken == "{")
                  return n - INDENT_WIDTH;
               else if (nextToken == "}")
                  return n - INDENT_WIDTH;
               else
                  return n;
            }
            else if (direction)
               return currIndent + INDENT_WIDTH;
            else
               return currIndent - INDENT_WIDTH;
         }
      }
      source = tokenizer(source, tokenizeR);
      var space = 0;
      var currentLine = [];
      var compactIfInEffect = false;

      function adjustSpaceForLineEnd() {
         compactIfInEffect = false;
         return adjustSpaceForCompactIfStatement()
            || adjustSpaceForBraces();
      }

      function adjustSpaceForCompactIfStatement() {
         var STATE_START = 0;        // at beginning of line
         var STATE_IF = 1;           // saw "if"
         var STATE_PARENS = 2;       // saw "("
         var STATE_PARENS_ENDED = 3; // saw final ")"
         var STATE_ELSE = 4;         // saw "else" at beginning of line
         var state = STATE_START;
         var parenCount = 0;

         for (var i = 0; i < currentLine.length; i++) {
            var tok = currentLine[i];
            if (tok.type == "whitespace" || tok.type == "comment")
               continue;

            type = tok.type;
            val = tok.value.replace(/(^\s+)|(\s+$)/g, '');

            if (state == STATE_START) {
               if (type == "reserved" && val == "if")
                  state = STATE_IF;
               else if (type == "reserved" && val == "else")
                  state = STATE_ELSE;
               else
                  return false; // this isn't an if statement
            }
            else if (state == STATE_IF) {
               if (type == "bracket" && val == "(") {
                  state = STATE_PARENS;
                  parenCount++;
               }
               else
                  return false; // if wasn't followed by a parenthesized expr
            }
            else if (state == STATE_ELSE) {
               if (type == "reserved" && val == "if")
                  state = STATE_IF;
               else
                  return false;
            }
            else if (state == STATE_PARENS) {
               if (type == "bracket") {
                  if (val == "(")
                     parenCount++;
                  else if (val == ")") {
                     parenCount--;
                     if (parenCount == 0)
                        state = STATE_PARENS_ENDED;
                  }
               }
            }
            else if (state == STATE_PARENS_ENDED) {
               return false; // the expression was on the same line as the if
            }
         }

         if (state == STATE_PARENS) {
            // the if clause itself spans multiple lines
            space += INDENT_WIDTH;
            return true;
         }
         else if (state == STATE_PARENS_ENDED || state == STATE_ELSE) {
            // If we got here, it is indeed a compact if
            space += INDENT_WIDTH;
            compactIfInEffect = true;
            return true;
         }

         return false;
      }

      function adjustSpaceForBraces() {
         var braceCount = 0;
         for (var i = 0; i < currentLine.length; i++) {
            var tok = currentLine[i];
            if (tok.type == "whitespace" || tok.type == "comment")
               continue;

            if (tok.type == "bracket") {
               if (tok.value == "{")
                  braceCount++;
               else if (tok.value == "}")
                  braceCount--;
            }
         }
         if (braceCount > 0)
            space += (INDENT_WIDTH*braceCount);
      }

      var iter = {
         next: function() {
            var tok = source.next();
            currentLine.push(tok);
            
            if (tok.type == "whitespace") {
               if (tok.value == "\n") {
                  adjustSpaceForLineEnd();
                  tok.indentation = indentTo(space);
                  space = 0;
                  currentLine = [];
               }
               else
                  space = tok.value.length;
            }
            return tok;
         },
         copy: function() {
            var _space = space;
            var _activeQuotChar = activeQuotChar;
            var _currentLine = currentLine.slice(0);
            return function(_source) {
               space = _space;
               activeQuotChar = _activeQuotChar;
               currentLine = _currentLine.slice(0);
               source = tokenizer(_source, tokenizeR);
               return iter;
            };
         }
      };

      return iter;
   }
   return {
      make: parseR,
      electricChars: "{}",
      firstIndentation: function(chars, current, direction) {
         if (direction == null)
            return 0;
         else if (direction)
            return current + INDENT_WIDTH;
         else
            return current - INDENT_WIDTH;
      }};
})();
