/*
 * loader.js
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

      if (!String.prototype.trimRight) {
         var trimEndRegexp = /\s\s*$/;
         String.prototype.trimRight = function () {
            return String(this).replace(trimEndRegexp, '');
         };
      }

define("rstudio/loader", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var event = require("ace/lib/event");
var EventEmitter = require("ace/lib/event_emitter").EventEmitter;
var Editor = require("ace/editor").Editor;
var EditSession = require("ace/edit_session").EditSession;
var UndoManager = require("ace/undomanager").UndoManager;
var Range = require("ace/range").Range;
var Utils = require("mode/utils");
var ExpandSelection = require("util/expand_selection");

require("mixins/token_iterator"); // adds mixins to TokenIterator.prototype

var RStudioEditor = function(renderer, session) {
   Editor.call(this, renderer, session);
   this.setBehavioursEnabled(true);
};
oop.inherits(RStudioEditor, Editor);

(function() {

   this.$highlightBrackets = function() {
      // Clear an existing highlight
      if (this.session.$bracketHighlight) {
         this.session.removeMarker(this.session.$bracketHighlight);
         this.session.$bracketHighlight = null;
      }

      // don't highlight if we have a selection (avoid a situation
      // where the highlighted bracket could appear to be part of
      // the user's current selection)
      if (!this.session.selection.isEmpty())
         return;

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
            candidates = ["'", "\""];
         else if (this.$surroundSelection === "quotes_and_brackets")
            candidates = ["'", "\"", "(", "{", "["];

         // in markdown documents, allow '_', '*' to surround selection
         do
         {
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


var RStudioEditSession = function(text, mode) {
   EditSession.call(this, text, mode);
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
         if (Utils.endsWith(state, "qstring"))
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

function loadEditor(container) {
   var env = {};
   container.env = env;

   var Renderer = require("ace/virtual_renderer").VirtualRenderer;

   var TextMode = require("ace/mode/text").Mode;
   var theme = {}; // prevent default textmate theme from loading

   env.editor = new RStudioEditor(new Renderer(container, theme), new RStudioEditSession(""));
   var session = env.editor.getSession();
   session.setMode(new TextMode());
   session.setUndoManager(new RStudioUndoManager());

   // Setup syntax checking
   var config = require("ace/config");
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
   squelch("touppercase");
   squelch("tolowercase");
   return env.editor;
}

exports.loadEditor = loadEditor;
});
