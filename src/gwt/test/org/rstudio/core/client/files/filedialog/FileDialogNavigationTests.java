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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
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
      context_ = new RemoteFileSystemContext(server_, null, new NewFolderDisplay(), session);
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

   // New Folder enters the folder it creates, which supersedes a deferred
   // save just as choosing a crumb does
   public void testNewFolderAfterDeferredSaveAbandonsIt()
   {
      SaveFileDialog dialog = saveFileDialog();
      dialog.setFilename("file.R");
      commitDirectory(dialog, "/a");
      dialog.maybeAccept();

      // created in /home, since the listing of /a has not arrived
      clickNewFolder(dialog);
      assertEquals("/home/new", server_.path(2));

      server_.succeed(2);
      server_.succeed(1);
      assertEquals(0, accepted_.size());
      assertEquals("/home/new", context_.pwd());
   }

   // a typed 'sub/file.R' whose cd('sub') fails is given back whole for
   // correction, rather than leaving 'file.R' staged where the dialog still is
   public void testFailedTypedFolderRestoresTypedPath()
   {
      SaveFileDialog dialog = saveFileDialog();
      dialog.setFilename("sub/file.R");
      dialog.maybeAccept();
      assertEquals("file.R", dialog.browser_.getFilename());

      server_.fail(1);
      assertEquals("sub/file.R", dialog.browser_.getFilename());
      assertEquals(0, accepted_.size());
   }

   // ... but not once the user has navigated elsewhere meanwhile: the failure
   // is then theirs, and the typed path must not come back over their input
   public void testNavigatingAfterDeferredSaveDropsTypedPath()
   {
      SaveFileDialog dialog = saveFileDialog();
      dialog.setFilename("sub/file.R");
      dialog.maybeAccept();
      commitDirectory(dialog, "/a");

      server_.fail(2);
      assertEquals("file.R", dialog.browser_.getFilename());
      assertEquals(0, accepted_.size());
   }

   // a context shared across dialogs (e.g. Projects') is taken by the next
   // dialog to open; the earlier dialog closing must not detach it
   public void testClosingDialogLeavesNewerDialogAttached()
   {
      SaveFileDialog first = saveFileDialog();
      ChooseFolderDialog2 second = chooseFolderDialog();
      first.onUnload();

      commitDirectory(second, "/a");
      server_.succeed(2);
      assertEquals("/a", context_.pwd());

      second.maybeAccept();
      assertAccepted("/a");
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

   private static void clickNewFolder(FileSystemDialog dialog)
   {
      ThemedButton button = findButton(dialog, ElementIds.getElementId(ElementIds.FILE_NEW_FOLDER_BUTTON));
      assertNotNull(button);

      NativeEvent click = Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
      DomEvent.fireNativeEvent(click, button);
   }

   private static ThemedButton findButton(Widget widget, String elementId)
   {
      if (widget instanceof ThemedButton && elementId.equals(widget.getElement().getId()))
         return (ThemedButton) widget;

      if (widget instanceof HasWidgets)
      {
         for (Widget child : (HasWidgets) widget)
         {
            ThemedButton button = findButton(child, elementId);
            if (button != null)
               return button;
         }
      }

      return null;
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

   // answers the New Folder prompt with 'new'
   private static class NewFolderDisplay extends DummyGlobalDisplay
   {
      @Override
      public void promptForText(String title,
                                String label,
                                String initialValue,
                                ProgressOperationWithInput<String> operation)
      {
         operation.execute("new", new NullProgressIndicator());
      }
   }

   private SessionInfo appSessionInfo_;
   private StubFilesServerOperations server_;
   private RemoteFileSystemContext context_;
   private ArrayList<FileSystemItem> accepted_;
}
