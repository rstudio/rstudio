/*
 * CppCompletionRequest.java
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

import java.util.ArrayList;

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionListPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletion;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletionResult;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Inject;

public class CppCompletionRequest 
                           extends ServerRequestCallback<CppCompletionResult>
{
   public CppCompletionRequest(String docPath,
                               Position completionPosition,
                               DocDisplay docDisplay, 
                               Invalidation.Token token,
                               boolean explicit)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      docDisplay_ = docDisplay;
      completionPosition_ = completionPosition;
      invalidationToken_ = token;
      explicit_ = explicit;
      
      server_.getCppCompletions(docPath, 
                                completionPosition.getRow() + 1, 
                                completionPosition.getColumn() + 1, 
                                this);
   }

   @Inject
   void initialize(CppServerOperations server)
   {
      server_ = server;
   }
   
   public Position getCompletionPosition()
   {
      return completionPosition_;
   }
   
   public CompletionListPopupPanel getCompletionPopup()
   {
      return popup_;
   }
     
   public void updateUI()
   {
      // if we don't have the completion list back from the server yet
      // then just ignore this (this function will get called again when
      // the request completes)
      if (completions_ != null)
      {  
         // discover text already entered
         String entered = docDisplay_.getCode(
             completionPosition_, docDisplay_.getCursorPosition());
         
         // build list of entries (filter on text already entered)
         ArrayList<String> entries = new ArrayList<String>();
         for (int i = 0; i < completions_.length(); i++)
         {
            String completion = completions_.get(i).getText();
            if ((entered.length() == 0) || completion.startsWith(entered))
               entries.add(completion);
         }
         
         // create the popup
         createCompletionPopup(entries.toArray(new String[0]));
      }
   }
   
   public void terminate()
   {
      closeCompletionPopup();
      terminated_ = true;
   }
   
   public boolean isTerminated()
   {
      return terminated_;
   }
   
   @Override
   public void onResponseReceived(CppCompletionResult result)
   {
      if (invalidationToken_.isInvalid())
         return;
      
      // null result means that completion is not supported for this file
      if (result == null)
         return;    
       
      // get the completions
      completions_ = result.getCompletions();
      
      // check for auto-accept
      if ((completions_.length() == 1) && explicit_)
      {
         applyValue(completions_.get(0).getText());
      }
      // check for none found condition on explicit completion
      else if ((completions_.length() == 0) && explicit_)
      {
         createCompletionPopup("(No matches)");
      }
      // otherwise just update the ui (apply filter, etc.)
      else
      {
         updateUI();
      }
   }
   
   private void createCompletionPopup(String message)
   {
      closeCompletionPopup();
      popup_ = new CompletionListPopupPanel(new String[0]);
      popup_.setText(message);
      showCompletionPopup();
   }
   
   private void createCompletionPopup(String[] entries)
   {
      closeCompletionPopup();
      if (entries.length > 0)
      {
         popup_ = new CompletionListPopupPanel(entries);
         showCompletionPopup();
      }
   }
   
   private void showCompletionPopup()
   {
      popup_.setMaxWidth(docDisplay_.getBounds().getWidth());
      popup_.setPopupPositionAndShow(new PositionCallback()
      {
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            popup_.selectFirst();
            
            InputEditorPosition position = 
                  docDisplay_.createInputEditorPosition(completionPosition_);  
            Rectangle bounds = docDisplay_.getPositionBounds(position);
            
            int windowBottom = Window.getScrollTop() + Window.getClientHeight() ;
            int cursorBottom = bounds.getBottom() ;
            
            if (windowBottom - cursorBottom >= offsetHeight)
               popup_.setPopupPosition(bounds.getLeft(), cursorBottom) ;
            else
               popup_.setPopupPosition(bounds.getLeft(), 
                                       bounds.getTop() - offsetHeight) ;
         }
      });

      popup_.addSelectionCommitHandler(new SelectionCommitHandler<String>()
      {
         public void onSelectionCommit(SelectionCommitEvent<String> e)
         {
            applyValue(e.getSelectedItem());
         }
      });
      
      popup_.addCloseHandler(new CloseHandler<PopupPanel>() {

         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            popup_ = null; 
            terminated_ = true;
         } 
      });
      
   }
   
   private void closeCompletionPopup()
   {
      if (popup_ != null)
      {
         popup_.removeFromParent();
         popup_ = null;
      }
   }
   
   @Override
   public void onError(ServerError error)
   {
      if (invalidationToken_.isInvalid())
         return;
      
      if (explicit_)
         createCompletionPopup(error.getUserMessage());
   }
   
   public void applyValue(String completion)
   {
      if (invalidationToken_.isInvalid())
         return;
      
      terminate();
     
      docDisplay_.setFocus(true); 
      docDisplay_.setSelection(docDisplay_.createSelection(
            completionPosition_, docDisplay_.getCursorPosition()));
      docDisplay_.replaceSelection(completion, true) ; 
   }
   
   private CppServerOperations server_;
  
   private final DocDisplay docDisplay_; 
   private final boolean explicit_;
   private final Invalidation.Token invalidationToken_;
   
   private final Position completionPosition_;
   
   private CompletionListPopupPanel popup_;
   private JsArray<CppCompletion> completions_;
   
   private boolean terminated_ = false;
}
