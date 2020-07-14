/*
 * rainbow_paren_highlight_rules.js
 *
 * Copyright (C) 2020 by RStudio, PBC
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

var $rainbowParentheses = false;
var $numParenColors = 7;

define("mode/rainbow_paren_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

  var RainbowParenHighlightRules = function() {
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
      token : "paren.keyword.operator.nomatch",
      regex : "[[({})\\]]",
      merge : false,
      onMatch: function(val, state, stack) {

      if (!$rainbowParentheses) {
        this.token = "paren.keyword.operator.nomatch";
        return this.token;
      }

      if (stack.length !== 2) {
        stack.length = 2;
        stack[0] = state;
        stack[1] = 0;
      }

      switch(val) {
        case "[":
        case "{":
        case "(":
          this.token = "paren.paren_color_" + (stack[1] % $numParenColors);
          stack[1] = stack[1] + 1;
          break;
        default:
          if (stack.length > 1 && stack[1] > 0) {
            stack[1] = stack[1] - 1;
            this.token = "paren.paren_color_" + (stack[1] % $numParenColors);
          }
      }

      return this.token;
    },
      next: "start"
    }
  };
});
