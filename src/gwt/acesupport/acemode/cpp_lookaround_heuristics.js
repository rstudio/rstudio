define("mode/cpp_lookaround_heuristics", function(require, exports, module) {

var CppLookaroundHeuristics = function() {};

(function() {

   var reStartsWithComma = /^\s*,/;
   var reStartsWithColon = /^\s*:/;
   var reStartsWithCommaOrColon = /^\s*[,:]/;
   var reEndsWithComma = /,\s*$|,\s*\/\//;
   var reEndsWithColon = /:\s*$|:\s*\/\//;
   var reClassOrStruct = /\bclass\b|\bstruct\b/;
   var reEndsWithBackslash = /\\\s*$/;

   this.$complements = {
      "<" : ">",
      ">" : "<",
      "{" : "}",
      "}" : "{",
      "[" : "]",
      "]" : "[",
      "(" : ")",
      ")" : "(",
      "'" : "'",
      '"' : '"'
   };

   this.getRowForOpenBraceIndentClassStyle = function(session, row, maxLookback) {

      var doc = session.getDocument();
      var count = 0;

      // We allow one 'miss' as far as looking for lines that start/end
      // with commas. This heuristic allows us to handle e.g.
      //
      //   class Foo : public A,
      //               public B,
      //               public C
      //   {
      //
      // as the first line we view will not have any commas or colons.
      var canRetry = true;

      var startRow = row;
      var firstLine = this.getLineSansComments(doc, startRow);

      // If the first line is just an open brace, go up one
      if (/^\s*\{\s*$/.test(firstLine)) {
         row = row - 1;
         firstLine = this.getLineSansComments(doc, row);
      }

      while (count < maxLookback && row >= 0) {

         var line;
         if (count == 0) {
            line = firstLine;
         } else {
            line = this.getLineSansComments(doc, row);
         }

         if (reClassOrStruct.test(line)) {
            return row;
         }

         // If this line starts with a colon, check the previous line
         // for a parenthesis to indent with.
         if (reStartsWithColon.test(line)) {
            var prevLine = this.getLineSansComments(doc, row - 1);
            if (/\)\s*$/.test(prevLine)) {
               
               var openParenPos = session.findMatchingBracket({
                  row: row - 1,
                  column: doc.$lines[row - 1].lastIndexOf(")") + 1
               });

               if (openParenPos) {
                  return openParenPos.row;
               }
               
            } else {
               return row - 1;
            }
         }

         // Similarly, if the line ends with a colon, this line is most likely
         // the one containing the parenthesis that provides the scope.
         if (reEndsWithColon.test(line)) {

            var lastParen = line.lastIndexOf(")");

            if (lastParen >= 0) {

               var openParenPos = session.findMatchingBracket({
                  row: row,
                  column: doc.$lines[row].lastIndexOf(")") + 1
               });

               if (openParenPos) {
                  return openParenPos.row;
               } else {
                  return row;
               }
               
            }

         }

         // Check whether we can keep walking up, or if we've run out of
         // 'valid' formats.
         if (!(reStartsWithCommaOrColon.test(line) ||
               reEndsWithComma.test(line))) {
            if (!canRetry) {
               break;
            } else {
               canRetry = false;
            }
         }

         count++;
         row--;
      }

      // Return null on failure
      return null;
      
   };

   this.getRowForOpenBraceIndentFunctionStyle = function(session, row, maxLookback) {

      var doc = session.getDocument();
      var firstLine = this.getLineSansComments(doc, row);

      // Fallback to indentation for functions, e.g.
      //
      //   int foo(int a, int b,
      //           int c, int d) {
      //
      if (/\)\s*\{/.test(firstLine)) {
         
         var openParenPos = session.findMatchingBracket({
            row: row,
            column: doc.$lines[row].lastIndexOf(")") + 1
         });

         if (openParenPos) {
            return openParenPos.row;
         }
      }

      // Fallback to indentation for functions, e.g.
      //
      //   int foo(int a, int b,
      //           int c, int d)
      //   {
      var prevLine = this.getLineSansComments(doc, row - 1);

      if (/\s*\{\s*$/.test(firstLine) && /\)\s*$/.test(prevLine)) {

         var openParenPos = session.findMatchingBracket({
            row: row - 1,
            column: doc.$lines[row - 1].lastIndexOf(")") + 1
         });

         if (openParenPos) {
            return openParenPos.row;
         }
      }

      // Fail -- return null
      return null;
      
   };
   
   // Given a row with a '{', we look back for the row that provides
   // the start of the scope, for purposes of indentation. We look back
   // for:
   //
   // 1. A class token, or
   // 2. A constructor with an initializer list.
   //
   // Return 'null' if no row could be found, and the corresponding row
   // otherwise.
   this.getRowForOpenBraceIndent = function(session, row, maxLookback) {

      var doc = session.getDocument();
      var lines = doc.$lines;
      if (lines.length <= 1) return null;

      // First, try class-style indentation lookup
      var classStyleIndent =
             this.getRowForOpenBraceIndentClassStyle(session, row, maxLookback);
      if (classStyleIndent !== null) {
         return classStyleIndent;
      }

      // Then, try function-style indentation lookup
      var fnStyleIndent =
             this.getRowForOpenBraceIndentFunctionStyle(session, row, maxLookback);
      if (fnStyleIndent !== null) {
         return fnStyleIndent;
      }

      // Give up and return null
      return null;
      
   };

   // Get a line, with comments (following '//') stripped. Also strip
   // a trailing '\' anticipating e.g. macros.
   this.getLineSansComments = function(doc, row) {
      
      var line = doc.getLine(row);
      
      var index = line.indexOf("//");
      if (index != -1) {
         line = line.substring(0, index);
      }
      
      if (reEndsWithBackslash.test(line)) {
         line = line.substring(0, line.lastIndexOf("\\"));
      }

      line = line.replace(/\bconst\s*&\s*\b/g, "")
                 .replace(/\bconst\s*\b/g, "")
                 .replace(/\bnoexcept\s*\b/g, "");
      
      
      return line;
      
   };

   // Find the row for which a matching character for the character
   // 'character' is on. 'lines' is the set of lines to search
   // through, 'row' is the row to begin the search on, and
   // 'maxLookaround' gives the maximum number of lines we should look
   // over. Direction gives the direction and should be 'forward' or
   // 'backward', and defaults to 'backward'.  Returns -1 is nothing
   // is found. 'balance' should be left undefined but can be
   // specified optionally if desired.
   this.findMatchingBracketRow = function(character, lines, row,
                                          maxLookaround, direction) {

      direction = typeof direction !== 'undefined' ? direction : "backward";
      return this.doFindMatchingBracketRow(character, lines, row,
                                           maxLookaround, direction,
                                           0, 0);
      
   };
   
   this.doFindMatchingBracketRow = function(character, lines, row,
                                            maxLookaround, direction,
                                            balance, count) {

      if (count > maxLookaround) return -1;
      if (row < 0 || row > lines.length - 1) return -1;

      var line = lines[row];

      var nRight = line.split(character).length - 1;
      var nLeft = line.split(this.$complements[character]).length - 1;

      balance = balance + nRight - nLeft;
      
      if (balance <= 0) {
         return row;
      }

      if (direction == "backward") {
         row = row - 1;
      } else if (direction == "forward") {
         row = row + 1;
      } else {
         row = row - 1;
      }

      return this.doFindMatchingBracketRow(character, lines, row,
                                           maxLookaround, direction, balance,
                                           count + 1);
   };

   this.findStartOfCommentBlock = function(lines, row, maxLookback) {
      var count = 0;
      var reCommentBlockStart = /^\s*\/+\*/;
      while (row >= 0 && count < maxLookback) {
         var line = lines[row];
         if (reCommentBlockStart.test(line)) {
            return row;
         }
         --row;
         ++count;
      }
      return null;
   };
   
}).call(CppLookaroundHeuristics.prototype);

exports.CppLookaroundHeuristics = CppLookaroundHeuristics;

});

