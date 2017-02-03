/*
 * CppCompletionManager.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;


import org.rstudio.core.client.CommandWith2Args;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionUtils;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.CppSourceLocation;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class CppCompletionManager implements CompletionManager
{
   public void onPaste(PasteEvent event)
   {
      hideCompletionPopup();
      if (rCompletionManager_ != null)
         rCompletionManager_.onPaste(event);
   }
   
   public CppCompletionManager(DocDisplay docDisplay,
                               InitCompletionFilter initFilter,
                               CppCompletionContext completionContext,
                               CompletionManager rCompletionManager)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docDisplay_ = docDisplay;
      initFilter_ = initFilter;
      completionContext_ = completionContext;
      rCompletionManager_ = rCompletionManager;
      snippets_ = new SnippetHelper((AceEditor) docDisplay_, completionContext.getDocPath());
      handlers_ = new HandlerRegistrations();
      
      handlers_.add(docDisplay_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            terminateCompletionRequest();
         }
      }));
   }
 
   @Inject
   void initialize(CppServerOperations server, 
                   FileTypeRegistry fileTypeRegistry,
                   UIPrefs uiPrefs)
   {
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
      uiPrefs_ = uiPrefs;
      suggestionTimer_ = new SuggestionTimer(this, uiPrefs_);
   }
   
   // close the completion popup (if any)
   @Override
   public void close()
   {
      // delegate to R mode if necessary
      if (DocumentMode.isCursorInRMode(docDisplay_) ||
            DocumentMode.isCursorInMarkdownMode(docDisplay_))
      {
         rCompletionManager_.close();
      }
      else
      {
         terminateCompletionRequest();
      }
   }
   
   @Override
   public void detach()
   {
      handlers_.removeHandler();
      snippets_.detach();
      rCompletionManager_.detach();
   }
   
   // perform completion at the current cursor location
   @Override
   public void codeCompletion()
   {
      // delegate to R mode if necessary
      if (DocumentMode.isCursorInRMode(docDisplay_) ||
            DocumentMode.isCursorInMarkdownMode(docDisplay_))
      {
         rCompletionManager_.codeCompletion();
      }
      // check whether it's okay to do a completion
      else if (shouldComplete(null))
      {
         suggestCompletions(true); 
      }
   }

   // go to help at the current cursor location
   @Override
   public void goToHelp()
   {
      // delegate to R mode if necessary
      if (DocumentMode.isCursorInRMode(docDisplay_))
      {
         rCompletionManager_.goToHelp();
      }
      else
      {
         // no implementation here yet since we don't have access
         // to C/C++ help (we could implement this via using libclang
         // to parse doxygen though)   
      }
   }

   // find the definition of the function at the current cursor location
   @Override
   public void goToFunctionDefinition()
   {  
      // delegate to R mode if necessary
      if (DocumentMode.isCursorInRMode(docDisplay_))
      {
         rCompletionManager_.goToFunctionDefinition();
      }
      else
      {
         completionContext_.cppCompletionOperation(new CppCompletionOperation(){

            @Override
            public void execute(String docPath, int line, int column)
            {
               server_.goToCppDefinition(
                     docPath, 
                     line, 
                     column, 
                     new CppCompletionServerRequestCallback<CppSourceLocation>(
                                                     "Finding definition...") {
                        @Override
                        public void onSuccess(CppSourceLocation loc)
                        {
                           if (loc != null)
                           {
                              fileTypeRegistry_.editFile(loc.getFile(), 
                                                         loc.getPosition());  
                           }
                        }
                     });
            }
            
         });
      }
   }
  
   // return false to indicate key not handled
   @Override
   public boolean previewKeyDown(NativeEvent event)
   {
      suggestionTimer_.cancel();
      
      // delegate to R mode if appropriate
      if (DocumentMode.isCursorInRMode(docDisplay_) ||
            DocumentMode.isCursorInMarkdownMode(docDisplay_))
         return rCompletionManager_.previewKeyDown(event);
      
      // if there is no completion request active then 
      // check for a key-combo that triggers completion or 
      // navigation / help
      int modifier = KeyboardShortcut.getModifierValue(event);
      if ((request_ == null) || request_.isTerminated())
      {  
         // check for user completion key combo 
         if (CompletionUtils.isCompletionRequest(event, modifier) &&
             shouldComplete(event)) 
         {
            return suggestCompletions(true);
         }
         else if (event.getKeyCode() == KeyCodes.KEY_TAB &&
                  modifier == KeyboardShortcut.SHIFT)
         {
            return snippets_.attemptSnippetInsertion(true);
         }
         else if (event.getKeyCode() == 112 // F1
                  && modifier == KeyboardShortcut.NONE)
         {
            goToHelp();
            return true;
         }
         else if (event.getKeyCode() == 113 // F2
                  && modifier == KeyboardShortcut.NONE)
         {
            goToFunctionDefinition();
            return true;
         }
         else
         {
            return false;
         }
      }
      // otherwise handle keys within the completion popup
      else
      {   
         // get the key code
         int keyCode = event.getKeyCode();
         
         // bail on modifier keys
         if (KeyboardHelper.isModifierKey(keyCode))
            return false;
         
         // if there is no popup then bail
         CppCompletionPopupMenu popup = getCompletionPopup();
         if ((popup == null) || !popup.isVisible())
            return false;
         
         // allow emacs-style navigation of popup entries
         if (modifier == KeyboardShortcut.CTRL)
         {
            switch (keyCode)
            {
            case KeyCodes.KEY_P: return popup.selectPrev();
            case KeyCodes.KEY_N: return popup.selectNext();
            }
         }
             
         // backspace triggers completion if the popup is visible
         if (keyCode == KeyCodes.KEY_BACKSPACE)
         {
            deferredSuggestCompletions(false, false);
            return false;
         }
         
         // tab accepts the current selection (popup handles Enter)
         else if (keyCode == KeyCodes.KEY_TAB)
         {
            popup.acceptSelected();
            return true;
         }
         
         // allow '.' when showing file completions
         else if (popup.getCompletionPosition().getScope() == CompletionPosition.Scope.File &&
                  KeyboardHelper.isPeriodKeycode(keyCode))
         {
            return false;
         }
         
         // non c++ identifier keys (that aren't navigational) close the popup
         else if (!CppCompletionUtils.isCppIdentifierKey(event))
         {
            terminateCompletionRequest();
            return false;
         }
         
         // otherwise leave it alone
         else
         {   
            return false;
         }
      }
   }

   // return false to indicate key not handled
   @Override
   public boolean previewKeyPress(char c)
   {
      suggestionTimer_.cancel();
      
      // delegate to R mode if necessary
      if (DocumentMode.isCursorInRMode(docDisplay_) || 
            DocumentMode.isCursorInMarkdownMode(docDisplay_))
      {
         return rCompletionManager_.previewKeyPress(c);
      }
      else
      {
         // don't do implicit completions if the user has set completion to manual
         // (but always do them if the completion popup is visible)
         if (!uiPrefs_.codeComplete().getValue().equals(UIPrefsAccessor.COMPLETION_MANUAL) ||
             isCompletionPopupVisible())
         {
            deferredSuggestCompletions(false, true);
         }
         
         return false;
      }
   }
   
   private void deferredSuggestCompletions(final boolean explicit, 
                                           final boolean canDelay)
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
         @Override
         public void execute()
         {
            suggestCompletions(explicit, canDelay);  
         }
      });
   }
   
   private boolean suggestCompletions(final boolean explicit)
   {
      return suggestCompletions(explicit, false);
   }
   
   private boolean suggestCompletions(final boolean explicit, boolean canDelay)
   {
      suggestionTimer_.cancel();
      
      // check for completions disabled
      if (!completionContext_.isCompletionEnabled())
         return false;
      
      // check for no selection
      InputEditorSelection selection = docDisplay_.getSelection() ;
      if (selection == null)
         return false;
      
      // check for contiguous selection
      if (!docDisplay_.isSelectionCollapsed())
         return false;    
  
      // calculate explicit value for getting completion position (if a 
      // previous request was explicit then count this as explicit)
      boolean positionExplicit = explicit || 
                                 ((request_ != null) && request_.isExplicit());
      
      // see if we even have a completion position
      boolean alwaysComplete = uiPrefs_.codeComplete().getValue().equals(
                                            UIPrefsAccessor.COMPLETION_ALWAYS);
      int autoChars = uiPrefs_.alwaysCompleteCharacters().getValue();
      final CompletionPosition completionPosition = 
            CppCompletionUtils.getCompletionPosition(docDisplay_,
                                                     positionExplicit,
                                                     alwaysComplete,
                                                     autoChars);
      
      if (completionPosition == null)
      {
         terminateCompletionRequest();
         return false;
      }
      else if ((request_ != null) &&
               !request_.isTerminated() &&
               request_.getCompletionPosition().isSupersetOf(completionPosition))
      {
         request_.updateUI(false);
      }
      else if (canDelay && 
               completionPosition.getScope() == CompletionPosition.Scope.Global)
      {
         suggestionTimer_.schedule(completionPosition);
      }
      else
      {
         performCompletionRequest(completionPosition, explicit);
      }
      
      return true;
   }

   private void performCompletionRequest(
         final CompletionPosition completionPosition, final boolean explicit)
   {  
      terminateCompletionRequest();
      
      final Invalidation.Token invalidationToken = 
            completionRequestInvalidation_.getInvalidationToken();
      
      completionContext_.withUpdatedDoc(new CommandWith2Args<String, String>() {

         @Override
         public void execute(String docPath, String docId)
         {
            if (invalidationToken.isInvalid())
               return;
            
            request_ = new CppCompletionRequest(
               docPath,
               docId,
               completionPosition,
               docDisplay_,
               invalidationToken,
               explicit,
               CppCompletionManager.this,
               new Command() {
                  @Override
                  public void execute()
                  {
                     suggestionTimer_.cancel();
                  }
               });
         }
      });
   }
   
   private static class SuggestionTimer
   {
      SuggestionTimer(CppCompletionManager manager, UIPrefs uiPrefs)
      {
         manager_ = manager;
         uiPrefs_ = uiPrefs;
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               manager_.performCompletionRequest(completionPosition_, false);
            }
         };
      }
      
      public void schedule(CompletionPosition completionPosition)
      {
         completionPosition_ = completionPosition;
         timer_.schedule(uiPrefs_.alwaysCompleteDelayMs().getValue());
      }
      
      public void cancel()
      {
         timer_.cancel();
      }
      
      private final CppCompletionManager manager_;
      private final UIPrefs uiPrefs_;
      private final Timer timer_;
      private CompletionPosition completionPosition_;
   }
   
     
   private CppCompletionPopupMenu getCompletionPopup()
   {
      CppCompletionPopupMenu popup = request_ != null ?
            request_.getCompletionPopup() : null;
      return popup;
   }
   
   private void hideCompletionPopup()
   {
      CppCompletionPopupMenu popup = getCompletionPopup();
      if (popup != null)
         popup.hide();
   }
   
   private boolean isCompletionPopupVisible()
   {
      CppCompletionPopupMenu popup = getCompletionPopup();
      return (popup != null) && popup.isVisible();
   }
   
   private void terminateCompletionRequest()
   {
      suggestionTimer_.cancel();
      completionRequestInvalidation_.invalidate();
      if (request_ != null)
      {
         request_.terminate();
         request_ = null;
      }
   }

   private boolean shouldComplete(NativeEvent event)
   {
      return initFilter_ == null || initFilter_.shouldComplete(event);
   }
   
   private CppServerOperations server_;
   private UIPrefs uiPrefs_;
   private FileTypeRegistry fileTypeRegistry_;
   private final DocDisplay docDisplay_;
   private final CppCompletionContext completionContext_;
   private CppCompletionRequest request_;
   private SuggestionTimer suggestionTimer_;
   private final InitCompletionFilter initFilter_ ;
   private final CompletionManager rCompletionManager_;
   private final Invalidation completionRequestInvalidation_ = new Invalidation();
   private final SnippetHelper snippets_;
   
   private final HandlerRegistrations handlers_;
  

}
