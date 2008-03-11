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

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

/**
 * Tests the {@link WidgetAnimation} class.
 */
public class WidgetAnimationTest extends GWTTestCase {
  /**
   * A customer {@link Animation} used for testing.
   */
  private static class TestWidgetAnimation extends WidgetAnimation {
    public boolean cancelled = false;
    public boolean completed = false;
    public boolean started = false;
    public boolean instaneousRun = false;
    public double curProgress = -1.0;

    @Override
    public void onCancel() {
      cancelled = true;
    }

    @Override
    public void onComplete() {
      completed = true;
    }

    @Override
    public void onInstantaneousRun() {
      instaneousRun = true;
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
  /**
   * Return a module that disables widget animations.
   */
  public String getModuleName() {
    return "com.google.gwt.user.WidgetAnimationTest";
  }

  /**
   * Test general functionality.
   */
  public void testRun() {
    TestWidgetAnimation anim = new TestWidgetAnimation();

    // Run animations
    long curTime = (new Date()).getTime();
    anim.run(300);

    // Check the results
    assertFalse(anim.started);
    assertFalse(anim.completed);
    assertTrue(anim.instaneousRun);
    assertEquals(-1.0, anim.curProgress);
  }
}
