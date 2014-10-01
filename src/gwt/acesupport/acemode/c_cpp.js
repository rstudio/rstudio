/*
 * c_cpp.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * The Original Code is Ajax.org Code Editor (ACE).
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *      Fabian Jakobs <fabian AT ajax DOT org>
 *      Gastón Kleiman <gaston.kleiman AT gmail DOT com>
 *
 * Based on Bespin's C/C++ Syntax Plugin by Marc McIntyre.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

define("mode/c_cpp", function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var Tokenizer = require("ace/tokenizer").Tokenizer;
var Range = require("ace/range").Range;
var c_cppHighlightRules = require("mode/c_cpp_highlight_rules").c_cppHighlightRules;

var MatchingBraceOutdent = require("mode/c_cpp_matching_brace_outdent").MatchingBraceOutdent;
var CStyleBehaviour = require("mode/behaviour/cstyle").CStyleBehaviour;

var CppStyleFoldMode = null;
if (!window.NodeWebkit) {
   CppStyleFoldMode = require("mode/c_cpp_fold_mode").FoldMode;
}

var SweaveBackgroundHighlighter = require("mode/sweave_background_highlighter").SweaveBackgroundHighlighter;
var RCodeModel = require("mode/r_code_model").RCodeModel;
var RMatchingBraceOutdent = require("mode/r_matching_brace_outdent").RMatchingBraceOutdent;

var CppLookaroundHeuristics = require("mode/cpp_lookaround_heuristics").CppLookaroundHeuristics;

var Mode = function(suppressHighlighting, doc, session) {
   this.$session = session;
   this.$doc = doc;
   this.$tokenizer = new Tokenizer(new c_cppHighlightRules().getRules());
   this.$tokens = new Array(doc.getLength());
   this.$outdent = new MatchingBraceOutdent();
   this.$r_outdent = {};
   oop.implement(this.$r_outdent, RMatchingBraceOutdent);
   this.$behaviour = new CStyleBehaviour();
   this.codeModel = new RCodeModel(doc, this.$tokenizer, /^r-/, /^\s*\/\*{3,}\s+[Rr]\s*$/);
   this.$sweaveBackgroundHighlighter = new SweaveBackgroundHighlighter(
      session,
         /^\s*\/\*{3,}\s+[Rr]\s*$/,
         /^\*\/$/,
      true
   );

   if (!window.NodeWebkit)     
      this.foldingRules = new CppStyleFoldMode();

   this.$heuristics = new CppLookaroundHeuristics();

};
oop.inherits(Mode, TextMode);

(function() {

   var reStartsWithOpenBrace = /^\s*\{/;
   var reStartsWithDefine = /^\s*#define/;
   var reEndsWithBackslash = /\\\s*$/;

   this.insertChunkInfo = {
      value: "/*** R\n\n*/\n",
      position: {row: 1, column: 0}
   };

   this.toggleCommentLines = function(state, doc, startRow, endRow) {
      var outdent = true;
      var re = /^(\s*)\/\//;

      for (var i = startRow; i <= endRow; i++) {
         if (!re.test(doc.getLine(i))) {
            outdent = false;
            break;
         }
      }

      if (outdent) {
         var deleteRange = new Range(0, 0, 0, 0);
         for (var i = startRow; i <= endRow; i++)
         {
            var line = doc.getLine(i);
            var m = line.match(re);
            deleteRange.start.row = i;
            deleteRange.end.row = i;
            deleteRange.end.column = m[0].length;
            doc.replace(deleteRange, m[1]);
         }
      } else {
         doc.indentRows(startRow, endRow, "//");
      }
   };

   this.getLanguageMode = function(position)
   {
      return this.$session.getState(position.row).match(/^r-/) ? 'R' : 'C_CPP';
   };

   this.inRLanguageMode = function(state)
   {
      return state.match(/^r-/);
   };

   // Identify whether we're currently writing a macro -- either the current
   // line starts with a '#define' statement, or a chain of lines ending with
   // '\' leads back to a line starting with a '#define' statement.
   this.inMacro = function(lines, row) {

      if (row < 0) {
         return false;
      } else if (reStartsWithDefine.test(lines[row])) {
         return true;
      } else if (reEndsWithBackslash.test(lines[row])) {
         return this.inMacro(lines, row - 1);
      } else {
         return false;
      }
   };

   this.$getUnindent = function(line, tabSize) {

      // Get the current line indent
      var indent = this.$getIndent(line);
      if (indent[0] == "\t") {
         return indent.substring(1, indent.length);
      }

      // Otherwise, try to remove a 'tabSize' number of spaces
      var hasLeadingSpaces = true;
      for (var i = 0; i < tabSize; i++) {
         if (indent[i] != " ") {
            hasLeadingSpaces = false;
            break;
         }
      }

      if (hasLeadingSpaces) {
         return indent.substring(tabSize, indent.length);
      }

      // Otherwise, just return the regular line indent
      return indent;
      
   };

   this.getNextLineIndent = function(state, line, tab, tabSize, row) {

      var session = this.$session;

      // Defer to the R language indentation rules when in R language mode
      if (this.inRLanguageMode(state))
         return this.codeModel.getNextLineIndent(row, line, state, tab, tabSize);

      var indent = this.$getIndent(line);
      var unindent = this.$getUnindent(line, tabSize);
      
      var lines = this.$doc.$lines;

      var lastLine;
      if (row > 0) {
         lastLine = lines[row - 1];
      } else {
         lastLine = "";
      }

      // Indentation rules for comments
      if (state == "comment" || state == "doc-start") {

         // If we're inserting a newline within a block comment, insert
         // more '*'.
         var spacesBeforeStarMatch = line.match(/^(\s*\/*)/);
         var spacesBeforeStar = spacesBeforeStarMatch !== null ?
                new Array(spacesBeforeStarMatch[1].length + 1).join(" ") :
                indent;
         
         var spacesAfterStar = " ";
         var spacesAfterStarMatch = line.match(/^\s*\*(\s*)/);
         if (spacesAfterStarMatch) {
            spacesAfterStar = new Array(spacesAfterStarMatch[1].length + 1).join(" ");
         }
         
         return spacesBeforeStar + "*" + spacesAfterStar;
         
      }

      // Rules for the 'general' state
      if (state == "start") {

         // Decisions made should not depend on trailing comments in the line
         // So, we strip those out for the purposes of indentation
         line = line.replace(/\/\/.*/, "");
         lastLine = lastLine.replace(/\/\/.*/, "");

         /**
          * We start by checking some special-cases for indentation --
          * ie, simple cases wherein we can resolve the correct form of
          * indentation from just the first, or previous, line.
          */

         // Indent after a #define with continuation
         if (/^\s*#define.*\\/.test(line)) {
            return indent + tab;
         }

         // Unindent after leaving a #define with continuation
         if (this.inMacro(lines, row - 1) &&
             !reEndsWithBackslash.test(line)) {
            return unindent;
         }

         // Only indent on an ending '>' if we're not in a template
         // We can do this by checking for a matching '>'
         if (/>\s*$/.test(line)) {
            var loc = this.$heuristics.findMatchingBracketRow(">", lines, row, 50);
            if (loc >= 0) {
               return indent;
            } else {
               return indent + tab;
            }
         }

         // Unindent after leaving a block comment
         if (/\*\/\s*$/.test(line)) {

            // Find the start of the comment block
            var blockStartRow = this.$heuristics.findStartOfCommentBlock(
               lines,
               row,
               100
            );
            
            if (blockStartRow >= 0) {
               return this.$getIndent(lines[blockStartRow]);
            }
         }

         // Indent for an unfinished class statement
         if (/^\s*class\s+[\w_]+\s*$/.test(line)) {
            return indent + tab;
         }

         // Indent for a :
         if (/:\s*$/.test(line)) {
            return indent + tab;
         }

         // Don't indent for namespaces, switch statements
         if (/\bnamespace\b.*\{\s*$/.test(line) ||
             /\bswitch\b.*\{\s*$/.test(line)) {
            return indent;
         }

         // Indent if the line ends on an operator token
         // Can't include > here since they may be used
         // for templates (it's handled above)
         var reEndsWithOperator = /[\+\-\/\*\|\<\&\^\%\=]\s*$/;
         if (reEndsWithOperator.test(line)) {
            if (reEndsWithOperator.test(lastLine)) {
               return indent;
            }
            return indent + tab;
         }

         // Indent if the line ends on an operator token
         // Can't include > here since they may be used
         // for templates (it's handled above)
         if (/[\+\-\/\*\|\<\&\^\%\=]\s*$/.test(line)) {
            return indent + tab;
         }

         // Indent after a 'case foo'
         if (/\s*case\s+[\w_'"]+/.test(line)) {
            return indent + tab;
         }

         // Indent a naked else
         if (/^\s*else\s*$/.test(line)) {
            return indent + tab;
         }

         // Unindent after leaving a naked else
         if (/^\s*else\s*$/.test(lastLine) &&
             !reStartsWithOpenBrace.test(line)) {
            return unindent;
         }

         // Indent e.g. "if (foo)"
         if (/^\s*if\s*\(.*\)\s*$/.test(line)) {
            return indent + tab;
         }

         // Unindent after leaving a naked if
         if (/^\s*if\s*\(.*\)\s*$/.test(lastLine) &&
             !reStartsWithOpenBrace.test(line)) {
            return unindent;
         }

         // Indent for an unfinished 'for' statement, e.g.
         //
         //   for (int i = 0;
         //
         var match = line.match(/^(\s*for\s*\().*;\s*$/);
         if (match) {
            return new Array(match[1].length + 1).join(" ");
         }

         // Unindent after leaving a naked for
         if (/^\s*for\s*\(.*\)\s*$/.test(lastLine) &&
             !/^\s*\{/.test(line)) {
            return unindent;
         }
         
         // Indent after 'class foo {' or 'struct foo {'
         if (line.match(/^\s*(class|struct)\s+\w+\s*\{\s*$/)) {
            return indent + tab;
         }

         // Indent following an opening paren
         if (line.match(/\(\s*$/)) {
            return indent + tab;
         }

         // Match the indent of the ':' for an initializer list, e.g.
         // if the user types
         //
         //    : foo_(foo)
         //
         // this is for users who like to line up commas with colons
         if (/^\s*[:,]\s*[\w_]+\(.*\)\s*$/.test(line)) {
            return this.$getIndent(line);
         }

         // If we're looking at a class with the first inheritted member
         // on the same line, e.g.
         //
         //   class Foo : public A,
         //               ^
         var match = line.match(/^(\s*(class|struct)\s+\w+\s*:\s*).*,\s*$/);
         if (match) {
            return new Array(match[1].length + 1).join(" ");
         }

         // If we're looking at a class with one inherited member
         // on the same line, e.g.
         //
         //  class Foo : public Bar<T> {
         //
         // then insert a tab.
         var match = line.match(/^\s*(class|struct)\s+\w+\s*:\s*.*\{\s*$/);
         if (match) {
            return indent + tab;
         }

         // If we're looking at something like inheritance for a class, e.g.
         //
         // class Foo
         // : public Bar,
         //   ^
         // then indent according to the first word following the ':'.
         var match = line.match(/^(\s*:\s*)(\w+).*,\s*$/);
         if (match) {
            return new Array(match[1].length + 1).join(" ");
         }

         // Similar to the above, but we have a leading colon with some
         // following text, and no closing comma; ie
         //
         //   class Foo
         //       : public A
         //       ^
         var match = line.match(/^(\s*)[:,]\s*[\w\s]*$/);
         if (match) {
            return new Array(match[1].length + 1).join(" ");
         }

         // If the line ends with a parenthesis, indent based on the
         // matching opening bracket. This allows indentation for e.g.
         //
         // foo(int a,
         //     int b,
         //     int c);
         // |
         //
         var match = line.match(/([\)\]]);?\s*$/);
         if (match) {

            var openPos = session.findMatchingBracket({
               row: row,
               column: match.index + 1
            });

            if (openPos) {
               return this.$getIndent(lines[openPos.row]);
            }

         }

         // Vertical alignment
         // We need to handle vertical alignment for two scenarios:
         // One, for multi-line function declarations, so that e.g.
         //
         //   void foo(int a, int b, 
         //            ^
         //
         // and two, for cases where we have multiple objects. Maybe
         // this can just be specialized for {.
         //
         //   static object foo {
         //        {foo, bar},
         //        ^
         //
         // Only do this if there are more opening parens than closing parens
         // on the line, so that indentation for e.g. initialization lists
         // work as expected:
         //
         //   Foo(Foo const& other)
         //       : a_(a),
         //         b_(b),
         //         ^
         if (line.match(/,\s*$/)) {

            // Get the balance of parentheses on the line
            var leftParens = line.split("(").length - 1;
            var rightParens = line.split(")").length - 1;

            if (leftParens > rightParens) {
               // get the associated brace position
               var bracePos = line.match(/[[{(]/);
               if (bracePos) {
                  var firstCharAfter = line.substr(bracePos.index).match(/([^\s])/);
                  var idx = firstCharAfter.index;

                  // Nudge out if the brace we just looked at was unmatched
                  var complement = this.$heuristics.$complements[firstCharAfter[1]];
                  if (!line.match("\\" + complement)) {
                     idx += 1;
                  }

                  return Array(idx + bracePos.index + 1).join(" ");

               }
            } else {
               return indent;
            }
         }

         // Indent based on lookaround heuristics
         if (!/^\s*$/.test(line)) {

            var heuristicRow = this.$heuristics.getRowForOpenBraceIndent(
               this.$session,
               row,
               20
            );

            if (heuristicRow !== null) {
               return this.$getIndent(lines[heuristicRow]) + tab;
            }

         }

         // If the closing character is an 'opener' (ie, one of
         // '(', '{', '[', or '<'), then indent
         if (/.*[\(\{\[\<]\s*$/.test(line)) {
            return indent + tab;
         }

      } // start state rules

      return indent;
   };
   
   this.checkOutdent = function(state, line, input) {
      if (this.inRLanguageMode(state))
         return this.$r_outdent.checkOutdent(state, line, input);
      else
         return this.$outdent.checkOutdent(state, line, input);
   };

   this.autoOutdent = function(state, doc, row) {
      if (this.inRLanguageMode(state))
         return this.$r_outdent.autoOutdent(state, doc, row);
      else
         return this.$outdent.autoOutdent(state, doc, row);
   };

}).call(Mode.prototype);

exports.Mode = Mode;
});
