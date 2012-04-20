/*
 * markdown.js
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

define("mode/rmarkdown", function(require, exports, module) {

var oop = require("ace/lib/oop");
var MarkdownMode = require("ace/mode/markdown").Mode;
var Tokenizer = require("ace/tokenizer").Tokenizer;
var RMarkdownHighlightRules = require("mode/rmarkdown_highlight_rules").RMarkdownHighlightRules;

var Mode = function() {   
   this.$tokenizer = new Tokenizer(new RMarkdownHighlightRules().getRules());
};
oop.inherits(Mode, MarkdownMode);

(function() {
}).call(Mode.prototype);

exports.Mode = Mode;
});
