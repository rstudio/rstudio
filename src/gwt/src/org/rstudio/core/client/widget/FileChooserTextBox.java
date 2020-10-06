/*
 * FileChooserTextBox.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Focusable;

public class FileChooserTextBox extends TextBoxWithButton
{
   /**
    * @param label label text
    * @param emptyLabel placeholder text
    * @param uniqueId unique ElementId suffix for this instance
    * @param buttonDisabled
    * @param focusAfter where to put focus after file is chosen
    * @param onChosen file chosen callback
    */
   public FileChooserTextBox(String label,
                             String emptyLabel,
                             ElementIds.TextBoxButtonId uniqueId,
                             boolean buttonDisabled,
                             final Focusable focusAfter,
                             final Command onChosen)
   {
      this(label, null, emptyLabel, uniqueId, buttonDisabled, focusAfter, onChosen);
   }

   /**
    * @param existingLabel label control to associate with textbox
    * @param emptyLabel placeholder text
    * @param uniqueId unique ElementId suffix for this instance
    * @param buttonDisabled
    * @param focusAfter where to put focus after file is chosen
    * @param onChosen file chosen callback
    */
   public FileChooserTextBox(FormLabel existingLabel,
                             String emptyLabel,
                             ElementIds.TextBoxButtonId uniqueId,
                             boolean buttonDisabled,
                             final Focusable focusAfter,
                             final Command onChosen)
   {
      this(null, existingLabel, emptyLabel, uniqueId, buttonDisabled, focusAfter, onChosen);
   }

   private FileChooserTextBox(String label,
                              FormLabel existingLabel,
                              String emptyLabel,
                              ElementIds.TextBoxButtonId uniqueId,
                              boolean buttonDisabled,
                              final Focusable focusAfter,
                              final Command onChosen)
   {
      super(label, existingLabel, emptyLabel, "Browse...",
            null, /* helpButton */
            uniqueId,
            true, /* readOnly */
            null /* clickHandler */);

      if (buttonDisabled)
      {
         setReadOnly(false);
         getButton().setEnabled(false);

         getTextBox().addChangeHandler(new ChangeHandler()
         {
            public void onChange(ChangeEvent event)
            {
               setText(getTextBox().getText());
            }
         });
      }

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
