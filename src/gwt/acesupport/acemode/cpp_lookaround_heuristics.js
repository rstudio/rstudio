define("mode/cpp_lookaround_heuristics", function(require, exports, module) {

var CppLookaroundHeuristics = function() {};

(function() {

   var reStartsWithComma = /^\s*,/;
   var reStartsWithColon = /^\s*:/;
   var reStartsWithCommaOrColon = /^\s*[,:]/;
   var reEndsWithComma = /,\s*$|,\s*\/\//;
   var reEndsWithColon = /:\s*$|:\s*\/\//;
   var reClass = /\bclass\b/;

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

      var lines = session.getDocument().$lines;
      if (lines.length <= 1) return null;
      
      var count = 0;

      // We allow one 'miss' as far as looking for lines that start/end
      // with commas. This heuristic allows us to handle e.g.
      //
      //   class Foo : public A,
      //               public B,
      //               public C
      //   {
      //
      // as the first line we view wil not have any commas or colons.
      var canRetry = true;

      var startRow = row;
      var firstLine = lines[startRow].replace(/\/\/.*/, "");

      while (count < maxLookback && row >= 0) {

         if (count == 0) {
            var line = firstLine;
         } else {
            var line = lines[row].replace(/\/\/.*/, "");
         }

         if (reClass.test(line)) {
            return row;
         }

         // If this line starts with a colon, check the previous line
         // for a parenthesis to indent with.
         if (reStartsWithColon.test(line)) {
            var prevLine = lines[row - 1].replace(/\/\/.*/, "");
            if (/\)\s*$/.test(prevLine)) {
               
               var openParenPos = session.findMatchingBracket({
                  row: row - 1,
                  column: prevLine.lastIndexOf(")") + 1
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
                  column: line.lastIndexOf(")") + 1
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

      // Fallback to indentation for functions, e.g.
      //
      //   int foo(int a, int b,
      //           int c, int d) {
      //
      if (/\)\s*\{/.test(firstLine)) {
         
         var openParenPos = session.findMatchingBracket({
            row: startRow,
            column: firstLine.lastIndexOf(")") + 1
         });

         if (openParenPos) {
            return openParenPos.row;
         }
      }

      // Fail -- return null
      return null;
   };

   // Get a line, with comments (following '//') stripped.
   this.getLineWithoutComments = function(doc, row) {
      var line = doc.getLine(row);
      var index = line.indexOf("//");
      if (index != -1) {
         return line.substring(0, index);
      }
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

