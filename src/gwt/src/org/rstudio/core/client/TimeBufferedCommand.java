/*
 * TimeBufferedCommand.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

import com.google.gwt.user.client.Timer;

import java.util.Date;

/**
 * Manages the execution of logic that should not be run too frequently.
 * Multiple calls over a (caller-defined) period of time will be coalesced
 * into one call.
 *
 * The command can optionally be run on a scheduled ("passive") basis; use
 * the two- or three-arg constructor. IMPORTANT NOTE: The implementation of
 * performAction must check if shouldSchedulePassive is true, and if it is,
 * then it should call schedulePassive() whenever it is done with the
 * operation. Failure to do so correctly (e.g. in error cases) will cause
 * the passive runs to immediately stop occurring. 
 */
public abstract class TimeBufferedCommand
{
   /**
    * Creates a TimeBufferedCommand that will only run when nudged.
    */
   protected TimeBufferedCommand(int activeIntervalMillis)
   {
      this(-1, -1, activeIntervalMillis);
   }

   /**
    * Creates a TimeBufferedCommand that will run when nudged, or every
    * passiveIntervalMillis milliseconds, whichever comes first.
    */
   protected TimeBufferedCommand(int passiveIntervalMillis,
                                 int activeIntervalMillis)
   {
      this(passiveIntervalMillis, passiveIntervalMillis, activeIntervalMillis);
   }

   /**
    * Creates a TimeBufferedCommand that will run when nudged, or every
    * passiveIntervalMillis milliseconds, whichever comes first; with a
    * custom period before the first "passive" run.
    */
   protected TimeBufferedCommand(int initialIntervalMillis,
                                 int passiveIntervalMillis,
                                 int activeIntervalMillis)
   {
      this.initialIntervalMillis_ = initialIntervalMillis;
      this.passiveIntervalMillis_ = passiveIntervalMillis;
      this.activeIntervalMillis_ = activeIntervalMillis;

      if (initialIntervalMillis_ >= 0 && passiveIntervalMillis_ > 0)
         scheduleExecution(true, Math.max(1, initialIntervalMillis_));
   }

   /**
    * See class javadoc for details about shouldSchedulePassive flag.
    */
   protected abstract void performAction(boolean shouldSchedulePassive);

   /**
    * Request that this command execute soon. (How soon depends
    * on the activeIntervalMillis constructor param.) 
    */
   public final void nudge()
   {
      scheduleExecution(false, activeIntervalMillis_);
   }

   public final void suspend()
   {
      stopped_ = true;
   }

   public final void resume()
   {
      assert passiveIntervalMillis_ <= 0 : "Cannot call start() on a " +
                                           "TimeBufferedCommand that fires on " +
                                           "passive intervals. Once stopped, " +
                                           "they stay stopped.";
      if (passiveIntervalMillis_ > 0)
         throw new IllegalStateException("Cannot call start() on a " +
                                         "TimeBufferedCommand that fires on " +
                                         "passive intervals. Once stopped, " +
                                         "they stay stopped.");
      stopped_ = false;
   }

   private final void scheduleExecution(final boolean passive, final int millis)
   {
      new Timer() {
         @Override
         public void run()
         {
            execute(passive, millis);
         }
      }.schedule(millis);
   }

   private final void execute(final boolean passive, int millisAgo)
   {
      if (stopped_)
         return;

      Date now = new Date();
      // see if we were preempted by someone else executing
      if (lastExecuted_ != null)
      {
         long millisSinceLast = now.getTime() - lastExecuted_.getTime();
         if (millisSinceLast < millisAgo - 50) // some fudge factor
         {
            // Someone executed in front of us. Abort this execute, but
            // if we're in the passive chain of execution, then reschedule.
            if (passive)
            {
               int gap = passiveIntervalMillis_ - (int)millisSinceLast;
               gap = Math.max(1, gap); // a non-positive value will cause error
               scheduleExecution(true, gap);
            }
            return;
         }
      }
      lastExecuted_ = now;

      performAction(passive);
   }

   protected final void schedulePassive()
   {
      scheduleExecution(true, passiveIntervalMillis_);
   }

   private final int initialIntervalMillis_;
   private final int passiveIntervalMillis_;
   private final int activeIntervalMillis_;
   protected Date lastExecuted_;
   private boolean stopped_;
}
