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

define("mode/markdown_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var lang = require("ace/lib/lang");
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
var JavaScriptHighlightRules = require("ace/mode/javascript_highlight_rules").JavaScriptHighlightRules;
var XmlHighlightRules = require("ace/mode/xml_highlight_rules").XmlHighlightRules;
var HtmlHighlightRules = require("ace/mode/html_highlight_rules").HtmlHighlightRules;
var CssHighlightRules = require("ace/mode/css_highlight_rules").CssHighlightRules;
var PerlHighlightRules = require("ace/mode/perl_highlight_rules").PerlHighlightRules;
var PythonHighlightRules = require("ace/mode/python_highlight_rules").PythonHighlightRules;
var RubyHighlightRules = require("ace/mode/ruby_highlight_rules").RubyHighlightRules;
var ScalaHighlightRules = require("ace/mode/scala_highlight_rules").ScalaHighlightRules;
var ShHighlightRules = require("ace/mode/sh_highlight_rules").ShHighlightRules;
var StanHighlightRules = require("mode/stan_highlight_rules").StanHighlightRules;
var SqlHighlightRules = require("mode/sql_highlight_rules").SqlHighlightRules;

var escaped = function(ch) {
    return "(?:[^" + lang.escapeRegExp(ch) + "\\\\]|\\\\.)*";
}

function github_embed(tag, prefix) {
    return { // Github style block
        token : "support.function",
        regex : "^\\s*```(?:" + "\\{" + tag + "[^\\}]*\\}" + "|" + tag + ")\\s*$",
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

    // handle highlighting for *abc*, _abc_ separately, as pandoc's
    // parser is a bit more strict about where '_' can appear
    var strongUnderscore = {
        token: ["text", "constant.numeric"],
        regex: "(\\s+|^)(__.+?__)\\b"
    };

    var emphasisUnderscore = {
        token: ["text", "constant.language.boolean"],
        regex: "(\\s+|^)(_(?=[^_])(?:(?:\\\\.)|(?:[^_\\\\]))*?_)\\b"
    };

    var strongStars = {
        token: ["constant.numeric"],
        regex: "([*][*].+?[*][*])"
    };

    var emphasisStars = {
        token: ["constant.language.boolean"],
        regex: "([*](?=[^*])(?:(?:\\\\.)|(?:[^*\\\\]))*?[*])"
    };

    this.$rules = {

        "basic" : [{
            token : "constant.language.escape",
            regex : /\\[\\`*_{}\[\]()#+\-.!]/
        }, { // inline r code
            token : "support.function.inline_r_chunk",
            regex : "`r (?:.*?[^`])`"
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
            token : ["text", "keyword", "text", "markup.href", "string", "text"],
            regex : "(\\[)(" +                                    // [
                escaped("]") +                                    // link text
                ")(\\]\\()"+                                      // ](
                '((?:[^\\)\\s\\\\]|\\\\.|\\s(?=[^"]))*)' +        // href
                '(\\s*"' +  escaped('"') + '"\\s*)?' +            // "title"
                "(\\))"                                           // )
        }, {
            token : ["text", "keyword", "text"],
            regex : "(<)("+
                "(?:https?|ftp|dict):[^'\">\\s]+"+
                "|"+
                "(?:mailto:)?[-.\\w]+\\@[-a-z0-9]+(?:\\.[-a-z0-9]+)*\\.[a-z]+"+
                ")(>)"
        },
            strongStars,
            strongUnderscore,
            emphasisStars,
            emphasisUnderscore
        ],
       
        start: [{
           token : "empty_line",
           regex : '^$',
           next: "allowBlock"
        }, { // inline r code
            token : "support.function.inline_r_chunk",
            regex : "`r (?:.*?[^`])`"
        }, { // code span `
            token : ["support.function", "support.function", "support.function"],
            regex : "(`+)([^\\r]*?[^`])(\\1)"
        }, { // h1 with equals
            token: "markup.heading.1",
            regex: "^\\={3,}\\s*$",
            next: "fieldblock"
        }, { // h1
            token: "markup.heading.1",
            regex: "^={3,}(?=\\s*$)"
        }, { // h2
            token: "markup.heading.2",
            regex: "^\\-{3,}(?=\\s*$)"
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
        github_embed("perl", "perlcode-"),
        github_embed("python", "pythoncode-"),
        github_embed("ruby", "rubycode-"),
        github_embed("scala", "scalacode-"),
        github_embed("sh", "shcode-"),
        github_embed("bash", "bashcode-"),
        github_embed("stan", "stancode-"),
        github_embed("sql", "sqlcode-"),
        
        { // Github style block
            token : "support.function",
            regex : "^\\s*```\\s*\\S*(?:{.*?\\})?\\s*$",
            next  : "githubblock"
        }, { // ioslides-style bullet
            token : "string.blockquote",
            regex : "^\\s*>\\s*(?=[-])"
        }, { // block quote
            token : "string.blockquote",
            regex : "^\\s*>\\s*",
            next  : "blockquote"
        }, { // reference
            token : ["text", "constant", "text", "url", "string", "text"],
            regex : "^([ ]{0,3}\\[)([^\\]]+)(\\]:\\s*)([^ ]+)(\\s*(?:[\"][^\"]+[\"])?(\\s*))$"
        }, { // link by reference
            token : ["text", "keyword", "text", "constant", "text"],
            regex : "(\\[)((?:[[^\\]]*\\]|[^\\[\\]])*)(\\][ ]?(?:\\n[ ]*)?\\[)(.*?)(\\])"
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
            token : "latex.markup.list.string.begin",
            regex : "\\\\\\[",
            next  : "mathjaxnativedisplay"
        }, { // MathJax native inline \( ... \)
            token : "latex.markup.list.string.begin",
            regex : "\\\\\\(",
            next  : "mathjaxnativeinline"
        }, { // $ escape
            token : "text",
            regex : "\\\\\\$"
        }, { // MathJax $$
            token : "latex.markup.list.string.begin",
            regex : "\\${2}",
            next  : "mathjaxdisplay"
        }, { // MathJax $...$ (org-mode style)
            token : ["latex.markup.list.string.begin","latex.support.function","latex.markup.list.string.end"],
            regex : "(\\$)((?:(?:\\\\.)|(?:[^\\$\\\\]))*?)(\\$)"
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
            regex : "\\\\(?:[a-zA-Z0-9]+|[^a-zA-Z0-9])"
        }, {
            // brackets
            token : "paren.keyword.operator",
            regex : "[{}]"
        }, {
            // pandoc citation
            token : "markup.list",
            regex : "-?\\@[\\w\\d-]+"
        }, {
            token : "text",
            regex : "[^\\*_%$`\\[#<>{}\\\\@\\s]+"
        }, {
            token : "text",
            regex : "\\\\"
        }, { // HR * - _
            token : "constant",
            regex : "^ {0,2}(?:(?: ?\\* ?){3,}|(?: ?\\- ?){3,}|(?: ?\\_ ?){3,})\\s*$",
            next: "allowBlock"
        }, { // list
            token : "text",
            regex : "^\\s*(?:[*+-]|\\d+\\.)\\s+",
            next  : "listblock"
        },
            strongStars,
            strongUnderscore,
            emphasisStars,
            emphasisUnderscore,
        { // html comment
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

        "listblock" : [ { // Lists only escape on completely blank lines.
            token : "empty_line",
            regex : "^\\s*$",
            next  : "start"
        }, { // list
            token : "text",
            regex : "^\\s{0,3}(?:[*+-]|\\d+\\.)\\s+",
            next  : "listblock"
        }, {
            include : "basic", noEscape: true
        }, { // Github style block
            token : "support.function",
            regex : "^\\s*```\\s*[a-zA-Z]*(?:{.*?\\})?\\s*$",
            next  : "githubblock"
        }, {
            defaultToken : "text" //do not use markup.list to allow stling leading `*` differntly
        }],

        "blockquote" : [{ // Blockquotes only escape on blank lines.
            token : "empty_line",
            regex : "^\\s*$",
            next  : "start"
        }, {
            token : "constant.language.escape",
            regex : /\\[\\`*_{}\[\]()#+\-.!]/
        }, { // inline r code
            token : "support.function.inline_r_chunk",
            regex : "`r (?:.*?[^`])`"
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
            token : ["text", "keyword", "text", "markup.href", "string", "text"],
            regex : "(\\[)(" +                                    // [
                escaped("]") +                                    // link text
                ")(\\]\\()"+                                      // ](
                '((?:[^\\)\\s\\\\]|\\\\.|\\s(?=[^"]))*)' +        // href
                '(\\s*"' +  escaped('"') + '"\\s*)?' +            // "title"
                "(\\))"                                           // )
        }, {
            token : ["text", "keyword", "text"],
            regex : "(<)("+
                "(?:https?|ftp|dict):[^'\">\\s]+"+
                "|"+
                "(?:mailto:)?[-.\\w]+\\@[-a-z0-9]+(?:\\.[-a-z0-9]+)*\\.[a-z]+"+
                ")(>)"
        },
            strongStars,
            strongUnderscore,
            emphasisStars,
            emphasisUnderscore,
        {
            defaultToken : "string.blockquote"
        }],

        "githubblock" : [ {
            token : "support.function",
            regex : "^\\s*```",
            next  : "start"
        }, {
            token : "support.function",
            regex : ".+"
        }],

         "fieldblock" : [{
            token : function(value) {
                var field = value.slice(0,-1);
                if (slideFields[field])
                    return "comment.doc.tag";
                else
                    return "text";
            },
            regex : "^" +"[\\w-]+\\:",
            next  : "fieldblockvalue"
        }, {
            token : "text",
            regex : "(?=.+)",
            next  : "start"
        }],

        "fieldblockvalue" : [{
            token : "text",
            regex : "$",
            next  : "fieldblock"
        }, {
            token : "text",
            regex : "[^{}]+"
        }],

        "mathjaxdisplay" : [{
            token : "latex.markup.list.string.end",
            regex : "\\${2}",
            next  : "start"
        }, {
            token : "latex.support.function",
            regex : "[^\\$]+"
        }],
        
        "mathjaxnativedisplay" : [{
            token : "latex.markup.list.string.end",
            regex : "\\\\\\]",
            next  : "start"
        }, {
            token : "latex.support.function",
            regex : "[\\s\\S]+?"
        }],
        
        "mathjaxnativeinline" : [{
            token : "latex.markup.list.string.end",
            regex : "\\\\\\)",
            next  : "start"
        }, {
            token : "latex.support.function",
            regex : "[\\s\\S]+?"
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
    
    this.embedRules(PerlHighlightRules, "perlcode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);
    
    this.embedRules(PythonHighlightRules, "pythoncode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);
    
    this.embedRules(RubyHighlightRules, "rubycode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);
    
    this.embedRules(ScalaHighlightRules, "scalacode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);

    this.embedRules(ShHighlightRules, "shcode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);

    this.embedRules(ShHighlightRules, "bashcode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);

    this.embedRules(SqlHighlightRules, "sqlcode-", [{
        token : "support.function",
        regex : "^\\s*```",
        next  : "pop"
    }]);

    this.normalizeRules();
};
oop.inherits(MarkdownHighlightRules, TextHighlightRules);

exports.MarkdownHighlightRules = MarkdownHighlightRules;
});
