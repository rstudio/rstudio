/*
 * ChooseFolderDialog2.java
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
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

import java.util.ArrayList;

public class ChooseFolderDialog2 extends FileSystemDialog
{
   public ChooseFolderDialog2(String title,
                              String label,
                              FileSystemContext context,
                              boolean allowFolderCreation,
                              ProgressOperationWithInput<FileSystemItem> operation)
   {
      super(title, null, Roles.getDialogRole(), label, context, "", allowFolderCreation, operation);
   }

   @Override
   public String getFilenameLabel()
   {
      return "Folder";
   }

   @Override
   public Widget createMainWidget()
   {
      Widget widget = super.createMainWidget();
      browser_.setFilenameEnabled(false);
      browser_.getFilenameStyle().setBackgroundColor("transparent");
      return widget;
   }

   @Override
   public FileSystemItem[] ls()
   {
      FileSystemItem[] items = super.ls();
      ArrayList<FileSystemItem> dirs = new ArrayList<FileSystemItem>();
      for (FileSystemItem item : items)
         if (item.isDirectory())
            dirs.add(item);
      return dirs.toArray(new FileSystemItem[0]);
   }

   @Override
   public void onNavigated()
   {
      super.onNavigated();
      browser_.setFilename(context_.pwd());
   }

   @Override
   public void onSelection(SelectionEvent<FileSystemItem> event)
   {
      super.onSelection(event);
      FileSystemItem item = event.getSelectedItem();
      if (item != null)
         browser_.setFilename(item.getPath());
      else
         browser_.setFilename("");
   }

   @Override
   protected FileSystemItem getSelectedItem()
   {
      return FileSystemItem.createDir(browser_.getFilename());
   }
}
