/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.animation;

import com.google.gwt.core.client.Duration;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/**
 * Tests the {@link Animation} class.
 */
public class AnimationTest extends GWTTestCase {
  /**
   * A customer {@link Animation} used for testing.
   */
  private static class TestAnimation extends Animation {
    public boolean cancelled = false;
    public boolean completed = false;
    public double curProgress = -1.0;
    public boolean started = false;

    @Override
    public void onCancel() {
      cancelled = true;
    }

    @Override
    public void onComplete() {
      completed = true;
    }

    @Override
    public void onStart() {
      started = true;
    }

    @Override
    public void onUpdate(double progress) {
      curProgress = progress;
    }

    public void reset() {
      cancelled = false;
      completed = false;
      started = false;
      curProgress = -1.0;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test canceling an {@link Animation} before it starts.
   */
  public void testCancelBeforeStarted() {
    final TestAnimation anim = new TestAnimation();
    double curTime = Duration.currentTimeMillis();
    anim.run(100, curTime + 200);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        assertFalse(anim.started);
        assertFalse(anim.completed);
        assertEquals(-1.0, anim.curProgress);
        anim.cancel();
        assertTrue(anim.cancelled);
        assertFalse(anim.started);
        assertFalse(anim.completed);
        anim.reset();
      }
    }.schedule(50);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        assertFalse(anim.started);
        assertFalse(anim.completed);
        assertEquals(-1.0, anim.curProgress);
        finishTest();
      }
    }.schedule(100);

    // Wait for test to finish
    delayTestFinish(150);
  }

  /**
   * Test canceling an {@link Animation} after it completes.
   */
  public void testCancelWhenComplete() {
    final TestAnimation anim = new TestAnimation();
    anim.run(100);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        assertTrue(anim.started);
        assertTrue(anim.completed);
        assertTrue(anim.curProgress > 0.0 && anim.curProgress <= 1.0);
        anim.cancel();
        assertFalse(anim.cancelled);
        assertTrue(anim.completed);
        anim.reset();
      }
    }.schedule(150);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        assertFalse(anim.started);
        assertFalse(anim.completed);
        assertEquals(-1.0, anim.curProgress);
        finishTest();
      }
    }.schedule(200);

    // Wait for test to finish
    delayTestFinish(250);
  }

  /**
   * Test canceling an {@link Animation} while it is running.
   */
  public void testCancelWhileRunning() {
    final TestAnimation anim = new TestAnimation();
    anim.run(500);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        assertTrue(anim.started);
        assertFalse(anim.completed);
        assertTrue(anim.curProgress > 0.0 && anim.curProgress <= 1.0);
        anim.cancel();
        assertTrue(anim.cancelled);
        assertFalse(anim.completed);
        anim.reset();
      }
    }.schedule(50);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        assertFalse(anim.started);
        assertFalse(anim.completed);
        assertEquals(-1.0, anim.curProgress);
        finishTest();
      }
    }.schedule(150);

    // Wait for test to finish
    delayTestFinish(200);
  }

  /**
   * Test that an animation runs synchronously if its duration is 0.
   */
  public void testNoDelay() {
    final TestAnimation animNow = new TestAnimation();
    final TestAnimation animPast = new TestAnimation();
    final TestAnimation animFuture = new TestAnimation();

    // Run animations
    double curTime = Duration.currentTimeMillis();
    animNow.run(0);
    animPast.run(0, curTime - 150);
    animFuture.run(0, curTime + 150);

    // Test synchronous start
    assertTrue(animNow.started);
    assertTrue(animNow.completed);
    assertEquals(-1.0, animNow.curProgress);

    assertTrue(animPast.started);
    assertTrue(animPast.completed);
    assertEquals(-1.0, animFuture.curProgress);

    assertFalse(animFuture.started);
    assertFalse(animFuture.completed);
    assertEquals(-1.0, animFuture.curProgress);
  }

  /**
   * Test general functionality.
   */
  public void testRun() {
    final TestAnimation animNow = new TestAnimation();
    final TestAnimation animPast = new TestAnimation();
    final TestAnimation animFuture = new TestAnimation();

    // Run animations
    double curTime = Duration.currentTimeMillis();
    animNow.run(300);
    animPast.run(300, curTime - 150);
    animFuture.run(300, curTime + 150);

    // Test synchronous start
    assertTrue(animNow.started);
    assertFalse(animNow.completed);
    assertTrue(animNow.curProgress >= 0.0 && animNow.curProgress <= 2.0);

    assertTrue(animPast.started);
    assertFalse(animPast.completed);
    assertTrue(animPast.curProgress > 0.0 && animPast.curProgress <= 1.0);

    assertFalse(animFuture.started);
    assertFalse(animFuture.completed);
    assertEquals(-1.0, animFuture.curProgress);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        assertTrue(animNow.started);
        assertFalse(animNow.completed);
        assertTrue(animNow.curProgress > 0.0 && animNow.curProgress <= 2.0);

        assertTrue(animPast.started);
        assertFalse(animPast.completed);
        assertTrue(animPast.curProgress > 0.0 && animPast.curProgress <= 1.0);

        assertFalse(animFuture.started);
        assertFalse(animFuture.completed);
        assertEquals(-1.0, animFuture.curProgress);
      }
    }.schedule(50);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        assertTrue(animNow.started);
        assertTrue(animNow.completed);
        assertTrue(animNow.curProgress > 0.0 && animNow.curProgress <= 1.0);

        assertTrue(animPast.started);
        assertTrue(animPast.completed);
        assertTrue(animPast.curProgress > 0.0 && animPast.curProgress <= 1.0);

        assertTrue(animFuture.started);
        assertFalse(animFuture.completed);
        assertTrue(animFuture.curProgress > 0.0
            && animFuture.curProgress <= 1.0);
        finishTest();
      }
    }.schedule(350);

    // Wait for the test to finish
    delayTestFinish(500);
  }
}
