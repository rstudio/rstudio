/*
 * RCompletionManager.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.JsVector;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.EventProperty;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.codesearch.model.DataDefinition;
import org.rstudio.studio.client.workbench.codesearch.model.FileFunctionDefinition;
import org.rstudio.studio.client.workbench.codesearch.model.ObjectDefinition;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.CompletionResult;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorLineWithCursorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.EditorBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.NavigableSourceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeFunction;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorCommandEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceGhostText;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.CodeModel;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.RInfixData;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.r.RCompletionToolTip;
import org.rstudio.studio.client.workbench.views.source.editors.text.r.SignatureToolTipManager;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class RCompletionManager implements CompletionManager
{  
   public void onPaste(PasteEvent event)
   {
      popup_.hide();
   }
   
   public RCompletionManager(InputEditorDisplay input,
                             NavigableSourceEditor navigableSourceEditor,
                             CompletionPopupDisplay popup,
                             CodeToolsServerOperations server,
                             InitCompletionFilter initFilter,
                             CompletionContext rContext,
                             RnwCompletionContext rnwContext,
                             DocDisplay docDisplay,
                             EditorBehavior behavior)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      input_ = input;
      navigableSourceEditor_ = navigableSourceEditor;
      popup_ = popup;
      server_ = server;
      rContext_ = rContext;
      initFilter_ = initFilter;
      rnwContext_ = rnwContext;
      docDisplay_ = docDisplay;
      behavior_ = behavior;
      sigTipManager_ = new SignatureToolTipManager(docDisplay_)
      {
         @Override
         protected boolean isEnabled(Position position)
         {
            return DocumentMode.isPositionInRMode(docDisplay_, position);
         }
      };
      
      suggestTimer_ = new SuggestionTimer(this, userPrefs_);
      snippets_ = new SnippetHelper((AceEditor) docDisplay, getSourceDocumentPath());
      requester_ = new CompletionRequester(rContext, rnwContext, docDisplay, snippets_);
      handlers_ = new HandlerRegistrations();
      
      handlers_.add(input_.addClickHandler(event ->
      {
         invalidatePendingRequests();
      }));

      handlers_.add(popup_.addSelectionCommitHandler(event ->
      {
         assert context_ != null : "onSelection called but handler is null";
         if (context_ != null)
            context_.onSelection(event.getSelectedItem());
      }));
      
      handlers_.add(popup_.addSelectionHandler(event ->
      {
         lastSelectedItem_ = event.getSelectedItem();
         if (popup_.isHelpVisible())
            context_.showHelp(lastSelectedItem_);
         else
            showHelpDeferred(context_, lastSelectedItem_, 600);
      }));
      
      handlers_.add(popup_.addSelectionHandler(event -> docDisplay_.setPopupVisible(true)));
      
      handlers_.add(popup_.addCloseHandler(event ->
      {
         Scheduler.get().scheduleDeferred(() -> docDisplay_.setPopupVisible(false));
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
      handlers_.add(eventBus_.addHandler(AceEditorCommandEvent.TYPE, event ->
      {
         invalidatePendingRequests();
         close();
      }));
   }
   
   @Inject
   public void initialize(GlobalDisplay globalDisplay,
                          FileTypeRegistry fileTypeRegistry,
                          EventBus eventBus,
                          HelpStrategy helpStrategy,
                          UserPrefs uiPrefs,
                          Copilot copilot)
   {
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      eventBus_ = eventBus;
      helpStrategy_ = helpStrategy;
      userPrefs_ = uiPrefs;
      copilot_ = copilot;
   }
   
   public void detach()
   {
      handlers_.removeHandler();
      sigTipManager_.detach();
      snippets_.detach();
      suggestTimer_.cancel();
      invalidation_.invalidate();
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
   
   public void goToHelp()
   {
      InputEditorLineWithCursorPosition linePos = 
            InputEditorUtil.getLineWithCursorPosition(input_);

      server_.getHelpAtCursor(
            linePos.getLine(), linePos.getPosition(),
            new SimpleRequestCallback<>(constants_.helpCaption()));
   }
   
   public void goToDefinition()
   {   
      // check for a file-local definition (intra-file navigation -- using
      // the active scope tree)
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor != null)
      {
         TokenCursor cursor = editor.getSession().getMode().getRCodeModel().getTokenCursor();
         if (cursor.moveToPosition(editor.getCursorPosition(), true))
         {
            // if the cursor is 'on' a left bracket, move back to the associated
            // token (ostensibly a function name)
            if (cursor.isLeftBracket())
               cursor.moveToPreviousToken();
            
            // if the previous token is an extraction operator, we shouldn't
            // navigate (as this isn't the 'full' function name)
            if (cursor.moveToPreviousToken())
            {
               if (cursor.isExtractionOperator())
                  return;
               
               cursor.moveToNextToken();
            }
            
            // if this is a string, try resolving that string as a file name
            if (cursor.hasType("string"))
            {
               String tokenValue = cursor.currentValue();
               String path = StringUtil.substring(tokenValue, 1, tokenValue.length() - 1);
               FileSystemItem filePath = FileSystemItem.createFile(path);
               
               // This will show a dialog error if no such file exists; this
               // seems the most appropriate action in such a case.
               fileTypeRegistry_.editFile(filePath);
            }
            
            String functionName = cursor.currentValue();
            JsArray<ScopeFunction> scopes =
                  editor.getAllFunctionScopes();

            for (int i = 0; i < scopes.length(); i++)
            {
               ScopeFunction scope = scopes.get(i);
               if (scope.getFunctionName() == functionName)
               {
                  navigableSourceEditor_.navigateToPosition(
                        SourcePosition.create(scope.getPreamble().getRow(),
                              scope.getPreamble().getColumn()),
                              true);
                  return;
               }
            }
         }
      }
      
      // intra-file navigation failed -- hit the server and find a definition
      // in the project index
      
      // determine current line and cursor position
      InputEditorLineWithCursorPosition lineWithPos = 
                      InputEditorUtil.getLineWithCursorPosition(input_);
      
      // delayed progress indicator
      final GlobalProgressDelayer progress = new GlobalProgressDelayer(
            globalDisplay_, 1000, constants_.searchingForDefinitionMessage());
      
      server_.getObjectDefinition(
         lineWithPos.getLine(),
         lineWithPos.getPosition(), 
         new ServerRequestCallback<ObjectDefinition>() {
            @Override
            public void onResponseReceived(ObjectDefinition def)
            {
                // dismiss progress
                progress.dismiss();
                    
                // if we got a hit
                if (def.getObjectName() != null)
                {   
                   // search locally if a function navigator was provided
                   if (navigableSourceEditor_ != null)
                   {
                      // try to search for the function locally
                      SourcePosition position = 
                         navigableSourceEditor_.findFunctionPositionFromCursor(
                                                         def.getObjectName());
                      if (position != null)
                      {
                         navigableSourceEditor_.navigateToPosition(position, 
                                                                   true);
                         return; // we're done
                      }

                   }
                   
                   // if we didn't satisfy the request using a function
                   // navigator and we got a file back from the server then
                   // navigate to the file/loc
                   if (def.getObjectType() == 
                         FileFunctionDefinition.OBJECT_TYPE)
                   {  
                      FileFunctionDefinition fileDef = 
                            def.getObjectData().cast();
                      fileTypeRegistry_.editFile(fileDef.getFile(), 
                                                 fileDef.getPosition());
                   }
                   
                   // if we didn't get a file back see if we got a 
                   // search path definition
                   else if (def.getObjectType() ==
                              SearchPathFunctionDefinition.OBJECT_TYPE)
                   {
                      SearchPathFunctionDefinition searchDef = 
                            def.getObjectData().cast();
                      eventBus_.fireEvent(
                            new CodeBrowserNavigationEvent(searchDef));
                   }
                   
                   // finally, check to see if it's a data frame
                   else if (def.getObjectType() == DataDefinition.OBJECT_TYPE)
                   {
                      eventBus_.fireEvent(new SendToConsoleEvent(
                            "View(" + def.getObjectName() + ")", true, false));
                   }
                }
            }

            @Override
            public void onError(ServerError error)
            {
               progress.dismiss();
               
               globalDisplay_.showErrorMessage(constants_.errorSearchingForFunctionMessage(),
                                               error.getUserMessage());
            }
         });
   }
   
   public boolean previewKeyDown(NativeEvent event)
   {
      suggestTimer_.cancel();
      
      if (sigTipManager_.previewKeyDown(event))
         return true;
      
      if (isDisabled())
         return false;
      
      /*
       * KEYS THAT MATTER
       *
       * When popup not showing:
       * Tab - attempt completion (handled in Console.java)
       * 
       * When popup showing:
       * Esc - dismiss popup
       * Ctrl+Esc - variation of Esc triggered by Ctrl+[ on iPadOS 13.1
       * Enter/Tab - accept current selection
       * Up-arrow/Down-arrow - change selected item
       * [identifier] - narrow suggestions--or if we're lame, just dismiss
       * All others - dismiss popup
       */
      
      nativeEvent_ = event;

      int keycode = event.getKeyCode();
      int modifier = KeyboardShortcut.getModifierValue(event);
 
      // Handle ghost text. Ghost text handlers get higher priority over other
      // completion handlers.
      if (docDisplay_.hasGhostText())
      {
         // Let tab insert ghost text if configured to do so
         if (keycode == KeyCodes.KEY_TAB && modifier == KeyboardShortcut.NONE)
         {
            invalidatePendingRequests();
            docDisplay_.applyGhostText();
            docDisplay_.scrollCursorIntoViewIfNecessary();
            return true;
         }

         // Check if the user just inserted some text matching the current
         // ghost text. If so, we'll suppress the next cursor change handler,
         // so we can continue presenting the current ghost text.
         String key = EventProperty.key(event);
         AceGhostText ghostText = docDisplay_.getGhostText();
         if (ghostText.text.startsWith(key))
         {
            String newGhostText = StringUtil.substring(ghostText.text, 1);
            docDisplay_.insertCode(key);
            docDisplay_.setGhostText(newGhostText);
            return true;
         }
      }
      
      if (!popup_.isShowing())
      {
         // don't allow ctrl + space for completions in Emacs mode
         if (docDisplay_.isEmacsModeOn() && event.getKeyCode() == KeyCodes.KEY_SPACE)
            return false;
         
         if (CompletionUtils.isCompletionRequest(event, modifier))
         {
            if (initFilter_ == null || initFilter_.shouldComplete(event))
            {
               String currentLine = docDisplay_.getCurrentLineUpToCursor();
               
               // If we're in markdown mode, only autocomplete in '```{r',
               // '[](', or '`r |' contexts
               if (DocumentMode.isCursorInMarkdownMode(docDisplay_))
               {
                  if (!(Pattern.create("^```{[rR]").test(currentLine) ||
                      Pattern.create(".*\\[.*\\]\\(").test(currentLine) ||
                      (Pattern.create(".*`r").test(currentLine) &&
                            StringUtil.countMatches(currentLine, '`') % 2 == 1)))
                     return false;
               }
               
               // If we're in tex mode, only provide completions in chunks
               if (DocumentMode.isCursorInTexMode(docDisplay_))
               {
                  if (!Pattern.create("^<<").test(currentLine))
                     return false;
               }

               return beginSuggest(true, false, true);
            }
         }
         else if (event.getKeyCode() == KeyCodes.KEY_TAB &&
                  modifier == KeyboardShortcut.SHIFT && !userPrefs_.tabKeyMoveFocus().getValue())
         {
            return snippets_.attemptSnippetInsertion(true);
         }
         else if (keycode == KeyCodes.KEY_F1
                  && modifier == KeyboardShortcut.NONE)
         {
            goToHelp();
            return true;
         }
         else if (keycode == KeyCodes.KEY_F2
                  && modifier == KeyboardShortcut.NONE)
         {
            goToDefinition();
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

            // Typing Ctrl+[ on iPadOS keyboard sends equivalent of Ctrl+Esc, but want to treat
            // as plain Esc keypress
            case KeyCodes.KEY_ESCAPE: invalidatePendingRequests(); return true;
            }
         }
         
         else if (modifier == KeyboardShortcut.NONE)
         {
            if (keycode == KeyCodes.KEY_ESCAPE)
            {
               invalidatePendingRequests();
               return true;
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
                     return true;
                  }
               }
               
               else if (keycode == KeyCodes.KEY_TAB)
               {
                  QualifiedName value = popup_.getSelectedValue();
                  if (value != null)
                  {
                     if (value.type == RCompletionType.DIRECTORY)
                        value = value.withSuggestOnAccept();
                     
                     context_.onSelection(value);
                     return true;
                  }
               }
               
               else if (keycode == KeyCodes.KEY_UP)
                  return popup_.selectPrev();
               else if (keycode == KeyCodes.KEY_DOWN)
                  return popup_.selectNext();
               else if (keycode == KeyCodes.KEY_PAGEUP)
                  return popup_.selectPrevPage();
               else if (keycode == KeyCodes.KEY_PAGEDOWN)
                  return popup_.selectNextPage();
               else if (keycode == KeyCodes.KEY_HOME)
                  return popup_.selectFirst();
               else if (keycode == KeyCodes.KEY_END)
                  return popup_.selectLast();
               
               if (keycode == KeyCodes.KEY_F1)
               {
                  context_.showHelpTopic();
                  return true;
               }
               else if (keycode == KeyCodes.KEY_F2)
               {
                  goToDefinition();
                  return true;
               }
            }
            
         }
         
         if (canContinueCompletions(event))
            return false;
         
         // if we insert a '/', we're probably forming a directory --
         // pop up completions
         if (modifier == KeyboardShortcut.NONE &&
             StringUtil.equals(EventProperty.key(event), "/"))
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
            
            // only suggest if the character previous to the cursor is an R identifier
            // also halt suggestions if we're about to remove the only character on the line
            if (cursorColumn > 0)
            {
               char ch = StringUtil.charAt(currentLine, cursorColumn - 2);
               char prevCh = StringUtil.charAt(currentLine, cursorColumn - 3);
               
               boolean isAcceptableCharSequence = isValidForRIdentifier(ch) ||
                     (ch == ':' && prevCh == ':') ||
                     ch == '$' ||
                     ch == '@' ||
                     ch == '/'; // for file completions
               
               if (currentLine.length() > 0 &&
                     cursorColumn > 0 &&
                     isAcceptableCharSequence)
               {
                  // manually remove the previous character
                  InputEditorSelection selection = input_.getSelection();
                  InputEditorPosition start = selection.getStart().movePosition(-1, true);
                  InputEditorPosition end = selection.getStart();

                  if (StringUtil.charAt(currentLine, cursorColumn) == ')' &&
                        StringUtil.charAt(currentLine, cursorColumn - 1) == '(')
                  {
                     // flush cache as old completions no longer relevant
                     requester_.flushCache();
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
   
   private boolean isAutoPopupEnabled()
   {
      return userPrefs_.codeCompletion().getValue() == UserPrefs.CODE_COMPLETION_ALWAYS;
   }
   
   private boolean isValidForRIdentifier(char c)
   {
      return (c >= 'a' && c <= 'z') ||
             (c >= 'A' && c <= 'Z') ||
             (c >= '0' && c <= '9') ||
             (c == '.') ||
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
      if (docDisplay_.isCursorInSingleLineString(false))
         return false;
      
      // Don't auto-popup if there is a character following the cursor
      // (this implies an in-line edit and automatic popups are likely to
      // be annoying)
      if (isValidForRIdentifier(docDisplay_.getCharacterAtCursor()))
         return false;
      
      boolean canAutoPopup =
            (currentLine.length() > lookbackLimit - 1 && isValidForRIdentifier(c));
      
      if (behavior_ == EditorBehavior.AceBehaviorConsole && 
            !userPrefs_.consoleCodeCompletion().getValue())
         canAutoPopup = false;

      if (canAutoPopup)
      {
         for (int i = 0; i < lookbackLimit; i++)
         {
            if (!isValidForRIdentifier(StringUtil.charAt(currentLine, cursorColumn - i - 1)))
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
         QualifiedName selectedItem =
               popup_.getSelectedValue();
         
         // NOTE: We should strip off trailing colons so that in-line edits of
         // package completions, e.g.
         //
         //     <foo>::
         //
         // can also dismiss the popup on a perfect match of <foo>.
         if (selectedItem != null &&
               selectedItem.name.replaceAll(":",  "").equals(token_ + c))
         {
            String fullToken = token_ + c;
            
            // Find prefix matches -- there should only be one if we really
            // want this behaviour (ie the current selection)
            int prefixMatchCount = 0;
            QualifiedName[] items = popup_.getItems();
            for (QualifiedName item : items)
            {
               if (item.name.startsWith(fullToken))
               {
                  ++prefixMatchCount;
                  if (prefixMatchCount > 1)
                     break;
               }
            }
            
            if (prefixMatchCount == 1)
            {
               // We place the completion list offscreen to ensure that
               // backspace events are handled later. 
               popup_.placeOffscreen();
               return false;
            }
         }
         
         if (c == ':')
         {
            invalidatePendingRequests();
            suggestTimer_.schedule(false, true, false);
            return false;
         }
         
         // When inserting a space, continue showing the current set of
         // completions and update the current completion token.
         if (c == ' ')
            return false;
         
         // Always update the current set of completions following
         // a key insertion. Defer execution so the key insertion can
         // enter the document.
         Scheduler.get().scheduleDeferred(() -> beginSuggest(false, true, false));
         return false;
      }
      else
      {
         // Bail if we're not in R mode
         if (!DocumentMode.isCursorInRMode(docDisplay_))
            return false;
         
         // Bail if we're in a single-line string
         if (docDisplay_.isCursorInSingleLineString(false))
            return false;
         
         // if there's a selection, bail
         if (input_.hasSelection()) 
            return false;
         
         // Bail if there is an alpha-numeric character
         // following the cursor
         if (isValidForRIdentifier(docDisplay_.getCharacterAtCursor()))
            return false;
         
         // Perform an auto-popup if a set number of R identifier characters
         // have been inserted (but only if the user has allowed it in prefs)
         if (!isAutoPopupEnabled())
            return false;
         
         // Immediately display completions after '$', '::', etc.
         if (input_.getCursorPosition().getColumn() > 0)
         {
            char prevChar = StringUtil.charAt(docDisplay_.getCurrentLine(),
                  input_.getCursorPosition().getColumn() - 1);
            if (
                  (c == ':' && prevChar == ':') ||
                        (c == '$') ||
                        (c == '@')
            )
            {
               // Bail if we're in Vim but not in insert mode
               if (docDisplay_.isVimModeOn() &&
                     !docDisplay_.isVimInInsertMode())
               {
                  return false;
               }
      
               Scheduler.get().scheduleDeferred(() -> beginSuggest(true, true, false));
               return false;
            }
         }
         
         // Check for a valid number of R identifier characters for autopopup
         boolean canAutoPopup = checkCanAutoPopup(c, userPrefs_.codeCompletionCharacters().getValue() - 1);
         
         // Attempt to pop up completions immediately after a function call.
         if (c == '(' && !isLineInComment(docDisplay_.getCurrentLine()))
         {
            // further validate that we're not working within a string
            // https://github.com/rstudio/rstudio/issues/8677
            Token currentToken = docDisplay_.getTokenAt(docDisplay_.getCursorPosition());
            if (currentToken != null && currentToken.hasType("identifier"))
            {
               String token = StringUtil.getToken(
                     docDisplay_.getCurrentLine(),
                     input_.getCursorPosition().getColumn(),
                     "[" + RegexUtil.wordCharacter() + "._]",
                     false,
                     true);
               
               if (token.matches("^(library|require|requireNamespace|data)\\s*$"))
                  canAutoPopup = true;
               
               Scheduler.get().scheduleDeferred(() -> 
               {
                  sigTipManager_.resolveActiveFunctionAndDisplayToolTip();
               });
            }
         }
         
         if (
               (canAutoPopup) ||
               isSweaveCompletion(c))
         {
            // Delay suggestion to avoid auto-popup while the user is typing
            invalidatePendingRequests();
            suggestTimer_.schedule(true, true, false);
         }
      }
      return false;
   }
   
   @SuppressWarnings("unused")
   private boolean isRoxygenTagValidHere()
   {
      if (input_.getText().matches("\\s*#+'.*"))
      {
         String linePart = StringUtil.substring(input_.getText(), 0, input_.getSelection().getStart().getPosition());
         if (linePart.matches("\\s*#+'\\s*"))
            return true;
      }
      return false;
   }

   private boolean isSweaveCompletion(char c)
   {
      if (rnwContext_ == null || (c != ',' && c != ' ' && c != '='))
         return false;

      int optionsStart = rnwContext_.getRnwOptionsStart(
            input_.getText(),
            input_.getSelection().getStart().getPosition());

      if (optionsStart < 0)
      {
         return false;
      }

      String linePart = StringUtil.substring(input_.getText(),
            optionsStart,
            input_.getSelection().getStart().getPosition());

      return c != ' ' || linePart.matches(".*,\\s*");
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
         return true;
      else if (KeyboardHelper.isHyphen(event))
         return true;
      else if (KeyboardHelper.isUnderscore(event))
         return true;
      
      if (event.getShiftKey())
         return false;
      
      if (keyCode >= '0' && keyCode <= '9')
         return true;
      if (keyCode == 190) // period
         return true;

      return false;
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
         requester_.flushCache();
   }
   
   public static class AutocompletionContextData extends JavaScriptObject
   {
      protected AutocompletionContextData()
      {
      }
      
      public static final native AutocompletionContextData create(String data,
                                                                  int type,
                                                                  int numCommas,
                                                                  Range range)
      /*-{
         return {
            data: data,
            type: type,
            numCommas: numCommas,
            range: range
         };
      }-*/;
      
      public static final AutocompletionContextData create(int type)
      {
         return create("", type, -1, null);
      }
      
      public final native String getData() /*-{ return this.data; }-*/;
      public final native int getType() /*-{ return this.type; }-*/;
   }

   // Things we need to form an appropriate autocompletion:
   //
   // 1. The token to the left of the cursor,
   // 2. The associated function call (if any -- for arguments),
   // 3. The associated data for a `[` call (if any -- completions from data object),
   // 4. The associated data for a `[[` call (if any -- completions from data object)
   public static class AutocompletionContext
   {
      // Be sure to sync these with 'SessionRCompletions.R'!
      public static final int TYPE_UNKNOWN = 0;
      public static final int TYPE_FUNCTION = 1;
      public static final int TYPE_SINGLE_BRACKET = 2;
      public static final int TYPE_DOUBLE_BRACKET = 3;
      public static final int TYPE_NAMESPACE_EXPORTED = 4;
      public static final int TYPE_NAMESPACE_ALL = 5;
      public static final int TYPE_DOLLAR = 6;
      public static final int TYPE_AT = 7;
      public static final int TYPE_FILE = 8;
      public static final int TYPE_CHUNK = 9;
      public static final int TYPE_ROXYGEN = 10;
      public static final int TYPE_HELP = 11;
      public static final int TYPE_ARGUMENT = 12;
      public static final int TYPE_PACKAGE = 13;
      public static final int TYPE_PLUMBER = 14;
      
      public AutocompletionContext(String token,
                                   JsVector<AutocompletionContextData> data,
                                   String functionCallString)
      {
         token_ = token;
         contextData_ = data;
         functionCallString_ = functionCallString;
      }
      
      public AutocompletionContext(String token, int type)
      {
         AutocompletionContextData data = AutocompletionContextData.create(type);
         JsVector<AutocompletionContextData> contextData = JsVector.createVector().cast();
         contextData.push(data);
         
         token_ = token;
         contextData_ = contextData;
      }
      
      public AutocompletionContext()
      {
      }

      public String getToken()
      {
         return token_;
      }

      public void setToken(String token)
      {
         token_ = token;
      }

      public String getFunctionCallString()
      {
         return functionCallString_;
      }

      public void setFunctionCallString(String functionCallString)
      {
         functionCallString_ = functionCallString;
      }
      
      public Range getStatementBounds()
      {
         return statementBounds_;
      }
      
      public void setStatementBounds(Range statementBounds)
      {
         statementBounds_ = statementBounds;
      }
      
      public JsVector<AutocompletionContextData> getContextData()
      {
         return contextData_;
      }
      
      public void setNeedsDocSync(boolean needsDocSync)
      {
         needsDocSync_ = needsDocSync;
      }
      
      public boolean getNeedsDocSync()
      {
         return needsDocSync_;
      }
      
      public void add(AutocompletionContextData data)
      {
         contextData_.push(data);
      }
      
      public void add(String data, int type)
      {
         contextData_.push(AutocompletionContextData.create(data, type, -1, null));
      }
      
      public void add(String data, int type, int numCommas)
      {
         contextData_.push(AutocompletionContextData.create(data, type, numCommas, null));
      }
      
      public void add(String data, int type, int numCommas, Range range)
      {
         contextData_.push(AutocompletionContextData.create(data, type, numCommas, range));
      }
      
      private String token_ = "";
      private String functionCallString_ = "";
      private JsVector<AutocompletionContextData> contextData_ = JsVector.createVector().cast();
      
      private Range statementBounds_ = null;
      private boolean needsDocSync_ = false;
   }
   
   private boolean isLineInRoxygenComment(String line)
   {
      return DocumentMode.PATTERN_ROXYGEN_LINE.test(line);
   }

   private boolean isLineInPlumberComment(String line)
   {
      Pattern pattern = Pattern.create("^\\s*#+\\*");
      return pattern.test(line);
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

      // Don't auto complete if tab was pressed and tab completion was disabled.
      if (nativeEvent_ != null &&
              nativeEvent_.getKeyCode() == KeyCodes.KEY_TAB &&
              (!userPrefs_.tabCompletion().getValue() || userPrefs_.tabKeyMoveFocus().getValue()))
         return false;
      
      if (!input_.isSelectionCollapsed())
         return false;
      
      invalidatePendingRequests(flushCache, false);
      
      InputEditorSelection selection = input_.getSelection();
      if (selection == null)
         return false;
      
      int cursorCol = selection.getStart().getPosition();
      String firstLine = StringUtil.substring(input_.getText(), 0, cursorCol);
      
      // never autocomplete in (non-roxygen) comments, or at the start
      // of roxygen comments (e.g. at "#' |")
      if (isLineInComment(firstLine) &&
            !isLineInRoxygenComment(firstLine) &&
            !isLineInPlumberComment(firstLine))
         return false;
      
      AceEditor editor = (AceEditor) docDisplay_;
      
      if (isLineInRoxygenComment(firstLine)) 
      {
         if (!DocumentMode.PATTERN_ROXYGEN_CAN_COMPLETE.test(firstLine))
         {
            // when below @examples, handle <TAB> here and take into account
            // the tab size, where the code begins (i.e. after |#' | ) and 
            // where the cursor currently is
            if (DocumentMode.isCursorInRoxygenExamples(docDisplay_) && !implicit) 
            {
               if (editor != null) 
               {
                  int tabSize = editor.getTabSize();
                  int cursor = editor.getCursorColumn();
                  
                  int commentSize = DocumentMode.PATTERN_ROXYGEN_LINE.match(firstLine, 0).getValue().length() + 1;
                  // if just after the ': add one space
                  String spaces = " ";
                  if (cursor >= commentSize)
                  {
                     // othewise add a tab or the remainder spaces to the next one
                     int remainder = (cursor - commentSize) % tabSize;
                     spaces = StringUtil.repeat(" ", remainder == 0 ? tabSize : remainder);
                  }
                  editor.insertCode(spaces);
               }
               return true;
            }
            else 
            {
               // otherwise let ace handle <TAB>
               return false;
            }
         }
      }

      // don't autocomplete if the cursor lies within the text of a
      // multi-line string. the logic here isn't perfect (ideally, we'd detect
      // whether we're in the 'qstring' or 'qqstring' state), but this will catch
      // the majority of cases
      Token cursorToken = docDisplay_.getTokenAt(docDisplay_.getCursorPosition());
      if (cursorToken != null && cursorToken.hasType("string"))
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
      if (!userPrefs_.tabMultilineCompletion().getValue())
      {
         if (nativeEvent_ != null &&
               nativeEvent_.getKeyCode() == KeyCodes.KEY_TAB)
            if (firstLine.matches("^\\s*$"))
               return false;
      }
      
      AutocompletionContext context = getAutocompletionContext();
      
      // Fix up the context token for non-file completions -- e.g. in
      //
      //    foo<-rn
      //
      // we erroneously capture '-' as part of the token name. This is awkward
      // but is effectively a bandaid until the autocompletion revamp.
      if (context.getToken().startsWith("-"))
         context.setToken(StringUtil.substring(context.getToken(), 1));
      
      // fix up roxygen autocompletion for case where '@' is snug against
      // the comment marker
      if (context.getToken() == "'@")
         context.setToken(StringUtil.substring(context.getToken(), 1));
      
      CompletionRequestContext requestContext = new CompletionRequestContext(
            invalidation_.getInvalidationToken(),
            docDisplay_.getCursorPosition(),
            selection,
            canAutoInsert);
      
      RInfixData infixData = RInfixData.create();
      if (editor != null)
      {
         CodeModel codeModel = editor.getSession().getMode().getRCodeModel();
         TokenCursor cursor = codeModel.getTokenCursor();
         
         if (cursor.moveToPosition(input_.getCursorPosition()))
         {
            // Try to see if there's an object name we should use to supplement
            // completions
            if (cursor.moveToPosition(input_.getCursorPosition()))
            {
               infixData = codeModel.getDataFromInfixChain(cursor);
            }
         }
      }
      
      String filePath = getSourceDocumentPath();
      String docId = getSourceDocumentId();
      
      // Provide 'line' for R custom completers
      String line = docDisplay_.getCurrentLineUpToCursor();
      
      requester_.getCompletions(
            context,
            infixData,
            filePath,
            docId,
            line,
            behavior_ == EditorBehavior.AceBehaviorConsole,
            implicit,
            requestContext);

      return true;
   }
   
   private void addAutocompletionContextForFile(AutocompletionContext context,
                                                String line)
   {
      int index = Math.max(line.lastIndexOf('"'), line.lastIndexOf('\''));
      String token = StringUtil.substring(line, index + 1);
      context.add(token, AutocompletionContext.TYPE_FILE);
      context.setToken(token);
   }
   
   private void addAutocompletionContextForNamespace(
         String token,
         AutocompletionContext context)
   {
         String[] splat = token.split(":{2,3}");
         String left = "";
         
         if (splat.length <= 0)
         {
            left = "";
         }
         else
         {
            left = splat[0];
         }
         
         int type = token.contains(":::") ?
               AutocompletionContext.TYPE_NAMESPACE_ALL :
                  AutocompletionContext.TYPE_NAMESPACE_EXPORTED;
               
         context.add(left, type);
   }
   
   
   private boolean addAutocompletionContextForDollar(AutocompletionContext context)
   {
      // Establish an evaluation context by looking backwards
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor == null)
         return false;
      
      CodeModel codeModel = editor.getSession().getMode().getRCodeModel();
      codeModel.tokenizeUpToRow(input_.getCursorPosition().getRow());
      
      TokenCursor cursor = codeModel.getTokenCursor();
         
      if (!cursor.moveToPosition(input_.getCursorPosition()))
         return false;
      
      // Move back to the '$'
      while (cursor.currentValue() != "$" && cursor.currentValue() != "@")
         if (!cursor.moveToPreviousToken())
            return false;
      
      int type = cursor.currentValue() == "$" ?
            AutocompletionContext.TYPE_DOLLAR :
            AutocompletionContext.TYPE_AT;
      
      // Put a cursor here
      TokenCursor contextEndCursor = cursor.cloneCursor();
      
      // We allow for arbitrary elements previous, so we want to get e.g.
      //
      //     env::foo()$bar()[1]$baz
      // Get the string forming the context
      //
      //
      // If this fails, we still want to report an empty evaluation context
      // (the completion is still occurring in a '$' context, so we do want
      // to exclude completions from other scopes)
      String data = "";
      
      if (cursor.moveToPreviousToken() && cursor.findStartOfEvaluationContext())
      {
         data = editor.getTextForRange(Range.fromPoints(
               cursor.currentPosition(),
               contextEndCursor.currentPosition()));
      }
      
      context.add(data, type);
      return true;
   }
   
   
   private AutocompletionContext getAutocompletionContext()
   {
      AutocompletionContext context = new AutocompletionContext();
      
      String firstLine = input_.getText();
      int row = input_.getCursorPosition().getRow();
      
      // trim to cursor position
      firstLine = StringUtil.substring(firstLine, 0, input_.getCursorPosition().getColumn());
      
      // Get the token at the cursor position.
      String tokenRegex = ".*[^" +
         RegexUtil.wordCharacter() +
         "._:$@'\"`-]";
      String token = firstLine.replaceAll(tokenRegex, "");
      
      // If we're completing an object within a string, assume it's a
      // file-system completion. Note that we may need other contextual information
      // to decide if e.g. we only want directories.
      String firstLineStripped = StringUtil.stripBalancedQuotes(
            StringUtil.stripRComment(firstLine));
      
      boolean isFileCompletion = false;
      if (firstLineStripped.indexOf('\'') != -1 || 
          firstLineStripped.indexOf('"') != -1)
      {
         isFileCompletion = true;
         addAutocompletionContextForFile(context, firstLine);
      }
      
      // If this line starts with '```{', then we're completing chunk options
      // pass the whole line as a token
      if (firstLine.startsWith("```{") || firstLine.startsWith("<<"))
         return new AutocompletionContext(firstLine, AutocompletionContext.TYPE_CHUNK);
      
      // In the first row of an embedded editor, we're also completing chunk
      // options
      if (behavior_ == EditorBehavior.AceBehaviorEmbedded && row == 0 && firstLine.startsWith("{"))
         return new AutocompletionContext(firstLine, AutocompletionContext.TYPE_CHUNK);

      // If this line starts with a '?', assume it's a help query
      if (firstLine.matches("^\\s*[?].*"))
         return new AutocompletionContext(token, AutocompletionContext.TYPE_HELP);
      
      // escape early for roxygen
      if (firstLine.matches("\\s*#+'.*"))
         return new AutocompletionContext(token, AutocompletionContext.TYPE_ROXYGEN);

      // plumber comments/annotations
      if (firstLine.matches("\\s*#+\\*.*"))
         return new AutocompletionContext(token, AutocompletionContext.TYPE_PLUMBER);

      // If the token has '$' or '@', add in the autocompletion context --
      // note that we still need parent contexts to give more information
      // about the appropriate completion
      if (token.contains("$") || token.contains("@"))
         addAutocompletionContextForDollar(context);
      
      // If the token has '::' or ':::', add that context. Note that
      // we still need outer contexts (so that e.g., if we try
      // 'debug(stats::rnorm)' we know not to auto-insert parens)
      if (token.contains("::"))
         addAutocompletionContextForNamespace(token, context);
      
      // If this is not a file completion, we need to further strip and
      // then set the token. Note that the token will have already been
      // set if this is a file completion.
      token = token.replaceAll(".*[$@:]", "");
      if (!isFileCompletion)
         context.setToken(token);
      
      // access to the R Code model
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor == null)
         return context;
      
      CodeModel codeModel = editor.getSession().getMode().getRCodeModel();
      
      // We might need to grab content from further up in the document than
      // the current cursor position -- so tokenize ahead.
      codeModel.tokenizeUpToRow(row + 100);
      
      // Make a token cursor and place it at the first token previous
      // to the cursor.
      TokenCursor tokenCursor = codeModel.getTokenCursor();
      if (!tokenCursor.moveToPosition(input_.getCursorPosition()))
         return context;
      
      // Check to see if the token following the cursor is a `::` or `:::`.
      // If that's the case, then we probably only want to complete package
      // names.
      if (tokenCursor.moveToNextToken())
      {
         if (tokenCursor.currentValue() == ":" ||
             tokenCursor.currentValue() == "::" ||
             tokenCursor.currentValue() == ":::")
         {
            return new AutocompletionContext(
                  token,
                  AutocompletionContext.TYPE_PACKAGE);
         }
         tokenCursor.moveToPreviousToken();
      }
      
      TokenCursor startCursor = tokenCursor.cloneCursor();
      
      // Find an opening '(' or '[' -- this provides the function or object
      // for completion.
      int initialNumCommas = 0;
      if (tokenCursor.currentValue() != "(" && tokenCursor.currentValue() != "[")
      {
         int commaCount = tokenCursor.findOpeningBracketCountCommas(
               new String[]{ "[", "(" }, true);
         
         // commaCount == -1 implies we failed to find an opening bracket
         if (commaCount == -1)
         {
            commaCount = tokenCursor.findOpeningBracketCountCommas("[", false);
            if (commaCount == -1)
               return context;
            else
               initialNumCommas = commaCount;
         }
         else
         {
            initialNumCommas = commaCount;
         }
      }
      
      // Figure out whether we're looking at '(', '[', or '[[',
      // and place the token cursor on the first token preceding.
      TokenCursor parenCursor = tokenCursor.cloneCursor();
      TokenCursor endOfDecl = tokenCursor.cloneCursor();
      int initialDataType = AutocompletionContext.TYPE_UNKNOWN;
      if (tokenCursor.currentValue() == "(")
      {
         initialDataType = AutocompletionContext.TYPE_FUNCTION;
         if (!tokenCursor.moveToPreviousToken())
            return context;
      }
      else if (tokenCursor.currentValue() == "[")
      {
         if (!tokenCursor.moveToPreviousToken())
            return context;
         
         if (tokenCursor.currentValue() == "[")
         {
            if (!endOfDecl.moveToPreviousToken())
               return context;
            
            initialDataType = AutocompletionContext.TYPE_DOUBLE_BRACKET;
            if (!tokenCursor.moveToPreviousToken())
               return context;
         }
         else
         {
            initialDataType = AutocompletionContext.TYPE_SINGLE_BRACKET;
         }
      }
      
      // Get the string marking the function or data
      if (!tokenCursor.findStartOfEvaluationContext())
         return context;
      
      // Try to get the function call string -- either there's
      // an associated closing paren we can use, or we should just go up
      // to the current cursor position.
      
      // First, attempt to determine where the closing paren is located. If
      // this fails, we'll just use the start cursor's position (and later
      // attempt to finish the expression to make it parsable)
      Position endPos = startCursor.currentPosition();
      endPos.setColumn(endPos.getColumn() + startCursor.currentValue().length());
      
      // try to look forward for closing paren
      if (endOfDecl.currentValue() == "(")
      {
         TokenCursor closingParenCursor = endOfDecl.cloneCursor();
         if (closingParenCursor.fwdToMatchingToken())
         {
            endPos = closingParenCursor.currentPosition();
            endPos.setColumn(endPos.getColumn() + 1);
         }
      }
      
      // We can now set the function call string.
      //
      // We strip out the current statement under the cursor, so that
      // match.call() can later properly resolve the current argument.
      //
      // Attempt to find the start of the current statement.
      TokenCursor clone = startCursor.cloneCursor();
      do
      {
         String value = clone.currentValue();
         if (value.contains(",") || value.equals("("))
            break;
         
         if (clone.bwdToMatchingToken())
            continue;
         
      } while (clone.moveToPreviousToken());
      Position startPosition = clone.currentPosition();
      
      // Include the opening paren if that's what we found
      if (clone.currentValue() == "(")
         startPosition.setColumn(startPosition.getColumn() + 1);
      
      String beforeText = editor.getTextForRange(Range.fromPoints(
            tokenCursor.currentPosition(),
            startPosition));
      
      // Now, attempt to find the end of the current statement.
      // Look for the ',' or ')' that ends the statement for the 
      // currently active argument.
      boolean lookupSucceeded = false;
      while (clone.moveToNextToken())
      {
         String value = clone.currentValue();
         if (value.contains(",") || value.equals(")"))
         {
            lookupSucceeded = true;
            break;
         }
         
         // Bail if we find a closing paren (we should walk over matched
         // pairs properly, so finding one implies that we have a parse error).
         if (value == "]" || value == "}")
            break;
         
         if (clone.fwdToMatchingToken())
            continue;
      }
      
      String afterText = "";
      if (lookupSucceeded)
      {
         afterText = editor.getTextForRange(Range.fromPoints(
               clone.currentPosition(),
               endPos));
      }
      
      context.setFunctionCallString(
            (beforeText + afterText).trim());
      
      // Try to identify whether we're producing autocompletions for
      // a _named_ function argument; if so, produce completions tuned to
      // that argument.
      TokenCursor argsCursor = startCursor.cloneCursor();
      do
      {
         String argsValue = argsCursor.currentValue();
         
         // Bail if we encounter tokens that we don't expect as part
         // of the current expression -- this implies we're not really
         // within a named argument, although this isn't perfect.
         if (argsValue == "," ||
             argsValue == "(" ||
             argsValue == "$" ||
             argsValue == "@" ||
             argsValue == "::" ||
             argsValue == ":::" ||
             argsValue == "]" ||
             argsValue == ")" ||
             argsValue == "}")
         {
            break;
         }
         
         // If we encounter an '=', we assume that this is
         // a function argument.
         if (argsValue == "=" && argsCursor.moveToPreviousToken())
         {
            if (!isFileCompletion)
               context.setToken(token);
            
            context.add(
                  argsCursor.currentValue(),
                  AutocompletionContext.TYPE_ARGUMENT,
                  0);
            break;
         }
         
      } while (argsCursor.moveToPreviousToken());
      
      String initialData =
            docDisplay_.getTextForRange(Range.fromPoints(
                  tokenCursor.currentPosition(),
                  endOfDecl.currentPosition())).trim();
      
      // And the first context
      Range initialRange = null;
      if (parenCursor.fwdToMatchingToken())
      {
         initialRange = Range.fromPoints(
               tokenCursor.currentPosition(),
               parenCursor.currentPosition());
      }
      context.add(initialData, initialDataType, initialNumCommas, initialRange);

      // Get the rest of the parent contexts (function calls, subsetting)
      String assocData;
      int dataType;
      int numCommas;
      String[] bracketTokens = new String[] { "[", "(" };
      
      while (true)
      {
         int commaCount = tokenCursor.findOpeningBracketCountCommas(bracketTokens, false);
         if (commaCount == -1)
            break;
         
         numCommas = commaCount;
         dataType = tokenCursor.currentValue() == "("
               ? AutocompletionContext.TYPE_FUNCTION
               : AutocompletionContext.TYPE_SINGLE_BRACKET;
         
         TokenCursor declEnd = tokenCursor.cloneCursor();
         if (!tokenCursor.moveToPreviousToken())
            return context;
         
         if (tokenCursor.currentValue() == "[")
         {
            if (!declEnd.moveToPreviousToken())
               return context;
            
            dataType = AutocompletionContext.TYPE_DOUBLE_BRACKET;
            if (!tokenCursor.moveToPreviousToken())
               return context;
         }
         
         tokenCursor.findStartOfEvaluationContext();
         assocData =
            docDisplay_.getTextForRange(Range.fromPoints(
                  tokenCursor.currentPosition(),
                  declEnd.currentPosition())).trim();
         
         declEnd.fwdToMatchingToken();
         Range range = Range.fromPoints(tokenCursor.currentPosition(), declEnd.currentPosition());
         context.add(assocData, dataType, numCommas, range);
      }
      
      // Try to get information about the current expression.
      TokenCursor cursor = tokenCursor.cloneCursor();
      Position contextStartPos = cursor.currentPosition();
      Position contextEndPos = cursor.currentPosition();
      
      while (true)
      {
         if (!cursor.findStartOfEvaluationContext())
            break;
         
         TokenCursor savedCursor = cursor.cloneCursor();
         if (!cursor.moveToPreviousToken())
            break;
        
         if (!cursor.isLookingAtBinaryOp())
         {
            cursor = savedCursor;
            break;
         }
         
         if (!cursor.moveToPreviousToken())
         {
            cursor = savedCursor;
            break;
         }
         
         continue;
      }
      contextStartPos = cursor.currentPosition();
      
      // Include the context end.
      while (true)
      {
         if (!cursor.moveToNextToken())
            break;
         
         if (cursor.fwdToMatchingToken())
            if (!cursor.moveToNextToken())
               break;
         
         if (!cursor.isLookingAtBinaryOp())
         {
            cursor.moveToPreviousToken();
            break;
         }
         
         if (!cursor.moveToNextToken())
            break;
         
         continue;
      }
      contextEndPos = cursor.currentPosition();
      
      Range statementBounds = Range.fromPoints(contextStartPos, contextEndPos);
      context.setStatementBounds(statementBounds);
      context.setNeedsDocSync(true);
      
      return context;
      
   }
   
   private void showSnippetHelp(QualifiedName item,
                                CompletionPopupDisplay popup)
   {
      popup.displaySnippetHelp(
            snippets_.getSnippetContents(item.name));
   }

   /**
    * It's important that we create a new instance of this each time.
    * It maintains state that is associated with a completion request.
    */
   private final class CompletionRequestContext extends
         ServerRequestCallback<CompletionResult>
   {
      public CompletionRequestContext(Invalidation.Token token,
                                      Position position,
                                      InputEditorSelection selection,
                                      boolean canAutoAccept)
      {
         invalidationToken_ = token;
         position_ = position;
         selection_ = selection;
         canAutoAccept_ = canAutoAccept;
      }
      
      public void showHelp(QualifiedName selectedItem)
      {
         if (selectedItem.type == RCompletionType.SNIPPET)
            showSnippetHelp(selectedItem, popup_);
         else
            helpStrategy_.showHelp(selectedItem, popup_);
      }
      
      public void showHelpTopic()
      {
         QualifiedName selectedItem = popup_.getSelectedValue();
         // TODO: Show help should navigate to snippet file?
         if (selectedItem.type != RCompletionType.SNIPPET)
            helpStrategy_.showHelpTopic(selectedItem);
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
      public void onResponseReceived(CompletionResult completions)
      {
         // bail if this request has been invalidated
         if (invalidationToken_.isInvalid())
            return;
         
         // bail if the cursor was moved to a new line
         if (docDisplay_.getCursorPosition().getRow() != position_.getRow())
            return;
         
         // update active completion context
         context_ = this;
         
         // Only display the top completions
         final QualifiedName[] results =
               completions.completions.toArray(new QualifiedName[0]);
         
         if (results.length == 0)
         {
            popup_.clearCompletions();
            boolean lastInputWasTab =
                  (nativeEvent_ != null && nativeEvent_.getKeyCode() == KeyCodes.KEY_TAB);
            
            boolean lineIsWhitespace = docDisplay_.getCurrentLine().matches("^\\s*$");

            // Insert tab if tab auto completion was disabled or the line is all whitespace.
            if (lastInputWasTab && (lineIsWhitespace || !userPrefs_.tabCompletion().getValue()))
            {
               docDisplay_.blockIndent();
               return;
            }
            
            if (canAutoAccept_)
            {
               popup_.showErrorMessage(
                     constants_.noMatchesLabel(),
                     new PopupPositioner(input_.getCursorBounds(), popup_));
            }
            else
            {
               // Show an empty popup message offscreen -- this is a hack to
               // ensure that we can get completion results on backspace after a
               // failed completion, e.g. 'stats::rna' -> 'stats::rn'
               popup_.placeOffscreen();
            }
            
            return;
         }
         
         // If there is only one result and the name is identical to the
         // current token, then implicitly accept that completion. we hide
         // the popup to ensure that backspace can re-load completions from
         // the cache
         if (results.length == 1 &&
             completions.token == results[0].name.replaceAll(":*", ""))
         {
            // For snippets we need to apply the completion if explicitly requested
            if (results[0].type == RCompletionType.SNIPPET && canAutoAccept_)
            {
               snippets_.applySnippet(completions.token, results[0].name);
               return;
            }
            
            popup_.placeOffscreen();
            return;
         }

         // Move range to beginning of token; we want to place the popup there.
         final String token = completions.token;

         Rectangle rect = input_.getPositionBounds(
               selection_.getStart().movePosition(-token.length(), true));

         token_ = token;
         overrideInsertParens_ = completions.dontInsertParens;

         if (results.length == 1
               && canAutoAccept_
               && completions.canAutoAccept()
               && results[0].type != RCompletionType.DIRECTORY)
         {
            onSelection(results[0]);
         }
         else
         {
            boolean preferBottom = !copilot_.isEnabled();
            popup_.showCompletionValues(
                  results,
                  new PopupPositioner(rect, popup_, preferBottom),
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
         helpStrategy_.clearCache();
         
         if (value == null)
         {
            assert false : "Selected comp value is null";
            return;
         }

         applyValue(qname);
         
         // For in-line edits, we don't want to auto-popup after replacement
         if (qname.suggestOnAccept || 
               (qname.name.endsWith(":") &&
                     docDisplay_.getCharacterAtCursor() != ':'))
         {
            Scheduler.get().scheduleDeferred(() -> beginSuggest(true, true, false));
         }
         else
         {
            popup_.hide();
            popup_.clearHelp(false);
            popup_.setHelpVisible(false);
            docDisplay_.setFocus(true);
         }
         
      }
      
      // Quote non-syntactic completion values where appropriate.
      //
      // For example, this is necessary when completing a list member;
      // e.g. in 'foo$bar' or 'foo@bar'.
      //
      private String quoteIfNotSyntacticNameCompletion(QualifiedName name)
      {
         String value = name.name;
         
         // we don't quote for certain completion types
         int type = name.type;
         if (type == RCompletionType.ROXYGEN ||
             type == RCompletionType.PACKAGE ||
             type == RCompletionType.ARGUMENT ||
             type == RCompletionType.SECUNDARY_ARGUMENT ||
             type == RCompletionType.OPTION ||
             type == RCompletionType.CONTEXT ||
             type == RCompletionType.KEYWORD)
         {
            return value;
         }
         
         // we don't quote for completions from certain custom sources
         String source = name.source;
         boolean isSpecialSource =
               StringUtil.equals(source,  "<file>") ||
               StringUtil.equals(source,  "<directory>") ||
               StringUtil.equals(source,  "<chunk-option>");
         if (isSpecialSource)
            return value;
         
         // quote if: 
         // - keywords, but just in a ::, :::, $, or @ contexts
         // - non syntactic
         boolean quote = false;

         int context = name.context;
         if (isKeyword(value)) 
         {
            if (context == AutocompletionContext.TYPE_AT ||
                context == AutocompletionContext.TYPE_DOLLAR ||
                context == AutocompletionContext.TYPE_NAMESPACE_EXPORTED ||
                context == AutocompletionContext.TYPE_NAMESPACE_ALL)
            {
               quote = true;
            }
         }
         else if (!RegexUtil.isSyntacticRIdentifier(value))
         {
            quote = true;
         }
         // if this is already a syntactic identifier, no quote is required
         if (quote)
            return "`" + value.replaceAll("`", "\\\\`") + "`";
         
         return value;
      }

      private boolean isKeyword(String value)
      {
         return StringUtil.equals(value, "NULL") || 
                StringUtil.equals(value, "NA") || 
                StringUtil.equals(value, "TRUE") ||
                StringUtil.equals(value, "FALSE") ||  
                StringUtil.equals(value, "Inf") ||  
                StringUtil.equals(value, "NaN") ||  
                StringUtil.equals(value, "NA_integer_") || 
                StringUtil.equals(value, "NA_real_") ||  
                StringUtil.equals(value, "NA_character_") || 
                StringUtil.equals(value, "NA_complex_") ||
                StringUtil.equals(value, "function") ||
                StringUtil.equals(value, "while") ||
                StringUtil.equals(value, "repeat") ||
                StringUtil.equals(value, "for") ||
                StringUtil.equals(value, "if") || 
                StringUtil.equals(value, "in") ||
                StringUtil.equals(value, "else") || 
                StringUtil.equals(value, "next") ||
                StringUtil.equals(value, "break")
                ;
      }
      
      private void applyValueRmdOption(final String value)
      {
         suggestTimer_.cancel();
         
         // If there is no token but spaces have been inserted, then compensate
         // for that. This is necessary as we allow for spaces in the completion,
         // and completions auto-popup after ',' so e.g. on
         //
         // ```{r, |}
         //      ^        -- automatically triggered completion
         //       ^       -- user inserted spaces
         //
         // if we accept a completion in that position, we should keep the
         // spaces the user inserted. (After the user has inserted a character,
         // it becomes part of the token and hence this is unnecessary.
         if (token_ == "")
         {
            int startPos = selection_.getStart().getPosition();
            String currentLine = docDisplay_.getCurrentLine();
            while (startPos < currentLine.length() &&
                  StringUtil.charAt(currentLine, startPos) == ' ')
               ++startPos;
            
            input_.setSelection(new InputEditorSelection(
                  selection_.getStart().movePosition(startPos, false),
                  input_.getSelection().getEnd()));
         }
         else
         {
            input_.setSelection(new InputEditorSelection(
                  selection_.getStart().movePosition(-token_.length(), true),
                  input_.getSelection().getEnd()));
         }
         
         input_.replaceSelection(value, true);
         token_ = value;
         selection_ = input_.getSelection();
      }

      private void applyValue(final QualifiedName qualifiedName)
      {
         String completionToken = token_;
         
         // Strip off the quotes for string completions.
         if (completionToken.startsWith("'") || completionToken.startsWith("\""))
            completionToken = StringUtil.substring(completionToken, 1);
         
         if (qualifiedName.source == "<chunk-option>")
         {
            applyValueRmdOption(qualifiedName.name);
            return;
         }
         
         if (qualifiedName.type == RCompletionType.SNIPPET)
         {
            snippets_.applySnippet(completionToken, qualifiedName.name);
            return;
         }

         if (qualifiedName.type == RCompletionType.ROXYGEN)
         {
            String snippet = qualifiedName.display;
            if (snippet.contains("\n"))
            {
               // inject the #' prefix
               String prefix = docDisplay_.getCurrentLine().replaceFirst("@.*", "");
               snippet = snippet.replace("\n", "\n" + prefix);
            }
            
            snippets_.applySnippetContent(completionToken, snippet);

            return;
         }
         
         boolean insertParen =
               userPrefs_.insertParensAfterFunctionCompletion().getValue() &&
               RCompletionType.isFunctionType(qualifiedName.type);
         
         // Don't insert a paren if there is already a '(' following
         // the cursor
         AceEditor editor = (AceEditor) input_;
         boolean textFollowingCursorIsOpenParen = false;
         boolean textFollowingCursorIsClosingParen = false;
         boolean textFollowingCursorIsColon = false;
         if (editor != null)
         {
            TokenCursor cursor =
                  editor.getSession().getMode().getRCodeModel().getTokenCursor();
            cursor.moveToPosition(editor.getCursorPosition());
            if (cursor.moveToNextToken())
            {
               textFollowingCursorIsOpenParen =
                     cursor.currentValue() == "(";
               textFollowingCursorIsClosingParen =
                     cursor.currentValue() == ")" && !cursor.bwdToMatchingToken();
               textFollowingCursorIsColon =
                     cursor.currentValue() == ":" ||
                     cursor.currentValue() == "::" ||
                     cursor.currentValue() == ":::";
            }
            
         }

         String value = qualifiedName.name;
         boolean shouldQuote = qualifiedName.shouldQuote;
         
         
         // Don't insert the `::` following a package completion if there is
         // already a `:` following the cursor
         if (textFollowingCursorIsColon)
            value = value.replaceAll(":", "");
         
         if (qualifiedName.type == RCompletionType.DIRECTORY)
            value = value + "/";
         
         if (!shouldQuote &&
             !RCompletionType.isFileType(qualifiedName.type) &&
             !StringUtil.equals(qualifiedName.language, "Python"))
         {
            value = quoteIfNotSyntacticNameCompletion(qualifiedName);
         }
         
         /* In some cases, applyValue can be called more than once
          * as part of the same completion instance--specifically,
          * if there's only one completion candidate and it is in
          * a package. To make sure that the selection movement
          * logic works the second time, we need to reset the
          * selection.
          */
         
         // There might be multiple cursors. Get the position of each cursor.
         Range[] ranges = editor.getNativeSelection().getAllRanges();
         
         // Determine the replacement value.
         boolean shouldInsertParens = insertParen &&
               !overrideInsertParens_ &&
               !textFollowingCursorIsOpenParen;
         
         boolean insertMatching = userPrefs_.insertMatching().getValue();
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
            if (!userPrefs_.insertSpacesAroundEquals().getValue() &&
                  value.endsWith(kSpaceEquals))
            {
               value = StringUtil.substring(value, 0, value.length() - kSpaceEquals.length()) + "=";
            }
         }
         
         int offset =
               completionToken.length() +
               docDisplay_.getCursorPosition().getColumn() -
               context_.position_.getColumn();
         
         // Loop over all of the active cursors, and replace.
         for (Range range : ranges)
         {
            // We should be typing, and so each range should just define a
            // cursor position. Take those positions, construct ranges, replace
            // text in those ranges, and proceed.
            Position replaceEnd = range.getEnd();
            Position replaceStart = replaceEnd.movedLeft(offset);
            
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
      
      private final Invalidation.Token invalidationToken_;
      private final Position position_;
      private InputEditorSelection selection_;
      private final boolean canAutoAccept_;
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
   private UserPrefs userPrefs_;
   private Copilot copilot_;

   private final CodeToolsServerOperations server_;
   private final InputEditorDisplay input_;
   private final NavigableSourceEditor navigableSourceEditor_;
   private final CompletionPopupDisplay popup_;
   private final CompletionRequester requester_;
   private final InitCompletionFilter initFilter_;
   
   private String token_;
   
   private final DocDisplay docDisplay_;
   private final SnippetHelper snippets_;
   private final EditorBehavior behavior_;

   private final Invalidation invalidation_ = new Invalidation();
   private CompletionRequestContext context_;
   private final CompletionContext rContext_;
   private final RnwCompletionContext rnwContext_;
   
   private final SignatureToolTipManager sigTipManager_;
   
   private NativeEvent nativeEvent_;
   
   private QualifiedName lastSelectedItem_;
   private Timer helpRequest_;
   private final SuggestionTimer suggestTimer_;
   
   private static class SuggestionTimer
   {
      SuggestionTimer(RCompletionManager manager, UserPrefs uiPrefs)
      {
         manager_ = manager;
         userPrefs_ = uiPrefs;
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
         timer_.schedule(userPrefs_.codeCompletionDelay().getValue());
      }
      
      public void cancel()
      {
         timer_.cancel();
      }
      
      private final RCompletionManager manager_;
      private final UserPrefs userPrefs_;
      private final Timer timer_;
      
      private boolean flushCache_;
      private boolean implicit_;
      private boolean canAutoInsert_;
      
   }
   
   private final HandlerRegistrations handlers_;
   private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);
}
