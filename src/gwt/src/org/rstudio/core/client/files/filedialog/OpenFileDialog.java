/*
 * OpenFileDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.files.filedialog;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.event.logical.shared.SelectionEvent;

public class OpenFileDialog extends FileDialog
{
   public OpenFileDialog(String title,
                         String label,
                         FileSystemContext context,
                         String filter,
                         boolean canChooseDirectories,
                         ProgressOperationWithInput<FileSystemItem> operation)
   {
      super(title, null, Roles.getDialogRole(), label, false, false, false, context, filter, operation);
      canChooseDirectories_ = canChooseDirectories;
   }

   @Override
   public boolean shouldAccept()
   {
      FileSystemItem item = browser_.getSelectedItem();
      String fileInput = browser_.getFilename().trim();

      if (canChooseDirectories_)
      {
         // if we have no user input nor a selected item,
         // interpret this as a request to open the active
         // directory as an RStudio project
         if (item == null && fileInput.isEmpty())
            return true;
      }

      // if the user has selected a directory, and there
      // is no user input, interpret that as a request to
      // navigate into that directory
      if (item != null && fileInput.isEmpty() && item.isDirectory())
      {
         cd(item.getPath());
         return false;
      }

      return super.shouldAccept();
   }

   @Override
   public void onNavigated()
   {
      super.onNavigated();
      browser_.setFilename("");
   }

   @Override
   protected FileSystemItem getSelectedItem()
   {
      FileSystemItem item = super.getSelectedItem();
      if (item == null)
         item = browser_.getCurrentDirectory();
      return item;
   }

   @Override
   public void onSelection(SelectionEvent<FileSystemItem> event)
   {
      super.onSelection(event);

      // clear the active filename whenever a directory
      // is selected -- this allows us to disambiguate
      // 'fresh' from 'stale' user input; ie, tell whether
      // the user is navigating in the widget or typing in
      // the file name textbox
      FileSystemItem item = event.getSelectedItem();
      if (item != null && item.isDirectory())
         browser_.setFilename("");
   }

   protected final boolean canChooseDirectories_;
}