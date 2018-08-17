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

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.studio.client.RStudioGinjector;
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
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
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
      popupPresenter_ = new CompletionPopupPresenter(popup);
      
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
               popup_.hide();
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
   public void onCompletionResponseReceived(String line,
                                            boolean canAutoAccept,
                                            Completions completions)
   {
      if (completions.isCacheable())
         completionCache_.store(line, completions);
      
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
               completions.getHelpHandler(),
               completions.getLanguage());
      }
      
      if (results.length == 0)
      {
         popup_.clearCompletions();
         
         if (canAutoAccept)
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
      
      boolean shouldImplicitlyAccept =
            results.length == 1 &&
            StringUtil.equals(completions.getToken(), results[0].name);
      
      if (shouldImplicitlyAccept)
      {
         QualifiedName completion = results[0];
         if (canAutoAccept && completion.type == RCompletionType.SNIPPET)
            snippets_.applySnippet(completions.getToken(), completion.name);
         else
            popup_.placeOffscreen();
         return;
      }
      
      String token = completions.getToken();
      
      boolean shouldAutoAccept =
            results.length == 1 &&
            canAutoAccept &&
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
   
   public boolean beginSuggest(boolean flushCache,
                               boolean implicit,
                               boolean canAutoInsert)
   {
      invalidatePendingRequests(flushCache, false);
      
      // TODO: handle comments
      // TODO: handle multi-line strings
      String line = docDisplay_.getCurrentLineUpToCursor();
      
      // don't auto-complete with tab on lines with only whitespace
      // (unless the user has opted in)
      if (!uiPrefs_.allowTabMultilineCompletion().getValue())
      {
         Event event = Event.getCurrentEvent();
         boolean isTabInsertionOnBlankLine =
               event.getKeyCode() == KeyCodes.KEY_TAB &&
               line.matches("^\\s*$");
         if (isTabInsertionOnBlankLine)
            return false;
      }
      
      // TODO: can auto accept
      context_ = new CompletionRequestContext(this, line, false, invalidation_.getInvalidationToken());
      if (completionCache_.satisfyRequest(line, context_))
         return true;
      
      getCompletions(line, context_);
      return true;
   }
   
   public void invalidatePendingRequests()
   {
      invalidatePendingRequests(true, true);
   }
   
   public void invalidatePendingRequests(boolean flushCache,
                                         boolean hidePopup)
   {
      
   }
   
   public abstract void goToHelp();
   public abstract void goToDefinition();
   public abstract void getCompletions(String line, CompletionRequestContext context);
   
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
   
   protected Boolean onKeyDown(NativeEvent event)
   {
      suggestTimer_.cancel();
      
      if (isDisabled())
         return false;
      
      if (popupPresenter_.handleKeyDown(event))
         return true;
      
      int keyCode = event.getKeyCode();
      int modifier = KeyboardShortcut.getModifierValue(event);
      
      switch (modifier)
      {
      
      case KeyboardShortcut.NONE:
      {
         switch (keyCode)
         {
         case KeyCodes.KEY_F1:
            goToHelp();
            return true;
            
         case KeyCodes.KEY_F2:
            goToDefinition();
            return true;
         }
      }
      
      case KeyboardShortcut.SHIFT:
      {
         switch (keyCode)
         {
         case KeyCodes.KEY_TAB:
            return snippets_.attemptSnippetInsertion(true);
         }
      }
      
      }
      
      return null;
   }
   
   protected Boolean onKeyPress(char charCode)
   {
      if (isDisabled())
         return false;
      
      if (popup_.isShowing())
      {
         Scheduler.get().scheduleDeferred(() -> beginSuggest(false, true, false));
      }
      else
      {
         if (canAutoPopup(charCode, uiPrefs_.alwaysCompleteCharacters().getValue() - 1))
            suggestTimer_.schedule(true, true, false);
      }
      
      return null;
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
               beginSuggest(flushCache_, implicit_, canAutoInsert_);
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
      
      private final Timer timer_;
      
      private boolean flushCache_;
      private boolean implicit_;
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
   private final CompletionPopupPresenter popupPresenter_;
   
   private CompletionRequestContext context_;
   private HandlerRegistration[] handlers_;
   private HelpStrategy helpStrategy_;
   
   protected EventBus events_;
   protected UIPrefs uiPrefs_;
}
