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

        // NOTE: The 'stack' object here (which is really just an array) is
        // shared by different highlight rules, and is a general way to set and
        // persist state in the tokenizer. However, because it's visible to
        // each matching highlight rule, and each individual highlight rule
        // might want to manipulate that stack, we need to choose the index
        // carefully to avoid stomping on state declared from other highlight
        // rules. 15 is chosen as a "large enough" number to avoid stepping
        // other rules, which normally would be manipulating the first few
        // slots of the 'stack' array.
        //
        // https://github.com/rstudio/rstudio/issues/11087
        stack = stack || [];
        stack[0] = state;
        stack[15] = stack[15] || 0;

        switch(val) {

        case "[": case "{": case "(":
          this.token = "paren.paren_color_" + (stack[15] % $numParenColors);
          stack[15] = stack[15] + 1;
          break;
        case "]": case "}": case ")":
          stack[15] = Math.max(0, stack[15] - 1);
          this.token = "paren.paren_color_" + (stack[15] % $numParenColors);
          break;
        }

      return this.token;
    },
      next: "start"
    }
  };
});
