/*
 * token_cursor.js
 *
 * Copyright (C) 2020 by RStudio, PBC
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

define("mode/token_cursor", ["require", "exports", "module"], function(require, exports, module) {

var $reParenType = /(?:^|[.])paren(?:$|[.])/;

var oop = require("ace/lib/oop");
var Utils = require("mode/utils");
var TokenCursor = function(tokens, row, offset) {

   this.$tokens = tokens;
   this.$row = row || 0;
   this.$offset = offset || 0;

};

(function () {

   this.cloneCursor = function()
   {
      return new TokenCursor(
         this.$tokens,
         this.$row,
         this.$offset
      );
   };

   var isArray = Utils.isArray;
   var contains = Utils.contains;
   var construct = Utils.construct;

   var $complements = {
      "(" : ")",
      "{" : "}",
      "<" : ">",
      "[" : "]",
      ")" : "(",
      "}" : "{",
      ">" : "<",
      "]" : "["
   };

   var $leftBrackets  = ["(", "[", "{"];
   var $rightBrackets = [")", "]", "}"];

   this.moveToStartOfRow = function(row)
   {
      this.$row = row;
      this.$offset = 0;
   };

   this.moveToEndOfRow = function(row)
   {
      this.$row = row;
      var tokens = this.$tokens[row];
      if (tokens && tokens.length)
         this.$offset = tokens.length - 1;
      else
         this.$offset = 0;
   }

   // Move the cursor to the previous token. Returns true (and moves the
   // the cursor) on success; returns false (and does not move the cursor)
   // on failure.
   this.moveToPreviousToken = function()
   {
      // Bail if we're at the start of the document (protect against
      // invalid token cursors)
      if (this.$row <= 0 && this.$offset <= 0)
      {
         this.$row = 0;
         this.$offset = 0;
         return false;
      }

      // If the offset is greater than zero, we know we can safely
      // decrement it
      if (this.$offset > 0)
      {
         this.$offset--;
         return true;
      }

      // Otherwise, we keep walking back until we find a row containing
      // at least one token
      var row = this.$row - 1;
      var length = 0;
      while (row >= 0)
      {
         // Check to see if we have any tokens on this line --
         // if we do, accept that
         var rowTokens = this.$tokens[row];
         if (rowTokens && rowTokens.length !== 0)
         {
            length = rowTokens.length;
            break;
         }
         
         row--;
      }

      // If we reached a negative row, we failed (we were actually on
      // the first token)
      if (row < 0)
         return false;

      // Otherwise, we can set the row + offset and return true
      this.$row = row;
      this.$offset = length - 1;
      return true;
   };

   // Move the cursor to the next token. Returns true (and moves the
   // the cursor) on success; returns false (and does not move the cursor)
   // on failure.
   this.moveToNextToken = function(maxRow)
   {
      // If maxRow is undefined, we'll iterate up to the length of
      // the tokens array
      if (typeof maxRow === "undefined")
         maxRow = (this.$tokens || []).length;

      // If we're already past the maxRow bound, fail
      if (this.$row > maxRow)
         return false;

      // Tokenize ahead, if appropriate
      if (this.$tokens[this.$row] == null)
      {
         if (this.$codeModel &&
             this.$codeModel.$tokenizeUpToRow)
         {
            this.$codeModel.$tokenizeUpToRow.call(this.$codeModel, maxRow);
         }
      }

      // If the number of tokens on the current row is greater than
      // the offset, we can just increment and return true
      var rowTokens = this.$tokens[this.$row];
      if (rowTokens &&
          this.$offset < rowTokens.length - 1)
      {
         this.$offset++;
         return true;
      }

      // Otherwise, we walk up rows until we find the first row
      // containing a token. Note that we may need to check for
      // invalidated rows (ie, rows that are null rather than having
      // an empty array
      var row = this.$row + 1;
      while (row <= maxRow)
      {
         rowTokens = this.$tokens[row];
         if (rowTokens && rowTokens.length !== 0)
            break;
         row++;
      }

      if (row > maxRow)
         return false;

      this.$row = row;
      this.$offset = 0;
      return true;
      
   };
   
   this.peekBwd = function(n) {
      
      if (typeof n === "undefined") {
         n = 1;
      }
      
      var clone = this.cloneCursor();
      for (var i = 0; i < n; i++) {
         if (!clone.moveToPreviousToken()) {
            return clone.$invalidate();
         }
      }
      return clone;
   };

   this.peekFwd = function(n) {
      
      if (typeof n === "undefined") {
         n = 1;
      }
      
      var clone = this.cloneCursor();
      for (var i = 0; i < n; i++) {
         if (!clone.moveToNextToken()) {
            return clone.$invalidate();
         }
      }
      return clone;
   };

   this.$invalidate = function() {
      this.$row = 0;
      this.$column = 0;
      this.$tokens = [];
      return this;
   };

   this.seekToNearestToken = function(position, maxRow)
   {
      if (position.row > maxRow)
         return false;

      this.$row = position.row;
      var rowTokens = this.$tokens[this.$row] || [];
      for (this.$offset = 0; this.$offset < rowTokens.length; this.$offset++)
      {
         var token = rowTokens[this.$offset];
         if (token.column >= position.column)
         {
            return true;
         }
      }

      if (position.row < maxRow) {
         return this.moveToNextToken(maxRow);
      } else {
         return false;
      }
      
   };

   this.bwdToNearestToken = function(position)
   {
      this.$row = position.row;
      this.$offset = position.column;
      
      var rowTokens = this.$tokens[this.$row] || [];
      for (; this.$offset >= 0; this.$offset--)
      {
         var token = rowTokens[this.$offset];
         if (typeof token !== "undefined" && (token.column <= position.column))
         {
            return true;
         }
      }
      return this.moveToPreviousToken();
   };

   

   this.bwdToMatchingToken = function() {

      var thisValue = this.currentValue();
      if (!contains($rightBrackets, thisValue))
         return false;
      
      var compValue = $complements[thisValue];
      
      var success = false;
      var parenCount = 0;
      
      while (this.moveToPreviousToken())
      {
         var currentValue = this.currentValue();
         if (currentValue === compValue)
         {
            if (parenCount === 0)
            {
               return true;
            }
            parenCount--;
         }
         else if (currentValue === thisValue)
         {
            parenCount++;
         }
      }

      return false;
      
   };

   this.fwdToMatchingToken = function() {

      var thisValue = this.currentValue();
      if (!contains($leftBrackets, thisValue))
         return false;
      
      var compValue = $complements[thisValue];

      var success = false;
      var parenCount = 0;
      while (this.moveToNextToken())
      {
         var currentValue = this.currentValue();
         if (currentValue === compValue)
         {
            if (parenCount === 0)
            {
               return true;
            }
            parenCount--;
         }
         else if (currentValue === thisValue)
         {
            parenCount++;
         }
      }

      return false;
      
   };

   this.equals = function(other) {
      return this.$row === other.$row && this.$offset === other.$offset;
   };

   this.moveBackwardOverMatchingParens = function()
   {
      if (!this.moveToPreviousToken())
         return false;
      
      if (this.currentValue() !== ")") {
         this.moveToNextToken();
         return false;
      }

      var success = false;
      var parenCount = 0;
      while (this.moveToPreviousToken())
      {
         var currentValue = this.currentValue();
         if (currentValue === "(")
         {
            if (parenCount === 0)
            {
               success = true;
               break;
            }
            parenCount--;
         }
         else if (currentValue === ")")
         {
            parenCount++;
         }
      }
      return success;
   };

   this.findToken = function(predicate, maxRow)
   {
      do
      {
         var t = this.currentToken();
         if (t && predicate(t))
            return t;
      }
      while (this.moveToNextToken(maxRow));
      return null;
   };

   this.findTokenBwd = function(predicate, maxRow)
   {
      do
      {
         var t = this.currentToken();
         if (t && predicate(t))
            return t;
      }
      while (this.moveToPreviousToken(maxRow));
      return null;
   };

   this.currentToken = function()
   {
      var rowTokens = this.$tokens[this.$row];
      if (rowTokens == null)
         return {};

      var token = rowTokens[this.$offset];
      if (token == null)
         return {};

      return token;
   };

   this.currentValue = function()
   {
      return this.currentToken().value;
   };

   this.currentType = function()
   {
      return this.currentToken().type;
   };

   this.hasType = function(/*...*/)
   {
      var tokenType = this.currentType();
      if (tokenType == null)
         return false;

      for (var i = 0; i < arguments.length; i++) {
         var type = arguments[i];
         if (tokenType === type ||
             tokenType.indexOf(type + ".") !== -1 ||
             tokenType.indexOf("." + type) !== -1)
         {
            return true;
         }
      }
      return false;
   };

   this.currentPosition = function()
   {
      var token = this.currentToken();
      if (token == null)
         return null;
      else
         return {row: this.$row, column: token.column};
   };

   this.isFirstSignificantTokenOnLine = function()
   {
      return this.$offset === 0;
   };

   this.isLastSignificantTokenOnLine = function()
   {
      return this.$offset == (this.$tokens[this.$row] || []).length - 1;
   };

   this.bwdUntil = function(predicate) {
      while (!predicate(this)) {
         this.moveToPreviousToken();
      }
   };

   this.bwdWhile = function(predicate) {
      while (predicate(this)) {
         this.moveToPreviousToken();
      }
   };

   // Move a token cursor to a document position. This essentially
   // involves translating a '{row, column}' document position to a
   // '{row, offset}' position for a token cursor. Note that this
   // function _excludes_ the token directly at the cursor
   // position by default, e.g.
   //
   //     foo[a, b|]
   //            ^
   // Note that the cursor is 'on' the ']' above, but we intend to
   // move it onto the 'b' token instead. This is the more common
   // action throughout the code model and hence why it is the
   // default. (The intention is that only tokens immediately
   // preceding the cursor should affect indentation choices, and
   // so we should exclude anything on, or after, the cursor
   // itself)
   this.moveToPosition = function(pos, rightInclusive) {

      var row = pos.row;
      var column = pos.column;
      
      var rowTokens = this.$tokens[row];

      // Ensure that we have tokenized up to the current position,
      // if a code model is available.
      if (rowTokens == null &&
          this.$codeModel &&
          this.$codeModel.$tokenizeUpToRow)
      {
         this.$codeModel.$tokenizeUpToRow.call(this.$codeModel, row);
         rowTokens = this.$tokens[row];
      }

      // If there are tokens on this row, we can move to the first token
      // on that line before the cursor position.
      //
      // Note that we validate that there is at least one token
      // left of, or at, of the cursor position before entering
      // this block.
      if (rowTokens && rowTokens.length > 0 && rowTokens[0].column <= column)
      {
         // We want to find the index of the largest token column still less than
         // the column passed in by the caller.
         var index = 0;
         for (; index < rowTokens.length; index++)
         {
            if (rowTokens[index].column >= column)
            {
               break;
            }
         }

         this.$row = row;

         // It's possible for us to go too far, if the column passed
         // in is too large. In that case, we still want to move to the
         // final token on the line.
         if (index === rowTokens.length)
            this.$offset = index - 1;
         else if (rightInclusive && rowTokens[index].column === column)
            this.$offset = index;
         else
            this.$offset = index - 1;
         
         return true;
      }

      // Otherwise, we just move to the first token previous to this line.
      // Clone the cursor, put that cursor at the start of the row, and try
      // to find the previous token.
      var clone = this.cloneCursor();
      clone.$row = row;
      clone.$offset = 0;
      
      if (clone.moveToPreviousToken())
      {
         this.$row = clone.$row;
         this.$offset = clone.$offset;
         return true;
      }

      return false;
   };

   // Walk backwards to find an opening bracket (in the array 'tokens').
   // If 'failOnOpenBrace' is true and we encounter a '{', we give up and return
   // false.
   this.findOpeningBracket = function(tokens, failOnOpenBrace)
   {
      // 'tokens' can be passed in either as a single token, or
      // an array of tokens. If we don't have an array, convert it
      // to one.
      if (!isArray(tokens))
         tokens = [tokens];
      
      var clone = this.cloneCursor();

      do
      {
         if (clone.bwdToMatchingToken())
            continue;
         
         var currentValue = clone.currentValue();
         
         if (failOnOpenBrace && currentValue === "{")
            return false;

         for (var i = 0; i < tokens.length; i++)
         {
            if (currentValue === tokens[i])
            {
               this.$row = clone.$row;
               this.$offset = clone.$offset;
               return true;
            }
         }

      } while (clone.moveToPreviousToken());

      return false;
      
   };

   this.findOpeningBracketCountCommas = function(tokens, failOnOpenBrace)
   {
      if (!isArray(tokens))
         tokens = [tokens];
      
      var clone = this.cloneCursor();
      var commaCount = 0;
      
      do
      {
         if (clone.bwdToMatchingToken())
            continue;
         
         var currentValue = clone.currentValue();
         
         if (currentValue === ",")
            commaCount += 1;

         if (failOnOpenBrace && currentValue === "{")
            return -1;

         for (var i = 0; i < tokens.length; i++)
         {
            if (currentValue === tokens[i])
            {
               this.$row = clone.$row;
               this.$offset = clone.$offset;
               return commaCount;
            }
         }
         
      } while (clone.moveToPreviousToken());
      
      return -1;
   };

}).call(TokenCursor.prototype);


