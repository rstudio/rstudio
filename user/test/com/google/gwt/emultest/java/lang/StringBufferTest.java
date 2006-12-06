// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * This class tests StringBuffer.
 */
public class StringBufferTest extends GWTTestCase {

  public StringBufferTest() {
    testAppend();
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
   * Tests correctness under repeated insertion and append.
   */
  public void testRepeatedAppendsAndInserts() {
    StringBuffer x = new StringBuffer();
    final int SIZE = 1000;
    for (int i = 0; i < SIZE; i++) {
      x.append("" + i % 10);
    }
    assertTrue("endwith1", x.toString().endsWith("0123456789"));
    assertTrue("startswith1", x.toString().startsWith("0123456789"));
    assertEquals(x.length(), SIZE);
    x = new StringBuffer();
    for (int i = 0; i < SIZE * 4; i++) {
      x.append("" + i % 10);
    }
    assertTrue("endswith2", x.toString().endsWith("0123456789"));
    assertTrue("startswith2", x.toString().startsWith("0123456789"));
    assertEquals("length2", x.length(), SIZE * 4);
    x = new StringBuffer();
    for (int i = 0; i < SIZE; i++) {
      x.insert(0, "" + i % 10);
    }
    assertTrue("endswith3", x.toString().endsWith("9876543210"));
    assertTrue("startswith3", x.toString().startsWith("9876543210"));
    assertEquals("length3", x.length(), SIZE);
    x = new StringBuffer();
    for (int i = 0; i < SIZE * 4; i++) {
      x.insert(0, "" + i % 10);
    }
    assertTrue("endswith4", x.toString().endsWith("9876543210"));
    assertTrue("startswith4", x.toString().startsWith("9876543210"));
    assertEquals("size4", x.length(), SIZE * 4);
  }

  /**
   * This method tests string creation and equality.
   */
  public void testContructor() {
    String constant = "abcdef";
    assertEquals(new StringBuffer(constant).toString(), constant);
    assertEquals(new StringBuffer().toString(), "");
  }

  /**
   * This method tests <code>ubstring</code>.
   */
  public void testSubstring() {
    StringBuffer haystack = new StringBuffer("abcdefghi");
    assertEquals("cd", haystack.substring(2, 4));
    assertEquals("bc", "abcdef".substring(1, 3));
    assertEquals("bcdef", "abcdef".substring(1));
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
    x = new StringBuffer("!");
    x.insert(1, C.FALSE_VALUE);
    assertEquals("!" + C.FALSE_STRING, x.toString());
    x = new StringBuffer("!");
    x.insert(1, C.TRUE_VALUE);
    assertEquals("!" + C.TRUE_STRING, x.toString());
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
   * This method tests <code>lastIndexOf</code>.
   */
  public void testLastIndexOf() {
    StringBuffer x = new StringBuffer("abcdeabcdef");
    assertEquals(9, x.lastIndexOf("e"));
    assertEquals(10, x.lastIndexOf("f"));
    assertEquals(-1, x.lastIndexOf("f", 1));
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
   * This method tests <code>charAt</code>.
   */
  public void testCharAt() {
    assertEquals(new StringBuffer("abc").charAt(1), 'b');
  }

  /** tests toCharArray */
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
   * This method gets the module name.
   * 
   * @return the module name.
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }


}