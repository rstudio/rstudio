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

var Range = require("ace/range").Range;
var TokenUtils = require("mode/token_utils").TokenUtils;
var TokenIterator = require("ace/token_iterator").TokenIterator;
var TokenCursor = require("mode/token_cursor").TokenCursor;

var CppCodeModel = function(doc, tokenizer, statePattern, codeBeginPattern) {
   
   this.$doc = doc;
   this.$tokenizer = tokenizer;

   this.$tokens = new Array(doc.getLength());
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

   var reStartsWithComma        = /^\s*,/;
   var reStartsWithColon        = /^\s*:/;
   var reStartsWithCommaOrColon = /^\s*[,:]/;

   var reOnlyWhitespace = /^\s*$/;

   // NOTE: We need to be careful of comment block starts and ends. (/*, */)
   var reStartsWithContinuationToken = /^\s*[,+\-/&^%$!<\>.?|=\'\":\)\(~]|^\s*\*[^/]|^\s*\/[^\*]/;
   var reEndsWithContinuationToken =       /[,+\-*&^%$!<\>.?|=\'\":\)\(~]\s*$|\*[^/]\s*$|\/[^\*]\s*$/;

   // Attach to 'this' so others can use it
   this.reStartsWithContinuationToken = reStartsWithContinuationToken;
   this.reEndsWithContinuationToken   = reEndsWithContinuationToken;

   // All of the common control block generating tokens in their 'naked' form --
   // ie, without an associated open brace on the same line.
   this.reNakedBlockTokens = {
      "do": /^\s*do\s*$/,
      "while": /^\s*while\s*\(.*\)\s*$/,
      "for": /^\s*for\s*\(.*\)\s*$/,
      "else": /^\s*else\s*$/,
      "if": /^\s*if\s*\(.*\)\s*$/,
      "elseif": /^\s*else\s+if\s*\(.*\)\s*$/
   };
   
   this.reNakedMatch = function(x) {
      for (var key in this.reNakedBlockTokens) {
         if (this.reNakedBlockTokens[key].test(x)) {
            return true;
         }
      }
      return false;
   };

   var charCount = function(string, character) {
      return string.split(character).length - 1;
   };
   
   var reEndsWithComma     = /,\s*$|,\s*\/\//;
   var reEndsWithColon     = /:\s*$|:\s*\/\//;
   var reClassOrStruct     = /\bclass\b|\bstruct\b/;
   var reEndsWithBackslash = /\\\s*$/;

   var reStartsWithOpenBrace = /\s*\{/;

   // Move backwards over an initialization list, e.g.
   //
   //     Foo : a_(a),
   //           b_(b),
   //           c_(c) const noexcept() {
   //
   // The assumption is that the cursor starts on an opening brace.
   var bwdOverInitializationList = function(tokenCursor) {

      var clonedCursor = tokenCursor.cloneCursor();
      while (doBwdOverInitializationList(clonedCursor, tokenCursor)) {
      }
      
      return tokenCursor;
   };

   var doBwdOverInitializationList = function(clonedCursor, tokenCursor) {

      // Move over matching parentheses -- note that this action puts
      // the cursor on the open paren on success.
      clonedCursor.moveBackwardOverMatchingParens();
      if (!clonedCursor.moveBackwardOverMatchingParens()) {
         if (!clonedCursor.moveToPreviousToken()) {
            return false;
         }
      }

      // Chomp keywords
      while (clonedCursor.currentType() === "keyword") {
         
         if (!clonedCursor.moveToPreviousToken()) {
            return false;
         }
         
      }
      
      // Move backwards over the name of the element initialized
      if (clonedCursor.moveToPreviousToken()) {

         // Check for a ':' or a ','
         var value = clonedCursor.currentValue();
         if (value === ":" || value === ",") {

            tokenCursor.$row = clonedCursor.$row;
            tokenCursor.$offset = clonedCursor.$offset;
            return true;
               
         }
      }

      return false;

   };

   // Move backwards over class inheritance.
   //
   // This moves the cursor backwards over any inheritting classes,
   // e.g.
   //
   //     class Foo :
   //         public A,
   //         public B {
   //
   // The cursor is expected to start on the opening brace.
   var bwdOverClassInheritance = function(tokenCursor) {

      var clonedCursor = tokenCursor.cloneCursor();
      return doBwdOverClassInheritance(clonedCursor, tokenCursor);

   };

   var doBwdOverClassInheritance = function(clonedCursor, tokenCursor) {

      // Move off of the open brace or comma
      if (!clonedCursor.moveToPreviousToken()) {
         return false;
      }

      // Jump over constants
      if (clonedCursor.currentType() === "constant") {
         if (!clonedCursor.moveToPreviousToken()) {
            return false;
         }
      }

      // Jump over '<>' pair if necessary -- this is for inheritance
      // from template classes, e.g.
      //
      //     class Foo :
      //         public A<T1, T2>
      //
      if (clonedCursor.currentValue() === ">") {

         if (!clonedCursor.bwdToMatchingToken()) {
            return false;
         }

         if (!clonedCursor.moveToPreviousToken()) {
            return false;
         }
         
      }

      // Move backwards over the name of the element initialized
      if (clonedCursor.moveToPreviousToken()) {

         // Chomp through '::' tokens and their associated
         // identifiers
         while (clonedCursor.currentValue() === "::") {

            if (!clonedCursor.moveToPreviousToken()) {
               return false;
            }

            if (clonedCursor.currentType() === "constant") {
               if (!clonedCursor.moveToPreviousToken()) {
                  return false;
               }
            }

            // Jump over '<>' pairs
            if (clonedCursor.currentValue() === ">") {

               if (!clonedCursor.bwdToMatchingToken()) {
                  return false;
               }

               if (!clonedCursor.moveToPreviousToken()) {
                  return false;
               }
            }

            // Chomp keywords
            while (clonedCursor.currentType() === "keyword") {
               if (!clonedCursor.moveToPreviousToken()) {
                  return false;
               }
            }

            // Move backwards over any more identifiers
            if (clonedCursor.currentType() === "identifier") {
               if (!clonedCursor.moveToPreviousToken()) {
                  return false;
               }
            }
         }

         // Chomp keywords
         while (clonedCursor.currentType() === "keyword") {
            if (!clonedCursor.moveToPreviousToken()) {
               return false;
            }
         }

         // Check for a ':' or a ','
         var value = clonedCursor.currentValue();
         if (value === ",") {
            return doBwdOverClassInheritance(clonedCursor, tokenCursor);
         } else if (value === ":") {
            tokenCursor.$row = clonedCursor.$row;
            tokenCursor.$offset = clonedCursor.$offset;
            return true;
         } else {
            return false;
         }
      }

      return false;
      
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
      if (lines.length <= 1) return -1;

      var line = lines[row];

      // Walk tokens backwards until we find something that provides
      // the appropriate indentation.
      if (this.$tokenUtils.$tokenizeUpToRow(row)) {

         // The brace should be the last token on the line -- place it there.
         // But the behaviour rules might insert a closing '}' or ';', so we'll
         // also try to walk back tokens to get the opening brace.
         //
         // This is necessary for the auto-outdenting -- it will see a line with
         // e.g. '{};'
         // and we want to select the '{' token on that line.
         var tokenCursor = new TokenCursor(this.$tokens);

         tokenCursor.$row = row;
         tokenCursor.$offset = this.$tokens[row].length - 1;

         // Try to find an open brace on this line (okay if we fail)
         while (tokenCursor.currentValue() !== "{") {

            if (tokenCursor.$row !== row) {
               return -1;
            }
            
            if (!tokenCursor.moveToPreviousToken()) {
               return -1;
            }

            if (tokenCursor.$offset < 0) {
               break;
            }

            if (typeof tokenCursor.currentValue() === "undefined") {
               break;
            }

         }
      
         // Move backwards over matching parens. Note that we may need to walk up
         // e.g. a constructor's initialization list, so we need to check for
         //
         //     , a_(a)
         //
         // so we need to look two tokens backwards to see if it's a
         // comma or a colon.
         debugCursor("Before moving over initialization list", tokenCursor);
         bwdOverInitializationList(tokenCursor);

         debugCursor("Before moving over class inheritance", tokenCursor);
         bwdOverClassInheritance(tokenCursor);

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
            if (["if", "else", "for", "while", "do", "struct", "class"].some(function(x) { return x === value; })) {
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
            }
            else
            {
               tokenCursor.moveToPreviousToken();
            }
         }

         if (tokenCursor.currentValue() === ":") {

            if (!tokenCursor.moveToPreviousToken())
               return -1;

            // We want to walk over specifiers preceeding the ':' which may
            // specify an initializer list. We need to walk e.g.
            //
            //    const foo) const noexcept(bar) :
            //
            // so we do this by jumping parens and keywords, stopping once
            // we hit an actual identifier.
            do {

               if (tokenCursor.currentValue() === ")") {
                  if (tokenCursor.bwdToMatchingToken()) {

                     if (tokenCursor.peekBack().currentType() === "keyword") {
                        continue;
                     } else {
                        break;
                     }
                  }
               }

               if (tokenCursor.currentType() === "identifier")
                  break;

            } while (tokenCursor.moveToPreviousToken());

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
            
            line = line.substring(0, start - 1) +
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

   // Find the row for which a matching character for the character
   // 'character' is on. 'lines' is the set of lines to search
   // through, 'row' is the row to begin the search on, and
   // 'maxLookaround' gives the maximum number of lines we should look
   // over. Direction gives the direction and should be 'forward' or
   // 'backward', and defaults to 'backward'.  Returns -1 is nothing
   // is found. 'balance' should be left undefined but can be
   // specified optionally if desired.
   this.findMatchingBracketRow = function(character, doc, row,
                                          maxLookaround, direction) {

      direction = typeof direction !== 'undefined' ? direction : "backward";
      return this.doFindMatchingBracketRow(character, doc, row,
                                           maxLookaround, direction,
                                           0, 0);
      
   };
   
   this.doFindMatchingBracketRow = function(character, doc, row,
                                            maxLookaround, direction,
                                            balance, count, shortCircuit) {

      if (count > maxLookaround) return -1;
      if (row < 0 || row > doc.$lines.length - 1) return -1;

      var line = this.getLineSansComments(doc, row);

      if (typeof shortCircuit === "function") {
         if (shortCircuit(line)) return row;
      }

      var nChar = line.split(character).length - 1;
      var nComp = line.split(this.$complements[character]).length - 1;

      balance = balance + nChar - nComp;

      if (balance <= 0) {
         return row;
      }

      if (direction === "backward") {
         row = row - 1;
      } else if (direction === "forward") {
         row = row + 1;
      } else {
         row = row - 1;
      }

      return this.doFindMatchingBracketRow(character, doc, row,
                                           maxLookaround, direction, balance,
                                           count + 1, shortCircuit);
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

   this.indentNakedTokens = function(doc, indent, tab, row, line) {

      if (typeof line === "undefined") {
         line = this.getLineSansComments(doc, row);
      }

      // Generic 'naked-looking' tokens -- e.g.
      //
      //   BOOST_FOREACH()
      //       ^
      var reNaked = /^\s*[\w_:]+\s*$|^\s*[\w_:]+\s*\(.*\)\s*$/;

      // First, check for an indentation
      if (
         (charCount(line, "(") === charCount(line, ")")) &&
         (reNaked.test(line) || this.reNakedMatch(line))
      ) {
         return indent + tab;
      }

      // If the line ends with a semicolon, try walking up naked
      // block generating tokens
      var lastLine = this.getLineSansComments(doc, row - 1);

      if (/;\s*$/.test(line) && (reNaked.test(lastLine) || this.reNakedMatch(lastLine))) {

         // Quit if we hit a class access modifier -- this is
         // a workaround for walking over e.g.
         //
         //   public:
         //       foo () {};
         //       ^
         if (/^\s*public\s*:\s*$|^\s*private\s*:\s*$|^\s*protected\s*:\s*$/.test(lastLine)) {
            return indent;
         }

         var lookbackRow = row - 1;
         while (reNaked.test(lastLine) || this.reNakedMatch(lastLine)) {

            // Quit if we encountered an 'if' or 'else'
            if (this.reNakedBlockTokens["if"].test(lastLine) ||
                this.reNakedBlockTokens["else"].test(lastLine) ||
                this.reNakedBlockTokens["elseif"].test(lastLine)) {
               return lookbackRow;
            }
            lookbackRow--;
            lastLine = this.getLineSansComments(doc, lookbackRow);
         }
         
         return lookbackRow + 1;
         
      }

      return null;

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
   
   
}).call(CppCodeModel.prototype);

exports.CppCodeModel = CppCodeModel;

});

