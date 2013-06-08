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

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link Timer} functionality.
 */
public class TimerTest extends GWTTestCase {

  private List<Timer> executedTimers;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    executedTimers = new ArrayList<Timer>();
  }

  private final class TestTimer extends Timer {
    @Override
    public void run() {
      executedTimers.add(this);
    }
  }

  public void testTimer() {
    Timer timer = new TestTimer();
    assertFalse(timer.isRunning());
    timer.schedule(10);
    assertTrue(timer.isRunning());
    assertExecutedTimerCount(1);
  }

  public void testCancelTimer() {
    Timer timer = new TestTimer();
    timer.schedule(10);
    timer.cancel();
    assertFalse(timer.isRunning());
    assertExecutedTimerCount(0);
  }

  public void testRescheduleTimer() {
    Timer timer = new TestTimer();
    timer.schedule(10);
    timer.schedule(20);
    assertExecutedTimerCount(1);
  }

  public void testRescheduleTimerRepeatingToNonRepeating() {
    Timer timer = new TestTimer();
    timer.scheduleRepeating(10);
    timer.schedule(20);
    assertExecutedTimerCount(1);
  }

  private final class CancelingTestTimer extends Timer {
    Timer otherTimer;

    @Override
    public void run() {
      otherTimer.cancel();
      executedTimers.add(this);
    }
  }

  // Issue https://code.google.com/p/google-web-toolkit/issues/detail?id=8101
  public void testCancelTimer_ieBug() {
    final CancelingTestTimer timer1 = new CancelingTestTimer();
    final CancelingTestTimer timer2 = new CancelingTestTimer();
    timer1.otherTimer = timer2;
    timer2.otherTimer = timer1;

    timer1.schedule(10);
    timer2.schedule(10);

    // only one of them should have been executed
    assertExecutedTimerCount(1);
  }

  private final class ReschedulingTestTimer extends Timer {
    Timer otherTimer;

    @Override
    public void run() {
      otherTimer.schedule(2000); // schedule far ahead, should be practically same as canceling
      executedTimers.add(this);
    }
  }

  // Issue https://code.google.com/p/google-web-toolkit/issues/detail?id=8101
  public void testRescheduleTimer_ieBug() {
    final ReschedulingTestTimer timer1 = new ReschedulingTestTimer();
    final ReschedulingTestTimer timer2 = new ReschedulingTestTimer();
    timer1.otherTimer = timer2;
    timer2.otherTimer = timer1;

    timer1.schedule(10);
    timer2.schedule(10);

    // only one of them should have been executed
    assertExecutedTimerCount(1);
  }

  private void assertExecutedTimerCount(final int count) {
    delayTestFinish(400);
    new Timer() {
      @Override
      public void run() {
        assertEquals(count, executedTimers.size());
        finishTest();
      }
    }.schedule(200);
  }
}
