/*
 * token_cursor.js
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

define("mode/token_cursor", function(require, exports, module) {

var oop = require("ace/lib/oop");
var TokenCursor = function(tokens, row, offset) {

   this.$tokens = tokens;
   this.$row = row || 0;
   this.$offset = offset || 0;

};

(function () {

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

   this.moveToStartOfRow = function(row)
   {
      this.$row = row;
      this.$offset = 0;
   };

   this.moveToPreviousToken = function()
   {
      if (this.$row < 0) return false;
      
      if (this.$row === 0 && this.$offset === 0)
         return false;
      
      if (this.$row >= this.$tokens.length) {
         while (this.$row >= this.$tokens.length) {
            this.$row--;
         }
         this.$offset = this.$tokens[this.$row].length;
      }
      
      while (this.$offset === 0 && this.$row > 0)
      {
         this.$row--;
         this.$offset = this.$tokens[this.$row].length;
      }

      if (this.$offset === 0)
         return false;

      this.$offset--;

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

   this.moveToNextToken = function(maxRow)
   {
      var clone = this.cloneCursor();

      if (typeof maxRow === "undefined") {
         maxRow = Infinity;
      }
      
      if (clone.$row > maxRow)
         return false;

      clone.$offset++;

      while (clone.$row < clone.$tokens.length &&
             clone.$tokens[clone.$row] !== null &&
             clone.$offset >= clone.$tokens[clone.$row].length &&
             clone.$row < maxRow)
      {
         clone.$row++;
         clone.$offset = 0;
      }

      if (clone.$tokens[clone.$row] == null || clone.$tokens[clone.$row].length === 0)
         return false;

      if (clone.$row >= clone.$tokens.length)
         return false;

      if (clone.$offset >= clone.$tokens[clone.$row].length)
         return false;

      this.$row = clone.$row;
      this.$offset = clone.$offset;
      return true;
   };

   this.fwd = this.moveToNextToken;

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

   this.bwdToMatchingToken = function() {

      var thisValue = this.currentValue();
      var compValue = $complements[thisValue];

      var isCloser = [")", "}", "]"].some(function(x) {
         return x === thisValue;
      });

      if (!isCloser) {
         return false;
      }

      var success = false;
      var parenCount = 0;
      while (this.moveToPreviousToken())
      {
         if (this.currentValue() === compValue)
         {
            if (parenCount === 0)
            {
               return true;
            }
            parenCount--;
         }
         else if (this.currentValue() === thisValue)
         {
            parenCount++;
         }
      }

      return false;
      
   };

   this.fwdToMatchingToken = function() {

      var thisValue = this.currentValue();
      var compValue = $complements[thisValue];

      var isOpener = ["(", "{", "["].some(function(x) {
         return x === thisValue;
      });

      if (!isOpener) {
         return false;
      }

      var success = false;
      var parenCount = 0;
      while (this.moveToNextToken())
      {
         if (this.currentValue() === compValue)
         {
            if (parenCount === 0)
            {
               return true;
            }
            parenCount--;
         }
         else if (this.currentValue() === thisValue)
         {
            parenCount++;
         }
      }

      return false;
      
   };

   this.equals = function(other) {
      return this.$row === other.$row && this.$offset === other.$offset;
   };

   this.bwdToMatchingTokenShortCircuit = function(shortCircuit) {

      var thisValue = this.currentValue();
      var compValue = $complements[thisValue];

      var isCloser = [")", "}", "]", ">", "'", "\""].some(function(x) {
         return x === thisValue;
      });

      if (!isCloser) {
         return false;
      }

      var success = false;
      var parenCount = 0;
      while (this.moveToPreviousToken())
      {
         if (shortCircuit(this))
         {
            return false;
         }
         
         if (this.currentValue() === compValue)
         {
            if (parenCount === 0)
            {
               return true;
            }
            parenCount--;
         }
         else if (this.currentValue() === thisValue)
         {
            parenCount++;
         }
      }

      return false;
      
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
         if (this.currentValue() === "(")
         {
            if (parenCount === 0)
            {
               success = true;
               break;
            }
            parenCount--;
         }
         else if (this.currentValue() === ")")
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
      var token = (this.$tokens[this.$row] || [])[this.$offset];
      return typeof token === "undefined" ?
         {} :
         token;
   };

   this.currentValue = function()
   {
      return this.currentToken().value;
   };

   this.currentType = function()
   {
      return this.currentToken().type;
   };

   this.currentPosition = function()
   {
      var token = this.currentToken();
      if (token === null)
         return null;
      else
         return {row: this.$row, column: token.column};
   };

   this.cloneCursor = function()
   {
      return new this.constructor(this.$tokens, this.$row, this.$offset);
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

   this.moveToPosition = function(pos) {

      var rowTokens = this.$tokens[pos.row];

      // If there's no tokens on this line, walk back until we find
      // a line with tokens
      var row = pos.row;
      while (rowTokens == null || rowTokens.length === 0) {
         row--;
         rowTokens = this.$tokens[row];
      }

      if (row < 0)
         return false;

      if (row !== pos.row) {
         this.$row = row;
         this.$offset = this.$tokens[row].length - 1;
         return true;
      }

      for (var i = 0; i < rowTokens.length; i++) {
         if (rowTokens[i].column >= pos.column) {
            break;
         }
      }

      this.$row = pos.row;
      this.$offset = i - 1;
      return true;
      
   };
   
   
}).call(TokenCursor.prototype);


var CppTokenCursor = function(tokens, row, offset) {
   this.$tokens = tokens;
   this.$row = row || 0;
   this.$offset = offset || 0;
};
oop.mixin(CppTokenCursor.prototype, TokenCursor.prototype);

(function() {

   // Move the tokne cursor backwards from an open brace over const, noexcept,
   // for function definitions.
   //
   // E.g.
   //
   //     int foo(int a) const noexcept(...) {
   //                    ^~~~~~~~~~~~~~~~~~~~^
   //
   // Places the token cursor on the first token following a closing paren.
   this.bwdOverConstNoexcept = function() {

      var clone = this.cloneCursor();
      if (clone.currentValue() !== "{") {
         return false;
      }

      // Move off of the open brace
      if (!clone.moveToPreviousToken())
         return false;
      
      // Try moving over a 'noexcept()'.
      var cloneTwo = clone.cloneCursor();
      if (cloneTwo.currentValue() === ")") {
         if (cloneTwo.bwdToMatchingToken()) {
            if (cloneTwo.moveToPreviousToken()) {
               if (cloneTwo.currentValue() === "noexcept") {
                  clone.$row = cloneTwo.$row;
                  clone.$offset = cloneTwo.$offset;
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
      
      do {

         if (this.bwdToMatchingArrow()) {
            this.moveToPreviousToken();
         }

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
      while (clonedCursor.currentType() === "keyword") {
         
         if (!clonedCursor.moveToPreviousToken()) {
            return false;
         }
         
      }
      
      // Move backwards over the name of the element initialized
      if (clonedCursor.moveToPreviousToken()) {

         // Check for a ':' or a ','
         var value = clonedCursor.currentValue();
         if (value === ",") {
            return this.doBwdOverInitializationList(clonedCursor, tokenCursor);
         } else if (value === ":") {
            var prevValue = clonedCursor.peekBwd().currentValue();
            if (!["public", "private", "protected"].some(function(x) {
               return x === prevValue;
            }))
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

exports.TokenCursor = TokenCursor;
exports.CppTokenCursor = CppTokenCursor;

});


