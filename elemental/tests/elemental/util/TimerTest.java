/*
 * Copyright 2010 Google Inc.
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
package elemental.util;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link Timer}.
 */
public class TimerTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  /**
   * Tests that {@link Timer#schedule(int)} works.
   */
  public void testTimeout() {
    new Timer() {
      @Override
      public void run() {
        finishTest();
      }
    }.schedule(500);

    delayTestFinish(2000);
  }

  /**
   * Tests that {@link Timer#scheduleRepeating(int)} works repeatedly.
   */
  public void testInterval() {
    new Timer() {
      int count;

      @Override
      public void run() {
        // Make sure we see at least two events.
        ++count;
        if (count >= 2) {
          cancel();
          finishTest();
        }
      }
    }.scheduleRepeating(100);

    delayTestFinish(2000);
  }
}
