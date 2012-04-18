/*
 * FilesServerOperations.java
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
package org.rstudio.studio.client.workbench.views.files.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import java.util.ArrayList;

public interface FilesServerOperations
{
   void stat(String path,
             ServerRequestCallback<FileSystemItem> requestCallback);

   // get a file listing
   void listFiles(FileSystemItem directory,
                  boolean monitor,
                  ServerRequestCallback<JsArray<FileSystemItem>> requestCallback);

   void listAllFiles(String path,
                     String pattern,
                     ServerRequestCallback<JsArrayString> requestCallback);

   // create a folder
   void createFolder(FileSystemItem folder,
                     ServerRequestCallback<Void> requestCallback);

   // delete files
   void deleteFiles(ArrayList<FileSystemItem> files,
                    ServerRequestCallback<Void> requestCallback);

   // copy file
   void copyFile(FileSystemItem sourceFile,
                 FileSystemItem targetFile,
                 boolean overwrite,
                 ServerRequestCallback<Void> requestCallback);

   // move files
   void moveFiles(ArrayList<FileSystemItem> files,
                  FileSystemItem targetDirectory,
                  ServerRequestCallback<Void> requestCallback);

   // rename file
   void renameFile(FileSystemItem file,
                   FileSystemItem targetFile,
                   ServerRequestCallback<Void> serverRequestCallback);


   String getFileUrl(FileSystemItem file);

   String getFileUploadUrl();

   void completeUpload(FileUploadToken token,
                       boolean commit,
                       ServerRequestCallback<Void> requestCallback);

   String getFileExportUrl(String name,
                           FileSystemItem file);

   String getFileExportUrl(String name,
                           FileSystemItem parentDirectory,
                           ArrayList<String> filenames);
}
