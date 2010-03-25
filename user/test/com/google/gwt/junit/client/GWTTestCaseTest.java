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

/*
 * Portions copyright 2006 Mat Gessel <mat.gessel@gmail.com>; licensed under
 * Apache 2.0
 */
package com.google.gwt.junit.client;

import static com.google.gwt.junit.client.GWTTestCaseTest.SetUpTearDownState.INITIAL;
import static com.google.gwt.junit.client.GWTTestCaseTest.SetUpTearDownState.IS_SETUP;
import static com.google.gwt.junit.client.GWTTestCaseTest.SetUpTearDownState.IS_TORNDOWN;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.Timer;

import junit.framework.AssertionFailedError;

/**
 * This class tests our implementation of the GWTTestCase class which provides
 * the behavior of TestCase that is necessary in GWT.
 */
public class GWTTestCaseTest extends GWTTestCase {

  /**
   * Tracks whether or not setup and teardown have run.
   */
  protected enum SetUpTearDownState {
    INITIAL, IS_SETUP, IS_TORNDOWN
  }

  /**
   * If non-null, records an error related to setup/teardown that could not have
   * been caught during normal test execution.
   */
  private static String outOfBandError = null;

  /**
   * These two variables test the retry functionality, currently used with
   * HtmlUnit.
   */
  private static boolean attemptedOnce = false;
  private static boolean htmlunitMode = true;

  private static void assertNotEquals(double a, double b, double delta) {
    boolean failed = false;
    try {
      assertEquals(a, b, delta);
    } catch (AssertionFailedError e) {
      // EXPECTED
      failed = true;
    }

    if (!failed) {
      fail("Expected failure for assertEquals(" + a + ", " + b + ", " + delta
          + ")");
    }
  }

  private static void assertNotEquals(float a, float b, float delta) {
    boolean failed = false;
    try {
      assertEquals(a, b, delta);
    } catch (AssertionFailedError e) {
      // EXPECTED
      failed = true;
    }

    if (!failed) {
      fail("Expected failure for assertEquals(" + a + ", " + b + ", " + delta
          + ")");
    }
  }

  /**
   * Tracks whether this test has been setup and torn down.
   */
  protected SetUpTearDownState setupTeardownFlag = INITIAL;

  public String getModuleName() {
    return "com.google.gwt.junit.JUnit";
  }

  public void testAssertEqualsDouble() {
    assertEquals(0.0, 0.0, 0.0);
    assertEquals(1.1, 1.1, 0.0);
    assertEquals(-1.1, -1.1, 0.0);
    assertEquals(Float.MIN_VALUE, Float.MIN_VALUE, 0.0);
    assertEquals(Float.MAX_VALUE, Float.MAX_VALUE, 0.0);
    assertNotEquals(0.0, 0.00000000000000000000000000000000000000001, 0.0);
    assertNotEquals(0.0, 0.0000000000000000001, 0.0);
    assertNotEquals(0.0, 0.000000001, 0.0);
    assertNotEquals(0.0, 0.0001, 0.0);
    assertNotEquals(0.0, 0.1, 0.0);
    assertNotEquals(1.0, 2.0, 0.1);
    assertNotEquals(2.0, 1.0, 0.1);
    assertNotEquals(-1.0, -2.0, 0.1);
    assertNotEquals(-2.0, -1.0, 0.1);
  }

  public void testAssertEqualsFloat() {
    assertEquals(0.0f, 0.0f, 0.0f);
    assertEquals(1.1f, 1.1f, 0.0f);
    assertEquals(-1.1f, -1.1f, 0.0f);
    assertEquals(Float.MIN_VALUE, Float.MIN_VALUE, 0.0f);
    assertEquals(Float.MAX_VALUE, Float.MAX_VALUE, 0.0f);
    assertNotEquals(0.0f, 0.00000000000000000000000000000000000000001f, 0.0f);
    assertNotEquals(0.0f, 0.0000000000000000001f, 0.0f);
    assertNotEquals(0.0f, 0.000000001f, 0.0f);
    assertNotEquals(0.0f, 0.0001f, 0.0f);
    assertNotEquals(0.0f, 0.1f, 0.0f);
    assertNotEquals(1.0f, 2.0f, 0.1f);
    assertNotEquals(2.0f, 1.0f, 0.1f);
    assertNotEquals(-1.0f, -2.0f, 0.1f);
    assertNotEquals(-2.0f, -1.0f, 0.1f);
  }

