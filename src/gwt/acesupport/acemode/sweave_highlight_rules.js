/*
 * sweave_highlight_rules.js
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
define("mode/sweave_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var TexHighlightRules = require("mode/tex_highlight_rules").TexHighlightRules;
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var SweaveHighlightRules = function() {

    // regexp must not have capturing parentheses
    // regexps are ordered -> the first match is used

    this.$rules = new TexHighlightRules().getRules();

    this.$rules["start"].unshift({
        token: "comment.codebegin",
        regex: "^\\s*\\<\\<.*\\>\\>=.*$",
        next: "r-start"
    });

    this.$rules["start"].unshift({
        token: "comment",
        regex: "^\\s*@(?:\\s.*)?$"
    });

    // embed R highlight rules within \Sexpr{}
    this.$rules["start"].unshift({
        regex: "(\\\\Sexpr)([{])",
        next: "r-start",
        onMatch: function(value, state, stack, line) {
            stack.length = 2;
            stack[0] = state;
            stack[1] = 1;
            return [
                { type: "keyword", value: "\\Sexpr" },
                { type: "paren.keyword.operator", value: "{" }
            ];
        }
    });

    var rRules = new RHighlightRules().getRules();
    this.addRules(rRules, "r-");

    // special handling for '{' and '}', for Sexpr
    // embeds
    this.$rules["r-start"].unshift({
        token : "paren.keyword.operator",
        regex : "[{]",
        merge : false,
        onMatch: function(value, state, stack, line) {
            if (stack.length)
                stack[1] += 1;
            return this.token;
        }
    });

    this.$rules["r-start"].unshift({
        token : "paren.keyword.operator",
        regex : "[}]",
        merge : false,
        onMatch: function(value, state, stack, line) {
            this.next = "r-start";
            if (stack.length) {
                stack[1] -= 1;
                if (stack[1] == 0) {
                    this.next = stack[0];
                    stack.splice(0);
                }
            }
            return this.token;
        }
    });

    this.$rules["r-start"].unshift({
        token: "comment.codeend",
        regex: "^\\s*@(?:\\s.*)?$",
        next: "start"
    });

    this.$rules["r-start"].unshift({
        token: "comment.codebegin",
        regex: "^\\<\\<.*\\>\\>=.*$",
        next: "r-start"
    });
};

oop.inherits(SweaveHighlightRules, TextHighlightRules);

exports.SweaveHighlightRules = SweaveHighlightRules;
});
