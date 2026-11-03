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

/**
 * Admits one quit at a time. The session answers an aborted quit with an
 * event that names no attempt, so a second quit sent before that event
 * arrived could be cancelled by it (and its normal exit then taken for a
 * crash); the gate closes when a quit starts and reopens only when that
 * quit fails or is aborted (a successful one ends the page).
 *
 * A caller that must arm something for its quit first (the desktop frame
 * keeps an R version, or an update, for the relaunch) takes a {@link Claim}
 * before arming, so that nothing is armed for a quit that then cannot start
 * and nothing armed is consumed by a quit already under way. The gate is
 * closed to everyone else while a claim is held.
 */
public class QuitGate
{
   public class Claim
   {
      private Claim() {}

      /**
       * Start the quit this claim was taken for. True once, and only while
       * the claim is still held; the gate stays closed for that quit.
       */
      public boolean start()
      {
         if (claim_ != this)
            return false;
         claim_ = null;
         return true;
      }

      /** Arming failed: give the quit up and reopen the gate. */
      public void release()
      {
         if (claim_ == this)
         {
            claim_ = null;
            closed_ = false;
         }
      }
   }

   /** Admit a quit now: false when one is in progress or claimed. */
   public boolean start()
   {
      if (closed_)
         return false;
      closed_ = true;
      return true;
   }

   /** Reserve the next quit: null when one is in progress or claimed. */
   public Claim claim()
   {
      if (closed_)
         return null;
      closed_ = true;
      claim_ = new Claim();
      return claim_;
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
   }

   private boolean closed_ = false;
   private Claim claim_ = null;
}