  /**
   * 
   */
  public void testAssertEqualsIntInt() {
    assertEquals(5, 5);

    boolean exWasThrown = false;
    try {
      assertEquals(5, 4);
    } catch (Throwable ex) {
      exWasThrown = true;
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception during assertEquals(int,int) testing");
      }
    }

    if (!exWasThrown) {
      fail("No exception was thrown during assertEquals(int,int) testing");
    }
  }

  /**
   * 
   */
  public void testAssertEqualsObjectObject() {
    Object obj1 = "String";

    assertEquals(obj1, obj1);

    boolean exWasThrown = false;
    try {
      Object obj2 = "not String";
      assertEquals(obj1, obj2);
    } catch (Throwable ex) {
      exWasThrown = true;
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception during assertEquals(String,String) testing");
      }
    }

    if (!exWasThrown) {
      fail("No exception was thrown during assertEquals(String,String) testing");
    }
  }

  /**
   * 
   */
  public void testAssertEqualsStringIntInt() {
    assertEquals("", 5, 5);

    boolean exWasThrown = false;
    try {
      assertEquals("hello", 5, 4);
    } catch (Throwable ex) {
      exWasThrown = true;
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception during assertEquals(String,int,int) testing");
      }
    }

    if (!exWasThrown) {
      fail("No exception was thrown during assertEquals(String,int,int) testing");
    }
  }

  /**
   * 
   */
  public void testAssertEqualsStringObjectObject() {
    Object obj1 = "String";

    assertEquals("msg", obj1, obj1);

    boolean exWasThrown = false;
    try {
      Object obj2 = "not String";
      assertEquals("msg", obj1, obj2);
    } catch (Throwable ex) {
      exWasThrown = true;
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception during assertEquals(String,Object,Object) testing");
      }
    }

    if (!exWasThrown) {
      fail("No exception was thrown during assertEquals(String,Object,Object) testing");
    }
  }

  /**
   * 
   */
  public void testAssertFalse() {
    // Should not fail
    assertFalse(false);

    // Should not fail
    assertFalse("We should be okay", false);

    boolean exWasThrown = false;
    try {
      // Should fail
      assertFalse(true);
    } catch (Throwable ex) {
      exWasThrown = true;
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception during assertFalse(boolean) testing");
      }
    }

    if (!exWasThrown) {
      fail("No exception was thrown");
    }
    exWasThrown = false;

    try {
      // Should fail
      assertFalse("This should be okay", true);
    } catch (Throwable ex) {
      exWasThrown = true;
      if (ex instanceof AssertionFailedError) {
        return;
      }
    }

    if (!exWasThrown) {
      fail("No exception was thrown");
    } else {
      fail("Unexpected exception during assertFalse(String, boolean) testing");
    }
  }

  /**
   * 
   */
  public void testAssertNotNull() {
    assertNotNull("Hello");
    assertNotNull("We should be okay", "Hello");

    try {
      assertNotNull("Hello");
    } catch (Throwable ex) {
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception during assertNotNull(Object) testing");
      }
    }

    try {
      assertNotNull("This should be okay", null);
    } catch (Throwable ex) {
      if (ex instanceof AssertionFailedError) {
        return;
      }
    }

    fail("Unexpected exception during assertNotNull(String, Object) testing");
  }

  /**
   * 
   */
  public void testAssertNotSame() {
    Object obj1 = "Foo";
    Object obj2 = "Bar";

    assertNotSame(obj1, obj2);
    assertNotSame("Hello", obj1, obj2);

    try {
      assertNotSame(obj1, "Baz");
    } catch (Throwable ex) {
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception");
      }
    }
  }

  /**
   * 
   */
  public void testAssertNull() {
    assertNull(null);
    assertNull("We should be okay", null);

    try {
      assertNull("Hello");
    } catch (Throwable ex) {
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception during assertNull(Object) testing");
      }
    }

    try {
      assertNull("This should be okay", "Hello");
    } catch (Throwable ex) {
      if (ex instanceof AssertionFailedError) {
        return;
      }
    }

    fail("Unexpected exception during assertNull(String, Object) testing");
  }

  /**
   * 
   */
  public void testAssertSame() {
    // TODO(mmendez): finish this test
  }

  /**
   * 
   */
  public void testAssertTrue() {
    assertTrue(true);
    assertTrue("We should be okay", true);

    try {
      assertTrue(false);
    } catch (Throwable ex) {
      if (!(ex instanceof AssertionFailedError)) {
        fail("Unexpected type of exception during assertFalse(boolean) testing");
      }
    }

    try {
      assertTrue("This should be okay", false);
    } catch (Throwable ex) {
      if (ex instanceof AssertionFailedError) {
        return;
      }
    }

    fail("Unexpected exception during assertTrue(String, boolean) testing");
  }

  /**
   * Test skipping a test for dev mode.
   */
  @DoNotRunWith(Platform.Devel)
  public void testPlatformDevel() {
    assertTrue("Should not run in devel mode", GWT.isScript());
  }

  /**
   * Test skipping a test for prod mode.
   */
  @DoNotRunWith(Platform.Prod)
  public void testPlatformProd() {
    assertTrue("Should not run in prod mode", !GWT.isScript());
  }

  public void testSetUpTearDown() throws Exception {
    assertSame(IS_SETUP, setupTeardownFlag);
    tearDown();
    assertSame(IS_TORNDOWN, setupTeardownFlag);
    setUp();
    assertSame(IS_SETUP, setupTeardownFlag);
    gwtTearDown();
    assertSame(IS_TORNDOWN, setupTeardownFlag);
    gwtSetUp();
    assertSame(IS_SETUP, setupTeardownFlag);
  }

  public void testSetUpTearDownAsync() {
    assertSame(IS_SETUP, setupTeardownFlag);
    delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        assertSame(IS_SETUP, setupTeardownFlag);
        finishTest();
        if (setupTeardownFlag != IS_TORNDOWN) {
          recordOutofBandError("Bad async success tearDown behavior not catchable by JUnit");
        }
      }
    }.schedule(1);
  }

  public void testSetUpTearDownAsyncHadNoOutOfBandErrors() {
    assertNoOutOfBandErrorsAsync();
  }

  @Override
  protected void gwtSetUp() throws Exception {
    setupTeardownFlag = IS_SETUP;
  }

  @Override
  protected void gwtTearDown() throws Exception {
    if (setupTeardownFlag != IS_SETUP) {
      // Must use window alert to grind the test to a halt in this failure.
      recordOutofBandError("Bad tearDown behavior not catchable by JUnit");
    }
    setupTeardownFlag = IS_TORNDOWN;
  }

  protected static void recordOutofBandError(String outOfBandError) {
    GWTTestCaseTest.outOfBandError = outOfBandError;
  }

  /**
   * Call this method to asynchronously check for out of band errors in the
   * previous test.
   */
  protected void assertNoOutOfBandErrorsAsync() {
    // Give things a chance to settle down.
    delayTestFinish(10000);
    new Timer() {
      @Override
      public void run() {
        if (outOfBandError != null) {
          String msg = outOfBandError;
          outOfBandError = null;
          fail(msg);
        }
        finishTest();
      }

    }.schedule(1000);
  }
}
