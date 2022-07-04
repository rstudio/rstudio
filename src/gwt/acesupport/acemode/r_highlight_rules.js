/*
 * r_highlight_rules.js
 *
 * Copyright (C) 2022 by RStudio, PBC
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
var $colorFunctionCalls = false;

define("mode/r_highlight_rules", ["require", "exports", "module"], function(require, exports, module)
{
  function include(rules) {
    var result = new Array(rules.length);
    for (var i = 0; i < rules.length; i++) {
      result[i] = {include: rules[i]};
    }
    return result;
  }

  var oop = require("ace/lib/oop");
  var lang = require("ace/lib/lang");
  var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
  var RainbowParenHighlightRules = require("mode/rainbow_paren_highlight_rules").RainbowParenHighlightRules;
  var Utils = require("mode/utils");

  var reLhsBracket = "[[({]";
  var reRhsBracket = "[\\])}]";

  var RoxygenHighlightRules = function()
  {
    var rules = {};

    rules["start"] = [
      {
        // escaped '@' sign
        token : "comment",
        regex : "@@",
        merge : false
      },
      {
        // latex-style keyword
        token : "keyword",
        regex : "\\\\[a-zA-Z0-9]+",
        merge : false
      },
      {
        // roxygen tag accepting a parameter
        token : ["keyword", "comment"],
        regex : "(@(?:export|field|inheritParams|name|param|rdname|slot|template|useDynLib))(\\s+)(?=[a-zA-Z0-9._-])",
        merge : false,
        next  : "rd-highlight"
      },
      {
        // generic roxygen tag
        token : "keyword",
        regex : "@(?!@)[^ ]*",
        merge : false
      },
      {
        // markdown link with =
        token : ["paren.keyword.operator", "comment"],
        regex : "(\\[)(=)",
        merge : false,
        next  : "markdown-link"
      },
      {
        // markdown link
        token : "paren.keyword.operator",
        regex : "\\[",
        merge : false,
        next  : "markdown-link"
      },
      {
        // markdown: `code`
        token : ["support.function", "support.function", "support.function"],
        regex : "(`+)(.*?[^`])(\\1)",
        merge : false
      },
      {
        // markdown: __strong__
        token: ["comment", "constant.language.boolean"],
        regex: "(\\s+|^)(__.+?__)\\b",
        merge: false
      },
      {
        // markdown: _emphasis_
        token: ["comment", "constant.language.boolean"],
        regex: "(\\s+|^)(_(?=[^_])(?:(?:\\\\.)|(?:[^_\\\\]))*?_)\\b",
        merge: false
      },
      {
        // markdown: **strong**
        token: ["constant.numeric"],
        regex: "([*][*].+?[*][*])",
        merge: false
      },
      {
        // markdown: *emphasis*
        token: ["constant.numeric"],
        regex: "([*](?=[^*])(?:(?:\\\\.)|(?:[^*\\\\]))*?[*])",
        merge: false
      },
      {
        // highlight brackets
        token : "paren.keyword.operator",
        regex : "(?:" + reLhsBracket + "|" + reRhsBracket + ")",
        merge : false
      },
      {
        defaultToken: "comment"
      }
    ];

    rules["highlight"] = [
      {
        // highlight non-comma tokens
        token : "identifier.support.function",
        regex : "[^ ,]+"
      },
      {
        // don't highlight commas (e.g. @param a,b,c)
        token : "comment",
        regex : ","
      },
      {
        // escape this state and eat whitespace
        token : "comment",
        regex : "\\s*",
        next  : "start"
      }
    ];

    rules["markdown-link"] = [
      {
        // escape when we find a ']'
        token : "paren.keyword.operator",
        regex : "\\]",
        next  : "start"
      },
      {
        // package qualifier: 'pkg::'
        token : ["identifier.support.class", "comment"],
        regex : "([a-zA-Z0-9_.]+)(:{1,3})"
      },
      {
        // quoted function or object
        token : "support.function",
        regex : "`.*?`"
      },
      {
        // non-parens
        token : "support.function",
        regex : "[^{}()[\\]]+"
      },
      {
        // brackets
        token : "paren.keyword.operator",
        regex : "(?:" + reLhsBracket + "|" + reRhsBracket + ")"
      },
      {
        defaultToken: "comment"
      }
    ];


    this.$rules = rules;
    this.normalizeRules();
  };

  oop.inherits(RoxygenHighlightRules, TextHighlightRules);

  var isColorBright = function(col)
  {
    // based on https://github.com/bgrins/TinyColor
    var rgb = parseInt(col, 16); // convert rrggbb to decimal
    var r = (rgb >> 16) & 0xff;  // extract red
    var g = (rgb >>  8) & 0xff;  // extract green
    var b = (rgb >>  0) & 0xff;  // extract blue
    
    return (r * 299 + g * 587 + b * 114) / 1000 > 128;
  };

  var RHighlightRules = function()
  {
    // NOTE: The backslash character is an alias for the 'function' symbol,
    // and can be used for defining short-hand functions, e.g.
    //
    //     \(x) x + 1
    //
    // It was introduced with R 4.2.0.
    var keywords = lang.arrayToMap([
      "\\", "function", "if", "else", "in",
      "break", "next", "repeat", "for", "while"
    ]);

    var specialFunctions = lang.arrayToMap([
      "return", "switch", "try", "tryCatch", "stop",
      "warning", "require", "library", "attach", "detach",
      "source", "setMethod", "setGeneric", "setGroupGeneric",
      "setClass", "setRefClass", "R6Class", "UseMethod", "NextMethod"
    ]);

    var builtinConstants = lang.arrayToMap([
      "NULL", "NA", "TRUE", "FALSE", "T", "F", "Inf",
      "NaN", "NA_integer_", "NA_real_", "NA_character_",
      "NA_complex_"
    ]);

    /* 
      # R code to regenerate: 
      cols <- col2rgb(colors())
      cols[] <- sub(" ", "0", sprintf("%2x", col2rgb(colors())))
      cols <- apply(cols, 2, paste, collapse = "")

      cat(paste0(
        'builtInColors.set("', colors(), '", "', cols, '");'
      ), sep = "\n")

    */
    var builtInColors = new Map();
    builtInColors.set("white", "ffffff");
    builtInColors.set("aliceblue", "f0f8ff");
    builtInColors.set("antiquewhite", "faebd7");
    builtInColors.set("antiquewhite1", "ffefdb");
    builtInColors.set("antiquewhite2", "eedfcc");
    builtInColors.set("antiquewhite3", "cdc0b0");
    builtInColors.set("antiquewhite4", "8b8378");
    builtInColors.set("aquamarine", "7fffd4");
    builtInColors.set("aquamarine1", "7fffd4");
    builtInColors.set("aquamarine2", "76eec6");
    builtInColors.set("aquamarine3", "66cdaa");
    builtInColors.set("aquamarine4", "458b74");
    builtInColors.set("azure", "f0ffff");
    builtInColors.set("azure1", "f0ffff");
    builtInColors.set("azure2", "e0eeee");
    builtInColors.set("azure3", "c1cdcd");
    builtInColors.set("azure4", "838b8b");
    builtInColors.set("beige", "f5f5dc");
    builtInColors.set("bisque", "ffe4c4");
    builtInColors.set("bisque1", "ffe4c4");
    builtInColors.set("bisque2", "eed5b7");
    builtInColors.set("bisque3", "cdb79e");
    builtInColors.set("bisque4", "8b7d6b");
    builtInColors.set("black", "000000");
    builtInColors.set("blanchedalmond", "ffebcd");
    builtInColors.set("blue", "0000ff");
    builtInColors.set("blue1", "0000ff");
    builtInColors.set("blue2", "0000ee");
    builtInColors.set("blue3", "0000cd");
    builtInColors.set("blue4", "00008b");
    builtInColors.set("blueviolet", "8a2be2");
    builtInColors.set("brown", "a52a2a");
    builtInColors.set("brown1", "ff4040");
    builtInColors.set("brown2", "ee3b3b");
    builtInColors.set("brown3", "cd3333");
    builtInColors.set("brown4", "8b2323");
    builtInColors.set("burlywood", "deb887");
    builtInColors.set("burlywood1", "ffd39b");
    builtInColors.set("burlywood2", "eec591");
    builtInColors.set("burlywood3", "cdaa7d");
    builtInColors.set("burlywood4", "8b7355");
    builtInColors.set("cadetblue", "5f9ea0");
    builtInColors.set("cadetblue1", "98f5ff");
    builtInColors.set("cadetblue2", "8ee5ee");
    builtInColors.set("cadetblue3", "7ac5cd");
    builtInColors.set("cadetblue4", "53868b");
    builtInColors.set("chartreuse", "7fff00");
    builtInColors.set("chartreuse1", "7fff00");
    builtInColors.set("chartreuse2", "76ee00");
    builtInColors.set("chartreuse3", "66cd00");
    builtInColors.set("chartreuse4", "458b00");
    builtInColors.set("chocolate", "d2691e");
    builtInColors.set("chocolate1", "ff7f24");
    builtInColors.set("chocolate2", "ee7621");
    builtInColors.set("chocolate3", "cd661d");
    builtInColors.set("chocolate4", "8b4513");
    builtInColors.set("coral", "ff7f50");
    builtInColors.set("coral1", "ff7256");
    builtInColors.set("coral2", "ee6a50");
    builtInColors.set("coral3", "cd5b45");
    builtInColors.set("coral4", "8b3e2f");
    builtInColors.set("cornflowerblue", "6495ed");
    builtInColors.set("cornsilk", "fff8dc");
    builtInColors.set("cornsilk1", "fff8dc");
    builtInColors.set("cornsilk2", "eee8cd");
    builtInColors.set("cornsilk3", "cdc8b1");
    builtInColors.set("cornsilk4", "8b8878");
    builtInColors.set("cyan", "00ffff");
    builtInColors.set("cyan1", "00ffff");
    builtInColors.set("cyan2", "00eeee");
    builtInColors.set("cyan3", "00cdcd");
    builtInColors.set("cyan4", "008b8b");
    builtInColors.set("darkblue", "00008b");
    builtInColors.set("darkcyan", "008b8b");
    builtInColors.set("darkgoldenrod", "b8860b");
    builtInColors.set("darkgoldenrod1", "ffb90f");
    builtInColors.set("darkgoldenrod2", "eead0e");
    builtInColors.set("darkgoldenrod3", "cd950c");
    builtInColors.set("darkgoldenrod4", "8b6508");
    builtInColors.set("darkgray", "a9a9a9");
    builtInColors.set("darkgreen", "006400");
    builtInColors.set("darkgrey", "a9a9a9");
    builtInColors.set("darkkhaki", "bdb76b");
    builtInColors.set("darkmagenta", "8b008b");
    builtInColors.set("darkolivegreen", "556b2f");
    builtInColors.set("darkolivegreen1", "caff70");
    builtInColors.set("darkolivegreen2", "bcee68");
    builtInColors.set("darkolivegreen3", "a2cd5a");
    builtInColors.set("darkolivegreen4", "6e8b3d");
    builtInColors.set("darkorange", "ff8c00");
    builtInColors.set("darkorange1", "ff7f00");
    builtInColors.set("darkorange2", "ee7600");
    builtInColors.set("darkorange3", "cd6600");
    builtInColors.set("darkorange4", "8b4500");
    builtInColors.set("darkorchid", "9932cc");
    builtInColors.set("darkorchid1", "bf3eff");
    builtInColors.set("darkorchid2", "b23aee");
    builtInColors.set("darkorchid3", "9a32cd");
    builtInColors.set("darkorchid4", "68228b");
    builtInColors.set("darkred", "8b0000");
    builtInColors.set("darksalmon", "e9967a");
    builtInColors.set("darkseagreen", "8fbc8f");
    builtInColors.set("darkseagreen1", "c1ffc1");
    builtInColors.set("darkseagreen2", "b4eeb4");
    builtInColors.set("darkseagreen3", "9bcd9b");
    builtInColors.set("darkseagreen4", "698b69");
    builtInColors.set("darkslateblue", "483d8b");
    builtInColors.set("darkslategray", "2f4f4f");
    builtInColors.set("darkslategray1", "97ffff");
    builtInColors.set("darkslategray2", "8deeee");
    builtInColors.set("darkslategray3", "79cdcd");
    builtInColors.set("darkslategray4", "528b8b");
    builtInColors.set("darkslategrey", "2f4f4f");
    builtInColors.set("darkturquoise", "00ced1");
    builtInColors.set("darkviolet", "9400d3");
    builtInColors.set("deeppink", "ff1493");
    builtInColors.set("deeppink1", "ff1493");
    builtInColors.set("deeppink2", "ee1289");
    builtInColors.set("deeppink3", "cd1076");
    builtInColors.set("deeppink4", "8b0a50");
    builtInColors.set("deepskyblue", "00bfff");
    builtInColors.set("deepskyblue1", "00bfff");
    builtInColors.set("deepskyblue2", "00b2ee");
    builtInColors.set("deepskyblue3", "009acd");
    builtInColors.set("deepskyblue4", "00688b");
    builtInColors.set("dimgray", "696969");
    builtInColors.set("dimgrey", "696969");
    builtInColors.set("dodgerblue", "1e90ff");
    builtInColors.set("dodgerblue1", "1e90ff");
    builtInColors.set("dodgerblue2", "1c86ee");
    builtInColors.set("dodgerblue3", "1874cd");
    builtInColors.set("dodgerblue4", "104e8b");
    builtInColors.set("firebrick", "b22222");
    builtInColors.set("firebrick1", "ff3030");
    builtInColors.set("firebrick2", "ee2c2c");
    builtInColors.set("firebrick3", "cd2626");
    builtInColors.set("firebrick4", "8b1a1a");
    builtInColors.set("floralwhite", "fffaf0");
    builtInColors.set("forestgreen", "228b22");
    builtInColors.set("gainsboro", "dcdcdc");
    builtInColors.set("ghostwhite", "f8f8ff");
    builtInColors.set("gold", "ffd700");
    builtInColors.set("gold1", "ffd700");
    builtInColors.set("gold2", "eec900");
    builtInColors.set("gold3", "cdad00");
    builtInColors.set("gold4", "8b7500");
    builtInColors.set("goldenrod", "daa520");
    builtInColors.set("goldenrod1", "ffc125");
    builtInColors.set("goldenrod2", "eeb422");
    builtInColors.set("goldenrod3", "cd9b1d");
    builtInColors.set("goldenrod4", "8b6914");
    builtInColors.set("gray", "bebebe");
    builtInColors.set("gray0", "000000");
    builtInColors.set("gray1", "030303");
    builtInColors.set("gray2", "050505");
    builtInColors.set("gray3", "080808");
    builtInColors.set("gray4", "0a0a0a");
    builtInColors.set("gray5", "0d0d0d");
    builtInColors.set("gray6", "0f0f0f");
    builtInColors.set("gray7", "121212");
    builtInColors.set("gray8", "141414");
    builtInColors.set("gray9", "171717");
    builtInColors.set("gray10", "1a1a1a");
    builtInColors.set("gray11", "1c1c1c");
    builtInColors.set("gray12", "1f1f1f");
    builtInColors.set("gray13", "212121");
    builtInColors.set("gray14", "242424");
    builtInColors.set("gray15", "262626");
    builtInColors.set("gray16", "292929");
    builtInColors.set("gray17", "2b2b2b");
    builtInColors.set("gray18", "2e2e2e");
    builtInColors.set("gray19", "303030");
    builtInColors.set("gray20", "333333");
    builtInColors.set("gray21", "363636");
    builtInColors.set("gray22", "383838");
    builtInColors.set("gray23", "3b3b3b");
    builtInColors.set("gray24", "3d3d3d");
    builtInColors.set("gray25", "404040");
    builtInColors.set("gray26", "424242");
    builtInColors.set("gray27", "454545");
    builtInColors.set("gray28", "474747");
    builtInColors.set("gray29", "4a4a4a");
    builtInColors.set("gray30", "4d4d4d");
    builtInColors.set("gray31", "4f4f4f");
    builtInColors.set("gray32", "525252");
    builtInColors.set("gray33", "545454");
    builtInColors.set("gray34", "575757");
    builtInColors.set("gray35", "595959");
    builtInColors.set("gray36", "5c5c5c");
    builtInColors.set("gray37", "5e5e5e");
    builtInColors.set("gray38", "616161");
    builtInColors.set("gray39", "636363");
    builtInColors.set("gray40", "666666");
    builtInColors.set("gray41", "696969");
    builtInColors.set("gray42", "6b6b6b");
    builtInColors.set("gray43", "6e6e6e");
    builtInColors.set("gray44", "707070");
    builtInColors.set("gray45", "737373");
    builtInColors.set("gray46", "757575");
    builtInColors.set("gray47", "787878");
    builtInColors.set("gray48", "7a7a7a");
    builtInColors.set("gray49", "7d7d7d");
    builtInColors.set("gray50", "7f7f7f");
    builtInColors.set("gray51", "828282");
    builtInColors.set("gray52", "858585");
    builtInColors.set("gray53", "878787");
    builtInColors.set("gray54", "8a8a8a");
    builtInColors.set("gray55", "8c8c8c");
    builtInColors.set("gray56", "8f8f8f");
    builtInColors.set("gray57", "919191");
    builtInColors.set("gray58", "949494");
    builtInColors.set("gray59", "969696");
    builtInColors.set("gray60", "999999");
    builtInColors.set("gray61", "9c9c9c");
    builtInColors.set("gray62", "9e9e9e");
    builtInColors.set("gray63", "a1a1a1");
    builtInColors.set("gray64", "a3a3a3");
    builtInColors.set("gray65", "a6a6a6");
    builtInColors.set("gray66", "a8a8a8");
    builtInColors.set("gray67", "ababab");
    builtInColors.set("gray68", "adadad");
    builtInColors.set("gray69", "b0b0b0");
    builtInColors.set("gray70", "b3b3b3");
    builtInColors.set("gray71", "b5b5b5");
    builtInColors.set("gray72", "b8b8b8");
    builtInColors.set("gray73", "bababa");
    builtInColors.set("gray74", "bdbdbd");
    builtInColors.set("gray75", "bfbfbf");
    builtInColors.set("gray76", "c2c2c2");
    builtInColors.set("gray77", "c4c4c4");
    builtInColors.set("gray78", "c7c7c7");
    builtInColors.set("gray79", "c9c9c9");
    builtInColors.set("gray80", "cccccc");
    builtInColors.set("gray81", "cfcfcf");
    builtInColors.set("gray82", "d1d1d1");
    builtInColors.set("gray83", "d4d4d4");
    builtInColors.set("gray84", "d6d6d6");
    builtInColors.set("gray85", "d9d9d9");
    builtInColors.set("gray86", "dbdbdb");
    builtInColors.set("gray87", "dedede");
    builtInColors.set("gray88", "e0e0e0");
    builtInColors.set("gray89", "e3e3e3");
    builtInColors.set("gray90", "e5e5e5");
    builtInColors.set("gray91", "e8e8e8");
    builtInColors.set("gray92", "ebebeb");
    builtInColors.set("gray93", "ededed");
    builtInColors.set("gray94", "f0f0f0");
    builtInColors.set("gray95", "f2f2f2");
    builtInColors.set("gray96", "f5f5f5");
    builtInColors.set("gray97", "f7f7f7");
    builtInColors.set("gray98", "fafafa");
    builtInColors.set("gray99", "fcfcfc");
    builtInColors.set("gray100", "ffffff");
    builtInColors.set("green", "00ff00");
    builtInColors.set("green1", "00ff00");
    builtInColors.set("green2", "00ee00");
    builtInColors.set("green3", "00cd00");
    builtInColors.set("green4", "008b00");
    builtInColors.set("greenyellow", "adff2f");
    builtInColors.set("grey", "bebebe");
    builtInColors.set("grey0", "000000");
    builtInColors.set("grey1", "030303");
    builtInColors.set("grey2", "050505");
    builtInColors.set("grey3", "080808");
    builtInColors.set("grey4", "0a0a0a");
    builtInColors.set("grey5", "0d0d0d");
    builtInColors.set("grey6", "0f0f0f");
    builtInColors.set("grey7", "121212");
    builtInColors.set("grey8", "141414");
    builtInColors.set("grey9", "171717");
    builtInColors.set("grey10", "1a1a1a");
    builtInColors.set("grey11", "1c1c1c");
    builtInColors.set("grey12", "1f1f1f");
    builtInColors.set("grey13", "212121");
    builtInColors.set("grey14", "242424");
    builtInColors.set("grey15", "262626");
    builtInColors.set("grey16", "292929");
    builtInColors.set("grey17", "2b2b2b");
    builtInColors.set("grey18", "2e2e2e");
    builtInColors.set("grey19", "303030");
    builtInColors.set("grey20", "333333");
    builtInColors.set("grey21", "363636");
    builtInColors.set("grey22", "383838");
    builtInColors.set("grey23", "3b3b3b");
    builtInColors.set("grey24", "3d3d3d");
    builtInColors.set("grey25", "404040");
    builtInColors.set("grey26", "424242");
    builtInColors.set("grey27", "454545");
    builtInColors.set("grey28", "474747");
    builtInColors.set("grey29", "4a4a4a");
    builtInColors.set("grey30", "4d4d4d");
    builtInColors.set("grey31", "4f4f4f");
    builtInColors.set("grey32", "525252");
    builtInColors.set("grey33", "545454");
    builtInColors.set("grey34", "575757");
    builtInColors.set("grey35", "595959");
    builtInColors.set("grey36", "5c5c5c");
    builtInColors.set("grey37", "5e5e5e");
    builtInColors.set("grey38", "616161");
    builtInColors.set("grey39", "636363");
    builtInColors.set("grey40", "666666");
    builtInColors.set("grey41", "696969");
    builtInColors.set("grey42", "6b6b6b");
    builtInColors.set("grey43", "6e6e6e");
    builtInColors.set("grey44", "707070");
    builtInColors.set("grey45", "737373");
    builtInColors.set("grey46", "757575");
    builtInColors.set("grey47", "787878");
    builtInColors.set("grey48", "7a7a7a");
    builtInColors.set("grey49", "7d7d7d");
    builtInColors.set("grey50", "7f7f7f");
    builtInColors.set("grey51", "828282");
    builtInColors.set("grey52", "858585");
    builtInColors.set("grey53", "878787");
    builtInColors.set("grey54", "8a8a8a");
    builtInColors.set("grey55", "8c8c8c");
    builtInColors.set("grey56", "8f8f8f");
    builtInColors.set("grey57", "919191");
    builtInColors.set("grey58", "949494");
    builtInColors.set("grey59", "969696");
    builtInColors.set("grey60", "999999");
    builtInColors.set("grey61", "9c9c9c");
    builtInColors.set("grey62", "9e9e9e");
    builtInColors.set("grey63", "a1a1a1");
    builtInColors.set("grey64", "a3a3a3");
    builtInColors.set("grey65", "a6a6a6");
    builtInColors.set("grey66", "a8a8a8");
    builtInColors.set("grey67", "ababab");
    builtInColors.set("grey68", "adadad");
    builtInColors.set("grey69", "b0b0b0");
    builtInColors.set("grey70", "b3b3b3");
    builtInColors.set("grey71", "b5b5b5");
    builtInColors.set("grey72", "b8b8b8");
    builtInColors.set("grey73", "bababa");
    builtInColors.set("grey74", "bdbdbd");
    builtInColors.set("grey75", "bfbfbf");
    builtInColors.set("grey76", "c2c2c2");
    builtInColors.set("grey77", "c4c4c4");
    builtInColors.set("grey78", "c7c7c7");
    builtInColors.set("grey79", "c9c9c9");
    builtInColors.set("grey80", "cccccc");
    builtInColors.set("grey81", "cfcfcf");
    builtInColors.set("grey82", "d1d1d1");
    builtInColors.set("grey83", "d4d4d4");
    builtInColors.set("grey84", "d6d6d6");
    builtInColors.set("grey85", "d9d9d9");
    builtInColors.set("grey86", "dbdbdb");
    builtInColors.set("grey87", "dedede");
    builtInColors.set("grey88", "e0e0e0");
    builtInColors.set("grey89", "e3e3e3");
    builtInColors.set("grey90", "e5e5e5");
    builtInColors.set("grey91", "e8e8e8");
    builtInColors.set("grey92", "ebebeb");
    builtInColors.set("grey93", "ededed");
    builtInColors.set("grey94", "f0f0f0");
    builtInColors.set("grey95", "f2f2f2");
    builtInColors.set("grey96", "f5f5f5");
    builtInColors.set("grey97", "f7f7f7");
    builtInColors.set("grey98", "fafafa");
    builtInColors.set("grey99", "fcfcfc");
    builtInColors.set("grey100", "ffffff");
    builtInColors.set("honeydew", "f0fff0");
    builtInColors.set("honeydew1", "f0fff0");
    builtInColors.set("honeydew2", "e0eee0");
    builtInColors.set("honeydew3", "c1cdc1");
    builtInColors.set("honeydew4", "838b83");
    builtInColors.set("hotpink", "ff69b4");
    builtInColors.set("hotpink1", "ff6eb4");
    builtInColors.set("hotpink2", "ee6aa7");
    builtInColors.set("hotpink3", "cd6090");
    builtInColors.set("hotpink4", "8b3a62");
    builtInColors.set("indianred", "cd5c5c");
    builtInColors.set("indianred1", "ff6a6a");
    builtInColors.set("indianred2", "ee6363");
    builtInColors.set("indianred3", "cd5555");
    builtInColors.set("indianred4", "8b3a3a");
    builtInColors.set("ivory", "fffff0");
    builtInColors.set("ivory1", "fffff0");
    builtInColors.set("ivory2", "eeeee0");
    builtInColors.set("ivory3", "cdcdc1");
    builtInColors.set("ivory4", "8b8b83");
    builtInColors.set("khaki", "f0e68c");
    builtInColors.set("khaki1", "fff68f");
    builtInColors.set("khaki2", "eee685");
    builtInColors.set("khaki3", "cdc673");
    builtInColors.set("khaki4", "8b864e");
    builtInColors.set("lavender", "e6e6fa");
    builtInColors.set("lavenderblush", "fff0f5");
    builtInColors.set("lavenderblush1", "fff0f5");
    builtInColors.set("lavenderblush2", "eee0e5");
    builtInColors.set("lavenderblush3", "cdc1c5");
    builtInColors.set("lavenderblush4", "8b8386");
    builtInColors.set("lawngreen", "7cfc00");
    builtInColors.set("lemonchiffon", "fffacd");
    builtInColors.set("lemonchiffon1", "fffacd");
    builtInColors.set("lemonchiffon2", "eee9bf");
    builtInColors.set("lemonchiffon3", "cdc9a5");
    builtInColors.set("lemonchiffon4", "8b8970");
    builtInColors.set("lightblue", "add8e6");
    builtInColors.set("lightblue1", "bfefff");
    builtInColors.set("lightblue2", "b2dfee");
    builtInColors.set("lightblue3", "9ac0cd");
    builtInColors.set("lightblue4", "68838b");
    builtInColors.set("lightcoral", "f08080");
    builtInColors.set("lightcyan", "e0ffff");
    builtInColors.set("lightcyan1", "e0ffff");
    builtInColors.set("lightcyan2", "d1eeee");
    builtInColors.set("lightcyan3", "b4cdcd");
    builtInColors.set("lightcyan4", "7a8b8b");
    builtInColors.set("lightgoldenrod", "eedd82");
    builtInColors.set("lightgoldenrod1", "ffec8b");
    builtInColors.set("lightgoldenrod2", "eedc82");
    builtInColors.set("lightgoldenrod3", "cdbe70");
    builtInColors.set("lightgoldenrod4", "8b814c");
    builtInColors.set("lightgoldenrodyellow", "fafad2");
    builtInColors.set("lightgray", "d3d3d3");
    builtInColors.set("lightgreen", "90ee90");
    builtInColors.set("lightgrey", "d3d3d3");
    builtInColors.set("lightpink", "ffb6c1");
    builtInColors.set("lightpink1", "ffaeb9");
    builtInColors.set("lightpink2", "eea2ad");
    builtInColors.set("lightpink3", "cd8c95");
    builtInColors.set("lightpink4", "8b5f65");
    builtInColors.set("lightsalmon", "ffa07a");
    builtInColors.set("lightsalmon1", "ffa07a");
    builtInColors.set("lightsalmon2", "ee9572");
    builtInColors.set("lightsalmon3", "cd8162");
    builtInColors.set("lightsalmon4", "8b5742");
    builtInColors.set("lightseagreen", "20b2aa");
    builtInColors.set("lightskyblue", "87cefa");
    builtInColors.set("lightskyblue1", "b0e2ff");
    builtInColors.set("lightskyblue2", "a4d3ee");
    builtInColors.set("lightskyblue3", "8db6cd");
    builtInColors.set("lightskyblue4", "607b8b");
    builtInColors.set("lightslateblue", "8470ff");
    builtInColors.set("lightslategray", "778899");
    builtInColors.set("lightslategrey", "778899");
    builtInColors.set("lightsteelblue", "b0c4de");
    builtInColors.set("lightsteelblue1", "cae1ff");
    builtInColors.set("lightsteelblue2", "bcd2ee");
    builtInColors.set("lightsteelblue3", "a2b5cd");
    builtInColors.set("lightsteelblue4", "6e7b8b");
    builtInColors.set("lightyellow", "ffffe0");
    builtInColors.set("lightyellow1", "ffffe0");
    builtInColors.set("lightyellow2", "eeeed1");
    builtInColors.set("lightyellow3", "cdcdb4");
    builtInColors.set("lightyellow4", "8b8b7a");
    builtInColors.set("limegreen", "32cd32");
    builtInColors.set("linen", "faf0e6");
    builtInColors.set("magenta", "ff00ff");
    builtInColors.set("magenta1", "ff00ff");
    builtInColors.set("magenta2", "ee00ee");
    builtInColors.set("magenta3", "cd00cd");
    builtInColors.set("magenta4", "8b008b");
    builtInColors.set("maroon", "b03060");
    builtInColors.set("maroon1", "ff34b3");
    builtInColors.set("maroon2", "ee30a7");
    builtInColors.set("maroon3", "cd2990");
    builtInColors.set("maroon4", "8b1c62");
    builtInColors.set("mediumaquamarine", "66cdaa");
    builtInColors.set("mediumblue", "0000cd");
    builtInColors.set("mediumorchid", "ba55d3");
    builtInColors.set("mediumorchid1", "e066ff");
    builtInColors.set("mediumorchid2", "d15fee");
    builtInColors.set("mediumorchid3", "b452cd");
    builtInColors.set("mediumorchid4", "7a378b");
    builtInColors.set("mediumpurple", "9370db");
    builtInColors.set("mediumpurple1", "ab82ff");
    builtInColors.set("mediumpurple2", "9f79ee");
    builtInColors.set("mediumpurple3", "8968cd");
    builtInColors.set("mediumpurple4", "5d478b");
    builtInColors.set("mediumseagreen", "3cb371");
    builtInColors.set("mediumslateblue", "7b68ee");
    builtInColors.set("mediumspringgreen", "00fa9a");
    builtInColors.set("mediumturquoise", "48d1cc");
    builtInColors.set("mediumvioletred", "c71585");
    builtInColors.set("midnightblue", "191970");
    builtInColors.set("mintcream", "f5fffa");
    builtInColors.set("mistyrose", "ffe4e1");
    builtInColors.set("mistyrose1", "ffe4e1");
    builtInColors.set("mistyrose2", "eed5d2");
    builtInColors.set("mistyrose3", "cdb7b5");
    builtInColors.set("mistyrose4", "8b7d7b");
    builtInColors.set("moccasin", "ffe4b5");
    builtInColors.set("navajowhite", "ffdead");
    builtInColors.set("navajowhite1", "ffdead");
    builtInColors.set("navajowhite2", "eecfa1");
    builtInColors.set("navajowhite3", "cdb38b");
    builtInColors.set("navajowhite4", "8b795e");
    builtInColors.set("navy", "000080");
    builtInColors.set("navyblue", "000080");
    builtInColors.set("oldlace", "fdf5e6");
    builtInColors.set("olivedrab", "6b8e23");
    builtInColors.set("olivedrab1", "c0ff3e");
    builtInColors.set("olivedrab2", "b3ee3a");
    builtInColors.set("olivedrab3", "9acd32");
    builtInColors.set("olivedrab4", "698b22");
    builtInColors.set("orange", "ffa500");
    builtInColors.set("orange1", "ffa500");
    builtInColors.set("orange2", "ee9a00");
    builtInColors.set("orange3", "cd8500");
    builtInColors.set("orange4", "8b5a00");
    builtInColors.set("orangered", "ff4500");
    builtInColors.set("orangered1", "ff4500");
    builtInColors.set("orangered2", "ee4000");
    builtInColors.set("orangered3", "cd3700");
    builtInColors.set("orangered4", "8b2500");
    builtInColors.set("orchid", "da70d6");
    builtInColors.set("orchid1", "ff83fa");
    builtInColors.set("orchid2", "ee7ae9");
    builtInColors.set("orchid3", "cd69c9");
    builtInColors.set("orchid4", "8b4789");
    builtInColors.set("palegoldenrod", "eee8aa");
    builtInColors.set("palegreen", "98fb98");
    builtInColors.set("palegreen1", "9aff9a");
    builtInColors.set("palegreen2", "90ee90");
    builtInColors.set("palegreen3", "7ccd7c");
    builtInColors.set("palegreen4", "548b54");
    builtInColors.set("paleturquoise", "afeeee");
    builtInColors.set("paleturquoise1", "bbffff");
    builtInColors.set("paleturquoise2", "aeeeee");
    builtInColors.set("paleturquoise3", "96cdcd");
    builtInColors.set("paleturquoise4", "668b8b");
    builtInColors.set("palevioletred", "db7093");
    builtInColors.set("palevioletred1", "ff82ab");
    builtInColors.set("palevioletred2", "ee799f");
    builtInColors.set("palevioletred3", "cd6889");
    builtInColors.set("palevioletred4", "8b475d");
    builtInColors.set("papayawhip", "ffefd5");
    builtInColors.set("peachpuff", "ffdab9");
    builtInColors.set("peachpuff1", "ffdab9");
    builtInColors.set("peachpuff2", "eecbad");
    builtInColors.set("peachpuff3", "cdaf95");
    builtInColors.set("peachpuff4", "8b7765");
    builtInColors.set("peru", "cd853f");
    builtInColors.set("pink", "ffc0cb");
    builtInColors.set("pink1", "ffb5c5");
    builtInColors.set("pink2", "eea9b8");
    builtInColors.set("pink3", "cd919e");
    builtInColors.set("pink4", "8b636c");
    builtInColors.set("plum", "dda0dd");
    builtInColors.set("plum1", "ffbbff");
    builtInColors.set("plum2", "eeaeee");
    builtInColors.set("plum3", "cd96cd");
    builtInColors.set("plum4", "8b668b");
    builtInColors.set("powderblue", "b0e0e6");
    builtInColors.set("purple", "a020f0");
    builtInColors.set("purple1", "9b30ff");
    builtInColors.set("purple2", "912cee");
    builtInColors.set("purple3", "7d26cd");
    builtInColors.set("purple4", "551a8b");
    builtInColors.set("red", "ff0000");
    builtInColors.set("red1", "ff0000");
    builtInColors.set("red2", "ee0000");
    builtInColors.set("red3", "cd0000");
    builtInColors.set("red4", "8b0000");
    builtInColors.set("rosybrown", "bc8f8f");
    builtInColors.set("rosybrown1", "ffc1c1");
    builtInColors.set("rosybrown2", "eeb4b4");
    builtInColors.set("rosybrown3", "cd9b9b");
    builtInColors.set("rosybrown4", "8b6969");
    builtInColors.set("royalblue", "4169e1");
    builtInColors.set("royalblue1", "4876ff");
    builtInColors.set("royalblue2", "436eee");
    builtInColors.set("royalblue3", "3a5fcd");
    builtInColors.set("royalblue4", "27408b");
    builtInColors.set("saddlebrown", "8b4513");
    builtInColors.set("salmon", "fa8072");
    builtInColors.set("salmon1", "ff8c69");
    builtInColors.set("salmon2", "ee8262");
    builtInColors.set("salmon3", "cd7054");
    builtInColors.set("salmon4", "8b4c39");
    builtInColors.set("sandybrown", "f4a460");
    builtInColors.set("seagreen", "2e8b57");
    builtInColors.set("seagreen1", "54ff9f");
    builtInColors.set("seagreen2", "4eee94");
    builtInColors.set("seagreen3", "43cd80");
    builtInColors.set("seagreen4", "2e8b57");
    builtInColors.set("seashell", "fff5ee");
    builtInColors.set("seashell1", "fff5ee");
    builtInColors.set("seashell2", "eee5de");
    builtInColors.set("seashell3", "cdc5bf");
    builtInColors.set("seashell4", "8b8682");
    builtInColors.set("sienna", "a0522d");
    builtInColors.set("sienna1", "ff8247");
    builtInColors.set("sienna2", "ee7942");
    builtInColors.set("sienna3", "cd6839");
    builtInColors.set("sienna4", "8b4726");
    builtInColors.set("skyblue", "87ceeb");
    builtInColors.set("skyblue1", "87ceff");
    builtInColors.set("skyblue2", "7ec0ee");
    builtInColors.set("skyblue3", "6ca6cd");
    builtInColors.set("skyblue4", "4a708b");
    builtInColors.set("slateblue", "6a5acd");
    builtInColors.set("slateblue1", "836fff");
    builtInColors.set("slateblue2", "7a67ee");
    builtInColors.set("slateblue3", "6959cd");
    builtInColors.set("slateblue4", "473c8b");
    builtInColors.set("slategray", "708090");
    builtInColors.set("slategray1", "c6e2ff");
    builtInColors.set("slategray2", "b9d3ee");
    builtInColors.set("slategray3", "9fb6cd");
    builtInColors.set("slategray4", "6c7b8b");
    builtInColors.set("slategrey", "708090");
    builtInColors.set("snow", "fffafa");
    builtInColors.set("snow1", "fffafa");
    builtInColors.set("snow2", "eee9e9");
    builtInColors.set("snow3", "cdc9c9");
    builtInColors.set("snow4", "8b8989");
    builtInColors.set("springgreen", "00ff7f");
    builtInColors.set("springgreen1", "00ff7f");
    builtInColors.set("springgreen2", "00ee76");
    builtInColors.set("springgreen3", "00cd66");
    builtInColors.set("springgreen4", "008b45");
    builtInColors.set("steelblue", "4682b4");
    builtInColors.set("steelblue1", "63b8ff");
    builtInColors.set("steelblue2", "5cacee");
    builtInColors.set("steelblue3", "4f94cd");
    builtInColors.set("steelblue4", "36648b");
    builtInColors.set("tan", "d2b48c");
    builtInColors.set("tan1", "ffa54f");
    builtInColors.set("tan2", "ee9a49");
    builtInColors.set("tan3", "cd853f");
    builtInColors.set("tan4", "8b5a2b");
    builtInColors.set("thistle", "d8bfd8");
    builtInColors.set("thistle1", "ffe1ff");
    builtInColors.set("thistle2", "eed2ee");
    builtInColors.set("thistle3", "cdb5cd");
    builtInColors.set("thistle4", "8b7b8b");
    builtInColors.set("tomato", "ff6347");
    builtInColors.set("tomato1", "ff6347");
    builtInColors.set("tomato2", "ee5c42");
    builtInColors.set("tomato3", "cd4f39");
    builtInColors.set("tomato4", "8b3626");
    builtInColors.set("turquoise", "40e0d0");
    builtInColors.set("turquoise1", "00f5ff");
    builtInColors.set("turquoise2", "00e5ee");
    builtInColors.set("turquoise3", "00c5cd");
    builtInColors.set("turquoise4", "00868b");
    builtInColors.set("violet", "ee82ee");
    builtInColors.set("violetred", "d02090");
    builtInColors.set("violetred1", "ff3e96");
    builtInColors.set("violetred2", "ee3a8c");
    builtInColors.set("violetred3", "cd3278");
    builtInColors.set("violetred4", "8b2252");
    builtInColors.set("wheat", "f5deb3");
    builtInColors.set("wheat1", "ffe7ba");
    builtInColors.set("wheat2", "eed8ae");
    builtInColors.set("wheat3", "cdba96");
    builtInColors.set("wheat4", "8b7e66");
    builtInColors.set("whitesmoke", "f5f5f5");
    builtInColors.set("yellow", "ffff00");
    builtInColors.set("yellow1", "ffff00");
    builtInColors.set("yellow2", "eeee00");
    builtInColors.set("yellow3", "cdcd00");
    builtInColors.set("yellow4", "8b8b00");
    builtInColors.set("yellowgreen", "9acd32");


    // NOTE: We accept '\' as a standalone identifier here
    // so that it can be parsed as the 'function' alias symbol.
    // 
    // Unicode escapes are picked to conform with TR31:
    // https://unicode.org/reports/tr31/#Default_Identifier_Syntax
    var reIdentifier = String.raw`(?:\\|_|[\p{L}\p{Nl}.][\p{L}\p{Nl}\p{Mn}\p{Mc}\p{Nd}\p{Pc}.]*)`;

    var $complements = {
      "{" : "}",
      "[" : "]",
      "(" : ")"
    };

    var rules = {};

    // Define rule sub-blocks that can be included to create
    // full rule states.
    rules["#comment"] = [
      {
        token : "comment.sectionhead",
        regex : "#+(?!').*(?:----|====|####)\\s*$",
        next  : "start"
      },
      {
        // R Markdown chunk metadata comments
        token : "comment.doc.tag",
        regex : "#\\s*[|].*$",
        next  : "start"
      },
      {
        // Begin Roxygen with todo
        token : ["comment", "comment.keyword.operator"],
        regex : "(#+['*]\\s*)(TODO|FIXME)\\b",
        next  : "rd-start"
      },
      {
        // Roxygen
        token : "comment",
        regex : "#+['*]",
        next  : "rd-start"
      },
      {
        // todo in plain comment
        token : ["comment", "comment.keyword.operator", "comment"],
        regex : "(#+\\s*)(TODO|FIXME)\\b(.*)$",
        next  : "start"
      },
      {
        token : "comment",
        regex : "#.*$",
        next  : "start"
      }
    ];

    rules["#string"] = [
      {
        token : "string",
        regex : "[rR]['\"][-]*[[({]",
        next  : "rawstring",
        onMatch: function(value, state, stack, line) {
          
          // initialize stack
          stack = stack || [];

          // save current state in stack
          stack[0] = state;

          // save the name of the next state
          // (needed because state names can be mutated in multi-mode documents)
          stack[1] = this.next;

          // save the expected suffix for exit
          stack[2] =
            $complements[value[value.length - 1]] +
            value.substring(2, value.length - 1) +
            value[1];

          return this.token;
        }
      },
      {
        token : "string", // hex color #rrggbb or #rrggbbaa
        regex : '(["\'])(#[0-9a-fA-F]{6})([0-9a-fA-F]{2})?(\\1)',
        next  : "start", 
        onMatch: function(value, state, stack, line) {
          var quote = value.substring(0,1);
          var col = value.substring(2, value.length - 1);
          var textColor = isColorBright(col.substring(0, 6)) ? "black" : "white";
          return [
              { type: "string", value: quote },
              { type: "string.hexcolor", value: "#" + col, style: "background: #"+col+"; color: " + textColor + " !important;" }, 
              { type: "string", value: quote }
          ];
        }
      },
      {
        token : "string", // hex color #rgb
        regex : '(["\'])(#[0-9a-fA-F]{3})(\\1)',
        next  : "start", 
        onMatch: function(value, state, stack, line) {
          var quote = value.substring(0, 1);
          var col = value.substring(2, value.length - 1); 
          var textColor = isColorBright(col.replace(/./g, "$&$&")) ? "black" : "white";
          return [
              { type: "string", value: quote },
              { type: "string.hexcolor", value: "#" + col, style: "background: #"+col+"; color: " + textColor + " !important;" }, 
              { type: "string", value: quote }
          ];
        }
      },
      {
        token : "string", // maybe R color strings
        regex : '(["\'])([a-z0-9]+)(\\1)', 
        next  : "start", 
        onMatch: function(value, state, stack, line) {
          var quote = value.substring(0, 1);
          var content = value.substring(1, value.length - 1);
          var rgb = builtInColors.get(content);
          if (rgb === undefined)
          {
            return this.token;
          }
          else
          {
            var textColor = isColorBright(rgb) ? "black" : "white";
            return [
                { type: "string", value: quote },
                { type: "string.hexcolor", value: content, style: "background: #"+rgb+"; color: " + textColor + " !important;" }, 
                { type: "string", value: quote }
            ];
          }
        }
      },
      {
        token : "string", // single line
        regex : '["](?:(?:\\\\.)|(?:[^"\\\\]))*?["]',
        next  : "start"
      },
      {
        token : "string", // single line
        regex : "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']",
        next  : "start"
      },
      {
        token : "string", // multi line string start
        merge : true,
        regex : '["]',
        next : "qqstring"
      },
      {
        token : "string", // multi line string start
        merge : true,
        regex : "[']",
        next : "qstring"
      }
    ];

    rules["#number"] = [
      {
        token : "constant.numeric", // hex
        regex : "0[xX][0-9a-fA-F]+[Li]?",
        merge : false,
        next  : "start"
      },
      {
        token : "constant.numeric", // number + integer
        regex : "(?:(?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))(?:[eE][+-]?\\d*)?[iL]?",
        merge : false,
        next  : "start"
      }
    ];

    rules["#quoted-identifier"] = [
      {
        token : "identifier",
        regex : "[`](?:(?:\\\\.)|(?:[^`\\\\]))*?[`]",
        merge : false,
        next  : "start"
      }
    ];

    rules["#keyword-or-identifier"] = [
      {
        token : function(value)
        {
          if (builtinConstants.hasOwnProperty(value))
            return "constant.language";
          else if (keywords.hasOwnProperty(value))
            return "keyword";
          else if (value.match(/^\.\.\d+$/))
            return "variable.language";
          else
            return "identifier";
        },
        regex   : reIdentifier,
        unicode : true,
        merge   : false,
        next    : "start"
      }
    ];

    rules["#package-access"] = [
      {
        token : function(value) {
          if ($colorFunctionCalls)
            return "identifier.support.class";
          else
            return "identifier";
        },
        regex   : reIdentifier + "(?=\\s*::)",
        unicode : true,
        merge   : false,
        next    : "start"
      }
    ];

    rules["#function-call"] = [
      {
        token : function(value) {
          if ($colorFunctionCalls)
            return "identifier.support.function";
          else
            return "identifier";
        },
        regex   : reIdentifier + "(?=\\s*\\()",
        unicode : true,
        merge   : false,
        next    : "start"
      }
    ];

    rules["#function-call-or-keyword"] = [
      {
        token : function(value) {
          if (specialFunctions.hasOwnProperty(value) || keywords.hasOwnProperty(value))
            return "keyword";
          else if ($colorFunctionCalls)
            return "identifier.support.function";
          else
            return "identifier";
        },
        regex   : reIdentifier + "(?=\\s*\\()",
        unicode : true,
        merge   : false,
        next    : "start"
      }
    ];

    rules["#operator"] = [
      {
        token : "keyword.operator",
        regex : "\\$|@",
        merge : false,
        next  : "afterDollar"
      },
      {
        token : "keyword.operator",
        regex : ":::|::|:=|\\|>|=>|%%|>=|<=|==|!=|<<-|->>|->|<-|\\|\\||&&|=|\\+|-|\\*\\*?|/|\\^|>|<|!|&|\\||~|\\$|:|@|\\?",
        merge : false,
        next  : "start"
      },
      {
        token : "keyword.operator.infix", // infix operators
        regex : "%.*?%",
        merge : false,
        next  : "start"
      },
      RainbowParenHighlightRules.getParenRule(),
      {
        token : function(value) {
          return $colorFunctionCalls ?
            "punctuation.keyword.operator" :
            "punctuation";
        },
        regex : "[;]",
        merge : false,
        next  : "start"
      },
      {
        token : function(value) {
          return $colorFunctionCalls ?
            "punctuation.keyword.operator" :
            "punctuation";
        },
        regex : "[,]",
        merge : false,
        next  : "start"
      }
    ];

    rules["#knitr-embed"] = [
      {
        token: "constant.language",
        regex: "^[<][<][^>]+[>][>]$",
        merge: false
      }
    ];

    rules["#text"] = [
      {
        token : "text",
        regex : "\\s+"
      }
    ];

    // Construct rules from previously defined blocks.
    rules["start"] = include([
      "#comment", "#string", "#number",
      "#package-access", "#quoted-identifier",
      "#function-call-or-keyword", "#keyword-or-identifier",
      "#knitr-embed", "#operator", "#text"
    ]);

    rules["afterDollar"] = include([
      "#comment", "#string", "#number",
      "#quoted-identifier",
      "#function-call", "#keyword-or-identifier",
      "#operator", "#text"
    ]);

    rules["rawstring"] = [

      // attempt to match the end of the raw string. be permissive
      // in what the regular expression matches, but validate that
      // the matched string is indeed the expected suffix based on
      // what was provided when we entered the 'rawstring' state
      {
        token : "string",
        regex : "[\\]})][-]*['\"]",
        onMatch: function(value, state, stack, line) {
          this.next = (value === stack[2]) ? stack[0] : stack[1];
          return this.token;
        }
      },

      {
        defaultToken : "string"
      }
    ];

    rules["qqstring"] = [
      {
        token : "string",
        regex : '(?:(?:\\\\.)|(?:[^"\\\\]))*?"',
        next  : "start"
      },
      {
        token : "string",
        regex : '.+',
        merge : true
      }
    ];

    rules["qstring"] = [
      {
        token : "string",
        regex : "(?:(?:\\\\.)|(?:[^'\\\\]))*?'",
        next  : "start"
      },
      {
        token : "string",
        regex : '.+',
        merge : true
      }
    ];

    this.$rules = rules;


    // Embed Roxygen highlight Roxygen highlight rules
    var rdRules = new RoxygenHighlightRules().getRules();

    // Add 'virtual-comment' to embedded rules
    for (var state in rdRules) {
      var rules = rdRules[state];
      for (var i = 0; i < rules.length; i++) {
        if (Utils.isArray(rules[i].token)) {
          for (var j = 0; j < rules[i].token.length; j++)
            rules[i].token[j] += ".virtual-comment";
        } else {
          rules[i].token += ".virtual-comment";
        }
      }
    }

    this.embedRules(rdRules, "rd-", [{
      token : "text",
      regex : "^",
      next  : "start"
    }]);

    this.normalizeRules();
  };

  oop.inherits(RHighlightRules, TextHighlightRules);

  exports.RHighlightRules = RHighlightRules;
  exports.setHighlightRFunctionCalls = function(value) {
    $colorFunctionCalls = value;
  };
});
