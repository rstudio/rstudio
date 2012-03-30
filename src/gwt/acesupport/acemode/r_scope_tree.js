/*
 * r_scope_tree.js
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
define('mode/r_scope_tree', function(require, exports, module) {

   function debuglog(str) {
      //console.log(str);
   }

   function assert(condition, label) {
      if (!condition)
         window.alert("[ASSERTION FAILED] " + label);
   }

   function comparePoints(pos1, pos2) {
      if (pos1.row != pos2.row)
         return pos1.row - pos2.row;
      return pos1.column - pos2.column;
   }


   var ScopeManager = function() {
      this.parsePos = {row: 0, column: 0};
      this.$root = new ScopeNode("(Top Level)", this.parsePos, null,
                                 ScopeNode.TYPE_ROOT);
   };

   (function() {

      this.onSectionHead = function(sectionLabel, sectionPos) {
         var existingScopes = this.getActiveScopes(sectionPos);
         if (existingScopes.length == 2 && existingScopes[1].isSection()) {
            this.$root.closeScope(sectionPos, ScopeNode.TYPE_SECTION);
         }
         else if (existingScopes.length != 1)
            return;

         this.$root.addNode(new ScopeNode(sectionLabel, sectionPos, sectionPos,
                                          ScopeNode.TYPE_SECTION));
      };

      this.onChunkStart = function(chunkLabel, label, chunkStartPos, chunkPos) {
         // Starting a chunk means closing the previous chunk, if any
         var prev = this.$root.closeScope(chunkStartPos, ScopeNode.TYPE_CHUNK);
         if (prev)
            debuglog("chunk-scope implicit end: " + prev.label);

         debuglog("adding chunk-scope " + label);
         var node = new ScopeNode(label, chunkPos, chunkStartPos,
                                  ScopeNode.TYPE_CHUNK);
         node.chunkLabel = chunkLabel;
         this.$root.addNode(node);
         this.printScopeTree();
      };

      this.onChunkEnd = function(pos) {
         var closed = this.$root.closeScope(pos, ScopeNode.TYPE_CHUNK);
         if (closed)
            debuglog("chunk-scope end: " + closed.label);
         else
            debuglog("extra chunk-scope end");
         this.printScopeTree();
         return closed;
      };

      this.onFunctionScopeStart = function(label, functionStartPos, scopePos) {
         debuglog("adding function brace-scope " + label);
         this.$root.addNode(new ScopeNode(label, scopePos, functionStartPos,
                                          ScopeNode.TYPE_BRACE));
         this.printScopeTree();
      };

      this.onScopeStart = function(pos) {
         debuglog("adding anon brace-scope");
         this.$root.addNode(new ScopeNode(null, pos, null,
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

      this.getActiveScopes = function(pos) {
         return this.$root.findNode(pos);
      };

      this.getScopeList = function() {
         return this.$root.$children;
      };

      this.findFunctionDefinitionFromUsage = function(usagePos, functionName) {
         return this.$root.findFunctionDefinitionFromUsage(usagePos,
                                                           functionName);
      };

      this.invalidateFrom = function(pos) {
         pos = {row: Math.max(0, pos.row-1), column: 0};
         debuglog("Invalidate from " + pos.row + ", " + pos.column);
         if (comparePoints(this.parsePos, pos) > 0)
            this.parsePos = this.$root.invalidateFrom(pos);
         this.printScopeTree();
      };

      this.getTopLevelScopeCount = function() {
         return this.$root.$children.length;
      };

      this.printScopeTree = function() {
         this.$root.printDebug();
      };

   }).call(ScopeManager.prototype);



   var ScopeNode = function(label, start, preamble, scopeType) {
      this.label = label;

      // The position of the open brace
      this.start = start;

      // The position of the start of the function declaration (possibly
      // with added whitespace)
      this.preamble = preamble || start;

      // The position of the close brance (possibly with added whitespace)
      this.end = null;

      // Whether this scope is
      this.scopeType = scopeType;

      this.$children = [];
   };

   ScopeNode.TYPE_ROOT = 1; // document root
   ScopeNode.TYPE_BRACE = 2; // curly brace
   ScopeNode.TYPE_CHUNK = 3; // Sweave chunk
   ScopeNode.TYPE_SECTION = 4; // Section header

   (function() {

      this.isRoot = function() { return this.scopeType == ScopeNode.TYPE_ROOT; };
      this.isBrace = function() { return this.scopeType == ScopeNode.TYPE_BRACE; };
      this.isChunk = function() { return this.scopeType == ScopeNode.TYPE_CHUNK; };
      this.isSection = function() { return this.scopeType == ScopeNode.TYPE_SECTION; };

      this.addNode = function(node) {
         assert(!node.end, "New node is already closed");
         assert(node.$children.length == 0, "New node already had children");

         // It's possible for this node to be already closed. If that's the
         // case, we need to open it back up. Example:
         //
         // foo <- function() { bar <- function [HERE] }) {
         //
         // If [HERE] is replaced with (, then the final brace will cause this
         // situation to be triggered because the previous brace belonged to
         // foo but no longer does.
         this.end = null;

         var index = this.$binarySearch(node.preamble);
         if (index >= 0) {
            // This node belongs inside an existing child
            this.$children[index].addNode(node);
         }
         else {
            // This node belongs directly under this scope. It's possible that
            // it subsumes some existing children under this scope. (We may not
            // know about a function scope until after we've seen some of its
            // children, since function scopes don't get created until we see
            // their opening brace but any argument defaults that are themselves
            // functions will have been seen already.)

            index = -(index+1);

            if (index < this.$children.length) {
               node.$children = this.$children.splice(
                                             index, this.$children.length - index);
            }

            this.$children.push(node);
         }
      };

      this.closeScope = function(pos, scopeType) {

         // NB: This function will never close the "this" node. This is by
         // design as we don't want the top-level node to ever be closed.

         // No children
         if (this.$children.length == 0)
            return null;

         var lastNode = this.$children[this.$children.length-1]

         // Last child is already closed
         if (lastNode.end)
            return null;

         // Last child had a descendant that needed to be closed and was the
         // appropriate type
         var closedChild = lastNode.closeScope(pos, scopeType);
         if (closedChild)
            return closedChild;

         // Close last child, if it's of the type we want to close
         if (scopeType == lastNode.scopeType) {
            lastNode.end = pos;
            // If any descendants are still open, force them closed. This could
            // be the case for e.g. Sweave chunk being closed while it contains
            // unclosed brace scopes.
            lastNode.$forceDescendantsClosed(pos);
            return lastNode;
         }

         return null;
      };

      this.$forceDescendantsClosed = function(pos) {
         if (this.$children.length == 0)
            return;
         var lastNode = this.$children[this.$children.length - 1];
         if (lastNode.end)
            return;
         lastNode.$forceDescendantsClosed(pos);
         lastNode.end = pos;
      }

      // Returns array of nodes that contain the position, from outermost to
      // innermost; or null if no nodes contain it.
      this.findNode = function(pos) {
         var index = this.$binarySearch(pos);
         if (index >= 0) {
            var result = this.$children[index].findNode(pos);
            if (result) {
               if (this.label)
                  result.unshift(this);
               return result;
            }
            if (this.label)
               return [this];
            return null;
         }
         else {
            return this.label ? [this] : null;
         }
      };

      this.$getFunctionStack = function(pos) {
         var index = this.$binarySearch(pos);
         var stack = index >= 0 ? this.$children[index].$getFunctionStack(pos)
                                : [];
         if (this.label) {
            stack.push(this);
         }
         return stack;
      };

      this.findFunctionDefinitionFromUsage = function(usagePos, functionName) {
         var functionStack = this.$getFunctionStack(usagePos);
         for (var i = 0; i < functionStack.length; i++) {
            var thisLevel = functionStack[i];
            for (var j = 0; j < thisLevel.$children.length; j++) {
               // optionally, short-circuit iteration if usagePos comes before
               // thisLevel.$children[j].preamble (or .start?)
               if (thisLevel.$children[j].label == functionName)
                  return thisLevel.$children[j];
            }
         }

         return null;
      };

      // Invalidates everything after pos, and possibly some stuff before.
      // Returns the position from which parsing should resume.
      this.invalidateFrom = function(pos) {

         var index = this.$binarySearch(pos);

         var resumePos;
         if (index >= 0)
         {
            // One of the child scopes contains this position (i.e. it's between
            // the preamble and end). Now figure out if the position is between
            // the child's start and end.

            if (comparePoints(pos, this.$children[index].start) <= 0)
            {
               // The position is between the child's preamble and the start.
               // We need to drop the child entirely and reparse.
               resumePos = this.$children[index].preamble;
            }
            else
            {
               // The position is between the child's start and end. We can keep
               // the scope, just recurse into the child to make sure its
               // children get invalidated correctly, and its 'end' property
               // is nulled out.
               resumePos = this.$children[index].invalidateFrom(pos);

               // Increment index so this child doesn't get removed.
               index++;
            }
         }
         else
         {
            index = -(index+1);
            resumePos = pos;
         }

         if (index < this.$children.length)
         {
            this.$children.splice(index, this.$children.length - index);
         }

         this.end = null;

         return resumePos;
      };

      this.exportFunctions = function(list)
      {
         if (this.label)
         {
            var here = {
               label: this.label,
               preamble: this.preamble,
               end: this.end,
               children: []
            };
            list.push(here);
            list = here.children;
         }

         for (var i = 0; i < this.$children.length; i++)
            this.$children[i].exportFunctions(list);
      };

      // Returns index of the child that contains this position, if it exists;
      // otherwise, -(index + 1) where index is where such a child would be.
      this.$binarySearch = function(pos, start /*optional*/, end /*optional*/) {
         if (typeof(start) === 'undefined')
            start = 0;
         if (typeof(end) === 'undefined')
            end = this.$children.length;

         // No elements left to test
         if (start === end)
            return -(start + 1);

         var mid = Math.floor((start + end)/2);
         var comp = this.$children[mid].comparePosition(pos);
         if (comp === 0)
            return mid;
         else if (comp < 0)
            return this.$binarySearch(pos, start, mid);
         else // comp > 0
            return this.$binarySearch(pos, mid + 1, end);
      };

      this.comparePosition = function(pos)
      {
         // TODO
         if (comparePoints(pos, this.preamble) < 0)
            return -1;
         if (this.end != null && comparePoints(pos, this.end) >= 0)
            return 1;
         return 0;
      };

      this.printDebug = function(indent) {
         if (typeof(indent) === 'undefined')
            indent = "";

         debuglog(indent + "\"" + this.label + "\" ["
                        + this.preamble.row + "x" + this.preamble.column
                  + (this.start ? ("-" + this.start.row + "x" + this.start.column) : "")
                        + ", "
                        + (this.end ? (this.end.row + "x" + this.end.column) : "null" ) + "]");
         for (var i = 0; i < this.$children.length; i++)
            this.$children[i].printDebug(indent + "    ");
      };

   }).call(ScopeNode.prototype);


   exports.ScopeManager = ScopeManager;

});