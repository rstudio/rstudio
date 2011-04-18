/*
 * r.js
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
define("mode/r", function(require, exports, module)
{

   var Range = require("ace/range").Range;
   var oop = require("pilot/oop");
   var TextMode = require("ace/mode/text").Mode;
   var Tokenizer = require("ace/tokenizer").Tokenizer;
   var TextHighlightRules = require("ace/mode/text_highlight_rules")
         .TextHighlightRules;
   var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;

   var MatchingBraceOutdent = function() {};

   (function() {
      this.checkOutdent = function(line, input) {
         if (! /^\s+$/.test(line))
            return false;

         return /^\s*[)}]/.test(input);
      };

      this.autoOutdent = function(doc, row) {
         var line = doc.getLine(row);
         var match = line.match(/^(\s*[)}])/);

         if (!match) return 0;

         var column = match[1].length;
         var openBracePos = doc.findMatchingBracket({row: row, column: column});

         if (!openBracePos || openBracePos.row == row) return 0;

         var indent = this.$getIndent(doc.getLine(openBracePos.row));
         doc.replace(new Range(row, 0, row, column-1), indent);
      };

      this.$getIndent = function(line) {
         var match = line.match(/^(\s+)/);
         if (match) {
            return match[1];
         }

         return "";
      };
   }).call(MatchingBraceOutdent.prototype);

   var Mode = function(suppressHighlighting)
   {
      if (suppressHighlighting)
         this.$tokenizer = new Tokenizer(new TextHighlightRules().getRules());
      else
         this.$tokenizer = new Tokenizer(new RHighlightRules().getRules());
      this.$outdent = new MatchingBraceOutdent();
   };
   oop.inherits(Mode, TextMode);

   (function()
   {
      this.getNextLineIndent = function(state, line, tab, bgTokenizer, row)
      {
         var indent = this.$getIndent(line);

         var startState = row == 0 ? "start" : bgTokenizer.getState(row-1);
         var tokenizedLine = this.$tokenizer.getLineTokens(line, startState);
         var tokens = tokenizedLine.tokens;
         var endState = tokenizedLine.state;

         return this.$getNextLineIndentForTokens(tokens, indent, tab, endState);
      };

      this.$getNextLineIndentForTokens = function(tokens, indent, tab, endState)
      {
         // filter out whitespace
         tokens = tokens.filter(function (t) { return !/^\s*$/.test(t.value) });

         // filter out comments
         tokens = tokens.filter(function (t) { return t.type != "comment"; });

         // If there's nothing significant on this line, don't change indent
         if (tokens.length == 0)
            return indent;

         // If we're inside a string, change indent to 0
         if (endState != "start")
            return "";

         // Create a string composed of only the braces. Careful not to pick
         // up braces in strings or comments.
         var braces = tokens.filter(function(t) { return /\bparen\b/.test(t.type) });
         var bracesStr = braces.reduce(function (memo, t) { return memo + t.value; }, "");

         function countOpens(str) { return str.replace(/[^\[({]/g, "").length; }
         function countCloses(str) { return str.replace(/[^\])}]/g, "").length; }
         var opens = countOpens(bracesStr);
         var closes = countCloses(bracesStr);

         if (opens > closes)
         {
            return indent + tab;
         }

         if (opens < closes)
         {
            return indent.replace(tab, "");
         }

         if (tokens[0].type === "keyword"
               && /^(if|while|for)$/.test(tokens[0].value))
         {
            // Check for the case where the previous line was "if (cond)"
            // cause we want to indent in those cases. But not if it's
            // "if (cond) expr". Need to be careful here because the
            // conditional expression can contain parens. So we move
            // through the token list, waiting for the outermost paren
            // to be matched. Once we have done that, see if there's
            // any tokens left. (Note that whitespace and comments have
            // been stripped at this point so they won't count as expr)

            var postIf = tokens.slice(2);
            var parenCount = 1;
            var pos = 0;
            for (; parenCount > 0 && pos < postIf.length; pos++)
            {
               var t = postIf[pos];
               if (/paren/.test(t.type))
               {
                  for (var j = 0; parenCount > 0 && j < t.value.length; j++)
                  {
                     if (t.value.charAt(j) == "(")
                        parenCount++;
                     else if (t.value.charAt(j) == ")")
                        parenCount--;
                  }
               }
            }

            if (pos == postIf.length)
            {
               return indent + tab;
            }
         }

         // See if we end with a binary operator; that means the operation
         // isn't done yet
         var lastToken = tokens[tokens.length - 1];
         if (/\boperator\b/.test(lastToken.type) && !/\bparen\b/.test(lastToken.type))
         {
            return indent + tab;
         }

         if (lastToken.type === "keyword" && lastToken.value === "repeat")
         {
            return indent + tab;
         }

         return indent;
      };

      this.checkOutdent = function(state, line, input)
      {
         return this.$outdent.checkOutdent(line, input);
      };

      this.autoOutdent = function(state, doc, row)
      {
         return this.$outdent.autoOutdent(doc, row);
      };
   }).call(Mode.prototype);

   exports.Mode = Mode;
});
