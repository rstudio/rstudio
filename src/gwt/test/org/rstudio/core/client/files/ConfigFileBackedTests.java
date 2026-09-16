/*
 * ConfigFileBackedTests.java
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
package org.rstudio.core.client.files;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.junit.client.GWTTestCase;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
import org.rstudio.studio.client.server.ServerRequestCallback;
import com.google.gwt.json.client.JSONValue;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.files.model.FileUploadToken;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

// Exercises the queue that replaced ConfigFileBacked's old polling Timer.
// The old execute() polled loading_/loaded_ on a 20ms Timer and silently
// dropped the queued command if the read_config_json response had not
// arrived within ~100 polls -- see ApplicationCommandManager.loadBindings(),
// which relied on this and lost queued commands under a slow session start.
// These tests fire the stubbed server's callback directly rather than
// waiting on a timer, so they are deterministic and would fail immediately
// (not merely time out) against the old implementation once the response is
// delayed past the callback.
public class ConfigFileBackedTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // A FilesServerOperations stub that never answers readConfigJSON on its
   // own; the test fires the captured callback when it wants the read to
   // "settle". All other methods are unused no-ops.
   private static class StubFilesServerOperations implements FilesServerOperations
   {
      public ServerRequestCallback<JavaScriptObject> lastReadCallback;
      public int readConfigJSONCallCount;

      @Override
      public void readConfigJSON(String relativePath,
                                  boolean logErrorIfNotFound,
                                  ServerRequestCallback<JavaScriptObject> requestCallback)
      {
         readConfigJSONCallCount++;
         lastReadCallback = requestCallback;
      }

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
                             ServerRequestCallback<DirectoryListing> requestCallback) {}

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

   private static native JavaScriptObject createObject() /*-{
      return {};
   }-*/;

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

   // Records how many times, and with what value, a queued command ran.
   private static class RecordingCommand implements CommandWithArg<JavaScriptObject>
   {
      int executeCount;
      JavaScriptObject lastValue;

      @Override
      public void execute(JavaScriptObject value)
      {
         executeCount++;
         lastValue = value;
      }
   }

   public void testCommandQueuedBeforeSettleDoesNotRunUntilResponseArrives()
   {
      StubFilesServerOperations server = new StubFilesServerOperations();
      ConfigFileBacked<JavaScriptObject> config = new ConfigFileBacked<>(
            server, "test.json", false, createObject());

      RecordingCommand command = new RecordingCommand();
      config.execute(command);

      assertEquals(0, command.executeCount);
      assertNotNull(server.lastReadCallback);

      JavaScriptObject response = createObject();
      server.lastReadCallback.onResponseReceived(response);

      assertEquals(1, command.executeCount);
      assertSame(response, command.lastValue);
   }

   public void testCommandStillRunsOnErrorWithDefaultValue()
   {
      StubFilesServerOperations server = new StubFilesServerOperations();
      JavaScriptObject defaultValue = createObject();
      ConfigFileBacked<JavaScriptObject> config = new ConfigFileBacked<>(
            server, "test.json", false, defaultValue);

      RecordingCommand command = new RecordingCommand();
      config.execute(command);

      assertEquals(0, command.executeCount);

      server.lastReadCallback.onError(new StubServerError());

      assertEquals(1, command.executeCount);
      assertSame(defaultValue, command.lastValue);
   }

   public void testExecuteRunsSynchronouslyOnceLoaded()
   {
      StubFilesServerOperations server = new StubFilesServerOperations();
      ConfigFileBacked<JavaScriptObject> config = new ConfigFileBacked<>(
            server, "test.json", false, createObject());

      RecordingCommand first = new RecordingCommand();
      config.execute(first);
      JavaScriptObject response = createObject();
      server.lastReadCallback.onResponseReceived(response);

      RecordingCommand second = new RecordingCommand();
      config.execute(second);

      assertEquals(1, second.executeCount);
      assertSame(response, second.lastValue);
   }

   public void testMultipleCommandsQueuedBeforeSettleAllRunInOrder()
   {
      StubFilesServerOperations server = new StubFilesServerOperations();
      ConfigFileBacked<JavaScriptObject> config = new ConfigFileBacked<>(
            server, "test.json", false, createObject());

      final ArrayList<Integer> executionOrder = new ArrayList<>();
      config.execute(value -> executionOrder.add(1));
      config.execute(value -> executionOrder.add(2));

      server.lastReadCallback.onResponseReceived(createObject());

      assertEquals(2, executionOrder.size());
      assertEquals(Integer.valueOf(1), executionOrder.get(0));
      assertEquals(Integer.valueOf(2), executionOrder.get(1));
   }

   public void testConcurrentExecuteCallsIssueOnlyOneRead()
   {
      StubFilesServerOperations server = new StubFilesServerOperations();
      ConfigFileBacked<JavaScriptObject> config = new ConfigFileBacked<>(
            server, "test.json", false, createObject());

      config.execute(new RecordingCommand());
      config.execute(new RecordingCommand());

      assertEquals(1, server.readConfigJSONCallCount);
   }
}
