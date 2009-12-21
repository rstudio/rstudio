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
package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for the Javascript emulation of the Integer/int autoboxed
 * fundamental type.
 */
public class IntegerTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testBadStrings() {
    try {
      new Integer("05abcd");
      fail("Constructor should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.decode("05abcd");
      fail("Decode should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.parseInt("05abcd");
      fail("parseInt should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.parseInt(String.valueOf(Long.MAX_VALUE));
      fail("parseInt should reject numbers greater than the range of int");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.parseInt(String.valueOf(Long.MIN_VALUE));
      fail("parseInt should reject numbers less than the range of int");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.parseInt(String.valueOf((long) Integer.MAX_VALUE + 1));
      fail("parseInt should reject numbers greater than the range of int");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.parseInt(String.valueOf((long) Integer.MIN_VALUE - 1));
      fail("parseInt should reject numbers less than the range of int");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.parseInt("-");
      fail("parseInt should reject \"-\"");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.parseInt(" -12345");
      fail("parseInt should reject leading whitespace");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Integer.parseInt("-12345 ");
      fail("parseInt should reject trailing whitespace");
    } catch (NumberFormatException e) {
      // Expected behavior
    }
  }

  public void testBinaryString() {
    assertEquals("11000000111001", Integer.toBinaryString(12345));
    assertEquals("0", Integer.toBinaryString(0));
    assertEquals("11111111111111111100111111000111",
        Integer.toBinaryString(-12345));
  }

  public void testBitCount() {
    assertEquals(0, Integer.bitCount(0));
    assertEquals(1, Integer.bitCount(1));
    assertEquals(32, Integer.bitCount(-1));
    assertEquals(31, Integer.bitCount(Integer.MAX_VALUE));
    assertEquals(1, Integer.bitCount(Integer.MIN_VALUE));
  }

  public void testCompareTo() {
    assertEquals(-1, new Integer(12345).compareTo(new Integer(12346)));
    assertEquals(1, new Integer("12345").compareTo(new Integer(12344)));
    assertEquals(0, new Integer("12345").compareTo(new Integer(12345)));
  }

  public void testConstants() {
    assertEquals(32, Integer.SIZE);
    assertEquals(0x7fffffff, Integer.MAX_VALUE);
    assertEquals(0x80000000, Integer.MIN_VALUE);
  }

  public void testConstructor() {
    assertEquals(12345, new Integer(12345).intValue());
    assertEquals(12345, new Integer("12345").intValue());
  }

  public void testDecode() {
    assertEquals(Integer.MAX_VALUE, Integer.decode(
        String.valueOf(Integer.MAX_VALUE)).intValue());
    assertEquals(Integer.MIN_VALUE, Integer.decode(
        String.valueOf(Integer.MIN_VALUE)).intValue());
    assertEquals(12345, Integer.decode("12345").intValue());
    assertEquals(31, Integer.decode("0x1f").intValue());
    assertEquals(-31, Integer.decode("-0X1F").intValue());
    assertEquals(31, Integer.decode("#1f").intValue());
    assertEquals(10, Integer.decode("012").intValue());
    try {
      Integer.decode("abx");
      fail();
    } catch (NumberFormatException e) {
      // pass
    }
  }

  public void testEquals() {
    assertFalse(new Integer(12345).equals(new Integer(12346)));
    assertEquals(new Integer("12345"), new Integer(12345));
  }

  public void testHashCode() {
    assertEquals(1234, new Integer(1234).hashCode());
  }

  public void testHighestOneBit() {
    assertEquals(0, Integer.highestOneBit(0));
    assertEquals(Integer.MIN_VALUE, Integer.highestOneBit(-1));
    assertEquals(Integer.MIN_VALUE, Integer.highestOneBit(-256));
    assertEquals(1, Integer.highestOneBit(1));
    assertEquals(0x80, Integer.highestOneBit(0x88));
    assertEquals(0x40000000, Integer.highestOneBit(Integer.MAX_VALUE));
  }

  public void testLowestOneBit() {
    assertEquals(0, Integer.lowestOneBit(0));
    assertEquals(1, Integer.lowestOneBit(-1));
    assertEquals(0x100, Integer.lowestOneBit(-256));
    assertEquals(1, Integer.lowestOneBit(1));
    assertEquals(0x80, Integer.lowestOneBit(0x880));
    assertEquals(0x80000000, Integer.lowestOneBit(Integer.MIN_VALUE));
  }

  public void testNumberOfLeadingZeros() {
    assertEquals(32, Integer.numberOfLeadingZeros(0));
    assertEquals(31, Integer.numberOfLeadingZeros(1));
    assertEquals(0, Integer.numberOfLeadingZeros(-1));
    assertEquals(16, Integer.numberOfLeadingZeros(0x8000));
    assertEquals(1, Integer.numberOfLeadingZeros(Integer.MAX_VALUE));
    assertEquals(0, Integer.numberOfLeadingZeros(Integer.MIN_VALUE));
    assertEquals(0, Integer.numberOfLeadingZeros(-0x8000));
  }

  public void testNumberOfTrailingZeros() {
    assertEquals(32, Integer.numberOfTrailingZeros(0));
    assertEquals(0, Integer.numberOfTrailingZeros(1));
    assertEquals(0, Integer.numberOfTrailingZeros(-1));
    assertEquals(15, Integer.numberOfTrailingZeros(0x8000));
    assertEquals(0, Integer.numberOfTrailingZeros(Integer.MAX_VALUE));
    assertEquals(31, Integer.numberOfTrailingZeros(Integer.MIN_VALUE));
    assertEquals(4, Integer.numberOfTrailingZeros(-0x7ff0));
  }

  public void testReverse() {
    assertEquals(0, Integer.reverse(0));
    assertEquals(-1, Integer.reverse(-1));
    assertEquals(Integer.MIN_VALUE, Integer.reverse(1));
    assertEquals(1, Integer.reverse(Integer.MIN_VALUE));
    assertEquals(0xaaaaaaaa, Integer.reverse(0x55555555));
    assertEquals(0xaaaa0000, Integer.reverse(0x00005555));
    assertEquals(0xaa00aa00, Integer.reverse(0x00550055));
    assertEquals(0x55555555, Integer.reverse(0xaaaaaaaa));
    assertEquals(0x00005555, Integer.reverse(0xaaaa0000));
    assertEquals(0x00550055, Integer.reverse(0xaa00aa00));
  }

  public void testReverseBytes() {
    assertEquals(0, Integer.reverseBytes(0));
    // two-complement bugs?
    assertEquals(0x84218421, Integer.reverseBytes(0x21842184));
    assertEquals(0x12481248, Integer.reverseBytes(0x48124812));
    assertEquals(0x21436587, Integer.reverseBytes(0x87654321));
  }

  public void testRotateLeft() {
    assertEquals(0, Integer.rotateLeft(0, 4));
    assertEquals(0x2, Integer.rotateLeft(1, 1));
    assertEquals(0x10, Integer.rotateLeft(1, 4));
    assertEquals(-1, Integer.rotateLeft(-1, 4));
    assertEquals(Integer.MIN_VALUE, Integer.rotateLeft(0x40000000, 1));
    assertEquals(1, Integer.rotateLeft(Integer.MIN_VALUE, 1));
  }

  public void testRotateRight() {
    assertEquals(0, Integer.rotateRight(0, 4));
    assertEquals(Integer.MIN_VALUE, Integer.rotateRight(1, 1));
    assertEquals(0x10000000, Integer.rotateRight(1, 4));
    assertEquals(-1, Integer.rotateRight(-1, 4));
  }

  public void testSignum() {
    assertEquals(0, Integer.signum(0));
    assertEquals(1, Integer.signum(1));
    assertEquals(-1, Integer.signum(-1));
    assertEquals(1, Integer.signum(Integer.MAX_VALUE));
    assertEquals(-1, Integer.signum(Integer.MIN_VALUE));
  }

  public void testStaticValueOf() {
    assertEquals(Integer.MIN_VALUE,
        Integer.valueOf(Integer.MIN_VALUE).intValue());
    assertEquals(Integer.MAX_VALUE,
        Integer.valueOf(Integer.MAX_VALUE).intValue());
  }

  public void testToHexString() {
    assertEquals("12345", Integer.toHexString(0x12345));
    assertEquals("fff12345", Integer.toHexString(0xFFF12345));
  }

  public void testToString() {
    assertEquals("12345", new Integer(12345).toString());
    assertEquals("-12345", new Integer("-12345").toString());
  }

  public void testValueOf() {
    assertEquals(new Integer(12345), Integer.valueOf("12345"));
    assertEquals(new Integer(1865), Integer.valueOf("12345", 6));
    assertEquals(12345, Integer.parseInt("12345"));
    assertEquals(1865, Integer.parseInt("12345", 6));
  }

  public void testXValue() {
    assertEquals("short", (short) 12345, new Integer(12345).shortValue());
    assertEquals("long", 1234567890L, new Integer(1234567890).longValue());
    assertEquals("double", 12345d, new Integer(12345).doubleValue(), 0.001);
    assertEquals("float", 12345f, new Integer(12345).floatValue(), 0.01);
    assertEquals("byte", (byte) 123, new Integer(123).byteValue());
    assertEquals("integer", 123, new Integer(123).intValue());
    assertEquals("short overflow", (short) 10713,
        new Integer(1234512345).shortValue());
    assertEquals("double2", 1234512345d, new Integer(1234512345).doubleValue(),
        0.001);
    // Invalid test right now; we don't coerce to single precision
    // assertEquals("float2",1234512345f, new
    // Integer(1234512345).floatValue(),0.001);
    assertEquals("byte overflow", (byte) -13, new Integer(123123).byteValue());
  }
}
