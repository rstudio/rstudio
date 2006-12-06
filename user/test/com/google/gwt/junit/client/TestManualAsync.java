// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.junit.client;

import com.google.gwt.user.client.Timer;

/**
 * This test must be run manually to inspect for correct results. Five of these
 * tests are designed to fail in specific ways, the other five should succeed.
 * The name of each test method indicates how it should behave.
 */
public class TestManualAsync extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.junit.JUnit";
  }

  /**
   * Fails normally
   */
  public void testDelayFail() {
    delayTestFinish(100);
    fail();
    finishTest();
  }

  /**
   * Completes normally
   */
  public void testDelayNormal() {
    delayTestFinish(100);
    finishTest();
  }

  /**
   * Fails normally
   */
  public void testFail() {
    fail();
  }

  /**
   * Async fails
   */
  public void testFailAsync() {
    delayTestFinish(200);
    new Timer() {
      public void run() {
        fail();
      }
    }.schedule(100);
  }

  /**
   * Completes normally
   */
  public void testNormal() {
  }

  /**
   * Completes async
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
   * Completes async
   */
  public void testRepeatingNormal() {
    delayTestFinish(200);
    new Timer() {
      public void run() {
        if (++i < 4) {
          delayTestFinish(200);
        } else {
          cancel();
          finishTest();
        }
      }

      private int i = 0;
    }.scheduleRepeating(100);
  }

  /**
   * Completes normally
   */
  public void testSpuriousFinishTest() {
    try {
      finishTest();
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  /**
   * Times out
   */
  public void testTimeoutAsync() {
    delayTestFinish(100);
    new Timer() {
      public void run() {
        finishTest();
      }
    }.schedule(200);
  }

}
