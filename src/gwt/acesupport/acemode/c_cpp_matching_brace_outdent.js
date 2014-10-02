define("mode/c_cpp_matching_brace_outdent", function(require, exports, module) {

var Range = require("ace/range").Range;
var CppLookaroundHeuristics = require("mode/cpp_lookaround_heuristics").CppLookaroundHeuristics;

var MatchingBraceOutdent = function() {
   this.$heuristics = new CppLookaroundHeuristics();
};

(function() {

   var reNaked = /^\s*[\w_:]+\s*$|^\s*[\w_:]+\s*\(.*\)\s*$/;

   // Set the indent of the line at 'row' to the indentation
   // at 'rowFrom'.
   this.setIndent = function(session, rowTo, rowFrom) {

      var doc = session.getDocument();

      var line = doc.$lines[rowTo];
      var lastLine = doc.$lines[rowFrom];

      var oldIndent = this.$getIndent(line);
      var newIndent = this.$getIndent(lastLine);

      doc.replace(
         new Range(rowTo, 0, rowTo, oldIndent.length),
         newIndent
      );
      
   };

   this.checkOutdent = function(state, line, input) {

      if (state == "start") {

         // private: / public: / protected:
         if (input == ":") {
            return true;
         }

         // outdenting for '\'
         if (input == "\\") {
            return true;
         }

         if (/^\s*[\{\}\>\]\<]/.test(input)) {
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

   this.outdentBraceForNakedTokens = function(session, row, line, lastLine) {

      if (/^\s*\{\s*$/.test(line)) {

         if (lastLine !== null && reNaked.test(lastLine)) {
            this.setIndent(session, row, row - 1);
            return true;
         }

      }

      return false;

   };

   this.checkDoubleArrowAlignment = function(session, row, line, lastLine) {

      if (/^\s*<<\s*$/.test(line)) {
         var index = lastLine.indexOf("<<");
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
      var line = this.$heuristics.getLineSansComments(doc, row);
      var lastLine = null;
      if (row > 0)
         lastLine = this.$heuristics.getLineSansComments(doc, row - 1);
      var indent = this.$getIndent(line);

      // Check for naked token outdenting
      if (this.outdentBraceForNakedTokens(session, row, line, lastLine)) {
         return;
      }

      // Check for '<<' alignment
      if (this.checkDoubleArrowAlignment(session, row, line, lastLine)) {
         return;
      }

      // If we just inserted a '>', find the matching '<' for indentation.
      if (/^\s*\>$/.test(line)) {

         var matchedRow = this.$heuristics.findMatchingBracketRow(
            ">",
            doc.$lines,
            row,
            50
         );

         if (matchedRow >= 0) {
            this.setIndent(session, row, matchedRow);
            return;
         }
      }

      // If we just inserted a '}' on a blank line, try and find the
      // matching '{' for indentation.
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
      if (/^\s*\}/.test(line)) {

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
            var heuristicRow = this.$heuristics.getRowForOpenBraceIndent(
               session,
               openBracketPos.row,
               20
            );

            if (heuristicRow !== null) {
               this.setIndent(session, row, heuristicRow);
               return;
            }
            // We didn't find a class token -- check if
            // we can look back for an initializer list
            var lineToCheck = doc.$lines[openBracketPos.row];
            if (/.*\)\s*\{/.test(lineToCheck)) {

               var openParenPos = session.findMatchingBracket({
                  row: openBracketPos.row,
                  column: lineToCheck.lastIndexOf(")") + 1
               });
               
               if (openParenPos) {
                  this.setIndent(session, row, openParenPos.row);
                  return;
               }
               
            }

         } 

      }

      // If we inserted ']' on a blank line, match the indentation
      // of its associated '['.
      if (/^\s*\]/.test(line)) {

         var openBracketPos = session.findMatchingBracket({
            row: row,
            column: line.lastIndexOf("]") + 1
         });

         if (openBracketPos) {
            this.setIndent(session, row, openBracketPos.row);
            return;
         }
      }

      // if we just typed 'public:', 'private:' or 'protected:',
      // we should outdent if possible. Do so by looking for the
      // enclosing 'class' scope.
      if (/^\s*public:\s*$|^\s*private:\s*$|^\s*protected:\s*$/.test(line)) {

         // look for the enclosing 'class' to get the indentation
         var len = 0;
         var match = false;
         var maxLookback = 200;
         var count = 0;
         for (var i = row; i >= 0; i--) {
            var line = this.$heuristics.getLineSansComments(doc, i);
            match = line.match(/\bclass\b/);
            if (match) {
               len = match.index;
               break;
            }

            count++;
            if (count > maxLookback) break;
         }

         if (match)
            doc.replace(new Range(row, 0, row, indent.length - len), "");

         return;
      }

      // If we just typed 'case <word>:', outdent if possible. Do so
      // by looking for the enclosing 'switch'.
      if (/^\s*case\s+\w+:/.test(line)) {

         // look for 'switch' statement for indentation
         var len = 0;
         var match = false;
         var maxLookback = 200;
         var count = 0;
         for (var i = row; i >= 0; i--) {
            var line = this.$heuristics.getLineSansComments(doc, i);
            match = line.match(/\bswitch\b/);
            if (match) {
               len = match.index;
               break;
            }

            count++;
            if (count > maxLookback) break;

         }

         if (match) {
            doc.replace(new Range(row, 0, row, indent.length - len), "");
         }

         return;
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
      if (line.match(/^\s*{/)) {

         var scopeRow = this.$heuristics.getRowForOpenBraceIndent(
            session,
            row - 1,
            20
         );

         if (scopeRow !== null) {
            this.setIndent(session, row, scopeRow);
            return;
         }

      }

      // Default matching rules
      var match = line.match(/^(\s*\})$/);
      if (!match) return 0;

      var column = match[1].length;
      var openBracePos = session.findMatchingBracket({
         row: row,
         column: column
      });

      if (!openBracePos) return 0;

      // Just use the indentation of the matching brace
      if (openBracePos.row >= 0) {
         this.setIndent(session, row, openBracePos.row);
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

}).call(MatchingBraceOutdent.prototype);

exports.MatchingBraceOutdent = MatchingBraceOutdent;
});
