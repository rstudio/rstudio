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
package com.google.gwt.animation.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/**
 * Tests the {@link Animation} class.
 */
public class AnimationTest extends GWTTestCase {
  /**
   * Increase this multiplier to increase the duration of the tests, reducing
   * the potential of an error caused by timing issues.
   */
  private static int DELAY_MULTIPLIER = 100;

  /**
   * A default implementation of {@link Animation} used for testing.
   */
  private static class DefaultAnimation extends Animation {
    protected boolean cancelled = false;
    protected boolean completed = false;
    protected boolean started = false;
    protected double curProgress = -1.0;

    /**
     * Assert the value of canceled.
     */
    public void assertCancelled(boolean expected) {
      assertEquals(expected, cancelled);
    }

    /**
     * Assert the value of completed.
     */
    public void assertCompleted(boolean expected) {
      assertEquals(expected, completed);
    }

    /**
     * Assert that the progress equals the specified value.
     */
    public void assertProgress(double expected) {
      assertEquals(expected, curProgress);
    }

    /**
     * Assert that the progress falls between min and max, inclusively.
     */
    public void assertProgressRange(double min, double max) {
      assertTrue(curProgress >= min && curProgress <= max);
    }

    /**
     * Assert the value of started.
     */
    public void assertStarted(boolean expected) {
      assertEquals(expected, started);
    }

    public void reset() {
      cancelled = false;
      completed = false;
      started = false;
      curProgress = -1.0;
    }

    @Override
    protected void onUpdate(double progress) {
      curProgress = progress;
    }

    @Override
    protected void onCancel() {
      super.onCancel();
      cancelled = true;
    }

    @Override
    protected void onComplete() {
      super.onComplete();
      completed = true;
    }

    @Override
    protected void onStart() {
      super.onStart();
      started = true;
    }
  }

  /**
   * A custom {@link Animation} used for testing.
   */
  private static class TestAnimation extends DefaultAnimation {
    /*
     * TODO: Consider timing issues for test system. Specifically, onUpdate is
     * not guaranteed to be called in the Animation timer if we miss our
     * deadline.
     */

    @Override
    protected void onCancel() {
      cancelled = true;
    }

    @Override
    protected void onComplete() {
      completed = true;
    }

