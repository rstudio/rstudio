package org.testng;

import org.testng.collections.Lists;

//import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Assertion tool class. Presents assertion methods with a more natural parameter order.
 * The order is always <B>actualValue</B>, <B>expectedValue</B> [, message].
 *
 * <p>
 * Modified by Google
 * <ul>
 * <li>Removed java.lang.reflect</li>
 * <li>Delegate fail messages to junit</li>
 * </ul>
 *
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class Assert {

  /**
   * Protect constructor since it is a static only class
   */
  protected Assert() {
    // hide constructor
  }

  /**
   * Asserts that a condition is true. If it isn't,
   * an AssertionError, with the given message, is thrown.
   * @param condition the condition to evaluate
   * @param message the assertion error message
   */
  static public void assertTrue(boolean condition, String message) {
    if(!condition) {
      failNotEquals( Boolean.valueOf(condition), Boolean.TRUE, message);
    }
  }

  /**
   * Asserts that a condition is true. If it isn't,
   * an AssertionError is thrown.
   * @param condition the condition to evaluate
   */
  static public void assertTrue(boolean condition) {
    assertTrue(condition, null);
  }

  /**
   * Asserts that a condition is false. If it isn't,
   * an AssertionError, with the given message, is thrown.
   * @param condition the condition to evaluate
   * @param message the assertion error message
   */
  static public void assertFalse(boolean condition, String message) {
    if(condition) {
      failNotEquals( Boolean.valueOf(condition), Boolean.FALSE, message); // TESTNG-81
    }
  }

  /**
   * Asserts that a condition is false. If it isn't,
   * an AssertionError is thrown.
   * @param condition the condition to evaluate
   */
  static public void assertFalse(boolean condition) {
    assertFalse(condition, null);
  }

  /**
   * Fails a test with the given message and wrapping the original exception.
   *
   * @param message the assertion error message
   * @param realCause the original exception
   */
  static public void fail(String message, Throwable realCause) {
    if(message == null){
      if (realCause != null){
        message = realCause.getMessage();
      }
    } else {
      message = message + ": " + realCause.getMessage();
    }
    junit.framework.Assert.fail(message);
  }

  /**
   * Fails a test with the given message.
   * @param message the assertion error message
   */
  static public void fail(String message) {
    junit.framework.Assert.fail(message);
  }

  /**
   * Fails a test with no message.
   */
  static public void fail() {
    fail(null);
  }

  /**
   * Asserts that two objects are equal. If they are not,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(Object actual, Object expected, String message) {
    if((expected == null) && (actual == null)) {
      return;
    }
    if(expected != null) {
      if (expected.getClass().isArray()) {
        assertArrayEquals(actual, expected, message);
        return;
      } else if (expected.equals(actual)) {
        return;
      }
    }
    failNotEquals(actual, expected, message);
  }

  /**
   * Asserts that two objects are equal. It they are not, an AssertionError,
   * with given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value (should be an non-null array value)
   * @param message the assertion error message
   */
  private static void assertArrayEquals(Object actual, Object expected, String message) {
    // is called only when expected is an array
    if (actual.getClass().isArray()) {
      if (actual instanceof Object[] && expected instanceof Object[]) {
        Object[] actualArray = (Object[]) actual;
        Object[] expectedArray = (Object[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            Object _actual = actualArray[i];
            Object _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
      else if (actual instanceof int[] && expected instanceof int[]) {
        int[] actualArray = (int[]) actual;
        int[] expectedArray = (int[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            int _actual = actualArray[i];
            int _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
      else if (actual instanceof float[] && expected instanceof float[]) {
        float[] actualArray = (float[]) actual;
        float[] expectedArray = (float[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            float _actual = actualArray[i];
            float _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
      else if (actual instanceof long[] && expected instanceof long[]) {
        long[] actualArray = (long[]) actual;
        long[] expectedArray = (long[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            long _actual = actualArray[i];
            long _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
      else if (actual instanceof double[] && expected instanceof double[]) {
        double[] actualArray = (double[]) actual;
        double[] expectedArray = (double[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            double _actual = actualArray[i];
            double _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
      else if (actual instanceof boolean[] && expected instanceof boolean[]) {
        boolean[] actualArray = (boolean[]) actual;
        boolean[] expectedArray = (boolean[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            boolean _actual = actualArray[i];
            boolean _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
      else if (actual instanceof byte[] && expected instanceof byte[]) {
        byte[] actualArray = (byte[]) actual;
        byte[] expectedArray = (byte[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            byte _actual = actualArray[i];
            byte _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
      else if (actual instanceof short[] && expected instanceof short[]) {
        short[] actualArray = (short[]) actual;
        short[] expectedArray = (short[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            short _actual = actualArray[i];
            short _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
      else if (actual instanceof char[] && expected instanceof char[]) {
        char[] actualArray = (char[]) actual;
        char[] expectedArray = (char[]) expected;
        int expectedLength = expectedArray.length;
        if (expectedLength == actualArray.length) {
          for (int i = 0; i < expectedLength; i++) {
            char _actual = actualArray[i];
            char _expected = expectedArray[i];
            try {
              assertEquals(_actual, _expected);
            } catch (AssertionError ae) {
              failArrayValuesAtIndexNotEqual(_actual, _expected, i, message);
            }
          }
          return;
        } else {
          failArrayLengthsNotEqual(actualArray.length, expectedLength, message);
        }
      }
    }
    failNotEquals(actual, expected, message);
  }

/**
   * Asserts that two objects are equal. If they are not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(Object actual, Object expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two Strings are equal. If they are not,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(String actual, String expected, String message) {
    assertEquals((Object) actual, (Object) expected, message);
  }

  /**
   * Asserts that two Strings are equal. If they are not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(String actual, String expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two doubles are equal concerning a delta.  If they are not,
   * an AssertionError, with the given message, is thrown.  If the expected
   * value is infinity then the delta value is ignored.
   * @param actual the actual value
   * @param expected the expected value
   * @param delta the absolute tolerate value value between the actual and expected value
   * @param message the assertion error message
   */
  static public void assertEquals(double actual, double expected, double delta, String message) {
    // handle infinity specially since subtracting to infinite values gives NaN and the
    // the following test fails
    if(Double.isInfinite(expected)) {
      if(!(expected == actual)) {
        failNotEquals(new Double(actual), new Double(expected), message);
      }
    }
    else if(!(Math.abs(expected - actual) <= delta)) { // Because comparison with NaN always returns false
      failNotEquals(new Double(actual), new Double(expected), message);
    }
  }

  /**
   * Asserts that two doubles are equal concerning a delta. If they are not,
   * an AssertionError is thrown. If the expected value is infinity then the
   * delta value is ignored.
   * @param actual the actual value
   * @param expected the expected value
   * @param delta the absolute tolerate value value between the actual and expected value
   */
  static public void assertEquals(double actual, double expected, double delta) {
    assertEquals(actual, expected, delta, null);
  }

  /**
   * Asserts that two floats are equal concerning a delta. If they are not,
   * an AssertionError, with the given message, is thrown.  If the expected
   * value is infinity then the delta value is ignored.
   * @param actual the actual value
   * @param expected the expected value
   * @param delta the absolute tolerate value value between the actual and expected value
   * @param message the assertion error message
   */
  static public void assertEquals(float actual, float expected, float delta, String message) {
    // handle infinity specially since subtracting to infinite values gives NaN and the
    // the following test fails
    if(Float.isInfinite(expected)) {
      if(!(expected == actual)) {
        failNotEquals(new Float(actual), new Float(expected), message);
      }
    }
    else if(!(Math.abs(expected - actual) <= delta)) {
      failNotEquals(new Float(actual), new Float(expected), message);
    }
  }

  /**
   * Asserts that two floats are equal concerning a delta. If they are not,
   * an AssertionError is thrown. If the expected
   * value is infinity then the delta value is ignored.
   * @param actual the actual value
   * @param expected the expected value
   * @param delta the absolute tolerate value value between the actual and expected value
   */
  static public void assertEquals(float actual, float expected, float delta) {
    assertEquals(actual, expected, delta, null);
  }

  /**
   * Asserts that two longs are equal. If they are not,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(long actual, long expected, String message) {
    assertEquals(Long.valueOf(actual), Long.valueOf(expected), message);
  }

  /**
   * Asserts that two longs are equal. If they are not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(long actual, long expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two booleans are equal. If they are not,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(boolean actual, boolean expected, String message) {
    assertEquals( Boolean.valueOf(actual), Boolean.valueOf(expected), message);
  }

  /**
   * Asserts that two booleans are equal. If they are not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(boolean actual, boolean expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two bytes are equal. If they are not,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(byte actual, byte expected, String message) {
    assertEquals(Byte.valueOf(actual), Byte.valueOf(expected), message);
  }

  /**
   * Asserts that two bytes are equal. If they are not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(byte actual, byte expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two chars are equal. If they are not,
   * an AssertionFailedError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(char actual, char expected, String message) {
    assertEquals(Character.valueOf(actual), Character.valueOf(expected), message);
  }

  /**
   * Asserts that two chars are equal. If they are not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(char actual, char expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two shorts are equal. If they are not,
   * an AssertionFailedError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(short actual, short expected, String message) {
    assertEquals(Short.valueOf(actual), Short.valueOf(expected), message);
  }

  /**
   * Asserts that two shorts are equal. If they are not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(short actual, short expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two ints are equal. If they are not,
   * an AssertionFailedError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(int actual,  int expected, String message) {
    assertEquals(Integer.valueOf(actual), Integer.valueOf(expected), message);
  }

  /**
   * Asserts that two ints are equal. If they are not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(int actual, int expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that an object isn't null. If it is,
   * an AssertionError is thrown.
   * @param object the assertion object
   */
  static public void assertNotNull(Object object) {
    assertNotNull(object, null);
  }

  /**
   * Asserts that an object isn't null. If it is,
   * an AssertionFailedError, with the given message, is thrown.
   * @param object the assertion object
   * @param message the assertion error message
   */
  static public void assertNotNull(Object object, String message) {
    assertTrue(object != null, message);
  }

  /**
   * Asserts that an object is null. If it is not,
   * an AssertionError, with the given message, is thrown.
   * @param object the assertion object
   */
  static public void assertNull(Object object) {
    assertNull(object, null);
  }

  /**
   * Asserts that an object is null. If it is not,
   * an AssertionFailedError, with the given message, is thrown.
   * @param object the assertion object
   * @param message the assertion error message
   */
  static public void assertNull(Object object, String message) {
    assertTrue(object == null, message);
  }

  /**
   * Asserts that two objects refer to the same object. If they do not,
   * an AssertionFailedError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertSame(Object actual, Object expected, String message) {
    if(expected == actual) {
      return;
    }
    failNotSame(actual, expected, message);
  }

  /**
   * Asserts that two objects refer to the same object. If they do not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertSame(Object actual, Object expected) {
    assertSame(actual, expected, null);
  }

  /**
   * Asserts that two objects do not refer to the same objects. If they do,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertNotSame(Object actual, Object expected, String message) {
    if(expected == actual) {
      failSame(actual, expected, message);
    }
  }

  /**
   * Asserts that two objects do not refer to the same object. If they do,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertNotSame(Object actual, Object expected) {
    assertNotSame(actual, expected, null);
  }

  static private void failArrayLengthsNotEqual(int actualLength, int expectedLength, 
      String message) {
    failNotEquals(actualLength, expectedLength, message == null ? "" : message
        + " (Array lengths are not the same)");
  }

  static private void failArrayValuesAtIndexNotEqual(Object actual, Object expected, int index, 
      String message) {
    failNotEquals(actual, expected, message == null ? "" : message
        + " (values as index " + index + " are not the same)");
  }

  static private void failSame(Object actual, Object expected, String message) {
    String formatted = "";
    if(message != null) {
      formatted = message + " ";
    }
    fail(formatted + "expected not same with:<" + expected +"> but was same:<" + actual + ">");
  }

  static private void failNotSame(Object actual, Object expected, String message) {
    String formatted = "";
    if(message != null) {
      formatted = message + " ";
    }
    fail(formatted + "expected same with:<" + expected + "> but was:<" + actual + ">");
  }

  static private void failNotEquals(Object actual , Object expected, String message ) {
    fail(format(actual, expected, message));
  }

  static String format(Object actual, Object expected, String message) {
    String formatted = "";
    if (null != message) {
      formatted = message + " ";
    }

    return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
  }

  /**
   * Asserts that two collections contain the same elements in the same order. If they do not,
   * an AssertionError is thrown.
   *
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(Collection actual, Collection expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two collections contain the same elements in the same order. If they do not,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(Collection actual, Collection expected, String message) {
    if(actual == expected) return;

    if (actual == null || expected == null) {
      if (message != null) fail(message);
      else fail("Collections not equal: expected: " + expected + " and actual: " + actual);
    }

    assertEquals(actual.size(), expected.size(), message + ": lists don't have the same size");

    Iterator actIt = actual.iterator();
    Iterator expIt = expected.iterator();
    int i = -1;
    while(actIt.hasNext() && expIt.hasNext()) {
      i++;
      Object e = expIt.next();
      Object a = actIt.next();
      String errorMessage = message == null
          ? "Lists differ at element [" + i + "]: " + e + " != " + a
          : message + ": Lists differ at element [" + i + "]: " + e + " != " + a;

      assertEquals(a, e, errorMessage);
    }
  }

  /**
   * Asserts that two arrays contain the same elements in the same order. If they do not,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(Object[] actual, Object[] expected, String message) {
    if(actual == expected) return;

    if ((actual == null && expected != null) || (actual != null && expected == null)) {
      if (message != null) fail(message);
      else fail("Arrays not equal: " + Arrays.toString(expected) + " and " + Arrays.toString(actual));
    }
    assertEquals(Arrays.asList(actual), Arrays.asList(expected), message);
  }

  /**
   * Asserts that two arrays contain the same elements in no particular order. If they do not,
   * an AssertionError, with the given message, is thrown.
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEqualsNoOrder(Object[] actual, Object[] expected, String message) {
    if(actual == expected) return;

    if ((actual == null && expected != null) || (actual != null && expected == null)) {
      failAssertNoEqual(actual, expected,
          "Arrays not equal: " + Arrays.toString(expected) + " and " + Arrays.toString(actual),
          message);
    }

    if (actual.length != expected.length) {
      failAssertNoEqual(actual, expected,
          "Arrays do not have the same size:" + actual.length + " != " + expected.length,
          message);
    }

    List actualCollection = Lists.newArrayList();
    for (Object a : actual) {
      actualCollection.add(a);
    }
    for (Object o : expected) {
      actualCollection.remove(o);
    }
    if (actualCollection.size() != 0) {
      failAssertNoEqual(actual, expected,
          "Arrays not equal: " + Arrays.toString(expected) + " and " + Arrays.toString(actual),
          message);
    }
  }

  private static void failAssertNoEqual(Object[] actual, Object[] expected,
      String message, String defaultMessage)
  {
    if (message != null) fail(message);
    else fail(defaultMessage);
  }

  /**
   * Asserts that two arrays contain the same elements in the same order. If they do not,
   * an AssertionError is thrown.
   *
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(Object[] actual, Object[] expected) {
    assertEquals(actual, expected, null);
  }

  /**
   * Asserts that two arrays contain the same elements in no particular order. If they do not,
   * an AssertionError is thrown.
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEqualsNoOrder(Object[] actual, Object[] expected) {
    assertEqualsNoOrder(actual, expected, null);
  }

  /**
   * Asserts that two arrays contain the same elements in the same order. If they do not,
   * an AssertionError is thrown.
   *
   * @param actual the actual value
   * @param expected the expected value
   */
  static public void assertEquals(final byte[] actual, final byte[] expected) {
    assertEquals(actual, expected, "");
  }

  /**
   * Asserts that two arrays contain the same elements in the same order. If they do not,
   * an AssertionError, with the given message, is thrown.
   *
   * @param actual the actual value
   * @param expected the expected value
   * @param message the assertion error message
   */
  static public void assertEquals(final byte[] actual, final byte[] expected, final String message) {
    if(expected == actual) {
      return;
    }
    if(null == expected) {
      fail("expected a null array, but not null found. " + message);
    }
    if(null == actual) {
      fail("expected not null array, but null found. " + message);
    }

    assertEquals(actual.length, expected.length, "arrays don't have the same size. " + message);

    for(int i= 0; i < expected.length; i++) {
      if(expected[i] != actual[i]) {
        fail("arrays differ firstly at element [" + i +"]; "
            + "expected value is <" + expected[i] +"> but was <"
            + actual[i] + ">. "
            + message);
      }
    }
  }

  /**
   * Asserts that two sets are equal.
   */
  static public void assertEquals(Set actual, Set expected) {
    if(actual == expected) return;

    if (actual == null || expected == null) {
      fail("Sets not equal: expected: " + expected + " and actual: " + actual);
    }

    if (!actual.equals(expected)) {
      fail("Sets differ: expected " + expected + " but got " + actual);
    }
  }

  /**
   * Asserts that two maps are equal.
   */
  static public void assertEquals(Map actual, Map expected) {
    if(actual == expected) return;

    if (actual == null || expected == null) {
      fail("Maps not equal: expected: " + expected + " and actual: " + actual);
    }

    if (!actual.equals(expected)) {
      fail("Maps differ: expected " + expected + " but got " + actual);
    }
  }

}
