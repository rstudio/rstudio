/*
 * julia_code.js
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// The block structure of Julia code, read from its tokens (so that keywords
// in strings and comments don't count): indentation and folding use it.
define("mode/julia_code", ["require", "exports", "module"], function(require, exports, module) {

// keywords opening a block that 'end' closes
var OPENERS = {
   "function": true, "macro": true, "module": true, "baremodule": true,
   "struct": true, "mutable struct": true, "abstract type": true,
   "primitive type": true, "if": true, "for": true, "while": true,
   "let": true, "begin": true, "quote": true, "try": true, "do": true
};

// keywords continuing an open block
var MIDDLES = { "else": true, "elseif": true, "catch": true, "finally": true };

// openers whose bodies are conventionally not indented
var UNINDENTED = { "module": true, "baremodule": true };

var OPEN_BRACKETS = "([{";
var CLOSE_BRACKETS = ")]}";

exports.isOpener = function(value) { return OPENERS.hasOwnProperty(value); };
exports.isMiddle = function(value) { return MIDDLES.hasOwnProperty(value); };

// The block keywords on a line, in order, each with its column; and the
// brackets the line leaves open (innermost last), each with its column.
exports.analyze = function(tokens)
{
   var keywords = [];
   var brackets = [];
   var closed = 0;
   var column = 0;

   for (var i = 0; i < tokens.length; i++)
   {
      var token = tokens[i];
      var value = token.value;
      var type = token.type || "";

      if (/\bparen\b/.test(type))
      {
         for (var j = 0; j < value.length; j++)
         {
            var ch = value.charAt(j);
            if (OPEN_BRACKETS.indexOf(ch) !== -1)
               brackets.push({ value: ch, column: column + j });
            else if (CLOSE_BRACKETS.indexOf(ch) !== -1 && brackets.length)
               brackets.pop();
            else if (CLOSE_BRACKETS.indexOf(ch) !== -1)
               closed++;
         }
      }
      else if (type === "keyword")
      {
         var inside = brackets.length ? brackets[brackets.length - 1].value : null;

         // in brackets, 'for' and 'if' belong to comprehensions and
         // generators; in square brackets, 'begin' and 'end' are indices
         var ignored =
            (inside !== null && (value === "for" || value === "if")) ||
            (inside === "[" && (value === "begin" || value === "end"));

         if (!ignored && (OPENERS[value] || MIDDLES[value] || value === "end"))
            keywords.push({ value: value, column: column });
      }

      column += value.length;
   }

   return { keywords: keywords, brackets: brackets, closedBrackets: closed };
};

// The number of blocks the keywords open, less those they close.
exports.netBlocks = function(keywords, countUnindented)
{
   var net = 0;
   for (var i = 0; i < keywords.length; i++)
   {
      var value = keywords[i].value;
      if (OPENERS[value] && (countUnindented || !UNINDENTED[value]))
         net++;
      else if (value === "end")
         net--;
   }
   return net;
};

// Does a line with these keywords leave a block open after it? A line that
// starts with 'else' (say) is inside its block, unless it also ends it.
exports.opensBlock = function(keywords, countUnindented)
{
   var net = exports.netBlocks(keywords, countUnindented);
   if (keywords.length && MIDDLES[keywords[0].value])
      net++;
   return net > 0;
};

// R Markdown chunk delimiters bound the search for a block's other end
function isChunkBoundary(line)
{
   return /^\s*(?:`{3,}|~{3,})/.test(line);
}

// The row of the opener of the block that an 'end' or 'else' (say) at the
// start of 'row' belongs to, or -1.
exports.findOpener = function(session, row)
{
   var depth = 0;
   for (var r = row - 1; r >= 0; r--)
   {
      if (isChunkBoundary(session.getLine(r)))
         return -1;

      var keywords = exports.analyze(session.getTokens(r)).keywords;
      for (var i = keywords.length - 1; i >= 0; i--)
      {
         var value = keywords[i].value;
         if (value === "end")
         {
            depth++;
         }
         else if (OPENERS[value])
         {
            if (depth === 0)
               return r;
            depth--;
         }
      }
   }
   return -1;
};

// The position of the 'end' closing the block that 'row' opens (or, for a
// line starting with 'else' and the like, continues), or null.
exports.findEnd = function(session, row)
{
   var keywords = exports.analyze(session.getTokens(row)).keywords;
   if (!exports.opensBlock(keywords, true))
      return null;

   var depth = exports.netBlocks(keywords, true);
   if (keywords.length && MIDDLES[keywords[0].value])
      depth++;

   var n = session.getLength();
   for (var r = row + 1; r < n; r++)
   {
      if (isChunkBoundary(session.getLine(r)))
         return null;

      keywords = exports.analyze(session.getTokens(r)).keywords;
      for (var i = 0; i < keywords.length; i++)
      {
         var value = keywords[i].value;
         if (OPENERS[value])
         {
            depth++;
         }
         else if (value === "end")
         {
            depth--;
            if (depth === 0)
               return { row: r, column: keywords[i].column };
         }
      }
   }
   return null;
};

});
