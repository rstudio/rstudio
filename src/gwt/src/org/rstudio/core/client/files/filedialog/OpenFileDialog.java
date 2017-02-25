/*
 * OpenFileDialog.java
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
package org.rstudio.core.client.files.filedialog;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

public class OpenFileDialog extends FileDialog
{
   public OpenFileDialog(String title,
                         FileSystemContext context,
                         String filter,
                         boolean canChooseDirectories,
                         ProgressOperationWithInput<FileSystemItem> operation)
   {
      super(title, null, "Open", false, false, false, context, filter, 
            operation);
      
      canChooseDirectories_ = canChooseDirectories;
   }
   
   @Override
   public boolean shouldAccept()
   {
      if (canChooseDirectories_)
      {
         FileSystemItem item = browser_.getSelectedItem();
         if (item == null || item.isDirectory())
            return true;
      }
      
      return super.shouldAccept();
   }
   
   @Override
   protected FileSystemItem getSelectedItem()
   {
      FileSystemItem item = browser_.getSelectedItem();
      if (item == null)
         item = browser_.getCurrentDirectory();
      return item;
   }
   
   protected final boolean canChooseDirectories_;
}
