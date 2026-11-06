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

import com.google.gwt.junit.client.GWTTestCase;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.workbench.views.files.model.StubFilesServerOperations;

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
      Session session = new Session(null);
      session.setSessionInfo(sessionInfo());

      server_ = new StubFilesServerOperations();
      context_ = new RemoteFileSystemContext(server_, null, null, session);
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
      assertEquals(2, server_.requestCount());
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
      assertEquals(2, server_.requestCount());
      assertDirectory("/second");
      assertEquals(1, navigated_);
      assertEquals(0, errors_);
   }

   public void testPendingFallbackIsSupersededByNavigation()
   {
      context_.cd("/first", "/fallback");
      server_.fail(0);
      assertEquals("/fallback", server_.path(1));
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
      assertEquals("/fallback", server_.path(1));
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
      assertEquals(1, server_.requestCount());
      assertEquals("/second", context_.pwd());
      assertEquals(1, navigated_);
      assertEquals(0, errors_);
   }

   // a context shared across dialogs (e.g. Projects') is refreshed by the
   // next dialog to open while an earlier dialog's listing may be pending
   public void testRefreshSupersedesPendingNavigation()
   {
      context_.cd("/first");
      context_.refresh();
      assertEquals("~", server_.path(1));

      server_.succeed(0);
      assertEquals(0, navigated_);

      server_.succeed(1);
      assertDirectory("~");
      assertEquals(1, navigated_);
   }

   public void testCdFallsBackToDefaultWorkingDir()
   {
      context_.cd("/missing");
      server_.fail(0);
      assertEquals("/default", server_.path(1));
      assertEquals(0, errors_);

      server_.succeed(1);
      assertDirectory("/default");
   }

   private void assertDirectory(String path)
   {
      assertEquals(path, context_.pwd());
      assertEquals(1, context_.ls().length);
      assertEquals(path + "/marker.txt", context_.ls()[0].getPath());
   }

   private static native SessionInfo sessionInfo() /*-{
      return {default_working_dir: "/default"};
   }-*/;

   private StubFilesServerOperations server_;
   private RemoteFileSystemContext context_;
   private int navigated_;
   private int errors_;
}
