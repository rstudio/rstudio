/*
 * AceEditorNative.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import java.util.LinkedList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.js.JsMap;
import org.rstudio.core.client.widget.CanSetControlId;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Command;

public class AceEditorNative extends JavaScriptObject
                             implements CanSetControlId
{
   
   protected AceEditorNative() {}

   public native final EditSession getSession() /*-{
      return this.getSession();
   }-*/;

   public native final Renderer getRenderer() /*-{
      return this.renderer;
   }-*/;
   
   public native final LineWidgetManager getLineWidgetManager() /*-{
      var session = this.getSession();
      if (!session.widgetManager) 
      {
         var LineWidgets = $wnd.require("ace/line_widgets").LineWidgets;
         session.widgetManager = new LineWidgets(session);
         session.widgetManager.attach(this);
      }
      return session.widgetManager;
   }-*/; 

   public native final void resize() /*-{
      this.resize();
   }-*/;
   
   public native final String getSelectedText() /*-{
      return this.getSelectedText();
   }-*/;
   
   public native final void setUseResizeObserver(boolean use) /*-{
      this.setOption("useResizeObserver", use);
   }-*/;

   public native final void setShowPrintMargin(boolean show) /*-{
      this.setShowPrintMargin(show);
   }-*/;

   public native final void setPrintMarginColumn(int column) /*-{
      this.setPrintMarginColumn(column);
   }-*/;

   public native final boolean getHighlightActiveLine() /*-{
      return this.getHighlightActiveLine();
   }-*/;
   
   public native final void setHighlightActiveLine(boolean highlight) /*-{
      this.setHighlightActiveLine(highlight);
   }-*/;

   public native final void setRelativeLineNumbers(boolean relative) /*-{
      this.setOption("relativeLineNumbers", relative);
   }-*/;

    public native final void setEnableKeyboardAccessibility(boolean keyboardAccessible) /*-{
      this.setOption("enableKeyboardAccessibility", keyboardAccessible);
   }-*/;

   public native final void setHighlightGutterLine(boolean highlight) /*-{
      this.setHighlightGutterLine(highlight);
   }-*/;
   
   public native final void setFixedWidthGutter(boolean value) /*-{
      this.renderer.setOption("fixedWidthGutter", value);
   }-*/;

   public native final void setHighlightSelectedWord(boolean highlight) /*-{
      this.setHighlightSelectedWord(highlight);
   }-*/;

   public native final boolean getReadOnly() /*-{
      return this.getReadOnly();
   }-*/;

   public native final void setReadOnly(boolean readOnly) /*-{
      this.setReadOnly(readOnly);
   }-*/;
   
   public native final void setCompletionOptions(boolean enabled,
                                                 boolean snippets,
                                                 boolean live,
                                                 int characterThreshold,
                                                 int delayMilliseconds) /*-{
      this.setOptions({
        enableBasicAutocompletion: enabled,
        enableSnippets: enabled && snippets,
        enableLiveAutocompletion: enabled && live,
        liveAutocompletionThreshold: characterThreshold,
        liveAutocompletionDelay: delayMilliseconds
      });
   }-*/;
   
   public native final void toggleCommentLines() /*-{
      this.toggleCommentLines();
   }-*/;

   public native final void focus() /*-{
      this.focus();
   }-*/;
   
   public native final boolean isFocused() /*-{
      return this.isFocused();
   }-*/;
   
   public native final boolean isRowFullyVisible(int row) /*-{
      return this.isRowFullyVisible(row);
   }-*/;

   public native final void blur() /*-{
      this.blur();
   }-*/;

   public native final void setKeyboardHandler(KeyboardHandler keyboardHandler) /*-{
      this.setKeyboardHandler(keyboardHandler);
   }-*/;
   
   public native final KeyboardHandler getKeyboardHandler() /*-{
      return this.getKeyboardHandler();
   }-*/;
   
   public native final void addKeyboardHandler(KeyboardHandler keyboardHandler) /*-{
      this.keyBinding.addKeyboardHandler(keyboardHandler);
   }-*/;
   
   public native final boolean isVimInInsertMode() /*-{
      return this.state.cm.state.vim.insertMode;
   }-*/;
   
   public native final void onChange(CommandWithArg<AceDocumentChangeEventNative> command) /*-{
      this.getSession().on("change",
        $entry(function (arg) {
            command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(arg);
        }));
   }-*/;

   public native final void onChangeFold(Command command) /*-{
      this.getSession().on("changeFold",
              $entry(function () {
                 command.@com.google.gwt.user.client.Command::execute()();
              }));
   }-*/;

   public native final void onChangeScrollTop(Command command) /*-{
       this.getSession().on("changeScrollTop",
           $entry(function () {
               command.@com.google.gwt.user.client.Command::execute()();
           }));
   }-*/;

   public native final JavaScriptObject addTokenizerUpdateHandler(CommandWithArg<AceBackgroundTokenizerUpdateEvent> command) /*-{
       var callback = $entry(function (e) {
           command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(e.data);
       });
       this.getSession().on("tokenizerUpdate", callback);
       return callback;
   }-*/;

   public native final void removeTokenizerUpdateHandler(JavaScriptObject callback) /*-{
       this.getSession().off("tokenizerUpdate", callback);
   }-*/;

   public native final JavaScriptObject addAfterRenderHandler(Command command) /*-{
       var callback = $entry(function () {
           command.@com.google.gwt.user.client.Command::execute()();
       });
       this.renderer.on("afterRender", callback);
       return callback;
   }-*/;

   public native final void removeAfterRenderHandler(JavaScriptObject callback) /*-{
       this.renderer.off("afterRender", callback);
   }-*/;

   @SuppressWarnings("hiding")
   public native final <Tooltip> void onShowGutterTooltip(CommandWithArg<Tooltip> command) /*-{
       this.on("showGutterTooltip",
           $entry(function (arg) {
               command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(arg);
           }));
   }-*/;

   public native final <T> void setHandler(String eventName, CommandWithArg<T> handler)
   /*-{
      var defaultHandler = this._defaultHandlers[eventName] || function(evt) {};
      this.setDefaultHandler(eventName, $entry(function(event) {
         event.defaultHandler = defaultHandler;
         handler.@org.rstudio.core.client.CommandWithArg::execute(*)(event);
      }));
   }-*/;
   
   public native final <T> void onGutterMouseDown(CommandWithArg<T> command) /*-{
      this.on("guttermousedown",
         $entry(function (arg) {
            command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(arg);
         }));         
   }-*/;

   public final HandlerRegistration delegateEventsTo(HasHandlers handlers)
   {
      final LinkedList<JavaScriptObject> handles = new LinkedList<>();
      handles.add(addDomListener(getTextInputElement(), "keydown", handlers));
      handles.add(addDomListener(getTextInputElement(), "keypress", handlers));
      handles.add(addDomListener(getTextInputElement(), "changeScrollTop", handlers));
      handles.add(addDomListener(this.cast(), "showGutterTooltip", handlers));
      handles.add(addDomListener(this.<Element>cast(), "focus", handlers));
      handles.add(addDomListener(this.<Element>cast(), "blur", handlers));

      return new HandlerRegistration()
      {
         public void removeHandler()
         {
            while (!handles.isEmpty())
               removeDomListener(handles.remove());
         }
      };
   }

   public final native Element getTextInputElement() /*-{
      return this.textInput.getElement();
   }-*/;

   /**
    * Forces the use of browser focus scrolling. This is the default on Chrome, but on other
    * browsers, a complicated hack involving setting the 'ace_nocontext' attribute on all parent
    * elements is used instead to avoid scroll jitter. This is not necessary in embedded editors,
    * and causes ProseMirror to go nuts (see issue 8518), so this hook allows us to turn it off.
    */
   public final native void useBrowserInputFocus() /*-{
      this.textInput.$focusScroll = "browser";
   }-*/;

   /**
    * Set an aria-label on the input element
    * @param label
    */
   public final void setTextInputAriaLabel(String label)
   {
      Element textInput = getTextInputElement();
      textInput.setAttribute("aria-label", label);
   }

   public final void setElementId(String id)
   {
      getTextInputElement().setId(id);
   }

   private native static JavaScriptObject addDomListener(
         Element element,
         String eventName,
         HasHandlers hasHandlers) /*-{
      var event = $wnd.require("ace/lib/event");
      var listener = $entry(function(e) {
         @com.google.gwt.event.dom.client.DomEvent::fireNativeEvent(Lcom/google/gwt/dom/client/NativeEvent;Lcom/google/gwt/event/shared/HasHandlers;Lcom/google/gwt/dom/client/Element;)(e, hasHandlers, element);
      }); 
      event.addListener(element, eventName, listener);
      return $entry(function() {
         event.removeListener(element, eventName, listener);
      });
   }-*/;

   private native static void removeDomListener(JavaScriptObject handle) /*-{
      handle();
   }-*/;

   public static native AceEditorNative createEditor(Element container) /*-{
      var require = $wnd.require;
      var loader = require("rstudio/loader");
      return loader.loadEditor(container);
   }-*/;
  
   public final native void manageDefaultKeybindings() /*-{
      
      // We bind 'Ctrl + Shift + M' to insert a magrittr shortcut on Windows
      delete this.commands.commandKeyBinding["ctrl-shift-m"];
      
      // We bind 'Ctrl + Shift + P' to run previous code on Windows
      delete this.commands.commandKeyBinding["ctrl-shift-p"];
      
      // We bind 'Ctrl + O' to open files on all platforms.
      // This conflicts with the default Ace binding for splitting lines;
      // rebind to Ctrl-J on all platforms.
      this.commands.byName["splitline"].bindKey = {mac: "Ctrl-J", win: "Ctrl-J"};
      
      // Don't bind 'Cmd+,'
      delete this.commands.commandKeyBinding["cmd-,"];
      
      // We bind 'Ctrl + Alt + A' to 'split into lines'
      if (this.commands.platform !== "mac")
         delete this.commands.commandKeyBinding["ctrl-alt-a"];
         
      // We don't use the internal Ace binding for 'jump to matching',
      // and the binding conflicts with 'Ctrl-P' for moving cursor up
      // when desired by the user (ie, when the RStudio 'jump to matching'
      // is moved out of the way)
      var binding = this.commands.commandKeyBinding["ctrl-p"];
      if (binding[1] && binding[1].name && binding[1].name === "jumptomatching") {
         this.commands.commandKeyBinding["ctrl-p"] = binding[0];
      }
      
   }-*/;

   public static <T> HandlerRegistration addEventListener(
         JavaScriptObject target,
         String event,
         CommandWithArg<T> command)
   {
      final JavaScriptObject functor = addEventListenerInternal(target,
                                                                event,
                                                                command);
      return new HandlerRegistration()
      {
         public void removeHandler()
         {
            invokeFunctor(functor);
         }
      };
   }

   private static native <T> JavaScriptObject addEventListenerInternal(
         JavaScriptObject target,
         String eventName,
         CommandWithArg<T> command)
   /*-{
      
      var callback = $entry(function(data) {
         
         // GWT barfs if we try to pass a 'native' integer here; since
         // we don't try to use these right now just wrap it into something
         // that GWT won't complain about.
         if (typeof data === "number") {
            data = { type: "number", value: data };
         }
         
         // Some Ace events provide something like a 'boxed' string with
         // the relevant payload in 'data.text'; if it exists, grab it.
         if (data && data.text) {
            data = data.text;
         }
         
         command.@org.rstudio.core.client.CommandWithArg::execute(*)(data);
         
      });

      target.addEventListener(eventName, callback);
      return function() {
         target.removeEventListener(eventName, callback);
      };
   }-*/;

   private static native void invokeFunctor(JavaScriptObject functor) /*-{
      functor();
   }-*/;
   
   public final native void scrollPageUp() /*-{ this.scrollPageUp(); }-*/;
   public final native void scrollPageDown() /*-{ this.scrollPageDown(); }-*/;

   public final native void gotoPageUp() /*-{ this.gotoPageUp(); }-*/;
   public final native void gotoPageDown() /*-{ this.gotoPageDown(); }-*/;
   
   public final native void selectPageUp() /*-{ this.selectPageUp(); }-*/;
   public final native void selectPageDown() /*-{ this.selectPageDown(); }-*/;
   
   public final native void scrollToRow(int row) /*-{
      this.scrollToRow(row);
   }-*/;
   
   public final native void centerSelection() /*-{
      this.centerSelection();
   }-*/;

   public final native void scrollToLine(int line, boolean center) /*-{
      this.scrollToLine(line, center);
   }-*/;
   
   public final native void jumpToMatching(boolean select, boolean expand) /*-{
      this.jumpToMatching(select, expand);
   }-*/;
   
   public final native void splitIntoLines() /*-{
      return this.multiSelect && this.multiSelect.splitIntoLines();
   }-*/;
   
   public native final void revealRange(Range range, boolean animate) /*-{
      this.revealRange(range, animate);
   }-*/;

   public final native void autoHeight() /*-{
      this.setOptions({
         minLines: 1,
         maxLines: Infinity,
         scrollPastEnd: false
      });
   }-*/;

   public final native void onCursorChange() /*-{
      this.onCursorChange();
   }-*/;

   public final void setInsertMatching(boolean value)
   {
      getSession().getMode().setInsertMatching(value);
   }

   public static native void setVerticallyAlignFunctionArgs(
         boolean verticallyAlign) /*-{
      $wnd.require("mode/r_code_model").setVerticallyAlignFunctionArgs(verticallyAlign);
   }-*/;

   public static native void setHierarchicalSectionFolding(
         boolean enable) /*-{
      $wnd.require("mode/r_code_model").setHierarchicalSectionFolding(enable);
   }-*/;

   public final native int getFirstVisibleRow() /*-{
      return this.getFirstVisibleRow();
   }-*/;

   public final native int getLastVisibleRow() /*-{
      return this.getLastVisibleRow();
   }-*/;
   
   public final native int findAll(String needle) /*-{
      return this.findAll(needle);
   }-*/;
   
   public final native int findAll(String needle, Range range, boolean wholeWord, boolean caseSensitive) /*-{
      return this.findAll(needle, {range: range, wholeWord: wholeWord, caseSensitive: caseSensitive});
   }-*/;
   
   public final native void insert(String text) /*-{
      var that = this;
      this.forEachSelection(function() {
         that.insert(text);
      });
   }-*/;

   public final native void startOperation() /*-{
      this.startOperation();
   }-*/;

   public final native void endOperation() /*-{
      this.endOperation();
   }-*/;
   
   public final native boolean inMultiSelectMode() /*-{ return this.inMultiSelectMode === true; }-*/;
   public final native void exitMultiSelectMode() /*-{ this.exitMultiSelectMode(); }-*/;
   public final native void clearSelection() /*-{ return this.clearSelection(); }-*/;
   
   public final native void moveCursorTo(int row, int column) /*-{
      return this.moveCursorTo(row, column);
   }-*/;
   
   public final native void moveCursorToPosition(Position pos) /*-{
      return this.moveCursorToPosition(pos);
   }-*/;
   
   public final native void moveCursorLeft(int times) /*-{
      var that = this;
      this.forEachSelection(function() {
         that.navigateLeft(times);
      });
   }-*/;
   
   public final native void moveCursorRight(int times) /*-{
      var that = this;
      this.forEachSelection(function() {
         that.navigateRight(times);
      });
   }-*/;
   
   public final native void expandSelectionLeft(int times) /*-{
      var that = this;
      this.forEachSelection(function() {
         var selection = that.getSelection();
         for (var i = 0; i < times; i++)
            selection.selectLeft();
      });
   }-*/;
   
   public final native void expandSelectionRight(int times) /*-{
      var that = this;
      this.forEachSelection(function() {
         var selection = that.getSelection();
         for (var i = 0; i < times; i++)
            selection.selectRight();
      });
   }-*/;
   
   public final native Position getCursorPosition() /*-{
      return this.getCursorPosition();
   }-*/;
   
   public final native Position getCursorPositionScreen() /*-{
      return this.getCursorPositionScreen();
   }-*/;
   
   public final native void blockIndent() /*-{
      return this.blockIndent();
   }-*/;

   public final native void blockOutdent() /*-{
      return this.blockOutdent();
   }-*/;
   
   public final native void expandSelection() /*-{
      return this.$expandSelection();
   }-*/;
   
   public final native void shrinkSelection() /*-{
      return this.$shrinkSelection();
   }-*/;
   
   public final native void clearSelectionHistory() /*-{
      return this.$clearSelectionHistory();
   }-*/;
   
   public final native Element getContainer() /*-{
      return this.container;
   }-*/;
   
   public final native AceCommandManager getCommandManager()
   /*-{
      return this.commands;
   }-*/;

   /**
    * Make this editor's line start / line end navigation commands act on
    * document lines rather than soft-wrapped screen rows.
    *
    * By default, Ace treats each visual row of a soft-wrapped line as its own
    * line, so Home / End (and, on macOS, Ctrl+A / Ctrl+E) stop at the wrap
    * boundary. That's the right behavior for a text editor, but not for a
    * console, where the whole wrapped command should behave as a single line
    * the way it does under readline.
    *
    * https://github.com/rstudio/rstudio/issues/18447
    */
   public final native void useDocumentLineNavigation()
   /*-{
      var commands = this.commands;

      // Ace's default command objects are shared by every editor instance, so
      // install clones rather than mutating the originals. The command tables
      // themselves are per-editor, so this only affects this editor.
      var override = function(name, exec) {
         var command = commands.commands[name];
         if (command == null)
            return;

         var clone = {};
         for (var key in command)
         {
            if (command.hasOwnProperty(key))
               clone[key] = command[key];
         }

         clone.exec = exec;
         commands.addCommand(clone);
      };

      var lineStart = function(editor) {
         return { row: editor.getCursorPosition().row, column: 0 };
      };

      var lineEnd = function(editor) {
         var row = editor.getCursorPosition().row;
         return { row: row, column: editor.getSession().getLine(row).length };
      };

      var navigateToLineStart = function(editor) {
         var position = lineStart(editor);
         editor.navigateTo(position.row, position.column);
      };

      var navigateToLineEnd = function(editor) {
         var position = lineEnd(editor);
         editor.navigateTo(position.row, position.column);
      };

      var selectToLineStart = function(editor) {
         var position = lineStart(editor);
         editor.getSelection().selectTo(position.row, position.column);
      };

      var selectToLineEnd = function(editor) {
         var position = lineEnd(editor);
         editor.getSelection().selectTo(position.row, position.column);
      };

      override("gotolinestart", navigateToLineStart);
      override("gotolineend", navigateToLineEnd);

      // Ace has two equivalent pairs of selection commands here: 'selectto*'
      // (Cmd+Shift+Left / Ctrl+Shift+A and friends) and 'select*' (Shift+Home /
      // Shift+End). Which one a given key reaches varies by platform, so both
      // have to move in step with the navigation commands above.
      override("selecttolinestart", selectToLineStart);
      override("selecttolineend", selectToLineEnd);
      override("selectlinestart", selectToLineStart);
      override("selectlineend", selectToLineEnd);
   }-*/;

   public final void tokenizeDocument()
   {
      tokenizeUpToRow(getSession().getLength() - 1);
   }
   
   public final void retokenizeDocument()
   {
      resetTokenizer();
      tokenizeUpToRow(getSession().getLength() - 1);
   }
   
   public final native void resetTokenizer() /*-{
      var session = this.getSession();
      var tokenizer = session.bgTokenizer;
      tokenizer.currentLine = 0;
   }-*/;
   
   public final native void tokenizeUpToRow(int row) /*-{
      var session = this.getSession();
      var tokenizer = session.bgTokenizer;
      var lastTokenizedRow = tokenizer.currentLine;
      var maxRow = Math.max(row, session.getLength() - 1);
      for (var i = lastTokenizedRow; i <= maxRow; i++)
         tokenizer.$tokenizeRow(i);
      tokenizer.fireUpdateEvent(lastTokenizedRow, maxRow);
   }-*/;
   
   public final native void setCommandManager(AceCommandManager commands)
   /*-{
      this.commands = commands;
   }-*/;
   
   public final native void setDragEnabled(boolean enabled) /*-{
      this.setOption("dragEnabled", enabled);
   }-*/;
   
   public final native boolean dragEnabled() /*-{
      return this.getOption("dragEnabled");
   }-*/;

   /**
    * Resets the mouse handler state to recover from any corruption.
    * This can happen if an exception occurs during mouse event handling,
    * leaving the handler in an inconsistent state.
    * See: https://github.com/rstudio/rstudio/issues/13436
    */
   public final native void resetMouseHandlerState()
   /*-{
      var handler = this.$mouseHandler;
      if (!handler) return;

      // Reset core state variables
      handler.state = "";
      handler.isMousePressed = false;
      handler.$mouseMoved = false;
      handler.$clickSelection = null;

      // Reset renderer state
      var renderer = this.renderer;
      if (renderer) {
         renderer.$isMousePressed = false;
      }

      // Clean up any lingering capture
      if (handler.releaseMouse) {
         try {
            handler.releaseMouse();
         } catch (e) {
            // Ignore errors during cleanup
         }
         handler.releaseMouse = null;
      }
      handler.$onCaptureMouseMove = null;

      // Reset virtual selection mode if stuck
      if (this.inVirtualSelectionMode) {
         this.inVirtualSelectionMode = false;
      }
   }-*/;

   /**
    * Returns true if the mouse handler appears to be in a corrupt state.
    * See: https://github.com/rstudio/rstudio/issues/13436
    */
   public final native boolean isMouseHandlerStateCorrupt()
   /*-{
      var handler = this.$mouseHandler;
      if (!handler) return false;

      // Check for suspicious state combinations:
      // State should be empty when mouse is not pressed
      if (handler.state && handler.state !== "" && !handler.isMousePressed) {
         return true;
      }

      // releaseMouse should be null when not in a capture operation
      if (handler.releaseMouse && !handler.isMousePressed) {
         return true;
      }

      // inVirtualSelectionMode should not persist indefinitely
      if (this.inVirtualSelectionMode && !handler.isMousePressed) {
         return true;
      }

      return false;
   }-*/;

   /**
    * Returns the reason the editor's multi-select state appears to be
    * corrupt, or null if it looks healthy. Corruption can arise when an
    * exception interrupts one of Ace's multi-select operations, which mutate
    * selection state and restore it on the way out. forEachSelection and
    * $moveLines are exception-protected by the mixins/multi_select_guard
    * wrapper, but other paths (addRange, toSingleRange, $onRemoveRange) can
    * still strand state, as can corruption predating the guard.
    * See: https://github.com/rstudio/rstudio/issues/13605
    */
   public final native String getMultiSelectCorruptionReason()
   /*-{
      var session = this.session;
      if (!session || !session.selection)
         return null;

      var selection = session.selection;

      // A temporary selection from an interrupted forEachSelection is still
      // installed. ('session.multiSelect' is the session's real selection
      // object once the session has been attached to an editor, and only
      // forEachSelection's temporary selections carry an 'index'; both are
      // invariants of the vendored Ace bundle, to be re-checked on updates.)
      if (selection.index !== undefined)
         return "temporary selection still installed";
      if (session.multiSelect && selection !== session.multiSelect)
         return "session selection is not the real selection";

      // The editor and selection disagree about multi-select mode, meaning a
      // 'multiSelect' or 'singleSelect' notification was lost.
      if (!!this.inMultiSelectMode != !!selection.inMultiSelectMode)
         return "editor/selection multi-select flags disagree";

      if (selection.inMultiSelectMode) {

         // Multi-select mode with no ranges to operate on.
         if (!selection.rangeCount)
            return "multi-select mode with no ranges";

         // The selection's range bookkeeping arrays have fallen out of sync.
         if (selection.ranges == null || selection.rangeList == null)
            return "range bookkeeping arrays missing";
         if (selection.ranges.length !== selection.rangeList.ranges.length)
            return "range bookkeeping arrays out of sync";

         // The range list must be attached to track document edits.
         if (selection.rangeList.session == null)
            return "range list detached";
      }

      // A stuck 'inVirtualSelectionMode' is deliberately not checked here:
      // Ace's add/block mouse selection paths legitimately keep that flag set
      // from mousedown until mouseup, across event loop ticks, and this check
      // can run mid-drag (the mousedown that begins a drag also focuses the
      // editor, scheduling the deferred focus-time check). Treating the flag
      // alone as corruption would reset an in-progress drag selection. An
      // aborted forEachSelection or $moveLines strands other state alongside
      // it, which the checks above do catch.
      return null;
   }-*/;

   /**
    * Resets the editor's multi-select state, dropping any extra cursors and
    * returning cleanly to single-selection mode.
    * See: https://github.com/rstudio/rstudio/issues/13605
    */
   public final native void resetMultiSelectState()
   /*-{
      // A failure in any individual recovery step is worth surfacing: a
      // silent partial reset would be mistaken for a successful one.
      var log = function(step, e) {
         @org.rstudio.core.client.Debug::log(Ljava/lang/String;)(
            "Error while resetting Ace multi-select state (" + step + "): " + e);
      };

      try {
         var session = this.session;
         if (!session || !session.selection)
            return;

         // Restore the session's real selection object if a temporary
         // selection from an interrupted operation is still installed.
         var multiSelect = session.multiSelect;
         if (multiSelect && session.selection !== multiSelect) {
            var tmpSel = session.selection;
            if (tmpSel && typeof tmpSel.detach === "function") {
               try { tmpSel.detach(); } catch (e) { log("detaching temporary selection", e); }
            }
            this.selection = session.selection = multiSelect;
         }

         var selection = session.selection;

         // Clear any stuck virtual selection state.
         this.inVirtualSelectionMode = false;

         // Remove any leftover selection markers.
         if (session.$selectionMarkers && session.$selectionMarkers.length) {
            try {
               this.removeSelectionMarkers(session.$selectionMarkers.slice());
            } catch (e) {
               log("removing selection markers", e);
            }

            // A mid-loop failure above leaves the marker count stale.
            session.selectionMarkerCount = session.$selectionMarkers.length;
         }

         // Drop all extra ranges and restore single-select bookkeeping.
         if (selection.rangeList) {
            if (selection.rangeList.session) {
               try { selection.rangeList.detach(); } catch (e) { log("detaching range list", e); }
            }
            selection.rangeList.ranges.length = 0;
         }

         if (selection.ranges)
            selection.ranges.length = 0;

         selection.rangeCount = 0;
         selection.inMultiSelectMode = false;

         // Entering multi-select mode disables selection restoration on undo
         // (Selection.addRange sets 'session.$undoSelect' to false); Ace
         // normally re-enables it in $onRemoveRange, which this reset
         // bypasses.
         session.$undoSelect = true;

         // Tear down editor-side multi-select mode: the multi-select keyboard
         // handler, the 'exec' default handler, the 'ace_multiselect' style
         // class, and the cursor/marker rendering.
         if (this.inMultiSelectMode && this.$onSingleSelect) {
            try {
               this.$onSingleSelect();
            } catch (e) {
               // $onSingleSelect tears down in steps, ending with the 'exec'
               // default handler that reroutes every command through
               // multi-select dispatch; make sure that handler is gone even
               // when an earlier step threw. Restore 'inMultiSelectMode' (the
               // teardown clears it first) so the corruption detector keeps
               // flagging this editor and the reset is retried.
               try { this.commands.removeDefaultHandler("exec", this.$onMultiSelectExec); } catch (e2) { }
               log("exiting editor multi-select mode", e);
               this.inMultiSelectMode = true;
               return;
            }
         }
         this.inMultiSelectMode = false;
      } catch (e) {
         // Backstop only; the recovery steps above catch and log their own
         // failures. Recovery must never throw.
         log("unexpected", e);
      }
   }-*/;

   public final native JsMap<Position> getMarks() /*-{
      
      var marks = {};
      if (this.state &&
          this.state.cm &&
          this.state.cm.state &&
          this.state.cm.state.vim &&
          this.state.cm.state.vim.marks)
       {
          marks = this.state.cm.state.vim.marks;
       }
       
      var result = {};
      for (var key in marks) {
         if (marks.hasOwnProperty(key)) {
            var mark = marks[key];
            result[key] = {
               row: mark.row,
               column: mark.column
            };
         }
      }
      
      return result;
   
   }-*/;
   
   public final native void setMarks(JsMap<Position> marks) /*-{
      
      if (this.state &&
          this.state.cm &&
          this.state.cm.state &&
          this.state.cm.state.vim)
      {
         var cm = this.state.cm;
         var vim = this.state.cm.state.vim;
         
         if (!vim.marks)
            vim.marks = {};
            
         for (var key in marks) {
            var mark = marks[key];
            vim.marks[key] = cm.setBookmark({line: mark.row, ch: mark.column});
         }
      }
   
   }-*/;
   
   public static final native void setDefaultInsertMatching(boolean value) /*-{
      $wnd.require("mode/auto_brace_insert").setInsertMatching(value);
   }-*/;
   
   public final static void syncUiPrefs(UserPrefs userPrefs)
   {
      if (uiPrefsSynced_)
         return;

      userPrefs.insertMatching().bind(new CommandWithArg<Boolean>() 
      {
         @Override
         public void execute(Boolean arg) 
         {
            setDefaultInsertMatching(arg);
         }
      });
      
      userPrefs.verticallyAlignArgumentsIndent().bind(new CommandWithArg<Boolean>()
      {
         @Override
         public void execute(Boolean arg)
         {
            setVerticallyAlignFunctionArgs(arg);
         }
      });

      userPrefs.hierarchicalSectionFolding().bind(new CommandWithArg<Boolean>()
      {
         @Override
         public void execute(Boolean arg)
         {
            setHierarchicalSectionFolding(arg);
         }
      });

      uiPrefsSynced_ = true;
   }
   
   public final native void setSurroundSelectionPref(String value) /*-{
      this.$surroundSelection = value;
   }-*/;
   
   public final native boolean isVimModeOn() /*-{
      return this.$vimModeHandler != null;
   }-*/;
   
   public final native boolean isEmacsModeOn() /*-{
      return this.$emacsModeHandler != null;
   }-*/;
   
   public final native void clearEmacsMark() /*-{
      this.pushEmacsMark(null);
   }-*/;
   
   // Get the underlying Ace instance associated with a DOM element.
   // This element may either be a child of the parent Ace container,
   // or the element itself.
   public static final native AceEditorNative getEditor(Element el) /*-{
      while (el != null) {
         if (el.env && el.env.editor)
            return el.env.editor;
         el = el.parentNode;
      }
      return null;
   }-*/;
   
   public final native void disableSearchHighlight() /*-{
      var highlight = this.session.$searchHighlight;
      if (highlight) {
         highlight.$update = highlight.update;
         highlight.update = function() {}
      }
   }-*/;
   
   public final native void enableSearchHighlight() /*-{
      var highlight = this.session.$searchHighlight;
      if (highlight && highlight.$update) {
         highlight.update = highlight.$update;
      }
   }-*/;
   
   public final native void execCommand(String commandName) /*-{
      this.execCommand(commandName);
   }-*/;
   
   public final native void setCursorStyle(String style) /*-{
      this.setOption("cursorStyle", style);
   }-*/;
   
   public final native void setScrollSpeed(double speed) /*-{
      this.setOption("scrollSpeed", speed);
   }-*/;
   
   public final native void setAnimatedScroll(boolean shouldAnimate) /*-{
      this.setAnimatedScroll(shouldAnimate);
   }-*/;
   
   public final native void setIndentedSoftWrap(boolean softWrap) /*-{
      this.setOption("indentedSoftWrap", softWrap);
   }-*/;

   public final native void setMaxLines(int max) /*-{
      this.setOption("maxLines", max);
   }-*/;

   public final native void setMinLines(int min) /*-{
      this.setOption("minLines", min);
   }-*/;

   public final native void setTabMovesFocus(boolean movesFocus) /*-{
      if (movesFocus) {
         this.commands.bindKey("Tab", null);
         this.commands.bindKey("Shift+Tab", null);
      } else {
         this.commands.bindKey("Tab", "indent");
         this.commands.bindKey("Shift+Tab", "outdent");
      }
   }-*/;
   
   public final native AceGhostText getGhostText() /*-{
      return this.renderer.$ghostText;
   }-*/;
   
   public final native void setGhostText(String text, Position position) /*-{
      this.setGhostText(text, position);
   }-*/;
   
   public final void applyGhostText()
   {
      AceGhostText ghostText = getGhostText();
      getSession().replace(
            Range.fromPoints(ghostText.position, ghostText.position),
            ghostText.text);
      removeGhostText();
   }
   
   public final native boolean hasGhostText() /*-{
      return this.renderer.$ghostText != null;
   }-*/;
   
   public final native void removeGhostText() /*-{
      this.removeGhostText();
   }-*/;

   // Synthetic token management - tokens that persist across re-tokenization
   public final native void addSyntheticToken(int row, int column, String text, String type) /*-{
      this.session.addSyntheticToken(row, column, text, type);
   }-*/;

   public final native void removeSyntheticTokensForRow(int row) /*-{
      this.session.removeSyntheticTokensForRow(row);
   }-*/;

   public final native void clearSyntheticTokens() /*-{
      this.session.clearSyntheticTokens();
   }-*/;

   private static final native void initialize()
   /*-{
      // Remove the 'Return' keybinding associated with Emacs.
      // We attach some custom behaviors to 'Return', and we
      // don't want Emacs to override those behaviors.
      // E.g. the 'Continue comment on newline insertion'
      // preference.
      var Emacs = $wnd.require("ace/keyboard/emacs");
      var handler = Emacs.handler || {};
      var bindings = handler.commandKeyBinding || {};
      if (bindings.hasOwnProperty("return")) {
         delete bindings["return"];
      }
   }-*/;
   
   public final native boolean hasActiveAceCompleter() /*-{
      return !!(this.completer && this.completer.activated);
   }-*/;
   
   // NOTE: We intentionally bypass Ace's 'setTheme()' API, as that only
   // allows one to load themes that are bundled with Ace, but we instead
   // load and manage themes ourselves.
   public final native void setTheme(AceTheme theme) /*-{
      theme = theme || { isDark: false };
      this.renderer.theme = theme;
   }-*/;

   static { initialize(); }

   private static boolean uiPrefsSynced_ = false;
}
