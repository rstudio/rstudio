/*
 * FileDialog.java
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

import com.google.gwt.event.logical.shared.SelectionEvent;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

public abstract class FileDialog extends FileSystemDialog
{
   protected FileDialog(String title,
                        String caption,
                        String buttonName,
                        boolean promptOnOverwrite,
                        boolean allowNonexistentFile,
                        boolean allowFolderCreation,
                        FileSystemContext context,
                        String filter,
                        ProgressOperationWithInput<FileSystemItem> operation)
   {
      super(title, caption, buttonName, context, filter, allowFolderCreation,
            operation);

      promptOnOverwrite_ = promptOnOverwrite;
      allowNonexistentFile_ = allowNonexistentFile;
   }

   /**
    * This should ONLY be called from the accept() method, otherwise the
    * filename may contain invalid data which could throw an exception.
    * @return
    */
   @Override
   protected FileSystemItem getSelectedItem()
   {
      return context_.itemForName(browser_.getFilename(), false, false);
   }

   @Override
   public String getFilenameLabel()
   {
      return "File name";
   }

   /**
    * Validate the current state of the dialog. Subclasses can override
    * this but super.shouldAccept() MUST be the last validation that occurs.
    * @return true if the dialog is in a valid state for acceptance
    */
   protected boolean shouldAccept()
   {
      browser_.setFilename(browser_.getFilename().trim());

      String filename = browser_.getFilename();

      if (filename.length() == 0)
         return false;

      int lastIndex = filename.lastIndexOf('/');
      if (lastIndex >= 0)
      {
         String dir = filename.substring(0, lastIndex);
         if (dir.length() == 0)
            dir = "/";
         String file = filename.substring(lastIndex + 1);

         // Targeted fix for "611: Permission denied error when attempting to
         // browse /shared folder in open file dialog". The /shared folder
         // doesn't have list permissions.
         if (dir.equals("/shared"))
         {
            cd(filename);
            return false;
         }

         browser_.setFilename(file);
         browser_.setFilenameEnabled(false);
         attemptAcceptOnNextNavigate_ = true;
         cd(dir);
         return false;
      }

      String filenameValidationError = context_.validatePathElement(filename, true);
      if (filenameValidationError != null)
      {
         browser_.selectFilename();
         showError(filenameValidationError);
         return false;
      }

      if (navigateIfDirectory())
         return false;

      boolean useExactFilename = browser_.getSelectedValue() != null
            && browser_.getSelectedValue().equals(filename);
      
      if (!useExactFilename || getAlwaysMungeFilename())
      {
         browser_.setFilename(mungeFilename(filename));
      }

      if (navigateIfDirectory())
         return false;

      FileSystemItem item = context_.itemForName(filename, true, false);
      if (item == null)
      {
         if (!allowNonexistentFile_)
         {
            showError("File does not exist");
            return false;
         }
      }
      else
      {
         if (item.isDirectory())
         {
            assert false : "This case should be covered by navigateIfDirectory";
            return false;
         }
         else if (promptOnOverwrite_)
         {
            /* WARNING. showOverwritePrompt() MAY CAUSE accept() TO BE CALLED
                DIRECTLY. ALL OTHER VALIDATION *MUST* BE COMPLETE BEFORE
                CALLING showOverwritePrompt()!!! */
            showOverwritePrompt();
            return false;
         }
      }

      return true;
   }

   private boolean navigateIfDirectory()
   {
      FileSystemItem item = context_.itemForName(browser_.getFilename(),
                                                 true,
                                                 false);
      if (item != null && item.isDirectory())
      {
         browser_.setFilename("");
         cd(item.getName());
         return true;
      }
      return false;
   }

   /**
    * Gives subclasses an opportunity to change the filename before acceptance.
    * This happens AFTER validation so it's imperative that no potentially
    * illegal values be returned from this method.
    */
   protected String mungeFilename(String filename)
   {
      return filename;
   }
   
   protected boolean getAlwaysMungeFilename()
   {
      return false;
   }

   protected void cd(String path)
   {
      browser_.setFilenameEnabled(false);
      browser_.cd(path);
   }

   @Override
   public void onNavigated()
   {
      super.onNavigated();

      browser_.setFilenameEnabled(true);
      if (attemptAcceptOnNextNavigate_)
      {
         attemptAcceptOnNextNavigate_ = false;
         maybeAccept();
      }
   }

   @Override
   protected void onDialogShown()
   {
      browser_.setFilenameFocus(true);
      browser_.selectFilename();
   }

   @Override
   public void onSelection(SelectionEvent<FileSystemItem> event)
   {
      super.onSelection(event);
      
      FileSystemItem item = event.getSelectedItem();
      if (item != null && !item.isDirectory())
         browser_.setFilename(item.getName());
   }

   @Override
   public void onError(String errorMessage)
   {
      attemptAcceptOnNextNavigate_ = false;
      super.onError(errorMessage);
   }

   protected boolean promptOnOverwrite_;
   protected boolean allowNonexistentFile_;
   private boolean attemptAcceptOnNextNavigate_ = false;
}

