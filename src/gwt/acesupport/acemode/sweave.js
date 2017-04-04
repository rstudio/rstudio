/*
 * sweave.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
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
define("mode/sweave", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var Tokenizer = require("ace/tokenizer").Tokenizer;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
var SweaveHighlightRules = require("mode/sweave_highlight_rules").SweaveHighlightRules;
var RCodeModel = require("mode/r_code_model").RCodeModel;
var MatchingBraceOutdent = require("ace/mode/matching_brace_outdent").MatchingBraceOutdent;
var RMatchingBraceOutdent = require("mode/r_matching_brace_outdent").RMatchingBraceOutdent;
var unicode = require("ace/unicode");
var Utils = require("mode/utils");
var AutoBraceInsert = require("mode/auto_brace_insert").AutoBraceInsert;

var Mode = function(suppressHighlighting, session) {
   if (suppressHighlighting)
      this.$tokenizer = new Tokenizer(new TextHighlightRules().getRules());
   else
      this.$tokenizer = new Tokenizer(new SweaveHighlightRules().getRules());

   this.$session = session;
   this.$outdent = new MatchingBraceOutdent();

   this.codeModel = new RCodeModel(
      session,
      this.$tokenizer,
      /^r-/,
      /<<(.*?)>>/,
      /^\s*@\s*$/
   );
   this.$r_outdent = new RMatchingBraceOutdent(this.codeModel);

   this.foldingRules = this.codeModel;
};
oop.inherits(Mode, TextMode);

(function() {

   this.tokenRe = new RegExp("^["
       + unicode.packages.L
       + unicode.packages.Mn + unicode.packages.Mc
       + unicode.packages.Nd
       + unicode.packages.Pc + "._]+", "g"
   );

   this.nonTokenRe = new RegExp("^(?:[^"
       + unicode.packages.L
       + unicode.packages.Mn + unicode.packages.Mc
       + unicode.packages.Nd
       + unicode.packages.Pc + "._]|\\s])+", "g"
   );

   this.$complements = {
            "(": ")",
            "[": "]",
            '"': '"',
            "'": "'",
            "{": "}"
         };
   this.$reOpen = /^[(["'{]$/;
   this.$reClose = /^[)\]"'}]$/;

   this.insertChunkInfo = {
      value: "<<>>=\n\n@\n",
      position: {row: 0, column: 2}, 
      content_position: {row: 1, column: 0}
   };

   this.getLanguageMode = function(position)
   {
      var state = Utils.getPrimaryState(this.$session, position.row);
      return state.match(/^r-/) ? 'R' : 'TeX';
   };

   this.$getNextLineIndent = this.getNextLineIndent;
   this.getNextLineIndent = function(state, line, tab, row)
   {
      var mode = Utils.activeMode(state, "tex");
      if (mode === "r")
         return this.codeModel.getNextLineIndent(state, line, tab, row);
      else
         return this.$getNextLineIndent(state, line, tab);
   };

   this.checkOutdent = function(state, line, input)
   {
      var mode = Utils.activeMode(state, "tex");
      if (mode === "r")
         return this.$r_outdent.checkOutdent(state, line, input);
      else
         return this.$outdent.checkOutdent(line, input);
   };

   this.autoOutdent = function(state, session, row)
   {
      var mode = Utils.activeMode(state, "tex");
      if (mode === "r")
         return this.$r_outdent.autoOutdent(state, session, row);
      else
         return this.$outdent.autoOutdent(session, row);
   };

   this.allowAutoInsert = this.smartAllowAutoInsert;

   this.$id = "mode/sweave";

}).call(Mode.prototype);

exports.Mode = Mode;
});
