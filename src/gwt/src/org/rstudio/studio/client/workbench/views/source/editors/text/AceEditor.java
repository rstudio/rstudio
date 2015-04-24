/*
 * AceEditor.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.DynamicIFrame;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.model.EventBasedChangeTracker;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager.InitCompletionFilter;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.NullCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.output.lint.DiagnosticsBackgroundPopup;
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceClickEvent.Handler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.TokenPredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.WordIterable;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.*;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionEvent;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionHandler;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

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
         // Never complete if there's an active selection
         Range range = getSession().getSelection().getRange();
         if (!range.isEmpty())
            return false;
         
         // Don't consider Tab to be a completion if we're at the start of a
         // line (e.g. only zero or more whitespace characters between the
         // beginning of the line and the cursor)
         if (event != null && event.getKeyCode() != KeyCodes.KEY_TAB)
            return true;
         
         // Short-circuit if the user has explicitly opted in
         if (uiPrefs_.allowTabMultilineCompletion().getValue())
            return true;
         
         int col = range.getStart().getColumn();
         if (col == 0)
            return false;
         
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
         AceEditor.this.addFoldChangeHandler(new org.rstudio.studio.client.workbench.views.source.editors.text.events.FoldChangeEvent.Handler()
         {
            @Override
            public void onFoldChange(FoldChangeEvent event)
            {
               changed_ = true;
            }
         });
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
      aceLoader_.addCallback(new Callback()
      {
         public void onLoaded()
         {
            aceSupportLoader_.addCallback(new Callback()
            {
               public void onLoaded()
               {
                  extLanguageToolsLoader_.addCallback(new Callback() 
                  {
                     public void onLoaded()
                     {
                        vimLoader_.addCallback(new Callback()
                        {
                           public void onLoaded()
                           {
                              emacsLoader_.addCallback(new Callback()
                              {
                                 public void onLoaded()
                                 {
                                    if (command != null)
                                       command.execute();
                                 }
                              });
                           }
                        });
                     }  
                  });
               }
            });
         }
      });
   }

   @Inject
   public AceEditor()
   {
      widget_ = new AceEditorWidget();
      ElementIds.assignElementId(widget_.getElement(), 
                                 ElementIds.SOURCE_TEXT_EDITOR);

      completionManager_ = new NullCompletionManager();
      diagnosticsBgPopup_ = new DiagnosticsBackgroundPopup(this);
      
      RStudioGinjector.INSTANCE.injectMembers(this);

      widget_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         public void onValueChange(ValueChangeEvent<Void> evt)
         {
            ValueChangeEvent.fire(AceEditor.this, null);
         }
      });
      
      widget_.addFoldChangeHandler(new FoldChangeEvent.Handler()
      {
         @Override
         public void onFoldChange(FoldChangeEvent event)
         {
            AceEditor.this.fireEvent(new FoldChangeEvent());
         }
      });
      
      addCapturingKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            int mod = KeyboardShortcut.getModifierValue(event.getNativeEvent());
            if (mod == KeyboardShortcut.CTRL)
            {
               switch (event.getNativeKeyCode())
               {
                  case 'U':
                     event.preventDefault();
                     InputEditorUtil.yankBeforeCursor(AceEditor.this, true);
                     break;
                  case 'K':
                     event.preventDefault();
                     InputEditorUtil.yankAfterCursor(AceEditor.this, true);
                     break;
                  case 'Y':
                     event.preventDefault();
                     Position start = getSelectionStart();
                     InputEditorUtil.pasteYanked(AceEditor.this);
                     indentPastedRange(Range.fromPoints(start,
                                                        getSelectionEnd()));
                     break;
               }
            }

         }
      });

      addPasteHandler(new PasteEvent.Handler()
      {
         @Override
         public void onPaste(PasteEvent event)
         {
            if (completionManager_ != null)
               completionManager_.onPaste(event);
            
            final Position start = getSelectionStart();

            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  Range range = Range.fromPoints(start, getSelectionEnd());
                  indentPastedRange(range);
               }
            });
         }
      });
      
      // handle click events
      addAceClickHandler(new AceClickEvent.Handler()
      {    
         @Override
         public void onClick(AceClickEvent event)
         {    
            fixVerticalOffsetBug();
            if (DomUtils.isCommandClick(event.getNativeEvent()))
            {     
               // eat the event so ace doesn't do anything with it
               event.preventDefault();
               event.stopPropagation();
               
               // set the cursor position
               setCursorPosition(event.getDocumentPosition());
               
               // go to function definition
               fireEvent(new CommandClickEvent());
            }
            else
            {
               // if the focus in the Help pane or another iframe
               // we need to make sure to get it back
               WindowEx.get().focus();
            }
         }
      });

      lastCursorChangedTime_ = 0;
      addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            fixVerticalOffsetBug();
            clearLineHighlight();
            lastCursorChangedTime_ = System.currentTimeMillis();
         }
      });
      
      lastModifiedTime_ = 0;
      addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Void> event)
         {
            lastModifiedTime_ = System.currentTimeMillis();
            clearDebugLineHighlight();
         }
      });
   }
   
   private void indentPastedRange(Range range)
   {
      if (fileType_ == null ||
          !fileType_.canAutoIndent() || 
          !RStudioGinjector.INSTANCE.getUIPrefs().reindentOnPaste().getValue())
      {
         return;
      }

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

   @Inject
   void initialize(CodeToolsServerOperations server,
                   UIPrefs uiPrefs)
   {
      server_ = server;
      uiPrefs_ = uiPrefs;
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
      updateLanguage(completionManager);
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
   public void setRCompletionContext(RCompletionContext rContext)
   {
      rContext_ = rContext;
   }

   private void updateLanguage(boolean suppressCompletion)
   {
      if (fileType_ == null)
         return;
      
      CompletionManager completionManager;
      if (!suppressCompletion)
      {
         if (fileType_.getEditorLanguage().useRCompletion())
         {
            completionManager = new RCompletionManager(
                  this,
                  this,
                  new CompletionPopupPanel(),
                  server_,
                  new Filter(),
                  rContext_,
                  fileType_.canExecuteChunks() ? rnwContext_ : null,
                  this,
                  false);
            
            // if this is cpp then we use our own completion manager
            // that can optionally delegate to the R completion manager
            if (fileType_.isC() || fileType_.isRmd())
            {
               completionManager = new CppCompletionManager(
                                                     this,
                                                     new Filter(),
                                                     cppContext_,
                                                     completionManager);
            }
         }
         else
            completionManager = new NullCompletionManager();
      }
      else
         completionManager = new NullCompletionManager();
      
      updateLanguage(completionManager);
   }
   
   private void updateLanguage(CompletionManager completionManager)
   {
      clearLint();
      if (fileType_ == null)
         return;
      
      completionManager_ = completionManager;
      
      updateKeyboardHandlers();
      syncCompletionPrefs();
      syncDiagnosticsPrefs();
      
      getSession().setEditorMode(
            fileType_.getEditorLanguage().getParserName(),
            false);
      getSession().setUseWrapMode(fileType_.getWordWrap());
      syncWrapLimit();
   } 
   
   @Override
   public void syncCompletionPrefs()
   {
      if (fileType_ == null)
         return;
      
      boolean enabled = fileType_.getEditorLanguage().useAceLanguageTools();
      boolean live = uiPrefs_.codeCompleteOther().getValue().equals(
                                       UIPrefsAccessor.COMPLETION_ALWAYS);
      int characterThreshold = uiPrefs_.alwaysCompleteCharacters().getValue();
      int delay = uiPrefs_.alwaysCompleteDelayMs().getValue();
      widget_.getEditor().setCompletionOptions(
            enabled, 
            uiPrefs_.enableSnippets().getValue(), 
            live, 
            characterThreshold, 
            delay);
   }
   
   @Override
   public void syncDiagnosticsPrefs()
   {
      if (fileType_ == null)
         return;
      
      boolean useWorker = uiPrefs_.showDiagnosticsOther().getValue() &&
            fileType_.getEditorLanguage().useAceLanguageTools();

      getSession().setUseWorker(useWorker);
      getSession().setWorkerTimeout(
            uiPrefs_.backgroundDiagnosticsDelayMs().getValue());
   }
   
   private void syncWrapLimit()
   {
      // bail if there is no filetype yet
      if (fileType_ == null)
         return;
      
      // We originally observed that large word-wrapped documents
      // would cause Chrome on Liunx to freeze (bug #3207), eventually
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
      // (1) To fix the horizontal scrollbar problem we revereted 
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
      // scollbar problem on desktop as well
   }
   
   private void updateKeyboardHandlers()
   {
      // create a keyboard previewer for our special hooks
      AceKeyboardPreviewer previewer = new AceKeyboardPreviewer(
                                                         completionManager_);
      
      // reset keyboard handlers
      widget_.getEditor().setKeyboardHandler(null);
      
      // if required add vim handlers to main editor
      if (useVimMode_)
      {
         widget_.getEditor().addKeyboardHandler(KeyboardHandler.vim());
      }
      
      // add the previewer's handler
      widget_.getEditor().addKeyboardHandler(previewer.getKeyboardHandler());
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

      final AceEditorNative ed = widget_.getEditor();

      if (preserveCursorPosition)
      {
         final Position cursorPos;
         final int scrollTop, scrollLeft;

         cursorPos = ed.getSession().getSelection().getCursor();
         scrollTop = ed.getRenderer().getScrollTop();
         scrollLeft = ed.getRenderer().getScrollLeft();

         // Setting the value directly on the document prevents undo/redo
         // stack from being blown away
         widget_.getEditor().getSession().getDocument().setValue(code);

         ed.getSession().getSelection().moveCursorTo(cursorPos.getRow(),
                                                     cursorPos.getColumn(),
                                                     false);
         ed.getRenderer().scrollToY(scrollTop);
         ed.getRenderer().scrollToX(scrollLeft);
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               ed.getRenderer().scrollToY(scrollTop);
               ed.getRenderer().scrollToX(scrollLeft);
            }
         });
      }
      else
      {
         ed.getSession().setValue(code);
         ed.getSession().getSelection().moveCursorTo(0, 0, false);
      }
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
   
   public void scrollToY(int y)
   {
      widget_.getEditor().getRenderer().scrollToY(y);
   }
   
   public void insertCode(String code)
   {
      insertCode(code, false);
   }
   
   public void insertCode(String code, boolean blockMode)
   {
      widget_.getEditor().insert(code);
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
   
   public void goToFunctionDefinition()
   {
      completionManager_.goToFunctionDefinition();
   }

   class PrintIFrame extends DynamicIFrame
   {
      public PrintIFrame(String code, double fontSize)
      {
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
         Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
         {
            public boolean execute()
            {
               PrintIFrame.this.removeFromParent();
               return false;
            }
         }, 1000 * 60 * 5);
      }

      private final String code_;
      private final double fontSize_;
   }

   public void print()
   {
      PrintIFrame printIFrame = new PrintIFrame(
            getCode(),
            RStudioGinjector.INSTANCE.getUIPrefs().fontSize().getValue());
      RootPanel.get().add(printIFrame);
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

   public int getLength(int row)
   {
      return getSession().getDocument().getLine(row).length();
   }

   public int getRowCount()
   {
      return getSession().getDocument().getLength();
   }
   
   public String getLine(int row)
   {
      return getSession().getLine(row);
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
      
      return line.charAt(column);
   }
   
   public char getCharacterBeforeCursor()
   {
      Position cursorPos = getCursorPosition();
      int column = cursorPos.getColumn();
      if (column == 0)
         return '\0';
      
      String line = getLine(cursorPos.getRow());
      return line.charAt(column - 1);
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
      Range selRange = getSession().getSelection().getRange();
      Position position = getSession().replace(selRange, code);
      Range range = Range.fromPoints(selRange.getStart(), position);
      getSession().getSelection().setSelectionRange(range);
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
         ensureCursorVisible();
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
         String line = getSession().getLine(curRow);
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

   public AnchoredSelection createAnchoredSelection(Position startPos,
                                                    Position endPos)
   {
      Anchor start = Anchor.createAnchor(getSession().getDocument(),
                                         startPos.getRow(),
                                         startPos.getColumn());
      Anchor end = Anchor.createAnchor(getSession().getDocument(),
                                       endPos.getRow(),
                                       endPos.getColumn());
      return new AnchoredSelectionImpl(start, end);
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
      Scheduler.get().scheduleFinally(new RepeatingCommand()
      {
         public boolean execute()
         {
            widget_.onResize();
            widget_.onActivate();
            return false;
         }
      });
   }

   public void onVisibilityChanged(boolean visible)
   {
      if (visible)
         widget_.getEditor().getRenderer().updateFontSize();
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

   public void setUseSoftTabs(boolean on)
   {
      getSession().setUseSoftTabs(on);
   }

   /**
    * Warning: This will be overridden whenever the file type is set
    */
   public void setUseWrapMode(boolean useWrapMode)
   {
      getSession().setUseWrapMode(useWrapMode);
   }

   public void setTabSize(int tabSize)
   {
      getSession().setTabSize(tabSize);
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
      widget_.getEditor().getRenderer().setBlinkingCursor(blinking);
   }
   
   public void setShowPrintMargin(boolean on)
   {
      widget_.getEditor().getRenderer().setShowPrintMargin(on);
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
   
   public HandlerRegistration addEditorFocusHandler(FocusHandler handler)
   {
      return widget_.addFocusHandler(handler);
   }

   public Scope getCurrentScope()
   {
      return getSession().getMode().getCodeModel().getCurrentScope(
            getCursorPosition());
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

   public Scope getCurrentChunk()
   {
      return getCurrentChunk(getCursorPosition());
   }

   @Override
   public Scope getCurrentChunk(Position position)
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

   public void setCursorPosition(Position position)
   {
      getSession().getSelection().setSelectionRange(
            Range.fromPoints(position, position));
   }

   @Override
   public void moveCursorNearTop(int rowOffset)
   {
      int screenRow = getSession().documentToScreenRow(getCursorPosition());
      widget_.getEditor().scrollToRow(Math.max(0, screenRow - rowOffset));
   }
   
   @Override
   public void moveCursorNearTop()
   {
      moveCursorNearTop(7);
   }
   
   @Override
   public void ensureCursorVisible()
   {
      int screenRow = getSession().documentToScreenRow(getCursorPosition());     
      if (!widget_.getEditor().isRowFullyVisible(screenRow))
         moveCursorNearTop();
   }
   
   @Override
   public boolean isCursorInSingleLineString()
   {
      return StringUtil.isEndOfLineInRStringState(getCurrentLineUpToCursor());
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

   public boolean hasScopeTree()
   {
      return getSession().getMode().getCodeModel().hasScopes();
   }
   
   public JsArray<Scope> getScopeTree()
   {
      return getSession().getMode().getCodeModel().getScopeTree();
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
   public void jumpToMatching()
   {
      widget_.getEditor().jumpToMatching(false, false);
   }
   
   @Override
   public void selectToMatching()
   {
      widget_.getEditor().jumpToMatching(true, false);
   }
   
   @Override
   public void expandToMatching()
   {
      widget_.getEditor().jumpToMatching(true, true);
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
      navigateToPosition(position, recordCurrent, false);
   }
   
   @Override 
   public void navigateToPosition(SourcePosition position, 
                                  boolean recordCurrent,
                                  boolean highlightLine)
   {
      if (recordCurrent)
         recordCurrentNavigationPosition();

      navigate(position, true, highlightLine);
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
   
   private void navigate(SourcePosition srcPosition, boolean addToHistory)
   {
      navigate(srcPosition, addToHistory, false);
   }
   
   private void navigate(SourcePosition srcPosition, 
                         boolean addToHistory,
                         boolean highlightLine)
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
         scrollToY(srcPosition.getScrollPosition());
      else if (position.getRow() != previousCursorPos.getRow())
         moveCursorNearTop();
      else
         ensureCursorVisible();
      
      // set focus
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

   public HandlerRegistration addAceClickHandler(Handler handler)
   {
      return widget_.addAceClickHandler(handler);
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

   public void autoHeight()
   {
      widget_.autoHeight();
   }

   public void forceCursorChange()
   {
      widget_.forceCursorChange();
   }

   public void scrollToCursor(ScrollPanel scrollPanel,
                              int paddingVert,
                              int paddingHoriz)
   {
      DomUtils.ensureVisibleVert(
            scrollPanel.getElement(),
            widget_.getEditor().getRenderer().getCursorElement(),
            paddingVert);
      DomUtils.ensureVisibleHoriz(
            scrollPanel.getElement(),
            widget_.getEditor().getRenderer().getCursorElement(),
            paddingHoriz, paddingHoriz,
            false);
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
   public void showLint(JsArray<LintItem> lint)
   {
      widget_.showLint(lint);
   }
   
   @Override
   public void clearLint()
   {
      widget_.clearLint();
   }
   
   public Range createAnchoredRange(Position start,
                                    Position end)
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
   
   public void blockOutdent()
   {
      widget_.getEditor().blockOutdent();
   }
   
   private static final int DEBUG_CONTEXT_LINES = 2;
   private final HandlerManager handlers_ = new HandlerManager(this);
   private final AceEditorWidget widget_;
   private CompletionManager completionManager_;
   private CodeToolsServerOperations server_;
   private UIPrefs uiPrefs_;
   private TextFileType fileType_;
   private boolean passwordMode_;
   private boolean useVimMode_ = false;
   private RnwCompletionContext rnwContext_;
   private CppCompletionContext cppContext_;
   private RCompletionContext rContext_ = null;
   private Integer lineHighlightMarkerId_ = null;
   private Integer lineDebugMarkerId_ = null;
   private Integer executionLine_ = null;
   
   private static final ExternalJavaScriptLoader aceLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.acejs().getSafeUri().asString());
   private static final ExternalJavaScriptLoader aceSupportLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.acesupportjs().getSafeUri().asString());
   private static final ExternalJavaScriptLoader vimLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.keybindingVimJs().getSafeUri().asString());
   private static final ExternalJavaScriptLoader emacsLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.keybindingEmacsJs().getSafeUri().asString());
   private static final ExternalJavaScriptLoader extLanguageToolsLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.extLanguageTools().getSafeUri().asString());
   
   private boolean popupVisible_;
   
   @SuppressWarnings("unused")
   private final DiagnosticsBackgroundPopup diagnosticsBgPopup_;
   
   private long lastCursorChangedTime_;
   private long lastModifiedTime_;
   
}
