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
var Utils = require("mode/utils");

(function() {

   this.clone = function()
   {
      var clone = new TokenIterator(this.$session, 0, 0);
      clone.moveToTokenIterator(this);
      return clone;
   };

   this.moveToTokenIterator = function(tokenIterator)
   {
      for (var key in tokenIterator)
         if (tokenIterator.hasOwnProperty(key))
            this[key] = tokenIterator[key];
   };

   this.$peek = function(offset, mover)
   {
      var clone = this.clone();
      var token;
      for (var i = 0; i < offset; i++)
         token = mover.call(clone, offset);
      return token;
   };

   this.peekFwd = function(offset)
   {
      return this.$peek(offset, this.stepForward);
   };

   this.peekBwd = function(offset)
   {
       return this.$peek(offset, this.stepBackward);
   };

   this.getCurrentTokenValue = function()
   {
      return this.getCurrentToken().value;
   };

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

   this.fwdToMatchingToken = function()
   {
      var token = this.getCurrentToken();
      if (Utils.isOpeningBracket(token.value, true)) {
         return $moveToMatchingToken(
               this,
               function(token) { return token.value;  },
               this.stepForward,
               token.value,
               Utils.getComplement(token.value)
         );

      } else if (token.type === "support.function.codebegin") {
         return $moveToMatchingToken(
               this,
               function(token) { return token.type; },
               this.stepForward,
               "support.function.codebegin",
               "support.function.codeend"
         );
      }
      return false;
   };

   this.bwdToMatchingToken = function()
   {
      var token = this.getCurrentToken();
      if (Utils.isClosingBracket(token.value, true)) {
         return $moveToMatchingToken(
               this,
               function(token) { return token.value; },
               this.stepBackward,
               token.value,
               Utils.getComplement(token.value)
         );

      } else if (token.type === "support.function.codeend") {
         return $moveToMatchingToken(
               this,
               function(token) { return token.type; },
               this.stepBackward,
               "support.function.codeend",
               "support.function.codebegin"
         );
      }
      return false;
   };

}).call(TokenIterator.prototype);

});
