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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class SpellingSandboxDialog extends ModalDialogBase
{
	
   interface Binder extends UiBinder<Widget, SpellingSandboxDialog>{}
   
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
	  Widget mainPanel = uiBinder.createAndBindUi(this);
	  //setWidget(mainPanel);
      
      changeButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            checkSpelling(txtWord_.getText().trim());
         }
      });
      
 
      changeAllButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            suggestionList(txtWord_.getText().trim());
         }
      });
      
      listBox_.addChangeHandler(new ChangeHandler() {
         public void onChange(ChangeEvent event) 
         {
            int i = listBox_.getSelectedIndex();
            if (i >= 0)
            {
        	   txtWord_.setText(listBox_.getValue(i));
            }	
         }	
      });
      
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
        	listBox_.clear();
        	for (int i = 0; i < suggestions.length(); i++)
        	{
        		listBox_.addItem(suggestions.get(i));
        	}
         }
      });
      
   }
   
   private void showResponse(String request, String response)
   {
      globalDisplay_.showMessage(MessageDialog.INFO, request, response);
         
      txtWord_.selectAll();
      txtWord_.setFocus(true);
   }
   
   @UiFactory ThemedButton makeThemedButton()
   {
	   return new ThemedButton("");
   }
   
   private static Binder uiBinder = GWT.create(Binder.class);
   @UiField TextArea txtWord_;
   @UiField ListBox listBox_;
   @UiField ThemedButton changeButton_;
   @UiField ThemedButton changeAllButton_;
   @UiField ThemedButton ignoreButton_;
   @UiField ThemedButton ignoreAllButton_;
   private final GlobalDisplay globalDisplay_;
   private final SpellingServerOperations server_;
  
}
