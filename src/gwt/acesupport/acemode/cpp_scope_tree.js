/*
 * cpp_scope_tree.js
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
define('mode/cpp_scope_tree', function(require, exports, module) {

function debuglog(str) {
   console.log(str);
}

var oop = require('ace/lib/oop');
var ScopeTree = require('mode/r_scope_tree');
var ScopeManager = ScopeTree.ScopeManager;
var ScopeNode = ScopeTree.ScopeNode;

var CppScopeNode = ScopeNode;
CppScopeNode.prototype = new ScopeNode();

CppScopeNode.TYPE_CLASS     = 1;
CppScopeNode.TYPE_NAMESPACE = 2;
CppScopeNode.TYPE_FUNCTION  = 3;
CppScopeNode.TYPE_LAMBDA    = 4;

(function() {

   this.isClass = function() {
      return this.scopeType == CppScopeNode.TYPE_BRACE &&
         this.scopeCategory == CppScopeNode.TYPE_CLASS;
   };
   
}).call(CppScopeNode.prototype);

var CppScopeManager = {};
oop.inherits(CppScopeManager, ScopeTree.ScopeManager);

(function() {

   this.onClassScopeStart = function(label, startPos, scopePos) {
      this.$root.addNode(
         new ScopeNode(label, scopePos, startPos, ScopeNode.TYPE_BRACE)
      );
      this.printScopeTree();
   };

   this.onNamespaceScopeStart = function(label, startPos, scopePos) {
      this.$root.addNode(
         new ScopeNode(label, scopePos, startPos, ScopeNode.TYPE_BRACE)
      );
      this.printScopeTree();
   };

   
}).call(CppScopeManager.prototype);

});
