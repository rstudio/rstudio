/*
 * FileOrUrlChooserTextBox.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.core.client.widget;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Focusable;

public class FileOrUrlChooserTextBox extends TextBoxWithButton
{
   private static String browseModeCaption_ = "Browse...";
   private static String updateModeCaption_ = "Update";
   private Boolean updateMode_ = false;
   private String lastTextBoxValue_;
   private int checkTextBoxInterval_ = 250;
   private final Operation updateOperation_;
   
   public FileOrUrlChooserTextBox(String label, Operation updateOperation, final Focusable focusAfter)
   {
      super(label, "", browseModeCaption_, null);
      
      updateOperation_ = updateOperation;
      
      super.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> arg0)
         {
            updateOperation_.execute();
         }
      });
      
      addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            if (updateMode_)
            {
               updateOperation_.execute();
            }
            else
            {
               RStudioGinjector.INSTANCE.getFileDialogs().openFile(
                     "Choose File",
                     RStudioGinjector.INSTANCE.getRemoteFileSystemContext(),
                     FileSystemItem.createFile(getText()),
                     new ProgressOperationWithInput<FileSystemItem>()
                     {
                        public void execute(FileSystemItem input,
                                            ProgressIndicator indicator)
                        {
                           if (input == null)
                              return;
   
                           setText(input.getPath());
                           preventModeChange();
                           
                           indicator.onCompleted();
                           if (focusAfter != null)
                              focusAfter.setFocus(true);
                        }
                     });
            }
         }
      });
      
      setReadOnly(false);
      
      checkForTextBoxChange();
   }
   
   @Override
   public String getText()
   {
      return getTextBox().getText();
   }
   
   @Override
   public void onDetach()
   {
      checkTextBoxInterval_ = 0;
   }
   
   private void checkForTextBoxChange()
   {
      if (checkTextBoxInterval_ == 0)
         return;
      
      // Check continuously for changes in the textbox to reliably detect changes even when OS pastes text
      new Timer()
      {
         @Override
         public void run()
         {
            if (lastTextBoxValue_ != null && getTextBox().getText() != lastTextBoxValue_)
            {
               switchToUpdateMode(!getTextBox().getText().isEmpty());
            }
            
            lastTextBoxValue_ = getTextBox().getText();
            checkForTextBoxChange();
         }
      }.schedule(checkTextBoxInterval_);
   }
   
   private void preventModeChange()
   {
      lastTextBoxValue_ = getTextBox().getText();
   }
   
   private void switchToUpdateMode(Boolean updateMode)
   {
      if (updateMode_ != updateMode)
      {
         updateMode_ = updateMode;
         if (updateMode)
         {
            getButton().setText(updateModeCaption_);
         }
         else
         {
            getButton().setText(browseModeCaption_);
         }
      }
   }
}