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
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

/* ***** BEGIN LICENSE BLOCK *****
 * Distributed under the BSD license:
 *
 * Copyright (c) 2010, Ajax.org B.V.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Ajax.org B.V. nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL AJAX.ORG B.V. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ***** END LICENSE BLOCK ***** */

define("mode/markdown_highlight_rules", function(require, exports, module) {

var oop = require("ace/lib/oop");
var lang = require("ace/lib/lang");
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
var JavaScriptHighlightRules = require("ace/mode/javascript_highlight_rules").JavaScriptHighlightRules;
var XmlHighlightRules = require("ace/mode/xml_highlight_rules").XmlHighlightRules;
var HtmlHighlightRules = require("ace/mode/html_highlight_rules").HtmlHighlightRules;
var CssHighlightRules = require("ace/mode/css_highlight_rules").CssHighlightRules;

var escaped = function(ch) {
    return "(?:[^" + lang.escapeRegExp(ch) + "\\\\]|\\\\.)*";
}

function github_embed(tag, prefix) {
    return { // Github style block
        token : "support.function",
        regex : "^\\s*```" + tag + "\\s*$",
        push  : prefix + "start"
    };
}

var MarkdownHighlightRules = function() {

    var slideFields = lang.arrayToMap(
        ("title|author|date|rtl|depends|autosize|width|height|transition|transition-speed|font-family|css|class|navigation|incremental|left|right|id|audio|video|type|at|help-doc|help-topic|source|console|console-input|execute|pause")
            .split("|")
    );
    
    // regexp must not have capturing parentheses
    // regexps are ordered -> the first match is used

    this.$rules = {

        "basic" : [{
            token : "constant.language.escape",
            regex : /\\[\\`*_{}\[\]()#+\-.!]/
        }, { // code span `
            token : ["support.function", "support.function", "support.function"],
            regex : "(`+)(.*?[^`])(\\1)"
        }, { // reference
            token : ["text", "constant", "text", "url", "string", "text"],
            regex : "^([ ]{0,3}\\[)([^\\]]+)(\\]:\\s*)([^ ]+)(\\s*(?:[\"][^\"]+[\"])?(\\s*))$"
        }, { // link by reference
            token : ["text", "keyword", "text", "constant", "text"],
            regex : "(\\[)(" + escaped("]") + ")(\\]\s*\\[)("+ escaped("]") + ")(\\])"
        }, { // link by url
            token : ["text", "keyword", "text", "markup.underline", "string", "text"],
            regex : "(\\[)(" +                                        // [
                escaped("]") +                                    // link text
                ")(\\]\\()"+                                      // ](
                '((?:[^\\)\\s\\\\]|\\\\.|\\s(?=[^"]))*)' +        // href
                '(\\s*"' +  escaped('"') + '"\\s*)?' +            // "title"
                "(\\))"                                           // )
        }, { // strong ** __
            token : ["constant.numeric", "constant.numeric", "constant.numeric"],
            regex : "([*]{2}|[_]{2}(?=\\S))(.*?\\S[*_]*)(\\1)"
        }, { // emphasis * _
            token : ["constant.language.boolean", "constant.language.boolean", "constant.language.boolean"],
            regex : "([*]|[_](?=\\S))(.*?\\S[*_]*)(\\1)"
        }, { //
            token : ["text", "keyword", "text"],
            regex : "(<)("+
                "(?:https?|ftp|dict):[^'\">\\s]+"+
                "|"+
                "(?:mailto:)?[-.\\w]+\\@[-a-z0-9]+(?:\\.[-a-z0-9]+)*\\.[a-z]+"+
                ")(>)"
        }],
       
        start: [{
           token : "empty_line",
           regex : '^$',
           next: "allowBlock"
        }, { // code span
            token : ["support.function", "support.function", "support.function"],
            regex : "(`+)([^\\r]*?[^`])(\\1)"
        }, { // h1 with equals
            token: "markup.heading.1",
            regex: "^\\={3,}\\s*$",
            next: "fieldblock"
        }, { // h1
            token: "markup.heading.1",
            regex: "^=+(?=\\s*$)"
        }, { // h2
            token: "markup.heading.2",
            regex: "^\\-+(?=\\s*$)"
        }, {
            token : function(value) {
                return "markup.heading." + value.length;
            },
            regex : /^#{1,6}(?=\s*[^ #]|\s+#.)/,
            next : "header"
        },
                                     
        github_embed("(?:javascript|js)", "jscode-"),
        github_embed("xml", "xmlcode-"),
        github_embed("html", "htmlcode-"),
        github_embed("css", "csscode-"),
        
        { // Github style block
            token : "support.function",
            regex : "^\\s*```\\s*\\S*(?:{.*?\\})?\\s*$",
            next  : "githubblock"
        }, { // block quote
            token : "string.blockquote",
            regex : "^\\s*>\\s*(?:[*+-]|\\d+\\.)?\\s+",
            next  : "blockquote"
        },
        { // reference
            token : ["text", "constant", "text", "url", "string", "text"],
            regex : "^([ ]{0,3}\\[)([^\\]]+)(\\]:\\s*)([^ ]+)(\\s*(?:[\"][^\"]+[\"])?(\\s*))$"
        }, { // link by reference
            token : ["text", "keyword", "text", "constant", "text"],
            regex : "(\\[)((?:[[^\\]]*\\]|[^\\[\\]])*)(\\][ ]?(?:\\n[ ]*)?\\[)(.*?)(\\])"
        }, { // link by url
            token : ["text", "keyword", "text", "markup.underline", "string", "text"],
            regex : "(\\[)"+
                "(\\[[^\\]]*\\]|[^\\[\\]]*)"+
                "(\\]\\([ \\t]*)"+
                "(<?(?:(?:[^\\(]*?\\([^\\)]*?\\)\\S*?)|(?:.*?))>?)"+
                "((?:[ \t]*\"(?:.*?)\"[ \\t]*)?)"+
                "(\\))"
        }, { // HR *
            token : "constant",
            regex : "^[ ]{0,2}(?:[ ]?\\*[ ]?){3,}\\s*$"
        }, { // HR -
            token : "constant",
            regex : "^[ ]{0,2}(?:[ ]?\\-[ ]?){3,}\\s*$"
        }, { // HR _
            token : "constant",
            regex : "^[ ]{0,2}(?:[ ]?\\_[ ]?){3,}\\s*$"
        }, { // MathJax native display \[ ... \]
            token : "markup.list",
            regex : "\\\\\\[",
            next  : "mathjaxnativedisplay"
        }, { // MathJax native inline \( ... \)
            token : "markup.list",
            regex : "\\\\\\(",
            next  : "mathjaxnativeinline"
        }, { // $ escape
            token : "text",
            regex : "\\\\\\$"
        }, { // MathJax $$
            token : "markup.list",
            regex : "\\${2}",
            next  : "mathjaxdisplay"
        }, { // MathJax $...$ (org-mode style)
            token : ["markup.list","support.function","markup.list"],
            regex : "(\\$)" + "((?!\\s)[^$]*[^$\\s])" + "(\\$)" + "(?![\\w\\d`])"
        }, { // strong ** __
            token : ["constant.numeric", "constant.numeric", "constant.numeric"],
            regex : "([*]{2}|[_]{2}(?=\\S))([^\\r]*?\\S[*_]*)(\\1)"
        }, { // emphasis * _
            token : ["constant.language.boolean", "constant.language.boolean", "constant.language.boolean"],
            regex : "([*]|[_](?=\\S))([^\\r]*?\\S[*_]*)(\\1)"
        }, { // simple links <url>
            token : ["text", "keyword", "text"],
            regex : "(<)("+
                "(?:https?|ftp|dict):[^'\">\\s]+"+
                "|"+
                "(?:mailto:)?[-.\\w]+\\@[-a-z0-9]+(?:\\.[-a-z0-9]+)*\\.[a-z]+"+
                ")(>)"
        }, {
            // embedded latex command
            token : "keyword",
            regex : "\\\\(?:[a-zA-z0-9]+|[^a-zA-z0-9])"
        }, {
            // embedded latex arg
            token : ["paren.keyword.operator", "text", "paren.keyword.operator"],
            regex : "(\\{)([^\\}]*)(\\})"
        }, {
            // pandoc citation with brackets
            token : "markup.list",
            regex : "\\[-?\\@[\\w\\d-]+\\]"
        }, {
            // pandoc citation
            token : "markup.list",
            regex : "-?\\@[\\w\\d-]+"
        }, {
            token : "text",
            regex : "[^\\*_%$`\\[#<>\\\\]+"
        }, {
            token : "text",
            regex : "\\\\"
        }, { // HR * - _
            token : "constant",
            regex : "^ {0,2}(?:(?: ?\\* ?){3,}|(?: ?\\- ?){3,}|(?: ?\\_ ?){3,})\\s*$",
            next: "allowBlock"
        }, { // list
            token : "text",
            regex : "^\\s{0,3}(?:[*+-]|\\d+\\.)\\s+",
            next  : "listblock-start"
        }, { // html comment
            token : "comment",
            regex : "<\\!--",
            next  : "html-comment"
        }, {
            include : "basic"
        }],

        "html-comment" : [{
           token: "comment",
           regex: "-->",
           next: "start"
        }, {
           defaultToken: "comment"
        }],
       
        // code block
        "allowBlock": [{
           token : "support.function",
           regex : "^ {4}.+",
           next : "allowBlock"
        }, {
           token : "empty",
           regex : "",
           next : "start"
        }],

        "header" : [{
            regex: "$",
            next : "start"
        }, {
            include: "basic"
        }, {
            defaultToken : "heading"
        }],

        "listblock-start" : [{
            token : "text",
            regex : /(?:\[[ x]\])?/,
            next  : "listblock"
        }],

        "listblock" : [ { // Lists only escape on completely blank lines.
            token : "empty_line",
            regex : "^$",
            next  : "start"
        }, { // list
            token : "text",
            regex : "^\\s{0,3}(?:[*+-]|\\d+\\.)\\s+",
            next  : "listblock-start"
        }, {
            include : "basic", noEscape: true
        }, { // Github style block
            token : "support.function",
            regex : "^\\s*```\\s*[a-zA-Z]*(?:{.*?\\})?\\s*$",
            next  : "githubblock"
        }, {
            defaultToken : "text" //do not use markup.list to allow stling leading `*` differntly
        }],

        "blockquote" : [ { // Blockquotes only escape on blank lines.
            token : "empty_line",
            regex : "^\\s*$",
            next  : "start"
        }, { // block quote
            token : "string.blockquote",
            regex : "^\\s*>\\s*(?:[*+-]|\\d+\\.)?\\s+",
            next  : "blockquote"
        }, {
            include : "basic", noEscape: true
        }, {
            defaultToken : "string.blockquote"
        }],

        "githubblock" : [ {
            token : "support.function",
            regex : "^\\s*```",
            next  : "start"
        }, {
            token : "support.function",
            regex : ".+"
        }]
    };

    this.embedRules(JavaScriptHighlightRules, "jscode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);

    this.embedRules(HtmlHighlightRules, "htmlcode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);

    this.embedRules(CssHighlightRules, "csscode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);

    this.embedRules(XmlHighlightRules, "xmlcode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);

    this.normalizeRules();
};
oop.inherits(MarkdownHighlightRules, TextHighlightRules);

exports.MarkdownHighlightRules = MarkdownHighlightRules;
});
