/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.animation;

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.junit.GWTMockUtilities;

import junit.framework.TestCase;

/**
 * Tests that AnmationScheduler can be disarmed using GWTMockUtilities.
 */
public class AnimationSchedulerJreTest extends TestCase {

  @Override
  public void setUp() {
    GWTMockUtilities.disarm();
  }

  @Override
  public void tearDown() {
    GWTMockUtilities.restore();
  }

  public void testLogConfiguration() {
    assertNotNull(AnimationScheduler.get());
  }
}
