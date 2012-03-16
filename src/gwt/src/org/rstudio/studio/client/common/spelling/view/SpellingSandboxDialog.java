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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
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
      
      spellCheckButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            startSpellChecking();
         }
      });
      
      stopCheckButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            stopSpellChecking(false);
         }
      });
      stopCheckButton_.setEnabled(false);
      
      changeButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            changeWord();
         }
      });
      changeButton_.setEnabled(false);

      changeAllButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            changeAllWords();
         }
      });
      changeAllButton_.setEnabled(false);
      
      ignoreButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            findMisSpelledWord();
         }
      });
      ignoreButton_.setEnabled(false);

      addWordButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            addWordToDictionary();
         }
      });
      addWordButton_.setEnabled(false);

      txtWord_.setEnabled(false);
      listBox_.setEnabled(false);
            
      return mainPanel;
   } 
   
   private void startSpellChecking()
   {
      if (debugTxt_.isEmpty())
      {
         showResponse("Spell Checker", "Nothing to SpellCheck!");
         return;
      } 
      else
      {
    	  debugTxt_.startSpellChecking();
      }
      
      spellCheckButton_.setEnabled(false);
     	toggleDialog(true);
      
      findMisSpelledWord();
      
   }
   private void stopSpellChecking(boolean complete)
   {
      if (complete)
         showResponse("Spell Checker", "Complete!");
      
      debugTxt_.spellCheckComplete();
      
      spellCheckButton_.setEnabled(true);
      
      // Clear out word lists
      txtWord_.setText("");
     	listBox_.clear();
      
     	// Disable all buttons
     	toggleDialog(false);
   }
   
   private void toggleDialog(boolean onOff)
   {
      stopCheckButton_.setEnabled(onOff);
      changeButton_.setEnabled(onOff);
      changeAllButton_.setEnabled(onOff);
      ignoreButton_.setEnabled(onOff);
      addWordButton_.setEnabled(onOff);
      txtWord_.setEnabled(onOff);
      listBox_.setEnabled(onOff);
   }
   
   @Override
   public void onDialogShown()
   {
      debugTxt_.setFocus(true);
   }
   
   private void findMisSpelledWord()
   {
      toggleDialog(false);
      currentWord_ = debugTxt_.getNextWord();
      if (currentWord_.isEmpty())
      {
         stopSpellChecking(true);
         return;
      }
      server_.checkSpelling(currentWord_, new SimpleRequestCallback<Boolean>() {
         @Override
         public void onResponseReceived(Boolean isCorrect)
         {
            if (!isCorrect){
               toggleDialog(true);
               txtWord_.setText(currentWord_);
               suggestionList(currentWord_);
            } 
            else
            {
               findMisSpelledWord();
            }
         }
      });
   }
   
   private void changeWord()
   {
      int i = listBox_.getSelectedIndex();
      if (i >= 0)
      {
         debugTxt_.changeWord(currentWord_,listBox_.getValue(i));
         findMisSpelledWord();
      } 
      else if (currentWord_.compareTo(txtWord_.getText()) != 0)
      {
         debugTxt_.changeWord(currentWord_,txtWord_.getText());
         findMisSpelledWord();
      } 
      else
      {
         showResponse("Spell Checker", 
                      "Please choose a word from the Suggestion List!");
      }
   }
   
   private void changeAllWords()
   {
      int i = listBox_.getSelectedIndex();
      if (i >= 0)
      {
         debugTxt_.changeAllWords(currentWord_,listBox_.getValue(i));
         findMisSpelledWord();
      } 
      else if (currentWord_.compareTo(txtWord_.getText()) != 0)
      {
         debugTxt_.changeAllWords(currentWord_,txtWord_.getText());
         findMisSpelledWord();
      } 
      else
      {
         showResponse("Spell Checker", 
                      "Please choose a word from the Suggestion List!");
      }
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
   
   private void addWordToDictionary()
   {
      server_.addToDictionary(txtWord_.getText(), 
                              new SimpleRequestCallback<Boolean>() {
         @Override
         public void onResponseReceived(Boolean added)
         {
            if (!added){
               showResponse("Spell Checker", "Server Error!");
            } 
            else
            {
               findMisSpelledWord();
            }
         }
      });
   }
   
   private void showResponse(String request, String response)
   {
      globalDisplay_.showMessage(MessageDialog.INFO, request, response);
   }
   
   @UiFactory ThemedButton makeThemedButton()
   {
	   return new ThemedButton("");
   }
   
   private static Binder uiBinder = GWT.create(Binder.class);
   @UiField ListBox listBox_;
   @UiField SpellingSandboxDebugText debugTxt_;
   @UiField TextArea txtWord_;
   @UiField ThemedButton changeButton_;
   @UiField ThemedButton changeAllButton_;
   @UiField ThemedButton ignoreButton_;
   @UiField ThemedButton addWordButton_;
   @UiField ThemedButton spellCheckButton_;
   @UiField ThemedButton stopCheckButton_;
   @UiField VerticalPanel sandboxPanel_;
   private final GlobalDisplay globalDisplay_;
   private final SpellingServerOperations server_;
   private String currentWord_;
  
}
