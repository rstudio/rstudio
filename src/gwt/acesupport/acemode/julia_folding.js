/*
 * julia_folding.js
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

// Folding for Julia: blocks from their opening keyword to 'end', block
// comments, and multi-line brackets.
define("mode/julia_folding", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var Range = require("ace/range").Range;
var BaseFoldMode = require("ace/mode/folding/fold_mode").FoldMode;
var JuliaCode = require("mode/julia_code");

var FoldMode = exports.FoldMode = function() {};
oop.inherits(FoldMode, BaseFoldMode);

(function() {

   // a line opening a block comment that it doesn't close
   function opensBlockComment(line)
   {
      return /^\s*#=/.test(line) && line.indexOf("=#") === -1;
   }

   this.getFoldWidget = function(session, foldStyle, row)
   {
      var line = session.getLine(row);
      if (opensBlockComment(line))
         return "start";

      var info = JuliaCode.analyze(session.getTokens(row));
      if (JuliaCode.opensBlock(info.keywords, true) || info.brackets.length)
         return "start";

      return "";
   };

   this.getFoldWidgetRange = function(session, foldStyle, row)
   {
      var line = session.getLine(row);

      if (opensBlockComment(line))
      {
         for (var r = row + 1; r < session.getLength(); r++)
         {
            var index = session.getLine(r).indexOf("=#");
            if (index !== -1)
               return new Range(row, line.length, r, index);
         }
         return null;
      }

      var end = JuliaCode.findEnd(session, row);
      if (end)
         return new Range(row, line.length, end.row, end.column);

      var brackets = JuliaCode.analyze(session.getTokens(row)).brackets;
      if (brackets.length)
      {
         var bracket = brackets[brackets.length - 1];
         return this.openingBracketBlock(session, bracket.value, row, bracket.column);
      }

      return null;
   };

}).call(FoldMode.prototype);

});
