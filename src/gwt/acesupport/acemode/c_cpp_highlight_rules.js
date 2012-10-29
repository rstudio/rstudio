/*
 * c_cpp_highlight_rules.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * The Original Code is Ajax.org Code Editor (ACE).
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *      Fabian Jakobs <fabian AT ajax DOT org>
 *      Gastón Kleiman <gaston.kleiman AT gmail DOT com>
 *
 * Based on Bespin's C/C++ Syntax Plugin by Marc McIntyre.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

define("mode/c_cpp_highlight_rules", function(require, exports, module) {

var oop = require("ace/lib/oop");
var lang = require("ace/lib/lang");
var DocCommentHighlightRules = require("mode/doc_comment_highlight_rules").DocCommentHighlightRules;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
var TexHighlightRules = require("mode/tex_highlight_rules").TexHighlightRules;

var c_cppHighlightRules = function() {

    var keywords = lang.arrayToMap(
        ("and|double|not_eq|throw|and_eq|dynamic_cast|operator|true|" +
        "asm|else|or|try|auto|enum|or_eq|typedef|bitand|explicit|private|" +
        "typeid|bitor|extern|protected|typename|bool|false|public|union|" +
        "break|float|register|unsigned|case|fro|reinterpret-cast|using|catch|" +
        "friend|return|virtual|char|goto|short|void|class|if|signed|volatile|" +
        "compl|inline|sizeof|wchar_t|const|int|static|while|const-cast|long|" +
        "static_cast|xor|continue|mutable|struct|xor_eq|default|namespace|" +
        "switch|delete|new|template|do|not|this|for").split("|")
    );

    var buildinConstants = lang.arrayToMap(
        ("NULL").split("|")
    );

    // regexp must not have capturing parentheses. Use (?:) instead.
    // regexps are ordered -> the first match is used

    this.$rules = {
        "start" : [
            {
                // Attributes
                token: "comment.doc.tag",
                regex: "\\/\\/\\s*\\[\\[.*\\]\\].*$"
            }, {
                // Roxygen
                token : "comment",
                regex : "\\/\\/'",
                next : "rd-start"
            }, {
                // Standard comment
                token : "comment",
                regex : "\\/\\/.*$"
            },
            DocCommentHighlightRules.getStartRule("doc-start"),
            {
                token : "comment", // multi line comment
                merge : true,
                regex : "\\/\\*",
                next : "comment"
            }, {
                token : "string", // single line
                regex : '["](?:(?:\\\\.)|(?:[^"\\\\]))*?["]'
            }, {
                token : "string", // multi line string start
                merge : true,
                regex : '["].*\\\\$',
                next : "qqstring"
            }, {
                token : "string", // single line
                regex : "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']"
            }, {
                token : "string", // multi line string start
                merge : true,
                regex : "['].*\\\\$",
                next : "qstring"
            }, {
                token : "constant.numeric", // hex
                regex : "0[xX][0-9a-fA-F]+\\b"
            }, {
                token : "constant.numeric", // float
                regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
            }, {
              token : "constant", // <CONSTANT>
              regex : "<[a-zA-Z0-9.]+>"
            }, {
              token : "keyword", // pre-compiler directivs
              regex : "(?:#include|#pragma|#line|#define|#undef|#ifdef|#else|#elif|#endif|#ifndef)"
          }, {
                token : function(value) {
                    if (value == "this")
                        return "variable.language";
                    else if (keywords.hasOwnProperty(value))
                        return "keyword";
                    else if (buildinConstants.hasOwnProperty(value))
                        return "constant.language";
                    else
                        return "identifier";
                },
                regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
            }, {
                token : "keyword.operator",
                regex : "!|\\$|%|&|\\*|\\-\\-|\\-|\\+\\+|\\+|~|==|=|!=|<=|>=|<<=|>>=|>>>=|<>|<|>|!|&&|\\|\\||\\?\\:|\\*=|%=|\\+=|\\-=|&=|\\^=|\\b(?:in|new|delete|typeof|void)"
            }, {
              token : "punctuation.operator",
              regex : "\\?|\\:|\\,|\\;|\\."
            }, {
                token : "paren.lparen",
                regex : "[[({]"
            }, {
                token : "paren.rparen",
                regex : "[\\])}]"
            }, {
                token : "text",
                regex : "\\s+"
            }
        ],
        "comment" : [
            {
                token : "comment", // closing comment
                regex : ".*?\\*\\/",
                next : "start"
            }, {
                token : "comment", // comment spanning whole line
                merge : true,
                regex : ".+"
            }
        ],
        "qqstring" : [
            {
                token : "string",
                regex : '(?:(?:\\\\.)|(?:[^"\\\\]))*?"',
                next : "start"
            }, {
                token : "string",
                merge : true,
                regex : '.+'
            }
        ],
        "qstring" : [
            {
                token : "string",
                regex : "(?:(?:\\\\.)|(?:[^'\\\\]))*?'",
                next : "start"
            }, {
                token : "string",
                merge : true,
                regex : '.+'
            }
        ]
    };
    
    var rdRules = new TexHighlightRules("comment").getRules();

    // Make all embedded TeX virtual-comment so they don't interfere with
    // auto-indent.
    for (var i = 0; i < rdRules["start"].length; i++) {
       rdRules["start"][i].token += ".virtual-comment";
    }

    this.addRules(rdRules, "rd-");
    this.$rules["rd-start"].unshift({
       token: "text",
       regex: "^",
       next: "start"
    });
    this.$rules["rd-start"].unshift({
       token : "keyword",
       regex : "@(?!@)[^ ]*"
    });
    this.$rules["rd-start"].unshift({
       token : "comment",
       regex : "@@"
    });
    this.$rules["rd-start"].push({
       token : "comment",
       regex : "[^%\\\\[({\\])}]+"
    });
    
    this.embedRules(DocCommentHighlightRules, "doc-",
        [ DocCommentHighlightRules.getEndRule("start") ]);
};

oop.inherits(c_cppHighlightRules, TextHighlightRules);

exports.c_cppHighlightRules = c_cppHighlightRules;
});
