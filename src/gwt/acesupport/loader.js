/*
 * loader.js
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

if (!String.prototype.trimRight) {
   var trimEndRegexp = /\s\s*$/;
   String.prototype.trimRight = function () {
      return String(this).replace(trimEndRegexp, '');
   };
}

define("rstudio/loader", ["require", "exports", "module"], function(require, exports, module) {

var Anchor = require("ace/anchor").Anchor;
var Editor = require("ace/editor").Editor;
var EditSession = require("ace/edit_session").EditSession;
var event = require("ace/lib/event");
var EventEmitter = require("ace/lib/event_emitter").EventEmitter;
var ExpandSelection = require("util/expand_selection");
var oop = require("ace/lib/oop");
var Range = require("ace/range").Range;
var Renderer = require("ace/virtual_renderer").VirtualRenderer;
var TextMode = require("ace/mode/text").Mode;
var UndoManager = require("ace/undomanager").UndoManager;
var Utils = require("mode/utils");

require("mixins/token_iterator"); // adds mixins to TokenIterator.prototype



// RStudioEditor ----

var RStudioEditor = function(renderer, session) {
   session.renderer = renderer;
   Editor.call(this, renderer, session);
   this.setBehavioursEnabled(true);
};
oop.inherits(RStudioEditor, Editor);

(function() {

   this.$highlightBrackets = function() {

      // don't highlight if we have a selection (avoid a situation
      // where the highlighted bracket could appear to be part of
      // the user's current selection)
      if (!this.session.selection.isEmpty()) {
         var session = this.session;
         if (session.$bracketHighlight) {
            session.$bracketHighlight.markerIds.forEach(function(id) {
               session.removeMarker(id);
            });
            session.$bracketHighlight = null;
         }
         return;
      }

      // delegate to base
      Editor.prototype.$highlightBrackets.call(this);
   };

   // Custom insert to handle enclosing of selection
   this.insert = function(text, pasted)
   {
      if (!this.session.selection.isEmpty())
      {
         // Read UI pref to determine what are eligible for surrounding
         var candidates = [];
         if (this.$surroundSelection === "quotes")
            candidates = ["'", "\"", "`"];
         else if (this.$surroundSelection === "quotes_and_brackets")
            candidates = ["'", "\"", "`", "(", "{", "["];

         // in markdown documents, allow '_', '*' to surround selection
         do
         {
            // assume this preference is only wanted when surrounding
            // other objects in general for now
            if (this.$surroundSelection !== "quotes_and_brackets")
               break;

            var mode = this.session.$mode;
            if (/\/markdown$/.test(mode.$id))
            {
               candidates.push("*", "_");
               break;
            }

            var position = this.getCursorPosition();
            if (mode.getLanguageMode && mode.getLanguageMode(position) === "Markdown")
            {
               candidates.push("*", "_");
               break;
            }
         } while (false);

         if (Utils.contains(candidates, text))
         {
            var lhs = text;
            var rhs = Utils.getComplement(text);
            return this.session.replace(
               this.session.selection.getRange(),
               lhs + this.session.getTextRange() + rhs
            );
         }
      }

      // Delegate to default insert implementation otherwise
      return Editor.prototype.insert.call(this, text, pasted);
   };

   this.remove = function(dir) {
      if (this.session.getMode().wrapRemove) {
         return this.session.getMode().wrapRemove(this, Editor.prototype.remove, dir);
      }
      else {
         return Editor.prototype.remove.call(this, dir);
      }
   };

   this.undo = function() {
      Editor.prototype.undo.call(this);
      this._dispatchEvent("undo");
   };

   this.redo = function() {
      Editor.prototype.redo.call(this);
      this._dispatchEvent("redo");
   };

   this.onPaste = function(text, event) {
      Editor.prototype.onPaste.call(this, text.replace(/\r\n|\n\r|\r/g, "\n"), event);
   };
}).call(RStudioEditor.prototype);



// RStudioEditSession ----

var RStudioEditSession = function(text, mode) {
   EditSession.call(this, text, mode);

   // Initialize instance-specific synthetic token storage
   // (must be per-instance, not on prototype, so sessions don't share the array)
   this.$syntheticTokens = [];

   var self = this;

   // Remove synthetic tokens that intersect the edit range
   this.on("change", function(delta) {
      if (!self.$syntheticTokens.length)
         return;

      var remaining = [];
      var rowsToInvalidate = [];

      for (var i = 0; i < self.$syntheticTokens.length; i++) {
         var token = self.$syntheticTokens[i];
         var row = token.anchor.row;
         var col = token.anchor.column;

         // Check if this token is affected by the edit.
         // For inserts, we check col + 1 because after consuming a prefix, the anchor
         // is positioned at the last character of the inserted text, but the next
         // character will be inserted one position to the right.
         var atEditPosition = (row === delta.start.row &&
            (col === delta.start.column ||
             (delta.action === "insert" && col + 1 === delta.start.column)));

         if (!atEditPosition) {
            // Token not at edit position - keep it
            remaining.push(token);
            continue;
         }

         if (delta.action === "insert") {
            // Only do prefix matching for ghost_text tokens (inline completions).
            // Other token types (like insertion_preview for NES) should just dismiss.
            if (token.type === "ghost_text") {
               // Get inserted text (only handle single-line inserts for prefix matching)
               var insertedText = delta.lines.length === 1 ? delta.lines[0] : null;

               // Check if synthetic token text starts with inserted text
               if (insertedText && token.text.indexOf(insertedText) === 0) {
                  // Consume the prefix - update token text
                  token.text = token.text.substring(insertedText.length);

                  // Position anchor at the end of the inserted text.
                  // The -1 is needed because the synthetic token injection places the token
                  // after the character at the anchor column, so we want the anchor at the
                  // last character of the inserted text.
                  var newColumn = delta.start.column + insertedText.length - 1;
                  token.anchor.setPosition(delta.start.row, newColumn);

                  // Invalidate row to re-render with updated token
                  rowsToInvalidate.push(delta.start.row);

                  // If token text is now empty, remove it; otherwise keep it
                  if (token.text.length === 0) {
                     token.anchor.detach();
                  } else {
                     remaining.push(token);
                  }
               } else {
                  // Inserted text doesn't match prefix (or multi-line insert) - dismiss
                  rowsToInvalidate.push(row);
                  token.anchor.detach();
               }
            } else {
               // Non-ghost_text token (e.g., insertion_preview) - dismiss on any insert
               rowsToInvalidate.push(row);
               token.anchor.detach();
            }
         } else if (delta.action === "remove") {
            // Delete at token position - dismiss
            rowsToInvalidate.push(row);
            token.anchor.detach();
         } else {
            // Unknown action - keep token
            remaining.push(token);
         }
      }

      if (remaining.length !== self.$syntheticTokens.length || rowsToInvalidate.length > 0) {
         self.$syntheticTokens = remaining;

         // Invalidate affected rows in background tokenizer
         var bgTokenizer = self.bgTokenizer;
         for (var j = 0; j < rowsToInvalidate.length; j++) {
            var row = rowsToInvalidate[j];
            bgTokenizer.lines[row] = null;
         }
      }
   });

   // Hook into the background tokenizer to inject synthetic tokens after tokenization
   var bgTokenizer = this.bgTokenizer;
   var $tokenizeRow = bgTokenizer.$tokenizeRow;

   bgTokenizer.$tokenizeRow = function(row) {
      var tokens = $tokenizeRow.call(this, row);

      // Inject any synthetic tokens for this row (skip lookup if none registered)
      if (self.$syntheticTokens.length) {
         var synthetics = self.$getSyntheticsForRow(row);
         if (synthetics.length) {
            tokens = self.$injectSyntheticTokens(tokens, synthetics);
            // Update the cache so getTokens() returns consistent results
            this.lines[row] = tokens;
         }
      }

      return tokens;
   };
};
oop.inherits(RStudioEditSession, EditSession);

(function() {

   this.insert = function(position, text) {
      if (this.getMode().wrapInsert) {
         return this.getMode().wrapInsert(this, EditSession.prototype.insert, position, text);
      }
      else {
         return EditSession.prototype.insert.call(this, position, text);
      }
   };

   this.addSyntheticToken = function(row, column, text, type) {
      var anchor = new Anchor(this.getDocument(), row, column);
      this.$syntheticTokens.push({ anchor: anchor, text: text, type: type });
   };

   this.removeSyntheticTokensForRow = function(row) {
      var remaining = [];
      for (var i = 0; i < this.$syntheticTokens.length; i++) {
         var token = this.$syntheticTokens[i];
         if (token.anchor.row === row) {
            token.anchor.detach();
         } else {
            remaining.push(token);
         }
      }
      this.$syntheticTokens = remaining;
   };

   this.clearSyntheticTokens = function() {
      for (var i = 0; i < this.$syntheticTokens.length; i++) {
         this.$syntheticTokens[i].anchor.detach();
      }
      this.$syntheticTokens = [];
   };

   // Helper to get synthetic tokens for a specific row
   this.$getSyntheticsForRow = function(row) {
      var synthetics = [];
      for (var i = 0; i < this.$syntheticTokens.length; i++) {
         var t = this.$syntheticTokens[i];
         if (t.anchor.row === row) {
            synthetics.push({ column: t.anchor.column, text: t.text, type: t.type });
         }
      }
      return synthetics;
   };

   // Helper to inject synthetic tokens into a token array
   this.$injectSyntheticTokens = function(tokens, synthetics) {
      if (!synthetics.length)
         return tokens;

      // Sort synthetics by column (descending) so we can inject from right to left
      // without messing up column positions
      synthetics = synthetics.slice().sort(function(a, b) { return b.column - a.column; });

      // Clone the tokens array so we don't mutate the original
      tokens = tokens.slice();

      for (var s = 0; s < synthetics.length; s++) {
         var synth = synthetics[s];
         var targetColumn = synth.column;
         var newToken = { value: synth.text, type: synth.type, synthetic: true };

         // Find where to inject
         var currentCol = 0;
         var injected = false;

         for (var i = 0; i < tokens.length; i++) {
            var token = tokens[i];
            var tokenEnd = currentCol + token.value.length;

            if (targetColumn <= tokenEnd) {
               // Injection point is within or at end of this token
               if (targetColumn === currentCol) {
                  // Insert before this token
                  tokens.splice(i, 0, newToken);
               } else if (targetColumn === tokenEnd) {
                  // Insert after this token
                  tokens.splice(i + 1, 0, newToken);
               } else {
                  // Split this token
                  var offset = targetColumn - currentCol;
                  var before = { value: token.value.substring(0, offset), type: token.type };
                  var after = { value: token.value.substring(offset), type: token.type };
                  tokens.splice(i, 1, before, newToken, after);
               }
               injected = true;
               break;
            }

            currentCol = tokenEnd;
         }

         if (!injected) {
            // Append at end
            tokens.push(newToken);
         }
      }

      return tokens;
   };

   // Override documentToScreenPosition to account for synthetic tokens
   this.documentToScreenPosition = function(docRow, docColumn) {
      var result = EditSession.prototype.documentToScreenPosition.call(this, docRow, docColumn);

      // Skip if no synthetic tokens registered
      if (!this.$syntheticTokens.length)
         return result;

      // Get the row from the input
      var row = typeof docRow === 'object' ? docRow.row : docRow;
      var col = typeof docRow === 'object' ? docRow.column : docColumn;

      // Get tokens for this row
      var tokens = this.getTokens(row);
      if (!tokens || !tokens.length)
         return result;

      // Calculate synthetic token width before the document column
      var syntheticWidth = 0;
      var currentDocCol = 0;

      for (var i = 0; i < tokens.length; i++) {
         var token = tokens[i];

         if (token.synthetic) {
            // Synthetic tokens appear at the current document position
            // Only count if cursor is AFTER the synthetic token position (not at it)
            if (currentDocCol < col) {
               syntheticWidth += token.value.length;
            }
         } else {
            // Real token - advance document column
            currentDocCol += token.value.length;
         }
      }

      if (syntheticWidth > 0) {
         result.column += syntheticWidth;
      }

      return result;
   };

   // Override screenToDocumentPosition to account for synthetic tokens
   this.screenToDocumentPosition = function(screenRow, screenColumn, offsetX) {
      // Get the document row first using original implementation
      var result = EditSession.prototype.screenToDocumentPosition.call(this, screenRow, screenColumn, offsetX);

      // Skip if no synthetic tokens registered
      if (!this.$syntheticTokens.length)
         return result;

      var docRow = result.row;

      // Get tokens for this row
      var tokens = this.getTokens(docRow);
      if (!tokens || !tokens.length)
         return result;

      // Walk through tokens, tracking both screen and document positions
      var screenCol = 0;
      var docCol = 0;

      for (var i = 0; i < tokens.length; i++) {
         var token = tokens[i];
         var tokenScreenEnd = screenCol + token.value.length;

         if (screenColumn < tokenScreenEnd) {
            // Click is within this token
            if (token.synthetic) {
               // Click is in a synthetic token - clamp to start of synthetic token (document position)
               result.column = docCol;
            } else {
               // Click is in a real token - compute offset within token
               var offsetInToken = screenColumn - screenCol;
               result.column = docCol + offsetInToken;
            }
            return result;
         }

         screenCol = tokenScreenEnd;
         if (!token.synthetic) {
            docCol += token.value.length;
         }
      }

      // Click is past all tokens - return document line end
      result.column = docCol;
      return result;
   };

   this.reindent = function(range) {

      var mode = this.getMode();
      if (!mode.getNextLineIndent)
         return;

      var start = range.start.row;
      var end = range.end.row;

      // First line is always unindented
      if (start === 0) {
         this.applyIndent(0, "");
         start++;
      }

      for (var i = start; i <= end; i++)
      {
         var state = Utils.getPrimaryState(this, i - 1);
         if (Utils.endsWith(state, "qstring") || state === "rawstring")
            continue;

         var newIndent = mode.getNextLineIndent(state,
                                                this.getLine(i - 1),
                                                this.getTabString(),
                                                i - 1,
                                                true);

         this.applyIndent(i, newIndent);
         mode.autoOutdent(state, this, i);
      }

      // optional outdenting (currently hard-wired for C++ modes)
      var codeModel = mode.codeModel;
      if (typeof codeModel !== "undefined") {
         var align = codeModel.alignContinuationSlashes;
         if (typeof align !== "undefined") {
            align(this.getDocument(), {
               start: start,
               end: end
            });
         }
      }


   };
   this.applyIndent = function(lineNum, indent) {
      var line = this.getLine(lineNum);
      var matchLen = line.match(/^\s*/g)[0].length;
      this.replace(new Range(lineNum, 0, lineNum, matchLen), indent);
   };

   this.setDisableOverwrite = function(disableOverwrite) {

      // Note that 'this' refers to the instance, not the prototype. It's
      // important that we override set/getOverwrite on a per-instance basis
      // only.

      if (disableOverwrite) {
         // jcheng 08/21/2012: The old way we did this (see git history) caused
         // a weird bug: the console would pick up the overwrite/insert mode of
         // the active source document iff vim mode was enabled. I could not
         // figure out why.

         // In case we are already in overwrite mode; set it to false so events
         // will be fired.
         this.setOverwrite(false);

         this.setOverwrite = function() { /* no-op */ };
         this.getOverwrite = function() { return false; };
      }
      else {
         // Restore the standard methods
         this.setOverwrite = EditSession.prototype.setOverwrite;
         this.getOverwrite = EditSession.prototype.getOverwrite;
      }
   };
}).call(RStudioEditSession.prototype);



