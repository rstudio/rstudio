/*
 * QuitGate.java
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
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.Duration;

/**
 * Admits one quit at a time. The session answers an aborted quit with an
 * event that names no attempt, so a second quit sent before that event
 * arrived could be cancelled by it (and its normal exit then taken for a
 * crash); the gate closes when a quit starts and reopens only when that
 * quit fails or is aborted (a successful one ends the page).
 *
 * A quit can also stall before the session has answered it (a last-chance
 * save that never completes, a desktop call that never calls back, a
 * session too busy to answer), and nothing then reopens the gate. So a
 * quit that has been in progress for {@link #kStaleQuitMs} no longer
 * refuses the next one, which re-runs the sequence as closing the window
 * again always did; the race above is accepted for a quit that old.
 *
 * The stalled quit's callbacks still run once it is replaced, and either
 * quit can be the one the session answers: it takes the first quit_session
 * it reads and exits, and the other request then fails. So each quit is an
 * {@link Attempt}, and only the latest attempt's failure, and only while
 * the session has accepted none, reopens the gate and undoes what was armed
 * for the quit; an accepted quit ends with the page or with the session's
 * abort event.
 *
 * A caller that must arm something for its quit first (the desktop frame
 * keeps an R version, or an update, for the relaunch) takes a {@link Claim}
 * before arming, so that nothing is armed for a quit that then cannot start
 * and nothing armed is consumed by a quit already under way. The gate is
 * closed to everyone else while a claim is held.
 */
public class QuitGate
{
   /** How long a quit may be in progress before another is admitted. */
   public static final double kStaleQuitMs = 30 * 1000;

   /** One admitted quit. */
   public class Attempt
   {
      private Attempt()
      {
         round_ = rounds_;
      }

      /** False once a later quit has been admitted or claimed, or the gate reopened. */
      public boolean isCurrent()
      {
         return current_ == this;
      }

      /**
       * The session accepted this quit: it is exiting, or will report the
       * quit aborted (which reopens the gate). An acceptance that arrives
       * after that abort is ignored.
       */
      public void acknowledged()
      {
         if (round_ == rounds_)
            acknowledged_ = true;
      }

      /**
       * This attempt's quit did not start. True when the caller must undo
       * what it armed for the quit, and the gate is open again; false when
       * that is not this attempt's to undo: a later quit replaced it, or the
       * session accepted a quit (whichever attempt sent it) and is exiting.
       */
      public boolean fail()
      {
         if (!isCurrent() || acknowledged_)
            return false;
         reopen();
         return true;
      }

      private final int round_;
   }

   public class Claim
   {
      private Claim(Attempt attempt)
      {
         attempt_ = attempt;
      }

      /**
       * Start the quit this claim was taken for. Its attempt once, and only
       * while the claim is still held; null otherwise. The gate stays
       * closed for that quit.
       */
      public Attempt start()
      {
         if (claim_ != this)
            return null;
         claim_ = null;
         return attempt_;
      }

      /** Arming failed: give the quit up and reopen the gate. */
      public void release()
      {
         if (claim_ == this)
            reopen();
      }

      private final Attempt attempt_;
   }

   /** Admit a quit now: its attempt, or null when one is in progress or claimed. */
   public Attempt start()
   {
      return start(Duration.currentTimeMillis());
   }

   /** Reserve the next quit: null when one is in progress or claimed. */
   public Claim claim()
   {
      return claim(Duration.currentTimeMillis());
   }

   public boolean isClosed()
   {
      return closed_;
   }

   /** The quit in progress did not happen: admit the next one. */
   public void reopen()
   {
      closed_ = false;
      claim_ = null;
      current_ = null;
      acknowledged_ = false;
      rounds_++;
   }

   Attempt start(double now)
   {
      if (!admits(now))
         return null;
      return close(now);
   }

   Claim claim(double now)
   {
      if (!admits(now))
         return null;
      claim_ = new Claim(close(now));
      return claim_;
   }

   private boolean admits(double now)
   {
      return !closed_ || now - closedAt_ >= kStaleQuitMs;
   }

   private Attempt close(double now)
   {
      closed_ = true;
      closedAt_ = now;
      claim_ = null;
      current_ = new Attempt();
      return current_;
   }

   private boolean closed_ = false;
   private double closedAt_ = 0;
   private Claim claim_ = null;
   /** the latest quit admitted or claimed; null once the gate reopens */
   private Attempt current_ = null;
   /** the session accepted a quit since the gate last reopened */
   private boolean acknowledged_ = false;
   /** bumped by every reopen(), so an acceptance can tell it came too late */
   private int rounds_ = 0;
}
