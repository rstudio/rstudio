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

var getVerticallyAlignFunctionArgs = require("mode/r_code_model").getVerticallyAlignFunctionArgs;

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
   this.getLineSansComments = this.$heuristics.getLineSansComments;

};
oop.inherits(Mode, TextMode);

(function() {

   var that = this;

   var reStartsWithOpenBrace = /^\s*\{/;
   var reStartsWithDefine = /^\s*#\s*define/;
   var reEndsWithBackslash = /\\\s*$/;

   this.allIndicesOf = function(string, character) {
      var result = [];
      for (var i = 0; i < string.length; i++) {
         if (string[i] == character) {
            result.push(i);
         }
      }
      return result;
   };

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

      var line = lines[row];

      if (row < 0) {
         return false;
      } else if (reEndsWithBackslash.test(line)) {
         if (reStartsWithDefine.test(line)) {
            return true;
         } else {
            return this.inMacro(lines, row - 1);
         }
      } else {
         return false;
      }
   };

   // Pad an indentation up to some size by adding leading spaces.
   // This preserves tabs in the indent. Returns indentation as-is
   // if it's already that size or greater.
   this.$padIndent = function(indent, tabSize, newIndentSize) {

      var tabsAsSpaces = new Array(tabSize + 1).join(" ");
      var indentLength = indent.replace("\t", tabsAsSpaces);

      if (indentLength >= newIndentSize) {
         return indent;
      } else {
         return indent +
            new Array(newIndentSize - indentLength + 1).join(" ");
      }
   };

   this.$getUnindent = function(line, tabSize) {

      // Get the current line indent
      var indent = this.$getIndent(line);
      if (indent === null || indent.length === 0) {
         return "";
      }

      // Try cutting off a tab.
      var tabIndex = indent.indexOf("\t");
      if (tabIndex != -1) {
         return indent.substring(0, tabIndex) +
            indent.substring(tabIndex + 1, indent.length);
      }

      // Otherwise, try to remove a 'tabSize' number of spaces
      var numLeadingSpaces = 0;
      for (var i = 0; i < tabSize && i < indent.length; i++) {
         if (indent[i] === " ") {
            numLeadingSpaces++;
         }
      }
      
      return indent.substring(numLeadingSpaces, indent.length);
      
   };

   this.getNextLineIndent = function(state, line, tab, tabSize, row, dontSubset) {

      // Defer to the R language indentation rules when in R language mode
      if (this.inRLanguageMode(state))
         return this.codeModel.getNextLineIndent(row, line, state, tab, tabSize);

      // Ask the R code model if we want to use vertical alignment
      var $verticallyAlignFunctionArgs = getVerticallyAlignFunctionArgs();

      var session = this.$session;
      var doc = session.getDocument();

      var indent = this.$getIndent(line);
      var unindent = this.$getUnindent(line, tabSize);
      
      var lines = doc.$lines;
      
      var lastLine;
      if (row > 0) {
         lastLine = lines[row - 1];
      } else {
         lastLine = "";
      }

      // Indentation rules for comments
      if (state == "comment" || state == "doc-start") {

         // Choose indentation for the current line based on the position
         // of the cursor -- but make sure we only apply this if the
         // cursor is on the same row as the line being indented
         if (cursor && cursor.row == row) {
            line = line.substring(0, cursor.column);
         }

         // Bail if line is just whitespace. This is necessary for when the
         // cursor is to the left of a comment block.
         if (/^\s*$/.test(line)) {
            return this.$getIndent(lines[row]);
         }
         
         // NOTE: It is the responsibility of c_style_behaviour to insert
         // a '*' and leading spaces on newline insertion! We just look
         // for the opening block and use indentation based on that. Otherwise,
         // reindent will replicate the leading comment stars.
         var commentStartRow = this.$heuristics.findStartOfCommentBlock(lines, row, 200);
         if (commentStartRow !== null) {
            return this.$getIndent(lines[commentStartRow]) + " ";
         }
         
      }

      // Rules for the 'general' state
      if (state == "start") {

         var match = null;

         /**
          * We start by checking some special-cases for indentation --
          * ie, simple cases wherein we can resolve the correct form of
          * indentation from just the first, or previous, line.
          */

         // Indent after a #define with continuation; but don't indent
         // without continutation
         if (reStartsWithDefine.test(line)) {
            if (/\\\s*$/.test(line)) {
               return indent + tab;
            }
            return indent;
         }

         // Unindent after leaving a #define with continuation
         if (this.inMacro(lines, row - 1) &&
             !reEndsWithBackslash.test(line)) {
            return unindent;
         }

         // Decisions made should not depend on trailing comments in the line
         // So, we strip those out for the purposes of indentation.
         //
         // Note that we strip _after_ the define steps so that we can
         // effectively leverage the indentation rules within macro settings.
         line = this.getLineSansComments(doc, row);
         var cursor = session.getSelection().getCursor();

         // Choose indentation for the current line based on the position
         // of the cursor -- but make sure we only apply this if the
         // cursor is on the same row as the line being indented.
         if (cursor && cursor.row == row && !dontSubset) {
            line = line.substring(0, cursor.column);
         }
         
         lastLine = this.getLineSansComments(doc, row - 1);

         // Only indent on an ending '>' if we're not in a template
         // We can do this by checking for a matching '<'
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
               200
            );
            
            if (blockStartRow >= 0) {
               return this.$getIndent(lines[blockStartRow]);
            }
         }

         // Special-case indentation for aligned streaming.
         // This handles indentation for the case of e.g.
         //
         //   std::cout << foo
         //             << bar
         //             << baz;
         //   ^
         //
         if (/^\s*<</.test(line)) {
            var currentRow = row - 1;
            while (/^\s*<</.test(lines[currentRow])) {
               currentRow--;
            }
            return this.$getIndent(lines[currentRow]);
         }

         // Indent for a :
         if (/:\s*$/.test(line)) {

            // If the line ends with a colon, and the previous line
            // ends with a question mark, then match the current line's
            // indent. This supports indentation for e.g.
            //
            //   x = foo ?
            //       bar :
            //       ^
            //
            if (/\?\s*$/.test(lastLine)) {
               return indent;
            }
            
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
         var reEndsWithOperator = /[\+\-\/\*\|\?<\&\^\%\=]\s*$/;
         if (reEndsWithOperator.test(line)) {
            return indent + tab;
         }

         // Indent if the line ends on an operator token
         // Can't include > here since they may be used
         // for templates (it's handled above)
         if (/[\+\-\/\*\|<\&\^\%\=]\s*$/.test(line)) {
            return indent + tab;
         }

         // Indent after a 'case foo' -- this handles e.g.
         //
         //   case foo: bar;
         //
         // ie, with the first statement on the same line
         if (/^\s*case\s+[\w_]+/.test(line)) {
            return indent + tab;
         }

         // Indent for an unfinished 'for' statement, e.g.
         //
         //   for (int i = 0;
         //        ^
         //
         match = line.match(/^(\s*for\s*\().*[;,]\s*$/);
         if (match) {

            // TODO: function that pads current indentation with spaces,
            // so that we respect tabs?
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
               indent + tab;
         }

         // Handle naked 'for', 'if' etc. tokens. This function is a bit awkward --
         // either it returns an indent to use, or returns a row number from
         // which we can infer the indent.
         var newIndent = this.$heuristics.indentNakedTokens(doc, indent, tab, row);
         if (newIndent !== null) {
            if (typeof newIndent === "string") {
               return newIndent;
            } else if (typeof newIndent === "number") {
               return this.$getIndent(lines[newIndent]);
            }
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
         // NOTE: This is a bad rule to use by default! Prefer auto-outdenting
         // commas instead if we really desire this behaviour. Leaving this
         // comment here in case I change my mind later ...
         //
         // if (/^\s*[:,]\s*[\w_]+\(.*\)\s*$/.test(line)) {
         //    return $verticallyAlignFunctionArgs ?
         //       this.$getIndent(line) :
         //       indent + tab;
         // }

         // If we've made a function definition all on one line,
         // just return the current indent.
         if (/\(.*\).*\{.*\}\s*;?\s*/.test(line)) {
            var lBraces = this.allIndicesOf(line, "{");
            var rBraces = this.allIndicesOf(line, "}");
            if (lBraces.length > 0 && lBraces.length == rBraces.length) {
               return indent;
            }
         }

         // If we have a class with an open brace on the same line, indent
         if (/^\s*(class|struct).*\{\s*$/.test(line)) {
            return indent + tab;
         }

         // Match the indentation of the ':' in a statement e.g.
         //
         //   class Foo : public A
         //             ^
         //
         // Note the absence of a closing comma. This is for users
         // who would prefer to align commas with colons, when
         // doing multi-line inheritance.
         if (/^\s*(class|struct).*:\s*[\w_]+/.test(line) && !/,\s*/.test(line)) {
            return $verticallyAlignFunctionArgs ?
               new Array(line.indexOf(":") + 1).join(" ") :
               indent + tab;
         }

         // If we're looking at a class with the first inheritted member
         // on the same line, e.g.
         //
         //   class Foo : public A,
         //               ^
         //
         match = line.match(/^(\s*(class|struct).*:\s*).*,\s*$/);
         if (match) {
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
               indent + tab;
         }

         // If we're looking at something like inheritance for a class, e.g.
         //
         // class Foo
         // : public Bar,
         //   ^
         //
         // then indent according to the first word following the ':'.
         match = line.match(/^(\s*:\s*)(\w+).*,\s*$/);
         if (match) {
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
               indent + tab;
         }

         // Similar to the above, but we have a leading colon with some
         // following text, and no closing comma; ie
         //
         //   class Foo
         //       : public A
         //       ^
         match = line.match(/^(\s*)[:,]\s*[\w\s]*$/);
         if (match) {
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
               indent + tab;
         }

         // If the line ends with a parenthesis, indent based on the
         // matching opening bracket. This allows indentation for e.g.
         //
         // foo(int a,
         //     int b,
         //     int c);
         // ^
         //
         // Do this only when the parenthesis is not matched on the same row
         // (because otherwise we're missing potential lookback information)
         match = line.match(/([\)\]])(;)?\s*$/);
         if (match) {

            var openPos = session.findMatchingBracket({
               row: row,
               column: lines[row].lastIndexOf(match[1]) + 1
            });

            var maybeTab = typeof match[2] !== "undefined" ?
                   "" :
                   tab;

            if (openPos && openPos.row != row) {
               return this.$getIndent(lines[openPos.row]) + maybeTab;
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
         var bracePos = /^.*([\[\{\(]).+,\s*$/.exec(line);
         if (bracePos) {

            // Loop through the openers until we find an unmatched brace on
            // the line
            var openers = ["(", "{", "["];
            for (var i = 0; i < openers.length; i++) {

               // Get the character alongside its complement
               var lChar = openers[i];
               var rChar = this.$heuristics.$complements[lChar];

               // Get the indices for matches of the character and its complement
               var lIndices = this.allIndicesOf(line, lChar);
               if (!lIndices.length) continue;
               
               var rIndices = this.allIndicesOf(line, rChar);

               // Get the index -- we use the first unmatched index
               var indexToUse = lIndices.length - rIndices.length - 1;
               if (indexToUse < 0) continue;

               var index = lIndices[indexToUse];

               if ($verticallyAlignFunctionArgs) {

                  // Find the first character following the open token --
                  // this is where we want to set the indentation
                  var firstCharAfter = line.substr(index + 1).match(/([^\s])/);
                  return new Array(index + firstCharAfter.index + 2).join(" ");
                  
               } else {
                  return indent + tab;
               }
               
            }
         }

         // Vertical alignment for closing parens
         //
         // This allows us to infer an appropriate indentation for things
         // like multi-dimensional arrays, e.g.
         //
         //   [
         //      [1, 2, 3,
         //       4, 5, 6,
         //       7, 8, 9],
         //      ^
         match = /(\]),\s*$/.exec(line);
         if (match) {
            
            var openBracePos = session.findMatchingBracket({
               row: row,
               column: match.index + 1
            });

            if (openBracePos) {
               return $verticallyAlignFunctionArgs ?
                  this.$getIndent(lines[openBracePos.row]) :
                  indent + tab;
            }
         }
         
         // Simpler comma ending indentation
         if (/,\s*$/.test(line)) {
            return indent;
         }

         // If the line ends with a semicolon, follow lines that begin /
         // end with operator tokens until we find a line without.
         if (/;\s*$/.test(line)) {
            
            var thisRow = row;
            var thisLine = this.getLineSansComments(doc, thisRow);

            // Try to indent for e.g.
            //
            //   x = 1
            //       + 2
            //       + 3;
            //   ^
            //
            // first.
            if (this.$heuristics.reStartsWithContinuationToken.test(thisLine)) {
               while (this.$heuristics.reStartsWithContinuationToken.test(thisLine)) {
                  thisRow--;
                  thisLine = this.getLineSansComments(doc, thisRow);
               }
               return this.$getIndent(thisLine);
            }

            // Now, try for
            //
            //   x = 1 +
            //       2 +
            //       3;
            //   ^
            thisRow--;
            thisLine = this.getLineSansComments(doc, thisRow);
            if (this.$heuristics.reEndsWithContinuationToken.test(thisLine)) {
               while (this.$heuristics.reEndsWithContinuationToken.test(thisLine)) {
                  // Short-circuit if we bump into e.g. 'case foo:' --
                  // this is so we don't walk too far past a ':', which is considered
                  // a continuation token. This is so cases like:
                  //
                  //   x = foo ?
                  //       bar :
                  //       baz;
                  //   ^
                  // can be indented correctly, without walking over a
                  //
                  //   case Foo:
                  //       bar;
                  //
                  // accidentally.
                  if (/^\s*case\b.*:\s*$/.test(thisLine)) {
                     return indent;
                  }
                  thisRow--;
                  thisLine = this.getLineSansComments(doc, thisRow);
               }
               return this.$getIndent(lines[thisRow + 1]);
            }
         }

         // Indent based on lookaround heuristics for open braces.
         if (!/^\s*$/.test(line) && /\{\s*$/.test(line)) {

            var heuristicRow = this.$heuristics.getRowForOpenBraceIndent(
               this.$session,
               row,
               20
            );

            if (heuristicRow !== null) {
               return this.$getIndent(lines[heuristicRow]) + tab;
            }

         }

         // If the closing character is a 'closer', then indent.
         // We do this so that we get indentation for a class ctor, e.g.
         //
         //   ClassCtor(int a, int b)
         //       ^
         //
         if (/\)\s*$/.test(line)) {
            return indent + tab;
         }

         // If the closing character is an 'opener' (ie, one of
         // '(', '{', '[', or '<'), then indent
         if (/[\(\{\[<]\s*$/.test(line)) {
            return indent + tab;
         }

         // Prefer indenting if the line ends with a character
         if (/\w\s*$/.test(line)) {
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
         return this.$r_outdent.autoOutdent(state, doc, row, this.codeModel);
      else
         return this.$outdent.autoOutdent(state, doc, row);
   };

}).call(Mode.prototype);

exports.Mode = Mode;
});
