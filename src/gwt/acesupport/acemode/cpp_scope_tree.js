/*
 * cpp_scope_tree.js
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
define('mode/cpp_scope_tree', ["require", "exports", "module"], function(require, exports, module) {

function debuglog(str) {
   // console.log(str);
}

var oop = require('ace/lib/oop');
var ScopeTree = require('mode/r_scope_tree');
var ScopeManager = ScopeTree.ScopeManager;
var ScopeNode = ScopeTree.ScopeNode;

var CppScopeNode = function(label, start, preamble, scopeType, scopeCategory, attributes) {
   this.label = label;
   this.start = start;
   this.preamble = preamble || start;
   this.end = null;
   this.scopeType = scopeType;
   this.scopeCategory = scopeCategory;
   this.attributes = attributes || {};
   this.parentScope = null;
   this.$children = [];
};
oop.mixin(CppScopeNode.prototype, ScopeNode.prototype);

CppScopeNode.CATEGORY_CLASS     = 1;
CppScopeNode.CATEGORY_NAMESPACE = 2;
CppScopeNode.CATEGORY_FUNCTION  = 3;
CppScopeNode.CATEGORY_LAMBDA    = 4;
CppScopeNode.CATEGORY_ANON      = 5;

(function() {

   this.isClass = function() {
      return this.scopeType == ScopeNode.TYPE_BRACE &&
         this.scopeCategory == CppScopeNode.CATEGORY_CLASS;
   };

   this.isNamespace = function() {
      return this.scopeType == ScopeNode.TYPE_BRACE &&
         this.scopeCategory == CppScopeNode.CATEGORY_NAMESPACE;
   };

   this.isFunction = function() {
      return this.scopeType == ScopeNode.TYPE_BRACE &&
         this.scopeCategory == CppScopeNode.CATEGORY_FUNCTION;
   };
   
   this.isLambda = function() {
      return this.scopeType == ScopeNode.TYPE_BRACE &&
         this.scopeCategory == CppScopeNode.CATEGORY_LAMBDA;
   };
   
   
}).call(CppScopeNode.prototype);

var CppScopeManager = function(ScopeNodeFactory) {

   this.$ScopeNodeFactory = ScopeNodeFactory;
   
   this.parsePos = {
      row: 0,
      column: 0
   };

   this.$root = new ScopeNodeFactory(
      "(Top Level)",
      this.parsePos,
      null,
      ScopeNode.TYPE_ROOT
   );
   
};
oop.mixin(CppScopeManager.prototype, ScopeManager.prototype);

(function() {

   this.onClassScopeStart = function(label, startPos, scopePos, name) {
      debuglog("adding class scope " + label);

      var node = new this.$ScopeNodeFactory(
         label,
         scopePos,
         startPos,
         ScopeNode.TYPE_BRACE,
         CppScopeNode.CATEGORY_CLASS,
         {name: name}
      );
      this.$root.addNode(node);

      this.printScopeTree();
   };

   this.onNamespaceScopeStart = function(label, startPos, scopePos, name) {
      debuglog("adding namespace scope " + label);

      var node = new this.$ScopeNodeFactory(
         label,
         scopePos,
         startPos,
         ScopeNode.TYPE_BRACE,
         CppScopeNode.CATEGORY_NAMESPACE,
         {name: name}
      );
      this.$root.addNode(node);

      this.printScopeTree();
   };

   this.onFunctionScopeStart = function(label, startPos, scopePos, name, args) {
      debuglog("adding function scope " + label);

      var node = new this.$ScopeNodeFactory(
         label,
         scopePos,
         startPos,
         ScopeNode.TYPE_BRACE,
         CppScopeNode.CATEGORY_FUNCTION,
         {name: name, args: args}
      );
      this.$root.addNode(node);

      this.printScopeTree();
   };

   this.onLambdaScopeStart = function(label, startPos, scopePos, args) {
      debuglog("adding lambda scope " + label);

      var node = new this.$ScopeNodeFactory(
         label,
         scopePos,
         startPos,
         ScopeNode.TYPE_BRACE,
         CppScopeNode.CATEGORY_LAMBDA,
         {args: args}
      );
      this.$root.addNode(node);

      this.printScopeTree();
   };

   this.onScopeStart = function(pos) {
      debuglog("adding anon brace-scope");
      this.$root.addNode(new this.$ScopeNodeFactory(null, pos, null,
                                          ScopeNode.TYPE_BRACE));
      this.printScopeTree();
   };

   this.onScopeEnd = function(pos) {
      var closed = this.$root.closeScope(pos, ScopeNode.TYPE_BRACE);
      if (closed)
         debuglog("brace-scope end: " + closed.label);
      else
         debuglog("extra brace-scope end");
      this.printScopeTree();
      return closed;
   };
   
}).call(CppScopeManager.prototype);

exports.CppScopeManager = CppScopeManager;
exports.CppScopeNode = CppScopeNode;

});