var CppTokenCursor = function(tokens, row, offset, codeModel) {
   this.$tokens = tokens;
   this.$row = row || 0;
   this.$offset = offset || 0;
   this.$codeModel = codeModel;
};
oop.mixin(CppTokenCursor.prototype, TokenCursor.prototype);

(function() {

   this.cloneCursor = function()
   {
      return new CppTokenCursor(
         this.$tokens,
         this.$row,
         this.$offset,
         this.$codeModel
      );
   };

   var contains = Utils.contains;

   // Move the tokne cursor backwards from an open brace over const, noexcept,
   // for function definitions.
   //
   // E.g.
   //
   //     int foo(int a) const noexcept(...) {
   //                    ^~~~~~~~~~~~~~~~~~~~^
   //
   // Places the token cursor on the first token following a closing paren.
   this.bwdOverConstNoexceptDecltype = function() {

      var clone = this.cloneCursor();
      if (clone.currentValue() !== "{") {
         return false;
      }

      // Move off of the open brace
      if (!clone.moveToPreviousToken())
         return false;

      // Try moving over a '-> decltype()'
      var cloneDecltype = clone.cloneCursor();
      if (cloneDecltype.currentValue() === ")") {
         if (cloneDecltype.bwdToMatchingToken()) {
            if (cloneDecltype.moveToPreviousToken()) {
               if (cloneDecltype.currentValue() === "decltype") {
                  if (cloneDecltype.moveToPreviousToken()) {
                     clone.$row = cloneDecltype.$row;
                     clone.$offset = cloneDecltype.$offset;
                  }
               }
            }
         }
      }
      
      // Try moving over a 'noexcept()'.
      var cloneNoexcept = clone.cloneCursor();
      if (cloneNoexcept.currentValue() === ")") {
         if (cloneNoexcept.bwdToMatchingToken()) {
            if (cloneNoexcept.moveToPreviousToken()) {
               if (cloneNoexcept.currentValue() === "noexcept") {
                  clone.$row = cloneNoexcept.$row;
                  clone.$offset = cloneNoexcept.$offset;
               }
            }
         }
      }

      // Try moving over a 'noexcept'.
      if (clone.currentValue() === "noexcept")
         if (!clone.moveToPreviousToken())
            return false;

      // Try moving over the 'const'
      if (clone.currentValue() === "const")
         if (!clone.moveToPreviousToken())
            return false;

      // Move back up one if we landed on the closing paren
      if (clone.currentValue() === ")")
         if (!clone.moveToNextToken())
            return false;

      this.$row = clone.$row;
      this.$offset = clone.$offset;
      return true;

   };

   this.bwdToMatchingArrow = function() {

      var thisValue = ">";
      var compValue = "<";

      if (this.currentValue() !== ">") return false;

      var success = false;
      var parenCount = 0;
      var clone = this.cloneCursor();
      while (clone.moveToPreviousToken())
      {
         if (clone.currentValue() === compValue)
         {
            if (parenCount === 0)
            {
               this.$row = clone.$row;
               this.$offset = clone.$offset;
               return true;
            }
            parenCount--;
         }
         else if (clone.currentValue() === thisValue)
         {
            parenCount++;
         }
      }

      return false;
      
   };

   // Move over a 'classy' specifier, e.g.
   //
   //     ::foo::bar<A, T>::baz<T>::bat
   //
   // This amounts to moving over identifiers, keywords and matching arrows.
   this.bwdOverClassySpecifiers = function() {

      var startValue = this.currentValue();
      if (startValue === ":" ||
          startValue === "," ||
          startValue === "{")
      {
         this.moveToPreviousToken();
      }

      do
      {
         if (this.bwdToMatchingArrow())
            this.moveToPreviousToken();

         var type = this.currentType();
         var value = this.currentValue();

         if (!(
            type === "keyword" ||
               value === "::" ||
               type === "identifier" ||
               type === "constant"
         ))
         {
            break;
         }

         if (value === "class" ||
             value === "struct" ||
             value === "enum" ||
             value === ":" ||
             value === ",")
         {
            return true;
         }
         
      } while (this.moveToPreviousToken());

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
   // The cursor is expected to start on the opening brace, and will
   // end on the opening ':' on success.
   this.bwdOverClassInheritance = function() {

      var clonedCursor = this.cloneCursor();
      return doBwdOverClassInheritance(clonedCursor, this);

   };

   var doBwdOverClassInheritance = function(clonedCursor, tokenCursor) {

      clonedCursor.bwdOverClassySpecifiers();

      // Check for a ':' or a ','
      var value = clonedCursor.currentValue();
      if (value === ",") {
         return doBwdOverClassInheritance(clonedCursor, tokenCursor);
      } else if (value === ":") {
         tokenCursor.$row = clonedCursor.$row;
         tokenCursor.$offset = clonedCursor.$offset;
         return true;
      }            

      return false;
      
   };

   // Move backwards over an initialization list, e.g.
   //
   //     Foo : a_(a),
   //           b_(b),
   //           c_(c) const noexcept() {
   //
   // The assumption is that the cursor starts on an opening brace.
   this.bwdOverInitializationList = function() {

      var clonedCursor = this.cloneCursor();
      return this.doBwdOverInitializationList(clonedCursor, this);

   };

   this.doBwdOverInitializationList = function(clonedCursor, tokenCursor) {

      // Move over matching parentheses -- note that this action puts
      // the cursor on the open paren on success.
      clonedCursor.moveBackwardOverMatchingParens();
      if (!clonedCursor.moveBackwardOverMatchingParens()) {
         if (!clonedCursor.moveToPreviousToken()) {
            return false;
         }
      }

      // Chomp keywords
      while (clonedCursor.currentType() === "keyword")
         if (!clonedCursor.moveToPreviousToken())
            return false;
      
      // Move backwards over the name of the element initialized
      if (clonedCursor.moveToPreviousToken()) {

         // Check for a ':' or a ','
         var value = clonedCursor.currentValue();
         if (value === ",") {
            return this.doBwdOverInitializationList(clonedCursor, tokenCursor);
         } else if (value === ":") {
            var prevValue = clonedCursor.peekBwd().currentValue();
            if (!contains(
               ["public", "private", "protected"],
               prevValue
            ))
            {
               tokenCursor.$row = clonedCursor.$row;
               tokenCursor.$offset = clonedCursor.$offset;
               return true;
            }
         }
      }

      return false;

   };
   
}).call(CppTokenCursor.prototype);

var RTokenCursor = function(tokens, row, offset, codeModel) {
   this.$tokens = tokens;
   this.$row = row || 0;
   this.$offset = offset || 0;
   this.$codeModel = codeModel;
};
oop.mixin(RTokenCursor.prototype, TokenCursor.prototype);

(function() {

   this.cloneCursor = function()
   {
      return new RTokenCursor(
         this.$tokens,
         this.$row,
         this.$offset,
         this.$codeModel
      );
   };

   var contains = Utils.contains;

   this.isValidAsIdentifier = function()
   {
      var type = this.currentType();
      return this.hasType("identifier", "constant") ||
             type === "symbol" ||
             type === "keyword" ||
             type === "string";
   };

   this.isExtractionOperator = function()
   {
      var value = this.currentValue();
      return value === "$" ||
             value === "@" ||
             value === "::" ||
             value === ":::";
   };

   // Find the start of the evaluation context for a generic expression,
   // e.g.
   //
   //     x[[1]]$foo[[1]][, 2]@bar[[1]]()
   //     ^~~~~~~~~~~~<~~~~~~~~~~~~~~~~~^
   this.findStartOfEvaluationContext = function() {
      
      var clone = this.cloneCursor();
      
      do
      {
         if (clone.bwdToMatchingToken())
            continue;
         
         // If we land on an identifier, we keep going if the token previous is
         // 'infix-y', and bail otherwise.
         if (clone.isValidAsIdentifier())
         {
            if (!clone.moveToPreviousToken())
               break;

            if (clone.isExtractionOperator())
               continue;
            
            if (!clone.moveToNextToken())
               return false;
            
            break;
            
         }

         // Fail if we get here as it implies we hit something not permissible
         // for the evaluation context
         return false;
         
      } while (clone.moveToPreviousToken());

      this.$row = clone.$row;
      this.$offset = clone.$offset;
      return true;
      
   };

   this.isLookingAtBinaryOp = function()
   {
      var type = this.currentType();
      return type === "keyword.operator" ||
             type === "keyword.operator.infix";
   };

   this.moveToStartOfCurrentStatement = function()
   {
      var clone = this.cloneCursor();
      while (clone.isLookingAtBinaryOp())
         if (!clone.moveToPreviousToken())
            return false;

      do
      {
         if (clone.bwdToMatchingToken())
            continue;
         
         // If we land on an identifier, we keep going if the token previous is
         // 'infix-y', and bail otherwise.
         if (clone.isValidAsIdentifier())
         {
            if (!clone.moveToPreviousToken())
               break;

            if (clone.isLookingAtBinaryOp())
            {
               while (clone.isLookingAtBinaryOp())
                  if (!clone.moveToPreviousToken())
                     return false;

               // Move back up one because the loop condition will take us back again
               if (!clone.moveToNextToken())
                  return false;

               continue;
            }
            
            if (!clone.moveToNextToken())
               return false;
            
            break;
            
         }

         // Fail if we get here as it implies we hit something not permissible
         // for the evaluation context
         return false;
         
      } while (clone.moveToPreviousToken());

      this.$row = clone.$row;
      this.$offset = clone.$offset;
      return true;
   };

   this.moveToEndOfCurrentStatement = function()
   {
      var clone = this.cloneCursor();
      while (clone.isLookingAtBinaryOp())
         if (!clone.moveToNextToken())
            return false;

      do
      {
         if (clone.fwdToMatchingToken())
            continue;

         if (clone.isValidAsIdentifier())
         {
            if (!clone.moveToNextToken())
               break;

            if (clone.isLookingAtBinaryOp())
            {
               while (clone.isLookingAtBinaryOp())
                  if (!clone.moveToNextToken())
                     return false;

               if (!clone.moveToPreviousToken())
                  return false;

               continue;
            }

            if (!clone.moveToPreviousToken())
               return false;

            break;
         }

         return false;
      } while (clone.moveToNextToken());

      this.$row = clone.$row;
      this.$offset = clone.$offset;
      return true;
   };

   function isSingleLineString(value)
   {
      if (value.indexOf("'") === 0)
         return value.lastIndexOf("'") === value.length - 1;
      else if (value.indexOf('"') === 0)
         return value.lastIndexOf('"') === value.length - 1;

      return false;
   }
   
   this.isSingleLineString = function()
   {
      return isSingleLineString(this.currentValue());
   };

   function isLeftBracket(bracket)
   {
      return bracket === '(' ||
             bracket === '[' ||
             bracket === '{';
   }

   this.isLeftBracket = function()
   {
      return isLeftBracket(this.currentValue());
   };

   function isRightBracket(bracket)
   {
      return bracket === ')' ||
             bracket === ']' ||
             bracket === '}';
   }

   this.isRightBracket = function()
   {
      return isRightBracket(this.currentValue());
   };

   // NOTE: A lot of the ugliness here stems from the fact that
   // both open and closing brackets have the same type; that is,
   //
   //    paren.***
   //
   this.isValidForEndOfStatement = function()
   {
      var type = this.currentType();
      var value = this.currentValue();

      if (type.search($reParenType) !== -1)
         return isRightBracket(value);

      return isSingleLineString(value) ||
             this.hasType("identifier", "constant", "variable");
   };

   this.isValidForStartOfStatement = function()
   {
      var type = this.currentType();
      var value = this.currentValue();

      if (type.search($reParenType) !== -1)
         return isLeftBracket(this.currentValue());

      var value = this.currentValue();
      return isSingleLineString(value) ||
             this.hasType("identifier", "constant", "variable");
   };

   // NOTE: By 'conditional' we mean following by a parenthetical
   // expression of some form
   this.isConditionalControlFlowKeyword = function()
   {
      var value = this.currentValue();
      return contains(
         ["if", "for", "while", "function"],
         value
      );
   };

   this.isControlFlowKeyword = function()
   {
      var value = this.currentValue();
      return contains(
         ["if", "for", "while", "else", "function",
          "repeat", "break", "next"],
         value
      );
   };

   this.isAtStartOfNewExpression = function(ifAtStartOfDocument)
   {
      var clone = this.cloneCursor();

      if (!clone.moveToPreviousToken())
         return ifAtStartOfDocument;

      if (this.isValidForStartOfStatement() &&
          clone.isValidForEndOfStatement() &&
          this.$row > clone.$row)
      {
         // If the previous token is a control flow keyword,
         // this is not a new expression (current cursor continues
         // previous expression)
         if (clone.isControlFlowKeyword())
            return false;
         
         // If the previous cursor is on a closing bracket,
         // ensure that it's not associated with control flow
         if (clone.currentValue() === ")" &&
             clone.bwdToMatchingToken() &&
             clone.moveToPreviousToken() &&
             clone.isConditionalControlFlowKeyword())
         {
            return false;
         }

         // Otherwise, these are separate statements.
         return true;
      }

      return false;
      
   };
   
}).call(RTokenCursor.prototype);


exports.TokenCursor = TokenCursor;
exports.CppTokenCursor = CppTokenCursor;
exports.RTokenCursor = RTokenCursor;

});


