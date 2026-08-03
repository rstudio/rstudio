/*
 * RemoteServerAuthWatcher.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
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
package org.rstudio.studio.client.server.remote;


import com.google.gwt.user.client.Timer;

public class RemoteServerAuthWatcher
{
   public static interface CheckAuthStatus
   {
      void checkAuthStatus();
   }

   // While the session is unauthorized we poll the server to detect when the
   // user has signed back in (typically in another browser tab). The poll
   // starts at the minimum interval and backs off up to the maximum so that a
   // user who is briefly logged out gets picked up quickly, without hammering
   // the server for the (potentially very long) time an abandoned tab sits on
   // the login-required dialog. Previously this polled once per second, which
   // caused load problems on busy production systems.
   private static final int MIN_POLL_INTERVAL_MS = 3000;
   private static final int MAX_POLL_INTERVAL_MS = 30000;

   public RemoteServerAuthWatcher(CheckAuthStatus authStatusChecker)
   {
      authStatusChecker_ = authStatusChecker;
      isListening_ = false;
      nextInterval_ = MIN_POLL_INTERVAL_MS;
      pollTimer_ = new Timer(){
         @Override
         public void run()
         {
            authStatusChecker_.checkAuthStatus();

            // schedule the next (backed-off) poll
            scheduleNextPoll();
         };
      };

      // watch for the tab becoming visible/hidden so we can pause polling
      // while the tab is in the background (registered once for the lifetime
      // of the watcher; onVisibilityChanged is a no-op when not listening)
      registerVisibilityHandler();
   }

   public void start()
   {
      if (isListening_)
         stop();

      isListening_ = true;
      nextInterval_ = MIN_POLL_INTERVAL_MS;

      // only poll while the tab is visible; if it's hidden we wait for it to
      // become visible again (see onVisibilityChanged)
      scheduleNextPoll();
   }

   public void stop()
   {
      isListening_ = false;
      pollTimer_.cancel();
   }

   // Poll immediately and reset the backoff. Invoked when the user explicitly
   // asks to retry now, or when a hidden tab becomes visible again.
   public void checkNow()
   {
      if (!isListening_)
         return;

      pollTimer_.cancel();
      nextInterval_ = MIN_POLL_INTERVAL_MS;

      // running the timer performs the check and schedules the next poll
      pollTimer_.run();
   }

   private void scheduleNextPoll()
   {
      // don't poll a hidden tab - checkNow() resumes when it becomes visible
      if (!isListening_ || !isDocumentVisible())
         return;

      pollTimer_.schedule(nextInterval_);
      nextInterval_ = Math.min(nextInterval_ * 2, MAX_POLL_INTERVAL_MS);
   }

   private void onVisibilityChanged()
   {
      if (!isListening_)
         return;

      if (isDocumentVisible())
      {
         // tab became visible again: check right away and resume polling
         checkNow();
      }
      else
      {
         // tab is hidden: pause polling until it becomes visible again
         pollTimer_.cancel();
      }
   }

   private static native boolean isDocumentVisible()
   /*-{
      return $doc.visibilityState !== "hidden";
   }-*/;

   private native void registerVisibilityHandler()
   /*-{
      var self = this;
      $doc.addEventListener("visibilitychange", $entry(function() {
         self.@org.rstudio.studio.client.server.remote.RemoteServerAuthWatcher::onVisibilityChanged()();
      }));
   }-*/;

   private CheckAuthStatus authStatusChecker_;
   private boolean isListening_;
   private int nextInterval_;
   private Timer pollTimer_;
}
