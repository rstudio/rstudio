/*
 * SpellingSandboxDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.spelling.view;

import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class SpellingSandboxDialog extends ModalDialogBase
{
   @Inject
   public SpellingSandboxDialog(GlobalDisplay globalDisplay, 
                                SpellingServerOperations server)
   {  
      globalDisplay_ = globalDisplay;
      server_ = server;
    
      setText("Spelling Sandbox");
      
      ThemedButton closeButton = new ThemedButton("Close", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            closeDialog();
         }
      });
      addCancelButton(closeButton);
   }
   
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel mainPanel = new VerticalPanel();
      mainPanel.setWidth("300px");
      
      txtWord_ = new TextBox();
      txtWord_.getElement().getStyle().setMarginBottom(7, Unit.PX);
      mainPanel.add(txtWord_);
      
      
      ThemedButton btnCheck = new ThemedButton("Check", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            checkSpelling(txtWord_.getText().trim());
         }
      });
      mainPanel.add(btnCheck);
      
      ThemedButton btnSuggest = new ThemedButton("Suggest", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            suggestionList(txtWord_.getText().trim());
         }
      });
      mainPanel.add(btnSuggest); 
      
      return mainPanel;
   } 
   
   @Override
   public void onDialogShown()
   {
      txtWord_.setFocus(true);
   }
   
   private void checkSpelling(String word)
   {
      server_.checkSpelling(word, new SimpleRequestCallback<Boolean>() {
         @Override
         public void onResponseReceived(Boolean isCorrect)
         {
            showResponse("Check Spelling", isCorrect ? "Correct" : "Incorrect");
         }
      });
      
   }
   
   private void suggestionList(String word)
   {
      server_.suggestionList(word, new SimpleRequestCallback<JsArrayString>() {
         @Override
         public void onResponseReceived(JsArrayString suggestions)
         {
            showResponse("Suggest", suggestions.join());
         }
      });
      
   }
   
   private void showResponse(String request, String response)
   {
      globalDisplay_.showMessage(MessageDialog.INFO, request, response);
         
      txtWord_.selectAll();
      txtWord_.setFocus(true);
   }
   
   private TextBox txtWord_;
   private final GlobalDisplay globalDisplay_;
   private final SpellingServerOperations server_;
  
}
