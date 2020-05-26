/*
 * stan.js
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

define("mode/stan", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var Tokenizer = require("ace/tokenizer").Tokenizer;
var MatchingBraceOutdent = require("ace/mode/matching_brace_outdent").MatchingBraceOutdent;
var StanHighlightRules = require("mode/stan_highlight_rules").StanHighlightRules;
var StanFoldMode = require("ace/mode/folding/cstyle").FoldMode;

var Mode = function() {
   this.$highlightRules = new StanHighlightRules();
   this.$tokenizer = new Tokenizer(this.$highlightRules.getRules());
   this.$outdent = new MatchingBraceOutdent();
   this.foldingRules = new StanFoldMode();
};
oop.inherits(Mode, TextMode);

(function() {

   this.lineCommentStart = ["//", "#"];
   this.blockComment = {start: "/*", end: "*/"};

   this.toggleCommentLines = function(state, doc, startRow, endRow) {
      var outdent = true;
      var re = /^(\s*)\/\//;

      for (var i=startRow; i<= endRow; i++) {
         if (!re.test(doc.getLine(i))) {
            outdent = false;
            break;
         }
      }

      if (outdent) {
         var deleteRange = new Range(0, 0, 0, 0);
         for (var i = startRow; i <= endRow; i++)
         {
            var line = doc.getLine(i);
            var m = line.match(re);
            deleteRange.start.row = i;
            deleteRange.end.row = i;
            deleteRange.end.column = m[0].length;
            doc.replace(deleteRange, m[1]);
         }
      }
      else {
         doc.indentRows(startRow, endRow, "//");
      }
   };

   this.getLanguageMode = function(position) {
      return "Stan";
   };

   this.getNextLineIndent = function(state, line, tab) {
      var indent = this.$getIndent(line);

      var tokenizedLine = this.getTokenizer().getLineTokens(line, state);
      var tokens = tokenizedLine.tokens;
      var endState = tokenizedLine.state;

      if (tokens.length && tokens[tokens.length - 1].type == "comment") {
         return indent;
      }

      if (state == "start") {
         var match = line.match(/^.*(?:[\{\(\[])\s*$/);
         if (match) {
            indent += tab;
         }
      }

      return indent;
   };


   this.checkOutdent = function(state, line, input) {
      return this.$outdent.checkOutdent(line, input);
   };

   this.autoOutdent = function(state, doc, row) {
      this.$outdent.autoOutdent(doc, row);
   };

   this.$id = "mode/stan";

}).call(Mode.prototype);

exports.Mode = Mode;
});
