/*
 * FileChooserTextBox.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Focusable;

public class FileChooserTextBox extends TextBoxWithButton
{
   
   
   public FileChooserTextBox(String label, Focusable focusAfter)
   {
      this(label, "", focusAfter, null);
   }
  
   public FileChooserTextBox(String label, 
                             String emptyLabel,
                             final Focusable focusAfter,
                             final Command onChosen)
   {
      super(label, emptyLabel, "Browse...", null);
      
      
      
      addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
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
                        indicator.onCompleted();
                        if (focusAfter != null)
                           focusAfter.setFocus(true);
                        if (onChosen != null)
                           onChosen.execute();
                     }
                  });
         }
      });
      
   }    
}
