/*
 * markdown.js
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

define("mode/markdown", function(require, exports, module) {

var oop = require("ace/lib/oop");
var TextMode = require("ace/mode/text").Mode;
var Tokenizer = require("ace/tokenizer").Tokenizer;
var MarkdownHighlightRules = require("mode/markdown_highlight_rules").MarkdownHighlightRules;
var MarkdownFoldMode = require("mode/markdown_folding").FoldMode;


var Mode = function() {   
   this.$tokenizer = new Tokenizer(new MarkdownHighlightRules().getRules());

   this.foldingRules = new MarkdownFoldMode();
};
oop.inherits(Mode, TextMode);

(function() {
    this.getNextLineIndent = function(state, line, tab) {
      return this.$getIndent(line);
    };
}).call(Mode.prototype);

exports.Mode = Mode;
});
