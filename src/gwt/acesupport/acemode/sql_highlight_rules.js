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
        "abs|absent|accessible|acos|add|all|allocate|alter|analyse|analyze|and|any|any_value|are|array|array_agg|array_max_cardinality|as|asc|asensitive|asin|asymmetric|at|atan|atomic|authorization|avg|backup|before|begin|begin_frame|begin_partition|between|bigint|binary|blob|boolean|both|break|browse|btrim|bulk|by|call|called|cardinality|cascade|cascaded|case|cast|ceil|ceiling|change|char|char_length|character|character_length|check|checkpoint|classifier|clob|close|clustered|coalesce|collate|collation|collect|column|commit|compute|concurrently|condition|connect|constraint|contains|containstable|continue|convert|copy|corr|corresponding|cos|cosh|count|covar_pop|covar_samp|create|cross|cube|cume_dist|current|current_catalog|current_date|current_default_transform_group|current_path|current_role|current_row|current_schema|current_time|current_timestamp|current_transform_group_for_type|current_user|cursor|cycle|database|databases|date|day|day_hour|day_microsecond|day_minute|day_second|dbcc|deallocate|dec|decfloat|decimal|declare|default|deferrable|define|delayed|delete|dense_rank|deny|deref|desc|describe|deterministic|disconnect|disk|distinct|distinctrow|distributed|div|do|double|drop|dual|dump|dynamic|each|element|else|elseif|empty|enclosed|end|end_frame|end_partition|end-exec|equals|errlvl|escape|escaped|every|except|exec|execute|exists|exit|exp|explain|external|extract|false|fetch|file|fillfactor|filter|first_value|float|float4|float8|floor|for|force|foreign|frame_row|free|freetext|freetexttable|freeze|from|full|fulltext|function|fusion|generated|get|global|goto|grant|greatest|group|grouping|groups|handler|having|high_priority|hold|holdlock|hour|hour_microsecond|hour_minute|hour_second|identity|identity_insert|identitycol|if|ignore|ilike|in|index|indicator|infile|initial|initially|inner|inout|insensitive|insert|int|int1|int2|int3|int4|int8|integer|intersect|intersection|interval|into|io_after_gtids|io_before_gtids|is|isnull|iterate|join|json|json_array|json_arrayagg|json_exists|json_object|json_objectagg|json_query|json_scalar|json_serialize|json_table|json_table_primitive|json_value|key|keys|kill|lag|language|large|last_value|lateral|lead|leading|least|leave|left|like|like_regex|limit|linear|lineno|lines|listagg|ln|load|local|localtime|localtimestamp|lock|log|log10|long|longblob|longtext|loop|low_priority|lower|lpad|ltrim|master_bind|master_ssl_verify_server_cert|match|match_number|match_recognize|matches|max|maxvalue|mediumblob|mediumint|mediumtext|member|merge|method|middleint|min|minute|minute_microsecond|minute_second|mod|modifies|module|month|multiset|national|natural|nchar|nclob|new|no|no_write_to_binlog|nocheck|nonclustered|none|normalize|not|notnull|nth_value|ntile|null|nullif|numeric|occurrences_regex|octet_length|of|off|offset|offsets|old|omit|on|one|only|open|opendatasource|openquery|openrowset|openxml|optimize|optimizer_costs|option|optionally|or|order|out|outer|outfile|over|overlaps|overlay|parameter|partition|pattern|per|percent|percent_rank|percentile_cont|percentile_disc|period|pivot|placing|plan|portion|position|position_regex|power|precedes|precision|prepare|primary|print|proc|procedure|ptf|public|purge|raiserror|range|rank|read|read_write|reads|readtext|real|reconfigure|recursive|ref|references|referencing|regexp|regr_avgx|regr_avgy|regr_count|regr_intercept|regr_r2|regr_slope|regr_sxx|regr_sxy|regr_syy|release|rename|repeat|replace|replication|require|resignal|restore|restrict|result|return|returning|returns|revert|revoke|right|rlike|rollback|rollup|row|row_number|rowcount|rowguidcol|rows|rpad|rule|running|save|savepoint|schema|schemas|scope|scroll|search|second|second_microsecond|securityaudit|seek|select|semantickeyphrasetable|semanticsimilaritydetailstable|semanticsimilaritytable|sensitive|separator|session_user|set|setuser|show|shutdown|signal|similar|sin|sinh|skip|smallint|some|spatial|specific|specifictype|sql|sql_big_result|sql_calc_found_rows|sql_small_result|sqlexception|sqlstate|sqlwarning|sqrt|ssl|start|starting|static|statistics|stddev_pop|stddev_samp|stored|straight_join|submultiset|subset|substring|substring_regex|succeeds|sum|symmetric|system|system_time|system_user|table|tablesample|tan|tanh|terminated|textsize|then|time|timestamp|timezone_hour|timezone_minute|tinyblob|tinyint|tinytext|to|top|trailing|tran|transaction|translate|translate_regex|translation|treat|trigger|trim|trim_array|true|truncate|try_convert|tsequal|uescape|undo|union|unique|unknown|unlock|unnest|unpivot|unsigned|until|update|updatetext|upper|usage|use|user|using|utc_date|utc_time|utc_timestamp|value|value_of|values|var_pop|var_samp|varbinary|varchar|varcharacter|variadic|varying|verbose|versioning|view|virtual|waitfor|when|whenever|where|while|width_bucket|window|with|within|within_group|without|write|writetext|xor|year|year_month|zerofill"
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
