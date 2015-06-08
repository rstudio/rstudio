/*
 * token_iterator.js
 *
 * Copyright (C) 2014 by RStudio, Inc.
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

// Mixins for the Ace TokenIterator 'class'.
define("mixins/token_iterator", function(require, exports, module) {

var TokenIterator = require("ace/token_iterator").TokenIterator;
var Range = require("ace/range").Range;

(function() {

   function isOpeningBracket(string, allowArrows)
   {
      return string.length === 1 && (
         string === "{" ||
         string === "(" ||
         string === "[" ||
         (!!allowArrows && string === "<"));
   }

   function isClosingBracket(string, allowArrows)
   {
      return string.length === 1 && (
         string === "}" ||
         string === ")" ||
         string === "]" ||
         (!!allowArrows && string === ">"));
   }

   var $complements = {

      "(" : ")",
      "{" : "}",
      "[" : "]",
      "<" : ">",

      ")" : "(",
      "}" : "{",
      "]" : "[",
      ">" : "<"
   };

   function getComplement(string)
   {
      return $complements[string];
   }

   this.moveToPreviousToken = function()
   {
      // First, check to see if we can use a token on the same row.
      var rowTokens = this.$rowTokens;
      var newIdx = this.$tokenIndex - 1;
      if (newIdx >= 0)
      {
         this.$tokenIndex--;
         return rowTokens[newIdx];
      }

      // Otherwise, walk back rows until we find a row
      // with tokens. Once we find one, put the iterator
      // at the last token on the row and return that token.
      var session = this.$session;
      var row = this.$row;
      
      if (row < 0)
         return null;

      while (true)
      {
         row--;
         if (row < 0)
            return null;
         
         rowTokens = session.getTokens(row);
         if (rowTokens && rowTokens.length)
         {
            this.$row = row;
            this.$tokenIndex = rowTokens.length - 1;
            this.$rowTokens = rowTokens;
            return rowTokens[rowTokens.length - 1];
         }
      }

      return null;
      
   };

   this.moveToNextToken = function()
   {
      // Check to see if we can use a token on the same row.
      var rowTokens = this.$rowTokens;
      var newIdx = this.$tokenIndex + 1;
      if (newIdx < rowTokens.length)
      {
         this.$tokenIndex++;
         return rowTokens[newIdx];
      };

      // Otherwise, walk up rows until we find a row with tokens.
      // Once found, set the token iterator to the first token on
      // that line, and return that token.
      var session = this.$session;
      var max = session.getLength();
      var row = this.$row;
      if (row >= max)
         return null;

      while (true)
      {
         row++;
         if (row >= max)
            return null;

         rowTokens = session.getTokens(row);
         if (rowTokens && rowTokens.length)
         {
            this.$row = row;
            this.$tokenIndex = 0;
            this.$rowTokens = rowTokens;
            return rowTokens[0];
         }
      }

      return null;
      
   };

   this.moveToStartOfRow = function()
   {
      this.$tokenIndex = 0;
      return this.getCurrentToken();
   };

   this.moveToEndOfRow = function()
   {
      this.$tokenIndex = this.$rowTokens.length - 1;
      return this.getCurrentToken();
   };

   this.moveToStartOfNextRowWithTokens = function()
   {
      var row = this.$row;
      var session = this.$session;
      var max = session.getLength();
      while (true)
      {
         row++;
         if (row >= max)
            return null;
         
         var tokens = session.getTokens(row);
         if (tokens && tokens.length)
         {
            this.$row = row;
            this.$tokenIndex = 0;
            this.$rowTokens = tokens;
            return this.getCurrentToken();
         }
      }      
   };

   /**
    * Move a TokenCursor to the token lying at position.
    * If no such token exists at that position, then we instead
    * move to the first token lying previous to that token.
    */
   this.moveToPosition = function(position)
   {
      // Try to get a token at the position supplied.
      var token = this.$session.getTokenAt(position.row, position.column);
 
      // If no token was returned, place a token cursor at the first
      // cursor previous to that token.
      //
      // Based on some simple testing, we can see that:
      //
      //    session.getToken(0, -100) returns the first token,
      //    session.getToken(0, 1000) returns null
      //
      // And so a 'null' result implies that we specified a column that was
      // too large.
      if (token == null) {
         
         // Temporarily move to the first token on the next row.
         // It's okay if this doesn't actually exist.
         this.$row = position.row + 1;
         this.$tokenIndex = 0;
         this.$rowTokens = this.$session.getTokens(this.$row);

         // Move to the previous token.
         return this.moveToPreviousToken();
      }

      // Otherwise, just set the indices to match that token.
      this.$row = position.row,
      this.$rowTokens = this.$session.getTokens(this.$row);
      this.$tokenIndex = token.index;
      return this.getCurrentToken();
   };

   /**
    * Clones the current token iterator. The clone
    * keeps a reference to the same underlying session.
    */
   this.clone = function()
   {
      var clone = new TokenIterator(this.$session, 0, 0);
      clone.moveToTokenIterator(this);
      return clone;
   };

   /**
    * Move a token iterator to the same position as a
    * separate token iterator.
    */
   this.moveToTokenIterator = function(tokenIterator)
   {
      for (var key in tokenIterator)
         if (tokenIterator.hasOwnProperty(key))
            this[key] = tokenIterator[key];
   };

   /**
    * Get the token lying `offset` tokens ahead of
    * the token iterator. Returns `null` if no such
    * token exists.
    */
   this.peekFwd = function(offset)
   {
      var clone = this.clone();
      var token = null;
      for (var i = 0; i < offset; i++)
         token = clone.moveToNextToken();
      return token;
   };

   /**
    * Get the token lying `offset` tokens behind
    * the token iterator. Returns `null` if no such
    * token exists.
    */
   this.peekBwd = function(offset)
   {
      var clone = this.clone();
      var token = null;
      for (var i = 0; i < offset; i++)
         token = clone.moveToPreviousToken();
      return token;
   };

   /**
    * Get the value of the token at the TokenIterator's
    * current position.
    */
   this.getCurrentTokenValue = function()
   {
      return this.getCurrentToken().value;
   };

   /**
    * Get the document position of the token at the
    * TokenIterator's current position.
    */
   this.getCurrentTokenPosition = function()
   {
      return {
         row: this.getCurrentTokenRow(),
         column: this.getCurrentTokenColumn()
      };
   };

   this.getCurrentTokenRange = function()
   {
      var start = this.getCurrentTokenPosition();
      var end = {
         row: start.row,
         column: start.column + this.getCurrentToken().value.length
      };

      return Range.fromPoints(start, end);
   };

   function $moveToMatchingToken(cursor, getter, mover, lhs, rhs)
   {
      var balance = 1;
      var token;
      var entity;

      var clone = cursor.clone();

      while ((token = mover.call(clone))) {
         entity = getter(token);
         if (entity === rhs) {
            balance--;
            if (balance === 0) {
               cursor.moveToTokenIterator(clone);
               return true;
            }
         } else if (entity === lhs) {
            balance++;
         }
      }

      return false;

   }

   /**
    * Move forward to the 'matching' token for the current token.
    * This amounts to moving from an opening bracket to the matching
    * closing bracket (if found), or moving forward to a token with
    * a matching type.
    */
   this.fwdToMatchingToken = function()
   {
      var token = this.getCurrentToken();
      if (isOpeningBracket(token.value, true)) {
         return $moveToMatchingToken(
               this,
               function(token) { return token.value;  },
               this.moveToNextToken,
               token.value,
               getComplement(token.value)
         );

      } else if (token.type === "support.function.codebegin") {
         return $moveToMatchingToken(
               this,
               function(token) { return token.type; },
               this.moveToNextToken,
               "support.function.codebegin",
               "support.function.codeend"
         );
      }
      return false;
   };

   this.bwdToMatchingToken = function()
   {
      var token = this.getCurrentToken();
      if (isClosingBracket(token.value, true)) {
         return $moveToMatchingToken(
               this,
               function(token) { return token.value; },
               this.moveToPreviousToken,
               token.value,
               getComplement(token.value)
         );

      } else if (token.type === "support.function.codeend") {
         return $moveToMatchingToken(
               this,
               function(token) { return token.type; },
               this.moveToPreviousToken,
               "support.function.codeend",
               "support.function.codebegin"
         );
      }
      return false;
   };

}).call(TokenIterator.prototype);

});
