define("mode/c_cpp_matching_brace_outdent", function(require, exports, module) {

var Range = require("ace/range").Range;

var CppTokenCursor = require("mode/token_cursor").CppTokenCursor;
var CppMatchingBraceOutdent = function(codeModel) {
   this.codeModel = codeModel;
};
var Utils = require("mode/utils");

// Allow the user to control various levels of outdenting if desired
var $outdentColon              = true; // : (initializer list)
var $outdentRightParen         = true; // )
var $outdentLeftBrace          = true; // {
var $outdentRightBrace         = true; // }
var $outdentRightBracket       = true; // ]
var $outdentRightArrow         = true; // >
var $alignDots                 = true; // .
var $alignEquals               = true; // int x = 1,
                                       //     y = 2;
var $alignStreamIn             = true; // >>
var $alignStreamOut            = true; // <<
var $alignClassAccessModifiers = true; // public: etc.
var $alignCase                 = true; // case 'a':

(function() {

   // Set the indent of the line at 'row' to the indentation at
   // 'rowFrom'. This operation is only performed if the indentation
   // of the lines at 'rowTo' and 'rowFrom' match.
   //
   // 'predicate' is an (optional) function taking the old and new
   // indents, and returning true or false -- this is used to ensure
   // outdenting only occurs when explicitly desired.
   this.setIndent = function(session, rowTo, rowFrom, extraIndent, predicate) {

      var doc = session.getDocument();
      extraIndent = typeof extraIndent === "string" ?
         extraIndent :
         "";

      var line = doc.$lines[rowTo];
      var prevLine = doc.$lines[rowFrom];

      var oldIndent = this.$getIndent(line);
      var newIndent = this.$getIndent(prevLine);

      if (typeof predicate !== "function" || predicate(oldIndent, newIndent)) {
         doc.replace(
            new Range(rowTo, 0, rowTo, oldIndent.length),
            newIndent + extraIndent
         );
      }
      
   };

   this.checkOutdent = function(state, line, input) {
      if (Utils.endsWith(state, "start")) {

         // private: / public: / protected
         // also class initializer lists
         if (input === ":") {
            return true;
         }

         // outdenting for lines starting with 'closers' and 'openers'
         // also preproc lines
         if (/^\s*[#\{\}\>\]\)<.:]/.test(input))
            return true;

         // outdenting for '='
         if (input === "=")
            return true;

      }

      // check for nudging of '/' to the left (?)
      if (Utils.endsWith(state, "comment")) {

         if (input == "/") {
            return true;
         }

      }

      return false;

   };

   this.escapeRegExp = function(string) {
      return string.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
   };

   this.alignStartToken = function(token, session, row, line, prevLine) {

      if (prevLine === null || line === null) return false;
      var regex = new RegExp("^\\s*" + this.escapeRegExp(token));
      if (regex.test(line)) {
         var index = prevLine.indexOf(token);
         if (index >= 0) {

            var doc = session.getDocument();
            var oldIndent = this.$getIndent(line);
            var newIndent = new Array(index + 1).join(" ");

            doc.replace(
               new Range(row, 0, row, oldIndent.length),
               newIndent
            );

            return true;
         }
      }
      return false;
   };

   this.alignEquals = function(session, row, line, prevLine) {

      if (prevLine === null || line === null) return false;
      var equalsIndex = line.indexOf("=");

      // Bail if there is more than one '=' on the line
      if (equalsIndex !== line.lastIndexOf("="))
         return false;
      
      var prevLineEqualsIndex = prevLine.indexOf("=");
      if (equalsIndex !== -1 && prevLineEqualsIndex !== -1 && /,\s*$/.test(prevLine))
      {
         var doc = session.getDocument();
         var oldIndent = this.$getIndent(line);
         if (oldIndent.length >= prevLineEqualsIndex)
            return false;

         var diff = prevLineEqualsIndex - equalsIndex;
         if (diff <= 0)
            return false;

         var newIndent = new Array(oldIndent.length + diff + 1).join(" ");

         doc.replace(
            new Range(row, 0, row, oldIndent.length),
            newIndent
         );

         return true;
         
      }

      return false;
      
   };

   this.autoOutdent = function(state, session, row) {

      var doc = session.doc;
      var line = doc.getLine(row);
      var prevLine = null;
      if (row > 0)
         prevLine = doc.getLine(row - 1);
      var indent = this.$getIndent(line);

      // Check for '<<', '.'alignment
      if ($alignStreamOut && this.alignStartToken("<<", session, row, line, prevLine))
         return;

      if ($alignStreamIn && this.alignStartToken(">>", session, row, line, prevLine))
         return;

      if ($alignDots && this.alignStartToken(".", session, row, line, prevLine))
         return;

      // if ($alignEquals && this.alignEquals(session, row, line, prevLine))
      //    return;

      // Outdent for a ':' places on its own line if it appears the
      // user is creating an initialization list for
      // a constructor, e.g.
      //
      //     SomeConstructor(int a,
      //                     int b)
      //         :
      //         ^
      //
      // We also perform a similar action for class inheritance, e.g.
      //
      //     class Foo
      //         :
      if ($outdentColon &&
          /^\s*:/.test(doc.getLine(row)) &&
          !/^\s*::/.test(doc.getLine(row))) {

         if (this.codeModel.$tokenUtils.$tokenizeUpToRow(row)) {

            var tokenCursor = new CppTokenCursor(this.codeModel.$tokens, row, 0);
            var rowToUse = -1;

            // First, handle constructors. Note that we need to first walk
            // over some keywords, e.g.
            //
            //     int foo(int a, int b) const noexcept()
            //         :
            //
            var clone = tokenCursor.cloneCursor();
            if (clone.peekBwd().currentValue() === ")" ||
                clone.peekBwd().currentType() === "keyword")
            {
               do {
                  
                  var type = clone.currentType();
                  var value = clone.currentValue();

                  // Stop conditions
                  if (type === "identifier" ||
                      value === ";")
                  {
                     rowToUse = clone.$row;
                     break;
                  }

                  // Chomp keywords
                  if (type === "keyword") {
                     continue;
                  }

                  // Walk over parens
                  clone.bwdToMatchingToken();
                  
               } while (clone.moveToPreviousToken());

            }

            // Otherwise, try walking over class inheritance
            else {
               tokenCursor.moveToPreviousToken();
               if (tokenCursor.bwdOverClassySpecifiers()) {
                  rowToUse = tokenCursor.$row;
               }
            }
            
            if (rowToUse >= 0) {
               this.setIndent(session, row, rowToUse, session.getTabString(),
                              function(oldIndent, newIndent) {
                                 return oldIndent === newIndent;
                              });
            }

         }
      }

      // Outdent for lines starting with a '>' if an associated matching
      // token can be found. This is intended for template contexts, e.g.
      //
      //     template <
      //         int RTYPE
      //     >
      //     ^
      //
      if ($outdentRightArrow &&
          /^\s*>/.test(line) &&
          !/^\s*>>/.test(line)) {

         var rowToUse = this.codeModel.getRowForMatchingEOLArrows(session, doc, row);
         if (rowToUse >= 0) {
            this.setIndent(session, row, rowToUse);
            return;
         }

         // TODO: Renable this block if we get better tokenization
         // (need to discover whether '<', '>' are operators or not)
         //
         // if (this.codeModel.$tokenUtils.$tokenizeUpToRow(row)) {
         //    var tokenCursor = new CppTokenCursor(this.codeModel.$tokens, row, 0);
         //    if (tokenCursor.bwdToMatchingArrow()) {
         //       this.setIndent(session, row, tokenCursor.$row);
         //    }
         // }
         
      }

      // Outdent for closing braces (to match the indentation of their
      // matched opening brace
      //
      // If the line on which the matching '{' was found is of
      // the form
      //
      //  foo) {
      //
      // then indent according to the location of the matching
      // '('
      //
      // This rule should apply to closing braces with semi-colons
      // or comments following as well.
      if ($outdentRightBrace && /^\s*\}/.test(line)) {

         var openBracketPos = session.findMatchingBracket({
            row: row,
            column: line.indexOf("}") + 1
         });

         if (openBracketPos !== null) {

            // If the open brace lies on its own line, match its indentation
            var openBracketLine =
                   doc.$lines[openBracketPos.row];

            if (/^\s*\{/.test(openBracketLine)) {
               this.setIndent(session, row, openBracketPos.row);
               return;
            }

            // Otherwise, try looking upwards to get an appropriate indentation
            var heuristicRow = this.codeModel.getRowForOpenBraceIndent(
               session,
               openBracketPos.row
            );

            if (heuristicRow >= 0) {
               this.setIndent(session, row, heuristicRow);
               return;
            }

         } 

      }

      if ($outdentRightParen) {

         var closingParenMatch = /^\s*\)/.exec(line);
         if (closingParenMatch) {
            var openParenPos = session.findMatchingBracket({
               row: row,
               column: line.indexOf(")") + 1
            });
            if (openParenPos) {
               this.setIndent(session, row, openParenPos.row);
               return;
            }
         }
      }

      if ($outdentRightBracket) {

         var closingBracketMatch = /^\s*\]/.exec(line);
         if (closingBracketMatch) {
            var openBracketPos = session.findMatchingBracket({
               row: row,
               column: line.indexOf("]") + 1
            });
            if (openBracketPos) {
               this.setIndent(session, row, openBracketPos.row);
               return;
            }
         }
      }
      
      // If we just typed 'public:', 'private:' or 'protected:',
      // we should outdent if possible. Do so by looking for the
      // enclosing 'class' scope.
      if ($alignClassAccessModifiers &&
          /^\s*public\s*:\s*$|^\s*private\s*:\s*$|^\s*protected\s*:\s*$/.test(line)) {

         // Find the associated open bracket.
         var openBracePos = session.$findOpeningBracket(
            "}",
            {
               row: row,
               column: line.length
            },
            new RegExp(/paren\.keyword\.operator/)
         );

         if (openBracePos) {
            // If this open brace is already associated with a class or struct,
            // step over all of those rows.
            var heuristicRow =
                   this.codeModel.getRowForOpenBraceIndent(session, openBracePos.row);

            if (heuristicRow >= 0) {
               this.setIndent(session, row, heuristicRow);
               return;
            } else {
               this.setIndent(session, row, openBracePos.row);
               return;
            }
         }
         
      }

      // Similar lookback for 'case foo:'.
      if ($alignCase &&
          (/^\s*case.+:/.test(line) || /^\s*default\s*:/.test(line)))
      {

         // Find the associated open bracket.
         var openBracePos = session.$findOpeningBracket(
            "}",
            {
               row: row,
               column: /(\S)/.exec(line).index + 1
            }
         );

         if (openBracePos) {
            var heuristicRow =
                   this.codeModel.getRowForOpenBraceIndent(session, openBracePos.row);

            if (heuristicRow >= 0) {
               this.setIndent(session, row, heuristicRow);
               return;
            } else {
               this.setIndent(session, row, openBracePos.row);
               return;
            }
         }
         
      }
      

      // If we just inserted a '{' on a new line to begin a class definition,
      // try looking up for the associated class statement.
      // We want to look back over the following common indentation styles:
      //
      // (1) class Foo
      //     : public A,
      //       public B,
      //       public C
      //
      // and
      //
      // (2) class Foo
      //     : public A
      //     , public B
      //     , public C
      //
      // We also design the rules to 'work' for initialization lists, e.g.
      //
      //    Foo()
      //    : foo_(foo),
      //      bar_(bar),
      //      baz_(baz)
      if ($outdentLeftBrace && /^\s*\{/.test(line)) {

         // Don't outdent if the previous line ends with a semicolon
         if (!/;\s*$/.test(prevLine)) {

            if (this.codeModel.$tokenUtils.$tokenizeUpToRow(row)) {

               var tokenCursor = new CppTokenCursor(this.codeModel.$tokens);
               tokenCursor.$row = row,
               tokenCursor.$offset = 0;

               if (tokenCursor.moveToPreviousToken()) {
                  if (tokenCursor.currentValue() === "=") {
                     return;
                  }
               }
            }

            var scopeRow = this.codeModel.getRowForOpenBraceIndent(
               session,
               row
            );

            if (scopeRow >= 0) {

               this.setIndent(session, row, scopeRow);
               return;
               
            }

         }

      }

      // For lines intended for the preprocessor, trim off the indentation.
      if (/^\s*#/.test(line))
      {
         var oldIndent = this.$getIndent(line);
         doc.replace(
            new Range(row, 0, row, oldIndent.length),
            ""
         );
         return;
      }

   };

   this.$getIndent = function(line) {
      var match = line.match(/^(\s+)/);
      if (match) {
         return match[1];
      }
      return "";
   };

}).call(CppMatchingBraceOutdent.prototype);

exports.CppMatchingBraceOutdent = CppMatchingBraceOutdent;

exports.getOutdentColon = function() { return $outdentColon; };
exports.setOutdentColon = function(x) { $outdentColon = x; };

exports.getOutdentRightParen = function() { return $outdentRightParen; };
exports.setOutdentRightParen = function(x) { $outdentRightParen = x; };

exports.getOutdentLeftBrace = function() { return $outdentLeftBrace; };
exports.setOutdentLeftBrace = function(x) { $outdentLeftBrace = x; };

exports.getOutdentRightBrace = function() { return $outdentRightBrace; };
exports.setOutdentRightBrace = function(x) { $outdentRightBrace = x; };

exports.getOutdentRightBracket = function() { return $outdentRightBracket; };
exports.setOutdentRightBracket = function(x) { $outdentRightBracket = x; };

exports.getOutdentRightArrow = function() { return $outdentRightArrow; };
exports.setOutdentRightArrow = function(x) { $outdentRightArrow = x; };

exports.getAlignDots = function() { return $alignDots; };
exports.setAlignDots = function(x) { $alignDots = x; };

exports.getAlignEquals = function() { return $alignEquals; };
exports.setAlignEquals = function(x) { $alignEquals = x; };

exports.getAlignStreamIn = function() { return $alignStreamIn; };
exports.setAlignStreamIn = function(x) { $alignStreamIn = x; };

exports.getAlignStreamOut = function() { return $alignStreamOut; };
exports.setAlignStreamOut = function(x) { $alignStreamOut = x; };

exports.getAlignClassAccessModifiers = function() { return $alignClassAccessModifiers; };
exports.setAlignClassAccessModifiers = function(x) { $alignClassAccessModifiers = x; };

exports.getAlignCase = function() { return $alignCase; };
exports.setAlignCase = function(x) { $alignCase = x; };


});
