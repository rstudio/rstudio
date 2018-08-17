/*
 * CompletionManagerBase.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.UnicodeLetters;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.NativeEventProperty;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorCommandEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public abstract class CompletionManagerBase
      implements CompletionRequestContext.Host
{
   public CompletionManagerBase(CompletionPopupDisplay popup,
                                DocDisplay docDisplay,
                                CompletionContext context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      popup_ = popup;
      docDisplay_ = docDisplay;
      
      invalidation_ = new Invalidation();
      completionCache_ = new CompletionCache();
      suggestTimer_ = new SuggestionTimer();
      helpTimer_ = new HelpTimer();
      
      snippets_ = new SnippetHelper((AceEditor) docDisplay, context.getId());
      
      init();
   }
   
   @Inject
   private void initialize(EventBus events,
                           UIPrefs uiPrefs,
                           HelpStrategy helpStrategy)
   {
      events_ = events;
      uiPrefs_ = uiPrefs;
      helpStrategy_ = helpStrategy;
   }
   
   private void init()
   {
      handlers_ = new HandlerRegistration[] {
            
            popup_.addAttachHandler((AttachEvent event) -> {
               docDisplay_.setPopupVisible(event.isAttached());
            }),
            
            popup_.addSelectionHandler((SelectionEvent<QualifiedName> event) -> {
               onPopupSelection(event.getSelectedItem());
            }),
            
            popup_.addSelectionCommitHandler((SelectionCommitEvent<QualifiedName> event) -> {
               onPopupSelectionCommit(event.getSelectedItem());
            }),
            
            docDisplay_.addBlurHandler((BlurEvent event) -> {
               invalidatePendingRequests();
            }),
            
            docDisplay_.addClickHandler((ClickEvent event) -> {
               invalidatePendingRequests();
            }),
            
            docDisplay_.addAttachHandler((AttachEvent event) -> {
               if (!event.isAttached())
                  removeHandlers();
            }),
            
            events_.addHandler(AceEditorCommandEvent.TYPE, (AceEditorCommandEvent event) -> {
               invalidatePendingRequests();
            })
            
      };
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
      onSelection(token_, completion);
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
               completions.getHelpHandler(),
               completions.getLanguage()));
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
      
      // if we receive a single completion result whose completion
      // is the same as the input token, we should implicitly accept
      boolean shouldImplicitlyAccept =
            results.length == 1 &&
            StringUtil.equals(completions.getToken(), results[0].name);
      
      if (shouldImplicitlyAccept)
      {
         QualifiedName completion = results[0];
         if (data.autoAcceptSingleCompletionResult() && completion.type == RCompletionType.SNIPPET)
            snippets_.applySnippet(completions.getToken(), completion.name);
         else
            popup_.placeOffscreen();
         
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
      token_ = token;
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
      
      // TODO: handle comments
      // TODO: handle multi-line strings
      // TODO: handle Tab completion preference
      // TODO: can auto accept
      String line = docDisplay_.getCurrentLineUpToCursor();
      CompletionRequestContext.Data data = new CompletionRequestContext.Data(
            line,
            isTabTriggered,
            canAutoAccept);
            
      context_ = new CompletionRequestContext(this, data);
      if (completionCache_.satisfyRequest(line, context_))
         return true;
      
      getCompletions(line, context_);
      return true;
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
   
   public abstract void goToHelp();
   public abstract void goToDefinition();
   public abstract void getCompletions(String line, CompletionRequestContext context);
   
   // Subclasses should override this to provide extra (e.g. context) completions.
   protected void addExtraCompletions(String token, List<QualifiedName> completions)
   {
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
      for (HandlerRegistration handler : handlers_)
         handler.removeHandler();
      
      snippets_.detach();
      popup_.hide();
   }
   
   // Is this a character that separates identifiers (e.g. whitespace)?
   @SuppressWarnings("deprecation")
   protected boolean isBoundaryCharacter(char ch)
   {
      if (ch == '\0')
         return true;
      
      return Character.isSpace(ch);
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
            case KeyCodes.KEY_LEFT:      invalidatePendingRequests(); return true;
            case KeyCodes.KEY_RIGHT:     invalidatePendingRequests(); return true;
            case KeyCodes.KEY_PAGEUP:    popup_.selectPrevPage();     return true;
            case KeyCodes.KEY_PAGEDOWN:  popup_.selectNextPage();     return true;
            case KeyCodes.KEY_HOME:      popup_.selectFirst();        return true;
            case KeyCodes.KEY_END:       popup_.selectLast();         return true;
            case KeyCodes.KEY_ESCAPE:    invalidatePendingRequests(); return true;
            case KeyCodes.KEY_ENTER:     return onPopupEnter();
            case KeyCodes.KEY_TAB:       return onPopupTab();
            case KeyCodes.KEY_BACKSPACE: return onPopupBackspace();
            }
            
            break;
         }
         }
         
         // if we're inserting something alphanumeric, then we can continue
         // with our completions
         String key = NativeEventProperty.key(event);
         if (key.isEmpty())
            return false;
         
         char ch = key.charAt(0);
         if (UnicodeLetters.isLetter(ch))
            return false;
         
         // unhandled key -- close the popup and end the current completion session
         invalidatePendingRequests();
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
         Scheduler.get().scheduleDeferred(() -> beginSuggest(false, false, false));
      }
      else
      {
         if (canAutoPopup(charCode, uiPrefs_.alwaysCompleteCharacters().getValue() - 1))
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
   
   protected boolean canAutoPopup(char ch, int lookbackLimit)
   {
      String codeComplete = uiPrefs_.codeComplete().getValue();
      if (!StringUtil.equals(codeComplete, UIPrefsAccessor.COMPLETION_ALWAYS))
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
         if (isBoundaryCharacter(currentLine.charAt(index)))
            return false;
      }
      
      return true;
   }
   
   private void onSelection(String completionToken,
                            QualifiedName completion)
   {
      suggestTimer_.cancel();
      
      String insertion = completion.name;
      int type = completion.type;
      
      if (type == RCompletionType.SNIPPET)
      {
         snippets_.applySnippet(completionToken, completion.name);
         return;
      }
      
      if (type == RCompletionType.DIRECTORY)
         insertion += "/";
      
      // TODO: Handle automatic paren insertion
      
      Range[] ranges = docDisplay_.getNativeSelection().getAllRanges();
      for (Range range : ranges)
      {
         Position replaceStart = range.getEnd().movedLeft(completionToken.length());
         Position replaceEnd = range.getEnd();
         docDisplay_.replaceRange(Range.fromPoints(replaceStart, replaceEnd), insertion);
      }
      
      // TODO: Move cursor back within parentheses
      // TODO: Display tooltip
      
      popup_.hide();
      popup_.clearHelp(false);
      popup_.setHelpVisible(false);
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
   
   private boolean onPopupBackspace()
   {
      if (docDisplay_.inMultiSelectMode())
         return false;
      
      int cursorColumn = docDisplay_.getCursorPosition().getColumn();
      if (cursorColumn < 2)
      {
         invalidatePendingRequests();
         return false;
      }
      
      String line = docDisplay_.getCurrentLine();
      char ch = line.charAt(cursorColumn - 2);
      if (!UnicodeLetters.isLetter(ch))
      {
         invalidatePendingRequests();
         return false;
      }
      
      return false;
   }
   
   private boolean onTab()
   {
      // if the line is blank, don't request completions unless
      // the user has explicitly opted in
      String line = docDisplay_.getCurrentLineUpToCursor();
      if (!uiPrefs_.allowTabMultilineCompletion().getValue())
      {
         if (line.matches("^\\s*"))
            return false;
      }
      
      beginSuggest(true, true, true);
      return true;
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
   
   private void removeHandlers()
   {
      for (HandlerRegistration handler : handlers_)
         handler.removeHandler();
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
         
         timer_.schedule(uiPrefs_.alwaysCompleteDelayMs().getValue());
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
   
   private final CompletionPopupDisplay popup_;
   private final DocDisplay docDisplay_;
   
   private final Invalidation invalidation_;
   private final CompletionCache completionCache_;
   private final SuggestionTimer suggestTimer_;
   private final HelpTimer helpTimer_;
   private String token_;
   
   private final SnippetHelper snippets_;
   
   private CompletionRequestContext context_;
   private HandlerRegistration[] handlers_;
   private HelpStrategy helpStrategy_;
   
   protected EventBus events_;
   protected UIPrefs uiPrefs_;
}
