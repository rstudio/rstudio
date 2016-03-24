/*
 * Copyright 2006 Google Inc.
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
package junit.framework;

import javaemul.internal.annotations.DoNotInline;

/**
 * Translatable version of JUnit's <code>Assert</code>.
 */
public class Assert {
  @DoNotInline
  public static void assertEquals(boolean obj1, boolean obj2) {
    assertEquals("", obj1, obj2);
  }

  @DoNotInline
  public static void assertEquals(byte obj1, byte obj2) {
    assertEquals("", obj1, obj2);
  }

  @DoNotInline
  public static void assertEquals(char obj1, char obj2) {
    assertEquals("", obj1, obj2);
  }

  @DoNotInline
  public static void assertEquals(double obj1, double obj2, double delta) {
    assertEquals("", obj1, obj2, delta);
  }

  @DoNotInline
  public static void assertEquals(float obj1, float obj2, float delta) {
    assertEquals("", obj1, obj2, delta);
  }

  @DoNotInline
  public static void assertEquals(int expected, int actual) {
    assertEquals("", expected, actual);
  }

  @DoNotInline
  public static void assertEquals(long obj1, long obj2) {
    assertEquals("", obj1, obj2);
  }

  @DoNotInline
  public static void assertEquals(Object obj1, Object obj2) {
    assertEquals("", obj1, obj2);
  }

  @DoNotInline
  public static void assertEquals(short expected, short actual) {
    assertEquals("", expected, actual);
  }

  @DoNotInline
  public static void assertEquals(String str, boolean obj1, boolean obj2) {
    assertEquals(str, Boolean.valueOf(obj1), Boolean.valueOf(obj2));
  }

  @DoNotInline
  public static void assertEquals(String str, byte obj1, byte obj2) {
    assertEquals(str, Byte.valueOf(obj1), Byte.valueOf(obj2));
  }

  @DoNotInline
  public static void assertEquals(String str, char obj1, char obj2) {
    assertEquals(str, Character.valueOf(obj1), Character.valueOf(obj2));
  }

  @DoNotInline
  public static void assertEquals(String str, double obj1, double obj2,
      double delta) {
    // Handles special cases like NaN
    if (Double.compare(obj1, obj2) == 0) {
      return;
    } else if (Math.abs(obj1 - obj2) <= delta) {
      return;
    } else {
      failFloatingPointNotEquals(str, format(obj1), format(obj2), format(delta));
    }
  }

  @DoNotInline
  public static void assertEquals(String str, float obj1, float obj2,
      float delta) {
        // Handles special cases like NaN
    if (Float.compare(obj1, obj2) == 0) {
      return;
    } else if (Math.abs(obj1 - obj2) <= delta) {
      return;
    } else {
      failFloatingPointNotEquals(str, format(obj1), format(obj2), format(delta));
    }
  }

  @DoNotInline
  public static void assertEquals(String msg, int expected, int actual) {
    if (expected != actual) {
      failNotEquals(msg, expected, actual);
    }
  }

  @DoNotInline
  public static void assertEquals(String str, long obj1, long obj2) {
    assertEquals(str, new Long(obj1), new Long(obj2));
  }

  @DoNotInline
  public static void assertEquals(String msg, Object obj1, Object obj2) {
    if (obj1 == null && obj2 == null) {
      return;
    }

    if (obj1 != null && obj1.equals(obj2)) {
      return;
    }

    failNotEquals(msg, obj1, obj2);
  }

  @DoNotInline
  public static void assertEquals(String str, short obj1, short obj2) {
    assertEquals(str, Short.valueOf(obj1), Short.valueOf(obj2));
  }

  @DoNotInline
  public static void assertEquals(String obj1, String obj2) {
    assertEquals("", obj1, obj2);
  }

  @DoNotInline
  public static void assertEquals(String message, String expected, String actual) {
    assertEquals(message, (Object) expected, (Object) actual);
  }

  @DoNotInline
  public static void assertFalse(boolean condition) {
    assertFalse(null, condition);
  }

  @DoNotInline
  public static void assertFalse(String message, boolean condition) {
    assertEquals(message, false, condition);
  }

  @DoNotInline
  public static void assertNotNull(Object obj) {
    assertNotNull(null, obj);
  }

  @DoNotInline
  public static void assertNotNull(String msg, Object obj) {
    if (obj == null) {
      fail(concatMessages(msg, "expected: not null, actual: null"));
    }
  }

  @DoNotInline
  public static void assertNotSame(Object obj1, Object obj2) {
    assertNotSame(null, obj1, obj2);
  }

  @DoNotInline
  public static void assertNotSame(String msg, Object obj1, Object obj2) {
    if (obj1 != obj2) {
      return;
    }

    fail(concatMessages(msg, "expected: not same as " + format(obj1) + ", actual: same"));
  }

  @DoNotInline
  public static void assertNull(Object obj) {
    assertNull(null, obj);
  }

  @DoNotInline
  public static void assertNull(String msg, Object obj) {
    assertEquals(msg, null, obj);
  }

  @DoNotInline
  public static void assertSame(Object obj1, Object obj2) {
    assertSame(null, obj1, obj2);
  }

  @DoNotInline
  public static void assertSame(String msg, Object obj1, Object obj2) {
    if (obj1 == obj2) {
      return;
    }

    failNotSame(msg, obj1, obj2);
  }

  @DoNotInline
  public static void assertTrue(boolean condition) {
    assertTrue(null, condition);
  }

  @DoNotInline
  public static void assertTrue(String message, boolean condition) {
    assertEquals(message, true, condition);
  }

  @DoNotInline
  public static void fail() {
    fail(null);
  }

  @DoNotInline
  public static void fail(String message) {
    if (message == null) {
      message = "failed";
    }

    throw new AssertionFailedError(message);
  }

  @DoNotInline
  public static void failNotEquals(String message, Object expected,
      Object actual) {
    fail(concatMessages(message, "expected: " + format(expected) + ", actual: " + format(actual)));
  }

  @DoNotInline
  public static void failNotSame(String message, Object expected, Object actual) {
    fail(concatMessages(
        message, "expected: same as " + format(expected) + ", actual: " + format(actual)));
  }

  @DoNotInline
  public static void failSame(String message) {
    fail(concatMessages(message, "expected: not same, actual: same"));
  }

  /**
   * Concatenates a user-supplied message and a JUnit-generated message.  The user message
   * may be null, which is treated as "".
   */
  private static String concatMessages(String userMessage, String junitMessage) {
    if (userMessage == null || userMessage.isEmpty()) {
      return junitMessage;
    }
    return userMessage + " - " + junitMessage;
  }

  /**
   * Fails with a message that the given floating-point numbers aren't within the delta.
   */
  private static void failFloatingPointNotEquals(
      String userMessage, String expected, String actual, String delta) {
    fail(concatMessages(
        userMessage,
        "expected: within " + delta + " of " + expected + ", actual: " + actual));
  }

  /**
   * Returns obj.toString() inside angled brackets if obj is not null; otherwise returns
   * "null".  This function is used to format arguments of assert*() methods.  The angled
   * brackets make a value easier to find by a human, and help the reader know the
   * boundary of the value when it has leading or trailing whitespace.  Also, they allow the
   * reader to distinguish between a null object (formatted as "null") and a String whose
   * contents are "null" (formatted as "<null>").
   */
  private static String format(Object obj) {
    if (obj == null) {
      return "null";
    }
    return "<" + obj.toString() + ">";
  }

  /**
   * Utility class, no public constructor needed.
   */
  protected Assert() {
  }
}
