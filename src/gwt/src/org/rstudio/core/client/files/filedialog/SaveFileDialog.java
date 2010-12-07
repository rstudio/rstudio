/*
 * SaveFileDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.FilenameTransform;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

public class SaveFileDialog extends FileDialog
{

   public SaveFileDialog(String title,
                         FileSystemContext context,
                         FilenameTransform transform,
                         ProgressOperationWithInput<FileSystemItem> operation)
   {
      super(title, null, "Save", true, true, context, operation);
      transform_ = transform;
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
      if (transform_ == null)
         return filename;
      else
         return transform_.transform(filename);
   }

   private final FilenameTransform transform_;
}
