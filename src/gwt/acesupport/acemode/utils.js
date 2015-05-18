/*
 * utils.js
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

define("mode/utils", function(require, exports, module) {

(function() {

   var that = this;

   // Simulate 'new Foo([args])'; ie, construction of an
   // object from an array of arguments
   this.construct = function(constructor, args)
   {
      function F() {
         return constructor.apply(this, args);
      }

      F.prototype = constructor.prototype;
      return new F();
   };

   this.contains = function(array, object)
   {
      for (var i = 0; i < array.length; i++)
         if (array[i] === object)
            return true;

      return false;
   };

   this.isArray = function(object)
   {
      return Object.prototype.toString.call(object) === '[object Array]';
   };

   this.getPrimaryState = function(session, row)
   {
      return that.primaryState(session.getState(row));
   };

   this.primaryState = function(states)
   {
      if (that.isArray(states))
         return states[0];
      return states;
   };

   this.activeMode = function(state, major)
   {
      var primary = that.primaryState(state);
      var modeIdx = primary.lastIndexOf("-");
      if (modeIdx === -1)
         return major;
      return primary.substring(0, modeIdx).toLowerCase();
   };

   this.endsWith = function(string, suffix)
   {
      return string.indexOf(suffix, string.length - suffix.length) !== -1;
   };

   this.escapeRegExp = function(string)
   {
      return string.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
   };

   this.embedRules = function(HighlightRules, EmbedRules,
                              prefix, reStart, reEnd)
   {
      var rules = HighlightRules.$rules;
      rules["start"].unshift({
         token: "support.function.codebegin",
         regex: reStart,
         next : prefix + "-start"
      });

      var embed = new EmbedRules().getRules();
      HighlightRules.addRules(embed, prefix + "-");
      
      rules[prefix + "-start"].unshift({
         token: "support.function.codeend",
         regex: reEnd,
         next : "start"
      });
   };
   
}).call(exports);

});
