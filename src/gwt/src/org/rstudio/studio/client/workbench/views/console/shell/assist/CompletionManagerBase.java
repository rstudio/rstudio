/*
 * CompletionManagerBase.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorCommandEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public abstract class CompletionManagerBase
      implements CompletionRequestContext.Host
{
   public abstract void goToHelp();
   public abstract void goToDefinition();
   public abstract void showAdditionalHelp(QualifiedName completion);
   public abstract boolean getCompletions(String line, CompletionRequestContext context);
   
   public interface Callback
   {
      public void onToken(TokenIterator it, Token token);
   }
   
   public CompletionManagerBase(CompletionPopupDisplay popup,
                                DocDisplay docDisplay,
                                CodeToolsServerOperations server,
                                CompletionContext context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      popup_ = popup;
      docDisplay_ = docDisplay;
      server_ = server;
      
      invalidation_ = new Invalidation();
      completionCache_ = new CompletionCache();
      suggestTimer_ = new SuggestionTimer();
      helpTimer_ = new HelpTimer();
      handlers_ = new ArrayList<HandlerRegistration>();
      snippets_ = new SnippetHelper((AceEditor) docDisplay, context.getId());
      
      // deferred so that handlers are toggled after subclasses have finished
      // construction
      Scheduler.get().scheduleDeferred(() -> toggleHandlers(true));
   }
   
   @Inject
   private void initialize(EventBus events,
                           UserPrefs uiPrefs,
                           HelpStrategy helpStrategy)
   {
      events_ = events;
      userPrefs_ = uiPrefs;
      helpStrategy_ = helpStrategy;
   }
   
   protected void onPopupSelection(QualifiedName completion)
   {
      helpTimer_.cancel();
      if (popup_.isHelpVisible())
         showPopupHelp(completion);
      else
         showPopupHelpDeferred(completion);
   }
   
   protected void onPopupSelectionCommit(QualifiedName completion)
   {
      if (completion.type == RCompletionType.SNIPPET)
         onSelection(snippetToken_, completion);
      else
         onSelection(completionToken_, completion);
   }
   
   @Override
   public void onCompletionResponseReceived(CompletionRequestContext.Data data,
                                            Completions completions)
   {
      String line = data.getLine();
      if (completions.isCacheable())
         completionCache_.store(line, completions);
      
      int n = completions.getCompletions().length();
      List<QualifiedName> names = new ArrayList<QualifiedName>();
      for (int i = 0; i < n; i++)
      {
         names.add(new QualifiedName(
               completions.getCompletions().get(i),
               completions.getPackages().get(i),
               false,
               completions.getType().get(i),
               completions.getMeta().get(i),
               completions.getHelpHandler(),
               completions.getLanguage()));
      }
      
      if (userPrefs_.enableSnippets().getValue())
      {
         String[] parts = line.split("\\s+");
         if (parts.length > 0)
         {
            snippetToken_ = parts[parts.length - 1];
            ArrayList<String> snippets = snippets_.getAvailableSnippets();
            for (String snippet : snippets)
               if (snippet.startsWith(snippetToken_))
                  names.add(QualifiedName.createSnippet(snippet));
         }
      }
      
      addExtraCompletions(completions.getToken(), names);
      
      QualifiedName[] results = new QualifiedName[names.size()];
      results = names.toArray(results);
      
      if (results.length == 0)
      {
         popup_.clearCompletions();
         
         if (data.autoAcceptSingleCompletionResult())
         {
            popup_.showErrorMessage(
                  "(No matches)",
                  new PopupPositioner(docDisplay_.getCursorBounds(), popup_));
         }
         else
         {
            popup_.placeOffscreen();
         }
         
         return;
      }
      
      // if the token we have matches all available completions, we should
      // implicitly accept it (handle cases where multiple completions with
      // the same value but different types are received)
      boolean shouldImplicitlyAccept = true;
      for (int i = 0; i < results.length; i++)
      {
         if (!StringUtil.equals(completions.getToken(), results[i].name))
         {
            shouldImplicitlyAccept = false;
            break;
         }
      }
      
      if (shouldImplicitlyAccept)
      {
         QualifiedName completion = results[0];
         if (data.autoAcceptSingleCompletionResult() && completion.type == RCompletionType.SNIPPET)
         {
            snippets_.applySnippet(completions.getToken(), completion.name);
            popup_.hide();
         }
         else
         {
            popup_.placeOffscreen();
         }
         
         // because we swallow tab keypresses for displaying completion popups,
         // if the user tried to press tab and the completion engine returned
         // only a single completion with the same value as the token, we need
         // to insert a literal tab to 'play' the tab key back into the document
         if (data.isTabTriggeredCompletion())
            docDisplay_.insertCode("\t");
         
         return;
      }
      
      String token = completions.getToken();
      
      boolean shouldAutoAccept =
            results.length == 1 &&
            data.autoAcceptSingleCompletionResult() &&
            results[0].type != RCompletionType.DIRECTORY;
      
      if (shouldAutoAccept)
      {
         onSelection(token, results[0]);
         return;
      }
      
      Position tokenPos = docDisplay_.getSelectionStart().movedLeft(token.length());
      Rectangle tokenBounds = docDisplay_.getPositionBounds(tokenPos);
      completionToken_ = token;
      popup_.showCompletionValues(
            results,
            new PopupPositioner(tokenBounds, popup_),
            false);
   }
   
   @Override
   public void onCompletionRequestError(String message)
   {
      
   }
   
   public void onCompletionCommit()
   {
      QualifiedName value = popup_.getSelectedValue();
      if (value == null)
         return;
      
      onPopupSelectionCommit(value);
   }
   
   public boolean beginSuggest()
   {
      return beginSuggest(true, false, true);
   }
   
   public boolean beginSuggest(boolean flushCache,
                               boolean isTabTriggered,
                               boolean canAutoAccept)
   {
      invalidatePendingRequests(flushCache, false);
      
      String line = docDisplay_.getCurrentLineUpToCursor();
      
      Token token = docDisplay_.getTokenAt(docDisplay_.getCursorPosition());
      if (token != null)
      {
         // don't complete within comments
         if (token.hasType("comment"))
            return false;

         // don't complete within multi-line strings
         if (token.hasType("string"))
         {
            String cursorTokenValue = token.getValue();
            boolean isSingleLineString =
                  cursorTokenValue.startsWith("'") ||
                  cursorTokenValue.startsWith("\"");
            if (!isSingleLineString)
               return false;
         }
      }
      
      CompletionRequestContext.Data data = new CompletionRequestContext.Data(
            line,
            isTabTriggered,
            canAutoAccept);
            
      context_ = new CompletionRequestContext(this, data);
      if (completionCache_.satisfyRequest(line, context_))
         return true;
      
      boolean canComplete = getCompletions(line, context_);
      
      // if tab was used to trigger the completion, but no completions
      // are available in that context, then insert a literal tab
      if (!canComplete && isTabTriggered)
         docDisplay_.insertCode("\t");
      
      return canComplete;
   }
   
   public Invalidation.Token getInvalidationToken()
   {
      return invalidation_.getInvalidationToken();
   }
   
   public void invalidatePendingRequests()
   {
      invalidatePendingRequests(true, true);
   }
   
   public void invalidatePendingRequests(boolean flushCache,
                                         boolean hidePopup)
   {
      invalidation_.invalidate();
      
      if (hidePopup && popup_.isShowing())
      {
         popup_.hide();
         popup_.clearHelp(false);
      }
      
      if (flushCache)
         completionCache_.flush();
   }
   
   // Subclasses should override this to provide extra (e.g. context) completions.
   protected void addExtraCompletions(String token, List<QualifiedName> completions)
   {
   }
   
   // Subclasses can override this if they want different behavior in
   // amending completions appropriate to their type
   protected String onCompletionSelected(QualifiedName requestedCompletion)
   {
      String value = requestedCompletion.name;
      int type = requestedCompletion.type;
      
      // add trailing '/' for directory completions
      if (type == RCompletionType.DIRECTORY)
         value += "/";
      
      // add '()' for function completions
      boolean insertParensAfterCompletion =
            RCompletionType.isFunctionType(type) &&
            userPrefs_.insertParensAfterFunctionCompletion().getValue();
      
      if (insertParensAfterCompletion)
         value += "()";
      
      return value;
   }
   
   // Subclasses can override to perform post-completion-insertion actions,
   // e.g. displaying a tooltip or similar
   protected void onCompletionInserted(QualifiedName completion)
   {
      int type = completion.type;
      if (!RCompletionType.isFunctionType(type))
         return;
      
      boolean insertParensAfterCompletion =
            RCompletionType.isFunctionType(type) &&
            userPrefs_.insertParensAfterFunctionCompletion().getValue();
      
      if (insertParensAfterCompletion)
         docDisplay_.moveCursorBackward();
   }
   
   // Subclasses can override depending on what characters are typically
   // considered part of identifiers / are relevant to a completion context.
   protected boolean isBoundaryCharacter(char ch)
   {
      boolean valid =
            Character.isLetterOrDigit(ch) ||
            ch == '.' ||
            ch == '_';
      
      return !valid;
   }
   
   // Subclasses can override based on what characters might want to trigger
   // completions, or force a new completion request.
   protected boolean isTriggerCharacter(char ch)
   {
      return false;
   }
   
   public void codeCompletion()
   {
      beginSuggest(true, false, true);
   }
   
   public void onPaste(PasteEvent event)
   {
      popup_.hide();
   }
   
   public void close()
   {
      popup_.hide();
   }
   
   public void detach()
   {
      removeHandlers();
      suggestTimer_.cancel();
      snippets_.detach();
      invalidation_.invalidate();
      popup_.hide();
   }
   
   public boolean previewKeyDown(NativeEvent event)
   {
      suggestTimer_.cancel();
      
      if (isDisabled())
         return false;
      
      int keyCode = event.getKeyCode();
      int modifier = KeyboardShortcut.getModifierValue(event);
      if (KeyboardHelper.isModifierKey(keyCode))
         return false;
      
      if (popup_.isShowing())
      {
         switch (modifier)
         {
         
         case KeyboardShortcut.CTRL:
         {
            switch (keyCode)
            {
            case KeyCodes.KEY_P: popup_.selectPrev(); return true;
            case KeyCodes.KEY_N: popup_.selectNext(); return true;
            }
            
            break;
         }
         
         case KeyboardShortcut.NONE:
         {
            switch (keyCode)
            {
            case KeyCodes.KEY_UP:        popup_.selectPrev();         return true;
            case KeyCodes.KEY_DOWN:      popup_.selectNext();         return true;
            case KeyCodes.KEY_PAGEUP:    popup_.selectPrevPage();     return true;
            case KeyCodes.KEY_PAGEDOWN:  popup_.selectNextPage();     return true;
            case KeyCodes.KEY_HOME:      popup_.selectFirst();        return true;
            case KeyCodes.KEY_END:       popup_.selectLast();         return true;
            case KeyCodes.KEY_ESCAPE:    invalidatePendingRequests(); return true;
            case KeyCodes.KEY_ENTER:     return onPopupEnter();
            case KeyCodes.KEY_TAB:       return onPopupTab();
            case KeyCodes.KEY_F1:        return onPopupAdditionalHelp();
            }
            
            break;
         }
         }
         
         switch (keyCode)
         {
         case KeyCodes.KEY_LEFT:
         case KeyCodes.KEY_RIGHT:
            invalidatePendingRequests();
         }
         
         return false;
      }
      else
      {
         switch (modifier)
         {

         case KeyboardShortcut.NONE:
         {
            switch (keyCode)
            {
            case KeyCodes.KEY_F1:  goToHelp();       return true;
            case KeyCodes.KEY_F2:  goToDefinition(); return true;
            case KeyCodes.KEY_TAB: return onTab();
            }
            
            break;
         }
         
         case KeyboardShortcut.CTRL:
         {
            switch (keyCode)
            {
            case KeyCodes.KEY_SPACE:
               if (docDisplay_.isEmacsModeOn())
                  return false;
               
               beginSuggest();
               return true;
            }
            
            break;
         }

         case KeyboardShortcut.SHIFT:
         {
            switch (keyCode)
            {
            case KeyCodes.KEY_TAB:
               return snippets_.attemptSnippetInsertion(true);
            }
            
            break;
         }

         }
      }
      
      return false;
   }
   
   public boolean previewKeyPress(char charCode)
   {
      if (isDisabled())
         return false;
      
      if (popup_.isShowing())
      {
         if (canContinueCompletions(charCode))
            Scheduler.get().scheduleDeferred(() -> beginSuggest(false, false, false));
         else
            invalidatePendingRequests();
      }
      else
      {
         if (canAutoPopup(charCode, userPrefs_.codeCompletionCharacters().getValue() - 1))
            suggestTimer_.schedule(true, false);
      }
      
      return false;
   }
   
   protected boolean isDisabled()
   {
      if (docDisplay_.isSnippetsTabStopManagerActive())
         return true;
      
      return false;
   }
   
   protected boolean canContinueCompletions(char ch)
   {
      // NOTE: We allow users to continue a completion 'session' in the case where
      // a character was mistyped; e.g. imagine the user requested completions with
      // the token 'rn' and got back:
      //
      //    - rnbinom
      //    - rnorm
      //
      // and accidentally typed a 'z'. while no completion item will match, we should
      // keep the completion session 'live' so that hitting backspace will continue
      // to show completions with the original 'rn' token.
      switch (ch)
      {
      
      case ' ':
      {
         // for spaces, only continue the completion session if this does indeed match
         // an existing completion item in the popup
         String token = completionToken_ + " ";
         if (popup_.hasCompletions())
         {
            for (QualifiedName item : popup_.getItems())
               if (StringUtil.isSubsequence(item.name, token, false))
                  return true;
         }
         
         return false;
      }
      
      }
      
      return true;
   }
   
   protected boolean canAutoPopup(char ch, int lookbackLimit)
   {
      String codeComplete = userPrefs_.codeCompletion().getValue();
      
      if (isTriggerCharacter(ch) && !StringUtil.equals(codeComplete, UserPrefs.CODE_COMPLETION_MANUAL))
         return true;
      
      if (!StringUtil.equals(codeComplete, UserPrefs.CODE_COMPLETION_ALWAYS))
         return false;

      if (docDisplay_.isVimModeOn() && !docDisplay_.isVimInInsertMode())
         return false;
      
      if (docDisplay_.isCursorInSingleLineString())
         return false;
      
      if (!isBoundaryCharacter(docDisplay_.getCharacterAtCursor()))
         return false;
         
      String currentLine = docDisplay_.getCurrentLine();
      Position cursorPos = docDisplay_.getCursorPosition();
      int cursorColumn = cursorPos.getColumn();
      
      boolean canAutoPopup =
            currentLine.length() >= lookbackLimit &&
            !isBoundaryCharacter(ch);
            
      if (!canAutoPopup)
         return false;
      
      for (int i = 0; i < lookbackLimit; i++)
      {
         int index = cursorColumn - i - 1;
         if (isBoundaryCharacter(StringUtil.charAt(currentLine, index)))
            return false;
      }
      
      return true;
   }
   
   private void onSelection(String completionToken,
                            QualifiedName completion)
   {
      suggestTimer_.cancel();
      
      popup_.hide();
      popup_.clearHelp(false);
      popup_.setHelpVisible(false);
      
      int type = completion.type;
      if (type == RCompletionType.SNIPPET)
      {
         snippets_.applySnippet(completionToken, completion.name);
      }
      else
      {
         String value = onCompletionSelected(completion);

         Range[] ranges = docDisplay_.getNativeSelection().getAllRanges();
         for (Range range : ranges)
         {
            Position replaceStart = range.getEnd().movedLeft(completionToken.length());
            Position replaceEnd = range.getEnd();
            docDisplay_.replaceRange(Range.fromPoints(replaceStart, replaceEnd), value);
         }

         onCompletionInserted(completion);
      }
      
      docDisplay_.setFocus(true);
   }
   
   private boolean onPopupEnter()
   {
      if (popup_.isOffscreen())
         return false;
      
      QualifiedName completion = popup_.getSelectedValue();
      if (completion == null)
      {
         popup_.hide();
         return false;
      }
      
      onPopupSelectionCommit(completion);
      return true;
   }
   
   private boolean onPopupTab()
   {
      if (popup_.isOffscreen())
         return false;
      
      QualifiedName completion = popup_.getSelectedValue();
      if (completion == null)
      {
         popup_.hide();
         return false;
      }
      
      onPopupSelectionCommit(completion);
      return true;
   }
   
   private boolean onPopupAdditionalHelp()
   {
      if (popup_.isOffscreen())
         return false;
      
      QualifiedName completion = popup_.getSelectedValue();
      if (completion == null)
         return false;
      
      showAdditionalHelp(completion);
      return false;
   }
   
   private void onDocumentChanged(DocumentChangedEvent event)
   {
      if (!popup_.isShowing())
         return;
      
      if (docDisplay_.inMultiSelectMode())
         return;
      
      if (!event.getEvent().getAction().contentEquals("removeText"))
         return;
      
      Scheduler.get().scheduleDeferred(() -> {
         
         int cursorColumn = docDisplay_.getCursorPosition().getColumn();
         if (cursorColumn == 0)
         {
            invalidatePendingRequests();
            return;
         }
         
         String line = docDisplay_.getCurrentLine();
         char ch = StringUtil.charAt(line, cursorColumn - 1);
         if (isBoundaryCharacter(ch))
         {
            invalidatePendingRequests();
            return;
         }
         
         beginSuggest(false, false, false);
         
      });
   }
   
   private boolean onTab()
   {
      // Don't auto complete if tab auto completion was disabled
      if (!userPrefs_.tabCompletion().getValue() || userPrefs_.tabKeyMoveFocus().getValue())
         return false;

      // if the line is blank, don't request completions unless
      // the user has explicitly opted in
      String line = docDisplay_.getCurrentLineUpToCursor();
      if (!userPrefs_.tabMultilineCompletion().getValue())
      {
         if (line.matches("^\\s*"))
            return false;
      }
      
      return beginSuggest(true, true, true);
   }
   
   private void showPopupHelp(QualifiedName completion)
   {
      if (completion.type == RCompletionType.SNIPPET)
         popup_.displaySnippetHelp(snippets_.getSnippetContents(completion.name));
      else
         helpStrategy_.showHelp(completion, popup_);
   }
   
   private void showPopupHelpDeferred(QualifiedName completion)
   {
      helpTimer_.schedule(completion);
   }
   
   private void toggleHandlers(boolean enable)
   {
      if (enable)
         addHandlers();
      else
         removeHandlers();
   }
   
   private void addHandlers()
   {
      removeHandlers();
      
      HandlerRegistration[] ownHandlers = defaultHandlers();
      for (HandlerRegistration handler : ownHandlers)
         handlers_.add(handler);
      
      HandlerRegistration[] userHandlers = handlers();
      if (userHandlers != null)
      {
         for (HandlerRegistration handler : userHandlers)
            handlers_.add(handler);
      }
   }
   
   private void removeHandlers()
   {
      for (HandlerRegistration handler : handlers_)
         handler.removeHandler();
      handlers_.clear();
   }
   
   protected HandlerRegistration[] handlers()
   {
      return null;
   }
   
   private HandlerRegistration[] defaultHandlers()
   {
      return new HandlerRegistration[] {
            
            docDisplay_.addAttachHandler((AttachEvent event) -> {
               toggleHandlers(event.isAttached());
            }),
            
            docDisplay_.addBlurHandler((BlurEvent event) -> {
               onBlur();
            }),
            
            docDisplay_.addClickHandler((ClickEvent event) -> {
               invalidatePendingRequests();
            }),
            
            docDisplay_.addDocumentChangedHandler((DocumentChangedEvent event) -> {
               onDocumentChanged(event);
            }),
            
            popup_.addMouseDownHandler((MouseDownEvent event) -> {
               ignoreNextBlur_ = true;
            }),
            
            popup_.addAttachHandler((AttachEvent event) -> {
               docDisplay_.setPopupVisible(event.isAttached());
            }),
            
            popup_.addSelectionHandler((SelectionEvent<QualifiedName> event) -> {
               onPopupSelection(event.getSelectedItem());
            }),
            
            popup_.addSelectionCommitHandler((SelectionCommitEvent<QualifiedName> event) -> {
               onPopupSelectionCommit(event.getSelectedItem());
            }),
            
            events_.addHandler(AceEditorCommandEvent.TYPE, (AceEditorCommandEvent event) -> {
               invalidatePendingRequests();
            })
            
      };
   }
   
   private void onBlur()
   {
      if (ignoreNextBlur_)
      {
         ignoreNextBlur_ = false;
         return;
      }
      
      invalidatePendingRequests();
   }
   
   private class SuggestionTimer
   {
      public SuggestionTimer()
      {
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               beginSuggest(flushCache_, false, canAutoInsert_);
            }
         };
      }
      
      public void schedule(boolean flushCache,
                           boolean canAutoInsert)
      {
         flushCache_ = flushCache;
         canAutoInsert_ = canAutoInsert;
         
         timer_.schedule(userPrefs_.codeCompletionDelay().getValue());
      }
      
      public void cancel()
      {
         timer_.cancel();
      }
      
      private final Timer timer_;
      
      private boolean flushCache_;
      private boolean canAutoInsert_;
   }
   
   private class HelpTimer
   {
      public HelpTimer()
      {
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               showPopupHelp(completion_);
            }
         };
      }
      
      public void schedule(QualifiedName completion)
      {
         completion_ = completion;
         
         timer_.schedule(600);
      }
      
      public void cancel()
      {
         timer_.cancel();
      }
      
      private QualifiedName completion_;
      
      private final Timer timer_;
   }
   
   protected final CompletionPopupDisplay popup_;
   protected final DocDisplay docDisplay_;
   protected final CodeToolsServerOperations server_;
   
   private final Invalidation invalidation_;
   private final CompletionCache completionCache_;
   private final SuggestionTimer suggestTimer_;
   private final HelpTimer helpTimer_;
   private final SnippetHelper snippets_;
   
   private final List<HandlerRegistration> handlers_;
   
   private String completionToken_;
   private String snippetToken_;
   private boolean ignoreNextBlur_;
   
   private CompletionRequestContext context_;
   private HelpStrategy helpStrategy_;
   
   protected EventBus events_;
   protected UserPrefs userPrefs_;
}
