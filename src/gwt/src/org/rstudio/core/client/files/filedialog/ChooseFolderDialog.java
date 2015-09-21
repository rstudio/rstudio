/*
 * ChooseFolderDialog.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

import java.util.ArrayList;


public class ChooseFolderDialog extends FileSystemDialog
{
   public ChooseFolderDialog(String title,
                             FileSystemContext context,
                             boolean allowFolderCreation,
                             ProgressOperationWithInput<FileSystemItem> operation)
   {
      super(title, null, "Choose", context, "", allowFolderCreation, operation);
   }

   @Override
   public String getFilenameLabel()
   {
      return "Folder";
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
   public Widget createMainWidget()
   {
      Widget mainWidget = super.createMainWidget();

      browser_.addKeyUpHandler(new KeyUpHandler()
      {
         public void onKeyUp(KeyUpEvent event)
         {
            maybeInvalidateSelection();
         }
      });
      browser_.addKeyPressHandler(new KeyPressHandler()
      {
         public void onKeyPress(KeyPressEvent event)
         {
            maybeInvalidateSelection();
         }
      });

      return mainWidget;
   }

   private void maybeInvalidateSelection()
   {
      String selectedValue = browser_.getSelectedValue();
      if (selectedValue != null && !selectedValue.equals(browser_.getFilename()))
         browser_.setSelectedRow(null);
   }

   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      browser_.setDirectoryFocus(true);
   }

   @Override
   public void onNavigated()
   {
      super.onNavigated();
      FileSystemItem[] dirs = context_.parseDir(context_.pwd());
      browser_.setFilename(dirs[dirs.length - 1].getName());
   }

   @Override
   public void onSelection(SelectionEvent<FileSystemItem> event)
   {
      super.onSelection(event);
      FileSystemItem item = event.getSelectedItem();
      if (item != null && item.isDirectory())
         browser_.setFilename(item.getName());
   }

   @Override
   protected boolean shouldAccept()
   {
      return getEffectiveDirectoryWithValidation() != null;
   }

   private String getEffectiveDirectoryWithValidation()
   {
      browser_.setFilename(browser_.getFilename().trim());
      String name = browser_.getFilename();

      // This handles the special case of "~"
      if (context_.isAbsolute(name))
         return name;

      if (name.length() == 0)
         return context_.pwd();

      if (name.contains("/"))
         return context_.combine(context_.pwd(), name);

      // If an item is selected (highlighted) in the browse control, then
      // only use it IF it is the same as the name in the name textbox. The
      // name textbox takes precedence.
      FileSystemItem selectedItem = browser_.getSelectedItem();
      if (selectedItem != null && selectedItem.getName().equals(name))
            return selectedItem.getPath();

      if (name.equals(context_.pwdItem().getName()))
      {
         // The identity condition
         return context_.pwd();
      }

      FileSystemItem item = context_.itemForName(name,
                                                 true,
                                                 false);
      if (item == null)
      {
         onError("The folder does not exist.");
         return null;
      }

      return item.getPath();
   }

   @Override
   protected FileSystemItem getSelectedItem()
   {
      return FileSystemItem.createDir(getEffectiveDirectoryWithValidation());
   }
}