// RStudioUndoManager ----

var RStudioUndoManager = function() {
   UndoManager.call(this);
};
oop.inherits(RStudioUndoManager, UndoManager);

(function() {
   this.peek = function() {
      return this.$undoStack.length ? this.$undoStack[this.$undoStack.length-1]
                                    : null;
   };
}).call(RStudioUndoManager.prototype);



// RStudioRenderer ----

var RStudioRenderer = function(container, theme) {
   Renderer.call(this, container, theme);
};
oop.inherits(RStudioRenderer, Renderer);

(function() {

   this.setTheme = function(theme) {

      if (theme)
         Renderer.prototype.setTheme.call(this, theme);

   }

}).call(RStudioRenderer.prototype);



function loadEditor(container) {
   var env = {};
   container.env = env;

   // Load the editor
   var renderer = new RStudioRenderer(container, "");
   var session = new RStudioEditSession("");
   var editor = new RStudioEditor(renderer, session);
   env.editor = editor;

   var session = editor.getSession();
   session.setMode(new TextMode());
   session.setUndoManager(new RStudioUndoManager());

   // Setup syntax checking
   var config = require("ace/config");
   config.set("basePath", "ace");
   config.set("workerPath", "js/workers");
   config.setDefaultValue("session", "useWorker", false);

   // We handle these commands ourselves.
   function squelch(cmd) {
      env.editor.commands.removeCommand(cmd);
   }
   squelch("findnext");
   squelch("findprevious");
   squelch("find");
   squelch("replace");
   squelch("togglecomment");
   squelch("gotoline");
   squelch("foldall");
   squelch("unfoldall");
   return env.editor;
}

exports.RStudioEditor = RStudioEditor;
exports.loadEditor = loadEditor;
});
