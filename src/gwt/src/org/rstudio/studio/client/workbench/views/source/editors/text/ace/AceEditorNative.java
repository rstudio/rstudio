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
      this.setDefaultHandler(eventName, function(event) {
         event.defaultHandler = defaultHandler;
         handler.@org.rstudio.core.client.CommandWithArg::execute(*)(event);
      });
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
   
   public final native void setGhostText(String text) /*-{
      this.setGhostText(text);
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
