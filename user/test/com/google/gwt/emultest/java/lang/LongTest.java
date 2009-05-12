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
 * TODO: document me.
 */
public class LongTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testBinaryString() {
    assertEquals("10001111101101101111011110001100100000000",
        Long.toBinaryString(1234500000000L));
    assertEquals("0", Long.toBinaryString(0L));
    assertEquals(
        "1111111111111111111111101110000010010010000100001110011100000000",
        Long.toBinaryString(-1234500000000L));
  }

  public void testBitCount() {
    assertEquals(0, Long.bitCount(0));
    assertEquals(1, Long.bitCount(1));
    assertEquals(64, Long.bitCount(-1));
    assertEquals(63, Long.bitCount(Long.MAX_VALUE));
    assertEquals(1, Long.bitCount(Long.MIN_VALUE));
  }

  public void testConstants() {
    assertEquals(64, Long.SIZE);
    assertEquals(0x7fffffffffffffffL, Long.MAX_VALUE);
    assertEquals(0x8000000000000000L, Long.MIN_VALUE);
  }

  public void testHighestOneBit() {
    assertEquals(0, Long.highestOneBit(0));
    assertEquals(Long.MIN_VALUE, Long.highestOneBit(-1));
    assertEquals(Long.MIN_VALUE, Long.highestOneBit(-256));
    assertEquals(1, Long.highestOneBit(1));
    assertEquals(0x80, Long.highestOneBit(0x88));
    assertEquals(0x4000000000000000L, Long.highestOneBit(Long.MAX_VALUE));
  }

  public void testLowestOneBit() {
    assertEquals(0, Long.lowestOneBit(0));
    assertEquals(1, Long.lowestOneBit(-1));
    assertEquals(0x100, Long.lowestOneBit(-256));
    assertEquals(1, Long.lowestOneBit(1));
    assertEquals(0x80, Long.lowestOneBit(0x780));
    assertEquals(Long.MIN_VALUE, Long.lowestOneBit(Long.MIN_VALUE));
  }

  public void testNumberOfLeadingZeros() {
    assertEquals(64, Long.numberOfLeadingZeros(0));
    assertEquals(63, Long.numberOfLeadingZeros(1));
    assertEquals(0, Long.numberOfLeadingZeros(-1));
    assertEquals(32, Long.numberOfLeadingZeros(0x80000000L));
    assertEquals(1, Long.numberOfLeadingZeros(Long.MAX_VALUE));
    assertEquals(0, Long.numberOfLeadingZeros(Long.MIN_VALUE));
    assertEquals(0, Long.numberOfLeadingZeros(-0x80000000L));
  }

  public void testNumberOfTrailingZeros() {
    assertEquals(64, Long.numberOfTrailingZeros(0));
    assertEquals(0, Long.numberOfTrailingZeros(1));
    assertEquals(0, Long.numberOfTrailingZeros(-1));
    assertEquals(31, Long.numberOfTrailingZeros(0x80000000L));
    assertEquals(0, Long.numberOfTrailingZeros(Long.MAX_VALUE));
    assertEquals(63, Long.numberOfTrailingZeros(Long.MIN_VALUE));
    assertEquals(20, Long.numberOfTrailingZeros(-0x7ff00000L));
  }
 
  public void testParse() {
    assertEquals(0L, Long.parseLong("0"));
    assertEquals(100000000000L, Long.parseLong("100000000000"));
    assertEquals(-100000000000L, Long.parseLong("-100000000000"));
    assertEquals(10L, Long.parseLong("010"));
    try {
      Long.parseLong("10L");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      Long.parseLong("");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      // Issue 2636
      new Long("");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      // issue 3647
      Long.parseLong("-");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      // issue 3647
      new Long("-");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      Long.parseLong(" -");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      new Long(" -");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      Long.parseLong("- ");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      new Long("- ");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      Long.parseLong("deadbeefbeef");
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      Long.parseLong("123456789ab123456789ab123456789ab123456789ab", 12);
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }

    assertEquals(0L, Long.parseLong("0", 12));
    assertEquals(73686780563L, Long.parseLong("123456789ab", 12));
    assertEquals(-73686780563L, Long.parseLong("-123456789ab", 12));
    try {
      Long.parseLong("c", 12);
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }

    assertEquals(0L, Long.parseLong("0", 16));
    assertEquals(-1L, Long.parseLong("-1", 16));
    assertEquals(1L, Long.parseLong("1", 16));
    assertEquals(0xdeadbeefdeadL, Long.parseLong("deadbeefdead", 16));
    assertEquals(-0xdeadbeefdeadL, Long.parseLong("-deadbeefdead", 16));
    try {
      Long.parseLong("deadbeefdeadbeefdeadbeefdeadbeef", 16);
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
    try {
      Long.parseLong("g", 16);
      fail("expected NumberFormatException");
    } catch (NumberFormatException ex) {
      // expected
    }
  }

  public void testReverse() {
    assertEquals(0L, Long.reverse(0L));
    assertEquals(-1L, Long.reverse(-1L));
    assertEquals(Long.MIN_VALUE, Long.reverse(1L));
    assertEquals(1L, Long.reverse(Long.MIN_VALUE));
    assertEquals(0xaaaaaaaaaaaaaaaaL, Long.reverse(0x5555555555555555L));
    assertEquals(0xaaaaaaaa00000000L, Long.reverse(0x55555555L));
    assertEquals(0xaa00aa00aa00aa00L, Long.reverse(0x0055005500550055L));
    assertEquals(0x5555555555555555L, Long.reverse(0xaaaaaaaaaaaaaaaaL));
    assertEquals(0x55555555L, Long.reverse(0xaaaaaaaa00000000L));
    assertEquals(0x0055005500550055L, Long.reverse(0xaa00aa00aa00aa00L));
  }

  public void testReverseBytes() {
    assertEquals(0, Long.reverseBytes(0));
    assertEquals(-1, Long.reverseBytes(-1));
    assertEquals(0x80L, Long.reverseBytes(Long.MIN_VALUE));
    assertEquals(Long.MIN_VALUE, Long.reverseBytes(0x80L));
    assertEquals(0xf0debc9a78563412L, Long.reverseBytes(0x123456789abcdef0L));
  }

  public void testRotateLeft() {
    assertEquals(0, Long.rotateLeft(0, 4));
    assertEquals(0x2, Long.rotateLeft(1, 1));
    assertEquals(0x10, Long.rotateLeft(1, 4));
    assertEquals(-1, Long.rotateLeft(-1, 4));
    assertEquals(Long.MIN_VALUE, Long.rotateLeft(0x4000000000000000L, 1));
    assertEquals(1, Long.rotateLeft(Long.MIN_VALUE, 1));
  }

  public void testRotateRight() {
    assertEquals(0, Long.rotateRight(0, 4));
    assertEquals(Long.MIN_VALUE, Long.rotateRight(1, 1));
    assertEquals(0x1000000000000000L, Long.rotateRight(1, 4));
    assertEquals(-1, Long.rotateRight(-1, 4));
  }

  public void testSignum() {
    assertEquals(0, Long.signum(0));
    assertEquals(1, Long.signum(1));
    assertEquals(-1, Long.signum(-1));
    assertEquals(1, Long.signum(Long.MAX_VALUE));
    assertEquals(-1, Long.signum(Long.MIN_VALUE));
  }

  public void testStaticValueOf() {
    assertEquals(Long.MIN_VALUE, Long.valueOf(Long.MIN_VALUE).longValue());
    assertEquals(Long.MAX_VALUE, Long.valueOf(Long.MAX_VALUE).longValue());
  }

  public void testToHexString() {
    assertEquals("1234500000000", Long.toHexString(0x1234500000000L));
    assertEquals("fff1234500000000", Long.toHexString(0xFFF1234500000000L));
  }
}
