/*
 * julia.js
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

// The editor mode for Julia: highlighting, indentation, folding, comments.
define("mode/julia", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var Range = require("ace/range").Range;
var JuliaHighlightRules = require("mode/julia_highlight_rules").JuliaHighlightRules;
var JuliaFoldMode = require("mode/julia_folding").FoldMode;
var JuliaCode = require("mode/julia_code");

var Mode = function(suppressHighlighting, session) {
   this.$session = session;
   this.HighlightRules = JuliaHighlightRules;
   this.foldingRules = new JuliaFoldMode();

   // pair brackets and quotes as typed (see $quotes)
   this.$behaviour = this.$defaultBehaviour;
};
oop.inherits(Mode, TextMode);

(function() {

   this.lineCommentStart = "#";
   this.blockComment = { start: "#=", end: "=#" };

   // A quote after a name or a closing bracket is the adjoint operator (A')
   // more often than a character literal begins, so only double quotes and
   // backticks are paired.
   this.$quotes = { '"': '"', "`": "`" };

   this.getLanguageMode = function(position)
   {
      return "Julia";
   };

   // The indent of the opener of the block that 'end' (or 'else', say) at
   // the start of 'row' belongs to; one level less than 'indent' when that
   // can't be found.
   this.$getOpenerIndent = function(row, indent, tab)
   {
      if (this.$session && row != null)
      {
         var opener = JuliaCode.findOpener(this.$session, row);
         if (opener !== -1)
            return this.$getIndent(this.$session.getLine(opener));
      }

      return indent.length >= tab.length ? indent.substring(tab.length) : "";
   };

   this.getNextLineIndent = function(state, line, tab, row)
   {
      // Ace's Enter doesn't pass the row; by then the cursor is on the new
      // line below it (as in r_code_model.js)
      if (typeof row !== "number" && this.$session)
         row = this.$session.getSelection().getCursor().row - 1;

      var indent = this.$getIndent(line);
      var tokens = this.getTokenizer().getLineTokens(line, state).tokens;
      var info = JuliaCode.analyze(tokens);
      var keywords = info.keywords;

      // a line starting with 'end' or 'else' (say) is about to be outdented
      // (see autoOutdent); follow where it is going rather than where it is
      var first = keywords.length && keywords[0].column === indent.length ? keywords[0].value : null;
      if (first === "end" || JuliaCode.isMiddle(first))
         indent = this.$getOpenerIndent(row, indent, tab);

      if (JuliaCode.opensBlock(keywords, false) || info.brackets.length)
         indent += tab;

      return indent;
   };

   // Outdent a line starting with 'end', 'else', 'elseif', 'catch' or
   // 'finally' once the word is complete, and a closing bracket typed first
   // on a line.
   this.checkOutdent = function(state, line, input)
   {
      if (/^\s*$/.test(line) && /^[\)\]\}]/.test(input))
         return true;

      return /^[\s;#(]/.test(input) &&
             /^\s*(?:end|else|elseif|catch|finally)$/.test(line);
   };

   this.autoOutdent = function(state, session, row)
   {
      var line = session.getLine(row);

      // a closing bracket goes where the line with its opening one starts
      var bracket = /^(\s*)[\)\]\}]/.exec(line);
      if (bracket)
      {
         var opening = session.findMatchingBracket({ row: row, column: bracket[1].length + 1 });
         if (opening && opening.row !== row)
         {
            var openingIndent = this.$getIndent(session.getLine(opening.row));
            session.replace(new Range(row, 0, row, bracket[1].length), openingIndent);
         }
         return;
      }

      var match = /^(\s*)(?:end|else|elseif|catch|finally)\b/.exec(line);
      if (!match)
         return;

      // the word must be the keyword, not text in a string or comment
      var token = session.getTokenAt(row, match[1].length + 1);
      if (!token || token.type !== "keyword")
         return;

      var opener = JuliaCode.findOpener(session, row);
      if (opener === -1)
         return;

      var indent = this.$getIndent(session.getLine(opener));
      if (indent !== match[1])
         session.replace(new Range(row, 0, row, match[1].length), indent);
   };

   this.transformAction = function(state, action, editor, session, param)
   {
      return false;
   };

   this.$id = "mode/julia";

}).call(Mode.prototype);

exports.Mode = Mode;

});
