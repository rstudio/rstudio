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
package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * This class tests classes StringBuffer and StringBuilder.
 */
public class StringBufferTest extends GWTTestCase {
  /**
   * This method gets the module name.
   * 
   * @return the module name.
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * This method tests <code>append</code>.
   */
  public void testAppend() {
    StringBuffer x = new StringBuffer();
    x.append(C.FLOAT_VALUE);
    assertTrue(x.toString().startsWith(C.FLOAT_STRING));
    x = new StringBuffer();
    x.append(C.INT_VALUE);
    assertEquals(C.INT_STRING, x.toString());
    x = new StringBuffer();
    x.append(C.LONG_VALUE);
    assertTrue(x.toString().startsWith(C.LONG_STRING));
    x = new StringBuffer();
    x.append(C.DOUBLE_VALUE);
    assertTrue(x.toString().startsWith(C.DOUBLE_STRING));
    x = new StringBuffer();
    x.append(C.CHAR_VALUE);
    assertEquals(C.CHAR_STRING, x.toString());
    x = new StringBuffer();
    x.append(C.CHAR_ARRAY_VALUE);
    assertEquals(C.CHAR_ARRAY_STRING, x.toString());
    x = new StringBuffer();
    x.append(C.CHAR_ARRAY_VALUE, 1, 4);
    assertEquals(C.CHAR_ARRAY_STRING.substring(1, 5), x.toString());
    x = new StringBuffer();
    x.append(C.FALSE_VALUE);
    assertEquals(C.FALSE_STRING, x.toString());
    x = new StringBuffer();
    x.append(C.TRUE_VALUE);
    assertEquals(C.TRUE_STRING, x.toString());
    x = new StringBuffer();
    x.append((String) null);
    assertEquals("null", x.toString());
    x = new StringBuffer();
    x.append((CharSequence) "abc");
    assertEquals("abc", x.toString());
    x = new StringBuffer();
    x.append("abcde", 2, 3);
    assertEquals("c", x.toString());
  }

  /**
   * Check that capacity methods are present, even though they do nothing.
   */
  public void testCapacity() {
    StringBuffer buf = new StringBuffer();
    buf.ensureCapacity(100);
    assertTrue(buf.capacity() >= 0);
    buf.trimToSize();
  }

  /**
   * This method tests <code>charAt</code>.
   */
  public void testCharAt() {
    assertEquals(new StringBuffer("abc").charAt(1), 'b');
  }

  /**
   * This method tests string creation and equality.
   */
  public void testContructor() {
    String constant = "abcdef";
    assertEquals(new StringBuffer(constant).toString(), constant);
    assertEquals(new StringBuffer().toString(), "");
    assertEquals(new StringBuffer((CharSequence) constant).toString(), constant);
  }

  /**
   * This method tests <code>delete</code>.
   */
  public void testDelete() {
    StringBuffer haystack = new StringBuffer("abcdefghi");
    haystack.delete(2, 4);
    assertEquals(haystack.toString(), "abefghi");
    haystack.deleteCharAt(0);
    assertEquals(haystack.toString(), "befghi");
    haystack.deleteCharAt(1);
    assertEquals(haystack.toString(), "bfghi");
  }

  /**
   * Tests toCharArray.
   */
  public void testGetChars() {
    StringBuffer x = new StringBuffer("ABCDEFGHIJ");
    char[] a1 = "abcdefghij".toCharArray();
    char[] desired = "abcDEFghij".toCharArray();
    x.getChars(3, 6, a1, 3);
    for (int i = 0; i < a1.length; i++) {
      assertEquals(a1[i], desired[i]);
    }
  }

  /**
   * This method tests <code>indexOf</code>.
   */
  public void testIndexOf() {
    String haystack = "abcdefghi";
    assertEquals(haystack.indexOf("q"), -1);
    assertEquals(haystack.indexOf('q'), -1);
    assertEquals(haystack.indexOf("a"), 0);
    assertEquals(haystack.indexOf('a'), 0);
    assertEquals(haystack.indexOf('a', 1), -1);
    assertEquals(haystack.indexOf("bc"), 1);
    assertEquals(haystack.indexOf(""), 0);
  }

