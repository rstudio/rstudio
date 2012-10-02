/*
 * markdown_highlight_rules.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
define("mode/rmarkdown_highlight_rules", function(require, exports, module) {

var oop = require("ace/lib/oop");
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var MarkdownHighlightRules = require("mode/markdown_highlight_rules").MarkdownHighlightRules;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var RMarkdownHighlightRules = function() {

    // regexp must not have capturing parentheses
    // regexps are ordered -> the first match is used

    this.$rules = new MarkdownHighlightRules().getRules();
    this.$rules["start"].unshift({
        token: "support.function.codebegin",
        regex: "^`{3,}\\s*\\{r(?:.*)\\}\\s*$",
        next: "r-start"
    });

    var rRules = new RHighlightRules().getRules();
    this.addRules(rRules, "r-");
    this.$rules["r-start"].unshift({
        token: "support.function.codeend",
        regex: "^`{3,}\\s*$",
        next: "start"
    });
};
oop.inherits(RMarkdownHighlightRules, TextHighlightRules);

exports.RMarkdownHighlightRules = RMarkdownHighlightRules;
});
