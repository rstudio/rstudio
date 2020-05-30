/*
 * CodeFilesList.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;

import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class CodeFilesList extends Composite
{
   public CodeFilesList()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      VerticalPanel panel = new VerticalPanel();
      panel.addStyleName(RES.styles().wizardMainColumn());
      
      HorizontalPanel labelPanel = new HorizontalPanel();
      FormLabel label = new FormLabel("Create package based on source files:");
      label.addStyleName(RES.styles().wizardTextEntryLabel());
      labelPanel.add(label);
      panel.add(labelPanel);
      
      HorizontalPanel dictionariesPanel = new HorizontalPanel();
      listBox_ = new ListBox();
      label.setFor(listBox_);
      listBox_.setMultipleSelect(true);
      listBox_.addStyleName(RES.styles().codeFilesListBox());
      listBox_.getElement().<SelectElement>cast().setSize(3);
      dictionariesPanel.add(listBox_);
      
      VerticalPanel buttonPanel = new VerticalPanel();
      SmallButton buttonAdd = createButton("Add...");
      buttonAdd.addClickHandler(addButtonClicked_);
      buttonPanel.add(buttonAdd);
      SmallButton buttonRemove = createButton("Remove");
      buttonRemove.addClickHandler(removeButtonClicked_);
      buttonPanel.add(buttonRemove);
      dictionariesPanel.add(buttonPanel);
      
      panel.add(dictionariesPanel);
      
      initWidget(panel);
   }

   @Inject
   void initialize(FileDialogs fileDialogs,
                   RemoteFileSystemContext fileSystemContext)
   {
      fileDialogs_ = fileDialogs;
      fileSystemContext_ = fileSystemContext;
   }
   
   public ArrayList<String> getCodeFiles()
   {
      ArrayList<String> codeFiles = new ArrayList<String>();
      for (int i=0; i<listBox_.getItemCount(); i++)
         codeFiles.add(listBox_.getItemText(i));
      return codeFiles;
   }
   
 
  
   private ClickHandler addButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         fileDialogs_.openFile(
            "Add Source File", 
            fileSystemContext_, 
            FileSystemItem.home(), 
            new ProgressOperationWithInput<FileSystemItem>() {

               @Override
               public void execute(FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  indicator.onCompleted();
                  if (input == null)
                     return;
                  
                 listBox_.addItem(input.getPath());
               }
               
            }); 
      }
   };
  
   
   private ClickHandler removeButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         while (listBox_.getSelectedIndex() != -1)
            listBox_.removeItem(listBox_.getSelectedIndex());
      }
   };
   
   private SmallButton createButton(String caption)
   {
      SmallButton button = new SmallButton(caption);
      button.addStyleName(RES.styles().codeFilesListButton());
      button.fillWidth();
      return button;
   }
   
   private final ListBox listBox_;
   private FileDialogs fileDialogs_;
   private RemoteFileSystemContext fileSystemContext_;
   
   static final NewProjectResources RES = NewProjectResources.INSTANCE;
}
