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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

import junit.framework.AssertionFailedError;

import java.util.Collections;

/**
 * This class tests our implementation of the GWTTestCase class which provides the behavior of
 * TestCase that is necessary in GWT.
 */
public class GWTTestCaseTest extends GWTTestCaseTestBase {

  private static void assertNotEquals(double a, double b, double delta) {
    boolean failed = false;
    try {
      assertEquals(a, b, delta);
    } catch (AssertionFailedError e) {
      // EXPECTED
      failed = true;
    }

    if (!failed) {
      fail("Expected failure for assertEquals(" + a + ", " + b + ", " + delta + ")");
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
      fail("Expected failure for assertEquals(" + a + ", " + b + ", " + delta + ")");
    }
  }

  private Object obj1 = Collections.nCopies(1, "data");
  private Object obj2 = Collections.nCopies(2, "data");
  private Object obj1Equal = Collections.nCopies(1, "data");

  @ExpectedFailure(withMessage = "testFail")
  public void testFail() {
    fail("testFail");
  }

  @ExpectedFailure(withType = Exception.class)
  public void testThrowsException() throws Exception {
    throw new Exception();
  }

  @ExpectedFailure(withType = JavaScriptException.class)
  public void testThrowsJavaScriptException() {
    throw new JavaScriptException("name", "desc");
  }

  @ExpectedFailure(withType = NullPointerException.class)
  public void testThrowsNullPointerException() {
    throw new NullPointerException();
  }

  static class SomeNonSerializableException extends RuntimeException {
    public SomeNonSerializableException(String msg) {
      super(msg);
    }
    // no default constructor
    // public SomeNonSerializableException() {}
  }

  // We lose some type information if class meta data is not available, setting expected failure
  // to RuntimeException will ensure this test case passes for no metadata.
  @ExpectedFailure(withType = RuntimeException.class,
      withMessage = "testThrowsNonSerializableException")
  public void testThrowsNonSerializableException() {
    throw new SomeNonSerializableException("testThrowsNonSerializableException");
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

  public void testAssertEqualsIntInt() {
    assertEquals(5, 5);
    assertEquals("msg", 5, 5);
  }

  @ExpectedFailure
  public void testAssertEqualsIntIntFail() {
    assertEquals(5, 4);
  }

  @ExpectedFailure(withMessage = "msg")
  public void testAssertEqualsIntIntFailWithMessage() {
    assertEquals("msg", 5, 4);
  }

  public void testAssertEqualsObjectObject() {
    assertEquals(obj1, obj1Equal);
    assertEquals("msg", obj1, obj1);
  }

  @ExpectedFailure
  public void testAssertEqualsObjectObjectFail() {
    assertEquals(obj1, obj2);
  }

  @ExpectedFailure(withMessage = "msg")
  public void testAssertEqualsObjectObjectFailWithMessage() {
    assertEquals("msg", obj1, obj2);
  }

  public void testAssertFalse() {
    assertFalse(false);
    assertFalse("msg", false);
  }

  @ExpectedFailure
  public void testAssertFalseFail() {
    assertFalse(true);
  }

  @ExpectedFailure(withMessage = "msg")
  public void testAssertFalseFailWithMessage() {
    assertFalse("msg", true);
  }

  public void testAssertNotNull() {
    assertNotNull("Hello");
    assertNotNull("msg", "Hello");
  }

  @ExpectedFailure
  public void testAssertNotNullFail() {
    assertNotNull(null);
  }

  @ExpectedFailure(withMessage = "msg")
  public void testAssertNotNullStringFailWithMessage() {
    assertNotNull("msg", null);
  }

  public void testAssertNotSame() {
    assertNotSame(obj1, obj2);
    assertNotSame("msg", obj1, obj2);
    assertNotSame(obj1, obj1Equal);
    assertNotSame("msg", obj1, obj1Equal);
  }

  @ExpectedFailure
  public void testAssertNotSameFail() {
    assertNotSame(obj1, obj1);
  }

  @ExpectedFailure(withMessage = "msg")
  public void testAssertNotSameFailWithMessage() {
    assertNotSame("msg", obj1, obj1);
  }

  public void testAssertNull() {
    assertNull(null);
    assertNull("msg", null);
  }

  @ExpectedFailure
  public void testAssertNullFail() {
    assertNull("Hello");
  }

  @ExpectedFailure(withMessage = "msg")
  public void testAssertNullFailWithMessage() {
    assertNull("msg", "Hello");
  }

  public void testAssertSame() {
    assertSame(obj1, obj1);
    assertSame("msg", obj1, obj1);
  }

  @ExpectedFailure
  public void testAssertSameFail() {
    assertSame(obj1, obj1Equal);
  }

  @ExpectedFailure(withMessage = "msg")
  public void testAssertSameFailWithMessage() {
    assertSame("msg", obj1, obj1Equal);
  }

  public void testAssertTrue() {
    assertTrue(true);
    assertTrue("msg", true);
  }

  @ExpectedFailure
  public void testAssertTrueFail() {
    assertTrue(false);
  }

  @ExpectedFailure(withMessage = "msg")
  public void testAssertTrueFailWithMessage() {
    assertTrue("msg", false);
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
}
