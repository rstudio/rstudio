/*
 * SaveFileDialog.java
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
package org.rstudio.core.client.files.filedialog;

import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

public class SaveFileDialog extends FileDialog
{

   public SaveFileDialog(String title,
                         FileSystemContext context,
                         String defaultExtension,
                         boolean forceDefaultExtension,
                         ProgressOperationWithInput<FileSystemItem> operation)
   {
      super(title, null, "Save", true, true, context, "", operation);
      defaultExtension_ = defaultExtension;
      forceDefaultExtension_ = forceDefaultExtension;
   }

   @Override
   protected void showOverwritePrompt()
   {
      context_.messageDisplay().showYesNoMessage(
            MessageDisplay.MSG_WARNING,
            "Confirm Overwrite",
            "This file already exists. Do you want to replace it?",
            false,
            new Operation()
            {
               public void execute()
               {
                  accept();
               }
            },
            null,
            true);
   }

   @Override
   protected String mungeFilename(String filename)
   {
      if (StringUtil.isNullOrEmpty(defaultExtension_))
         return filename;
      else
      {
         // if there is no extension then we need to add one
         String ext = FileSystemItem.getExtensionFromPath(filename);
         if (ext.length() == 0 ||
             (forceDefaultExtension_ && (ext != defaultExtension_)))
         {
            return filename + defaultExtension_;
         }
         else
         {
            return filename;
         }
      }
   }

   private final String defaultExtension_;
   private final boolean forceDefaultExtension_;
}
