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
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionListPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionUtils;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletion;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletionResult;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

public class CppCompletionManager implements CompletionManager
{
   public CppCompletionManager(DocDisplay docDisplay,
                               InitCompletionFilter initFilter,
                               CppServerOperations server,
                               CppCompletionContext completionContext,
                               CompletionManager rCompletionManager)
   {
      docDisplay_ = docDisplay;
      initFilter_ = initFilter;
      server_ = server;
      completionContext_ = completionContext;
      rCompletionManager_ = rCompletionManager; 
      docDisplay_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            invalidatePendingRequests();
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
         closeCompletionPopup();
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
      else if ((popup_ == null) && shouldComplete(null))
      {
         suggestCompletions(); 
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
      
      // if there is no completion popup visible then check
      // for a completion request or help/goto key-combo
      int modifier = KeyboardShortcut.getModifierValue(event);
      if (popup_ == null)
      { 
         // check for user completion key combo 
         if (CompletionUtils.isCompletionRequest(event, modifier) &&
             shouldComplete(event)) 
         {
            suggestCompletions();
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
         // bare modifiers do nothing
         int keyCode = event.getKeyCode();
         
         // chrome on ubuntu now sends this before every keydown
         // so we need to explicitly ignore it. see:
         // https://github.com/ivaynberg/select2/issues/2482
         if (keyCode == KeyCodes.KEY_WIN_IME)
         {
            return false ;
         }
         
         if (keyCode == KeyCodes.KEY_SHIFT ||
             keyCode == KeyCodes.KEY_CTRL ||
             keyCode == KeyCodes.KEY_ALT)
         {          
            return false ; 
         }
         
         // escape and left keys close the popup
         if (event.getKeyCode() == KeyCodes.KEY_ESCAPE ||
             event.getKeyCode() == KeyCodes.KEY_LEFT)
         {
            invalidatePendingRequests();
            return true;
         }
          
         // enter/tab/right accept the current selection
         else if (event.getKeyCode() == KeyCodes.KEY_ENTER ||
                  event.getKeyCode() == KeyCodes.KEY_TAB ||
                  event.getKeyCode() == KeyCodes.KEY_RIGHT)
         {
            context_.onSelected(popup_.getSelectedValue());
            return true;
         }
         
         // basic navigation keys
         else if (event.getKeyCode() == KeyCodes.KEY_UP)
            return popup_.selectPrev();
         else if (event.getKeyCode() == KeyCodes.KEY_DOWN)
            return popup_.selectNext();
         else if (event.getKeyCode() == KeyCodes.KEY_PAGEUP)
            return popup_.selectPrevPage() ;
         else if (event.getKeyCode() == KeyCodes.KEY_PAGEDOWN)
            return popup_.selectNextPage() ;
         else if (event.getKeyCode() == KeyCodes.KEY_HOME)
            return popup_.selectFirst() ;
         else if (event.getKeyCode() == KeyCodes.KEY_END)
            return popup_.selectLast() ;
         
         // non c++ identifier keys (that aren't navigational) close the popup
         else if (!isCppIdentifierKey(event))
         {
            invalidatePendingRequests();
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
         return rCompletionManager_.previewKeyPress(c);
      
      // if the popup is showing and this is a valid C++ identifier key
      // then re-execute the completion request
      if ((popup_ != null) && isCppIdentifierChar(c))
      {
         suggestCompletions(); 
         return false;
      }
      
      // if there is no popup and this key should begin a completion
      // then do that
      else if ((popup_ == null) && triggerCompletion(c))
      {
         suggestCompletions();  
         return false;
      }
      
      else if (CompletionUtils.handleEncloseSelection(docDisplay_, c))
      {
         return true;
      }
      else
      {
         return false;
      }
   }
   
   private boolean triggerCompletion(char c)
   {
      if (c == '.')
      {
         return true;
      }
      else
      {
         return false;
      }
   }
   
   private void suggestCompletions()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
         @Override
         public void execute()
         {
            doSuggestCompletions();  
         }
      });
   }
   
   private void doSuggestCompletions()
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
     
      // scratch any existing requests
      invalidatePendingRequests() ;
      
     
      
      context_ = new CompletionRequestContext(
                        completionRequestInvalidation_.getInvalidationToken());
      
      final Position cursorPos = docDisplay_.getCursorPosition();
      completionContext_.withUpdatedDoc(new CommandWithArg<String>() {

         @Override
         public void execute(String docPath)
         {
            server_.getCppCompletions(docPath, 
                                      cursorPos.getRow() + 1, 
                                      cursorPos.getColumn() + 1, 
                                      context_);
            
         }
         
      });
   }
  
   
   private final class CompletionRequestContext extends
                                 ServerRequestCallback<CppCompletionResult>
   {  
      public CompletionRequestContext(Invalidation.Token token)
      {
         invalidationToken_ = token;
      }
      
      @Override 
      public void onResponseReceived(CppCompletionResult result)
      {
         if (invalidationToken_.isInvalid())
            return ;
         
         // null result means that completion is not supported for this file
         if (result == null)
            return;
         
         // close existing popup
         closeCompletionPopup();     
          
         if (result.getCompletions().length() == 0)
         {
            popup_ = new CompletionListPopupPanel(new String[0]);
            popup_.setText("(No matching commands)");
         }
         else
         {
            JsArray<CppCompletion> completions = result.getCompletions();
            String[] entries = new String[completions.length()];
            for (int i = 0; i < completions.length(); i++)
               entries[i] = completions.get(i).getText();
            popup_ = new CompletionListPopupPanel(entries);
         }
         
         popup_.setMaxWidth(docDisplay_.getBounds().getWidth());
         popup_.setPopupPositionAndShow(new PositionCallback()
         {
            public void setPosition(int offsetWidth, int offsetHeight)
            {
               Rectangle bounds = docDisplay_.getCursorBounds();

               int top = bounds.getTop() + bounds.getHeight();
              
               popup_.selectFirst();
               popup_.setPopupPosition(bounds.getLeft(), top);
            }
         });

         popup_.addSelectionCommitHandler(new SelectionCommitHandler<String>()
         {
            public void onSelectionCommit(SelectionCommitEvent<String> e)
            {
               onSelected(e.getSelectedItem());
            }
         });
         
         popup_.addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(CloseEvent<PopupPanel> event)
            {
               popup_ = null;          
            } 
         });
      }
      
      @Override
      public void onError(ServerError error)
      {
         if (invalidationToken_.isInvalid())
            return ;
         
         popup_ = new CompletionListPopupPanel(new String[0]);
         popup_.setText(error.getUserMessage());
      }
      
      public void onSelected(String completion)
      {
         if (invalidationToken_.isInvalid())
            return;
         
         closeCompletionPopup();
          
         docDisplay_.insertCode(completion);
      }

      private final Invalidation.Token invalidationToken_;
   }
   
   private void invalidatePendingRequests()
   {
      completionRequestInvalidation_.invalidate();
      closeCompletionPopup();
   }
   
  
   private void closeCompletionPopup()
   {
      if (popup_ != null)
      {
         popup_.hide();
         popup_ = null;
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
   
   private static boolean isCppIdentifierKey(NativeEvent event)
   {
      if (event.getAltKey() || event.getCtrlKey() || event.getMetaKey())
         return false ;
      
      int keyCode = event.getKeyCode() ;
      if (keyCode >= 'a' && keyCode <= 'z')
         return true ;
      if (keyCode >= 'A' && keyCode <= 'Z')
         return true ;
      if (keyCode == 189 && event.getShiftKey()) // underscore
         return true ;
     
      if (event.getShiftKey())
         return false ;
      
      if (keyCode >= '0' && keyCode <= '9')
         return true ;
      
      return false ;
   }
   
   public static boolean isCppIdentifierChar(char c)
   {
      return ((c >= 'a' && c <= 'z') || 
              (c >= 'A' && c <= 'Z') || 
              (c >= '0' && c <= '9') ||
               c == '_');
   }
   
   private final DocDisplay docDisplay_;
   private CompletionListPopupPanel popup_;
   private final CppCompletionContext completionContext_;
   private final CppServerOperations server_;
   private CompletionRequestContext context_;
   private final InitCompletionFilter initFilter_ ;
   private final CompletionManager rCompletionManager_;
   private final Invalidation completionRequestInvalidation_ = new Invalidation();
   
  

}
