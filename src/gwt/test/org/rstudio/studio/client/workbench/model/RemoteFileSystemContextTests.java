/*
 * RemoteFileSystemContextTests.java
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
package org.rstudio.studio.client.workbench.model;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.junit.client.GWTTestCase;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.files.model.FileUploadToken;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

public class RemoteFileSystemContextTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      server_ = new StubFilesServerOperations();
      context_ = new RemoteFileSystemContext(server_, null, null, null);
      navigated_ = 0;
      errors_ = 0;
      context_.setCallbacks(new FileSystemContext.Callbacks()
      {
         public void onNavigated() { navigated_++; }
         public void onError(String message) { errors_++; }
         public void onDirectoryCreated(FileSystemItem directory) {}
      });
   }

   public void testOlderListingCannotFinishPendingNavigation()
   {
      context_.cd("/first", null);
      context_.cd("/second", null);
      server_.succeed(0);
      assertEquals("~", context_.pwd());
      assertNull(context_.ls());
      assertEquals(0, navigated_);

      server_.succeed(1);
      assertDirectory("/second");
      assertEquals(1, navigated_);
   }

   public void testOlderListingCannotReplaceLatestDirectory()
   {
      context_.cd("/first", null);
      context_.cd("/second", null);
      server_.succeed(1);
      server_.succeed(0);
      assertDirectory("/second");
      assertEquals(1, navigated_);
   }

   public void testOlderErrorCannotFinishPendingNavigation()
   {
      context_.cd("/first", null);
      context_.cd("/second", null);
      server_.fail(0);
      assertEquals(0, errors_);
      assertEquals(0, navigated_);
      server_.succeed(1);
      assertDirectory("/second");
   }

   public void testOlderErrorCannotStartFallback()
   {
      context_.cd("/first", "/fallback");
      context_.cd("/second", null);
      server_.fail(0);
      assertEquals(2, server_.requests_.size());
      server_.succeed(1);
      assertDirectory("/second");
      assertEquals(0, errors_);
   }

   public void testOlderErrorAfterLatestListingCannotStartFallback()
   {
      context_.cd("/first", "/fallback");
      context_.cd("/second", null);
      server_.succeed(1);
      server_.fail(0);
      assertEquals(2, server_.requests_.size());
      assertDirectory("/second");
      assertEquals(1, navigated_);
      assertEquals(0, errors_);
   }

   public void testPendingFallbackIsSupersededByNavigation()
   {
      context_.cd("/first", "/fallback");
      server_.fail(0);
      assertEquals("/fallback", server_.paths_.get(1));
      context_.cd("/second", null);
      server_.succeed(1);
      assertEquals(0, navigated_);
      server_.succeed(2);
      assertDirectory("/second");
      assertEquals(1, navigated_);
   }

   public void testLatestNavigationStillUsesFallback()
   {
      context_.cd("/missing", "/fallback");
      server_.fail(0);
      assertEquals("/fallback", server_.paths_.get(1));
      assertEquals(0, navigated_);
      assertEquals(0, errors_);
      server_.succeed(1);
      assertDirectory("/fallback");
      assertEquals(1, navigated_);
   }

   public void testLatestFailureIsReportedWithoutAcceptingOlderListing()
   {
      context_.cd("/first", null);
      context_.cd("/missing", "/fallback");
      server_.fail(1);
      server_.fail(2);
      server_.succeed(0);
      assertEquals(1, errors_);
      assertEquals(0, navigated_);
      assertEquals("~", context_.pwd());
      assertNull(context_.ls());
   }

   public void testSetwdSupersedesPendingListing()
   {
      context_.cd("/first", null);
      context_.setwd("/second");
      server_.succeed(0);
      assertEquals("/second", context_.pwd());
      assertNull(context_.ls());
      assertEquals(1, navigated_);
   }

   public void testSetwdSupersedesPendingFallbackError()
   {
      context_.cd("/first", "/fallback");
      context_.setwd("/second");
      server_.fail(0);
      assertEquals(1, server_.requests_.size());
      assertEquals("/second", context_.pwd());
      assertEquals(1, navigated_);
      assertEquals(0, errors_);
   }

   private void assertDirectory(String path)
   {
      assertEquals(path, context_.pwd());
      assertEquals(1, context_.ls().length);
      assertEquals(path + "/marker.txt", context_.ls()[0].getPath());
   }

   private static native DirectoryListing listing(String path) /*-{
      return {files: [{path: path, dir: false}]};
   }-*/;

   private static class StubFilesServerOperations implements FilesServerOperations
   {
      private final ArrayList<String> paths_ = new ArrayList<>();
      private final ArrayList<ServerRequestCallback<DirectoryListing>> requests_ = new ArrayList<>();

      public void succeed(int request)
      {
         requests_.get(request).onResponseReceived(listing(paths_.get(request) + "/marker.txt"));
      }

      public void fail(int request)
      {
         requests_.get(request).onError(new StubServerError());
      }

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
      public void createFolder(FileSystemItem folder, ServerRequestCallback<VoidResponse> requestCallback) {}

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
   }

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

   private StubFilesServerOperations server_;
   private RemoteFileSystemContext context_;
   private int navigated_;
   private int errors_;
}
