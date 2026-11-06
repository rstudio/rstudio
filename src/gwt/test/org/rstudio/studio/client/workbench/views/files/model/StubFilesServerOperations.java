/*
 * StubFilesServerOperations.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.files.model;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONValue;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;

/**
 * Records each listFiles() request so a test can answer them in any order.
 * A successful listing holds one file, `<directory>/marker.txt`. A
 * createFolder() request succeeds at once.
 */
public class StubFilesServerOperations implements FilesServerOperations
{
   public void succeed(int request)
   {
      requests_.get(request).onResponseReceived(listing(paths_.get(request) + "/marker.txt"));
   }

   public void fail(int request)
   {
      requests_.get(request).onError(new StubServerError());
   }

   public int requestCount()
   {
      return requests_.size();
   }

   public String path(int request)
   {
      return paths_.get(request);
   }

   // the file dialogs' list draws a file's size and date
   private static native DirectoryListing listing(String path) /*-{
      return {files: [{path: path, dir: false, length: 0, lastModified: 0}]};
   }-*/;

   @Override
   public void readConfigJSON(String path, boolean logError,
                              ServerRequestCallback<JavaScriptObject> callback) {}

   @Override
   public void stat(String path, ServerRequestCallback<FileSystemItem> requestCallback) {}

   @Override
   public void isTextFile(String path, ServerRequestCallback<Boolean> requestCallback) {}

   @Override
   public void isGitDirectory(String path, ServerRequestCallback<Boolean> requestCallback) {}

   @Override
   public void isPackageDirectory(String path, ServerRequestCallback<Boolean> requestCallback) {}

   @Override
   public void getFileContents(String path, String encoding, ServerRequestCallback<String> requestCallback) {}

   @Override
   public void listFiles(FileSystemItem directory, boolean monitor, boolean showHidden,
                         ServerRequestCallback<DirectoryListing> requestCallback)
   {
      paths_.add(directory.getPath());
      requests_.add(requestCallback);
   }

   @Override
   public void listAllFiles(String path, String pattern, ServerRequestCallback<JsArrayString> requestCallback) {}

   @Override
   public void createFile(FileSystemItem file, String contents, ServerRequestCallback<VoidResponse> requestCallback) {}

   @Override
   public void createFolder(FileSystemItem folder, ServerRequestCallback<VoidResponse> requestCallback)
   {
      requestCallback.onResponseReceived(null);
   }

   @Override
   public void deleteFiles(ArrayList<FileSystemItem> files, ServerRequestCallback<VoidResponse> requestCallback) {}

   @Override
   public void copyFile(FileSystemItem sourceFile, FileSystemItem targetFile, boolean overwrite,
                        ServerRequestCallback<VoidResponse> requestCallback) {}

   @Override
   public void moveFiles(ArrayList<FileSystemItem> files, FileSystemItem targetDirectory,
                         ServerRequestCallback<VoidResponse> requestCallback) {}

   @Override
   public void renameFile(FileSystemItem file, FileSystemItem targetFile,
                          ServerRequestCallback<VoidResponse> serverRequestCallback) {}

   @Override
   public void touchFile(FileSystemItem newFile, ServerRequestCallback<VoidResponse> requestCallback) {}

   @Override
   public String getFileUrl(FileSystemItem file) { return null; }

   @Override
   public String getFileUploadUrl() { return null; }

   @Override
   public void completeUpload(FileUploadToken token, boolean commit,
                              ServerRequestCallback<VoidResponse> requestCallback) {}

   @Override
   public String getFileExportUrl(String name, FileSystemItem file) { return null; }

   @Override
   public String getFileExportUrl(String name, FileSystemItem parentDirectory, ArrayList<String> filenames) { return null; }

   @Override
   public void writeConfigJSON(String relativePath, JavaScriptObject object,
                               ServerRequestCallback<Boolean> requestCallback) {}

   @Override
   public void getIssueUrl(String id, ServerRequestCallback<String> requestCallback) {}

   @Override
   public void makeProjectRelative(JsArrayString paths, ServerRequestCallback<JsArrayString> requestCallback) {}

   @Override
   public String resolveAliasedPath(FileSystemItem file) { return null; }

   private static class StubServerError implements ServerError
   {
      @Override
      public int getCode() { return ServerError.EXECUTION; }

      @Override
      public String getMessage() { return "stub error"; }

      @Override
      public String getRedirectUrl() { return null; }

      @Override
      public ServerErrorCause getCause() { return null; }

      @Override
      public String getUserMessage() { return "stub error"; }

      @Override
      public JSONValue getClientInfo() { return null; }
   }

   private final ArrayList<String> paths_ = new ArrayList<>();
   private final ArrayList<ServerRequestCallback<DirectoryListing>> requests_ = new ArrayList<>();
}
