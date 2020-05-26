/*
 * rhtml_highlight_rules.js
 *
 * Copyright (C) 2020 by RStudio, PBC
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
define("mode/rhtml_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var HtmlHighlightRules = require("ace/mode/html_highlight_rules").HtmlHighlightRules;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var RHtmlHighlightRules = function() {

    // regexp must not have capturing parentheses
    // regexps are ordered -> the first match is used

    this.$rules = new HtmlHighlightRules().getRules();
    this.$rules["start"].unshift({
        token: "support.function.codebegin",
        regex: "^<" + "!--\\s*begin.rcode\\s*(?:.*)",
        next: "r-start"
    });

    var rRules = new RHighlightRules().getRules();
    this.addRules(rRules, "r-");
    this.$rules["r-start"].unshift({
        token: "support.function.codeend",
        regex: "^\\s*end.rcode\\s*-->",
        next: "start"
    });
};
oop.inherits(RHtmlHighlightRules, TextHighlightRules);

exports.RHtmlHighlightRules = RHtmlHighlightRules;
});
