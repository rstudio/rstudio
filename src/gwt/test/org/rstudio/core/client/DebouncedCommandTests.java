/*
 * DebouncedCommandTests.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.core.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

public class DebouncedCommandTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // a DebouncedCommand that counts how many times it has executed
   private static class CountingCommand extends DebouncedCommand
   {
      CountingCommand(int delayMs)
      {
         super(delayMs);
      }

      @Override
      protected void execute()
      {
         executeCount++;
      }

      int executeCount = 0;
   }

   // baseline: a nudge() executes once the delay elapses
   public void testNudgeFiresAfterDelay()
   {
      final CountingCommand cmd = new CountingCommand(DELAY_MS);

      delayTestFinish(TEST_TIMEOUT_MS);
      cmd.nudge();

      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals(1, cmd.executeCount);
            finishTest();
         }
      }.schedule(CHECK_MS);
   }

   // cancelPending() drops a queued execution: the nudged command never fires
   public void testCancelPendingDropsQueuedExecution()
   {
      final CountingCommand cmd = new CountingCommand(DELAY_MS);

      delayTestFinish(TEST_TIMEOUT_MS);
      cmd.nudge();
      cmd.cancelPending();

      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals(0, cmd.executeCount);
            finishTest();
         }
      }.schedule(CHECK_MS);
   }

   // the contract that distinguishes cancelPending() from suspend(): a nudge()
   // after cancelPending() is still honored and fires
   public void testNudgeAfterCancelPendingStillFires()
   {
      final CountingCommand cmd = new CountingCommand(DELAY_MS);

      delayTestFinish(TEST_TIMEOUT_MS);
      cmd.nudge();
      cmd.cancelPending();
      cmd.nudge();

      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals(1, cmd.executeCount);
            finishTest();
         }
      }.schedule(CHECK_MS);
   }

   // by contrast, suspend() latches the command off: a later nudge() does not fire
   public void testNudgeAfterSuspendDoesNotFire()
   {
      final CountingCommand cmd = new CountingCommand(DELAY_MS);

      delayTestFinish(TEST_TIMEOUT_MS);
      cmd.suspend();
      cmd.nudge();

      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals(0, cmd.executeCount);
            finishTest();
         }
      }.schedule(CHECK_MS);
   }

   // resume() re-enables a suspended command so that nudge() fires again
   public void testNudgeAfterResumeFires()
   {
      final CountingCommand cmd = new CountingCommand(DELAY_MS);

      delayTestFinish(TEST_TIMEOUT_MS);
      cmd.suspend();
      cmd.resume();
      cmd.nudge();

      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals(1, cmd.executeCount);
            finishTest();
         }
      }.schedule(CHECK_MS);
   }

   private static final int DELAY_MS = 50;
   private static final int CHECK_MS = 300;
   private static final int TEST_TIMEOUT_MS = 2000;
}
