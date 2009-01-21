/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.client;

import static com.google.gwt.junit.client.GWTTestCaseTest.SetUpTearDownState.IS_SETUP;
import static com.google.gwt.junit.client.GWTTestCaseTest.SetUpTearDownState.IS_TORNDOWN;

import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;

/**
 * This test must be run manually to inspect for correct results. Many of these
 * tests are designed to fail in specific ways, the rest should succeed. The
 * name of each test method indicates how it should behave.
 */
public class TestManualAsync extends GWTTestCaseTest {

  // The following tests (all prefixed with test_) are intended to test the
  // interaction of synchronous failures (within event handlers) with various
  // other types of failures and successes. All of them are expected to fail
  // with the message "Expected failure".
  //
  // Nomenclature for these tests:
  // DTF => delayTestFinish()
  // SF => synchronous failure (from event handler)
  // FT => finishTest()
  // F => fail()
  // R => return;

  public void test_dtf_sf() {
    delayTestFinish();
    synchronousFailure("test_dtf_sf");
  }

  public void test_dtf_sf_f() {
    delayTestFinish();
    synchronousFailure("test_dtf_sf_f");
    failNow("test_dtf_sf_f");
  }

  public void test_dtf_sf_ft() {
    delayTestFinish();
    synchronousFailure("test_dtf_sf_ft");
    finishTest();
  }

  public void test_dtf_sf_r_f() {
    delayTestFinish();
    synchronousFailure("test_dtf_sf_r_f");
    failLater("test_dtf_sf_r_f");
  }

  public void test_dtf_sf_r_ft() {
    delayTestFinish();
    synchronousFailure("test_dtf_sf_r_ft");
    finishTestLater();
  }

  public void test_sf() {
    synchronousFailure("test_sf");
  }

  public void test_sf_dtf_f() {
    synchronousFailure("test_sf_dtf_f");
    delayTestFinish();
    failNow("test_sf_dtf_f");
  }

  public void test_sf_dtf_ft() {
    synchronousFailure("test_sf_dtf_ft");
    delayTestFinish();
    finishTest();
  }

  public void test_sf_dtf_r_f() {
    synchronousFailure("test_sf_dtf_r_f");
    delayTestFinish();
    failLater("test_sf_dtf_r_f");
  }

  public void test_sf_dtf_r_ft() {
    synchronousFailure("test_sf_dtf_r_ft");
    delayTestFinish(5 * 60 * 1000);
    finishTestLater();
  }

  public void test_sf_f() {
    synchronousFailure("test_sf_f");
    failNow("test_sf_f");
  }

  /**
   * Fails normally.
   */
  public void testDelayFail() {
    delayTestFinish(100);
    fail("Expected failure");
    finishTest();
  }

  /**
   * Completes normally.
   */
  public void testDelayNormal() {
    delayTestFinish(100);
    finishTest();
  }

  /**
   * Fails normally.
   */
  public void testFail() {
    fail("Expected failure");
  }

  /**
   * Async fails.
   */
  public void testFailAsync() {
    delayTestFinish(1000);
    new Timer() {
      public void run() {
        fail("Expected failure");
      }
    }.schedule(100);
  }

  /**
   * Tests the case where a JUnit exception is thrown from an event handler, but
   * after this test method has completed successfully.
   * 
   * This test should *not* fail, but the next one should.
   */
  public void testLateFailPart1() {
    // Leave the test in synchronous mode, but crank up a timer to fail in 2.5s.
    new Timer() {
      @Override
      public void run() {
        // This fail should be called during the next test.
        fail();
      }
    }.schedule(2500);

    // We don't actually assert anything here. This test exists solely to make
    // the next one fail.
  }

  /**
   * Second half of the previous test.
   */
  public void testLateFailPart2() {
    // Go into async mode from 5s, finishing in 4. The timer from the previous
    // test will go off and call fail() before finishTest() is called.
    delayTestFinish(5000);
    new Timer() {
      @Override
      public void run() {
        finishTest();
      }
    }.schedule(4000);
  }

