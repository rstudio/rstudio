/*
 * cpp_code_model.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

define("mode/cpp_code_model", function(require, exports, module) {

var oop = require("ace/lib/oop");
var Range = require("ace/range").Range;
var TokenUtils = require("mode/token_utils").TokenUtils;
var TokenIterator = require("ace/token_iterator").TokenIterator;
var CppTokenCursor = require("mode/token_cursor").CppTokenCursor;

var getVerticallyAlignFunctionArgs = require("mode/r_code_model").getVerticallyAlignFunctionArgs;

var CppCodeModel = function(session, tokenizer, statePattern, codeBeginPattern) {

   this.$session = session;
   this.$doc = session.getDocument();
   this.$tokenizer = tokenizer;

   this.$tokens = new Array(this.$doc.getLength());
   this.$statePattern = statePattern;
   this.$codeBeginPattern = codeBeginPattern;

   this.$tokenUtils = new TokenUtils(
      this.$doc,
      this.$tokenizer,
      this.$tokens,
      this.$statePattern,
      this.$codeBeginPattern
   );

   var that = this;
   this.$doc.on('change', function(evt) {
      that.$onDocChange.apply(that, [evt]);
   });
   
};

(function() {

   var debugCursor = function(message, cursor) {
      // console.log(message);
      // console.log(cursor);
      // console.log(cursor.currentToken());
   };

   var controlFlowKeywords = [
      "if", "else", "for", "do", "while", "struct", "class", "try",
      "catch", "switch"
   ];

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

   this.allIndicesOf = function(string, character) {
      var result = [];
      for (var i = 0; i < string.length; i++) {
         if (string[i] == character) {
            result.push(i);
         }
      }
      return result;
   };

   // Heuristic for finding a matching '>'.
   //
   // We attempt to find matches for '<' and '>' where:
   //
   // 1. '>' occurs at the beginning of the line, and
   // 2. '<' occurs at the end of the line.
   //
   // Primarily intended for template contexts, e.g.
   //
   // template <                     <-- returns this row
   //     int RTYPE
   // >
   //
   // ^                              <-- want to align with that line
   this.getRowForMatchingEOLArrows = function(session, doc, row) {
      var maxLookback = 100;
      var balance = 0;
      var thisLine = "";
      for (var i = 1; i < maxLookback; i++) {
         thisLine = this.getLineSansComments(doc, row - i);

         // Small escape hatch -- break if we encounter a line ending with
         // a semi-colon since that should never happen in template contexts
         if (/;\s*$/.test(thisLine))
            break;
         
         if (/<\s*$/.test(thisLine) && !/<<\s*$/.test(thisLine)) {
            if (balance === 0) {
               return row - i;
            } else {
               balance--;
            }
         } else if (/^\s*>/.test(thisLine) && !/^\s*>>/.test(thisLine)) {
            balance++;
         }
      }
      
      return -1;
      
   };

   var reStartsWithDefine = /^\s*#\s*define/;
   var reEndsWithBackslash = /\\\s*$/;

   // NOTE: We need to be careful of comment block starts and ends. (/*, */)
   var reStartsWithContinuationToken = /^\s*[+\-/&^%$!<\>.?|=~]|^\s*\*[^/]|^\s*\/[^\*]/;
   var reEndsWithContinuationToken =       /[+\-*&^%$!<\>.?|=~]\s*$|\*[^/]\s*$|\/[^\*]\s*$/;

   var reContinuation = function(x) {
      return reStartsWithContinuationToken.test(x) ||
         reEndsWithContinuationToken.test(x);
   };

   var charCount = function(string, character) {
      return string.split(character).length - 1;
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

   // Given a row with a '{', we look back for the row that provides
   // the start of the scope, for purposes of indentation. We look back
   // for:
   //
   // 1. A class token, or
   // 2. A constructor with an initializer list.
   //
   // Return 'null' if no row could be found, and the corresponding row
   // otherwise.
   this.getRowForOpenBraceIndent = function(session, row, useCursor) {

      var doc = session.getDocument();
      var lines = doc.$lines;
      if (lines.length <= 1) return -1;

      var line = lines[row];

      // Walk tokens backwards until we find something that provides
      // the appropriate indentation.
      if (this.$tokenUtils.$tokenizeUpToRow(row)) {

         try {

            // Remove any trailing '\' tokens, then reapply them. This way, indentation
            // will work even in 'macro mode'.
            var tokens = new Array(this.$tokens.length);

            for (var i = 0; i < this.$tokens.length; i++) {
               if (this.$tokens[i] != null &&
                   this.$tokens[i].length > 0) {
                  var rowTokens = this.$tokens[i];
                  if (rowTokens[rowTokens.length - 1].value === "\\") {
                     tokens[i] = this.$tokens[i].splice(rowTokens.length - 1, 1)[0];
                  }
               } 
            }

            var tokenCursor = new CppTokenCursor(this.$tokens);
            if (useCursor) {
               var cursor = session.getSelection().getCursor();
               tokenCursor.moveToPosition(cursor);
            } else {
               tokenCursor.$row = row;

               var i = tokenCursor.$tokens[row].length - 1;
               for (var i = tokenCursor.$tokens[row].length - 1;
                    i >= 0;
                    i--)
               {
                  tokenCursor.$offset = i;
                  if (tokenCursor.currentValue() === "{") {
                     break;
                  }
               }
            }
            if (tokenCursor.currentValue() !== "{") {
               return -1;
            }

            if (tokenCursor.peekBack().currentValue() === "{") {
               return -1;
            }
            
            // Move backwards over matching parens. Note that we may need to walk up
            // e.g. a constructor's initialization list, so we need to check for
            //
            //     , a_(a)
            //
            // so we need to look two tokens backwards to see if it's a
            // comma or a colon.
            debugCursor("Before moving over initialization list", tokenCursor);
            tokenCursor.bwdOverInitializationList();

            debugCursor("Before moving over class inheritance", tokenCursor);
            tokenCursor.bwdOverClassInheritance();

            // If we didn't walk over anything previously, the cursor
            // will still be on the same '{'.  Walk backwards one token.
            if (tokenCursor.currentValue() === "{") {
               if (!tokenCursor.moveToPreviousToken()) {
                  return -1;
               }
            }

            // Bail if we encountered a '{'
            if (tokenCursor.currentValue() === "{") {
               return -1;
            }

            // Move backwards over any keywords.
            debugCursor("Before walking over keywords", tokenCursor);
            while (tokenCursor.currentType() === "keyword") {

               // Return on 'control flow' keywords.
               var value = tokenCursor.currentValue();
               if (controlFlowKeywords.some(function(x) { return x === value; }))
               {
                  return tokenCursor.$row;
               }

               if (tokenCursor.$row === 0 && tokenCursor.$offset === 0) {
                  return tokenCursor.$row;
               }
               
               if (!tokenCursor.moveToPreviousToken()) {
                  return -1;
               }
            }

            // Move backwards over matching parens.
            debugCursor("Before walking over matching parens", tokenCursor);

            // If we landed on a ':' token and the previous token is
            // e.g. public, then we went too far -- go back up one token.
            if (tokenCursor.currentValue() === ":") {

               var prevValue = tokenCursor.peekBack().currentValue();
               if (["public", "private", "protected"].some(function(x) {
                  return x === prevValue;
               }))
               {
                  tokenCursor.moveToNextToken();
                  return tokenCursor.$row;
               }
            }

            if (tokenCursor.currentValue() === ":") {

               // We want to walk over specifiers preceeding the ':' which may
               // specify an initializer list. We need to walk e.g.
               //
               //    const foo) const noexcept(bar) :
               //
               // so we do this by jumping parens and keywords, stopping once
               // we hit an actual identifier.
               while (tokenCursor.moveToPreviousToken()) {

                  if (tokenCursor.bwdToMatchingToken()) {

                     if (tokenCursor.peekBack().currentType() === "keyword") {
                        continue;
                     } else {
                        break;
                     }
                  }

                  if (tokenCursor.currentType() === "identifier")
                     break;
               }

            }

            if (tokenCursor.currentValue() === ")") {
               if (!tokenCursor.bwdToMatchingToken()) {
                  return -1;
               }
            }

            if (tokenCursor.currentValue() === "(") {
               if (!tokenCursor.moveToPreviousToken()) {
                  return -1;
               }
            }

            // Use this row for indentation.
            debugCursor("Ended at", tokenCursor);
            if (tokenCursor.currentValue() === "=") {
               if (tokenCursor.moveToPreviousToken()) {
                  return tokenCursor.$row;
               }
            }

            return tokenCursor.$row;
            
         } finally {

            for (var i = 0; i < tokens.length; i++) {
               if (typeof tokens[i] !== "undefined") {
                  this.$tokens[i].push(tokens[i]);
               }
            }
            
         }

      }

      // Give up
      return -1;
      
   };

   var getRegexIndices = function(regex, line) {

      var match = null;
      var indices = [];
      while ((match = regex.exec(line))) {
         indices.push(match.index);
      }
      return indices;
   };

   // Get a line, with comments (following '//') stripped. Also strip
   // a trailing '\' anticipating e.g. macros.
   this.getLineSansComments = function(doc, row) {

      if (row < 0) {
         return "";
      }
      
      var line = doc.getLine(row);

      // Strip quotes before stripping comments -- this is to avoid
      // problems with e.g.
      //
      //   int foo("// comment");
      //
      // Note that we preserve the quotes themselves, e.g. post strip
      // the line would appear as:
      //
      //   int foo("");
      //
      // as this allows other heuristics to still work fine.
      var indices = getRegexIndices(/(?!\\)\"/g, line);

      if (indices.length > 0 && indices.length % 2 === 0) {

         for (var i = 0; i < indices.length / 2; i = i + 2) {

            var start = indices[i];
            var end = indices[i + 1];

            line = line.substring(0, start + 1) +
                   line.substring(end, line.length);
         }
      }

      // Strip out a trailing line comment
      var index = line.indexOf("//");
      if (index != -1) {
         line = line.substring(0, index);
      }

      // Strip off a trailing '\' -- this is mainly done
      // for macro mode (so we get regular indentation rules)
      if (reEndsWithBackslash.test(line)) {
         line = line.substring(0, line.lastIndexOf("\\"));
      }

      return line;
      
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
      return -1;
   };

   this.getNextLineIndent = function(row, line, state, tab, tabSize, dontSubset) {

      // Ask the R code model if we want to use vertical alignment
      var $verticallyAlignFunctionArgs = getVerticallyAlignFunctionArgs();

      var session = this.$session;
      var doc = session.getDocument();

      // If we went back too far, use the first row for indentation.
      if (row === -1) {
         var lineZero = doc.getLine(0);
         if (lineZero.length > 0) {
            return this.$getIndent(lineZero);
         } else {
            return "";
         }
      }

      // If the line is blank, try looking back for indentation.
      if (line.length === 0) {
         return this.getNextLineIndent(row - 1, doc.getLine(row - 1), state, tab, tabSize, dontSubset);
      }

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
         var commentStartRow = this.findStartOfCommentBlock(lines, row, 200);
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

         // If this line is just whitespace, match that line's indent. This
         // ensures multiple enter keypresses can blast the cursor off into
         // space.
         if (typeof line !== "string") {
            return "";
         } else if (line.length === 0 ||
                    /^\s*$/.test(line))
         {
            return this.$getIndent(lines[row]);
         }

         // Unindent after leaving a block comment.
         //
         // /**
         //  *
         //  */
         // ^
         if (/\*\/\s*$/.test(line)) {

            // Find the start of the comment block
            var blockStartRow = this.findStartOfCommentBlock(
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
         //
         // Need some special handling for e.g.
         //
         //   class Foo::Bar : public A
         //                  ^
         //
         var match = line.match(/(^\s*(?:class|struct)\s+.*\w[^:]):[^:]\s*.+/);
         if (match && !/,\s*/.test(line)) {
            return $verticallyAlignFunctionArgs ?
               new Array(match[1].length + 1).join(" ") :
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

         // Indent for lines ending with a '<'.
         if (/<\s*$/.test(line)) {
            return indent + tab;
         }

         // If the line is entirely a string, then match that line's indent.
         if (/^\s*\".*\"\s*$/.test(line)) {
            return indent;
         }

         // Don't indent for templates e.g.
         //
         //     template < ... >
         if (/^\s*template\s*<.*>\s*$/.test(line) &&
             line.split(">").length == line.split("<").length) {
            return indent;
         }

         // If the line is just a '>', try looking back for an opening '<'.
         var templateArrowRow = this.getRowForMatchingEOLArrows(session, doc, row);
         if (templateArrowRow >= 0)
            return this.$getIndent(lines[templateArrowRow]);

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
         var bracePos = /([\[\{\(<]).+,\s*$/.exec(line);
         if (bracePos) {

            // Loop through the openers until we find an unmatched brace on
            // the line
            var openers = ["(", "{", "[", "<"];
            for (var i = 0; i < openers.length; i++) {

               // Get the character alongside its complement
               var lChar = openers[i];
               var rChar = this.$complements[lChar];

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

         // If this line begins, or ends, with an operator token alongside the previous,
         // then just use this line's indentation. This ensures that we match the indentation
         // for continued lines, e.g.
         //
         //     a +
         //         b +
         //         ^
         //
         var i = row - 1;
         var prevLineNotWhitespace = prevLine;
         while (i >= 0 && /^\s*/.test(prevLineNotWhitespace)) {
            prevLineNotWhitespace = this.getLineSansComments(doc, i);
            i--;
         }

         if (reContinuation(line) && reContinuation(prevLineNotWhitespace)) {
            return this.$getIndent(line);
         }

         // Try token walking
         if (this.$tokenUtils.$tokenizeUpToRow(row + 2)) {

            try {
               
               // Remove any trailing '\' tokens, then reapply them. This way, indentation
               // will work even in 'macro mode'.
               var tokens = new Array(this.$tokens.length);

               for (var i = 0; i < this.$tokens.length; i++) {
                  if (this.$tokens[i] != null &&
                      this.$tokens[i].length > 0) {
                     var rowTokens = this.$tokens[i];
                     if (rowTokens[rowTokens.length - 1].value === "\\") {
                        tokens[i] = this.$tokens[i].splice(rowTokens.length - 1, 1)[0];
                     }
                  } 
               }

               var tokenCursor = new CppTokenCursor(
                  this.$tokens,
                  row,
                  this.$tokens[row].length - 1
               );

               // If 'dontSubset' is false, then we want to plonk the token cursor
               // on the first token before the cursor.
               if (!dontSubset) {
                  var rowTokens = this.$tokens[row];
                  if (rowTokens != null && rowTokens.length > 0) {
                     for (var i = 0; i < rowTokens.length; i++) {
                        var tokenColumn = rowTokens[i].column;
                        if (tokenColumn >= cursor.column) {
                           break;
                        }
                     }
                     if (i > 0) {
                        tokenCursor.$offset = i - 1;
                     }
                  }
               }

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
                   startType === "identifier" ||
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
                        var parenMatch = line.match(/.*?\(\s*(\S)/);
                        if (parenMatch) {
                           return new Array(parenMatch[0].length).join(" ");
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

                  // We hit a colon ':'...
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

                  // Alignment for e.g.
                  // int foo(int
                  //
                  //             ^
                  if ($verticallyAlignFunctionArgs) {
                     if (tokenCursor.currentValue() === "(" &&
                         !tokenCursor.isLastSignificantTokenOnLine())
                     {
                        tokenCursor.moveToNextToken();
                        return new Array(tokenCursor.currentPosition().column + 1 + tabSize).join(" ");
                        
                     }
                  }

                  // We hit an 'if' or an 'else'
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

                     var openBraceIndentRow = this.getRowForOpenBraceIndent(session, tokenCursor.$row);
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

                  // Step over parens. Walk over '>' only if we can
                  // find its match to be associated with a 'template'.
                  if (tokenCursor.currentValue() === ">")
                  {
                     var clone = tokenCursor.cloneCursor();
                     if (clone.bwdToMatchingArrow()) {
                        if (clone.peekBack().currentValue() === "template") {
                           if (startValue === ">") additionalIndent = "";
                           return this.$getIndent(lines[clone.$row]) + additionalIndent;
                        }
                     }
                  }
                  
                  tokenCursor.bwdToMatchingToken();
                  tokenCursor.moveToPreviousToken();
               }

            } finally {

               for (var i = 0; i < tokens.length; i++) {
                  if (typeof tokens[i] !== "undefined") {
                     this.$tokens[i].push(tokens[i]);
                  }
               }

            }
            
         }

      } // start state rules

      return indent;
   };

   this.$onDocChange = function(evt)
   {
      var delta = evt.data;

      if (delta.action === "insertLines")
      {
         this.$tokenUtils.$insertNewRows(delta.range.start.row,
                             delta.range.end.row - delta.range.start.row);
      }
      else if (delta.action === "insertText")
      {
         if (this.$doc.isNewLine(delta.text))
         {
            this.$tokenUtils.$invalidateRow(delta.range.start.row);
            this.$tokenUtils.$insertNewRows(delta.range.end.row, 1);
         }
         else
         {
            this.$tokenUtils.$invalidateRow(delta.range.start.row);
         }
      }
      else if (delta.action === "removeLines")
      {
         this.$tokenUtils.$removeRows(delta.range.start.row,
                          delta.range.end.row - delta.range.start.row);
         this.$tokenUtils.$invalidateRow(delta.range.start.row);
      }
      else if (delta.action === "removeText")
      {
         if (this.$doc.isNewLine(delta.text))
         {
            this.$tokenUtils.$removeRows(delta.range.end.row, 1);
            this.$tokenUtils.$invalidateRow(delta.range.start.row);
         }
         else
         {
            this.$tokenUtils.$invalidateRow(delta.range.start.row);
         }
      }

   };

   this.$getIndent = function(line)
   {
      var match = /^([ \t]*)/.exec(line);
      if (!match)
         return ""; // should never happen, but whatever
      else
         return match[1];
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

      // Otherwise, try to remove up to 'tabSize' number of spaces
      var numLeadingSpaces = 0;
      for (var i = 0; i < tabSize && i < indent.length; i++) {
         if (indent[i] === " ") {
            numLeadingSpaces++;
         }
      }
      
      return indent.substring(numLeadingSpaces, indent.length);
      
   };

   
}).call(CppCodeModel.prototype);

exports.CppCodeModel = CppCodeModel;

});

