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
// Two of Ace's multi-select operations temporarily mutate selection state
// and restore it only on the way out, with no exception protection:
//
// - Editor.forEachSelection() swaps a temporary Selection object in as the
//   session's selection, silences the real selection's event registry, and
//   sets 'inVirtualSelectionMode';
// - Editor.$moveLines() detaches the selection's range list and sets
//   'inVirtualSelectionMode'.
//
// If a document or selection listener throws mid-operation, that state is
// left permanently corrupted: mouse selection stops working, typed input is
// silently discarded, and multi-select mode can never be exited. Only
// reloading the IDE recovers. See:
//
//   https://github.com/rstudio/rstudio/issues/13605
//
// The wrappers below snapshot the at-risk state before delegating to Ace,
// and restore anything the wrapped implementation failed to clean up.
// Exceptions still propagate; only the state cleanup is made unconditional.
// The wrappers don't care what the wrapped functions compute, but they do
// depend on a small set of Ace internals -- the 'session.selection'
// reassignment, 'Selection.detach()', the private '_eventRegistry' field,
// and the private 'rangeList.session' field -- which should be re-verified
// when updating Ace.

define("mixins/multi_select_guard", ["require", "exports", "module"], function(require, exports, module) {

var Editor = require("ace/editor").Editor;

// Ensure ace/multi_select has augmented Editor.prototype before we wrap.
require("ace/multi_select");

// Log through the IDE's debug bridge when available (absent in the
// standalone test pages). The wrappers only observe stranded state at the
// moment it is repaired, so these messages are the one record of which
// state an aborted operation left behind; the propagating exception alone
// doesn't say.
function log(message)
{
   if (typeof window !== "undefined" && window.$Debug)
      window.$Debug.log("multi_select_guard: " + message);
}

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
               // Best-effort: rethrowing here would mask the exception that
               // aborted the operation. A partial detach can leak a document
               // listener, so at least leave a record of it.
               try { tmpSel.detach(); } catch (e) { log("forEachSelection: error detaching temporary selection: " + e); }
            }
            this.selection = session.selection = selection;
            log("forEachSelection: restored selection after aborted iteration");
         }

         // Restore the selection's silenced event registry.
         if (selection && eventRegistry && selection._eventRegistry !== eventRegistry)
         {
            selection._eventRegistry = eventRegistry;
            log("forEachSelection: restored silenced event registry");
         }

         if (!!this.inVirtualSelectionMode !== !!virtualMode)
         {
            this.inVirtualSelectionMode = virtualMode;
            log("forEachSelection: restored inVirtualSelectionMode");
         }
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
         if (!!this.inVirtualSelectionMode !== !!virtualMode)
         {
            this.inVirtualSelectionMode = virtualMode;
            log("$moveLines: restored inVirtualSelectionMode");
         }

         // The multi-select branch detaches the range list while moving
         // lines; re-attach it if an exception skipped that step.
         if (session && selection &&
             selection.inMultiSelectMode &&
             selection.rangeList &&
             selection.rangeList.session == null)
         {
            selection.rangeList.attach(session);
            log("$moveLines: re-attached range list after aborted move");
         }
      }
   };

}).call(Editor.prototype);

});
