/*
 * SessionOpenerTests.java
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
import java.util.List;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.DummyApplicationServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleRestartRCompletedEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

public class SessionOpenerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // A ping sent while the old session is still shutting down fails; the
   // restart is only done once a later ping reaches the new session.
   public void testFailedPingIsRetriedUntilTheSessionAnswers()
   {
      PingServer server = new PingServer();
      SessionOpener opener = createOpener(server);

      List<String> announced = new ArrayList<>();
      events_.addHandler(ConsoleRestartRCompletedEvent.TYPE, event -> announced.add("announced"));

      List<String> completed = new ArrayList<>();
      opener.sendPing(10, 5000, 5000, () -> completed.add("completed"));

      delayTestFinish(30000);
      new Timer()
      {
         @Override
         public void run()
         {
            assertTrue("expected a ping", server.pending_.size() > 0);
            server.failAll();

            new Timer()
            {
               @Override
               public void run()
               {
                  assertEquals("announced a restart that never answered", 0, announced.size());
                  assertEquals("gave up after a failed ping", 0, completed.size());
                  assertTrue("expected a retry after the failure", server.pending_.size() > 0);

                  server.succeedAll();
                  assertEquals("answered ping did not announce the restart", 1, announced.size());
                  assertEquals("answered ping did not complete the wait", 1, completed.size());
                  finishTest();
               }
            }.schedule(500);
         }
      }.schedule(500);
   }

   // A ping can sit unanswered for a long time while rserver relaunches the
   // session behind its proxy. That wait must not queue up more pings, and a
   // response that only arrives after we gave up still means R is back.
   public void testOutstandingPingIsAwaitedAndStillAnnouncesAfterTimeout()
   {
      PingServer server = new PingServer();
      SessionOpener opener = createOpener(server);

      List<String> announced = new ArrayList<>();
      events_.addHandler(ConsoleRestartRCompletedEvent.TYPE, event -> announced.add("announced"));

      List<String> completed = new ArrayList<>();
      opener.sendPing(10, 100, 5000, () -> completed.add("completed"));

      delayTestFinish(30000);
      new Timer()
      {
         @Override
         public void run()
         {
            // the loop ticked ~100 times over its 10ms interval, but only the
            // first tick got to send a ping
            assertEquals("piled on pings while one was outstanding", 1, server.pending_.size());

            // the wait timed out, so the caller has been released
            assertEquals("timed-out wait did not complete", 1, completed.size());
            assertEquals("announced a restart that never answered", 0, announced.size());

            server.succeedAll();
            assertEquals("late ping did not announce the restart", 1, announced.size());
            assertEquals("late ping completed the wait twice", 1, completed.size());
            finishTest();
         }
      }.schedule(1000);
   }

   // Announcing a restart refocuses the console, so an answer that arrives
   // well after we gave up must not pull the user back to it.
   public void testAnswerPastTheGracePeriodIsIgnored()
   {
      PingServer server = new PingServer();
      SessionOpener opener = createOpener(server);

      List<String> announced = new ArrayList<>();
      events_.addHandler(ConsoleRestartRCompletedEvent.TYPE, event -> announced.add("announced"));

      List<String> completed = new ArrayList<>();
      opener.sendPing(10, 100, 100, () -> completed.add("completed"));

      delayTestFinish(30000);
      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals("timed-out wait did not complete", 1, completed.size());
            assertEquals("expected the timed-out ping to be outstanding", 1, server.pending_.size());

            server.succeedAll();
            assertEquals("announced a restart long after giving up", 0, announced.size());
            assertEquals("late ping completed the wait twice", 1, completed.size());
            finishTest();
         }
      }.schedule(1000);
   }

   private SessionOpener createOpener(PingServer server)
   {
      events_ = new EventBus(null, null);
      return new SessionOpener(() -> null, () -> null, () -> server, () -> events_);
   }

   private static class PingServer extends DummyApplicationServerOperations
   {
      @Override
      public void ping(ServerRequestCallback<VoidResponse> requestCallback)
      {
         pending_.add(requestCallback);
      }

      public void succeedAll()
      {
         VoidResponse response = JavaScriptObject.createObject().cast();
         for (ServerRequestCallback<VoidResponse> callback : takePending())
            callback.onResponseReceived(response);
      }

      public void failAll()
      {
         for (ServerRequestCallback<VoidResponse> callback : takePending())
            callback.onError(new PingError());
      }

      private List<ServerRequestCallback<VoidResponse>> takePending()
      {
         List<ServerRequestCallback<VoidResponse>> pending = new ArrayList<>(pending_);
         pending_.clear();
         return pending;
      }

      private final List<ServerRequestCallback<VoidResponse>> pending_ = new ArrayList<>();
   }

   private static class PingError implements ServerError
   {
      @Override
      public int getCode()
      {
         return ServerError.CONNECTION;
      }

      @Override
      public String getMessage()
      {
         return "ping failed";
      }

      @Override
      public String getRedirectUrl()
      {
         return null;
      }

      @Override
      public ServerErrorCause getCause()
      {
         return null;
      }

      @Override
      public String getUserMessage()
      {
         return getMessage();
      }

      @Override
      public JSONValue getClientInfo()
      {
         return null;
      }
   }

   private EventBus events_;
}
