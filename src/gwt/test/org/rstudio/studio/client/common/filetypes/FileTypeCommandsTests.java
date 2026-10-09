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
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

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

   public void testProbeStartsAtConstructionWhenSessionInfoExists()
   {
      // the eager singleton may be created after session init
      assertEquals(1, server_.requests_);
      assertFalse(commands_.hasHTMLCapabilities());
      server_.latest().onResponseReceived(createCapabilities(true));
      assertTrue(commands_.hasHTMLCapabilities());
   }

   public void testProbeStartsOnSessionInitWithoutWaitingForAnEditor()
   {
      EventBus events = new EventBus(null, null);
      Session session = new Session(events);
      CapabilityServer server = new CapabilityServer();
      new FileTypeCommands(session, events, server);
      assertEquals(0, server.requests_);

      session.setSessionInfo(createSessionInfo());
      events.fireEvent(new SessionInitEvent());
      assertEquals(1, server.requests_);
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
      server_.latest().onResponseReceived(createCapabilities(true));
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

      server_.latest().onResponseReceived(createCapabilities(false));
      assertEquals(1, received.size());
      assertFalse(received.get(0).isRMarkdownSupported());
   }

   public void testDismissedEditorDoesNotReceiveProbeResult()
   {
      List<HTMLCapabilities> received = new ArrayList<>();
      HandlerRegistration registration = commands_.withHTMLCapabilities(received::add);
      registration.removeHandler();
      server_.latest().onResponseReceived(createCapabilities(true));
      assertEquals(0, received.size());
   }

   public void testEditorsOpenedAfterProbeReceiveCachedResult()
   {
      events_.fireEvent(new SessionInitEvent());
      server_.latest().onResponseReceived(createCapabilities(true));
      List<HTMLCapabilities> received = new ArrayList<>();
      commands_.withHTMLCapabilities(received::add);
      assertEquals(1, received.size());
      assertTrue(received.get(0).isRMarkdownSupported());
      assertEquals(1, server_.requests_);
   }

   public void testPackageChangeDuringProbeSupersedesIt()
   {
      List<HTMLCapabilities> received = new ArrayList<>();
      commands_.withHTMLCapabilities(received::add);
      events_.fireEvent(new PackageStateChangedEvent(null));
      assertEquals(2, server_.requests_);

      // the superseded probe's answer is stale
      server_.callbacks_.get(0).onResponseReceived(createCapabilities(false));
      assertEquals(0, received.size());
      assertFalse(commands_.hasHTMLCapabilities());

      server_.callbacks_.get(1).onResponseReceived(createCapabilities(true));
      assertEquals(1, received.size());
      assertTrue(received.get(0).isRMarkdownSupported());
   }

   public void testFailedProbeIsRetriedForWaitingEditors()
   {
      List<HTMLCapabilities> received = new ArrayList<>();
      commands_.withHTMLCapabilities(received::add);
      server_.latest().onError(new ProbeError());
      assertEquals(1, server_.requests_);
      assertEquals(0, received.size());

      delayTestFinish(FileTypeCommands.HTML_CAPABILITIES_RETRY_MS * 5);
      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals(2, server_.requests_);
            server_.latest().onResponseReceived(createCapabilities(true));
            assertEquals(1, received.size());
            assertTrue(commands_.hasHTMLCapabilities());
            finishTest();
         }
      }.schedule(FileTypeCommands.HTML_CAPABILITIES_RETRY_MS * 2);
   }

   public void testLaterEditorsRequestAgainAfterRetriesAreExhausted()
   {
      List<HTMLCapabilities> received = new ArrayList<>();
      commands_.withHTMLCapabilities(received::add);
      for (int i = 0; i <= FileTypeCommands.MAX_HTML_CAPABILITIES_RETRIES; i++)
      {
         server_.latest().onError(new ProbeError());
         commands_.withHTMLCapabilities(received::add);
      }

      // each failure is followed by one request from the next editor only
      assertEquals(FileTypeCommands.MAX_HTML_CAPABILITIES_RETRIES + 2, server_.requests_);

      // actions queued before the final failure were dropped with it; only
      // the editor that asked afterwards is answered
      server_.latest().onResponseReceived(createCapabilities(true));
      assertEquals(1, received.size());
   }

   public void testActionsQueuedBeforeARetriedFailureAreStillAnswered()
   {
      List<HTMLCapabilities> received = new ArrayList<>();
      commands_.withHTMLCapabilities(received::add);
      server_.latest().onError(new ProbeError());
      commands_.withHTMLCapabilities(received::add);
      assertEquals(2, server_.requests_);

      server_.latest().onResponseReceived(createCapabilities(true));
      assertEquals(2, received.size());
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
         callbacks_.add(callback);
      }

      ServerRequestCallback<HTMLCapabilities> latest()
      {
         return callbacks_.get(callbacks_.size() - 1);
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
      private final List<ServerRequestCallback<HTMLCapabilities>> callbacks_ = new ArrayList<>();
   }

   private static class ProbeError implements ServerError
   {
      @Override
      public int getCode() { return EXECUTION; }

      @Override
      public String getMessage() { return "probe failed"; }

      @Override
      public String getRedirectUrl() { return null; }

      @Override
      public ServerErrorCause getCause() { return null; }

      @Override
      public String getUserMessage() { return getMessage(); }

      @Override
      public JSONValue getClientInfo() { return null; }
   }

   private EventBus events_;
   private CapabilityServer server_;
   private FileTypeCommands commands_;
}
