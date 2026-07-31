/*
 * multi_select_guard.js
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

// Exception-safety guards for Ace's multi-select support.
//
// Ace's Editor.forEachSelection() and Editor.$moveLines() temporarily mutate
// selection state -- swapping in a temporary Selection object, silencing the
// selection's event registry, setting 'inVirtualSelectionMode', detaching the
// range list -- and restore it only on the way out, with no exception
// protection. If a document or selection listener throws mid-operation, that
// state is left permanently corrupted: mouse selection stops working, typed
// input is silently discarded, and multi-select mode can never be exited.
// Only reloading the IDE recovers. See:
//
//   https://github.com/rstudio/rstudio/issues/13605
//
// The wrappers below snapshot the at-risk state before delegating to Ace,
// and restore anything the wrapped implementation failed to clean up. They
// intentionally know nothing about what the wrapped functions do in between,
// so they should remain valid across Ace updates. Exceptions still propagate;
// only the state cleanup is made unconditional.

define("mixins/multi_select_guard", ["require", "exports", "module"], function(require, exports, module) {

var Editor = require("ace/editor").Editor;

// Ensure ace/multi_select has augmented Editor.prototype before we wrap.
require("ace/multi_select");

(function() {

   var forEachSelection = this.forEachSelection;
   this.forEachSelection = function(cmd, args, options) {

      var session = this.session;
      var selection = this.selection;
      var eventRegistry = selection ? selection._eventRegistry : null;
      var virtualMode = this.inVirtualSelectionMode;

      try
      {
         return forEachSelection.call(this, cmd, args, options);
      }
      finally
      {
         // If an exception interrupted the iteration, the temporary selection
         // used for virtual execution is still installed; detach it and
         // restore the real selection.
         if (session && selection && session.selection !== selection)
         {
            var tmpSel = session.selection;
            if (tmpSel && typeof tmpSel.detach === "function")
            {
               try { tmpSel.detach(); } catch (e) { }
            }
            this.selection = session.selection = selection;
         }

         // Restore the selection's silenced event registry.
         if (selection && eventRegistry && selection._eventRegistry !== eventRegistry)
            selection._eventRegistry = eventRegistry;

         this.inVirtualSelectionMode = virtualMode;
      }
   };

   var $moveLines = this.$moveLines;
   this.$moveLines = function(dir, copy) {

      var session = this.session;
      var selection = this.selection;
      var virtualMode = this.inVirtualSelectionMode;

      try
      {
         return $moveLines.call(this, dir, copy);
      }
      finally
      {
         this.inVirtualSelectionMode = virtualMode;

         // The multi-select branch detaches the range list while moving
         // lines; re-attach it if an exception skipped that step.
         if (session && selection &&
             selection.inMultiSelectMode &&
             selection.rangeList &&
             selection.rangeList.session == null)
         {
            selection.rangeList.attach(session);
         }
      }
   };

}).call(Editor.prototype);

});