  /**
   * This method tests <code>insert</code>.
   */
  public void testInsert() {
    StringBuffer x = new StringBuffer("!");
    x.insert(1, C.FLOAT_VALUE);
    assertTrue(x.toString().startsWith("!" + C.FLOAT_STRING));
    x = new StringBuffer("!");
    x.insert(1, C.INT_VALUE);
    assertEquals("!" + C.INT_STRING, x.toString());
    x = new StringBuffer("!");
    x.insert(1, C.LONG_VALUE);
    assertEquals("!" + C.LONG_STRING, x.toString());
    x = new StringBuffer("!");
    x.insert(1, C.DOUBLE_VALUE);
    assertTrue(x.toString().startsWith("!" + C.DOUBLE_STRING));
    x = new StringBuffer("!");
    x.insert(1, C.CHAR_VALUE);
    assertEquals("!" + C.CHAR_STRING, x.toString());
    x = new StringBuffer("!");
    x.insert(1, C.CHAR_ARRAY_VALUE);
    assertEquals("!" + C.CHAR_ARRAY_STRING, x.toString());
    x = new StringBuffer("!");
    x.insert(1, C.CHAR_ARRAY_VALUE, 1, 4);
    assertEquals("!" + C.CHAR_ARRAY_STRING.substring(1, 5), x.toString());
    x = new StringBuffer("01234");
    x.insert(2, (CharSequence) "abcde");
    assertEquals("01abcde234", x.toString());
    x = new StringBuffer("01234");
    x.insert(2, "abcde", 2, 4);
    assertEquals("01cd234", x.toString());
    x = new StringBuffer("!");
    x.insert(1, C.FALSE_VALUE);
    assertEquals("!" + C.FALSE_STRING, x.toString());
    x = new StringBuffer("!");
    x.insert(1, C.TRUE_VALUE);
    assertEquals("!" + C.TRUE_STRING, x.toString());
  }

  /**
   * This method does interleaved inserts and deletes.
   */
  public void testInterleavedInsertAndDelete() {
    StringBuffer x = new StringBuffer();
    for (int i = 0; i < 9; i++) {
      x.append("1234567890");
    }
    for (int i = 0; i < 10; i++) {
      x.delete(5, 15);
    }
    assertEquals(x.toString(), "12345");
  }

  /**
   * This method tests <code>lastIndexOf</code>.
   */
  public void testLastIndexOf() {
    StringBuffer x = new StringBuffer("abcdeabcdef");
    assertEquals(9, x.lastIndexOf("e"));
    assertEquals(10, x.lastIndexOf("f"));
    assertEquals(-1, x.lastIndexOf("f", 1));
  }

  /**
   * This method tests <code>length</code>, and tests moderately long
   * StringBuffers.
   */
  public void testLength() {
    assertEquals(3, new StringBuffer("abc").length());
    StringBuffer str = new StringBuffer("x");
    for (int i = 0; i < 16; i++) {
      str.append(str);
    }
    assertEquals(1 << 16, str.length());
  }

  /**
   * This method tests <code>toLowerCase</code>.
   */
  public void testLowerCase() {
    assertEquals("abc", "AbC".toLowerCase());
    assertEquals("abc", "abc".toLowerCase());
    assertEquals("", "".toLowerCase());
  }

  /**
   * Tests correctness under repeated insertion and append.
   */
  public void testRepeatedAppendsAndInserts() {
    StringBuffer x = new StringBuffer();
    final int size = 1000;
    for (int i = 0; i < size; i++) {
      x.append("" + i % 10);
    }
    assertTrue("endwith1", x.toString().endsWith("0123456789"));
    assertTrue("startswith1", x.toString().startsWith("0123456789"));
    assertEquals(x.length(), size);
    x = new StringBuffer();
    for (int i = 0; i < size * 4; i++) {
      x.append("" + i % 10);
    }
    assertTrue("endswith2", x.toString().endsWith("0123456789"));
    assertTrue("startswith2", x.toString().startsWith("0123456789"));
    assertEquals("length2", x.length(), size * 4);
    x = new StringBuffer();
    for (int i = 0; i < size; i++) {
      x.insert(0, "" + i % 10);
    }
    assertTrue("endswith3", x.toString().endsWith("9876543210"));
    assertTrue("startswith3", x.toString().startsWith("9876543210"));
    assertEquals("length3", x.length(), size);
    x = new StringBuffer();
    for (int i = 0; i < size * 4; i++) {
      x.insert(0, "" + i % 10);
    }
    assertTrue("endswith4", x.toString().endsWith("9876543210"));
    assertTrue("startswith4", x.toString().startsWith("9876543210"));
    assertEquals("size4", x.length(), size * 4);
  }

  /**
   * This method tests <code>replace</code>.
   */
  public void testReplace() {
    StringBuffer x = new StringBuffer("xxyyxx");
    x.replace(2, 4, "YY");
    assertEquals("xxYYxx", x.toString());
  }

  /**
   * This method tests <code>setLength</code>.
   */
  public void testSetLength() {
    StringBuffer x = new StringBuffer("abcdefghi");
    x.setLength(20);
    assertEquals(x.length(), 20);
    assertEquals(x.toString(), x.toString(), "abcdefghi\0\0\0\0\0\0\0\0\0\0\0");
    x.setLength(5);
    assertEquals(x.length(), 5);
    assertEquals(x.toString(), "abcde");
  }

  /**
   * This method tests <code>startsWith</code>.
   */
  public void testStartsWith() {
    String haystack = "abcdefghi";
    assertTrue(haystack.startsWith("abc"));
    assertTrue(haystack.startsWith("bc", 1));
    assertTrue(haystack.startsWith(haystack));
    assertFalse(haystack.startsWith(haystack + "j"));
  }

