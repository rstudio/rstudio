/*
 * PythonCompletionManager.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.codesearch.model.DataDefinition;
import org.rstudio.studio.client.workbench.codesearch.model.FileFunctionDefinition;
import org.rstudio.studio.client.workbench.codesearch.model.ObjectDefinition;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.CompletionResult;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorLineWithCursorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.NavigableSourceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.RCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeFunction;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorCommandEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.CodeModel;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.DplyrJoinContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.RInfixData;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.r.RCompletionToolTip;
import org.rstudio.studio.client.workbench.views.source.editors.text.r.SignatureToolTipManager;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.PythonServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// NOTE: This is mostly just a fork of the RCompletionManager code.
// In a perfect world, there would be a lot more code shared between
// these classes, but unfortunately the RCompletionManager has become
// a bit of a mess and so the cleanest way forward (barring a rewrite
// of the completion class) was to just fork and change the bits we need.
public class PythonCompletionManager implements CompletionManager
{  
   // globally suppress F1 and F2 so no default browser behavior takes those
   // keystrokes (e.g. Help in Chrome)
   static
   {
      Event.addNativePreviewHandler(new NativePreviewHandler() {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            if (event.getTypeInt() == Event.ONKEYDOWN)
            {
               int keyCode = event.getNativeEvent().getKeyCode();
               if ((keyCode == 112 || keyCode == 113) &&
                   KeyboardShortcut.NONE ==
                      KeyboardShortcut.getModifierValue(event.getNativeEvent()))
               {
                 event.getNativeEvent().preventDefault();
               }
            }
         }
      });
   }
   
   public void onPaste(PasteEvent event)
   {
      popup_.hide();
   }
   
   public PythonCompletionManager(InputEditorDisplay input,
                                  NavigableSourceEditor navigableSourceEditor,
                                  CompletionPopupDisplay popup,
                                  CodeToolsServerOperations server,
                                  InitCompletionFilter initFilter,
                                  RCompletionContext rContext,
                                  RnwCompletionContext rnwContext,
                                  DocDisplay docDisplay,
                                  boolean isConsole)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      input_ = input ;
      navigableSourceEditor_ = navigableSourceEditor;
      popup_ = popup ;
      server_ = server ;
      rContext_ = rContext;
      initFilter_ = initFilter ;
      rnwContext_ = rnwContext;
      docDisplay_ = docDisplay;
      isConsole_ = isConsole;
      sigTipManager_ = new SignatureToolTipManager(docDisplay_);
      suggestTimer_ = new SuggestionTimer(this, uiPrefs_);
      snippets_ = new SnippetHelper((AceEditor) docDisplay, getSourceDocumentPath());
      requester_ = new CompletionRequester(rnwContext, docDisplay, snippets_);
      handlers_ = new HandlerRegistrations();
      completionCache_ = new CompletionCache();
      
      handlers_.add(input_.addBlurHandler(new BlurHandler() {
         public void onBlur(BlurEvent event)
         {
            if (!ignoreNextInputBlur_)
               invalidatePendingRequests() ;
            ignoreNextInputBlur_ = false ;
         }
      }));

      handlers_.add(input_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            invalidatePendingRequests();
         }
      }));

      handlers_.add(popup_.addSelectionCommitHandler(new SelectionCommitHandler<QualifiedName>() {
         public void onSelectionCommit(SelectionCommitEvent<QualifiedName> event)
         {
            assert context_ != null : "onSelection called but handler is null" ;
            if (context_ != null)
               context_.onSelection(event.getSelectedItem()) ;
         }
      }));
      
      handlers_.add(popup_.addSelectionHandler(new SelectionHandler<QualifiedName>() {
         public void onSelection(SelectionEvent<QualifiedName> event)
         {
            lastSelectedItem_ = event.getSelectedItem();
            if (popup_.isHelpVisible())
               context_.showHelp(lastSelectedItem_);
            else
               showHelpDeferred(context_, lastSelectedItem_, 600);
         }
      }));
      
      handlers_.add(popup_.addMouseDownHandler(new MouseDownHandler() {
         public void onMouseDown(MouseDownEvent event)
         {
            ignoreNextInputBlur_ = true ;
         }
      }));
      
      handlers_.add(popup_.addSelectionHandler(new SelectionHandler<QualifiedName>() {
         
         @Override
         public void onSelection(SelectionEvent<QualifiedName> event)
         {
            docDisplay_.setPopupVisible(true);
         }
      }));
      
      handlers_.add(popup_.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  docDisplay_.setPopupVisible(false);
               }
            });
         }
      }));
      
      handlers_.add(popup_.addAttachHandler(new AttachEvent.Handler()
      {
         private boolean wasSigtipShowing_ = false;
         
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            RCompletionToolTip toolTip = sigTipManager_.getToolTip();
            
            if (event.isAttached())
            {
               if (toolTip != null && toolTip.isShowing())
               {
                  wasSigtipShowing_ = true;
                  toolTip.setVisible(false);
               }
               else
               {
                  wasSigtipShowing_ = false;
               }
            }
            else
            {
               if (toolTip != null && wasSigtipShowing_)
                  toolTip.setVisible(true);
            }
         }
      }));
      
      // hide the autocompletion popup if the user executes
      // an Ace editor command (e.g. insert pipe operator)
      handlers_.add(eventBus_.addHandler(
            AceEditorCommandEvent.TYPE,
            new AceEditorCommandEvent.Handler()
            {
               @Override
               public void onEditorCommand(AceEditorCommandEvent event)
               {
                  invalidatePendingRequests();
                  close();
               }
            }));
      
   }
   
   @Inject
   public void initialize(GlobalDisplay globalDisplay,
                          FileTypeRegistry fileTypeRegistry,
                          EventBus eventBus,
                          HelpStrategy helpStrategy,
                          UIPrefs uiPrefs)
   {
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      eventBus_ = eventBus;
      helpStrategy_ = helpStrategy;
      uiPrefs_ = uiPrefs;
   }
   
   public void detach()
   {
      handlers_.removeHandler();
      sigTipManager_.detach();
      snippets_.detach();
      popup_.hide();
   }

   public void close()
   {
      popup_.hide();
   }
   
   public void codeCompletion()
   {
      if (initFilter_ == null || initFilter_.shouldComplete(null))
         beginSuggest(true, false, true);
   }
   
   @Override
   public void goToHelp()
   {
      // TODO
   }
   
   public void goToFunctionDefinition()
   {
      // TODO
   }
   
   public boolean previewKeyDown(NativeEvent event)
   {
      suggestTimer_.cancel();
      
      if (sigTipManager_.previewKeyDown(event))
         return true;
      
      if (isDisabled())
         return false;
      
      /**
       * KEYS THAT MATTER
       *
       * When popup not showing:
       * Tab - attempt completion (handled in Console.java)
       * 
       * When popup showing:
       * Esc - dismiss popup
       * Enter/Tab - accept current selection
       * Up-arrow/Down-arrow - change selected item
       * [identifier] - narrow suggestions--or if we're lame, just dismiss
       * All others - dismiss popup
       */
      
      nativeEvent_ = event;

      int keycode = event.getKeyCode();
      int modifier = KeyboardShortcut.getModifierValue(event);

      if (!popup_.isShowing())
      {
         // don't allow ctrl + space for completions in Emacs mode
         if (docDisplay_.isEmacsModeOn() && event.getKeyCode() == KeyCodes.KEY_SPACE)
            return false;
         
         if (CompletionUtils.isCompletionRequest(event, modifier))
         {
            if (initFilter_ == null || initFilter_.shouldComplete(event))
            {
               // TODO: Check document mode?
               return beginSuggest(true, false, true);
            }
         }
         else if (event.getKeyCode() == KeyCodes.KEY_TAB &&
                  modifier == KeyboardShortcut.SHIFT)
         {
            return snippets_.attemptSnippetInsertion(true);
         }
         else if (keycode == 112 // F1
                  && modifier == KeyboardShortcut.NONE)
         {
            goToHelp();
            return true;
         }
         else if (keycode == 113 // F2
                  && modifier == KeyboardShortcut.NONE)
         {
            goToFunctionDefinition();
            return true;
         }
      }
      else
      {
         // bail on modifier keys
         if (KeyboardHelper.isModifierKey(keycode))
            return false;
         
         // allow emacs-style navigation of popup entries
         if (modifier == KeyboardShortcut.CTRL)
         {
            switch (keycode)
            {
            case KeyCodes.KEY_P: return popup_.selectPrev();
            case KeyCodes.KEY_N: return popup_.selectNext();
            }
         }
         
         else if (modifier == KeyboardShortcut.NONE)
         {
            if (keycode == KeyCodes.KEY_ESCAPE)
            {
               invalidatePendingRequests() ;
               return true ;
            }
            
            // NOTE: It is possible for the popup to still be showing, but
            // showing offscreen with no values. We only grab these keys
            // when the popup is both showing, and has completions.
            // This functionality is here to ensure backspace works properly;
            // e.g "stats::rna" -> "stats::rn" brings completions if the user
            // had originally requested completions at e.g. "stats::".
            if (popup_.hasCompletions() && !popup_.isOffscreen())
            {
               if (keycode == KeyCodes.KEY_ENTER)
               {
                  QualifiedName value = popup_.getSelectedValue();
                  if (value != null)
                  {
                     context_.onSelection(value);
                     return true ;
                  }
               }
               
               else if (keycode == KeyCodes.KEY_TAB)
               {
                  QualifiedName value = popup_.getSelectedValue() ;
                  if (value != null)
                  {
                     if (value.type == RCompletionType.DIRECTORY)
                        context_.suggestOnAccept_ = true;
                     
                     context_.onSelection(value);
                     return true;
                  }
               }
               
               else if (keycode == KeyCodes.KEY_UP)
                  return popup_.selectPrev() ;
               else if (keycode == KeyCodes.KEY_DOWN)
                  return popup_.selectNext() ;
               else if (keycode == KeyCodes.KEY_PAGEUP)
                  return popup_.selectPrevPage() ;
               else if (keycode == KeyCodes.KEY_PAGEDOWN)
                  return popup_.selectNextPage() ;
               else if (keycode == KeyCodes.KEY_HOME)
                  return popup_.selectFirst();
               else if (keycode == KeyCodes.KEY_END)
                  return popup_.selectLast();
               
               if (keycode == 112) // F1
               {
                  context_.showHelpTopic() ;
                  return true ;
               }
               else if (keycode == 113) // F2
               {
                  goToFunctionDefinition();
                  return true;
               }
            }
            
         }
         
         if (canContinueCompletions(event))
            return false;
         
         // if we insert a '/', we're probably forming a directory --
         // pop up completions
         if (keycode == 191 && modifier == KeyboardShortcut.NONE)
         {
            input_.insertCode("/");
            return beginSuggest(true, true, false);
         }
         
         // continue showing completions on backspace
         if (keycode == KeyCodes.KEY_BACKSPACE && modifier == KeyboardShortcut.NONE &&
             !docDisplay_.inMultiSelectMode())
         {
            int cursorColumn = input_.getCursorPosition().getColumn();
            String currentLine = docDisplay_.getCurrentLine();
            
            // only suggest if the character previous to the cursor is a Python identifier
            // also halt suggestions if we're about to remove the only character on the line
            if (cursorColumn > 0)
            {
               // TODO: determine whether we really want to continue showing completions
               char ch = currentLine.charAt(cursorColumn - 2);
               
               boolean isAcceptableCharSequence =
                     isValidForPythonIdentifier(ch) ||
                     ch == '.' ||  // attribute completion
                     ch == '/';    // for file completions
               
               if (currentLine.length() > 0 &&
                     cursorColumn > 0 &&
                     isAcceptableCharSequence)
               {
                  // manually remove the previous character
                  InputEditorSelection selection = input_.getSelection();
                  InputEditorPosition start = selection.getStart().movePosition(-1, true);
                  InputEditorPosition end = selection.getStart();

                  if (currentLine.charAt(cursorColumn) == ')' && currentLine.charAt(cursorColumn - 1) == '(')
                  {
                     // flush cache as old completions no longer relevant
                     requester_.flushCache();
                     completionCache_.flush();
                     end = selection.getStart().movePosition(1, true);
                  }

                  input_.setSelection(new InputEditorSelection(start, end));
                  input_.replaceSelection("", false);
                  
                  return beginSuggest(false, false, false);
               }
            }
            else
            {
               invalidatePendingRequests();
               return true;
            }
         }
         
         invalidatePendingRequests();
         return false;
      }
      
      return false;
   }
   
   private boolean isValidForPythonIdentifier(char c) {
      return (c >= 'a' && c <= 'z') ||
             (c >= 'A' && c <= 'Z') ||
             (c >= '0' && c <= '9') ||
             (c == '_');
   }
   
   private boolean checkCanAutoPopup(char c, int lookbackLimit)
   {
      if (docDisplay_.isVimModeOn() &&
            !docDisplay_.isVimInInsertMode())
         return false;
      
      String currentLine = docDisplay_.getCurrentLine();
      Position cursorPos = input_.getCursorPosition();
      int cursorColumn = cursorPos.getColumn();
      
      // Don't auto-popup when the cursor is within a string
      if (docDisplay_.isCursorInSingleLineString())
         return false;
      
      // Don't auto-popup if there is a character following the cursor
      // (this implies an in-line edit and automatic popups are likely to
      // be annoying)
      if (isValidForPythonIdentifier(docDisplay_.getCharacterAtCursor()))
         return false;
      
      boolean canAutoPopup =
            currentLine.length() > lookbackLimit - 1 &&
            isValidForPythonIdentifier(c);
      
      if (isConsole_ && !uiPrefs_.alwaysCompleteInConsole().getValue())
         canAutoPopup = false;

      if (canAutoPopup)
      {
         for (int i = 0; i < lookbackLimit; i++)
         {
            if (!isValidForPythonIdentifier(currentLine.charAt(cursorColumn - i - 1)))
            {
               canAutoPopup = false;
               break;
            }
         }
      }

      return canAutoPopup;
      
   }
   
   public boolean previewKeyPress(char c)
   {
      suggestTimer_.cancel();
      
      if (isDisabled())
         return false;
      
      if (popup_.isShowing())
      {
         // If insertion of this character completes an available suggestion,
         // and is not a prefix match of any other suggestion, then implicitly
         // apply that.
         // TODO
         QualifiedName selectedItem = popup_.getSelectedValue();
         
         if (c == ' ')
            return false;
         
         // Always update the current set of completions following
         // a key insertion. Defer execution so the key insertion can
         // enter the document.
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               beginSuggest(false, true, false);
            }
         });
         return false;
         
      }
      else
      {
         // Bail if we're not in Python mode
         if (!DocumentMode.isCursorInPythonMode(docDisplay_))
            return false;
         
         // Bail if we're in a single-line string
         if (docDisplay_.isCursorInSingleLineString())
            return false;
         
         // if there's a selection, bail
         if (input_.hasSelection()) 
            return false;
         
         // Bail if there is an alpha-numeric character
         // following the cursor
         if (isValidForPythonIdentifier(docDisplay_.getCharacterAtCursor()))
            return false;
         
         // Perform an auto-popup if a set number of Python identifier characters
         // have been inserted (but only if the user has allowed it in prefs)
         boolean autoPopupEnabled =
               uiPrefs_.codeComplete().getValue() == UIPrefsAccessor.COMPLETION_ALWAYS;

         if (!autoPopupEnabled)
            return false;
         
         // Immediately display completions after '.'
         if (c == '.')
         {
            // Bail if we're in Vim but not in insert mode
            if (docDisplay_.isVimModeOn() &&
                !docDisplay_.isVimInInsertMode())
            {
               return false;
            }
            
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  beginSuggest(true, true, false);
               }
            });
            return false;
         }
         
         // Check for a valid number of Python identifier characters for autopopup
         boolean canAutoPopup = checkCanAutoPopup(c, uiPrefs_.alwaysCompleteCharacters().getValue() - 1);
         if (canAutoPopup)
         {
            // Delay suggestion to avoid auto-popup while the user is typing
            suggestTimer_.schedule(true, true, false);
         }
      }
      
      return false;
   }
   
   private static boolean canContinueCompletions(NativeEvent event)
   {
      if (event.getAltKey()
            || event.getCtrlKey()
            || event.getMetaKey())
      {
         return false;
      }
      
      int keyCode = event.getKeyCode();
      
      if (keyCode >= 'a' && keyCode <= 'z')
         return true;
      else if (keyCode >= 'A' && keyCode <= 'Z')
         return true;
      else if (keyCode == ' ')
         return false;
      else if (KeyboardHelper.isHyphen(event))
         return false;
      else if (KeyboardHelper.isUnderscore(event))
         return true;
      
      if (event.getShiftKey())
         return false;
      
      if (keyCode >= '0' && keyCode <= '9')
         return true;
      if (keyCode == 190) // period
         return true;
      
      return false ;
   }

   private void invalidatePendingRequests()
   {
      invalidatePendingRequests(true, true);
   }

   private void invalidatePendingRequests(boolean flushCache,
                                          boolean hidePopup)
   {
      invalidation_.invalidate();
      
      if (hidePopup && popup_.isShowing())
      {
         popup_.hide();
         popup_.clearHelp(false);
      }
      
      if (flushCache)
      {
         requester_.flushCache();
         completionCache_.flush();
      }
   }
   
   private boolean isLineInComment(String line)
   {
      return StringUtil.stripBalancedQuotes(line).contains("#");
   }
   
   /**
    * If false, the suggest operation was aborted
    */
   private boolean beginSuggest(boolean flushCache,
                                boolean implicit,
                                boolean canAutoInsert)
   {
      suggestTimer_.cancel();
      
      if (!input_.isSelectionCollapsed())
         return false;
      
      invalidatePendingRequests(flushCache, false);
      
      InputEditorSelection selection = input_.getSelection();
      if (selection == null)
         return false;
      
      String line = docDisplay_.getCurrentLineUpToCursor();
      
      // don't auto-complete within comments
      if (isLineInComment(line))
         return false;
      
      // don't autocomplete if the cursor lies within the text of a
      // multi-line string. the logic here isn't perfect (ideally, we'd detect
      // whether we're in the 'qstring' or 'qqstring' state), but this will catch
      // the majority of cases
      Token cursorToken = docDisplay_.getTokenAt(docDisplay_.getCursorPosition());
      if (cursorToken.hasType("string"))
      {
         String cursorTokenValue = cursorToken.getValue();
         boolean isSingleLineString =
               cursorTokenValue.startsWith("'") ||
               cursorTokenValue.startsWith("\"");
         if (!isSingleLineString)
            return false;
      }
      
      // don't auto-complete with tab on lines with only whitespace,
      // if the insertion character was a tab (unless the user has opted in)
      if (!uiPrefs_.allowTabMultilineCompletion().getValue())
      {
         if (nativeEvent_ != null &&
               nativeEvent_.getKeyCode() == KeyCodes.KEY_TAB)
            if (line.matches("^\\s*$"))
               return false;
      }
      
      context_ = new CompletionRequestContext(
            line,
            invalidation_.getInvalidationToken(),
            selection,
            canAutoInsert);
      
      if (completionCache_.satisfyRequest(line, context_))
         return true;
      
      server_.pythonGetCompletions(line, context_);
      return true;
   }
   
   /**
    * It's important that we create a new instance of this each time.
    * It maintains state that is associated with a completion request.
    */
   private final class CompletionRequestContext
         extends ServerRequestCallback<Completions>
   {
      public CompletionRequestContext(String line,
                                      Invalidation.Token token,
                                      InputEditorSelection selection,
                                      boolean canAutoAccept)
      {
         line_ = line;
         invalidationToken_ = token;
         selection_ = selection;
         canAutoAccept_ = canAutoAccept;
      }
      
      public void showHelp(QualifiedName selectedItem)
      {
         // TODO
      }
      
      public void showHelpTopic()
      {
         // TODO
      }

      @Override
      public void onError(ServerError error)
      {
         if (invalidationToken_.isInvalid())
            return;
         
         popup_.showErrorMessage(
               error.getUserMessage(), 
               new PopupPositioner(input_.getCursorBounds(), popup_));
      }

      @Override
      public void onResponseReceived(Completions completions)
      {
         if (invalidationToken_.isInvalid())
            return;
         
         // cache completions
         completionCache_.store(line_, completions);
         
         // translate to array of qualified names
         int n = completions.getCompletions().length();
         QualifiedName[] results = new QualifiedName[n];
         for (int i = 0; i < n; i++)
         {
            results[i] = new QualifiedName(
                  completions.getCompletions().get(i),
                  completions.getPackages().get(i),
                  false,
                  completions.getType().get(i),
                  completions.getHelpHandler());
         }
         
         if (results.length == 0)
         {
            popup_.clearCompletions();
            boolean lastInputWasTab =
                  (nativeEvent_ != null && nativeEvent_.getKeyCode() == KeyCodes.KEY_TAB);
            
            boolean lineIsWhitespace = docDisplay_.getCurrentLine().matches("^\\s*$");
            
            if (lastInputWasTab && lineIsWhitespace)
            {
               docDisplay_.insertCode("\t");
               return;
            }
            
            if (canAutoAccept_)
            {
               popup_.showErrorMessage(
                     "(No matches)", 
                     new PopupPositioner(input_.getCursorBounds(), popup_));
            }
            else
            {
               // Show an empty popup message offscreen -- this is a hack to
               // ensure that we can get completion results on backspace after a
               // failed completion
               popup_.placeOffscreen();
            }
            
            return ;
         }
         
         // If there is only one result and the name is identical to the
         // current token, then implicitly accept that completion. we hide
         // the popup to ensure that backspace can re-load completions from
         // the cache
         if (results.length == 1 &&
             StringUtil.equals(completions.getToken(), results[0].name))
         {
            // For snippets we need to apply the completion if explicitly requested
            if (results[0].type == RCompletionType.SNIPPET && canAutoAccept_)
            {
               snippets_.applySnippet(completions.getToken(), results[0].name);
               return;
            }
            
            popup_.placeOffscreen();
            return;
         }

         // Move range to beginning of token; we want to place the popup there.
         final String token = completions.getToken();

         Rectangle rect = input_.getPositionBounds(
               selection_.getStart().movePosition(-token.length(), true));

         token_ = token;
         suggestOnAccept_ = completions.getSuggestOnAccept();
         overrideInsertParens_ = completions.getOverrideInsertParens();

         if (results.length == 1
               && canAutoAccept_
               && results[0].type != RCompletionType.DIRECTORY)
         {
            onSelection(results[0]);
         }
         else
         {
            popup_.showCompletionValues(
                  results,
                  new PopupPositioner(rect, popup_),
                  false);
         }
      }
      
      private void onSelection(QualifiedName qname)
      {
         suggestTimer_.cancel();
         final String value = qname.name;
         
         if (invalidationToken_.isInvalid())
            return;
         
         requester_.flushCache();
         completionCache_.flush();
         helpStrategy_.clearCache();
         
         if (value == null)
         {
            assert false : "Selected comp value is null";
            return;
         }

         applyValue(qname);
         
         popup_.hide() ;
         popup_.clearHelp(false);
         popup_.setHelpVisible(false);
         docDisplay_.setFocus(true);
         
      }
      
      private void applyValue(final QualifiedName qualifiedName)
      {
         String completionToken = token_;
         
         // Strip off the quotes for string completions.
         if (completionToken.startsWith("'") || completionToken.startsWith("\""))
            completionToken = completionToken.substring(1);
         
         if (qualifiedName.type == RCompletionType.SNIPPET)
         {
            snippets_.applySnippet(completionToken, qualifiedName.name);
            return;
         }
         
         boolean insertParen =
               uiPrefs_.insertParensAfterFunctionCompletion().getValue() &&
               RCompletionType.isFunctionType(qualifiedName.type);
         
         // TODO: don't insert a parenthesis if there is already a
         // '(' following the cursor
         AceEditor editor = (AceEditor) input_;
         boolean textFollowingCursorIsOpenParen = false;
         boolean textFollowingCursorIsClosingParen = false;

         String value = qualifiedName.name;
         String source = qualifiedName.source;
         boolean shouldQuote = qualifiedName.shouldQuote;
         if (qualifiedName.type == RCompletionType.DIRECTORY)
            value = value + "/";
         
         // There might be multiple cursors. Get the position of each cursor.
         Range[] ranges = editor.getNativeSelection().getAllRanges();
         
         // Determine the replacement value.
         boolean shouldInsertParens =
               insertParen &&
               !overrideInsertParens_ &&
               !textFollowingCursorIsOpenParen;
         
         boolean insertMatching = uiPrefs_.insertMatching().getValue();
         boolean needToMoveCursorInsideParens = false;
         if (shouldInsertParens)
         {
            // Munge the value -- determine whether we want to append '()' 
            // for e.g. function completions, and so on.
            if (textFollowingCursorIsClosingParen || !insertMatching)
            {
               value = value + "(";
            }
            else
            {
               value = value + "()";
               needToMoveCursorInsideParens = true;
            }
         }
         else
         {
            if (shouldQuote)
               value = "\"" + value + "\"";

            // don't add spaces around equals if requested
            final String kSpaceEquals = " = ";
            if (!uiPrefs_.insertSpacesAroundEquals().getValue() &&
                  value.endsWith(kSpaceEquals))
            {
               value = value.substring(0, value.length() - kSpaceEquals.length()) + "=";
            }
         }
         
         // Loop over all of the active cursors, and replace.
         for (Range range : ranges)
         {
            // We should be typing, and so each range should just define a
            // cursor position. Take those positions, construct ranges, replace
            // text in those ranges, and proceed.
            Position replaceEnd = range.getEnd();
            Position replaceStart = Position.create(
                  replaceEnd.getRow(),
                  replaceEnd.getColumn() - completionToken.length());
            
            editor.replaceRange(
                  Range.fromPoints(replaceStart, replaceEnd),
                  value);
            
         }
         
         // Set the active selection, and update the token.
         token_ = value;
         selection_ = input_.getSelection();
         
         // Move the cursor(s) back inside parens if necessary.
         if (needToMoveCursorInsideParens)
            editor.moveCursorLeft();
         
         if (RCompletionType.isFunctionType(qualifiedName.type))
            sigTipManager_.displayToolTip(qualifiedName.name, 
                                          qualifiedName.source,
                                          qualifiedName.helpHandler);
      }
      
      private final Invalidation.Token invalidationToken_ ;
      private final String line_;
      private InputEditorSelection selection_ ;
      private final boolean canAutoAccept_;
      private boolean suggestOnAccept_;
      private boolean overrideInsertParens_;
      
   }
   
   private String getSourceDocumentPath()
   {
      if (rContext_ == null)
         return "";
      else
         return StringUtil.notNull(rContext_.getPath());
   }
   
   private String getSourceDocumentId()
   {
      if (rContext_ != null)
         return StringUtil.notNull(rContext_.getId());
      else
         return "";
   }
   
   public void showHelpDeferred(final CompletionRequestContext context,
                                final QualifiedName item,
                                int milliseconds)
   {
      if (helpRequest_ != null && helpRequest_.isRunning())
         helpRequest_.cancel();
      
      helpRequest_ = new Timer() {
         @Override
         public void run()
         {
            if (item.equals(lastSelectedItem_) && popup_.isShowing())
               context.showHelp(item);
         }
      };
      helpRequest_.schedule(milliseconds);
   }
   
   private boolean isDisabled()
   {
      // Disable the completion manager while a snippet tabstop
      // manager is active
      if (docDisplay_.isSnippetsTabStopManagerActive())
         return true;
      
      return false;
   }
   
   private GlobalDisplay globalDisplay_;
   private FileTypeRegistry fileTypeRegistry_;
   private EventBus eventBus_;
   private HelpStrategy helpStrategy_;
   private UIPrefs uiPrefs_;

   private final CodeToolsServerOperations server_;
   private final InputEditorDisplay input_;
   private final NavigableSourceEditor navigableSourceEditor_;
   private final CompletionPopupDisplay popup_;
   private final CompletionRequester requester_;
   private final InitCompletionFilter initFilter_;
   private final CompletionCache completionCache_;
   
   // Prevents completion popup from being dismissed when you merely
   // click on it to scroll.
   private boolean ignoreNextInputBlur_ = false;
   private String token_ ;
   
   private final DocDisplay docDisplay_;
   private final SnippetHelper snippets_;
   private final boolean isConsole_;

   private final Invalidation invalidation_ = new Invalidation();
   private CompletionRequestContext context_ ;
   private final RCompletionContext rContext_;
   private final RnwCompletionContext rnwContext_;
   
   private final SignatureToolTipManager sigTipManager_;
   
   private NativeEvent nativeEvent_;
   
   private QualifiedName lastSelectedItem_;
   private Timer helpRequest_;
   private final SuggestionTimer suggestTimer_;
   
   private static class SuggestionTimer
   {
      SuggestionTimer(PythonCompletionManager manager, UIPrefs uiPrefs)
      {
         manager_ = manager;
         uiPrefs_ = uiPrefs;
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               manager_.beginSuggest(
                     flushCache_,
                     implicit_,
                     canAutoInsert_);
            }
         };
      }
      
      public void schedule(boolean flushCache,
                           boolean implicit,
                           boolean canAutoInsert)
      {
         flushCache_ = flushCache;
         implicit_ = implicit;
         canAutoInsert_ = canAutoInsert;
         timer_.schedule(uiPrefs_.alwaysCompleteDelayMs().getValue());
      }
      
      public void cancel()
      {
         timer_.cancel();
      }
      
      private final PythonCompletionManager manager_;
      private final UIPrefs uiPrefs_;
      private final Timer timer_;
      
      private boolean flushCache_;
      private boolean implicit_;
      private boolean canAutoInsert_;
      
   }
   
   private final HandlerRegistrations handlers_;
}
