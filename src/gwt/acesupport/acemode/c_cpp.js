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
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
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

var CppCodeModel = require("mode/cpp_code_model").CppCodeModel;

var getVerticallyAlignFunctionArgs = require("mode/r_code_model").getVerticallyAlignFunctionArgs;

var TokenCursor = require("mode/token_cursor").TokenCursor;

var Mode = function(suppressHighlighting, doc, session) {

   // Keep references to current session, document
   this.$session = session;
   this.$doc = doc;

   // R-related tokenization
   this.$r_tokenizer = new Tokenizer(new RHighlightRules().getRules());
   this.$r_outdent = {};
   oop.implement(this.$r_outdent, RMatchingBraceOutdent);
   this.$r_codeModel = new RCodeModel(doc, this.$r_tokenizer, /^r-/, /^\s*\/\*{3,}\s+[Rr]\s*$/);

   // C/C++ related tokenization
   this.$tokenizer = new Tokenizer(new c_cppHighlightRules().getRules());
   this.$codeModel = new CppCodeModel(this.$doc, this.$tokenizer);
   
   this.$behaviour = new CStyleBehaviour(this.$codeModel);
   this.$outdent = new MatchingBraceOutdent(this.$codeModel);
   
   this.$sweaveBackgroundHighlighter = new SweaveBackgroundHighlighter(
      session,
         /^\s*\/\*{3,}\s+[Rr]\s*$/,
         /^\s*\*\/$/,
      true
   );

   if (!window.NodeWebkit)     
      this.foldingRules = new CppStyleFoldMode();

   this.getLineSansComments = this.$codeModel.getLineSansComments;

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
         return this.$r_codeModel.getNextLineIndent(row, line, state, tab, tabSize);

      // Ask the R code model if we want to use vertical alignment
      var $verticallyAlignFunctionArgs = getVerticallyAlignFunctionArgs();

      var session = this.$session;
      var doc = session.getDocument();

      var indent = this.$getIndent(line);
      var unindent = this.$getUnindent(line, tabSize);
      
      var lines = doc.$lines;
      
      var prevLine;
      if (row > 0) {
         prevLine = lines[row - 1];
      } else {
         prevLine = "";
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
         var commentStartRow = this.$codeModel.findStartOfCommentBlock(lines, row, 200);
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

         // Don't indent after a preprocessor line
         if (/^\s*#\s*\S/.test(line)) {
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

         // If this line is just whitespace, match that indent. Otherwise
         // multiple presses of enter can send the cursor off into space.
         if (line.length > 0 && /^\s*$/.test(line)) {
            return this.$getIndent(line);
         }
         
         var cursor = session.getSelection().getCursor();

         // Choose indentation for the current line based on the position
         // of the cursor -- but make sure we only apply this if the
         // cursor is on the same row as the line being indented.
         //
         // Note that callers can set 'dontSubset' to avoid this behaviour;
         // this is desired for e.g. the 'reindent' function (which should
         // not take the position of the cursor into account)
         if (cursor && cursor.row == row && !dontSubset) {
            line = line.substring(0, cursor.column);
         }
         prevLine = this.getLineSansComments(doc, row - 1);

         // Only indent on an ending '>' if we're not in a template
         // We can do this by checking for a matching '<'. This also
         // handles system includes, e.g.
         //
         //   #include <header>
         //   ^
         if (/>\s*$/.test(line)) {
            if (this.$codeModel.$tokenUtils.$tokenizeUpToRow(row)) {
               var tokenCursor = new TokenCursor(this.$codeModel.$tokens, row, 0);
               if (tokenCursor.bwdToMatchingToken()) {
                  return this.$getIndent(lines[tokenCursor.$row]);
               }
            } else {
               return indent + tab;
            }
         }

         // Unindent after leaving a block comment.
         //
         // /**
         //  *
         //  */
         // ^
         if (/\*\/\s*$/.test(line)) {

            // Find the start of the comment block
            var blockStartRow = this.$codeModel.findStartOfCommentBlock(
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

         // Do something similar for '.' alignment, for chained
         // function calls:
         //
         //   foo.bar()
         //      .baz()
         //      .bam();
         //
         if (/^\s*\./.test(line)) {
            var currentRow = row - 1;
            while (/^\s*\./.test(lines[currentRow])) {
               currentRow--;
            }
            return this.$getIndent(lines[currentRow]);
         }

         // Don't indent for namespaces, switch statements.
         if (/\bnamespace\b.*\{\s*$/.test(line) ||
             /\bswitch\b.*\{\s*$/.test(line)) {
            return indent;
         }

         // Indent following an opening paren.
         // We prefer inserting two tabs here, reflecting the rules of
         // the Google C++ style guide:
         // http://google-styleguide.googlecode.com/svn/trunk/cppguide.html#Function_Declarations_and_Definitions
         if (line.match(/\(\s*$/)) {
            return indent + tab + tab;
         }

         // If we have a class with an open brace on the same line, indent
         //
         //   class Foo {
         //       ^
         //
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

         // If the line is entirely a string, then match that line's indent.
         if (/^\s*\".*\"\s*$/.test(line)) {
            return this.$getIndent(line);
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
         var bracePos = /([\[\{\(]).+,\s*$/.exec(line);
         if (bracePos) {

            // Loop through the openers until we find an unmatched brace on
            // the line
            var openers = ["(", "{", "["];
            for (var i = 0; i < openers.length; i++) {

               // Get the character alongside its complement
               var lChar = openers[i];
               var rChar = this.$codeModel.$complements[lChar];

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

         // Try token walking
         if (this.$codeModel.$tokenUtils.$tokenizeUpToRow(row + 1)) {

            var tokenCursor = new TokenCursor(
               this.$codeModel.$tokens,
               row,
               this.$codeModel.$tokens[row].length - 1
            );

            // If there is no token on this current line (this can occur when this code
            // is accessed by e.g. the matching brace offset code) then move back
            // to the previous row
            while (tokenCursor.$offset === -1 && tokenCursor.$row > 0) {
               tokenCursor.$row--;
               tokenCursor.$offset = tokenCursor.$tokens[tokenCursor.$row].length - 1;
            }

            // Set additional indent based on the first character
            var additionalIndent = "";

            // Keep track of where we started

            var startCursor = tokenCursor.cloneCursor();
            var startValue = startCursor.currentValue();
            var startType = startCursor.currentType();
            
            if (startType === "constant" ||
                startType === "keyword" || 
                ["{", ")", ">", ":"].some(function(x) {
                   return x === startValue;
                })) {
               additionalIndent = tab;
            }

            // Move over any initial semicolons
            while (tokenCursor.currentValue() === ";") {
               if (!tokenCursor.moveToPreviousToken()) {
                  break;
               }
            }

            var lastCursor = tokenCursor.cloneCursor();
            var walkedOverParens = false;

            // If the token cursor is on a comma...
            if (tokenCursor.currentValue() === ",") {

               // ... and the previous character is a ']', find its match for indentation.
               if ($verticallyAlignFunctionArgs) {
                  var peekOne = tokenCursor.peekBack();
                  if (peekOne.currentValue() === "]") {
                     if (peekOne.bwdToMatchingToken()) {
                        return new Array(peekOne.currentPosition().column + 1).join(" ");
                     }
                  }
               }

               // ... and there are more opening parens than closing on the line,
               // then vertically align
               if ($verticallyAlignFunctionArgs) {
                  var balance = line.split("(").length - line.split(")").length;
                  if (balance > 0) {
                     var parenMatch = line.match(/\(\s*(.)/);
                     if (parenMatch) {
                        return new Array(parenMatch.index + 1).join(" ");
                     }
                  }
               }

               // ... just return the indent of the current line
               return this.$getIndent(lines[row]);
            }

            // If the token cursor is on an operator, ident if the previous
            // token is not a class modifier token.
            if (startType === "keyword.operator" &&
                startValue !== ":") {
               return this.$getIndent(lines[row]) + tab;
            }

            // If we started on an opening '<' for a template, indent.
            if (startValue === "<") {
               if (startCursor.peekBack().currentValue() === "template") {
                  return this.$getIndent(lines[row]) + tab;
               }
            }

            while (true) {

               // Stop conditions:

               // The token cursor is undefined (we moved past the start of the
               // document)
               if (typeof tokenCursor.currentValue() === "undefined") {
                  if (typeof lastCursor.currentValue() !== "undefined") {
                     return this.$getIndent(lines[lastCursor.$row]) + additionalIndent;
                  }
                  return additionalIndent;
               }

               lastCursor = tokenCursor.cloneCursor();

               // We hit a semi-colon -- use the first token after that semi-colon.
               if (tokenCursor.currentValue() === ";") {
                  if (tokenCursor.moveToNextToken()) {
                     return this.$getIndent(lines[tokenCursor.$row]) + additionalIndent;
                  }
               }

               // We hit a 'control flow' keyword ...
               if (["for", "while", "do", "try"].some(function(x) {
                  return x === tokenCursor.currentValue();
               }))
               {
                  // ... and the first token wasn't a semi-colon, then indent
                  if (startValue !== ";") {
                     return this.$getIndent(lines[tokenCursor.$row]) + additionalIndent;
                  }
                  
               }

               // We hit a ':'...
               var peekOne = tokenCursor.peekBack();
               if (tokenCursor.currentValue() === ":") {

                  // ... preceeded by a class access modifier
                  if (["public", "private", "protected"].some(function(x) {
                     return x === peekOne.currentValue();
                  }))
                  {
                     // Indent once relative to the 'public:'s indentation.
                     return this.$getIndent(lines[peekOne.$row]) + tab;
                  }

                  // ... with a line starting with 'case'
                  var maybeCaseLine = lines[tokenCursor.$row];
                  if (/^\s*case/.test(maybeCaseLine)) {
                     return this.$getIndent(maybeCaseLine) + tab;
                  }

                  // ... opening an initialization list
                  if (peekOne.currentValue() === ")") {
                     var clone = peekOne.cloneCursor();
                     if (clone.bwdToMatchingToken()) {

                        var peek1 = clone.peekBack(1);
                        var peek2 = clone.peekBack(2);

                        if (
                           (peek1 !== null && peek1.currentType() === "identifier") &&
                           (peek2 !== null && !/\boperator\b/.test(peek2.currentType()))
                        )
                        {
                           
                           return this.$getIndent(lines[clone.peekBack().$row]) + additionalIndent;
                        }
                     }
                  }
               }

               // We hit a '[]()' lambda expression.
               if (tokenCursor.currentValue() === "]" &&
                   tokenCursor.peekFwd().currentValue() === "(") {
                  var clone = tokenCursor.cloneCursor();
                  if (clone.bwdToMatchingToken()) {
                     return this.$getIndent(lines[clone.$row]) + additionalIndent;
                  }
               }

               // Vertical alignment for e.g. 'for ( ... ;'.
               //
               // NOTE: Any ')' token found with a match _will have been jumped over_,
               // so we can assume that any opening token found does not have a match.
               if (tokenCursor.currentValue() === "(" &&
                   peekOne.currentValue() === "for" &&
                   startValue === ";")
               {
                  
                  // Find the matching paren for the '(' after the cursor
                  var lookaheadCursor = tokenCursor.peekFwd().cloneCursor();

                  return $verticallyAlignFunctionArgs ?
                     new Array(tokenCursor.peekFwd().currentPosition().column + 1).join(" ") :
                     this.$getIndent(lines[tokenCursor.peekFwd().$row]) + tab;
                     
               }

               // We hit an 'if'
               if (tokenCursor.currentValue() === "if" ||
                   tokenCursor.currentValue() === "else") {
                  return this.$getIndent(lines[tokenCursor.$row]) + additionalIndent;
               }

               // We hit 'template <'
               if (tokenCursor.currentValue() === "template" &&
                   tokenCursor.peekFwd().currentValue() === "<")
               {
                  return this.$getIndent(lines[tokenCursor.$row]);
               }

               // We hit an '{'
               if (tokenCursor.currentValue() === "{") {

                  var openBraceIndentRow = this.$codeModel.getRowForOpenBraceIndent(session, tokenCursor.$row);
                  if (openBraceIndentRow >= 0) {
                     
                     // Don't indent if the brace is on the same line as a 'namespace' token
                     var line = this.getLineSansComments(doc, openBraceIndentRow);
                     var indent = this.$getIndent(line);
                     
                     return /\bnamespace\b/.test(line) ?
                        indent :
                        indent + tab;
                     
                  } else {
                     return this.$getIndent(lines[tokenCursor.$row]) + tab;
                  }
               }

               // We hit a preprocessor token
               if (/\bpreproc\b/.test(tokenCursor.currentType())) {
                  return this.$getIndent(lines[tokenCursor.$row]);
               }

               // We're at the start of the document
               if (tokenCursor.$row === 0 && tokenCursor.$offset === 0) {
                  return this.$getIndent(lines[0]) + additionalIndent;
               }

               // Walking:

               // Step over parens. Walk over '>' only if the next token
               // is a 'class' or 'struct'.
               if ([")", "}", "]"].some(function(x) {
                  return x === tokenCursor.currentValue();
               }) ||
                   (tokenCursor.currentValue() === ">" &&
                    tokenCursor.peekFwd().currentType() === "keyword"))
               {
                  if (tokenCursor.bwdToMatchingToken()) {
                     if (tokenCursor.currentValue() === "(") {
                        walkedOverParens = true;
                     }
                  }
               }

               tokenCursor.moveToPreviousToken();
            }
            
         }

         // Indent based on lookaround heuristics for open braces.
         if (/\{\s*$/.test(line)) {

            var heuristicRow = this.$codeModel.getRowForOpenBraceIndent(
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

         // Prefer indenting if the closing character is a letter.
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
         return this.$r_outdent.autoOutdent(state, doc, row, this.$r_codeModel);
      else
         return this.$outdent.autoOutdent(state, doc, row);
   };

}).call(Mode.prototype);

exports.Mode = Mode;
});
