/*
 * FileDialogNavigationTests.java
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
package org.rstudio.core.client.files.filedialog;

import java.util.ArrayList;

import com.google.gwt.junit.client.GWTTestCase;

import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.DummyGlobalDisplay;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.files.model.StubFilesServerOperations;

/**
 * Drives the web file dialogs through a RemoteFileSystemContext whose
 * listings the test answers in a chosen order. Request 0 is always the
 * dialog's initial listing of /home.
 */
public class FileDialogNavigationTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      // the breadcrumb reads the active project from the injected session
      Session appSession = RStudioGinjector.INSTANCE.getSession();
      appSessionInfo_ = appSession.getSessionInfo();
      appSession.setSessionInfo(sessionInfo());

      Session session = new Session(null);
      session.setSessionInfo(sessionInfo());

      server_ = new StubFilesServerOperations();
      context_ = new RemoteFileSystemContext(server_, null, new DummyGlobalDisplay(), session);
      accepted_ = new ArrayList<>();
   }

   @Override
   protected void gwtTearDown()
   {
      // the injected session outlives this suite
      RStudioGinjector.INSTANCE.getSession().setSessionInfo(appSessionInfo_);
   }

   // #18960: crumb A, crumb B, then Choose before either listing arrives
   public void testChooseAfterTwoNavigationsUsesLatestFolder()
   {
      ChooseFolderDialog2 dialog = chooseFolderDialog();
      commitDirectory(dialog, "/a");
      commitDirectory(dialog, "/b");
      dialog.maybeAccept();

      server_.succeed(1);
      assertEquals(0, accepted_.size());

      server_.succeed(2);
      assertAccepted("/b");
   }

   public void testSaveAfterTwoNavigationsUsesLatestFolder()
   {
      SaveFileDialog dialog = saveFileDialog();
      dialog.setFilename("file.R");
      commitDirectory(dialog, "/a");
      commitDirectory(dialog, "/b");
      dialog.maybeAccept();

      server_.succeed(1);
      assertEquals(0, accepted_.size());

      server_.succeed(2);
      assertAccepted("/b/file.R");
   }

   // a typed 'sub/file.R' defers the save behind cd('sub'); choosing a crumb
   // meanwhile must not save 'file.R' in the crumb's folder
   public void testNavigatingAfterDeferredSaveAbandonsIt()
   {
      SaveFileDialog dialog = saveFileDialog();
      dialog.setFilename("sub/file.R");
      dialog.maybeAccept();
      assertEquals("/home/sub", server_.path(1));

      commitDirectory(dialog, "/a");
      server_.succeed(2);
      server_.succeed(1);
      assertEquals(0, accepted_.size());
      assertEquals("/a", context_.pwd());
   }

   public void testNavigatingAfterDeferredChooseAbandonsIt()
   {
      ChooseFolderDialog2 dialog = chooseFolderDialog();
      commitDirectory(dialog, "/a");
      dialog.maybeAccept();
      commitDirectory(dialog, "/b");

      server_.succeed(1);
      server_.succeed(2);
      assertEquals(0, accepted_.size());
      assertEquals("/b", context_.pwd());
   }

   private ChooseFolderDialog2 chooseFolderDialog()
   {
      ChooseFolderDialog2 dialog = new ChooseFolderDialog2("Choose Folder", "Choose", context_, false, recordAccepted());
      open(dialog);
      return dialog;
   }

   private SaveFileDialog saveFileDialog()
   {
      SaveFileDialog dialog = new SaveFileDialog("Save File", "Save", context_, "", false, recordAccepted());
      open(dialog);
      return dialog;
   }

   // as WebFileDialogs does, minus showing the dialog
   private void open(FileSystemDialog dialog)
   {
      dialog.createMainWidget();
      context_.cd("/home");
      server_.succeed(0);
   }

   // what a breadcrumb crumb or a folder row in the list fires
   private static void commitDirectory(FileSystemDialog dialog, String path)
   {
      dialog.onSelectionCommit(new SelectionCommitEvent<FileSystemItem>(FileSystemItem.createDir(path)) {});
   }

   private ProgressOperationWithInput<FileSystemItem> recordAccepted()
   {
      return new ProgressOperationWithInput<FileSystemItem>()
      {
         @Override
         public void execute(FileSystemItem input, ProgressIndicator indicator)
         {
            accepted_.add(input);
         }
      };
   }

   private void assertAccepted(String path)
   {
      assertEquals(1, accepted_.size());
      assertEquals(path, accepted_.get(0).getPath());
   }

   private static native SessionInfo sessionInfo() /*-{
      return {default_working_dir: "/default"};
   }-*/;

   private SessionInfo appSessionInfo_;
   private StubFilesServerOperations server_;
   private RemoteFileSystemContext context_;
   private ArrayList<FileSystemItem> accepted_;
}
