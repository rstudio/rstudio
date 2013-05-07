/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.user.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link Timer#cancel()} functionality.
 */
public class TimerCancelTest extends GWTTestCase {

  private static final class CountingTimer extends Timer {
    private int timerCount;
    @Override
    public void run() {
      timerCount++;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  public void testCancelTimer() {
    final CountingTimer canceledTimer = new CountingTimer();

    Timer cancelingTimer = new Timer() {
      @Override
      public void run() {
        assertEquals(0, canceledTimer.timerCount);
        canceledTimer.cancel();
      }
    };
    cancelingTimer.schedule(50);
    canceledTimer.schedule(100);


    busyWait(200);

    delayTestFinish(500);
    new Timer() {
      @Override
      public void run() {
        assertEquals(0, canceledTimer.timerCount);
        finishTest();
      }
    }.schedule(300);
  }

  public void testRestartTimer() {
    final CountingTimer restartedTimer = new CountingTimer();

    Timer cancelingTimer = new Timer() {
      @Override
      public void run() {
        assertEquals(0, restartedTimer.timerCount);
        restartedTimer.cancel();
        restartedTimer.schedule(100);
      }
    };

    cancelingTimer.schedule(50);
    restartedTimer.schedule(100);

    busyWait(200);

    delayTestFinish(500);
    new Timer() {
      @Override
      public void run() {
        assertEquals(1, restartedTimer.timerCount);
        finishTest();
      }
    }.schedule(400);
  }

  private static void busyWait(double duration) {
    /*
     * It seems that IE adds an event to the javascript event loop immediately when a timer expires
     * (supposedly from a separate thread). After this has happened, canceling the timer has no
     * effect because it is already in the queue and no further checks are done when running the
     * event once it reaches the head of the queue.
     *
     * This means that to trigger the bug, we must ensure the timer has been added to the event loop
     * queue before it is canceled. To ensure this happens, we will busy wait until both timers
     * should have fired. This means the following happens:
     *
     * 1) While busy waiting, IE adds events for each timer to the event loop
     *
     * 2) IE pumps the event loop, running the helper timer that cancels the tested timer. This does
     * however not have any effect because the timer is already in the event loop queue.
     *
     * 3) IE pumps the event loop again and runs the event for the tested timer, without realizing
     * that it has been canceled.
     *
     * Without busy waiting, the tested timer would not yet have been added to the event loop queue
     * at the point when the timer is canceled, in which case canceling the timer would work as
     * expected.
     *
     * If the two timers are not scheduled in the same order that they will run, it seems that IE
     * does some additional checks that makes the problem disappear.
     */

    double start = Duration.currentTimeMillis();
    while (Duration.currentTimeMillis() - start <= duration) {
      // Busy wait
    }
  }

}
