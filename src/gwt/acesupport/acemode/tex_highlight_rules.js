/*
 * tex_highlight_rules.js
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
define("mode/tex_highlight_rules", function(require, exports, module) {

var oop = require("pilot/oop");
var lang = require("pilot/lang");
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var TexHighlightRules = function() {

    // regexp must not have capturing parentheses. Use (?:) instead.
    // regexps are ordered -> the first match is used

    this.$rules = {
        "start" : [
	        {
	            token : "comment",
	            regex : "%.*$"
	        }, {
	            token : "text", // non-command
	            regex : "\\\\[$&%#\\{\\}]"
	        }, {
	            token : "keyword", // command
	            regex : "\\\\(?:[a-zA-z0-9]+|[^a-zA-z0-9])"
	        }, {
	            token : "string", // double quoted string
	            regex : "``",
				next : "qqstring"
	        }, {
	            token : "string", // single quoted string
	            regex : "`",
	            next : "qstring"
	        }, {
	            token : "paren",
	            regex : "[[({]"
	        }, {
	            token : "paren",
	            regex : "[\\])}]"
	        }, {
	            token : "text",
	            regex : "\\s+"
	        }
        ],
        "qqstring" : [
	        {
	            token : "string",
	            regex : "['][']",
	            next : "start"
	        }, {
	            token : "comment",
	            regex : "%.*$"
	        }, {
	            token : "string", // non-command
	            regex : "\\\\[$&%#\\{\\}]"
	        }, {
	            token : "keyword", // command
	            regex : "\\\\(?:[a-zA-z0-9]+|[^a-zA-z0-9])"
	        }, {
	            token : "paren",
	            regex : "[[({]"
	        }, {
	            token : "paren",
	            regex : "[\\])}]"
	        }, {
	            token : "string",
	            regex : "[^\\\\'[({\\])}%]+"
	        }, {
		        token : "string",
				regex : "."
			}
        ],
        "qstring" : [
	        {
	            token : "string",
	            regex : "['](?!['])",
	            next : "start"
	        }, {
	            token : "comment",
	            regex : "%.*$"
	        }, {
	            token : "string", // non-command
	            regex : "\\\\[$&%#\\{\\}]"
	        }, {
	            token : "keyword", // command
	            regex : "\\\\(?:[a-zA-z0-9]+|[^a-zA-z0-9])"
	        }, {
	            token : "paren",
	            regex : "[[({]"
	        }, {
	            token : "paren",
	            regex : "[\\])}]"
	        }, {
	            token : "string",
	            regex : "[^\\\\'[({\\])}%]+"
	        }, {
		        token : "string",
				regex : "."
			}
        ]
    };
};

oop.inherits(TexHighlightRules, TextHighlightRules);

exports.TexHighlightRules = TexHighlightRules;
});
