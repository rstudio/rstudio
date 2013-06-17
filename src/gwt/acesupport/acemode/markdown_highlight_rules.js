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
define("mode/markdown_highlight_rules", function(require, exports, module) {

var oop = require("ace/lib/oop");
var lang = require("ace/lib/lang");
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var MarkdownHighlightRules = function() {

    var slideFields = lang.arrayToMap(
        ("title|author|date|rtl|depends|autosize|width|height|transition|transition-speed|font-family|css|class|navigation|incremental|left|right|id|audio|video|type|at|help-doc|help-topic|source|console|console-input|execute|pause")
              .split("|")
        );

    // regexp must not have capturing parentheses
    // regexps are ordered -> the first match is used

    this.$rules = {
        "start" : [ {
            token : "empty_line",
            regex : '^$'
        }, { // code span `
            token : ["support.function", "support.function", "support.function"],
            regex : "(`+)([^\\r]*?[^`])(\\1)"
        }, { // code block
            token : "support.function",
            regex : "^[ ]{4}.+"
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
        }, { // header
            token : function(value) {
                return "markup.heading." + value.search(/[^#]/);
            },
            regex : "^#{1,6}(?:[^ #].*| +.*(?:[^ #].*|[^ ]+.* +#+ *))$"
        },  { // Github style block
            token : "support.function",
            regex : "^```[a-zA-Z]+\\s*$",
            next  : "githubblock"
        }, { // block quote
            token : "string",
            regex : "^>[ ].+$",
            next  : "blockquote"
        }, { // reference
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
        }, { // MathJax $$(?:latex)?
            token : "markup.list",
            regex : "\\${2}(?:latex(?:\\s|$))?",
            next  : "mathjaxdisplay"
        }, { // MathJax $latex
            token : "markup.list",
            regex : "\\$latex\\s",
            next  : "mathjaxinline"
        }, { // MathJax $...$ (org-mode style)
            token : ["markup.list","support.function","markup.list"],
            regex : "(\\$)" + "((?!\\s)[^$]*[^$\\s])" + "(\\$)" + "(?![\\w\\d`])"
        }, { // strong ** __
            token : ["constant.numeric", "constant.numeric", "constant.numeric"],
            regex : "([*]{2}|[_]{2}(?=\\S))([^\\r]*?\\S[*_]*)(\\1)"
        }, { // emphasis * _
            token : ["constant.language.boolean", "constant.language.boolean", "constant.language.boolean"],
            regex : "([*]|[_](?=\\S))([^\\r]*?\\S[*_]*)(\\1)"
        }, { // 
            token : ["text", "url", "text"],
            regex : "(<)("+
                      "(?:https?|ftp|dict):[^'\">\\s]+"+
                      "|"+
                      "(?:mailto:)?[-.\\w]+\\@[-a-z0-9]+(?:\\.[-a-z0-9]+)*\\.[a-z]+"+
                    ")(>)"
        }, {
            token : "text",
            regex : "[^\\*_%$`\\[#<>\\\\]+"
        } , {
            token : "text",
            regex : "\\\\"
        } ],
        
        "blockquote" : [ { // BLockquotes only escape on blank lines.
            token : "empty_line",
            regex : "^\\s*$",
            next  : "start"
        }, {
            token : "string",
            regex : ".+"
        } ],
        
        "githubblock" : [ {
            token : "support.function",
            regex : "^```",
            next  : "start"
        }, {
            token : "support.function",
            regex : ".+"
        } ],
        
        "fieldblock" : [ {
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
        } ],

        "fieldblockvalue" : [ {
            token : "text",
            regex : "$",
            next  : "fieldblock"
        }, {
            token : "text",
            regex : ".+"
        } ],

        "mathjaxdisplay" : [ {
            token : "markup.list",
            regex : "\\${2}",
            next  : "start"
        }, {
            token : "support.function",
            regex : "[^\\$]+"
        } ],
        
        "mathjaxnativedisplay" : [ {
            token : "markup.list",
            regex : "\\\\\\]",
            next  : "start"
        }, {
            token : "support.function",
            regex : "[\\s\\S]+?"
        } ],
        
        "mathjaxinline" : [ {
            token : "markup.list",
            regex : "\\$",
            next  : "start"
        }, {
            token : "support.function",
            regex : "[^\\$]+"
        } ],

        "mathjaxnativeinline" : [ {
            token : "markup.list",
            regex : "\\\\\\)",
            next  : "start"
        }, {
            token : "support.function",
            regex : "[\\s\\S]+?"
        } ]
    };
};
oop.inherits(MarkdownHighlightRules, TextHighlightRules);

exports.MarkdownHighlightRules = MarkdownHighlightRules;
});
