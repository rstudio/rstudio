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
var $colorPreview = false;

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

  var colorTokens = function(quote, text, rgb)
  {
    var textColor = isColorBright(rgb.substring(0, 6)) ? "black" : "white";
    return [
      { type: "string", value: quote },
      { type: "string.hexcolor", value: text, style: "background: #"+rgb+"; color: " + textColor + " !important;" }, 
      { type: "string", value: quote }
    ];
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

      cat(
        "var builtInColors = new Map([",
        paste(paste0('  ["', colors(), '", "', cols, '"]'), collapse = ",\n"),
        "]);",
        sep = "\n"
      )
    */
    var builtInColors = new Map([
      ["white", "ffffff"],
      ["aliceblue", "f0f8ff"],
      ["antiquewhite", "faebd7"],
      ["antiquewhite1", "ffefdb"],
      ["antiquewhite2", "eedfcc"],
      ["antiquewhite3", "cdc0b0"],
      ["antiquewhite4", "8b8378"],
      ["aquamarine", "7fffd4"],
      ["aquamarine1", "7fffd4"],
      ["aquamarine2", "76eec6"],
      ["aquamarine3", "66cdaa"],
      ["aquamarine4", "458b74"],
      ["azure", "f0ffff"],
      ["azure1", "f0ffff"],
      ["azure2", "e0eeee"],
      ["azure3", "c1cdcd"],
      ["azure4", "838b8b"],
      ["beige", "f5f5dc"],
      ["bisque", "ffe4c4"],
      ["bisque1", "ffe4c4"],
      ["bisque2", "eed5b7"],
      ["bisque3", "cdb79e"],
      ["bisque4", "8b7d6b"],
      ["black", "000000"],
      ["blanchedalmond", "ffebcd"],
      ["blue", "0000ff"],
      ["blue1", "0000ff"],
      ["blue2", "0000ee"],
      ["blue3", "0000cd"],
      ["blue4", "00008b"],
      ["blueviolet", "8a2be2"],
      ["brown", "a52a2a"],
      ["brown1", "ff4040"],
      ["brown2", "ee3b3b"],
      ["brown3", "cd3333"],
      ["brown4", "8b2323"],
      ["burlywood", "deb887"],
      ["burlywood1", "ffd39b"],
      ["burlywood2", "eec591"],
      ["burlywood3", "cdaa7d"],
      ["burlywood4", "8b7355"],
      ["cadetblue", "5f9ea0"],
      ["cadetblue1", "98f5ff"],
      ["cadetblue2", "8ee5ee"],
      ["cadetblue3", "7ac5cd"],
      ["cadetblue4", "53868b"],
      ["chartreuse", "7fff00"],
      ["chartreuse1", "7fff00"],
      ["chartreuse2", "76ee00"],
      ["chartreuse3", "66cd00"],
      ["chartreuse4", "458b00"],
      ["chocolate", "d2691e"],
      ["chocolate1", "ff7f24"],
      ["chocolate2", "ee7621"],
      ["chocolate3", "cd661d"],
      ["chocolate4", "8b4513"],
      ["coral", "ff7f50"],
      ["coral1", "ff7256"],
      ["coral2", "ee6a50"],
      ["coral3", "cd5b45"],
      ["coral4", "8b3e2f"],
      ["cornflowerblue", "6495ed"],
      ["cornsilk", "fff8dc"],
      ["cornsilk1", "fff8dc"],
      ["cornsilk2", "eee8cd"],
      ["cornsilk3", "cdc8b1"],
      ["cornsilk4", "8b8878"],
      ["cyan", "00ffff"],
      ["cyan1", "00ffff"],
      ["cyan2", "00eeee"],
      ["cyan3", "00cdcd"],
      ["cyan4", "008b8b"],
      ["darkblue", "00008b"],
      ["darkcyan", "008b8b"],
      ["darkgoldenrod", "b8860b"],
      ["darkgoldenrod1", "ffb90f"],
      ["darkgoldenrod2", "eead0e"],
      ["darkgoldenrod3", "cd950c"],
      ["darkgoldenrod4", "8b6508"],
      ["darkgray", "a9a9a9"],
      ["darkgreen", "006400"],
      ["darkgrey", "a9a9a9"],
      ["darkkhaki", "bdb76b"],
      ["darkmagenta", "8b008b"],
      ["darkolivegreen", "556b2f"],
      ["darkolivegreen1", "caff70"],
      ["darkolivegreen2", "bcee68"],
      ["darkolivegreen3", "a2cd5a"],
      ["darkolivegreen4", "6e8b3d"],
      ["darkorange", "ff8c00"],
      ["darkorange1", "ff7f00"],
      ["darkorange2", "ee7600"],
      ["darkorange3", "cd6600"],
      ["darkorange4", "8b4500"],
      ["darkorchid", "9932cc"],
      ["darkorchid1", "bf3eff"],
      ["darkorchid2", "b23aee"],
      ["darkorchid3", "9a32cd"],
      ["darkorchid4", "68228b"],
      ["darkred", "8b0000"],
      ["darksalmon", "e9967a"],
      ["darkseagreen", "8fbc8f"],
      ["darkseagreen1", "c1ffc1"],
      ["darkseagreen2", "b4eeb4"],
      ["darkseagreen3", "9bcd9b"],
      ["darkseagreen4", "698b69"],
      ["darkslateblue", "483d8b"],
      ["darkslategray", "2f4f4f"],
      ["darkslategray1", "97ffff"],
      ["darkslategray2", "8deeee"],
      ["darkslategray3", "79cdcd"],
      ["darkslategray4", "528b8b"],
      ["darkslategrey", "2f4f4f"],
      ["darkturquoise", "00ced1"],
      ["darkviolet", "9400d3"],
      ["deeppink", "ff1493"],
      ["deeppink1", "ff1493"],
      ["deeppink2", "ee1289"],
      ["deeppink3", "cd1076"],
      ["deeppink4", "8b0a50"],
      ["deepskyblue", "00bfff"],
      ["deepskyblue1", "00bfff"],
      ["deepskyblue2", "00b2ee"],
      ["deepskyblue3", "009acd"],
      ["deepskyblue4", "00688b"],
      ["dimgray", "696969"],
      ["dimgrey", "696969"],
      ["dodgerblue", "1e90ff"],
      ["dodgerblue1", "1e90ff"],
      ["dodgerblue2", "1c86ee"],
      ["dodgerblue3", "1874cd"],
      ["dodgerblue4", "104e8b"],
      ["firebrick", "b22222"],
      ["firebrick1", "ff3030"],
      ["firebrick2", "ee2c2c"],
      ["firebrick3", "cd2626"],
      ["firebrick4", "8b1a1a"],
      ["floralwhite", "fffaf0"],
      ["forestgreen", "228b22"],
      ["gainsboro", "dcdcdc"],
      ["ghostwhite", "f8f8ff"],
      ["gold", "ffd700"],
      ["gold1", "ffd700"],
      ["gold2", "eec900"],
      ["gold3", "cdad00"],
      ["gold4", "8b7500"],
      ["goldenrod", "daa520"],
      ["goldenrod1", "ffc125"],
      ["goldenrod2", "eeb422"],
      ["goldenrod3", "cd9b1d"],
      ["goldenrod4", "8b6914"],
      ["gray", "bebebe"],
      ["gray0", "000000"],
      ["gray1", "030303"],
      ["gray2", "050505"],
      ["gray3", "080808"],
      ["gray4", "0a0a0a"],
      ["gray5", "0d0d0d"],
      ["gray6", "0f0f0f"],
      ["gray7", "121212"],
      ["gray8", "141414"],
      ["gray9", "171717"],
      ["gray10", "1a1a1a"],
      ["gray11", "1c1c1c"],
      ["gray12", "1f1f1f"],
      ["gray13", "212121"],
      ["gray14", "242424"],
      ["gray15", "262626"],
      ["gray16", "292929"],
      ["gray17", "2b2b2b"],
      ["gray18", "2e2e2e"],
      ["gray19", "303030"],
      ["gray20", "333333"],
      ["gray21", "363636"],
      ["gray22", "383838"],
      ["gray23", "3b3b3b"],
      ["gray24", "3d3d3d"],
      ["gray25", "404040"],
      ["gray26", "424242"],
      ["gray27", "454545"],
      ["gray28", "474747"],
      ["gray29", "4a4a4a"],
      ["gray30", "4d4d4d"],
      ["gray31", "4f4f4f"],
      ["gray32", "525252"],
      ["gray33", "545454"],
      ["gray34", "575757"],
      ["gray35", "595959"],
      ["gray36", "5c5c5c"],
      ["gray37", "5e5e5e"],
      ["gray38", "616161"],
      ["gray39", "636363"],
      ["gray40", "666666"],
      ["gray41", "696969"],
      ["gray42", "6b6b6b"],
      ["gray43", "6e6e6e"],
      ["gray44", "707070"],
      ["gray45", "737373"],
      ["gray46", "757575"],
      ["gray47", "787878"],
      ["gray48", "7a7a7a"],
      ["gray49", "7d7d7d"],
      ["gray50", "7f7f7f"],
      ["gray51", "828282"],
      ["gray52", "858585"],
      ["gray53", "878787"],
      ["gray54", "8a8a8a"],
      ["gray55", "8c8c8c"],
      ["gray56", "8f8f8f"],
      ["gray57", "919191"],
      ["gray58", "949494"],
      ["gray59", "969696"],
      ["gray60", "999999"],
      ["gray61", "9c9c9c"],
      ["gray62", "9e9e9e"],
      ["gray63", "a1a1a1"],
      ["gray64", "a3a3a3"],
      ["gray65", "a6a6a6"],
      ["gray66", "a8a8a8"],
      ["gray67", "ababab"],
      ["gray68", "adadad"],
      ["gray69", "b0b0b0"],
      ["gray70", "b3b3b3"],
      ["gray71", "b5b5b5"],
      ["gray72", "b8b8b8"],
      ["gray73", "bababa"],
      ["gray74", "bdbdbd"],
      ["gray75", "bfbfbf"],
      ["gray76", "c2c2c2"],
      ["gray77", "c4c4c4"],
      ["gray78", "c7c7c7"],
      ["gray79", "c9c9c9"],
      ["gray80", "cccccc"],
      ["gray81", "cfcfcf"],
      ["gray82", "d1d1d1"],
      ["gray83", "d4d4d4"],
      ["gray84", "d6d6d6"],
      ["gray85", "d9d9d9"],
      ["gray86", "dbdbdb"],
      ["gray87", "dedede"],
      ["gray88", "e0e0e0"],
      ["gray89", "e3e3e3"],
      ["gray90", "e5e5e5"],
      ["gray91", "e8e8e8"],
      ["gray92", "ebebeb"],
      ["gray93", "ededed"],
      ["gray94", "f0f0f0"],
      ["gray95", "f2f2f2"],
      ["gray96", "f5f5f5"],
      ["gray97", "f7f7f7"],
      ["gray98", "fafafa"],
      ["gray99", "fcfcfc"],
      ["gray100", "ffffff"],
      ["green", "00ff00"],
      ["green1", "00ff00"],
      ["green2", "00ee00"],
      ["green3", "00cd00"],
      ["green4", "008b00"],
      ["greenyellow", "adff2f"],
      ["grey", "bebebe"],
      ["grey0", "000000"],
      ["grey1", "030303"],
      ["grey2", "050505"],
      ["grey3", "080808"],
      ["grey4", "0a0a0a"],
      ["grey5", "0d0d0d"],
      ["grey6", "0f0f0f"],
      ["grey7", "121212"],
      ["grey8", "141414"],
      ["grey9", "171717"],
      ["grey10", "1a1a1a"],
      ["grey11", "1c1c1c"],
      ["grey12", "1f1f1f"],
      ["grey13", "212121"],
      ["grey14", "242424"],
      ["grey15", "262626"],
      ["grey16", "292929"],
      ["grey17", "2b2b2b"],
      ["grey18", "2e2e2e"],
      ["grey19", "303030"],
      ["grey20", "333333"],
      ["grey21", "363636"],
      ["grey22", "383838"],
      ["grey23", "3b3b3b"],
      ["grey24", "3d3d3d"],
      ["grey25", "404040"],
      ["grey26", "424242"],
      ["grey27", "454545"],
      ["grey28", "474747"],
      ["grey29", "4a4a4a"],
      ["grey30", "4d4d4d"],
      ["grey31", "4f4f4f"],
      ["grey32", "525252"],
      ["grey33", "545454"],
      ["grey34", "575757"],
      ["grey35", "595959"],
      ["grey36", "5c5c5c"],
      ["grey37", "5e5e5e"],
      ["grey38", "616161"],
      ["grey39", "636363"],
      ["grey40", "666666"],
      ["grey41", "696969"],
      ["grey42", "6b6b6b"],
      ["grey43", "6e6e6e"],
      ["grey44", "707070"],
      ["grey45", "737373"],
      ["grey46", "757575"],
      ["grey47", "787878"],
      ["grey48", "7a7a7a"],
      ["grey49", "7d7d7d"],
      ["grey50", "7f7f7f"],
      ["grey51", "828282"],
      ["grey52", "858585"],
      ["grey53", "878787"],
      ["grey54", "8a8a8a"],
      ["grey55", "8c8c8c"],
      ["grey56", "8f8f8f"],
      ["grey57", "919191"],
      ["grey58", "949494"],
      ["grey59", "969696"],
      ["grey60", "999999"],
      ["grey61", "9c9c9c"],
      ["grey62", "9e9e9e"],
      ["grey63", "a1a1a1"],
      ["grey64", "a3a3a3"],
      ["grey65", "a6a6a6"],
      ["grey66", "a8a8a8"],
      ["grey67", "ababab"],
      ["grey68", "adadad"],
      ["grey69", "b0b0b0"],
      ["grey70", "b3b3b3"],
      ["grey71", "b5b5b5"],
      ["grey72", "b8b8b8"],
      ["grey73", "bababa"],
      ["grey74", "bdbdbd"],
      ["grey75", "bfbfbf"],
      ["grey76", "c2c2c2"],
      ["grey77", "c4c4c4"],
      ["grey78", "c7c7c7"],
      ["grey79", "c9c9c9"],
      ["grey80", "cccccc"],
      ["grey81", "cfcfcf"],
      ["grey82", "d1d1d1"],
      ["grey83", "d4d4d4"],
      ["grey84", "d6d6d6"],
      ["grey85", "d9d9d9"],
      ["grey86", "dbdbdb"],
      ["grey87", "dedede"],
      ["grey88", "e0e0e0"],
      ["grey89", "e3e3e3"],
      ["grey90", "e5e5e5"],
      ["grey91", "e8e8e8"],
      ["grey92", "ebebeb"],
      ["grey93", "ededed"],
      ["grey94", "f0f0f0"],
      ["grey95", "f2f2f2"],
      ["grey96", "f5f5f5"],
      ["grey97", "f7f7f7"],
      ["grey98", "fafafa"],
      ["grey99", "fcfcfc"],
      ["grey100", "ffffff"],
      ["honeydew", "f0fff0"],
      ["honeydew1", "f0fff0"],
      ["honeydew2", "e0eee0"],
      ["honeydew3", "c1cdc1"],
      ["honeydew4", "838b83"],
      ["hotpink", "ff69b4"],
      ["hotpink1", "ff6eb4"],
      ["hotpink2", "ee6aa7"],
      ["hotpink3", "cd6090"],
      ["hotpink4", "8b3a62"],
      ["indianred", "cd5c5c"],
      ["indianred1", "ff6a6a"],
      ["indianred2", "ee6363"],
      ["indianred3", "cd5555"],
      ["indianred4", "8b3a3a"],
      ["ivory", "fffff0"],
      ["ivory1", "fffff0"],
      ["ivory2", "eeeee0"],
      ["ivory3", "cdcdc1"],
      ["ivory4", "8b8b83"],
      ["khaki", "f0e68c"],
      ["khaki1", "fff68f"],
      ["khaki2", "eee685"],
      ["khaki3", "cdc673"],
      ["khaki4", "8b864e"],
      ["lavender", "e6e6fa"],
      ["lavenderblush", "fff0f5"],
      ["lavenderblush1", "fff0f5"],
      ["lavenderblush2", "eee0e5"],
      ["lavenderblush3", "cdc1c5"],
      ["lavenderblush4", "8b8386"],
      ["lawngreen", "7cfc00"],
      ["lemonchiffon", "fffacd"],
      ["lemonchiffon1", "fffacd"],
      ["lemonchiffon2", "eee9bf"],
      ["lemonchiffon3", "cdc9a5"],
      ["lemonchiffon4", "8b8970"],
      ["lightblue", "add8e6"],
      ["lightblue1", "bfefff"],
      ["lightblue2", "b2dfee"],
      ["lightblue3", "9ac0cd"],
      ["lightblue4", "68838b"],
      ["lightcoral", "f08080"],
      ["lightcyan", "e0ffff"],
      ["lightcyan1", "e0ffff"],
      ["lightcyan2", "d1eeee"],
      ["lightcyan3", "b4cdcd"],
      ["lightcyan4", "7a8b8b"],
      ["lightgoldenrod", "eedd82"],
      ["lightgoldenrod1", "ffec8b"],
      ["lightgoldenrod2", "eedc82"],
      ["lightgoldenrod3", "cdbe70"],
      ["lightgoldenrod4", "8b814c"],
      ["lightgoldenrodyellow", "fafad2"],
      ["lightgray", "d3d3d3"],
      ["lightgreen", "90ee90"],
      ["lightgrey", "d3d3d3"],
      ["lightpink", "ffb6c1"],
      ["lightpink1", "ffaeb9"],
      ["lightpink2", "eea2ad"],
      ["lightpink3", "cd8c95"],
      ["lightpink4", "8b5f65"],
      ["lightsalmon", "ffa07a"],
      ["lightsalmon1", "ffa07a"],
      ["lightsalmon2", "ee9572"],
      ["lightsalmon3", "cd8162"],
      ["lightsalmon4", "8b5742"],
      ["lightseagreen", "20b2aa"],
      ["lightskyblue", "87cefa"],
      ["lightskyblue1", "b0e2ff"],
      ["lightskyblue2", "a4d3ee"],
      ["lightskyblue3", "8db6cd"],
      ["lightskyblue4", "607b8b"],
      ["lightslateblue", "8470ff"],
      ["lightslategray", "778899"],
      ["lightslategrey", "778899"],
      ["lightsteelblue", "b0c4de"],
      ["lightsteelblue1", "cae1ff"],
      ["lightsteelblue2", "bcd2ee"],
      ["lightsteelblue3", "a2b5cd"],
      ["lightsteelblue4", "6e7b8b"],
      ["lightyellow", "ffffe0"],
      ["lightyellow1", "ffffe0"],
      ["lightyellow2", "eeeed1"],
      ["lightyellow3", "cdcdb4"],
      ["lightyellow4", "8b8b7a"],
      ["limegreen", "32cd32"],
      ["linen", "faf0e6"],
      ["magenta", "ff00ff"],
      ["magenta1", "ff00ff"],
      ["magenta2", "ee00ee"],
      ["magenta3", "cd00cd"],
      ["magenta4", "8b008b"],
      ["maroon", "b03060"],
      ["maroon1", "ff34b3"],
      ["maroon2", "ee30a7"],
      ["maroon3", "cd2990"],
      ["maroon4", "8b1c62"],
      ["mediumaquamarine", "66cdaa"],
      ["mediumblue", "0000cd"],
      ["mediumorchid", "ba55d3"],
      ["mediumorchid1", "e066ff"],
      ["mediumorchid2", "d15fee"],
      ["mediumorchid3", "b452cd"],
      ["mediumorchid4", "7a378b"],
      ["mediumpurple", "9370db"],
      ["mediumpurple1", "ab82ff"],
      ["mediumpurple2", "9f79ee"],
      ["mediumpurple3", "8968cd"],
      ["mediumpurple4", "5d478b"],
      ["mediumseagreen", "3cb371"],
      ["mediumslateblue", "7b68ee"],
      ["mediumspringgreen", "00fa9a"],
      ["mediumturquoise", "48d1cc"],
      ["mediumvioletred", "c71585"],
      ["midnightblue", "191970"],
      ["mintcream", "f5fffa"],
      ["mistyrose", "ffe4e1"],
      ["mistyrose1", "ffe4e1"],
      ["mistyrose2", "eed5d2"],
      ["mistyrose3", "cdb7b5"],
      ["mistyrose4", "8b7d7b"],
      ["moccasin", "ffe4b5"],
      ["navajowhite", "ffdead"],
      ["navajowhite1", "ffdead"],
      ["navajowhite2", "eecfa1"],
      ["navajowhite3", "cdb38b"],
      ["navajowhite4", "8b795e"],
      ["navy", "000080"],
      ["navyblue", "000080"],
      ["oldlace", "fdf5e6"],
      ["olivedrab", "6b8e23"],
      ["olivedrab1", "c0ff3e"],
      ["olivedrab2", "b3ee3a"],
      ["olivedrab3", "9acd32"],
      ["olivedrab4", "698b22"],
      ["orange", "ffa500"],
      ["orange1", "ffa500"],
      ["orange2", "ee9a00"],
      ["orange3", "cd8500"],
      ["orange4", "8b5a00"],
      ["orangered", "ff4500"],
      ["orangered1", "ff4500"],
      ["orangered2", "ee4000"],
      ["orangered3", "cd3700"],
      ["orangered4", "8b2500"],
      ["orchid", "da70d6"],
      ["orchid1", "ff83fa"],
      ["orchid2", "ee7ae9"],
      ["orchid3", "cd69c9"],
      ["orchid4", "8b4789"],
      ["palegoldenrod", "eee8aa"],
      ["palegreen", "98fb98"],
      ["palegreen1", "9aff9a"],
      ["palegreen2", "90ee90"],
      ["palegreen3", "7ccd7c"],
      ["palegreen4", "548b54"],
      ["paleturquoise", "afeeee"],
      ["paleturquoise1", "bbffff"],
      ["paleturquoise2", "aeeeee"],
      ["paleturquoise3", "96cdcd"],
      ["paleturquoise4", "668b8b"],
      ["palevioletred", "db7093"],
      ["palevioletred1", "ff82ab"],
      ["palevioletred2", "ee799f"],
      ["palevioletred3", "cd6889"],
      ["palevioletred4", "8b475d"],
      ["papayawhip", "ffefd5"],
      ["peachpuff", "ffdab9"],
      ["peachpuff1", "ffdab9"],
      ["peachpuff2", "eecbad"],
      ["peachpuff3", "cdaf95"],
      ["peachpuff4", "8b7765"],
      ["peru", "cd853f"],
      ["pink", "ffc0cb"],
      ["pink1", "ffb5c5"],
      ["pink2", "eea9b8"],
      ["pink3", "cd919e"],
      ["pink4", "8b636c"],
      ["plum", "dda0dd"],
      ["plum1", "ffbbff"],
      ["plum2", "eeaeee"],
      ["plum3", "cd96cd"],
      ["plum4", "8b668b"],
      ["powderblue", "b0e0e6"],
      ["purple", "a020f0"],
      ["purple1", "9b30ff"],
      ["purple2", "912cee"],
      ["purple3", "7d26cd"],
      ["purple4", "551a8b"],
      ["red", "ff0000"],
      ["red1", "ff0000"],
      ["red2", "ee0000"],
      ["red3", "cd0000"],
      ["red4", "8b0000"],
      ["rosybrown", "bc8f8f"],
      ["rosybrown1", "ffc1c1"],
      ["rosybrown2", "eeb4b4"],
      ["rosybrown3", "cd9b9b"],
      ["rosybrown4", "8b6969"],
      ["royalblue", "4169e1"],
      ["royalblue1", "4876ff"],
      ["royalblue2", "436eee"],
      ["royalblue3", "3a5fcd"],
      ["royalblue4", "27408b"],
      ["saddlebrown", "8b4513"],
      ["salmon", "fa8072"],
      ["salmon1", "ff8c69"],
      ["salmon2", "ee8262"],
      ["salmon3", "cd7054"],
      ["salmon4", "8b4c39"],
      ["sandybrown", "f4a460"],
      ["seagreen", "2e8b57"],
      ["seagreen1", "54ff9f"],
      ["seagreen2", "4eee94"],
      ["seagreen3", "43cd80"],
      ["seagreen4", "2e8b57"],
      ["seashell", "fff5ee"],
      ["seashell1", "fff5ee"],
      ["seashell2", "eee5de"],
      ["seashell3", "cdc5bf"],
      ["seashell4", "8b8682"],
      ["sienna", "a0522d"],
      ["sienna1", "ff8247"],
      ["sienna2", "ee7942"],
      ["sienna3", "cd6839"],
      ["sienna4", "8b4726"],
      ["skyblue", "87ceeb"],
      ["skyblue1", "87ceff"],
      ["skyblue2", "7ec0ee"],
      ["skyblue3", "6ca6cd"],
      ["skyblue4", "4a708b"],
      ["slateblue", "6a5acd"],
      ["slateblue1", "836fff"],
      ["slateblue2", "7a67ee"],
      ["slateblue3", "6959cd"],
      ["slateblue4", "473c8b"],
      ["slategray", "708090"],
      ["slategray1", "c6e2ff"],
      ["slategray2", "b9d3ee"],
      ["slategray3", "9fb6cd"],
      ["slategray4", "6c7b8b"],
      ["slategrey", "708090"],
      ["snow", "fffafa"],
      ["snow1", "fffafa"],
      ["snow2", "eee9e9"],
      ["snow3", "cdc9c9"],
      ["snow4", "8b8989"],
      ["springgreen", "00ff7f"],
      ["springgreen1", "00ff7f"],
      ["springgreen2", "00ee76"],
      ["springgreen3", "00cd66"],
      ["springgreen4", "008b45"],
      ["steelblue", "4682b4"],
      ["steelblue1", "63b8ff"],
      ["steelblue2", "5cacee"],
      ["steelblue3", "4f94cd"],
      ["steelblue4", "36648b"],
      ["tan", "d2b48c"],
      ["tan1", "ffa54f"],
      ["tan2", "ee9a49"],
      ["tan3", "cd853f"],
      ["tan4", "8b5a2b"],
      ["thistle", "d8bfd8"],
      ["thistle1", "ffe1ff"],
      ["thistle2", "eed2ee"],
      ["thistle3", "cdb5cd"],
      ["thistle4", "8b7b8b"],
      ["tomato", "ff6347"],
      ["tomato1", "ff6347"],
      ["tomato2", "ee5c42"],
      ["tomato3", "cd4f39"],
      ["tomato4", "8b3626"],
      ["turquoise", "40e0d0"],
      ["turquoise1", "00f5ff"],
      ["turquoise2", "00e5ee"],
      ["turquoise3", "00c5cd"],
      ["turquoise4", "00868b"],
      ["violet", "ee82ee"],
      ["violetred", "d02090"],
      ["violetred1", "ff3e96"],
      ["violetred2", "ee3a8c"],
      ["violetred3", "cd3278"],
      ["violetred4", "8b2252"],
      ["wheat", "f5deb3"],
      ["wheat1", "ffe7ba"],
      ["wheat2", "eed8ae"],
      ["wheat3", "cdba96"],
      ["wheat4", "8b7e66"],
      ["whitesmoke", "f5f5f5"],
      ["yellow", "ffff00"],
      ["yellow1", "ffff00"],
      ["yellow2", "eeee00"],
      ["yellow3", "cdcd00"],
      ["yellow4", "8b8b00"],
      ["yellowgreen", "9acd32"]
    ]);

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
          if (!$colorPreview) 
            return this.token;

          var quote = value.substring(0,1);
          var col = value.substring(2, value.length - 1);
          return colorTokens(quote, "#" + col, col);
        }
      },
      {
        token : "string", // hex color #rgb
        regex : '(["\'])(#[0-9a-fA-F]{3})(\\1)',
        next  : "start", 
        onMatch: function(value, state, stack, line) {
          if (!$colorPreview) 
            return this.token;
          
          var quote = value.substring(0, 1);
          var col = value.substring(2, value.length - 1); 
          return colorTokens(quote, "#" + col, col.replace(/./g, "$&$&"));
        }
      },
      {
        // strings that *might* be R named colors
        // - first check that they are lower-case letters maybe followed by numbers
        // - then in that case test against the builtInColors map
        token : "string",
        regex : '(["\'])([a-z]+[0-9]*)(\\1)', 
        next  : "start", 
        onMatch: function(value, state, stack, line) {
          if (!$colorPreview) 
            return this.token;
          
          var quote = value.substring(0, 1);
          var content = value.substring(1, value.length - 1);
          var rgb = builtInColors.get(content);
          if (rgb === undefined)
            return this.token;
          else
            return colorTokens(quote, content, rgb);
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
  exports.setColorPreview = function(value) {
    $colorPreview = value;
  }
});
