/*
 * FileExport.java
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

package org.rstudio.studio.client.common.fileexport;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationCsrfToken;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.InputElement;
import com.google.inject.Inject;

public class FileExport
{
   public FileExport()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay,
                   FilesServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
   }
   
   public void export(String caption, String description, FileSystemItem file)
   {
      ArrayList<FileSystemItem> files = new ArrayList<>();
      files.add(file);
      export(caption, description, null, files);
   }
   
   public void export(String caption,
                      String description,
                      final FileSystemItem parentDir,
                      final ArrayList<FileSystemItem> files)
   {
      // validation: some files provided
      if  (files.size() == 0)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_INFO, constants_.noFilesSelectedCaption(),
               constants_.noFilesSelectedMessage());
         return;
      }
         
      // case: single file which is not a folder 
      if ((files.size()) == 1 && !files.get(0).isDirectory())
      {
         final FileSystemItem file = files.get(0);
         
         showFileExport(caption,
                        description,
                        file.getStem(),
                        file.getExtension(),     
                        new ProgressOperationWithInput<String>(){
            public void execute(String name, ProgressIndicator progress)
            {
               // progress complete
               progress.onCompleted();

               // Create a form for POST submission
               exportFile(name, file);
            }
         });
      }
      
      // case: folder or multiple files
      else
      {
         // determine the default zip file name based on the selection
         String defaultArchiveName;
         if (files.size() == 1)
            defaultArchiveName = files.get(0).getStem();
         else
            defaultArchiveName = "rstudio-export";
         
         // prompt user
         final String ZIP = ".zip";
         showFileExport(caption,
                        description,
                        defaultArchiveName,
                        ZIP,
                        new ProgressOperationWithInput<String>(){
            
            public void execute(String archiveName, ProgressIndicator progress)
            {
               // progress complete
               progress.onCompleted();

               // force zip extension in case the user deleted it
               if (!archiveName.endsWith(ZIP))
                  archiveName += ZIP;

               // build list of filenames
               ArrayList<String> filenames = new ArrayList<>();
               for (FileSystemItem file : files)
                  filenames.add(file.getName());

               // execute the download via POST form
               exportFiles(archiveName, parentDir, filenames);
            }
         });
         
      }
   }
   
   private void showFileExport(String caption,
                               String description,
                               String defaultName,
                               String defaultExtension,
                               ProgressOperationWithInput<String> operation)
   {
      globalDisplay_.promptForText(
            caption,
            constants_.showFileExportLabel(description),
            defaultName + defaultExtension,
            -1, -1,
            constants_.downloadButtonCaption(),
             operation);
   }
   
   public void exportFile(String name, FileSystemItem file)
   {
      // Create a form for POST submission
      FormElement form = Document.get().createFormElement();
      form.setMethod("post");
      form.setAction(server_.getFileExportUrl(name, file));
      form.setTarget("_blank");

      // Add parameters
      addHiddenField(form, "name", name);
      addHiddenField(form, "file", file.getPath());
      addHiddenField(form, "rs-csrf-token", ApplicationCsrfToken.getCsrfToken());

      // Submit form
      Document.get().getBody().appendChild(form);
      form.submit();
      form.removeFromParent();
   }

   public void exportFiles(String archiveName,
                           FileSystemItem parentDirectory,
                           ArrayList<String> filenames)
   {
      // Create a form for POST submission
      FormElement form = Document.get().createFormElement();
      form.setMethod("post");
      form.setAction(server_.getFileExportUrl(archiveName, parentDirectory, filenames));
      form.setTarget("_blank");

      // Add parameters
      addHiddenField(form, "name", archiveName);
      addHiddenField(form, "parent", parentDirectory.getPath());
      addHiddenField(form, "rs-csrf-token", ApplicationCsrfToken.getCsrfToken());

      for (int i = 0; i < filenames.size(); i++)
      {
         addHiddenField(form, "file" + i, filenames.get(i));
      }

      // Submit form
      Document.get().getBody().appendChild(form);
      form.submit();
      form.removeFromParent();
   }

   private void addHiddenField(FormElement form, String name, String value)
   {
      InputElement field = Document.get().createHiddenInputElement();
      field.setName(name);
      field.setValue(value);
      form.appendChild(field);
   }

   private GlobalDisplay globalDisplay_;
   private FilesServerOperations server_;
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
}