  /**
   * Completes normally.
   */
  public void testNormal() {
  }

  /**
   * Completes async.
   */
  public void testNormalAsync() {
    delayTestFinish(200);
    new Timer() {
      public void run() {
        finishTest();
      }
    }.schedule(100);
  }

  /**
   * Completes async.
   */
  public void testRepeatingNormal() {
    delayTestFinish(200);
    new Timer() {
      private int i = 0;

      public void run() {
        if (++i < 4) {
          delayTestFinish(200);
        } else {
          cancel();
          finishTest();
        }
      }
    }.scheduleRepeating(100);
  }

  /**
   * Fails async.
   */
  public void testSetUpTearDownFailAsync() {
    assertEquals(IS_SETUP, setupTeardownFlag);
    delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        fail("Expected failure");
      }
    }.schedule(1);

    new Timer() {
      @Override
      public void run() {
        /*
         * The failing test should have triggered tearDown.
         */
        if (setupTeardownFlag != IS_TORNDOWN) {
          recordOutofBandError("Bad async failure tearDown behavior not catchable by JUnit");
        }
      }
    }.schedule(100);
  }

  /**
   * Completes async.
   */
  public void testSetUpTearDownFailAsyncHadNoOutOfBandErrors() {
    assertNoOutOfBandErrorsAsync();
  }

  /**
   * Times out async.
   */
  public void testSetUpTearDownTimeoutAsync() {
    assertSame(IS_SETUP, setupTeardownFlag);
    delayTestFinish(1);
    new Timer() {
      @Override
      public void run() {
        /*
         * The failing test should have triggered tearDown.
         */
        if (setupTeardownFlag != IS_TORNDOWN) {
          recordOutofBandError("Bad async timeout tearDown behavior not catchable by JUnit");
        }
      }
    }.schedule(100);
  }

  /**
   * Completes async.
   */
  public void testSetUpTearDownTimeoutAsyncHadNoOutOfBandErrors() {
    assertNoOutOfBandErrorsAsync();
  }

  /**
   * Completes normally.
   */
  public void testSpuriousFinishTest() {
    try {
      finishTest();
      fail("Unexpected failure");
    } catch (IllegalStateException e) {
    }
  }

  /**
   * Times out.
   */
  public void testTimeoutAsync() {
    delayTestFinish(100);
    new Timer() {
      public void run() {
        finishTest();
      }
    }.schedule(200);
  }

  // Call delayTestFinish() with enough time so that failLater() will
  // definitely fail.
  private void delayTestFinish() {
    delayTestFinish(2500);
  }

  // Fail asynchronously after a small amount of time.
  private void failLater(final String failMsg) {
    new Timer() {
      @Override
      public void run() {
        failNow(failMsg);
      }
    }.schedule(100);
  }

  // Fail synchronously with an "expected failure" message.
  private void failNow(String failMsg) {
    fail("Expected failure (" + failMsg + ")");
  }

  // Finish the test asynchronously after a small amount of time.
  private void finishTestLater() {
    new Timer() {
      @Override
      public void run() {
        finishTest();
      }
    }.schedule(1);
  }

  // Trigger a test failure synchronously, but from within an event handler.
  // (The exception thrown from fail() will get caught by the GWT
  // UncaughtExceptionHandler).
  private void synchronousFailure(final String failMsg) {
    ButtonElement btn = Document.get().createButtonElement();
    Document.get().getBody().appendChild(btn);
    Event.sinkEvents(btn, Event.ONCLICK);

    EventListener listener = new EventListener() {
      public void onBrowserEvent(Event event) {
        failNow(failMsg);
      }
    };

    DOM.setEventListener(btn.<com.google.gwt.user.client.Element>cast(), listener);
    btn.click();
  }
}