  /**
   * A smoke test that StringBuilder's methods are available and basically work.
   * The implementation is currently shared with StringBuffer, so all the tricky
   * test cases are not repeated.
   */
  public void testStringBuilder() {
    StringBuilder bld = new StringBuilder();
    bld = new StringBuilder(100);
    bld = new StringBuilder("abc");
    assertEquals("abc", bld.toString());

    bld = new StringBuilder((CharSequence) "abc");
    assertEquals("abc", bld.toString());

    bld = new StringBuilder();
    bld.append(true);
    assertEquals("true", bld.toString());

    bld = new StringBuilder();
    bld.append('a');
    assertEquals("a", bld.toString());

    bld = new StringBuilder();
    char[] abcde = {'a', 'b', 'c', 'd', 'e'};
    bld.append(abcde);
    assertEquals("abcde", bld.toString());

    bld = new StringBuilder();
    bld.append(abcde, 1, 3);
    assertEquals("bcd", bld.toString());

    bld = new StringBuilder();
    bld.append((CharSequence) "abcde");
    assertEquals("abcde", bld.toString());

    bld = new StringBuilder();
    bld.append("abcde", 2, 4);
    assertEquals("cd", bld.toString());

    bld = new StringBuilder();
    bld.append(1.5);
    assertEquals("1.5", bld.toString());

    bld = new StringBuilder();
    bld.append(1.5F);
    assertEquals("1.5", bld.toString());

    bld = new StringBuilder();
    bld.append(5);
    assertEquals("5", bld.toString());

    bld = new StringBuilder();
    bld.append(5L);
    assertEquals("5", bld.toString());

    bld = new StringBuilder();
    bld.append(new Object() {
      @Override
      public String toString() {
        return "obj";
      }
    });
    assertEquals("obj", bld.toString());

    bld = new StringBuilder();
    bld.append("abc");
    assertEquals("abc", bld.toString());

    bld = new StringBuilder();
    bld.append(new StringBuffer("abc"));
    assertEquals("abc", bld.toString());

    bld = new StringBuilder();
    bld.append("abcde");
    assertEquals('c', bld.charAt(2));

    bld = new StringBuilder();
    bld.append("abcde");
    bld.delete(1, 2);
    assertEquals("acde", bld.toString());

    bld = new StringBuilder();
    bld.append("abcde");
    bld.deleteCharAt(2);
    assertEquals("abde", bld.toString());

    // check that capacity methods are present
    bld = new StringBuilder();
    assertTrue(bld.capacity() >= 0);
    bld.ensureCapacity(100);
    bld.trimToSize();

    bld = new StringBuilder();
    bld.append("abcde");
    char[] chars = {'0', '0', '0', '0', '0'};
    bld.getChars(2, 4, chars, 2);
    assertEquals('0', chars[0]);
    assertEquals('0', chars[1]);
    assertEquals('c', chars[2]);
    assertEquals('d', chars[3]);
    assertEquals('0', chars[4]);

    bld = new StringBuilder("01234");
    assertEquals(2, bld.indexOf("23"));

    bld = new StringBuilder();
    bld.append("0123401234");
    assertEquals(5, bld.indexOf("0123", 1));

    bld = new StringBuilder("01234");
    bld.insert(2, true);
    assertEquals("01true234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, 'X');
    assertEquals("01X234", bld.toString());

    bld = new StringBuilder("01234");
    char[] chars2 = {'a', 'b', 'c', 'd', 'e'};
    bld.insert(2, chars2, 3, 2);
    assertEquals("01de234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, (CharSequence) "abcde");
    assertEquals("01abcde234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, "abcde", 2, 4);
    assertEquals("01cd234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, 1.5);
    assertEquals("011.5234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, 1.5F);
    assertEquals("011.5234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, 99);
    assertEquals("0199234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, 99L);
    assertEquals("0199234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, new Object() {
      @Override
      public String toString() {
        return "obj";
      }
    });
    assertEquals("01obj234", bld.toString());

    bld = new StringBuilder("01234");
    bld.insert(2, "XX");
    assertEquals("01XX234", bld.toString());

    bld = new StringBuilder("0123401234");
    assertEquals(5, bld.lastIndexOf("0123"));

    bld = new StringBuilder("0123401234");
    assertEquals(0, bld.lastIndexOf("0123", 4));

    bld = new StringBuilder("01234");
    assertEquals(5, bld.length());

    bld = new StringBuilder("01234");
    bld.replace(2, 3, "XYZ");
    assertEquals("01XYZ34", bld.toString());

    bld = new StringBuilder("01234");
    bld.setCharAt(2, 'X');
    assertEquals("01X34", bld.toString());

    bld = new StringBuilder("01234");
    bld.setLength(2);
    assertEquals("01", bld.toString());

    bld = new StringBuilder("01234");
    assertEquals("23", bld.subSequence(2, 4));

    bld = new StringBuilder("01234");
    assertEquals("234", bld.substring(2));

    bld = new StringBuilder("01234");
    assertEquals("23", bld.substring(2, 4));
  }

  /**
   * This method tests <code>substring</code>.
   */
  public void testSubstring() {
    StringBuffer haystack = new StringBuffer("abcdefghi");
    assertEquals("cd", haystack.substring(2, 4));
    assertEquals("bc", "abcdef".substring(1, 3));
    assertEquals("bcdef", "abcdef".substring(1));
  }
}
