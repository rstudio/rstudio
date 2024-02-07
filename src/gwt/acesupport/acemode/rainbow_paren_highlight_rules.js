/*
 * rainbow_paren_highlight_rules.js
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

var $rainbowParentheses = false;
var $numParenColors = 7;

define("mode/rainbow_paren_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

  var RainbowParenHighlightRules = function () {
  };

  exports.RainbowParenHighlightRules = RainbowParenHighlightRules;
  exports.setRainbowParentheses = function(value) {
    $rainbowParentheses = value;
  };
  exports.getRainbowParentheses = function() {
    return $rainbowParentheses;
  };
  exports.setNumParenColors = function(value) {
    $numParenColors = value;
  };

  RainbowParenHighlightRules.getParenRule = function() {
    return {
      token: "paren.keyword.operator.nomatch",
      regex: "[[({})\\]]",
      merge: false,
      onMatch: function (value, state, stack, line, context) {

        if (!$rainbowParentheses) {
          this.token = "paren.keyword.operator.nomatch";
          return this.token;
        }

        context.rainbow = context.rainbow || 0;

        switch (value) {

          case "[": case "{": case "(":
            this.token = `paren.paren_color_${context.rainbow % $numParenColors}`;
            context.rainbow += 1;
            break;

          case "]": case "}": case ")":
            context.rainbow = Math.max(0, context.rainbow - 1);
            this.token = `paren.paren_color_${context.rainbow % $numParenColors}`;
            break;
        }

        return this.token;
      }
    };
  }

});
