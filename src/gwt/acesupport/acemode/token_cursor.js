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

   this.peekBack = function(n) {
      var clone = this.cloneCursor();
      for (var i = 0; i < n; i++) {
         if (!clone.moveToPreviousToken()) {
            return null;
         }
      }
      return clone;
   };

   this.moveToNextToken = function(maxRow)
   {
      if (this.$row > maxRow)
         return false;

      this.$offset++;

      while (this.$offset >= this.$tokens[this.$row].length && this.$row < maxRow)
      {
         this.$row++;
         this.$offset = 0;
      }

      if (this.$offset >= this.$tokens[this.$row].length)
         return false;

      return true;
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

   this.bwdToMatchingToken = function() {

      var thisValue = this.currentValue();
      var compValue = $complements[thisValue];

      var isCloser = [")", "}", "]", ">"].some(function(x) {
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
            if (parenCount == 0)
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
            if (parenCount == 0)
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
      return new TokenCursor(this.$tokens, this.$row, this.$offset);
   };

   this.isFirstSignificantTokenOnLine = function()
   {
      return this.$offset == 0;
   };

   this.isLastSignificantTokenOnLine = function()
   {
      return this.$offset == (this.$tokens[this.$row] || []).length - 1;
   };
   
}).call(TokenCursor.prototype);


exports.TokenCursor = TokenCursor;
});


