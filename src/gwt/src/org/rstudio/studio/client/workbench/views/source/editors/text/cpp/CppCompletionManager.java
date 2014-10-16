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


import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionListPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionUtils;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;

public class CppCompletionManager implements CompletionManager
{
   public CppCompletionManager(DocDisplay docDisplay,
                               InitCompletionFilter initFilter,
                               CppCompletionContext completionContext,
                               CompletionManager rCompletionManager)
   {
      docDisplay_ = docDisplay;
      initFilter_ = initFilter;
      completionContext_ = completionContext;
      rCompletionManager_ = rCompletionManager; 
      docDisplay_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            terminateCompletionRequest();
         }
      });
   }
 
   // close the completion popup (if any)
   @Override
   public void close()
   {
      // delegate to R mode if necessary
      if (isCursorInRMode())
      {
         rCompletionManager_.close();
      }
      else
      {
         terminateCompletionRequest();
      }
   }
   
   
   // perform completion at the current cursor location
   @Override
   public void codeCompletion()
   {
      // delegate to R mode if necessary
      if (isCursorInRMode())
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
      if (isCursorInRMode())
      {
         rCompletionManager_.goToHelp();
      }
      else
      {
         // TODO: go to help
         
      }
   }

   // find the definition of the function at the current cursor location
   @Override
   public void goToFunctionDefinition()
   {  
      // delegate to R mode if necessary
      if (isCursorInRMode())
      {
         rCompletionManager_.goToFunctionDefinition();
      }
      else
      {
         // TODO: go to function definition
         
        
      }
   }
   
   // return false to indicate key not handled
   @Override
   public boolean previewKeyDown(NativeEvent event)
   {
      // delegate to R mode if appropriate
      if (isCursorInRMode())
         return rCompletionManager_.previewKeyDown(event);
      
      // if there is no completion request active then 
      // check for a key-combo that triggers completion or 
      // navigation / help
      int modifier = KeyboardShortcut.getModifierValue(event);
      if (request_ == null)
      { 
         // check for user completion key combo 
         if (CompletionUtils.isCompletionRequest(event, modifier) &&
             shouldComplete(event)) 
         {
            suggestCompletions(true);
            return true;  
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
         
         // chrome on ubuntu now sends this before every keydown
         // so we need to explicitly ignore it. see:
         // https://github.com/ivaynberg/select2/issues/2482
         if (keyCode == KeyCodes.KEY_WIN_IME)
         {
            return false ;
         }
         
         // modifier keys always no-op
         if (keyCode == KeyCodes.KEY_SHIFT ||
             keyCode == KeyCodes.KEY_CTRL ||
             keyCode == KeyCodes.KEY_ALT)
         {          
            return false ; 
         }
         
         // backspace always triggers a reset of completion state
         if (keyCode == KeyCodes.KEY_BACKSPACE)
         {
            suggestCompletions();
            return false;
         }
         
         // get the popup -- if there is no popup then bail
         CompletionListPopupPanel popup = getCompletionPopup();
         if (popup == null)
            return false;
         
         // escape and left keys terminate the request
         if (event.getKeyCode() == KeyCodes.KEY_ESCAPE ||
             event.getKeyCode() == KeyCodes.KEY_LEFT)
         {
            terminateCompletionRequest();
            return true;
         }
          
         // enter/tab/right accept the current selection
         else if (event.getKeyCode() == KeyCodes.KEY_ENTER ||
                  event.getKeyCode() == KeyCodes.KEY_TAB ||
                  event.getKeyCode() == KeyCodes.KEY_RIGHT)
         {
            request_.applyValue(popup.getSelectedValue());
            return true;
         }
         
         // basic navigation keys
         else if (event.getKeyCode() == KeyCodes.KEY_UP)
            return popup.selectPrev();
         else if (event.getKeyCode() == KeyCodes.KEY_DOWN)
            return popup.selectNext();
         else if (event.getKeyCode() == KeyCodes.KEY_PAGEUP)
            return popup.selectPrevPage() ;
         else if (event.getKeyCode() == KeyCodes.KEY_PAGEDOWN)
            return popup.selectNextPage() ;
         else if (event.getKeyCode() == KeyCodes.KEY_HOME)
            return popup.selectFirst() ;
         else if (event.getKeyCode() == KeyCodes.KEY_END)
            return popup.selectLast() ;
         
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
      // delegate to R mode if necessary
      if (isCursorInRMode())
      {
         return rCompletionManager_.previewKeyPress(c);
      }
      else if (CompletionUtils.handleEncloseSelection(docDisplay_, c))
      {
         return true;
      }
      else
      {
         suggestCompletions();
         return false;
      }
   }
   
   private void suggestCompletions()
   {
      suggestCompletions(false);
   }
   
   private void suggestCompletions(final boolean explicit)
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
         @Override
         public void execute()
         {
            doSuggestCompletions(explicit);  
         }
      });
   }
   
   private void doSuggestCompletions(final boolean explicit)
   {
      // check for completions disabled
      if (!completionContext_.isCompletionEnabled())
         return;
      
      // check for no selection
      InputEditorSelection selection = docDisplay_.getSelection() ;
      if (selection == null)
         return;
      
      // check for contiguous selection
      if (!docDisplay_.isSelectionCollapsed())
         return;    
  
      // see if we even have a completion position
      final Position completionPosition = 
            CppCompletionUtils.getCompletionPosition(docDisplay_);
      if (completionPosition == null)
      {
         terminateCompletionRequest();
         return;
      }
      
      if ((request_ != null) &&
          !request_.isTerminated() &&
          (request_.getCompletionPosition().compareTo(completionPosition) == 0))
      {
         request_.updateUI();
      }
      else
      {
         terminateCompletionRequest();
         
         completionContext_.withUpdatedDoc(new CommandWithArg<String>() {

            @Override
            public void execute(String docPath)
            {
               request_ = new CppCompletionRequest(
                  docPath,
                  CppCompletionUtils.getCompletionPosition(docDisplay_),
                  docDisplay_,
                  completionRequestInvalidation_.getInvalidationToken(),
                  explicit);
            }
         });
         
      }
   }
     
   private CompletionListPopupPanel getCompletionPopup()
   {
      CompletionListPopupPanel popup = request_ != null ?
            request_.getCompletionPopup() : null;
      return popup;
   }
   
   private void terminateCompletionRequest()
   {
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

   private boolean isCursorInRMode()
   {
      String m = docDisplay_.getLanguageMode(docDisplay_.getCursorPosition());
      if (m == null)
         return false;
      if (m.equals(TextFileType.R_LANG_MODE))
         return true;
      return false;
   }
   
   private final DocDisplay docDisplay_;
   private final CppCompletionContext completionContext_;
   private CppCompletionRequest request_;
   private final InitCompletionFilter initFilter_ ;
   private final CompletionManager rCompletionManager_;
   private final Invalidation completionRequestInvalidation_ = new Invalidation();
   
  

}
