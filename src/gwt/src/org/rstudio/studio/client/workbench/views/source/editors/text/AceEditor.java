/*
 * AceEditor.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.AceSupport;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.KeyboardTracker;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JsMap;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.patch.TextChange;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.core.client.widget.DynamicIFrame;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.DocumentMode.Mode;
import org.rstudio.studio.client.events.EditEvent;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.MainWindowObject;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.model.EventBasedChangeTracker;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager.InitCompletionFilter;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.DelegatingCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.MarkdownCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.NullCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PythonCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.SqlCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.StanCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.output.lint.DiagnosticsBackgroundPopup;
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorWidget.TabKeyMode;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceAfterCommandExecutedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceBackgroundHighlighter;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceClickEvent.Handler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommandManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorBackgroundLinkHighlighter;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorCommandEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceInputEditorPosition;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceKeyboardActivityEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceResources;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AnchoredRange;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.CodeModel;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.EditSession;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.FoldingRules;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.KeyboardHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Marker;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Search;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Selection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.TokenPredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.WordIterable;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.AceSelectionChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ActiveScopeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.BreakpointMoveEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.BreakpointSetEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CommandClickEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FindRequestedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FoldChangeEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.LineWidgetsChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.UndoRedoHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDoc;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionEvent;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionHandler;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileHandler;
import org.rstudio.studio.client.workbench.views.source.events.ScrollYEvent;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AceEditor implements DocDisplay,
                                  InputEditorDisplay,
                                  NavigableSourceEditor
{
   public enum NewLineMode
   {
      Windows("windows"),
      Unix("unix"),
      Auto("auto");

      NewLineMode(String type)
      {
         this.type = type;
      }

      public String getType()
      {
         return type;
      }

      private String type;
   }

   private class Filter implements InitCompletionFilter
   {
      public boolean shouldComplete(NativeEvent event)
      {
         // Never complete if there's an active selection.
         Range range = getSession().getSelection().getRange();
         if (!range.isEmpty())
            return false;

         // If the user explicitly requested an auto-complete by pressing 'ctrl-space' instead of 'tab', always attempt auto-complete.
         if (event != null && event.getKeyCode() != KeyCodes.KEY_TAB)
            return true;

         // Tab was pressed. Don't attempt auto-complete if the user opted out of tab completions.
         if (!userPrefs_.tabCompletion().getValue() || userPrefs_.tabKeyMoveFocus().getValue())
            return false;

         // If the user opted in to multi-line tab completion, there is no case where we don't attempt auto-complete.
         if (userPrefs_.tabMultilineCompletion().getValue())
            return true;

         // Otherwise, don't complete if we're at the start of the line...
         int col = range.getStart().getColumn();
         if (col == 0)
            return false;

         // ... or if there is nothing but whitespace between the start of the line and the cursor.
         String line = getSession().getLine(range.getStart().getRow());
         return line.substring(0, col).trim().length() != 0;

      }
   }

   private class AnchoredSelectionImpl implements AnchoredSelection
   {
      private AnchoredSelectionImpl(Anchor start, Anchor end)
      {
         start_ = start;
         end_ = end;
      }

      public String getValue()
      {
         return getSession().getTextRange(getRange());
      }

      public void apply()
      {
         getSession().getSelection().setSelectionRange(
            getRange());
      }

      public Range getRange()
      {
         return Range.fromPoints(start_.getPosition(), end_.getPosition());
      }

      public void detach()
      {
         start_.detach();
         end_.detach();
      }

      private final Anchor start_;
      private final Anchor end_;
   }

   private class AceEditorChangeTracker extends EventBasedChangeTracker<Void>
   {
      private AceEditorChangeTracker()
      {
         super(AceEditor.this);
         AceEditor.this.addFoldChangeHandler(event -> changed_ = true);
         AceEditor.this.addLineWidgetsChangedHandler(event -> changed_ = true);
      }

      @Override
      public ChangeTracker fork()
      {
         AceEditorChangeTracker forked = new AceEditorChangeTracker();
         forked.changed_ = changed_;
         return forked;
      }
   }

   public static void preload()
   {
      load(null);
   }

   public static void load(final Command command)
   {
      aceLoader_.addCallback(() ->
            aceSupportLoader_.addCallback(() ->
                  extLanguageToolsLoader_.addCallback(() ->
                        vimLoader_.addCallback(() ->
                              emacsLoader_.addCallback(() ->
                              {
                                 AceSupport.initialize();

                                 if (command != null)
                                    command.execute();
                              })
                        )
                  )
            )
      );
   }

   public static final native AceEditor getEditor(Element el)
   /*-{
      for (; el != null; el = el.parentElement)
         if (el.$RStudioAceEditor != null)
            return el.$RStudioAceEditor;
   }-*/;
   
   private static final native void attachToWidget(Element el, AceEditor editor)
   /*-{
      el.$RStudioAceEditor = editor;
   }-*/;
   
   private static final native void detachFromWidget(Element el)
   /*-{
      el.$RStudioAceEditor = null;
   }-*/;

   @Inject
   public AceEditor()
   {
      widget_ = new AceEditorWidget();
      snippets_ = new SnippetHelper(this);
      monitor_ = new AceEditorMonitor(this);
      editorEventListeners_ = new ArrayList<>();
      mixins_ = new AceEditorMixins(this);
      editLines_ = new AceEditorEditLinesHelper(this);

      completionManager_ = new NullCompletionManager();
      diagnosticsBgPopup_ = new DiagnosticsBackgroundPopup(this);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      backgroundTokenizer_ = new BackgroundTokenizer(this);
      vim_ = new Vim(this);
      bgLinkHighlighter_ = new AceEditorBackgroundLinkHighlighter(this);
      bgChunkHighlighter_ = new AceBackgroundHighlighter(this);
      
      widget_.addValueChangeHandler(evt ->
      {
         if (!valueChangeSuppressed_)
         {
            ValueChangeEvent.fire(AceEditor.this, null);
         }
      });
      
      widget_.addFoldChangeHandler(event -> AceEditor.this.fireEvent(new FoldChangeEvent()));
      
      events_.addHandler(EditEvent.TYPE, new EditEvent.Handler()
      {
         @Override
         public void onEdit(EditEvent event)
         {
            if (event.isBeforeEdit())
            {
               activeEditEventType_ = event.getType();
            }
            else
            {
               Scheduler.get().scheduleDeferred(() ->
               {
                  activeEditEventType_ = EditEvent.TYPE_NONE;
               });
            }
         }
      });

      addPasteHandler(event ->
      {
         if (completionManager_ != null)
            completionManager_.onPaste(event);

         final Position start = getSelectionStart();

         Scheduler.get().scheduleDeferred(() ->
         {
            Range range = Range.fromPoints(start, getSelectionEnd());
            if (shouldIndentOnPaste())
               indentPastedRange(range);
         });
      });

      // handle click events
      addAceClickHandler(event ->
      {
         fixVerticalOffsetBug();
         if (DomUtils.isCommandClick(event.getNativeEvent()))
         {
            // eat the event so ace doesn't do anything with it
            event.preventDefault();
            event.stopPropagation();

            // go to function definition
            fireEvent(new CommandClickEvent(event));
         }
         else
         {
            // if the focus in the Help pane or another iframe
            // we need to make sure to get it back
            WindowEx.get().focus();
         }
      });

      lastCursorChangedTime_ = 0;
      addCursorChangedHandler(event ->
      {
         fixVerticalOffsetBug();
         clearLineHighlight();
         lastCursorChangedTime_ = System.currentTimeMillis();
      });

      lastModifiedTime_ = 0;
      addValueChangeHandler(event ->
      {
         lastModifiedTime_ = System.currentTimeMillis();
         clearDebugLineHighlight();
      });
      
      widget_.addAttachHandler(event ->
      {
         if (event.isAttached())
         {
            attachToWidget(widget_.getElement(), AceEditor.this);

            // If the ID was set earlier, as is done for the Console's edit field, don't stomp over it
            if (StringUtil.isNullOrEmpty(widget_.getElement().getId()))
               ElementIds.assignElementId(widget_, ElementIds.SOURCE_TEXT_EDITOR);
         }
         else
            detachFromWidget(widget_.getElement());

         if (!event.isAttached())
         {
            for (HandlerRegistration handler : editorEventListeners_)
               handler.removeHandler();
            editorEventListeners_.clear();

            if (completionManager_ != null)
            {
               completionManager_.detach();
               completionManager_ = null;
            }

            if (s_lastFocusedEditor == AceEditor.this)
            {
               s_lastFocusedEditor = null;
            }
         }
      });
      
      widget_.addFocusHandler((FocusEvent event) ->
      {
         String id = AceEditor.this.getWidget().getElement().getId();
         MainWindowObject.lastFocusedEditorId().set(id);
      });
      
      addFocusHandler((FocusEvent event) -> s_lastFocusedEditor = this);
      
      events_.addHandler(
            AceEditorCommandEvent.TYPE,
            event ->
            {
               // skip this if this is only for the actively focused Ace instance
               // (note: in RStudio Server, the Ace Editor instance may become
               // unfocused when e.g. executing commands from the menu, so we
               // need to ensure this routes to the most recently focused editor)
               boolean ignore =
                     event.getExecutionPolicy() == AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED &&
                     AceEditor.this != s_lastFocusedEditor;

               if (ignore)
                  return;

               switch (event.getCommand())
               {
               case AceEditorCommandEvent.YANK_REGION:                yankRegion();               break;
               case AceEditorCommandEvent.YANK_BEFORE_CURSOR:         yankBeforeCursor();         break;
               case AceEditorCommandEvent.YANK_AFTER_CURSOR:          yankAfterCursor();          break;
               case AceEditorCommandEvent.PASTE_LAST_YANK:            pasteLastYank();            break;
               case AceEditorCommandEvent.INSERT_ASSIGNMENT_OPERATOR: insertAssignmentOperator(); break;
               case AceEditorCommandEvent.INSERT_PIPE_OPERATOR:       insertPipeOperator();       break;
               case AceEditorCommandEvent.JUMP_TO_MATCHING:           jumpToMatching();           break;
               case AceEditorCommandEvent.SELECT_TO_MATCHING:         selectToMatching();         break;
               case AceEditorCommandEvent.EXPAND_TO_MATCHING:         expandToMatching();         break;
               case AceEditorCommandEvent.ADD_CURSOR_ABOVE:           addCursorAbove();           break;
               case AceEditorCommandEvent.ADD_CURSOR_BELOW:           addCursorBelow();           break;
               case AceEditorCommandEvent.EDIT_LINES_FROM_START:      editLinesFromStart();       break;
               case AceEditorCommandEvent.INSERT_SNIPPET:             onInsertSnippet();          break;
               case AceEditorCommandEvent.MOVE_LINES_UP:              moveLinesUp();              break;
               case AceEditorCommandEvent.MOVE_LINES_DOWN:            moveLinesDown();            break;
               case AceEditorCommandEvent.EXPAND_TO_LINE:             expandToLine();             break;
               case AceEditorCommandEvent.COPY_LINES_DOWN:            copyLinesDown();            break;
               case AceEditorCommandEvent.JOIN_LINES:                 joinLines();                break;
               case AceEditorCommandEvent.REMOVE_LINE:                removeLine();               break;
               case AceEditorCommandEvent.SPLIT_INTO_LINES:           splitIntoLines();           break;
               case AceEditorCommandEvent.BLOCK_INDENT:               blockIndent();              break;
               case AceEditorCommandEvent.BLOCK_OUTDENT:              blockOutdent();             break;
               case AceEditorCommandEvent.REINDENT:                   reindent();                 break;
               }
            });
   }
   
   public void yankRegion()
   {
      if (isVimModeOn() && !isVimInInsertMode())
         return;
      
      // no-op if there is no selection
      String selectionValue = getSelectionValue();
      if (StringUtil.isNullOrEmpty(selectionValue))
         return;
      
      if (Desktop.hasDesktopFrame() && isEmacsModeOn())
      {
         Desktop.getFrame().setClipboardText(selectionValue);
         replaceSelection("");
         clearEmacsMark();
      }
      else
      {
         yankedText_ = getSelectionValue();
         replaceSelection("");
      }
   }
   
   public void yankBeforeCursor()
   {
      if (isVimModeOn() && !isVimInInsertMode())
         return;
      
      Position cursorPos = getCursorPosition();
      Range yankRange = Range.fromPoints(
            Position.create(cursorPos.getRow(), 0),
            cursorPos);
      
      if (Desktop.hasDesktopFrame() && isEmacsModeOn())
      {
         String text = getTextForRange(yankRange);
         Desktop.getFrame().setClipboardText(StringUtil.notNull(text));
         replaceRange(yankRange, "");
         clearEmacsMark();
      }
      else
      {
         setSelectionRange(yankRange);
         yankedText_ = getSelectionValue();
         replaceSelection("");
      }
   }
   
   public void yankAfterCursor()
   {
      if (isVimModeOn() && !isVimInInsertMode())
         return;
      
      Position cursorPos = getCursorPosition();
      Range yankRange = null;
      String line = getLine(cursorPos.getRow());
      int lineLength = line.length();
      
      // if the cursor is already at the end of the line
      // (allowing for trailing whitespace), then eat the
      // newline as well; otherwise, just eat to end of line
      String rest = line.substring(cursorPos.getColumn());
      if (rest.trim().isEmpty())
      {
         yankRange = Range.fromPoints(
               cursorPos,
               Position.create(cursorPos.getRow() + 1, 0));
      }
      else
      {
         yankRange = Range.fromPoints(
               cursorPos,
               Position.create(cursorPos.getRow(), lineLength));
      }
      
      if ((Desktop.hasDesktopFrame()) && isEmacsModeOn())
      {
         String text = getTextForRange(yankRange);
         Desktop.getFrame().setClipboardText(StringUtil.notNull(text));
         replaceRange(yankRange, "");
         clearEmacsMark();
      }
      else
      {
         setSelectionRange(yankRange);
         yankedText_ = getSelectionValue();
         replaceSelection("");
      }
   }
   
   public void pasteLastYank()
   {
      if (isVimModeOn() && !isVimInInsertMode())
         return;
      
      if (Desktop.hasDesktopFrame() && isEmacsModeOn())
      {
         Desktop.getFrame().getClipboardText((String text) ->
         {
            replaceSelection(text);
            setCursorPosition(getSelectionEnd());
         });
      }
      else
      {
         if (yankedText_ == null)
            return;
         
         replaceSelection(yankedText_);
         setCursorPosition(getSelectionEnd());
      }
   }
   
   public void insertAssignmentOperator()
   {
      if (DocumentMode.isCursorInRMode(this))
         insertAssignmentOperatorImpl("<-");
      else
         insertAssignmentOperatorImpl("=");
   }
   
   @SuppressWarnings("deprecation")
   private void insertAssignmentOperatorImpl(String op)
   {
      boolean hasWhitespaceBefore =
            Character.isSpace(getCharacterBeforeCursor()) ||
            (!hasSelection() && getCursorPosition().getColumn() == 0);
      
      String insertion = hasWhitespaceBefore
            ? op + " "
            : " " + op + " ";
      
      insertCode(insertion, false);
   }
   
   @SuppressWarnings("deprecation")
   public void insertPipeOperator()
   {
      boolean hasWhitespaceBefore =
            Character.isSpace(getCharacterBeforeCursor()) ||
            (!hasSelection() && getCursorPosition().getColumn() == 0);
      
      if (hasWhitespaceBefore)
         insertCode("%>% ", false);
      else
         insertCode(" %>% ", false);
   }
   
   private boolean shouldIndentOnPaste()
   {
      if (fileType_ == null || !fileType_.canAutoIndent())
         return false;
      
      // if the user has requested reindent on paste, then we reindent
      boolean indentPref = RStudioGinjector.INSTANCE.getUserPrefs().reindentOnPaste().getValue();
      if (indentPref)
         return true;
      
      // if the user has explicitly executed a paste with indent command, we reindent
      if (activeEditEventType_ == EditEvent.TYPE_PASTE_WITH_INDENT)
         return true;
      
      // finally, infer based on whether shift key is down
      return keyboard_.isShiftKeyDown();
   }

   private void indentPastedRange(Range range)
   {
      String firstLinePrefix = getSession().getTextRange(
            Range.fromPoints(Position.create(range.getStart().getRow(), 0),
                             range.getStart()));

      if (firstLinePrefix.trim().length() != 0)
      {
         Position newStart = Position.create(range.getStart().getRow() + 1, 0);
         if (newStart.compareTo(range.getEnd()) >= 0)
            return;

         range = Range.fromPoints(newStart, range.getEnd());
      }

      getSession().reindent(range);
   }
   
   public AceCommandManager getCommandManager()
   {
      return getWidget().getEditor().getCommandManager();
   }
   
   public void setEditorCommandBinding(String id, List<KeySequence> keys)
   {
      getWidget().getEditor().getCommandManager().rebindCommand(id, keys);
   }
   
   public void resetCommands()
   {
      AceCommandManager manager = AceCommandManager.create();
      JsObject commands = manager.getCommands();
      for (String key : JsUtil.asIterable(commands.keys()))
      {
         AceCommand command = commands.getObject(key);
         getWidget().getEditor().getCommandManager().addCommand(command);
      }
   }

   @Inject
   void initialize(CodeToolsServerOperations server,
                   UserPrefs uiPrefs,
                   CollabEditor collab,
                   KeyboardTracker keyboard,
                   Commands commands,
                   EventBus events)
   {
      server_ = server;
      userPrefs_ = uiPrefs;
      collab_ = collab;
      keyboard_ = keyboard;
      commands_ = commands;
      events_ = events;
   }

   public TextFileType getFileType()
   {
      return fileType_;
   }

   public void setFileType(TextFileType fileType)
   {
      setFileType(fileType, false);
   }

   public void setFileType(TextFileType fileType, boolean suppressCompletion)
   {
      fileType_ = fileType;
      updateLanguage(suppressCompletion);
   }

   public void setFileType(TextFileType fileType,
                           CompletionManager completionManager)
   {
      fileType_ = fileType;
      updateLanguage(completionManager, null);
   }

   @Override
   public void setRnwCompletionContext(RnwCompletionContext rnwContext)
   {
      rnwContext_ = rnwContext;
   }

   @Override
   public void setCppCompletionContext(CppCompletionContext cppContext)
   {
      cppContext_ = cppContext;
   }

   @Override
   public void setRCompletionContext(CompletionContext context)
   {
      context_ = context;
   }

   private void updateLanguage(boolean suppressCompletion)
   {
      if (fileType_ == null)
         return;
      
      CompletionManager completionManager;
      
      if (!suppressCompletion && fileType_.getEditorLanguage().useRCompletion())
      {
         // GWT throws an exception if we bind Ace using 'AceEditor.this' below
         // so work around that by just creating a final reference and use that
         final AceEditor editor = this;
         
         completionManager = new DelegatingCompletionManager(this, context_)
         {
            @Override
            protected void initialize(Map<Mode, CompletionManager> managers)
            {
               // R completion manager
               if (fileType_.isR() || fileType_.isRmd() || fileType_.isCpp() ||
                   fileType_.isRhtml() || fileType_.isRnw())
               {
                  managers.put(DocumentMode.Mode.R, new RCompletionManager(
                        editor,
                        editor,
                        new CompletionPopupPanel(),
                        server_,
                        new Filter(),
                        context_,
                        fileType_.canExecuteChunks() ? rnwContext_ : null,
                           editor,
                           false));
               }
               
               // Markdown completion manager
               if (fileType_.isMarkdown() || fileType_.isRmd())
               {
                  managers.put(DocumentMode.Mode.MARKDOWN, new MarkdownCompletionManager(
                        editor,
                        new CompletionPopupPanel(),
                        server_,
                        context_));
               }
               
               // Python completion manager
               if (fileType_.isPython() || fileType_.isRmd())
               {
                  managers.put(DocumentMode.Mode.PYTHON, new PythonCompletionManager(
                        editor,
                        new CompletionPopupPanel(),
                        server_,
                        context_));
               }
               
               // C++ completion manager
               if (fileType_.isC())
               {
                  managers.put(DocumentMode.Mode.C_CPP, new CppCompletionManager(
                        editor,
                        new Filter(),
                        cppContext_));
               }
               
               // SQL completion manager
               if (fileType_.isSql() || fileType_.isRmd())
               {
                  managers.put(DocumentMode.Mode.SQL, new SqlCompletionManager(
                        editor,
                        new CompletionPopupPanel(),
                        server_,
                        context_));
               }
               
               // Stan completion manager
               if (fileType_.isStan() || fileType_.isRmd())
               {
                  managers.put(DocumentMode.Mode.STAN, new StanCompletionManager(
                        editor,
                        new CompletionPopupPanel(),
                        server_,
                        context_));
               }
            }
         };
      }
      else
      {
         completionManager = new NullCompletionManager();
      }
      
      ScopeTreeManager scopeTreeManager = null;
      
      if (fileType_.isStan())
      {
         scopeTreeManager = new StanScopeTreeManager(this);
      }

      updateLanguage(completionManager, scopeTreeManager);
   }

   private void updateLanguage(CompletionManager completionManager,
                               ScopeTreeManager scopeTreeManager)
   {
      clearLint();
      if (fileType_ == null)
         return;

      if (completionManager_ != null)
      {
         completionManager_.detach();
         completionManager_ = null;
      }
      
      if (scopes_ != null)
      {
         scopes_.detach();
         scopes_ = null;
      }
      
      completionManager_ = completionManager;
      scopes_ = scopeTreeManager;

      updateKeyboardHandlers();
      syncCompletionPrefs();
      syncDiagnosticsPrefs();
      
      snippets_.ensureSnippetsLoaded();
      getSession().setEditorMode(
            fileType_.getEditorLanguage().getParserName(),
            false);
      
      handlers_.fireEvent(new EditorModeChangedEvent(getModeId()));

      syncWrapLimit();
   }

   @Override
   public void syncCompletionPrefs()
   {
      if (fileType_ == null)
         return;

      boolean enabled = fileType_.getEditorLanguage().useAceLanguageTools();
      boolean live = userPrefs_.codeCompletionOther().getValue() ==
                                       UserPrefs.CODE_COMPLETION_OTHER_ALWAYS;
      int characterThreshold = userPrefs_.codeCompletionCharacters().getValue();
      int delay = userPrefs_.codeCompletionDelay().getValue();
      
      widget_.getEditor().setCompletionOptions(
            enabled,
            userPrefs_.enableSnippets().getValue(),
            live,
            characterThreshold,
            delay);
      
   }

   @Override
   public void syncDiagnosticsPrefs()
   {
      if (fileType_ == null)
         return;

      boolean useWorker = userPrefs_.showDiagnosticsOther().getValue() &&
            fileType_.getEditorLanguage().useAceLanguageTools();

      getSession().setUseWorker(useWorker);
      getSession().setWorkerTimeout(
            userPrefs_.backgroundDiagnosticsDelayMs().getValue());
   }

   private void syncWrapLimit()
   {
      // bail if there is no filetype yet
      if (fileType_ == null)
         return;

      // We originally observed that large word-wrapped documents
      // would cause Chrome on Linux to freeze (bug #3207), eventually
      // running of of memory. Running the profiler indicated that the
      // time was being spent inside wrap width calculations in Ace.
      // Looking at the Ace bug database there were other wrapping problems
      // that were solvable by changing the wrap mode from "free" to a
      // specific range. So, for Chrome on Linux we started syncing the
      // wrap limit to the user-specified margin width.
      //
      // Unfortunately, this caused another problem whereby the ace
      // horizontal scrollbar would show up over the top of the editor
      // and the console (bug #3428). We tried reverting the fix to
      // #3207 and sure enough this solved the horizontal scrollbar
      // problem _and_ no longer froze Chrome (so perhaps there was a
      // bug in Chrome).
      //
      // In the meantime we added user pref to soft wrap to the margin
      // column, essentially allowing users to opt-in to the behavior
      // we used to fix the bug. So the net is:
      //
      // (1) To fix the horizontal scrollbar problem we reverted
      //     the wrap mode behavior we added from Chrome (under the
      //     assumption that the issue has been fixed in Chrome)
      //
      // (2) We added another check for desktop mode (since we saw
      //     the problem in both Chrome and Safari) to prevent the
      //     application of the problematic wrap mode setting.
      //
      // Perhaps there is an ace issue here as well, so the next time
      // we sync to Ace tip we should see if we can bring back the
      // wrapping option for Chrome (note the repro for this
      // is having a soft-wrapping source document in the editor that
      // exceed the horizontal threshold)

      // NOTE: we no longer do this at all since we observed the
      // scrollbar problem on desktop as well
   }

   private void updateKeyboardHandlers()
   {
      // clear out existing editor handlers (they will be refreshed if necessary)
      for (HandlerRegistration handler : editorEventListeners_)
         if (handler != null)
            handler.removeHandler();
      editorEventListeners_.clear();
         
      // save and restore Vim marks as they can be lost when refreshing
      // the keyboard handlers. this is necessary as keyboard handlers are
      // regenerated on each document save, and clearing the Vim handler will
      // clear any local Vim state.
      JsMap<Position> marks = JsMap.create().cast();
      if (useVimMode_)
         marks = widget_.getEditor().getMarks();
      
      // create a keyboard previewer for our special hooks
      AceKeyboardPreviewer previewer = new AceKeyboardPreviewer(completionManager_);

      // set default key handler
      if (useVimMode_)
         widget_.getEditor().setKeyboardHandler(KeyboardHandler.vim());
      else if (useEmacsKeybindings_)
         widget_.getEditor().setKeyboardHandler(KeyboardHandler.emacs());
      else
         widget_.getEditor().setKeyboardHandler(null);
      
      // add the previewer
      widget_.getEditor().addKeyboardHandler(previewer.getKeyboardHandler());
      
      // Listen for command execution
      editorEventListeners_.add(AceEditorNative.addEventListener(
            widget_.getEditor().getCommandManager(),
            "afterExec",
            (CommandWithArg<JavaScriptObject>) event -> events_.fireEvent(new AceAfterCommandExecutedEvent(event))));
      
      // Listen for keyboard activity
      editorEventListeners_.add(AceEditorNative.addEventListener(
            widget_.getEditor(),
            "keyboardActivity",
            (CommandWithArg<JavaScriptObject>) event -> events_.fireEvent(new AceKeyboardActivityEvent(event))));

      if (useVimMode_)
         widget_.getEditor().setMarks(marks);
   }
   
   public String getCode()
   {
      return getSession().getValue();
   }

   public void setCode(String code, boolean preserveCursorPosition)
   {
      // Calling setCode("", false) while the editor contains multiple lines of
      // content causes bug 2928: Flickering console when typing. Empirically,
      // first setting code to a single line of content and then clearing it,
      // seems to correct this problem.
      if (StringUtil.isNullOrEmpty(code))
         doSetCode(" ", preserveCursorPosition);

      doSetCode(code, preserveCursorPosition);
   }

   private void doSetCode(String code, boolean preserveCursorPosition)
   {
      // Filter out Escape characters that might have snuck in from an old
      // bug in 0.95. We can choose to remove this when 0.95 ships, hopefully
      // any documents that would be affected by this will be gone by then.
      code = code.replaceAll("\u001B", "");
      
      // Normalize newlines -- convert all of '\r', '\r\n', '\n\r' to '\n'.
      final String normalizedCode = StringUtil.normalizeNewLines(code);
      
      final AceEditorNative ed = widget_.getEditor();

      if (preserveCursorPosition)
      {
         withPreservedCursorPosition(() -> {
            // Setting the value directly on the document prevents undo/redo
            // stack from being blown away
            widget_.getEditor().getSession().getDocument().setValue(normalizedCode);
         });
      }
      else
      {
         ed.getSession().setValue(normalizedCode);
         ed.getSession().getSelection().moveCursorTo(0, 0, false);
      }
   }
   
   private void withPreservedCursorPosition(Runnable runnable)
   {
      final AceEditorNative ed = widget_.getEditor();
      
      final Position cursorPos;
      final int scrollTop, scrollLeft;

      cursorPos = ed.getSession().getSelection().getCursor();
      scrollTop = ed.getRenderer().getScrollTop();
      scrollLeft = ed.getRenderer().getScrollLeft();

      runnable.run();

      ed.getSession().getSelection().moveCursorTo(cursorPos.getRow(),
                                                  cursorPos.getColumn(),
                                                  false);
      scrollToY(scrollTop, 0);
      scrollToX(scrollLeft);
      Scheduler.get().scheduleDeferred(() ->
      {
         scrollToY(scrollTop, 0);
         scrollToX(scrollLeft);
      });
   }

   public int getScrollLeft()
   {
      return widget_.getEditor().getRenderer().getScrollLeft();
   }

   public void scrollToX(int x)
   {
      widget_.getEditor().getRenderer().scrollToX(x);
   }

   public int getScrollTop()
   {
      return widget_.getEditor().getRenderer().getScrollTop();
   }

   public void scrollToY(int y, int animateMs)
   {
      // cancel any existing scroll animation
      if (scrollAnimator_ != null)
         scrollAnimator_.complete();
      
      if (animateMs == 0)
         widget_.getEditor().getRenderer().scrollToY(y);
      else
         scrollAnimator_ = new ScrollAnimator(y, animateMs);

      fireEvent(new ScrollYEvent(Position.create(getFirstVisibleRow(), 0)));
   }

   public void insertCode(String code)
   {
      insertCode(code, false);
   }

   public void insertCode(String code, boolean blockMode)
   {
      widget_.getEditor().insert(StringUtil.normalizeNewLines(code));
   }
   
   public void applyChanges(TextChange[] changes)
   {
      applyChanges(changes, false);
   }
   
   public void applyChanges(TextChange[] changes, boolean preserveCursorPosition)
   {
      // special case for a single change that neither adds nor removes
      // (identity operation). we don't feed this through the code below
      // because a single non-mutating change will result in a selection
      // at the beginning of the file
      if (changes.length == 1 && changes[0].type == TextChange.Type.Equal)
         return;
      
      // application of changes (will run this below either with or w/o 
      // preserving the cursor position)
      Runnable applyChanges = () -> {
         // alias apis
         AceEditorNative editor = widget_.getEditor();
         EditSession session = editor.getSession();
         Selection selection = session.getSelection();
         AceCommandManager commandManager = editor.getCommandManager();
         
         // function to advance the selection
         Consumer<Integer> advanceSelection = (Integer charsLeft) -> {
            Position startPos = selection.getCursor();
            Position newPos = advancePosition(session, startPos, charsLeft);
            selection.moveCursorTo(newPos.getRow(), newPos.getColumn(), false);
         };
         
         // if we have at least 1 change then set the cursor location 
         // to the beginning of the file
         if (changes.length > 0)
            selection.moveCursorTo(0, 0, false);      
         
         // process changes
         for (int i = 0; i<changes.length; i++) 
         {
            // get change and length
            TextChange change = changes[i];
            int length = change.value.length();
            
            // insert text (selection will be advanced to the end of the string)
            if (change.type == TextChange.Type.Insert)
            {
               if (change.value.length() > 0)
                  commandManager.exec("insertstring", editor, change.value);
            }
            
            // remove text -- we advance past it and then use the "backspace"
            // command b/c ace gives nicer undo behavior for this action (compared
            // to executing the "del" command)
            else if (change.type == TextChange.Type.Delete)
            {
               Range newRange = selection.getRange();
               newRange.setEnd(advancePosition(session, selection.getCursor(), length));
               selection.setSelectionRange(newRange);
               commandManager.exec("backspace", editor);
            }
            
            // advance selection (unless this is the last change, in which 
            // case it just represents advancing to the end of the file)
            else if (i != (changes.length-1))
            {
               advanceSelection.accept(length);
            } 
         }  
      };
      
      if (preserveCursorPosition)
         withPreservedCursorPosition(applyChanges);
      else
         applyChanges.run();
   }
   
   private static Position advancePosition(EditSession session, Position startPos, Integer chars)
   {
      // iterate through rows until we've consumed all the chars
      int row = startPos.getRow();
      int col = startPos.getColumn();
      while (row < session.getLength()) {
         
         // how many chars left in the current column?
         String line = session.getLine(row);
         // +1 is for the newline
         int charsLeftInLine = line.length() + 1 - col;
         
         // is the number of chars we still need to consume lte
         // the number of charsLeft?
         if (chars < charsLeftInLine) 
         {
            col = col + chars;
            break;
         }
         else
         {
            chars -= charsLeftInLine;
            col = 0;
            row++;
         }
      }
      
      return Position.create(row, col);
   }
   
   @Override
   public Position positionFromIndex(int index)
   {
      EditSession session = widget_.getEditor().getSession();
      return advancePosition(session, Position.create(0,0), index);
   }
   
   @Override
   public int indexFromPosition(Position position)
   {
      EditSession session = widget_.getEditor().getSession();
      int index = 0;
      int row = 0;
      while (row < position.getRow())
      {
         index += (session.getLine(row).length() + 1); // +1 for newline
         row++;
      }
      index += position.getColumn();
      return index;
   }
   

   public String getCode(Position start, Position end)
   {
      return getSession().getTextRange(Range.fromPoints(start, end));
   }

   @Override
   public InputEditorSelection search(String needle,
                                      boolean backwards,
                                      boolean wrap,
                                      boolean caseSensitive,
                                      boolean wholeWord,
                                      Position start,
                                      Range range,
                                      boolean regexpMode)
   {
      Search search = Search.create(needle,
                                    backwards,
                                    wrap,
                                    caseSensitive,
                                    wholeWord,
                                    start,
                                    range,
                                    regexpMode);

      Range resultRange = search.find(getSession());
      if (resultRange != null)
      {
         return createSelection(resultRange.getStart(), resultRange.getEnd());
      }
      else
      {
         return null;
      }
   }
   
   @Override
   public void quickAddNext()
   {
      if (getNativeSelection().isEmpty())
      {
         getNativeSelection().selectWord();
         return;
      }
      
      String needle = getSelectionValue();
      Search search = Search.create(
            needle,
            false,
            true,
            true,
            false,
            getCursorPosition(),
            null,
            false);
      
      Range range = search.find(getSession());
      if (range == null)
         return;
      
      getNativeSelection().addRange(range, false);
      centerSelection();
   }

   @Override
   public void insertCode(InputEditorPosition position, String content)
   {
      insertCode(selectionToPosition(position), content);
   }

   public void insertCode(Position position, String content)
   {
      getSession().insert(position, content);
   }

   @Override
   public String getCode(InputEditorSelection selection)
   {
      return getCode(((AceInputEditorPosition)selection.getStart()).getValue(),
                     ((AceInputEditorPosition)selection.getEnd()).getValue());
   }
   
   @Override
   public JsArrayString getLines()
   {
      return getLines(0, getSession().getLength());
   }
   
   @Override
   public JsArrayString getLines(int startRow, int endRow)
   {
      return getSession().getLines(startRow, endRow);
   }
   
   public void focus()
   {
      widget_.getEditor().focus();
   }

   public boolean isFocused()
   {
      return widget_.getEditor().isFocused();
   }


   public void codeCompletion()
   {
      completionManager_.codeCompletion();
   }

   public void goToHelp()
   {
      completionManager_.goToHelp();
   }

   public void goToDefinition()
   {
      completionManager_.goToDefinition();
   }

   class PrintIFrame extends DynamicIFrame
   {
      public PrintIFrame(String code, double fontSize)
      {
         super("Print Frame");
         code_ = code;
         fontSize_ = fontSize;

         getElement().getStyle().setPosition(com.google.gwt.dom.client.Style.Position.ABSOLUTE);
         getElement().getStyle().setLeft(-5000, Unit.PX);
      }

      @Override
      protected void onFrameLoaded()
      {
         Document doc = getDocument();
         PreElement pre = doc.createPreElement();
         pre.setInnerText(code_);
         pre.getStyle().setProperty("whiteSpace", "pre-wrap");
         pre.getStyle().setFontSize(fontSize_, Unit.PT);
         doc.getBody().appendChild(pre);

         getWindow().print();

         // Bug 1224: ace: print from source causes inability to reconnect
         // This was caused by the iframe being removed from the document too
         // quickly after the print job was sent. As a result, attempting to
         // navigate away from the page at any point afterwards would result
         // in the error "Document cannot change while printing or in Print
         // Preview". The only thing you could do is close the browser tab.
         // By inserting a 5-minute delay hopefully Firefox would be done with
         // whatever print related operations are important.
         Scheduler.get().scheduleFixedDelay(() ->
         {
            PrintIFrame.this.removeFromParent();
            return false;
         }, 1000 * 60 * 5);
      }

      private final String code_;
      private final double fontSize_;
   }

   public void print()
   {
      if (Desktop.hasDesktopFrame())
      {
         // the desktop frame prints the code directly
         Desktop.getFrame().printText(StringUtil.notNull(getCode()));
      }
      else
      {
         // in server mode, we render the code to an IFrame and then print
         // the frame using the browser
         PrintIFrame printIFrame = new PrintIFrame(
               getCode(),
               RStudioGinjector.INSTANCE.getUserPrefs().fontSizePoints().getValue());
         RootPanel.get().add(printIFrame);
      }
   }

   public String getText()
   {
      return getSession().getLine(
            getSession().getSelection().getCursor().getRow());
   }

   public void setText(String string)
   {
      setCode(string, false);
      getSession().getSelection().moveCursorFileEnd();
   }

   public boolean hasSelection()
   {
      return !getSession().getSelection().getRange().isEmpty();
   }

   public final Selection getNativeSelection() {
      return widget_.getEditor().getSession().getSelection();
   }

   public InputEditorSelection getSelection()
   {
      Range selection = getSession().getSelection().getRange();
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), selection.getStart()),
            new AceInputEditorPosition(getSession(), selection.getEnd()));

   }

   public String getSelectionValue()
   {
      return getSession().getTextRange(
            getSession().getSelection().getRange());
   }

   public Position getSelectionStart()
   {
      return getSession().getSelection().getRange().getStart();
   }

   public Position getSelectionEnd()
   {
      return getSession().getSelection().getRange().getEnd();
   }

   @Override
   public Range getSelectionRange()
   {
      return Range.fromPoints(getSelectionStart(), getSelectionEnd());
   }

   @Override
   public void setSelectionRange(Range range)
   {
      getSession().getSelection().setSelectionRange(range);
   }
   
   public void setSelectionRanges(JsArray<Range> ranges)
   {
      int n = ranges.length();
      if (n == 0)
         return;
      
      if (vim_.isActive())
         vim_.exitVisualMode();
      
      setSelectionRange(ranges.get(0));
      for (int i = 1; i < n; i++)
         getNativeSelection().addRange(ranges.get(i), false);
      
      scrollCursorIntoViewIfNecessary();
   }
   
   public int getLength(int row)
   {
      return getSession().getDocument().getLine(row).length();
   }

   public int getRowCount()
   {
      return getSession().getDocument().getLength();
   }

   @Override
   public int getPixelWidth()
   {
      Element[] content = DomUtils.getElementsByClassName(
            widget_.getElement(), "ace_content");
      if (content.length < 1)
         return widget_.getElement().getOffsetWidth();
      else
         return content[0].getOffsetWidth();
   }

   public String getLine(int row)
   {
      return getSession().getLine(row);
   }
   
   @Override
   public Position getDocumentEnd()
   {
      int lastRow = getRowCount() - 1;
      return Position.create(lastRow, getLength(lastRow));
   }
   
   @Override
   public void setInsertMatching(boolean value)
   {
      widget_.getEditor().setInsertMatching(value);
   }
   
   @Override
   public void setSurroundSelectionPref(String value)
   {
      widget_.getEditor().setSurroundSelectionPref(value);
   }

   @Override
   public InputEditorSelection createSelection(Position pos1, Position pos2)
   {
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), pos1),
            new AceInputEditorPosition(getSession(), pos2));
   }

   @Override
   public Position selectionToPosition(InputEditorPosition pos)
   {
      // HACK: This cast is gross, InputEditorPosition should just become
      // AceInputEditorPosition
      return Position.create((Integer) pos.getLine(), pos.getPosition());
   }

   @Override
   public InputEditorPosition createInputEditorPosition(Position pos)
   {
      return new AceInputEditorPosition(getSession(), pos);
   }

   @Override
   public Iterable<Range> getWords(TokenPredicate tokenPredicate,
                                   CharClassifier charClassifier,
                                   Position start,
                                   Position end)
   {
      return new WordIterable(getSession(),
                              tokenPredicate,
                              charClassifier,
                              start,
                              end);
   }
   
   @Override
   public SpellingDoc getSpellingDoc()
   {
      // detach anchors on dispose
      ArrayList<org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor> 
        anchors = new ArrayList<org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor>();
      
      return new SpellingDoc() {

         
         @Override
         public Iterable<WordRange> getWords(int start, int end)
         {
            return new Iterable<WordRange>() {

               @Override
               public Iterator<WordRange> iterator()
               {
                  // get underlying iterator
                  Iterator<Range> ranges = AceEditor.this.getWords(
                        fileType_.getSpellCheckTokenPredicate(),
                        fileType_.getCharPredicate(),
                        positionFromIndex(start),
                        end != -1 ? positionFromIndex(end) : null).iterator();
                  
                  // shim it on to spelling doc iterator
                  return new Iterator<WordRange>() {
                     
                     @Override
                     public boolean hasNext()
                     {
                        return ranges.hasNext();
                     }

                     @Override
                     public WordRange next()
                     {
                        Range range = ranges.next(); 
                        return new WordRange(
                           indexFromPosition(range.getStart()),
                           indexFromPosition(range.getEnd())
                        ); 
                     }
                  };
               }
            };
         }

         @Override
         public Anchor createAnchor(int position)
         {
            // create ace anchor
            org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor anchor =
              AceEditor.this.createAnchor(positionFromIndex(position));
            
            // track anchors for disposal
            anchors.add(anchor);
            
            // shim on spelling doc anchor
            return new Anchor() {
               @Override
               public int getPosition()
               {
                  return indexFromPosition(anchor.getPosition());
               }
            };
         }
         
         @Override
         public boolean shouldCheck(WordRange wordRange)
         {
            Range range = toAceRange(wordRange);
            
            // Don't spellcheck yaml
            Scope s = getScopeAtPosition(range.getStart());
            if (s != null && s.isYaml())
               return false;

            // This will capture all braced text in a way that the
            // highlight rules don't and shouldn't.
            String word = getTextForRange(range);
            int start = range.getStart().getColumn();
            int end = start + word.length();
            String line = getLine(range.getStart().getRow());
            Pattern p =  Pattern.create("\\{[^\\{\\}]*" + word + "[^\\{\\}]*\\}");
            Match m = p.match(line, 0);
            while (m != null)
            {
               // ensure that the match is the specific word we're looking
               // at to fix edge cases such as {asdf}asdf
               if (m.getIndex() < start &&
                   (m.getIndex() + m.getValue().length()) > end)
                  return false;

               m = m.nextMatch();
            }

            return true;
         }

         @Override
         public void setSelection(WordRange wordRange)
         {
            setSelectionRange(toAceRange(wordRange)); 
         }

         @Override
         public String getText(WordRange wordRange)
         {
            return getTextForRange(toAceRange(wordRange));
         }

         @Override
         public int getCursorPosition()
         {
            return indexFromPosition(AceEditor.this.getCursorPosition());
         }

         @Override
         public void replaceSelection(String text)
         {
            AceEditor.this.replaceSelection(text);
         }

         @Override
         public int getSelectionStart()
         {
            return indexFromPosition(AceEditor.this.getSelectionStart());
         }

         @Override
         public int getSelectionEnd()
         {
            return indexFromPosition(AceEditor.this.getSelectionEnd());
         }

         @Override
         public Rectangle getCursorBounds()
         {
            return AceEditor.this.getCursorBounds();
         }

         @Override
         public void moveCursorNearTop()
         {
            AceEditor.this.moveCursorNearTop();
         }
         
         private Range toAceRange(WordRange wordRange)
         {
            Position startPos = positionFromIndex(wordRange.start);
            Position endPos = positionFromIndex(wordRange.end);
            return Range.create(
               startPos.getRow(), 
               startPos.getColumn(), 
               endPos.getRow(), 
               endPos.getColumn()
            );
         }

         @Override
         public void dispose()
         {
            anchors.forEach((anchor) -> { anchor.detach(); });
         }
      };
   }
   
 


   @Override
   public String getTextForRange(Range range)
   {
      return getSession().getTextRange(range);
   }

   @Override
   public Anchor createAnchor(Position pos)
   {
      return Anchor.createAnchor(getSession().getDocument(),
                                 pos.getRow(),
                                 pos.getColumn());
   }

   private void fixVerticalOffsetBug()
   {
      widget_.getEditor().getRenderer().fixVerticalOffsetBug();
   }

   @Override
   public String debug_getDocumentDump()
   {
      return widget_.getEditor().getSession().getDocument().getDocumentDump();
   }

   @Override
   public void debug_setSessionValueDirectly(String s)
   {
      widget_.getEditor().getSession().setValue(s);
   }

   public void setSelection(InputEditorSelection selection)
   {
      AceInputEditorPosition start = (AceInputEditorPosition)selection.getStart();
      AceInputEditorPosition end = (AceInputEditorPosition)selection.getEnd();
      getSession().getSelection().setSelectionRange(Range.fromPoints(
            start.getValue(), end.getValue()));
   }

   public Rectangle getCursorBounds()
   {
      Range range = getSession().getSelection().getRange();
      return toScreenCoordinates(range);
   }

   public Rectangle toScreenCoordinates(Range range)
   {
      Renderer renderer = widget_.getEditor().getRenderer();
      ScreenCoordinates start = renderer.textToScreenCoordinates(
                  range.getStart().getRow(),
                  range.getStart().getColumn());
      ScreenCoordinates end = renderer.textToScreenCoordinates(
                  range.getEnd().getRow(),
                  range.getEnd().getColumn());
      return new Rectangle(start.getPageX(),
                           start.getPageY(),
                           end.getPageX() - start.getPageX(),
                           renderer.getLineHeight());
   }

   public Position toDocumentPosition(ScreenCoordinates coordinates)
   {
      return widget_.getEditor().getRenderer().screenToTextCoordinates(
            coordinates.getPageX(),
            coordinates.getPageY());
   }

   public Range toDocumentRange(Rectangle rectangle)
   {
      Renderer renderer = widget_.getEditor().getRenderer();
      return Range.fromPoints(
            renderer.screenToTextCoordinates(rectangle.getLeft(), rectangle.getTop()),
            renderer.screenToTextCoordinates(rectangle.getRight(), rectangle.getBottom()));
   }
   
   @Override
   public Rectangle getPositionBounds(Position position)
   {
      Renderer renderer = widget_.getEditor().getRenderer();
      ScreenCoordinates start = renderer.textToScreenCoordinates(
            position.getRow(),
            position.getColumn());

      return new Rectangle(start.getPageX(), start.getPageY(),
                           (int) Math.round(renderer.getCharacterWidth()),
                           (int) (renderer.getLineHeight() * 0.8));
   }
   
   @Override
   public Rectangle getRangeBounds(Range range)
   {
      range = Range.toOrientedRange(range);
      
      Renderer renderer = widget_.getEditor().getRenderer();
      if (!range.isMultiLine())
      {
         ScreenCoordinates start = documentPositionToScreenCoordinates(range.getStart());
         ScreenCoordinates end   = documentPositionToScreenCoordinates(range.getEnd());
         
         int width  = (end.getPageX() - start.getPageX()) + (int) renderer.getCharacterWidth();
         int height = (end.getPageY() - start.getPageY()) + (int) renderer.getLineHeight();
         
         return new Rectangle(start.getPageX(), start.getPageY(), width, height);
      }
      
      Position startPos = range.getStart();
      Position endPos   = range.getEnd();
      int startRow = startPos.getRow();
      int endRow   = endPos.getRow();
      
      // figure out top left coordinates
      ScreenCoordinates topLeft = documentPositionToScreenCoordinates(Position.create(startRow, 0));
      
      // figure out bottom right coordinates (need to walk rows to figure out longest line)
      ScreenCoordinates bottomRight = documentPositionToScreenCoordinates(Position.create(endPos));
      for (int row = startRow; row <= endRow; row++)
      {
         Position rowEndPos = Position.create(row, getLength(row));
         ScreenCoordinates coords = documentPositionToScreenCoordinates(rowEndPos);
         if (coords.getPageX() > bottomRight.getPageX())
            bottomRight = ScreenCoordinates.create(coords.getPageX(), bottomRight.getPageY());
      }
      
      // construct resulting range
      int width  = (bottomRight.getPageX() - topLeft.getPageX()) + (int) renderer.getCharacterWidth();
      int height = (bottomRight.getPageY() - topLeft.getPageY()) + (int) renderer.getLineHeight();
      return new Rectangle(topLeft.getPageX(), topLeft.getPageY(), width, height);
   }
   
   @Override
   public Rectangle getPositionBounds(InputEditorPosition position)
   {
      Position pos = ((AceInputEditorPosition) position).getValue();
      return getPositionBounds(pos);
   }

   public Rectangle getBounds()
   {
      return new Rectangle(
            widget_.getAbsoluteLeft(),
            widget_.getAbsoluteTop(),
            widget_.getOffsetWidth(),
            widget_.getOffsetHeight());
   }

   public void setFocus(boolean focused)
   {
      if (focused)
         widget_.getEditor().focus();
      else
         widget_.getEditor().blur();
   }

   public void replaceRange(Range range, String text) {
      getSession().replace(range, text);
   }

   public String replaceSelection(String value, boolean collapseSelection)
   {
      Selection selection = getSession().getSelection();
      String oldValue = getSession().getTextRange(selection.getRange());

      replaceSelection(value);

      if (collapseSelection)
      {
         collapseSelection(false);
      }

      return oldValue;
   }

   public boolean isSelectionCollapsed()
   {
      return getSession().getSelection().isEmpty();
   }

   public boolean isCursorAtEnd()
   {
      int lastRow = getRowCount() - 1;
      Position cursorPos = getCursorPosition();
      return cursorPos.compareTo(Position.create(lastRow,
                                                 getLength(lastRow))) == 0;
   }

   public void clear()
   {
      setCode("", false);
   }
   
   public boolean inMultiSelectMode()
   {
      return widget_.getEditor().inMultiSelectMode();
   }
   
   public void exitMultiSelectMode()
   {
      widget_.getEditor().exitMultiSelectMode();
   }
   
   public void clearSelection()
   {
      widget_.getEditor().clearSelection();
   }

   public void collapseSelection(boolean collapseToStart)
   {
      Selection selection = getSession().getSelection();
      Range rng = selection.getRange();
      Position pos = collapseToStart ? rng.getStart() : rng.getEnd();
      selection.setSelectionRange(Range.fromPoints(pos, pos));
   }

   public InputEditorSelection getStart()
   {
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), Position.create(0, 0)));
   }

   public InputEditorSelection getEnd()
   {
      EditSession session = getSession();
      int rows = session.getLength();
      Position end = Position.create(rows, session.getLine(rows).length());
      return new InputEditorSelection(new AceInputEditorPosition(session, end));
   }

   public String getCurrentLine()
   {
      int row = getSession().getSelection().getRange().getStart().getRow();
      return getSession().getLine(row);
   }

   public char getCharacterAtCursor()
   {
      Position cursorPos = getCursorPosition();
      int column = cursorPos.getColumn();
      String line = getLine(cursorPos.getRow());
      if (column == line.length())
         return '\0';

      return StringUtil.charAt(line, column);
   }

   public char getCharacterBeforeCursor()
   {
      Position cursorPos = getCursorPosition();
      int column = cursorPos.getColumn();
      if (column == 0)
         return '\0';

      String line = getLine(cursorPos.getRow());
      return StringUtil.charAt(line, column - 1);
   }


   public String getCurrentLineUpToCursor()
   {
      return getCurrentLine().substring(0, getCursorPosition().getColumn());
   }

   public int getCurrentLineNum()
   {
      Position pos = getCursorPosition();
      return getSession().documentToScreenRow(pos);
   }

   public int getCurrentLineCount()
   {
      return getSession().getScreenLength();
   }

   @Override
   public String getLanguageMode(Position position)
   {
      return getSession().getMode().getLanguageMode(position);
   }
   
   @Override
   public String getModeId()
   {
     return getSession().getMode().getId();
   }

   public void replaceCode(String code)
   {
      int endRow, endCol;

      endRow = getSession().getLength() - 1;
      if (endRow < 0)
      {
         endRow = 0;
         endCol = 0;
      }
      else
      {
         endCol = getSession().getLine(endRow).length();
      }

      Range range = Range.fromPoints(Position.create(0, 0),
                                     Position.create(endRow, endCol));
      getSession().replace(range, code);
   }

   public void replaceSelection(String code)
   {
      code = StringUtil.normalizeNewLines(code);
      Range selRange = getSession().getSelection().getRange();
      Position position = getSession().replace(selRange, code);
      Range range = Range.fromPoints(selRange.getStart(), position);
      getSession().getSelection().setSelectionRange(range);
      if (isEmacsModeOn()) clearEmacsMark();
   }

   public boolean moveSelectionToNextLine(boolean skipBlankLines)
   {
      int curRow = getSession().getSelection().getCursor().getRow();
      while (++curRow < getSession().getLength())
      {
         String line = getSession().getLine(curRow);
         Pattern pattern = Pattern.create("[^\\s]");
         Match match = pattern.match(line, 0);
         if (skipBlankLines && match == null)
            continue;
         int col =  (match != null) ? match.getIndex() : 0;
         getSession().getSelection().moveCursorTo(curRow, col, false);
         getSession().unfold(curRow, true);
         scrollCursorIntoViewIfNecessary();
         return true;
      }
      return false;
   }

   @Override
   public boolean moveSelectionToBlankLine()
   {
      int curRow = getSession().getSelection().getCursor().getRow();

      // if the current row is the last row then insert a new row
      if (curRow == (getSession().getLength() - 1))
      {
         int rowLen = getSession().getLine(curRow).length();
         getSession().getSelection().moveCursorTo(curRow, rowLen, false);
         insertCode("\n");
      }

      while (curRow < getSession().getLength())
      {
         String line = getSession().getLine(curRow).trim();
         if (line.length() == 0)
         {
            getSession().getSelection().moveCursorTo(curRow, 0, false);
            getSession().unfold(curRow, true);
            return true;
         }

         curRow++;
      }
      return false;
   }
   
   @Override
   public void expandSelection()
   {
      widget_.getEditor().expandSelection();
   }
   
   @Override
   public void shrinkSelection()
   {
      widget_.getEditor().shrinkSelection();
   }
   
   @Override
   public void expandRaggedSelection()
   {
      if (!inMultiSelectMode())
         return;
      
      // TODO: It looks like we need to use an alternative API when
      // using Vim mode.
      if (isVimModeOn())
         return;
      
      boolean hasSelection = hasSelection();
      
      Range[] ranges =
            widget_.getEditor().getSession().getSelection().getAllRanges();
      
      // Get the maximum columns for the current selection.
      int colMin = Integer.MAX_VALUE;
      int colMax = 0;
      for (Range range : ranges)
      {
         colMin = Math.min(range.getStart().getColumn(), colMin);
         colMax = Math.max(range.getEnd().getColumn(), colMax);
      }
      
      // For each range:
      //
      //    1. Set the left side of the selection to the minimum,
      //    2. Set the right side of the selection to the maximum,
      //       moving the cursor and inserting whitespace as necessary.
      for (Range range : ranges)
      {
         range.getStart().setColumn(colMin);
         range.getEnd().setColumn(colMax);
         
         String line = getLine(range.getStart().getRow());
         if (line.length() < colMax)
         {
            insertCode(
                  Position.create(range.getStart().getRow(), line.length()),
                  StringUtil.repeat(" ", colMax - line.length()));
         }
      }
      
      clearSelection();
      Selection selection = getNativeSelection();
      for (Range range : ranges)
      {
         if (hasSelection)
            selection.addRange(range, true);
         else
         {
            Range newRange = Range.create(
                  range.getEnd().getRow(),
                  range.getEnd().getColumn(),
                  range.getEnd().getRow(),
                  range.getEnd().getColumn());
            selection.addRange(newRange, true);
         }
      }
   }
   
   @Override
   public void clearSelectionHistory()
   {
      widget_.getEditor().clearSelectionHistory();
   }

   @Override
   public void reindent()
   {
      boolean emptySelection = getSelection().isEmpty();
      getSession().reindent(getSession().getSelection().getRange());
      if (emptySelection)
         moveSelectionToNextLine(false);
   }

   @Override
   public void reindent(Range range)
   {
      getSession().reindent(range);
   }

   @Override
   public void toggleCommentLines()
   {
      widget_.getEditor().toggleCommentLines();
   }

   public ChangeTracker getChangeTracker()
   {
      return new AceEditorChangeTracker();
   }

   // Because anchored selections create Ace event listeners, they
   // must be explicitly detached (otherwise they will listen for
   // edit events into perpetuity). The easiest way to facilitate this
   // is to have anchored selection tied to the lifetime of a particular
   // 'host' widget; this way, on detach, we can ensure that the associated
   // anchors are detached as well.
   public AnchoredSelection createAnchoredSelection(Widget hostWidget,
                                                    Position startPos,
                                                    Position endPos)
   {
      Anchor start = Anchor.createAnchor(getSession().getDocument(),
                                         startPos.getRow(),
                                         startPos.getColumn());
      Anchor end = Anchor.createAnchor(getSession().getDocument(),
                                       endPos.getRow(),
                                       endPos.getColumn());
      final AnchoredSelection selection = new AnchoredSelectionImpl(start, end);
      if (hostWidget != null)
      {
         hostWidget.addAttachHandler(event ->
         {
            if (!event.isAttached() && selection != null)
               selection.detach();
         });
      }
      
      return selection;
   }
   
   public AnchoredSelection createAnchoredSelection(Position start, Position end)
   {
      return createAnchoredSelection(null, start, end);
   }

   public void fitSelectionToLines(boolean expand)
   {
      Range range = getSession().getSelection().getRange();
      Position start = range.getStart();
      Position newStart = start;

      if (start.getColumn() > 0)
      {
         if (expand)
         {
            newStart = Position.create(start.getRow(), 0);
         }
         else
         {
            String firstLine = getSession().getLine(start.getRow());
            if (firstLine.substring(0, start.getColumn()).trim().length() == 0)
               newStart = Position.create(start.getRow(), 0);
         }
      }

      Position end = range.getEnd();
      Position newEnd = end;
      if (expand)
      {
         int endRow = end.getRow();
         if (endRow == newStart.getRow() || end.getColumn() > 0)
         {
            // If selection ends at the start of a line, keep the selection
            // there--unless that means less than one line will be selected
            // in total.
            newEnd = Position.create(
                  endRow, getSession().getLine(endRow).length());
         }
      }
      else
      {
         while (newEnd.getRow() != newStart.getRow())
         {
            String line = getSession().getLine(newEnd.getRow());
            if (line.substring(0, newEnd.getColumn()).trim().length() != 0)
               break;

            int prevRow = newEnd.getRow() - 1;
            int len = getSession().getLine(prevRow).length();
            newEnd = Position.create(prevRow, len);
         }
      }

      getSession().getSelection().setSelectionRange(
            Range.fromPoints(newStart, newEnd));
   }

   public int getSelectionOffset(boolean start)
   {
      Range range = getSession().getSelection().getRange();
      if (start)
         return range.getStart().getColumn();
      else
         return range.getEnd().getColumn();
   }

   public void onActivate()
   {
      Scheduler.get().scheduleFinally(() ->
      {
         widget_.onResize();
         widget_.onActivate();
         return false;
      });
   }

   public void onVisibilityChanged(boolean visible)
   {
      if (visible)
         widget_.getEditor().getRenderer().updateFontSize();
   }
   
   public void onResize()
   {
      widget_.onResize();
   }
   
   public void setHighlightSelectedLine(boolean on)
   {
      widget_.getEditor().setHighlightActiveLine(on);
   }

   public void setHighlightSelectedWord(boolean on)
   {
      widget_.getEditor().setHighlightSelectedWord(on);
   }

   public void setShowLineNumbers(boolean on)
   {
      widget_.getEditor().getRenderer().setShowGutter(on);
   }
   
   public boolean getUseSoftTabs()
   {
      return getSession().getUseSoftTabs();
   }

   public void setUseSoftTabs(boolean on)
   {
      getSession().setUseSoftTabs(on);
   }
   
   public void setScrollPastEndOfDocument(boolean enable)
   {
      widget_.getEditor().getRenderer().setScrollPastEnd(enable);
   }
   
   public void setHighlightRFunctionCalls(boolean highlight)
   {
      _setHighlightRFunctionCallsImpl(highlight);
      widget_.getEditor().retokenizeDocument();
   }

   public void setRainbowParentheses(boolean rainbow)
   {
      _setRainbowParenthesesImpl(rainbow);
      widget_.getEditor().retokenizeDocument();
   }

   public boolean getRainbowParentheses()
   {
      return _getRainbowParenthesesImpl();
   }

   public void setScrollLeft(int x)
   {
      getSession().setScrollLeft(x);
   }
   
   public void setScrollTop(int y)
   {
      getSession().setScrollTop(y);
   }
   
   public void scrollTo(int x, int y)
   {
      getSession().setScrollLeft(x);
      getSession().setScrollTop(y);
   }
   
   private native final void _setHighlightRFunctionCallsImpl(boolean highlight)
   /*-{
      var Mode = $wnd.require("mode/r_highlight_rules");
      Mode.setHighlightRFunctionCalls(highlight);
   }-*/;

   private native final void _setRainbowParenthesesImpl(boolean rainbow)
   /*-{
      var Mode = $wnd.require("mode/rainbow_paren_highlight_rules");
      Mode.setRainbowParentheses(rainbow);
   }-*/;

   private native final boolean _getRainbowParenthesesImpl()
   /*-{
     var Mode = $wnd.require("mode/rainbow_paren_highlight_rules");
     return Mode.getRainbowParentheses();
   }-*/;

   public void enableSearchHighlight()
   {
      widget_.getEditor().enableSearchHighlight();
   }
   
   public void disableSearchHighlight()
   {
      widget_.getEditor().disableSearchHighlight();
   }

   /**
    * Sets the soft wrap mode for the editor
    */
   public void setUseWrapMode(boolean useWrapMode)
   {
      getSession().setUseWrapMode(useWrapMode);
   }

   /**
    * Gets whether or not the editor is using soft wrapping
    */
   public boolean getUseWrapMode()
   {
      return getSession().getUseWrapMode();
   }

   public void setTabSize(int tabSize)
   {
      getSession().setTabSize(tabSize);
   }
   
   public void autoDetectIndentation(boolean on)
   {
      if (!on)
         return;
      
      JsArrayString lines = getLines();
      if (lines.length() < 5)
         return;

      int indentSize = StringUtil.detectIndent(lines);
      if (indentSize > 0)
      {
         setTabSize(indentSize);
         setUseSoftTabs(true);
      }
   }

   public void setShowInvisibles(boolean show)
   {
      widget_.getEditor().getRenderer().setShowInvisibles(show);
   }

   public void setShowIndentGuides(boolean show)
   {
      widget_.getEditor().getRenderer().setShowIndentGuides(show);
   }

   public void setBlinkingCursor(boolean blinking)
   {
      String style = blinking ? "ace" : "wide";
      widget_.getEditor().setCursorStyle(style);
   }

   public void setShowPrintMargin(boolean on)
   {
      widget_.getEditor().getRenderer().setShowPrintMargin(on);
   }

   @Override
   public void setUseEmacsKeybindings(boolean use)
   {
      if (widget_.getEditor().getReadOnly())
         return;

      useEmacsKeybindings_ = use;
      updateKeyboardHandlers();
   }
   
   @Override
   public boolean isEmacsModeOn()
   {
      return useEmacsKeybindings_;
   }
   
   @Override
   public void clearEmacsMark()
   {
      widget_.getEditor().clearEmacsMark();
   }
   
   @Override
   public void setUseVimMode(boolean use)
   {
      // no-op if the editor is read-only (since vim mode doesn't
      // work for read-only ace instances)
      if (widget_.getEditor().getReadOnly())
         return;

      useVimMode_ = use;
      updateKeyboardHandlers();
   }

   @Override
   public boolean isVimModeOn()
   {
      return useVimMode_;
   }
   
   @Override
   public boolean isVimInInsertMode()
   {
      return useVimMode_ && widget_.getEditor().isVimInInsertMode();
   }

   public void setPadding(int padding)
   {
      widget_.getEditor().getRenderer().setPadding(padding);
   }

   public void setPrintMarginColumn(int column)
   {
      widget_.getEditor().getRenderer().setPrintMarginColumn(column);
      syncWrapLimit();
   }

   @Override
   public JsArray<AceFold> getFolds()
   {
      return getSession().getAllFolds();
   }

   @Override
   public String getFoldState(int row)
   {
      AceFold fold = getSession().getFoldAt(row, 0);
      if (fold == null)
         return null;
      
      Position foldPos = fold.getStart();
      return getSession().getState(foldPos.getRow());
   }

   @Override
   public void addFold(Range range)
   {
      getSession().addFold("...", range);
   }

   @Override
   public void addFoldFromRow(int row)
   {
      FoldingRules foldingRules = getSession().getMode().getFoldingRules();
      if (foldingRules == null)
         return;
      Range range = foldingRules.getFoldWidgetRange(getSession(),
                                                    "markbegin",
                                                    row);

      if (range != null)
         addFold(range);
   }

   @Override
   public void unfold(AceFold fold)
   {
      getSession().unfold(Range.fromPoints(fold.getStart(), fold.getEnd()),
                          false);
   }

   @Override
   public void unfold(int row)
   {
      getSession().unfold(row, false);
   }

   @Override
   public void unfold(Range range)
   {
      getSession().unfold(range, false);
   }

   public void setReadOnly(boolean readOnly)
   {
      widget_.getEditor().setReadOnly(readOnly);
   }

   public HandlerRegistration addCursorChangedHandler(final CursorChangedHandler handler)
   {
      return widget_.addCursorChangedHandler(handler);
   }
   
   public HandlerRegistration addSaveCompletedHandler(SaveFileHandler handler)
   {
      return handlers_.addHandler(SaveFileEvent.TYPE, handler);
   }
   
   public HandlerRegistration addAttachHandler(AttachEvent.Handler handler)
   {
      return widget_.addAttachHandler(handler);
   }
   
   public HandlerRegistration addEditorFocusHandler(FocusHandler handler)
   {
      return widget_.addFocusHandler(handler);
   }
   
   public HandlerRegistration addEditorBlurHandler(BlurHandler handler)
   {
      return widget_.addBlurHandler(handler);
   }

   public HandlerRegistration addContextMenuHandler(ContextMenuHandler handler)
   {
      return widget_.addContextMenuHandler(handler);
   }

   public HandlerRegistration addScrollYHandler(ScrollYEvent.Handler handler)
   {
      return widget_.addScrollYHandler(handler);
   }

   public Scope getScopeAtPosition(Position position)
   {
      if (hasCodeModelScopeTree())
         return getSession().getMode().getCodeModel().getCurrentScope(position);
      
      if (scopes_ != null)
         return scopes_.getScopeAt(position);
      
      return null;
   }

   public Scope getCurrentScope()
   {
      return getScopeAtPosition(getCursorPosition());
   }
   
   @Override
   public String getNextLineIndent()
   {
      EditSession session = getSession();

      Position cursorPosition = getCursorPosition();
      int row = cursorPosition.getRow();
      String state = getSession().getState(row);

      String line = getCurrentLine().substring(
            0, cursorPosition.getColumn());
      String tab = session.getTabString();
      int tabSize = session.getTabSize();

      return session.getMode().getNextLineIndent(
            state,
            line,
            tab,
            tabSize,
            row);
   }

   @Override
   public Scope getCurrentChunk()
   {
      return getChunkAtPosition(getCursorPosition());
   }

   @Override
   public Scope getChunkAtPosition(Position position)
   {
      return getSession().getMode().getCodeModel().getCurrentChunk(position);
   }

   @Override
   public ScopeFunction getCurrentFunction(boolean allowAnonymous)
   {
      return getFunctionAtPosition(getCursorPosition(), allowAnonymous);
   }

   @Override
   public ScopeFunction getFunctionAtPosition(Position position,
                                              boolean allowAnonymous)
   {
      return getSession().getMode().getCodeModel().getCurrentFunction(
            position, allowAnonymous);
   }

   @Override
   public Scope getCurrentSection()
   {
      return getSectionAtPosition(getCursorPosition());
   }

   @Override
   public Scope getSectionAtPosition(Position position)
   {
      return getSession().getMode().getCodeModel().getCurrentSection(position);
   }

   public Position getCursorPosition()
   {
      return getSession().getSelection().getCursor();
   }
   
   public Position getCursorPositionScreen()
   {
      return widget_.getEditor().getCursorPositionScreen();
   }

   public void setCursorPosition(Position position)
   {
      getSession().getSelection().setSelectionRange(
            Range.fromPoints(position, position));
   }
   
   public void goToLineStart()
   {
      widget_.getEditor().getCommandManager().exec("gotolinestart", widget_.getEditor());
   }
   
   public void goToLineEnd()
   {
      widget_.getEditor().getCommandManager().exec("gotolineend", widget_.getEditor());
   }

   @Override
   public void moveCursorNearTop(int rowOffset)
   {
      int screenRow = getSession().documentToScreenRow(getCursorPosition());
      widget_.getEditor().scrollToRow(Math.max(0, screenRow - rowOffset));
   }
   
   @Override
   public void moveCursorForward()
   {
      moveCursorForward(1);
   }
   
   @Override
   public void moveCursorForward(int characters)
   {
      widget_.getEditor().moveCursorRight(characters);
   }
   
   @Override
   public void moveCursorBackward()
   {
      moveCursorBackward(1);
   }
   
   @Override
   public void moveCursorBackward(int characters)
   {
      widget_.getEditor().moveCursorLeft(characters);
   }

   @Override
   public void moveCursorNearTop()
   {
      moveCursorNearTop(7);
   }

   @Override
   public void ensureCursorVisible()
   {
      if (!widget_.getEditor().isRowFullyVisible(getCursorPosition().getRow()))
         moveCursorNearTop();
   }

   @Override
   public void ensureRowVisible(int row)
   {
      if (!widget_.getEditor().isRowFullyVisible(row))
         setCursorPosition(Position.create(row, 0));
   }
   
   @Override
   public void scrollCursorIntoViewIfNecessary(int rowsAround)
   {
      Position cursorPos = getCursorPosition();
      int cursorRow = cursorPos.getRow();

      if (cursorRow >= widget_.getEditor().getLastVisibleRow() - rowsAround)
      {
         Position alignPos = Position.create(cursorRow + rowsAround, 0);
         widget_.getEditor().getRenderer().alignCursor(alignPos, 1);
      }
      else if (cursorRow <= widget_.getEditor().getFirstVisibleRow() + rowsAround)
      {
         Position alignPos = Position.create(cursorRow - rowsAround, 0);
         widget_.getEditor().getRenderer().alignCursor(alignPos, 0);
      }
   }

   @Override
   public void scrollCursorIntoViewIfNecessary()
   {
      scrollCursorIntoViewIfNecessary(0);
   }

   @Override
   public boolean isCursorInSingleLineString()
   {
      return StringUtil.isEndOfLineInRStringState(getCurrentLineUpToCursor());
   }

   public void gotoPageUp()
   {
      widget_.getEditor().gotoPageUp();
   }

   public void gotoPageDown()
   {
      widget_.getEditor().gotoPageDown();
   }

   public void scrollToBottom()
   {
      SourcePosition pos = SourcePosition.create(getCurrentLineCount() - 1, 0);
      navigate(pos, false);
   }

   public void revealRange(Range range, boolean animate)
   {
      widget_.getEditor().revealRange(range, animate);
   }

   public CodeModel getCodeModel()
   {
      return getSession().getMode().getCodeModel();
   }

   public boolean hasCodeModel()
   {
      return getSession().getMode().hasCodeModel();
   }

   public boolean hasCodeModelScopeTree()
   {
      return hasCodeModel() && getCodeModel().hasScopes();
   }

   public void buildScopeTree()
   {
      // Builds the scope tree as a side effect
      if (hasCodeModelScopeTree())
         getScopeTree();
   }

   public int buildScopeTreeUpToRow(int row)
   {
      if (!hasCodeModelScopeTree())
         return 0;

      return getSession().getMode().getRCodeModel().buildScopeTreeUpToRow(row);
   }

   public JsArray<Scope> getScopeTree()
   {
      if (hasCodeModelScopeTree())
         return getSession().getMode().getCodeModel().getScopeTree();

      if (scopes_ != null)
         return scopes_.getScopeTree();

      return JavaScriptObject.createArray().cast();
   }

   @Override
   public InsertChunkInfo getInsertChunkInfo()
   {
      return getSession().getMode().getInsertChunkInfo();
   }

   @Override
   public void foldAll()
   {
      getSession().foldAll();
   }

   @Override
   public void unfoldAll()
   {
      getSession().unfoldAll();
   }

   @Override
   public void toggleFold()
   {
      getSession().toggleFold();
   }

   @Override
   public void setFoldStyle(String style)
   {
      getSession().setFoldStyle(style);
   }

   @Override
   public JsMap<Position> getMarks()
   {
      return widget_.getEditor().getMarks();
   }

   @Override
   public void setMarks(JsMap<Position> marks)
   {
      widget_.getEditor().setMarks(marks);
   }

   @Override
   public void jumpToMatching()
   {
      widget_.getEditor().jumpToMatching(false, false);
      scrollCursorIntoViewIfNecessary();
   }

   @Override
   public void selectToMatching()
   {
      widget_.getEditor().jumpToMatching(true, false);
      scrollCursorIntoViewIfNecessary();
   }

   @Override
   public void expandToMatching()
   {
      widget_.getEditor().jumpToMatching(true, true);
      scrollCursorIntoViewIfNecessary();
   }

   @Override
   public void addCursorAbove()
   {
      widget_.getEditor().execCommand("addCursorAbove");
   }

   @Override
   public void addCursorBelow()
   {
      widget_.getEditor().execCommand("addCursorBelow");
   }

   @Override
   public void editLinesFromStart()
   {
      editLines_.editLinesFromStart();
   }

   @Override
   public void moveLinesUp()
   {
      widget_.getEditor().execCommand("movelinesup");
   }

   @Override
   public void moveLinesDown()
   {
      widget_.getEditor().execCommand("movelinesdown");
   }

   @Override
   public void expandToLine()
   {
      widget_.getEditor().execCommand("expandtoline");
   }

   @Override
   public void copyLinesDown()
   {
      widget_.getEditor().execCommand("copylinesdown");
   }

   @Override
   public void joinLines()
   {
      widget_.getEditor().execCommand("joinlines");
   }

   @Override
   public void removeLine()
   {
      widget_.getEditor().execCommand("removeline");
   }

   @Override
   public void splitIntoLines()
   {
      widget_.getEditor().splitIntoLines();
   }

   @Override
   public int getFirstFullyVisibleRow()
   {
      return widget_.getEditor().getRenderer().getFirstFullyVisibleRow();
   }

   @Override
   public SourcePosition findFunctionPositionFromCursor(String functionName)
   {
      Scope func =
         getSession().getMode().getCodeModel().findFunctionDefinitionFromUsage(
                                                      getCursorPosition(),
                                                      functionName);
      if (func != null)
      {
         Position position = func.getPreamble();
         return SourcePosition.create(position.getRow(), position.getColumn());
      }
      else
      {
         return null;
      }
   }

   public JsArray<ScopeFunction> getAllFunctionScopes()
   {
      CodeModel codeModel = widget_.getEditor().getSession().getMode().getRCodeModel();
      if (codeModel == null)
         return null;

      return codeModel.getAllFunctionScopes();
   }

   @Override
   public void recordCurrentNavigationPosition()
   {
      fireRecordNavigationPosition(getCursorPosition());
   }

   @Override
   public void navigateToPosition(SourcePosition position,
                                  boolean recordCurrent)
   {
      navigateToPosition(position, recordCurrent, false, false);
   }

   @Override
   public void navigateToPosition(SourcePosition position,
                                  boolean recordCurrent,
                                  boolean highlightLine,
                                  boolean restoreCursorPosition)
   {
      if (recordCurrent)
         recordCurrentNavigationPosition();

      navigate(position, true, highlightLine, restoreCursorPosition);
   }

   @Override
   public void restorePosition(SourcePosition position)
   {
      navigate(position, false);
   }

   @Override
   public boolean isAtSourceRow(SourcePosition position)
   {
      Position currPos = getCursorPosition();
      return currPos.getRow() == position.getRow();
   }

   @Override
   public void highlightDebugLocation(SourcePosition startPosition,
                                      SourcePosition endPosition,
                                      boolean executing)
   {
      int firstRow = widget_.getEditor().getFirstVisibleRow();
      int lastRow = widget_.getEditor().getLastVisibleRow();

      // if the expression is large, let's just try to land in the middle
      int debugRow = (int) Math.floor(startPosition.getRow() + (
            endPosition.getRow() - startPosition.getRow())/2);

      // if the row at which the debugging occurs is inside a fold, unfold it
      getSession().unfold(debugRow, true);

      // if the line to be debugged is past or near the edges of the screen,
      // scroll it into view. allow some lines of context.
      if (debugRow <= (firstRow + DEBUG_CONTEXT_LINES) ||
          debugRow >= (lastRow - DEBUG_CONTEXT_LINES))
      {
         widget_.getEditor().scrollToLine(debugRow, true);
      }

      applyDebugLineHighlight(
            startPosition.asPosition(),
            endPosition.asPosition(),
            executing);
   }

   @Override
   public void endDebugHighlighting()
   {
      clearDebugLineHighlight();
   }

   @Override
   public HandlerRegistration addBreakpointSetHandler(
         BreakpointSetEvent.Handler handler)
   {
      return widget_.addBreakpointSetHandler(handler);
   }

   @Override
   public HandlerRegistration addBreakpointMoveHandler(
         BreakpointMoveEvent.Handler handler)
   {
      return widget_.addBreakpointMoveHandler(handler);
   }

   @Override
   public void addOrUpdateBreakpoint(Breakpoint breakpoint)
   {
      widget_.addOrUpdateBreakpoint(breakpoint);
   }

   @Override
   public void removeBreakpoint(Breakpoint breakpoint)
   {
      widget_.removeBreakpoint(breakpoint);
   }

   @Override
   public void toggleBreakpointAtCursor()
   {
      widget_.toggleBreakpointAtCursor();
   }

   @Override
   public void removeAllBreakpoints()
   {
      widget_.removeAllBreakpoints();
   }

   @Override
   public boolean hasBreakpoints()
   {
      return widget_.hasBreakpoints();
   }
   
   public void setChunkLineExecState(int start, int end, int state)
   {
      widget_.setChunkLineExecState(start, end, state);
   }

   private void navigate(SourcePosition srcPosition, boolean addToHistory)
   {
      navigate(srcPosition, addToHistory, false, false);
   }

   private void navigate(SourcePosition srcPosition,
                         boolean addToHistory,
                         boolean highlightLine,
                         boolean restoreCursorPosition)
   {
      // get existing cursor position
      Position previousCursorPos = getCursorPosition();

      // set cursor to function line
      Position position = Position.create(srcPosition.getRow(),
                                          srcPosition.getColumn());
      setCursorPosition(position);

      // skip whitespace if necessary
      if (srcPosition.getColumn() == 0)
      {
         int curRow = getSession().getSelection().getCursor().getRow();
         String line = getSession().getLine(curRow);
         int funStart = line.indexOf(line.trim());
         position = Position.create(curRow, funStart);
         setCursorPosition(position);
      }

      // scroll as necessary
      if (srcPosition.getScrollPosition() != -1)
         scrollToY(srcPosition.getScrollPosition(), 0);
      else if (position.getRow() != previousCursorPos.getRow())
         moveCursorNearTop();
      else
         ensureCursorVisible();

      // restore original cursor position or set focus
      if (restoreCursorPosition)
         setCursorPosition(previousCursorPos);
      else
         focus();

      if (highlightLine)
         applyLineHighlight(position.getRow());

      // add to navigation history if requested and our current mode
      // supports history navigation
      if (addToHistory)
         fireRecordNavigationPosition(position);
   }

   private void fireRecordNavigationPosition(Position pos)
   {
      SourcePosition srcPos = SourcePosition.create(pos.getRow(),
                                                    pos.getColumn());
      fireEvent(new RecordNavigationPositionEvent(srcPos));
   }

   @Override
   public HandlerRegistration addRecordNavigationPositionHandler(
                                    RecordNavigationPositionHandler handler)
   {
      return handlers_.addHandler(RecordNavigationPositionEvent.TYPE, handler);
   }

   @Override
   public HandlerRegistration addCommandClickHandler(
                                             CommandClickEvent.Handler handler)
   {
      return handlers_.addHandler(CommandClickEvent.TYPE, handler);
   }

   @Override
   public HandlerRegistration addFindRequestedHandler(
                                 FindRequestedEvent.Handler handler)
   {
      return handlers_.addHandler(FindRequestedEvent.TYPE, handler);
   }

   public void setFontSize(double size)
   {
      // No change needed--the AceEditorWidget uses the "normalSize" style
      // However, we do need to resize the gutter
      widget_.getEditor().getRenderer().updateFontSize();
      widget_.forceResize();
      widget_.getLineWidgetManager().syncLineWidgetHeights();
   }

   public HandlerRegistration addValueChangeHandler(
         ValueChangeHandler<Void> handler)
   {
      return handlers_.addHandler(ValueChangeEvent.getType(), handler);
   }

   public HandlerRegistration addFoldChangeHandler(
         FoldChangeEvent.Handler handler)
   {
      return handlers_.addHandler(FoldChangeEvent.TYPE, handler);
   }
   
   public HandlerRegistration addLineWidgetsChangedHandler(
                           LineWidgetsChangedEvent.Handler handler)
   {
      return handlers_.addHandler(LineWidgetsChangedEvent.TYPE, handler);
   }
   
   public boolean isScopeTreeReady(int row)
   {
      // NOTE: 'hasScopeTree()' implies JavaScript-side scope tree
      if (hasCodeModelScopeTree())
         return backgroundTokenizer_.isReady(row);
      
      if (scopes_ != null)
         return scopes_.isReady(row);
      
      return false;
   }

   public HandlerRegistration addScopeTreeReadyHandler(ScopeTreeReadyEvent.Handler handler)
   {
      return handlers_.addHandler(ScopeTreeReadyEvent.TYPE, handler);
   }

   public HandlerRegistration addActiveScopeChangedHandler(ActiveScopeChangedEvent.Handler handler)
   {
      return handlers_.addHandler(ActiveScopeChangedEvent.TYPE, handler);
   }
   
   public HandlerRegistration addRenderFinishedHandler(RenderFinishedEvent.Handler handler)
   {
      return widget_.addHandler(handler, RenderFinishedEvent.TYPE);
   }
   
   public HandlerRegistration addDocumentChangedHandler(DocumentChangedEvent.Handler handler)
   {
      return widget_.addHandler(handler, DocumentChangedEvent.TYPE);
   }
   
   public HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler)
   {
      return widget_.addCapturingKeyDownHandler(handler);
   }

   public HandlerRegistration addCapturingKeyPressHandler(KeyPressHandler handler)
   {
      return widget_.addCapturingKeyPressHandler(handler);
   }

   public HandlerRegistration addCapturingKeyUpHandler(KeyUpHandler handler)
   {
      return widget_.addCapturingKeyUpHandler(handler);
   }

   public HandlerRegistration addUndoRedoHandler(UndoRedoHandler handler)
   {
      return widget_.addUndoRedoHandler(handler);
   }
   
   public HandlerRegistration addPasteHandler(PasteEvent.Handler handler)
   {
      return widget_.addPasteHandler(handler);
   }
   
   public HandlerRegistration addEditHandler(EditEvent.Handler handler)
   {
      return widget_.addHandler(handler, EditEvent.TYPE);
   }

   public HandlerRegistration addAceClickHandler(Handler handler)
   {
      return widget_.addAceClickHandler(handler);
   }
   
   public HandlerRegistration addEditorModeChangedHandler(
         EditorModeChangedEvent.Handler handler)
   {
      return handlers_.addHandler(EditorModeChangedEvent.TYPE, handler);
   }

   public JavaScriptObject getCleanStateToken()
   {
      return getSession().getUndoManager().peek();
   }

   public boolean checkCleanStateToken(JavaScriptObject token)
   {
      JavaScriptObject other = getSession().getUndoManager().peek();
      if (token == null ^ other == null)
         return false;
      return token == null || other.equals(token);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public Widget asWidget()
   {
      return widget_;
   }

   public EditSession getSession()
   {
      return widget_.getEditor().getSession();
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return widget_.addBlurHandler(handler);
   }
   
   public HandlerRegistration addMouseDownHandler(MouseDownHandler handler)
   {
      return widget_.addMouseDownHandler(handler);
   }
   
   public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler)
   {
      return widget_.addMouseMoveHandler(handler);
   }
   
   public HandlerRegistration addMouseUpHandler(MouseUpHandler handler)
   {
      return widget_.addMouseUpHandler(handler);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return widget_.addClickHandler(handler);
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return widget_.addFocusHandler(handler);
   }

   public AceEditorWidget getWidget()
   {
      return widget_;
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return widget_.addKeyDownHandler(handler);
   }
   
   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return widget_.addKeyPressHandler(handler);
   }
   
   public HandlerRegistration addKeyUpHandler(KeyUpHandler handler)
   {
      return widget_.addKeyUpHandler(handler);
   }
   
   public HandlerRegistration addSelectionChangedHandler(AceSelectionChangedEvent.Handler handler)
   {
      return widget_.addSelectionChangedHandler(handler);
   }

   public void autoHeight()
   {
      widget_.autoHeight();
   }

   public void forceCursorChange()
   {
      widget_.forceCursorChange();
   }

   public void scrollToLine(int row, boolean center)
   {
      widget_.getEditor().scrollToLine(row, center);
   }

   public void centerSelection()
   {
      widget_.getEditor().centerSelection();
   }

   public void alignCursor(Position position, double ratio)
   {
      widget_.getEditor().getRenderer().alignCursor(position, ratio);
   }

   public void forceImmediateRender()
   {
      widget_.getEditor().getRenderer().forceImmediateRender();
   }

   public void setNewLineMode(NewLineMode mode)
   {
      getSession().setNewLineMode(mode.getType());
   }

   public boolean isPasswordMode()
   {
      return passwordMode_;
   }

   public void setPasswordMode(boolean passwordMode)
   {
      passwordMode_ = passwordMode;
      widget_.getEditor().getRenderer().setPasswordMode(passwordMode);
   }

   public void setDisableOverwrite(boolean disableOverwrite)
   {
      getSession().setDisableOverwrite(disableOverwrite);
   }

   private Integer createLineHighlightMarker(int line, String style)
   {
      return createRangeHighlightMarker(Position.create(line, 0),
                                        Position.create(line+1, 0),
                                        style);
   }

   private Integer createRangeHighlightMarker(
         Position start,
         Position end,
         String style)
   {
      Range range = Range.fromPoints(start, end);
      return getSession().addMarker(range, style, "text", false);
   }


   private void applyLineHighlight(int line)
   {
      clearLineHighlight();

      if (!widget_.getEditor().getHighlightActiveLine())
      {
         lineHighlightMarkerId_ = createLineHighlightMarker(line,
                                                            "ace_find_line");
      }
   }

   private void clearLineHighlight()
   {
      if (lineHighlightMarkerId_ != null)
      {
         getSession().removeMarker(lineHighlightMarkerId_);
         lineHighlightMarkerId_ = null;
      }
   }

   private void applyDebugLineHighlight(
         Position startPos,
         Position endPos,
         boolean executing)
   {
      clearDebugLineHighlight();
      lineDebugMarkerId_ = createRangeHighlightMarker(
            startPos, endPos,
            "ace_active_debug_line");
      if (executing)
      {
         executionLine_ = startPos.getRow();
         widget_.getEditor().getRenderer().addGutterDecoration(
               executionLine_,
               "ace_executing-line");
      }
   }

   private void clearDebugLineHighlight()
   {
      if (lineDebugMarkerId_ != null)
      {
         getSession().removeMarker(lineDebugMarkerId_);
         lineDebugMarkerId_ = null;
      }
      if (executionLine_ != null)
      {
         widget_.getEditor().getRenderer().removeGutterDecoration(
               executionLine_,
               "ace_executing-line");
         executionLine_ = null;
      }
   }
   
   public void setPopupVisible(boolean visible)
   {
      popupVisible_ = visible;
   }

   public boolean isPopupVisible()
   {
      return popupVisible_;
   }
   
   public void selectAll(String needle)
   {
      widget_.getEditor().findAll(needle);
   }

   public void selectAll(String needle, Range range, boolean wholeWord, boolean caseSensitive)
   {
      widget_.getEditor().findAll(needle, range, wholeWord, caseSensitive);
   }

   public void moveCursorLeft()
   {
      moveCursorLeft(1);
   }

   public void moveCursorLeft(int times)
   {
      widget_.getEditor().moveCursorLeft(times);
   }

   public void moveCursorRight()
   {
      moveCursorRight(1);
   }

   public void moveCursorRight(int times)
   {
      widget_.getEditor().moveCursorRight(times);
   }

   public void expandSelectionLeft(int times)
   {
      widget_.getEditor().expandSelectionLeft(times);
   }

   public void expandSelectionRight(int times)
   {
      widget_.getEditor().expandSelectionRight(times);
   }

   public int getTabSize()
   {
      return widget_.getEditor().getSession().getTabSize();
   }

   // TODO: Enable similar logic for C++ mode?
   public int getStartOfCurrentStatement()
   {
      if (!DocumentMode.isSelectionInRMode(this))
         return -1;

      TokenCursor cursor =
            getSession().getMode().getCodeModel().getTokenCursor();

      if (!cursor.moveToPosition(getCursorPosition()))
         return -1;

      if (!cursor.moveToStartOfCurrentStatement())
         return -1;

      return cursor.getRow();
   }

   // TODO: Enable similar logic for C++ mode?
   public int getEndOfCurrentStatement()
   {
      if (!DocumentMode.isSelectionInRMode(this))
         return -1;

      TokenCursor cursor =
            getSession().getMode().getCodeModel().getTokenCursor();

      if (!cursor.moveToPosition(getCursorPosition()))
         return -1;

      if (!cursor.moveToEndOfCurrentStatement())
         return -1;

      return cursor.getRow();
   }
   
   private boolean rowEndsInBinaryOperatorOrOpenParen(int row)
   {
      // move to the last interesting token on this line 
      JsArray<Token> tokens = getSession().getTokens(row);
      for (int i = tokens.length() - 1; i >= 0; i--)
      {
         Token t = tokens.get(i);
         if (t.hasType("text", "comment", "virtual-comment"))
            continue;
         if (t.getType()  == "keyword.operator" ||
             t.getType()  == "keyword.operator.infix" ||
             t.getValue() == "," ||
             t.getValue() == "(")
            return true;
         break;
      } 
      return false;
   }
   
   private boolean rowIsEmptyOrComment(int row)
   {
      JsArray<Token> tokens = getSession().getTokens(row);
      for (int i = 0, n = tokens.length(); i < n; i++)
         if (!tokens.get(i).hasType("text", "comment", "virtual-comment"))
            return false;
      return true;
   }
   
   private boolean rowStartsWithClosingBracket(int row)
   {
      JsArray<Token> tokens = getSession().getTokens(row);
      
      int n = tokens.length();
      if (n == 0)
         return false;
      
      for (int i = 0; i < n; i++)
      {
         Token token = tokens.get(i);
         if (token.hasType("text"))
            continue;
         
         String tokenValue = token.getValue();
         return tokenValue == "}" ||
                tokenValue == ")" ||
                tokenValue == "]";
      }
      
      return false;
   }
   
   private boolean rowEndsWithOpenBracket(int row)
   {
      JsArray<Token> tokens = getSession().getTokens(row);
      
      int n = tokens.length();
      if (n == 0)
         return false;
      
      for (int i = 0; i < n; i++)
      {
         Token token = tokens.get(n - i - 1);
         if (token.hasType("text", "comment", "virtual-comment"))
            continue;
         
         String tokenValue = token.getValue();
         return tokenValue == "{" ||
                tokenValue == "(" ||
                tokenValue == "[";
      }
      
      return false;
   }
   
   /**
    * Finds the last non-empty line starting at the given line.
    * 
    * @param initial Row to start on
    * @param limit Row at which to stop searching
    * @return Index of last non-empty line, or limit line if no empty lines
    *   were found.
    */
   private int findParagraphBoundary(int initial, int limit)
   {
      // no work to do if already at limit
      if (initial == limit)
         return initial;
      
      // walk towards limit
      int delta = limit > initial ? 1 : -1;
      for (int row = initial + delta; row != limit; row += delta)
      {
         if (getLine(row).trim().isEmpty())
            return row - delta;
      }
      
      // didn't find boundary
      return limit;
   }

   @Override
   public Range getParagraph(Position pos, int startRowLimit, int endRowLimit)
   {
      // find upper and lower paragraph boundaries
      return Range.create(
            findParagraphBoundary(pos.getRow(), startRowLimit), 0,
            findParagraphBoundary(pos.getRow(), endRowLimit)+ 1, 0);
   }

   @Override
   public Range getMultiLineExpr(Position pos, int startRowLimit, int endRowLimit)
   {
      if (DocumentMode.isSelectionInRMode(this))
      {
         return rMultiLineExpr(pos, startRowLimit, endRowLimit);
      }
      else
      {
         return getParagraph(pos, startRowLimit, endRowLimit);
      }
   }
   
   private Range rMultiLineExpr(Position pos, int startRowLimit, int endRowLimit)
   {
      // create token cursor (will be used to walk tokens as needed)
      TokenCursor c = getSession().getMode().getCodeModel().getTokenCursor();
      
      // assume start, end at current position
      int startRow = pos.getRow();
      int endRow   = pos.getRow();
      
      // expand to enclosing '(' or '['
      do
      {
         c.setRow(pos.getRow());
         
         // move forward over commented / empty lines
         int n = getSession().getLength();
         while (rowIsEmptyOrComment(c.getRow()))
         {
            if (c.getRow() == n - 1)
               break;
            
            c.setRow(c.getRow() + 1);
         }
         
         // move to last non-right-bracket token on line
         c.moveToEndOfRow(c.getRow());
         while (c.valueEquals(")") || c.valueEquals("]"))
            if (!c.moveToPreviousToken())
               break;
         
         // find the top-most enclosing bracket
         // check for function scope
         String[] candidates = new String[] {"(", "["};
         int savedRow = -1;
         int savedOffset = -1;
         while (c.findOpeningBracket(candidates, true))
         {
            // check for function scope
            if (c.valueEquals("(") &&
                c.peekBwd(1).valueEquals("function") &&
                c.peekBwd(2).isLeftAssign())
            {
               ScopeFunction scope = getFunctionAtPosition(c.currentPosition(), false);
               if (scope != null)
                  return Range.fromPoints(scope.getPreamble(), scope.getEnd());
            }
            
            // move off of opening bracket and continue lookup
            savedRow = c.getRow();
            savedOffset = c.getOffset();
            if (!c.moveToPreviousToken())
               break;
         }
         
         // if we found a row, use it
         if (savedRow != -1 && savedOffset != -1)
         {
            c.setRow(savedRow);
            c.setOffset(savedOffset);
            if (c.fwdToMatchingToken())
            {
               startRow = savedRow;
               endRow = c.getRow();
            }
         }
         
      }
      while (false);
      
      // check for binary operator on start line
      if (rowEndsInBinaryOperatorOrOpenParen(startRow))
      {
         // move token cursor to that row
         c.moveToEndOfRow(startRow);

         // skip comments, operators, etc.
         while (c.hasType("text", "comment", "virtual-comment", "keyword.operator"))
         {
            if (c.isRightBracket())
               break;

            if (!c.moveToPreviousToken())
               break;
         }

         // if we landed on a closing bracket, look for its match
         // and then continue search from that row. otherwise,
         // just look back a single row
         if (c.valueEquals(")") || c.valueEquals("]"))
         {
            if (c.bwdToMatchingToken())
            {
               startRow = c.getRow();
            }
         }
      }
      
      // discover start of current statement
      while (startRow >= startRowLimit)
      {
         // if we've hit the start of the document, bail
         if (startRow <= 0)
            break;
         
         // if the row starts with a closing bracket, expand to its match
         if (rowStartsWithClosingBracket(startRow))
         {
            c.moveToStartOfRow(startRow);
            if (c.bwdToMatchingToken())
            {
               startRow = c.getRow();
               continue;
            }
         }
         
         // check for binary operator on previous line
         int prevRow = startRow - 1;
         if (rowEndsInBinaryOperatorOrOpenParen(prevRow))
         {
            // move token cursor to that row
            c.moveToEndOfRow(prevRow);
            
            // skip comments, operators, etc.
            while (c.hasType("text", "comment", "virtual-comment", "keyword.operator"))
            {
               if (c.isRightBracket())
                  break;
                  
               if (!c.moveToPreviousToken())
                  break;
            }
          
            // if we landed on a closing bracket, look for its match
            // and then continue search from that row. otherwise,
            // just look back a single row
            if (c.valueEquals(")") || c.valueEquals("]"))
            {
               if (c.bwdToMatchingToken())
               {
                  startRow = c.getRow();
                  continue;
               }
            }
            else
            {
               startRow--;
               continue;
            }
         }
         
         // keep going over blank, commented lines
         if (rowIsEmptyOrComment(prevRow))
         {
            startRow--;
            continue;
         }
         
         // keep going if we're in a multiline string
         String state = getSession().getState(prevRow);
         if (state == "qstring" || state == "qqstring")
         {
            startRow--;
            continue;
         }
         
         // bail out of the loop -- we've found the start of the statement
         break;
      }
      
      // discover end of current statement -- we search from the inferred statement
      // start, so that we can perform counting of matching pairs of brackets
      endRow = startRow;
      
      // NOTE: '[[' is not tokenized as a single token in our Ace tokenizer,
      // so it is not included here (this shouldn't cause issues in practice
      // since balanced pairs of '[' and '[[' would still imply a correct count
      // of matched pairs of '[' anyhow)
      int parenCount = 0;   // '(', ')'
      int braceCount = 0;   // '{', '}'
      int bracketCount = 0; // '[', ']'
      
      while (endRow <= endRowLimit)
      {
         // continue search if we're in a multi-line string
         // (forego updating our bracket counts)
         String state = getSession().getState(endRow);
         if (state == "qstring" || state == "qqstring")
         {
            endRow++;
            continue;
         }
         
         // update bracket token counts
         JsArray<Token> tokens = getTokens(endRow);
         for (Token token : JsUtil.asIterable(tokens))
         {
            String value = token.getValue();
            
            parenCount += value == "(" ? 1 : 0;
            parenCount -= value == ")" ? 1 : 0;
            
            braceCount += value == "{" ? 1 : 0;
            braceCount -= value == "}" ? 1 : 0;
            
            bracketCount += value == "[" ? 1 : 0;
            bracketCount -= value == "]" ? 1 : 0;
         }
         
         // continue search if line ends with binary operator
         if (rowEndsInBinaryOperatorOrOpenParen(endRow) || rowIsEmptyOrComment(endRow))
         {
            endRow++;
            continue;
         }
         
         // continue search if we have unbalanced brackets
         if (parenCount > 0 || braceCount > 0 || bracketCount > 0)
         {
            endRow++;
            continue;
         }
         
         // we had balanced brackets and no trailing binary operator; bail
         break;
      }
      
      // if we're unbalanced at this point, that means we tried to
      // expand in an unclosed expression -- just execute the current
      // line rather than potentially executing unintended code
      if (parenCount + braceCount + bracketCount > 0)
         return Range.create(pos.getRow(), 0, pos.getRow() + 1, 0);
      
      // shrink selection for empty lines at borders
      while (startRow < endRow && rowIsEmptyOrComment(startRow))
         startRow++;
      
      while (endRow > startRow && rowIsEmptyOrComment(endRow))
         endRow--;
      
      // fixup for single-line execution
      if (startRow > endRow)
         startRow = endRow;
      
      // if we've captured the body of a function definition, expand
      // to include whole definition
      c.setRow(startRow);
      c.setOffset(0);
      if (c.valueEquals("{") &&
          c.moveToPreviousToken() &&
          c.valueEquals(")") &&
          c.bwdToMatchingToken() &&
          c.moveToPreviousToken() &&
          c.valueEquals("function") &&
          c.moveToPreviousToken() &&
          c.isLeftAssign())
      {
         ScopeFunction fn = getFunctionAtPosition(c.currentPosition(), false);
         if (fn != null)
            return Range.fromPoints(fn.getPreamble(), fn.getEnd());
      }
      
      // construct range
      int endColumn = getSession().getLine(endRow).length();
      Range range = Range.create(startRow, 0, endRow, endColumn);
      
      // return empty range if nothing to execute
      if (getTextForRange(range).trim().isEmpty())
         range = Range.fromPoints(pos, pos);
      
      return range;
   }
   
   // ---- Annotation related operations

   public JsArray<AceAnnotation> getAnnotations()
   {
      return widget_.getAnnotations();
   }

   public void setAnnotations(JsArray<AceAnnotation> annotations)
   {
      widget_.setAnnotations(annotations);
   }

   @Override
   public void removeMarkersAtCursorPosition()
   {
      widget_.removeMarkersAtCursorPosition();
   }

   @Override
   public void removeMarkersOnCursorLine()
   {
      widget_.removeMarkersOnCursorLine();
   }

   @Override
   public void removeMarkers(BiPredicate<AceAnnotation, Marker> predicate)
   {
      widget_.removeMarkers(predicate);
   }

   @Override
   public void removeMarkersAtWord(String word)
   {
      widget_.removeMarkersAtWord(word);
   }

   @Override
   public void showLint(JsArray<LintItem> lint)
   {
      widget_.showLint(lint);
   }

   @Override
   public void clearLint()
   {
      widget_.clearLint();
   }
   
   @Override
   public void showInfoBar(String message)
   {
      if (infoBar_ == null)
      {
         infoBar_ = new AceInfoBar(widget_);
         widget_.addKeyDownHandler(event ->
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
               infoBar_.hide();
         });
      }
         
      infoBar_.setText(message);
      infoBar_.show();
   }

   public AnchoredRange createAnchoredRange(Position start, Position end)
   {
      return widget_.getEditor().getSession().createAnchoredRange(start, end);
   }

   public void insertRoxygenSkeleton()
   {
      getSession().getMode().getCodeModel().insertRoxygenSkeleton();
   }

   public long getLastModifiedTime()
   {
      return lastModifiedTime_;
   }

   public long getLastCursorChangedTime()
   {
      return lastCursorChangedTime_;
   }
   
   public int getFirstVisibleRow()
   {
      return widget_.getEditor().getFirstVisibleRow();
   }

   public int getLastVisibleRow()
   {
      return widget_.getEditor().getLastVisibleRow();
   }

   public void blockIndent()
   {
      widget_.getEditor().blockIndent();
   }

   public void blockOutdent()
   {
      widget_.getEditor().blockOutdent();
   }
   
   public ScreenCoordinates documentPositionToScreenCoordinates(Position position)
   {
      return widget_.getEditor().getRenderer().textToScreenCoordinates(position);
   }

   public Position screenCoordinatesToDocumentPosition(int pageX, int pageY)
   {
      return widget_.getEditor().getRenderer().screenToTextCoordinates(pageX, pageY);
   }

   public boolean isPositionVisible(Position position)
   {
      return widget_.getEditor().isRowFullyVisible(position.getRow());
   }
   
   @Override
   public void tokenizeDocument()
   {
      widget_.getEditor().tokenizeDocument();
   }
   
   @Override
   public void retokenizeDocument()
   {
      widget_.getEditor().retokenizeDocument();
   }
   
   @Override
   public Token getTokenAt(int row, int column)
   {
      return getSession().getTokenAt(row, column);
   }
   
   @Override
   public Token getTokenAt(Position position)
   {
      return getSession().getTokenAt(position);
   }
   
   @Override
   public JsArray<Token> getTokens(int row)
   {
      return getSession().getTokens(row);
   }
   
   @Override
   public TokenIterator createTokenIterator()
   {
      return createTokenIterator(null);
   }
   
   @Override
   public TokenIterator createTokenIterator(Position position)
   {
      TokenIterator it = TokenIterator.create(getSession());
      if (position == null)
         position = Position.create(0, 0);
      it.moveToPosition(position);
      return it;
   }
 

   @Override
   public void beginCollabSession(CollabEditStartParams params, 
         DirtyState dirtyState)
   {
      // suppress external value change events while the editor's contents are
      // being swapped out for the contents of the collab session--otherwise
      // there's going to be a lot of flickering as dirty state (etc) try to
      // keep up
      valueChangeSuppressed_ = true;

      collab_.beginCollabSession(this, params, dirtyState, () -> valueChangeSuppressed_ = false);
   }
   
   @Override
   public boolean hasActiveCollabSession()
   {
      return collab_.hasActiveCollabSession(this);
   }
   
   @Override
   public boolean hasFollowingCollabSession()
   {
      return collab_.hasFollowingCollabSession(this);
   }
   
   public void endCollabSession()
   {
      collab_.endCollabSession(this);
   }
   
   @Override
   public void setDragEnabled(boolean enabled)
   {
      widget_.setDragEnabled(enabled);
   }
   
   @Override
   public boolean isSnippetsTabStopManagerActive()
   {
      return isSnippetsTabStopManagerActiveImpl(widget_.getEditor());
   }
   
   private static final native
   boolean isSnippetsTabStopManagerActiveImpl(AceEditorNative editor)
   /*-{
      return editor.tabstopManager != null;
   }-*/;
   
   private boolean onInsertSnippet()
   {
      return snippets_.onInsertSnippet();
   }
   
   public void toggleTokenInfo()
   {
      toggleTokenInfo(widget_.getEditor());
   }
   
   private static final native void toggleTokenInfo(AceEditorNative editor) /*-{
      if (editor.tokenTooltip && editor.tokenTooltip.destroy) {
         editor.tokenTooltip.destroy();
      } else {
         var TokenTooltip = $wnd.require("ace/token_tooltip").TokenTooltip;
         editor.tokenTooltip = new TokenTooltip(editor);
      }
   }-*/;

   @Override
   public void addLineWidget(final LineWidget widget)
   {
      // position the element far offscreen if it's above the currently
      // visible row; Ace does not position line widgets above the viewport
      // until the document is scrolled there
      if (widget.getRow() < getFirstVisibleRow())
      {
         widget.getElement().getStyle().setTop(-10000, Unit.PX);
         
         // set left/right values so that the widget consumes space; necessary
         // to get layout offsets inside the widget while rendering but before
         // it comes onscreen
         widget.getElement().getStyle().setLeft(48, Unit.PX);
         widget.getElement().getStyle().setRight(15, Unit.PX);
      }
      
      widget_.getLineWidgetManager().addLineWidget(widget);
      adjustScrollForLineWidget(widget);
      fireLineWidgetsChanged();
   }
   
   @Override
   public void removeLineWidget(LineWidget widget)
   {
      widget_.getLineWidgetManager().removeLineWidget(widget);
      fireLineWidgetsChanged();
   }
   
   @Override
   public void removeAllLineWidgets()
   {
      widget_.getLineWidgetManager().removeAllLineWidgets();
      fireLineWidgetsChanged();
   }
   
   @Override
   public void onLineWidgetChanged(LineWidget widget)
   {
      // if the widget is above the viewport, this size change might push it
      // into visibility, so push it offscreen first
      if (widget.getRow() + 1 < getFirstVisibleRow())
         widget.getElement().getStyle().setTop(-10000, Unit.PX);

      widget_.getLineWidgetManager().onWidgetChanged(widget);
      adjustScrollForLineWidget(widget);
      fireLineWidgetsChanged();
   }
   
   @Override
   public JsArray<LineWidget> getLineWidgets()
   {
      return widget_.getLineWidgetManager().getLineWidgets();
   }
   
   @Override
   public LineWidget getLineWidgetForRow(int row)
   {
      return widget_.getLineWidgetManager().getLineWidgetForRow(row);
   }
   

   @Override
   public boolean hasLineWidgets()
   {
      return widget_.getLineWidgetManager().hasLineWidgets();
   }

   private void adjustScrollForLineWidget(LineWidget w)
   {
      // the cursor is above the line widget, so the line widget is going
      // to change the cursor position; adjust the scroll position to hold 
      // the cursor in place
      if (getCursorPosition().getRow() > w.getRow())
      {
         int delta = w.getElement().getOffsetHeight() - w.getRenderedHeight();
         
         // skip if no change to report
         if (delta == 0)
            return;

         // we adjust the scrolltop on the session since it knows the
         // currently queued scroll position; the renderer only knows the 
         // actual scroll position, which may not reflect unrendered changes
         getSession().setScrollTop(getSession().getScrollTop() + delta);
      }
      
      // mark the current height as rendered
      w.setRenderedHeight(w.getElement().getOffsetHeight());
   }
   
   @Override
   public JsArray<ChunkDefinition> getChunkDefs()
   {
      // chunk definitions are populated at render time, so don't return any
      // if we haven't rendered yet
      if (!isRendered())
         return null;
      
      JsArray<ChunkDefinition> chunks = JsArray.createArray().cast();
      JsArray<LineWidget> lineWidgets = getLineWidgets();
      ScopeList scopes = new ScopeList(this);
      for (int i = 0; i<lineWidgets.length(); i++)
      {
         LineWidget lineWidget = lineWidgets.get(i);
         if (lineWidget.getType() == ChunkDefinition.LINE_WIDGET_TYPE)
         {
            ChunkDefinition chunk = lineWidget.getData();
            chunks.push(chunk.with(lineWidget.getRow(), 
                  TextEditingTargetNotebook.getKnitrChunkLabel(
                        lineWidget.getRow(), this, scopes)));
         }
      }
      
      return chunks;
   }
   

   @Override
   public boolean isRendered()
   {
      return widget_.isRendered();
   }
   
   @Override
   public boolean showChunkOutputInline()
   {
      return showChunkOutputInline_;
   }
   
   @Override
   public void setShowChunkOutputInline(boolean show)
   {
      showChunkOutputInline_ = show;
   }

   /**
    * Set an aria-label on the input element
    * @param label
    */
   public final void setTextInputAriaLabel(String label)
   {
      widget_.getEditor().setTextInputAriaLabel(label);
   }

   public void setTabAlwaysMovesFocus()
   {
      widget_.setTabKeyMode(TabKeyMode.AlwaysMoveFocus);
   }

   public final void setScrollSpeed(double speed)
   {
      widget_.getEditor().setScrollSpeed(speed);
   }
   
   public final void setIndentedSoftWrap(boolean softWrap)
   {
      widget_.getEditor().setIndentedSoftWrap(softWrap);
   }

   private void fireLineWidgetsChanged()
   {
      AceEditor.this.fireEvent(new LineWidgetsChangedEvent());
   }
   
   private static class BackgroundTokenizer
   {
      public BackgroundTokenizer(final AceEditor editor)
      {
         editor_ = editor;
         
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               // Stop our timer if we've tokenized up to the end of the document.
               if (row_ >= editor_.getRowCount())
               {
                  editor_.fireEvent(new ScopeTreeReadyEvent(
                        editor_.getScopeTree(),
                        editor_.getCurrentScope()));
                  return;
               }
               
               row_ += ROWS_TOKENIZED_PER_ITERATION;
               row_ = Math.max(row_, editor.buildScopeTreeUpToRow(row_));
               timer_.schedule(DELAY_MS);
            }
         };
         
         editor_.addDocumentChangedHandler(event ->
         {
            if (editor_.hasCodeModelScopeTree())
            {
               row_ = event.getEvent().getRange().getStart().getRow();
               timer_.schedule(DELAY_MS);
            }
         });
      }
      
      public boolean isReady(int row)
      {
         return row < row_;
      }
      
      private final AceEditor editor_;
      private final Timer timer_;
      
      private int row_ = 0;
      
      private static final int DELAY_MS = 5;
      private static final int ROWS_TOKENIZED_PER_ITERATION = 200;
   }

   private class ScrollAnimator 
                 implements AnimationScheduler.AnimationCallback
   {
      public ScrollAnimator(int targetY, int ms)
      {
         targetY_ = targetY;
         startY_ = widget_.getEditor().getRenderer().getScrollTop();
         delta_ = targetY_ - startY_;
         ms_ = ms;
         handle_ = AnimationScheduler.get().requestAnimationFrame(this);
      }
      
      public void complete()
      {
         handle_.cancel();
         scrollAnimator_ = null;
      }
      
      @Override
      public void execute(double timestamp)
      {
         if (startTime_ < 0)
            startTime_ = timestamp;
         double elapsed = timestamp - startTime_;
         if (elapsed >= ms_)
         {
            scrollToY(targetY_, 0);
            complete();
            return;
         }

         // ease-out exponential
         scrollToY((int)(delta_ * (-Math.pow(2, -10 * elapsed / ms_) + 1)) + startY_, 0);

         // request next frame
         handle_ = AnimationScheduler.get().requestAnimationFrame(this);
      }

      private final int ms_;
      private final int targetY_;
      private final int startY_;
      private final int delta_;
      private double startTime_ = -1;
      private AnimationScheduler.AnimationHandle handle_;
   }
   
   private static final int DEBUG_CONTEXT_LINES = 2;
   private final HandlerManager handlers_ = new HandlerManager(this);
   private final AceEditorWidget widget_;
   private final SnippetHelper snippets_;
   private final AceEditorMonitor monitor_;
   private ScrollAnimator scrollAnimator_;
   private CompletionManager completionManager_;
   private ScopeTreeManager scopes_;
   private CodeToolsServerOperations server_;
   private UserPrefs userPrefs_;
   private KeyboardTracker keyboard_;
   private CollabEditor collab_;
   private Commands commands_;
   private EventBus events_;
   private TextFileType fileType_;
   private boolean passwordMode_;
   private boolean useEmacsKeybindings_ = false;
   private boolean useVimMode_ = false;
   private RnwCompletionContext rnwContext_;
   private CppCompletionContext cppContext_;
   private CompletionContext context_ = null;
   private Integer lineHighlightMarkerId_ = null;
   private Integer lineDebugMarkerId_ = null;
   private Integer executionLine_ = null;
   private boolean valueChangeSuppressed_ = false;
   private AceInfoBar infoBar_;
   private boolean showChunkOutputInline_ = false;
   private BackgroundTokenizer backgroundTokenizer_;
   private final Vim vim_;
   private final AceBackgroundHighlighter bgChunkHighlighter_;
   private final AceEditorBackgroundLinkHighlighter bgLinkHighlighter_;
   private int scrollTarget_ = 0;
   private HandlerRegistration scrollCompleteReg_;
   private final AceEditorMixins mixins_;
   private final AceEditorEditLinesHelper editLines_;
   
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release)
   {
      return getLoader(release, null);
   }
   
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release,
                                                           StaticDataResource debug)
   {
      if (debug == null || !SuperDevMode.isActive())
         return new ExternalJavaScriptLoader(release.getSafeUri().asString());
      else
         return new ExternalJavaScriptLoader(debug.getSafeUri().asString());
   }
                                                           
    
   private static final ExternalJavaScriptLoader aceLoader_ =
         getLoader(AceResources.INSTANCE.acejs(), AceResources.INSTANCE.acejsUncompressed());
   
   
   private static final ExternalJavaScriptLoader aceSupportLoader_ =
         getLoader(AceResources.INSTANCE.acesupportjs());
   
   private static final ExternalJavaScriptLoader vimLoader_ =
         getLoader(AceResources.INSTANCE.keybindingVimJs(),
                   AceResources.INSTANCE.keybindingVimUncompressedJs());
   
   private static final ExternalJavaScriptLoader emacsLoader_ =
         getLoader(AceResources.INSTANCE.keybindingEmacsJs(),
                   AceResources.INSTANCE.keybindingEmacsUncompressedJs());
   
   private static final ExternalJavaScriptLoader extLanguageToolsLoader_ =
         getLoader(AceResources.INSTANCE.extLanguageTools(),
                   AceResources.INSTANCE.extLanguageToolsUncompressed());
   
   private boolean popupVisible_;

   private final DiagnosticsBackgroundPopup diagnosticsBgPopup_;

   private long lastCursorChangedTime_;
   private long lastModifiedTime_;
   private String yankedText_ = null;
   
   private int activeEditEventType_ = EditEvent.TYPE_NONE;
   
   private static AceEditor s_lastFocusedEditor = null;
   
   private final List<HandlerRegistration> editorEventListeners_;

}
