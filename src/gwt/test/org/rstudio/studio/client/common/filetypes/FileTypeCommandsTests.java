/*
 * FileTypeCommandsTests.java
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
package org.rstudio.studio.client.common.filetypes;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.junit.client.GWTTestCase;

public class FileTypeCommandsTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      events_ = new EventBus(null, null);
      Session session = new Session(events_);
      session.setSessionInfo(createSessionInfo());
      server_ = new CapabilityServer();
      commands_ = new FileTypeCommands(session, events_, server_);
   }

   public void testPlaceholderDoesNotCompleteRestoredEditorChecks()
   {
      assertFalse(commands_.getHTMLCapabiliites().isRMarkdownSupported());
      List<HTMLCapabilities> received = new ArrayList<>();
      commands_.withHTMLCapabilities(received::add);
      commands_.withHTMLCapabilities(received::add);
      events_.fireEvent(new SessionInitEvent());

      assertEquals(1, server_.requests_);
      assertEquals(0, received.size());
      server_.callback_.onResponseReceived(createCapabilities(true));
      assertEquals(2, received.size());
      assertTrue(received.get(0).isRMarkdownSupported());
      assertTrue(commands_.getHTMLCapabiliites().isRMarkdownSupported());
   }

   public void testMissingPackagesAreReportedAfterProbe()
   {
      List<HTMLCapabilities> received = new ArrayList<>();
      events_.fireEvent(new SessionInitEvent());
      commands_.withHTMLCapabilities(received::add);
      assertEquals(0, received.size());

      server_.callback_.onResponseReceived(createCapabilities(false));
      assertEquals(1, received.size());
      assertFalse(received.get(0).isRMarkdownSupported());
   }

   public void testDismissedEditorDoesNotReceiveProbeResult()
   {
      List<HTMLCapabilities> received = new ArrayList<>();
      HandlerRegistration registration = commands_.withHTMLCapabilities(received::add);
      registration.removeHandler();
      server_.callback_.onResponseReceived(createCapabilities(true));
      assertEquals(0, received.size());
   }

   public void testEditorsOpenedAfterProbeReceiveCachedResult()
   {
      events_.fireEvent(new SessionInitEvent());
      server_.callback_.onResponseReceived(createCapabilities(true));
      List<HTMLCapabilities> received = new ArrayList<>();
      commands_.withHTMLCapabilities(received::add);
      assertEquals(1, received.size());
      assertTrue(received.get(0).isRMarkdownSupported());
      assertEquals(1, server_.requests_);
   }

   public void testPackageChangeDuringProbeWaitsForFreshResult()
   {
      List<HTMLCapabilities> received = new ArrayList<>();
      commands_.withHTMLCapabilities(received::add);
      events_.fireEvent(new PackageStateChangedEvent(null));
      server_.callback_.onResponseReceived(createCapabilities(false));
      assertEquals(0, received.size());
      assertEquals(2, server_.requests_);

      server_.callback_.onResponseReceived(createCapabilities(true));
      assertEquals(1, received.size());
      assertTrue(received.get(0).isRMarkdownSupported());
   }

   private static native SessionInfo createSessionInfo() /*-{
      return { html_capabilities: {} };
   }-*/;

   private static native HTMLCapabilities createCapabilities(boolean supported) /*-{
      return { r_markdown_supported: supported, stitch_supported: supported };
   }-*/;

   private static class CapabilityServer implements HTMLPreviewServerOperations
   {
      @Override
      public void getHTMLCapabilities(ServerRequestCallback<HTMLCapabilities> callback)
      {
         requests_++;
         callback_ = callback;
      }

      @Override
      public void previewHTML(HTMLPreviewParams params, ServerRequestCallback<Boolean> callback) {}

      @Override
      public void terminatePreviewHTML(ServerRequestCallback<VoidResponse> callback) {}

      @Override
      public void copyFile(
            FileSystemItem source,
            FileSystemItem target,
            boolean overwrite,
            ServerRequestCallback<VoidResponse> callback) {}

      @Override
      public String getApplicationURL(String path) { return null; }

      @Override
      public String getFileUrl(FileSystemItem file) { return null; }

      @Override
      public void rpubsIsPublished(String htmlFile, ServerRequestCallback<Boolean> callback) {}

      @Override
      public void rpubsUpload(
            String contextId,
            String title,
            String rmdFile,
            String htmlFile,
            String uploadId,
            boolean isUpdate,
            ServerRequestCallback<Boolean> callback) {}

      @Override
      public void rpubsTerminateUpload(String contextId, ServerRequestCallback<VoidResponse> callback) {}

      private int requests_;
      private ServerRequestCallback<HTMLCapabilities> callback_;
   }

   private EventBus events_;
   private CapabilityServer server_;
   private FileTypeCommands commands_;
}