    @Override
    protected void onStart() {
      started = true;
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
    delayTestFinish(20 * DELAY_MULTIPLIER);
    anim.run(10 * DELAY_MULTIPLIER, curTime + 10 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        anim.assertStarted(false);
        anim.assertCompleted(false);
        anim.assertProgress(-1.0);
        anim.cancel();
        anim.assertStarted(false);
        anim.assertCancelled(true);
        anim.assertCompleted(false);
        anim.reset();
      }
    }.schedule(5 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        anim.assertStarted(false);
        anim.assertCompleted(false);
        anim.assertProgress(-1.0);
        finishTest();
      }
    }.schedule(15 * DELAY_MULTIPLIER);
  }

  /**
   * Test canceling an {@link Animation} after it completes.
   */
  public void testCancelWhenComplete() {
    final TestAnimation anim = new TestAnimation();
    delayTestFinish(25 * DELAY_MULTIPLIER);
    anim.run(10 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        anim.assertStarted(true);
        anim.assertCompleted(true);
        anim.assertProgressRange(0.0, 1.0);
        anim.cancel();
        anim.assertCancelled(false);
        anim.assertCompleted(true);
        anim.reset();
      }
    }.schedule(15 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        anim.assertStarted(false);
        anim.assertCompleted(false);
        anim.assertProgress(-1.0);
        finishTest();
      }
    }.schedule(20 * DELAY_MULTIPLIER);
  }

  /**
   * Test canceling an {@link Animation} while it is running.
   */
  public void testCancelWhileRunning() {
    final TestAnimation anim = new TestAnimation();
    delayTestFinish(20 * DELAY_MULTIPLIER);
    anim.run(50 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        anim.assertStarted(true);
        anim.assertCompleted(false);
        anim.cancel();
        anim.assertCancelled(true);
        anim.assertCompleted(false);
        anim.reset();
      }
    }.schedule(5 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        anim.assertStarted(false);
        anim.assertCompleted(false);
        anim.assertProgress(-1.0);
        finishTest();
      }
    }.schedule(15 * DELAY_MULTIPLIER);
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
    animPast.run(0, curTime - 15 * DELAY_MULTIPLIER);
    animFuture.run(0, curTime + 15 * DELAY_MULTIPLIER);

    // Test synchronous start
    animNow.assertStarted(true);
    animNow.assertCompleted(true);
    animNow.assertProgress(-1.0);

    animPast.assertStarted(true);
    animPast.assertCompleted(true);
    animPast.assertProgress(-1.0);

    animFuture.assertStarted(false);
    animFuture.assertCompleted(false);
    animFuture.assertProgress(-1.0);
  }

  /**
   * Test the default implementations of events in {@link Animation}.
   */
  public void testDefaultAnimation() {
    // Verify initial state
    final DefaultAnimation anim = new DefaultAnimation();
    anim.assertProgress(-1.0);
    anim.assertStarted(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);

    // Starting an animation calls onUpdate(interpolate(0.0))
    anim.reset();
    anim.onStart();
    anim.assertProgress(0.0);
    anim.assertStarted(true);
    anim.assertCompleted(false);
    anim.assertCancelled(false);

    // Completing an animation calls onUpdate(interpolate(1.0))
    anim.reset();
    anim.onComplete();
    anim.assertProgress(1.0);
    anim.assertStarted(false);
    anim.assertCompleted(true);
    anim.assertCancelled(false);

    // Canceling an animation that is not running does not call onStart or
    // onComplete
    anim.reset();
    anim.onCancel();
    anim.assertProgress(-1.0);
    anim.assertStarted(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);

    // Canceling an animation before it starts does not call onStart or
    // onComplete
    anim.reset();
    anim.run(20 * DELAY_MULTIPLIER, Duration.currentTimeMillis() + 100
        * DELAY_MULTIPLIER);
    anim.cancel();
    anim.assertProgress(-1.0);
    anim.assertStarted(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
  }

  /**
   * Test general functionality.
   */
  public void testRun() {
    final TestAnimation animNow = new TestAnimation();
    final TestAnimation animPast = new TestAnimation();
    final TestAnimation animFuture = new TestAnimation();

    delayTestFinish(50 * DELAY_MULTIPLIER);
    // Run animations
    double curTime = Duration.currentTimeMillis();
    animNow.run(30 * DELAY_MULTIPLIER);
    animPast.run(30 * DELAY_MULTIPLIER, curTime - 10 * DELAY_MULTIPLIER);
    animFuture.run(30 * DELAY_MULTIPLIER, curTime + 10 * DELAY_MULTIPLIER);

    // Test synchronous start
    animNow.assertStarted(true);
    animNow.assertCompleted(false);
    animNow.assertProgress(-1.0);

    animPast.assertStarted(true);
    animPast.assertCompleted(false);
    animPast.assertProgress(-1.0);

    animFuture.assertStarted(false);
    animFuture.assertCompleted(false);
    animFuture.assertProgress(-1.0);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        animNow.assertStarted(true);
        animNow.assertCompleted(false);
        animNow.assertProgressRange(0.0, 1.0);

        animPast.assertStarted(true);
        animPast.assertCompleted(false);
        animPast.assertProgressRange(0.0, 1.0);

        animFuture.assertStarted(false);
        animFuture.assertCompleted(false);
        animFuture.assertProgress(-1.0);
      }
    }.schedule(5 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        animNow.assertStarted(true);
        animNow.assertCompleted(false);
        animNow.assertProgressRange(0.0, 1.0);

        animPast.assertStarted(true);
        animPast.assertCompleted(false);
        animPast.assertProgressRange(0.0, 1.0);

        animFuture.assertStarted(true);
        animFuture.assertCompleted(false);
        animFuture.assertProgressRange(0.0, 1.0);
      }
    }.schedule(15 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        animNow.assertStarted(true);
        animNow.assertCompleted(false);
        animNow.assertProgressRange(0.0, 1.0);

        animPast.assertStarted(true);
        animPast.assertCompleted(true);
        animPast.assertProgressRange(0.0, 1.0);

        animFuture.assertStarted(true);
        animFuture.assertCompleted(false);
        animFuture.assertProgressRange(0.0, 1.0);
      }
    }.schedule(25 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        animNow.assertStarted(true);
        animNow.assertCompleted(true);
        animNow.assertProgressRange(0.0, 1.0);

        animPast.assertStarted(true);
        animPast.assertCompleted(true);
        animPast.assertProgressRange(0.0, 1.0);

        animFuture.assertStarted(true);
        animFuture.assertCompleted(false);
        animFuture.assertProgressRange(0.0, 1.0);
      }
    }.schedule(35 * DELAY_MULTIPLIER);

    // Check progress
    new Timer() {
      @Override
      public void run() {
        animNow.assertStarted(true);
        animNow.assertCompleted(true);
        animNow.assertProgressRange(0.0, 1.0);

        animPast.assertStarted(true);
        animPast.assertCompleted(true);
        animPast.assertProgressRange(0.0, 1.0);

        animFuture.assertStarted(true);
        animFuture.assertCompleted(true);
        animFuture.assertProgressRange(0.0, 1.0);

        finishTest();
      }
    }.schedule(45 * DELAY_MULTIPLIER);
  }
}
