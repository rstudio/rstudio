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
 * {@link Attempt}, and the gate reopens (and what was armed for quitting is
 * undone) only once the latest attempt has failed, no attempt's request is
 * still unanswered, and the session has accepted none; an accepted quit
 * ends with the page or with the session's abort event.
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
       * This attempt told the desktop frame a quit is coming, which a quit
       * that then does not happen must undo. Until some attempt does, the
       * frame's pending quit is not the gate's: a console q() records one
       * without passing through it.
       */
      public void armed()
      {
         if (round_ == rounds_)
            armed_ = true;
      }

      /**
       * This attempt's quit_session request is on its way: until it is
       * answered, the session may yet quit for it.
       */
      public void sent()
      {
         if (round_ == rounds_ && !inFlight_)
         {
            inFlight_ = true;
            outstanding_++;
         }
      }

      /**
       * The session accepted this quit: it is exiting, or will report the
       * quit aborted (which reopens the gate). An acceptance that arrives
       * after that abort is ignored.
       */
      public void acknowledged()
      {
         settle();
         if (round_ == rounds_)
            acknowledged_ = true;
      }

      /**
       * This attempt's quit did not happen: refused, failed, or given up
       * before it was sent. The gate reopens once the latest attempt has
       * failed, no request the session may still act on is unanswered, and
       * the session accepted none; until then the undo falls to whichever
       * outstanding request fails last, or to the session's exit or abort.
       * True when the gate reopened and an attempt had armed the desktop
       * frame, which the caller must then undo.
       */
      public boolean fail()
      {
         settle();
         if (round_ != rounds_ || acknowledged_)
            return false;
         if (isCurrent())
            latestFailed_ = true;
         if (!latestFailed_ || outstanding_ > 0)
            return false;
         boolean undo = armed_;
         reopen();
         return undo;
      }

      private void settle()
      {
         if (!inFlight_)
            return;
         inFlight_ = false;
         if (round_ == rounds_)
            outstanding_--;
      }

      private final int round_;
      private boolean inFlight_ = false;
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

      /**
       * Arming failed: give the quit up. Answers as {@link Attempt#fail}
       * does, so a claim that replaced a stalled quit the session has
       * accepted (or may still accept) leaves the gate closed for it.
       */
      public boolean release()
      {
         if (claim_ != this)
            return false;
         claim_ = null;
         return attempt_.fail();
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
      latestFailed_ = false;
      outstanding_ = 0;
      armed_ = false;
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
      latestFailed_ = false;
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
   /** the latest attempt failed, but an older request was still unanswered */
   private boolean latestFailed_ = false;
   /** quit_session requests sent this round and not yet answered */
   private int outstanding_ = 0;
   /** an attempt this round told the desktop frame a quit is coming */
   private boolean armed_ = false;
   /** bumped by every reopen(), so an acceptance can tell it came too late */
   private int rounds_ = 0;
}
