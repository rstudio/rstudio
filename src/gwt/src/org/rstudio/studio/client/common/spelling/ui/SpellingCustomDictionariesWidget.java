/*
 * SpellingCustomDictionariesWidget.java
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
package org.rstudio.studio.client.common.spelling.ui;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.LabelWithHelp;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.spelling.SpellingService;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class SpellingCustomDictionariesWidget extends Composite
{
   public SpellingCustomDictionariesWidget()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      VerticalPanel panel = new VerticalPanel();
      
      HorizontalPanel dictionariesPanel = new HorizontalPanel();
      listBox_ = new ListBox();
      listBox_.setMultipleSelect(false);
      listBox_.addStyleName(RES.styles().listBox());
      listBox_.getElement().<SelectElement>cast().setSize(4);
      dictionariesPanel.add(listBox_);
      
      VerticalPanel buttonPanel = new VerticalPanel();
      SmallButton buttonAdd = createButton("Add...");
      buttonAdd.addClickHandler(addButtonClicked_);
      buttonPanel.add(buttonAdd);
      SmallButton buttonRemove = createButton("Remove...");
      buttonRemove.addClickHandler(removeButtonClicked_);
      buttonPanel.add(buttonRemove);
      dictionariesPanel.add(buttonPanel);
      
      panel.add(new LabelWithHelp("Custom dictionaries:",
            "custom_dictionaries",
            "Help on custom spelling dictionaries",
            listBox_));
      panel.add(dictionariesPanel);
      
      initWidget(panel);
   }

   @Inject
   void initialize(SpellingService spellingService,
                   GlobalDisplay globalDisplay,
                   FileDialogs fileDialogs,
                   RemoteFileSystemContext fileSystemContext)
   {
      spellingService_ = spellingService;
      globalDisplay_= globalDisplay;
      fileDialogs_ = fileDialogs;
      fileSystemContext_ = fileSystemContext;
      customDictsModified_ = false;
   }
   
   public void setDictionaries(JsArrayString dictionaries)
   {
      listBox_.clear();
      for (int i=0; i<dictionaries.length(); i++)
         listBox_.addItem(dictionaries.get(i));
   }
   
   public void setProgressIndicator(ProgressIndicator progressIndicator)
   {
      progressIndicator_ = progressIndicator;
   }
      
   
   
   private ClickHandler addButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         fileDialogs_.openFile(
            "Add Custom Dictionary (*.dic)", 
            fileSystemContext_, 
            FileSystemItem.home(), 
            "Dictionaries (*.dic)",
            new ProgressOperationWithInput<FileSystemItem>() {

               @Override
               public void execute(FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  indicator.onCompleted();
                  if (input == null)
                     return;
                  
                  progressIndicator_.onProgress("Adding dictionary...");

                  spellingService_.addCustomDictionary(
                                                  input.getPath(),
                                                  customDictRequestCallback_);

                  customDictsModified_ = true;
               }
               
            }); 
      }
   };
  
   
   private ClickHandler removeButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         // get selected index
         int index = listBox_.getSelectedIndex();
         if (index != -1)
         {
            final String dictionary = listBox_.getValue(index);
            globalDisplay_.showYesNoMessage(
                  MessageDialog.WARNING, 
                  "Confirm Remove", 
                  "Are you sure you want to remove the " + dictionary + 
                  " custom dictionary?",
                  new Operation() {
                     @Override
                     public void execute()
                     {
                        progressIndicator_.onProgress("Removing dictionary...");

                        spellingService_.removeCustomDictionary(
                                                  dictionary,
                                                  customDictRequestCallback_);

                        customDictsModified_ = true;
                     }
                  },
                  false);
            
         }
      }
   };
 
   private ServerRequestCallback<JsArrayString> customDictRequestCallback_ =
                                 new ServerRequestCallback<JsArrayString>() {

      @Override
      public void onResponseReceived(JsArrayString dictionaries)
      {
         progressIndicator_.onCompleted();
         setDictionaries(dictionaries);
      }
      
      @Override
      public void onError(ServerError error)
      {
         progressIndicator_.onError(error.getUserMessage());
      }
   };
   
   private SmallButton createButton(String caption)
   {
      SmallButton button = new SmallButton(caption);
      button.addStyleName(RES.styles().button());
      button.fillWidth();
      return button;
   }

   public boolean getCustomDictsModified()
   {
      return customDictsModified_;
   }

   private final ListBox listBox_;
   private SpellingService spellingService_;
   private ProgressIndicator progressIndicator_;
   private GlobalDisplay globalDisplay_;
   private FileDialogs fileDialogs_;
   private RemoteFileSystemContext fileSystemContext_;
   private boolean customDictsModified_;

   static interface Styles extends CssResource
   {
      String helpButton();
      String listBox();
      String button();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("SpellingCustomDictionariesWidget.css")
      Styles styles();
   }
   
   static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

}
