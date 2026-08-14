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
   // the login-required dialog. Previously this polled once per second for as
   // long as the dialog was up.
   private static final int MIN_POLL_INTERVAL_MS = 3000;
   private static final int MAX_POLL_INTERVAL_MS = 30000;

   public RemoteServerAuthWatcher(CheckAuthStatus authStatusChecker)
   {
      this(authStatusChecker, MIN_POLL_INTERVAL_MS, MAX_POLL_INTERVAL_MS);
   }

   // Package-private for testing. The production intervals are measured in
   // seconds, which would make a test of the backoff slower than the whole
   // rest of the unit test suite; tests pass in scaled-down equivalents.
   RemoteServerAuthWatcher(CheckAuthStatus authStatusChecker,
                           int minIntervalMs,
                           int maxIntervalMs)
   {
      authStatusChecker_ = authStatusChecker;
      minInterval_ = minIntervalMs;
      maxInterval_ = maxIntervalMs;
      isListening_ = false;
      nextInterval_ = minInterval_;
      lastCheckMillis_ = 0;
      pollTimer_ = new Timer(){
         @Override
         public void run()
         {
            lastCheckMillis_ = System.currentTimeMillis();
            try
            {
               authStatusChecker_.checkAuthStatus();
            }
            finally
            {
               // Schedule the next (backed-off) poll. The timer is one-shot,
               // so this call *is* the poll chain, not just a safety net -- and
               // it's in a finally so that a throw from the check can't break
               // the chain and leave the dialog up with nothing polling behind
               // it.
               scheduleNextPoll();
            }
         };
      };

      // watch for the tab becoming visible/hidden so we can pause polling
      // while the tab is in the background (registered once for the lifetime
      // of the watcher; onVisibilityChanged is a no-op when not listening)
      registerVisibilityHandler();
   }

   public void start()
   {
      // Already polling: leave the existing chain alone rather than restarting
      // it. setUnauthorized() calls through to here, and a re-delivered
      // unauthorized event shouldn't wind the backoff back to the minimum --
      // the tab has been sitting on the dialog either way. (The base version
      // stopped and restarted here, which with a fixed 1s interval made no
      // difference.)
      if (isListening_)
         return;

      isListening_ = true;
      nextInterval_ = minInterval_;

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
      nextInterval_ = minInterval_;

      // Both the "Try again now" button and every hidden->visible transition
      // land here, so without a floor a user alt-tabbing between their login
      // tab and the IDE -- or clicking the button repeatedly, since the common
      // outcome of a click looks identical to not clicking -- issues one auth
      // request per switch, unbounded. Defer rather than drop: returning here
      // without a timer pending would leave a tab that was just made visible
      // with no poll chain at all.
      long sinceLastCheck = System.currentTimeMillis() - lastCheckMillis_;
      if (sinceLastCheck < minInterval_)
      {
         pollTimer_.schedule((int) (minInterval_ - sinceLastCheck));
         return;
      }

      // running the timer performs the check and schedules the next poll
      pollTimer_.run();
   }

   private void scheduleNextPoll()
   {
      // don't poll a hidden tab - checkNow() resumes when it becomes visible
      if (!isListening_ || !isDocumentVisible())
         return;

      pollTimer_.schedule(nextInterval_);
      nextInterval_ = Math.min(nextInterval_ * 2, maxInterval_);
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
   private final int minInterval_;
   private final int maxInterval_;
   private boolean isListening_;
   private int nextInterval_;
   private long lastCheckMillis_;
   private Timer pollTimer_;
}
