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

   var oop = require("pilot/oop");
   var TextMode = require("ace/mode/text").Mode;
   var Tokenizer = require("ace/tokenizer").Tokenizer;
   var TextHighlightRules = require("ace/mode/text_highlight_rules")
         .TextHighlightRules;
   var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
   var MatchingBraceOutdent = require("ace/mode/matching_brace_outdent")
         .MatchingBraceOutdent;

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
      this.getNextLineIndent = function(state, line, tab)
      {
         var indent = this.$getIndent(line);

         var tokenizedLine = this.$tokenizer.getLineTokens(line, state);
         var tokens = tokenizedLine.tokens;
         var endState = tokenizedLine.state;

         if (tokens.length && tokens[tokens.length - 1].type == "comment")
         {
            return indent;
         }

         if (state == "start")
         {
            var match = line.match(/^.*[\{\(\[]\s*$/);
            if (match)
            {
               indent += tab;
            }
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
