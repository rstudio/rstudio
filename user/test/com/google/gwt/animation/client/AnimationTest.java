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

import com.google.gwt.animation.client.AnimationScheduler.AnimationCallback;
import com.google.gwt.animation.client.testing.StubAnimationScheduler;
import com.google.gwt.core.client.Duration;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.List;

/**
 * Tests the {@link Animation} class.
 * 
 * <p>
 * This class uses the {@link StubAnimationScheduler} to manually trigger
 * callbacks.
 * </p>
 */
public class AnimationTest extends GWTTestCase {

  /**
   * A default implementation of {@link Animation} used for testing.
   */
  private class DefaultAnimation extends Animation {
    protected boolean canceled = false;
    protected boolean completed = false;
    protected double curProgress = -1.0;
    protected boolean started = false;
    protected boolean updated = false;

    public DefaultAnimation() {
      super(scheduler);
    }

    /**
     * Assert the value of canceled.
     */
    public void assertCancelled(boolean expected) {
      assertEquals(expected, canceled);
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
     * Assert the value of started.
     */
    public void assertStarted(boolean expected) {
      assertEquals(expected, started);
    }

    /**
     * Assert the value of updated.
     */
    public void assertUpdated(boolean expected) {
      assertEquals(expected, updated);
    }

    public void reset() {
      canceled = false;
      completed = false;
      updated = false;
      started = false;
      curProgress = -1.0;
    }

    @Override
    protected void onCancel() {
      super.onCancel();
      canceled = true;
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

    @Override
    protected void onUpdate(double progress) {
      updated = true;
      curProgress = progress;
    }
  }

  /**
   * A custom {@link Animation} used for testing.
   */
  private class TestAnimation extends DefaultAnimation {
    @Override
    protected void onCancel() {
      canceled = true;
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

  /**
   * The maximum delay before an animation will run. Animations may run slowly
   * if the browser tab is not focused.
   * 
   * Increase this multiplier to increase the duration of the tests, reducing
   * the potential of an error caused by timing issues.
   */
  private static int DELAY_MULTIPLIER = 3000;

  private List<AnimationCallback> callbacks;
  private double curTime;
  private StubAnimationScheduler scheduler;

  @Override
  public String getModuleName() {
    return "com.google.gwt.animation.Animation";
  }

  /**
   * Test canceling an {@link Animation} after it completes.
   */
  public void testCancelAfterOnComplete() {
    final TestAnimation anim = new TestAnimation();
    anim.run(DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Complete the animation.
    // TODO(cromwellian) remove this 4000 hack after the failure is found
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER + 4000);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
    anim.assertCancelled(false);
    assertEquals(0, callbacks.size());
    anim.reset();

    // Cancel the animation.
    anim.cancel(); // no-op.
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertProgress(-1);
    anim.assertCancelled(false);
  }

  /**
   * Test canceling an {@link Animation} before onStart is called.
   */
  public void testCancelBeforeOnStart() {
    final TestAnimation anim = new TestAnimation();

    // Run the animation in the future.
    anim.run(DELAY_MULTIPLIER, curTime + 1000);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    assertEquals(1, callbacks.size());
    anim.reset();

    // Cancel the animation before it starts.
    anim.cancel();
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
    assertEquals(0, callbacks.size());
  }

  /**
   * Test canceling an {@link Animation} between updates.
   */
  public void testCancelBetweenUpdates() {
    TestAnimation anim = new TestAnimation();
    anim.run(10 * DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Update the animation.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(true);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Cancel the animation.
    assertEquals(1, callbacks.size());
    anim.cancel();
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
    anim.assertProgress(-1.0);
    assertEquals(0, callbacks.size());
  }

  /**
   * Test canceling an {@link Animation} within onComplete.
   */
  public void testCancelDuringOnComplete() {
    final TestAnimation anim = new TestAnimation() {
      @Override
      protected void onComplete() {
        super.onComplete();
        assertStarted(false);
        assertUpdated(false);
        assertCompleted(true);
        assertCancelled(false);
        reset();

        // Cancel the animation.
        cancel(); // no-op.
      }
    };

    // Run the animation.
    anim.run(DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Force the animation to complete.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER + 100);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    assertEquals(0, callbacks.size());
  }

  /**
   * Test canceling an {@link Animation} within onStart.
   */
  public void testCancelDuringOnStart() {
    final TestAnimation anim = new TestAnimation() {
      @Override
      protected void onStart() {
        super.onStart();
        assertStarted(true);
        assertUpdated(false);
        assertCompleted(false);
        assertCancelled(false);
        reset();

        // Cancel the animation.
        cancel();
      }
    };

    // Run the animation.
    anim.run(DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCancelled(true);
    anim.assertCompleted(false);
    assertEquals(0, callbacks.size());
  }

  /**
   * Test canceling an {@link Animation} during an update.
   */
  public void testCancelDuringOnUpdate() {
    final TestAnimation anim = new TestAnimation() {
      @Override
      protected void onUpdate(double progress) {
        super.onUpdate(progress);
        assertStarted(false);
        assertUpdated(true);
        assertCompleted(false);
        assertCancelled(false);
        reset();

        // Cancel the test while it is running.
        cancel();
      }
    };

    // Run the animation.
    anim.run(10 * DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.assertProgress(-1.0);
    anim.reset();

    // Force the update.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
    anim.assertProgress(-1.0);
    assertEquals(0, callbacks.size());
  }

  /**
   * Test the default implementations of events in {@link Animation}.
   */
  public void testDefaultAnimation() {
    // Verify initial state
    final DefaultAnimation anim = new DefaultAnimation();
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCancelled(false);
    anim.assertCompleted(false);

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
    anim.run(10 * DELAY_MULTIPLIER, curTime + DELAY_MULTIPLIER);
    anim.cancel();
    anim.assertProgress(-1.0);
    anim.assertStarted(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
  }

  /**
   * Test that restarting an {@link Animation} within onComplete does not break.
   * See issue 5639.
   */
  public void testRunDuringOnComplete() {
    final TestAnimation anim = new TestAnimation() {
      @Override
      protected void onComplete() {
        super.onComplete();
        assertStarted(false);
        assertUpdated(false);
        assertCompleted(true);
        assertCancelled(false);
        reset();

        // Run the animation.
        run(DELAY_MULTIPLIER);
      }
    };

    // Run the animation.
    anim.run(DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Force the animation to complete.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER + 100);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    assertEquals(1, callbacks.size());
  }

  /**
   * Test that an animation runs in the future.
   */
  public void testRunFuture() {
    final TestAnimation anim = new TestAnimation();
    anim.run(2 * DELAY_MULTIPLIER, curTime + 2 * DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Update, but still before the start time.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Start the animation.
    executeLastCallbackAt(curTime + 2 * DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Update the animation.
    executeLastCallbackAt(curTime + 3 * DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(true);
    anim.assertCompleted(false);
    anim.reset();

    // Complete the animation.
    executeLastCallbackAt(curTime + 4 * DELAY_MULTIPLIER + 100);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
  }

  /**
   * Test that an animation runs synchronously if its duration is 0.
   */
  public void testRunNow() {
    final TestAnimation anim = new TestAnimation();
    anim.run(2 * DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Update the progress.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(true);
    anim.assertCompleted(false);
    anim.reset();

    // Complete the animation.
    executeLastCallbackAt(curTime + 2 * DELAY_MULTIPLIER + 100);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
  }

  /**
   * Test running an animation that started in the past.
   */
  public void testRunPast() {
    final TestAnimation anim = new TestAnimation();
    anim.run(3 * DELAY_MULTIPLIER, curTime - DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Update the progress.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(true);
    anim.assertCompleted(false);
    anim.reset();

    // Complete the animation.
    executeLastCallbackAt(curTime + 2 * DELAY_MULTIPLIER + 100);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
  }

  /**
   * Test running an animation that started and finished in the past.
   */
  public void testRunPaster() {
    final TestAnimation anim = new TestAnimation();
    anim.run(DELAY_MULTIPLIER, curTime - 2 * DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    scheduler = new StubAnimationScheduler();
    callbacks = scheduler.getAnimationCallbacks();
    curTime = Duration.currentTimeMillis();
  }

  @Override
  protected void gwtTearDown() throws Exception {
    scheduler = null;
    callbacks = null;
  }

  /**
   * Execute the last callback requested from the scheduler at the specified
   * time.
   * 
   * @param timestamp the time to pass to the callback
   */
  private void executeLastCallbackAt(double timestamp) {
    assertTrue(callbacks.size() > 0);
    AnimationCallback callback = callbacks.remove(callbacks.size() - 1);
    callback.execute(timestamp);
  }
}
