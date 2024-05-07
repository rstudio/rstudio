/*
 * sweave_highlight_rules.js
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
define("mode/sweave_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var Utils = require("mode/utils");

var TexHighlightRules = require("mode/tex_highlight_rules").TexHighlightRules;
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var SweaveHighlightRules = function() {

    // regexp must not have capturing parentheses
    // regexps are ordered -> the first match is used

    // Use TeX highlight rules as a base
    this.$rules = new TexHighlightRules().getRules();

    // Embed R highlight rules
    this.addRules(new RHighlightRules().getRules(), "r-");          // Sweave code chunks
    this.addRules(new RHighlightRules().getRules(), "r-sexpr-");    // \Sexpr{}


    // enter an R code chunk
    this.$rules["start"].unshift({
        token: "comment.codebegin",
        regex: "^\\s*<<.*>>=.*$",
        next: "r-start"
    });

    // exit an R code chunk
    this.$rules["r-start"].unshift({
        token: "comment.codeend",
        regex: "^\\s*@(?:\\s.*)?$",
        next: "start"
    });

    // Sweave comments start with an '@'
    this.$rules["start"].unshift({
        token: "comment",
        regex: "^\\s*@(?:\\s.*)?$"
    });

    // embed R highlight rules within \Sexpr{}
    this.$rules["start"].unshift({
        regex: "(\\\\Sexpr)([{])",
        next: "r-sexpr-start",
        onMatch: function(value, state, stack, line, context) {
            context.sexpr = context.sexpr || {};
            context.sexpr.state = state;
            context.sexpr.count = 1;
            return [
                { type: "keyword", value: "\\Sexpr" },
                { type: "paren.keyword.operator", value: "{" }
            ];
        }
    });

    // special handling for '{' and '}', for Sexpr embeds
    this.$rules["r-sexpr-start"].unshift({
        token : "paren.keyword.operator",
        regex : "[{]",
        merge : false,
        onMatch: function(value, state, stack, line, context) {
            context.sexpr.count += 1;
            return this.token;
        }
    });

    this.$rules["r-sexpr-start"].unshift({
        token : "paren.keyword.operator",
        regex : "[}]",
        merge : false,
        onMatch: function(value, state, stack, line, context) {
            context.sexpr.count -= 1;
            if (context.sexpr.count === 0) {
                this.next = context.sexpr.state;
                delete context.sexpr;
            } else {
                this.next = state;
            }
            return this.token;
        }
    });

};

oop.inherits(SweaveHighlightRules, TextHighlightRules);

exports.SweaveHighlightRules = SweaveHighlightRules;
});
