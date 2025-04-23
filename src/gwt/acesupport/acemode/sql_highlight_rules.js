/*
 * sql_highlight_rules.js
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * The Initial Developer of the Original Code is Jeffrey Arnold
 * Portions created by the Initial Developer are Copyright (C) 2014
 * the Initial Developer. All Rights Reserved.
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

define("mode/sql_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

  var oop = require("ace/lib/oop");
  var lang = require("ace/lib/lang");
  var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

  var SqlHighlightRules = function() {

    // https://en.wikipedia.org/wiki/List_of_SQL_reserved_words
    var keywords = (
        "abs|absent|acos|all|allocate|alter|and|any|any_value|are|array|array_agg|array_max_cardinality|as|asensitive|asin|asymmetric|at|atan|atomic|authorization|avg|begin|begin_frame|begin_partition|between|bigint|binary|blob|boolean|both|btrim|by|call|called|cardinality|cascaded|case|cast|ceil|ceiling|char|character|character_length|char_length|check|classifier|clob|close|coalesce|collate|collect|column|commit|condition|connect|constraint|contains|convert|copy|corr|corresponding|cos|cosh|count|covar_pop|covar_samp|create|cross|cube|cume_dist|current|current_catalog|current_date|current_default_transform_group|current_path|current_role|current_row|current_schema|current_time|current_timestamp|current_transform_group_for_type|current_user|cursor|cycle|date|day|deallocate|dec|decfloat|decimal|declare|default|define|delete|dense_rank|deref|describe|deterministic|disconnect|distinct|do|double|drop|dynamic|each|element|else|elseif|empty|end|end-exec|end_frame|end_partition|equals|escape|every|except|exec|execute|exists|exp|external|extract|false|fetch|filter|first_value|float|floor|for|foreign|frame_row|free|from|full|function|fusion|get|global|grant|greatest|group|grouping|groups|handler|having|hold|hour|identity|if|in|indicator|initial|inner|inout|insensitive|insert|int|integer|intersect|intersection|interval|into|is|iterate|join|json|json_array|json_arrayagg|json_exists|json_object|json_objectagg|json_query|json_scalar|json_serialize|json_table|json_table_primitive|json_value|lag|language|large|last_value|lateral|lead|leading|least|leave|left|like|like_regex|listagg|ln|local|localtime|localtimestamp|log|log10|loop|lower|lpad|ltrim|match|matches|match_number|match_recognize|max|member|merge|method|min|minute|mod|modifies|module|month|multiset|national|natural|nchar|nclob|new|no|none|normalize|not|nth_value|ntile|null|nullif|numeric|occurrences_regex|octet_length|of|offset|old|omit|on|one|only|open|or|order|out|outer|over|overlaps|overlay|parameter|partition|pattern|per|percent|percentile_cont|percentile_disc|percent_rank|period|portion|position|position_regex|power|precedes|precision|prepare|primary|procedure|ptf|range|rank|reads|real|recursive|ref|references|referencing|regr_avgx|regr_avgy|regr_count|regr_intercept|regr_r2|regr_slope|regr_sxx|regr_sxy|regr_syy|release|repeat|resignal|result|return|returns|revoke|right|rollback|rollup|row|rows|row_number|rpad|running|savepoint|scope|scroll|search|second|seek|select|sensitive|session_user|set|show|signal|similar|sin|sinh|skip|smallint|some|specific|specifictype|sql|sqlexception|sqlstate|sqlwarning|sqrt|start|static|stddev_pop|stddev_samp|submultiset|subset|substring|substring_regex|succeeds|sum|symmetric|system|system_time|system_user|table|tablesample|tan|tanh|then|time|timestamp|timezone_hour|timezone_minute|to|trailing|translate|translate_regex|translation|treat|trigger|trim|trim_array|true|truncate|uescape|union|unique|unknown|unnest|until|update|upper|user|using|value|values|value_of|varbinary|varchar|varying|var_pop|var_samp|versioning|when|whenever|where|while|width_bucket|window|with|within|without|year"
    );

    var builtinConstants = (
        "true|false"
    );

    var builtinFunctions = (
        "avg|count|first|last|max|min|sum|ucase|lcase|mid|len|round|rank|now|format|" + 
        "coalesce|ifnull|isnull|nvl"
    );

    var dataTypes = (
        "int|numeric|decimal|date|varchar|char|bigint|float|double|bit|binary|text|set|timestamp|" +
        "money|real|number|integer"
    );

    var keywordMapper = this.createKeywordMapper({
        "support.function": builtinFunctions,
        "keyword": keywords,
        "constant.language": builtinConstants,
        "storage.type": dataTypes
    }, "identifier", true);

    this.$rules = {
        "start" : [ {
            token : "comment",
            regex : "--.*$"
        },  {
            token : "comment",
            start : "/\\*",
            end : "\\*/"
        }, {
            token: "comment",
            regex: "^#.*$"
        }, {
          token : "comment.doc.tag",
          regex : "\\?[a-zA-Z_][a-zA-Z0-9_$]*"
        }, {
            // Obviously these are neither keywords nor operators, but
            // labelling them as such was the easiest way to get them
            // to be colored distinctly from regular text
            token : "paren.keyword.operator",
            merge : false,
            regex : "[[({]",
            next  : "start"
        },
        {
            // Obviously these are neither keywords nor operators, but
            // labelling them as such was the easiest way to get them
            // to be colored distinctly from regular text
            token : "paren.keyword.operator",
            merge : false,
            regex : "[\\])}]",
            next  : "start"
        }, {
            token : "string",           // " string
            regex : '".*?"'
        }, {
            token : "string",           // ' string
            regex : "'.*?'"
        }, {
            token : "constant.numeric", // float
            regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
        }, {
            token : keywordMapper,
            regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
        }, {
            token : "keyword.operator",
            regex : "\\+|\\-|\\/|\\/\\/|%|<@>|@>|<@|&|\\^|~|<|>|<=|=>|==|!=|<>|=|\\."
        }, {
            token : "paren.lparen",
            regex : "[\\(]"
        }, {
            token : "paren.rparen",
            regex : "[\\)]"
        }, {
            token : "text",
            regex : "\\s+"
        }]
    };
    this.normalizeRules();
  };

  oop.inherits(SqlHighlightRules, TextHighlightRules);

  exports.SqlHighlightRules = SqlHighlightRules;
});
