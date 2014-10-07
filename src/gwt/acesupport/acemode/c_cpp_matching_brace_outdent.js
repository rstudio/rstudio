define("mode/c_cpp_matching_brace_outdent", function(require, exports, module) {

var Range = require("ace/range").Range;

var TokenCursor = require("mode/token_cursor").TokenCursor;
var MatchingBraceOutdent = function(codeModel) {
   this.$codeModel = codeModel;
};

// Allow the user to control outdenting if desired
var $outdentColon              = true; // : (initializer list)
var $outdentRightParen         = true; // )
var $outdentLeftBrace          = true; // {
var $outdentRightBrace         = true; // }
var $outdentRightBracket       = true; // ]
var $outdentChevron            = true; // >
var $alignDots                 = true; // .
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

      if (state == "start") {

         // private: / public: / protected
         // also class initializer lists
         if (input == ":") {
            return true;
         }

         // outdenting for lines starting with 'closers' and 'openers'
         if (/^\s*[\{\}\>\]\)<.:]/.test(input)) {
            return true;
         }

      }

      // check for nudging of '/' to the left (?)
      if (state == "comment") {

         if (input == "/") {
            return true;
         }

      }

      return false;

   };

   this.outdentBraceForNakedTokens = function(session, row, line, prevLine) {

      if (/^\s*\{/.test(line)) {
         var re = this.$codeModel.reNakedBlockTokens;
         if (prevLine !== null) {
            for (var key in re) {
               if (re[key].test(prevLine)) {
                  this.setIndent(session, row, row - 1);
                  return true;
               }
            }
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

   this.autoOutdent = function(state, session, row) {

      var doc = session.doc;
      var line = this.$codeModel.getLineSansComments(doc, row);
      var prevLine = null;
      if (row > 0)
         prevLine = this.$codeModel.getLineSansComments(doc, row - 1);
      var indent = this.$getIndent(line);

      // Check for naked token outdenting
      if (this.outdentBraceForNakedTokens(session, row, line, prevLine)) {
         return;
      }

      // Check for '<<', '.'alignment
      if ($alignStreamOut && this.alignStartToken("<<", session, row, line, prevLine)) {
         return;
      }

      if ($alignStreamIn && this.alignStartToken(">>", session, row, line, prevLine)) {
         return;
      }

      if ($alignDots && this.alignStartToken(".", session, row, line, prevLine)) {
         return;
      }

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
      if ($outdentColon && /^\s*:/.test(line)) {

         if (this.$codeModel.$tokenUtils.$tokenizeUpToRow(row)) {

            var tokenCursor = new TokenCursor(this.$codeModel.$tokens, row, 0);
            var rowToUse = null;
            if (tokenCursor.moveBackwardOverMatchingParens()) {
               if (tokenCursor.moveToPreviousToken()) {
                  rowToUse = tokenCursor.$row;
               }
            } else {
               while (tokenCursor.moveToPreviousToken()) {
                  if (tokenCursor.currentValue() === "class") {
                     rowToUse = tokenCursor.$row;
                     break;
                  }

                  var type = tokenCursor.currentType();
                  if (!(type === "keyword" || type === "identifier")) {
                     break;
                  }
               }
            }
         
            if (rowToUse !== null) {
               this.setIndent(session, row, rowToUse, session.getTabString(),
                              function(oldIndent, newIndent) {
                                 return oldIndent === newIndent;
                              });
            }

         }
      }

      // If we just inserted a '>', find the matching '<' for indentation.
      //
      // But this runs into problems with indentation for use of the '>>'
      // operator on its own line, e.g.
      //
      //    std::cin >> foo
      //             >> bar
      //             >> baz;
      //
      if ($outdentChevron && /^\s*\>$/.test(line)) {

         var matchedRow = this.$codeModel.findMatchingBracketRow(
            ">",
            doc,
            row,
            50
         );

         if (matchedRow >= 0) {
            var matchedLine = this.$codeModel.getLineSansComments(doc, matchedRow);
            if (!/^\s*>>/.test(line)) {
               this.setIndent(session, row, matchedRow);
               return;
            }
         }
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
                   doc.$lines[openBracketPos.row].replace(/\/\/.*/, "");

            if (/^\s*\{\s*$/.test(openBracketLine)) {
               this.setIndent(session, row, openBracketPos.row);
               return;
            }

            // Otherwise, try looking upwards to get an appropriate indentation
            var heuristicRow = this.$codeModel.getRowForOpenBraceIndent(
               session,
               openBracketPos.row
            );

            if (heuristicRow !== null) {
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
          /^\s*public\s*:|^\s*private\s*:|^\s*protected\s*:/.test(line)) {

         // Find the associated open bracket.
         var openBraceRow = this.$codeModel.doFindMatchingBracketRow(
            "}",
            doc,
            row,
            1E6,
            "backward",
            1,
            0
         );

         if (openBraceRow >= 0) {
            // If this open brace is already associated with a class or struct,
            // step over all of those rows.
            var heuristicRow =
                   this.$codeModel.getRowForOpenBraceIndent(session, openBraceRow);

            if (heuristicRow !== null && heuristicRow >= 0) {
               this.setIndent(session, row, heuristicRow);
               return;
            } else {
               this.setIndent(session, row, openBraceRow);
               return;
            }
         }
         
      }

      // Similar lookback for 'case foo:', but we have a twist: we want to walk
      // up rows, but in case we run into a 'case:' with indentation already set
      // in a different way from the auto-outdent, we match that indentation.
      //
      // This implies that e.g.
      //
      //     switch (x)
      //     {
      //         case Foo: bar; break;
      //         ^
      //
      // so we match the indentation of the 'case', rather than the open brace
      // associated with the switch.
      if ($alignCase && /^\s*case.+:/.test(line)) {

         // Find the associated open bracket.
         var openBraceRow = this.$codeModel.doFindMatchingBracketRow(
            "}",
            doc,
            row - 1,
            1E6,
            "backward",
            1,
            0,
            function(x) { return /^\s*case.+:/.test(x); }
         );

         if (openBraceRow >= 0) {
            var heuristicRow =
                   this.$codeModel.getRowForOpenBraceIndent(session, openBraceRow);

            if (heuristicRow !== null && heuristicRow >= 0) {
               this.setIndent(session, row, heuristicRow);
               return;
            } else {
               this.setIndent(session, row, openBraceRow);
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

            var scopeRow = this.$codeModel.getRowForOpenBraceIndent(
               session,
               row
            );

            if (scopeRow !== null) {

               // Don't indent if the 'class' has an associated open brace. This ensures
               // that we get outdenting e.g.
               //
               //     class Foo {
               //         {
               //         ^
               //
               // , ie, we avoid putting the open brace at indentation of 'class' token.
               if (this.$codeModel.getLineSansComments(doc, scopeRow).indexOf("{") === -1) {
                  this.setIndent(session, row, scopeRow);
                  return;
               }
            }

         }

      }

   };

   this.$getIndent = function(line) {
      var match = line.match(/^(\s+)/);
      if (match) {
         return match[1];
      }
      return "";
   };

}).call(MatchingBraceOutdent.prototype);

exports.MatchingBraceOutdent = MatchingBraceOutdent;

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

exports.getOutdentChevron = function() { return $outdentChevron; };
exports.setOutdentChevron = function(x) { $outdentChevron = x; };

exports.getAlignDots = function() { return $alignDots; };
exports.setAlignDots = function(x) { $alignDots = x; };

exports.getAlignStreamIn = function() { return $alignStreamIn; };
exports.setAlignStreamIn = function(x) { $alignStreamIn = x; };

exports.getAlignStreamOut = function() { return $alignStreamOut; };
exports.setAlignStreamOut = function(x) { $alignStreamOut = x; };

exports.getAlignClassAccessModifiers = function() { return $alignClassAccessModifiers; };
exports.setAlignClassAccessModifiers = function(x) { $alignClassAccessModifiers = x; };

exports.getAlignCase = function() { return $alignCase; };
exports.setAlignCase = function(x) { $alignCase = x; };


});
