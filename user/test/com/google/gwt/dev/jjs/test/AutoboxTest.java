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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests autoboxing.
 */
public class AutoboxTest extends GWTTestCase {

  private Boolean boxedBoolean = Boolean.TRUE;

  private Byte boxedByte = new Byte((byte) 0xAB);

  private Character boxedChar = new Character('c');

  private Double boxedDouble = new Double(1.2323);

  private Float boxedFloat = new Float(1.2F);

  private Integer boxedInt = new Integer(20000000);

  private Long boxedLong = new Long(1231231231231L);

  private Short boxedShort = new Short((short) 6550);

  private boolean unboxedBoolean = true;

  private byte unboxedByte = (byte) 0xAB;

  private char unboxedChar = 'c';

  private double unboxedDouble = 1.2323;

  private float unboxedFloat = 1.2F;

  private int unboxedInt = 20000000;

  private long unboxedLong = 1231231231231L;

  private short unboxedShort = 6550;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testBoxing() {
    Boolean boolean_ = unboxedBoolean;
    assertTrue(boolean_.booleanValue() == unboxedBoolean);
    Byte byte_ = unboxedByte;
    assertTrue(byte_.byteValue() == unboxedByte);
    Character char_ = unboxedChar;
    assertTrue(char_.charValue() == unboxedChar);
    Short short_ = unboxedShort;
    assertTrue(short_.shortValue() == unboxedShort);
    Integer int_ = unboxedInt;
    assertTrue(int_.intValue() == unboxedInt);
    Long long_ = unboxedLong;
    assertTrue(long_.longValue() == unboxedLong);
    Float float_ = unboxedFloat;
    assertTrue(float_.floatValue() == unboxedFloat);
    Double double_ = unboxedDouble;
    assertTrue(double_.doubleValue() == unboxedDouble);

    // test boxing of return values
    assertTrue(box(unboxedBoolean).booleanValue() == unboxedBoolean);
    assertTrue(box(unboxedByte).byteValue() == unboxedByte);
    assertTrue(box(unboxedShort).shortValue() == unboxedShort);
    assertTrue(box(unboxedChar).charValue() == unboxedChar);
    assertTrue(box(unboxedInt).intValue() == unboxedInt);
    assertTrue(box(unboxedLong).longValue() == unboxedLong);
    assertTrue(box(unboxedFloat).floatValue() == unboxedFloat);
    assertTrue(box(unboxedDouble).doubleValue() == unboxedDouble);

    // test boxing of parameters
    assertTrue(unbox(unboxedBoolean) == unboxedBoolean);
    assertTrue(unbox(unboxedByte) == unboxedByte);
    assertTrue(unbox(unboxedShort) == unboxedShort);
    assertTrue(unbox(unboxedChar) == unboxedChar);
    assertTrue(unbox(unboxedInt) == unboxedInt);
    assertTrue(unbox(unboxedLong) == unboxedLong);
    assertTrue(unbox(unboxedFloat) == unboxedFloat);
    assertTrue(unbox(unboxedDouble) == unboxedDouble);
  }

  /**
   * JLS 5.2 has a special case for assignment of a constant to a variable of
   * type Byte, Short, or Character. Such an assignment is allowed so long as
   * the constant fits within the type's range. In such cases, the box type can
   * be different from the type of the constant.
   */
  public void testBoxingDifferentType() {
    Character c = 1;
    assertEquals(Character.valueOf((char) 1), c);

    Byte b = 2;
    assertEquals(Byte.valueOf((byte) 2), b);

    Short s = 3;
    assertEquals(Short.valueOf((short) 3), s);
  }

  /**
   * Verify that .valueOf() methods return identical references for types within
   * certain ranges.
   */
  public void testCaching() {
    assertSame((byte) 3, (byte) 3);
    assertSame('A', 'A');
    assertSame((short) 120, (short) 120);
    assertSame(-13, -13);
    assertSame(7L, 7L);
  }

  /**
   * Test ++, --, and compound assignments like += when the left-hand side is a
   * boxed Integer. Use assertNotSame to ensure that a new Integer is created
   * instead of modifying the original integer in place. (Issue 2446).
   */
  public void testCompoundAssignmentsWithInteger() {
    {
      Integer operand, original, result;
      original = operand = 0;
      result = operand++;
      // operand must be different object now.
      assertNotSame("[o++] original != operand, ", original, operand);
      assertSame("[o++] original == result, ", original, result);
      assertNotSame("[o++] result != operand, ", result, operand);
      // checks against boxedvalues cached object.
      assertSame("[o++] valueOf(n) == operand, ", 1, operand);
      // checks cached object's value.
      assertEquals("[o++] n == operand.value, ", 1, operand.intValue());
    }

    {
      Integer operand, original, result;
      original = operand = 2;
      result = ++operand;
      assertNotSame("[++o] original != operand, ", original, operand);
      assertNotSame("[++o] original != result, ", original, result);
      assertSame("[++o] result == operand, ", result, operand);
      assertSame("[++o] valueOf(n) == operand, ", 3, operand);
      assertEquals("[++o] n == operand.value, ", 3, operand.intValue());
    }

    {
      Integer operand, original, result;
      original = operand = 5;
      result = operand--;
      assertNotSame("[o--] original != operand, ", original, operand);
      assertSame("[o--] original == result, ", original, result);
      assertNotSame("[o--] result != operand, ", result, operand);
      assertSame("[o--] valueOf(n) == operand, ", 4, operand);
      assertEquals("[o--] n == operand.value, ", 4, operand.intValue());
    }

    {
      Integer operand, original, result;
      original = operand = 7;
      result = --operand;
      assertNotSame("[--o] original != operand, ", original, operand);
      assertNotSame("[--o] original != result, ", original, result);
      assertSame("[--o] result == operand, ", result, operand);
      assertSame("[--o] valueOf(n) == operand, ", 6, operand);
      assertEquals("[--o] n == operand.value, ", 6, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 8;
      operand += 2;
      assertNotSame("[+=] original != operand, ", original, operand);
      assertSame("[+=] valueOf(n) == operand, ", 10, operand);
      assertEquals("[+=] n == operand.value, ", 10, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 11;
      operand -= 2;
      assertNotSame("[-=] original != operand, ", original, operand);
      assertSame("[-=] valueOf(n) == operand, ", 9, operand);
      assertEquals("[-=] n == operand.value, ", 9, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 21;
      operand *= 2;
      assertNotSame("[*=] original != operand, ", original, operand);
      assertSame("[*=] valueOf(n) == operand, ", 42, operand);
      assertEquals("[*=] n == operand.value, ", 42, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 30;
      operand /= 2;
      assertNotSame("[/=] original != operand, ", original, operand);
      assertSame("[/=] valueOf(n) == operand, ", 15, operand);
      assertEquals("[/=] n == operand.value, ", 15, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 123;
      operand %= 100;
      assertNotSame("[%=] original != operand, ", original, operand);
      assertSame("[%=] valueOf(n) == operand, ", 23, operand);
      assertEquals("[%=] n == operand.value, ", 23, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 0x55;
      operand &= 0xF;
      assertNotSame("[&=] original != operand, ", original, operand);
      assertSame("[&=] valueOf(n) == operand, ", 0x5, operand);
      assertEquals("[&=] n == operand.value, ", 0x5, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 0x55;
      operand |= 0xF;
      assertNotSame("[|=] original != operand, ", original, operand);
      assertSame("[|=] valueOf(n) == operand, ", 0x5F, operand);
      assertEquals("[|=] n == operand.value, ", 0x5F, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 0x55;
      operand ^= 0xF;
      assertNotSame("[&=] original != operand, ", original, operand);
      assertSame("[&=] valueOf(n) == operand, ", 0x5A, operand);
      assertEquals("[&=] n == operand.value, ", 0x5A, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = 0x3F;
      operand <<= 1;
      assertNotSame("[<<=] original != operand, ", original, operand);
      assertSame("[<<=] valueOf(n) == operand, ", 0x7E, operand);
      assertEquals("[<<=] n == operand.value, ", 0x7E, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = -16;
      operand >>= 1;
      assertNotSame("[>>=] original != operand, ", original, operand);
      assertSame("[>>=] valueOf(n) == operand, ", -8, operand);
      assertEquals("[>>=] n == operand.value, ", -8, operand.intValue());
    }

    {
      Integer operand, original;
      original = operand = -1;
      operand >>>= 1;
      assertNotSame("[>>>=] original != operand, ", original, operand);
      assertEquals("[>>>=] valueOf(n).equals(operand), ",
          Integer.valueOf(0x7FFFFFFF), operand);
      assertEquals("[>>>=] n == operand.value, ", 0x7FFFFFFF,
          operand.intValue());
    }
  }

  /**
   * Tests operations like += and *= where the left-hand side is a boxed Long.
   */
  public void testCompoundAssignmentsWithLong() {
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 += 5;
      assertEquals(10L, (long) long1);
      assertEquals(15L, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 += 1;
      assertEquals(10, (long) long1);
      assertEquals(11, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 -= 1;
      assertEquals(10, (long) long1);
      assertEquals(9, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 *= 2;
      assertEquals(10, (long) long1);
      assertEquals(20, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 /= 2;
      assertEquals(10, (long) long1);
      assertEquals(5, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 %= 3;
      assertEquals(10, (long) long1);
      assertEquals(1, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 <<= 1;
      assertEquals(10, (long) long1);
      assertEquals(20, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 >>= 1;
      assertEquals(10, (long) long1);
      assertEquals(5, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 >>>= 1;
      assertEquals(10, (long) long1);
      assertEquals(5, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 &= 8;
      assertEquals(10, (long) long1);
      assertEquals(8, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 |= 1;
      assertEquals(10, (long) long1);
      assertEquals(11, (long) long2);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 ^= 1;
      assertEquals(10, (long) long1);
      assertEquals(11, (long) long2);
    }
  }

  /**
   * Tests ++ and -- on all boxed types. Use assertNotSame to ensure that a new
   * wrapper is created instead of modifying the original boxed value in place.
   * (Issue 2446).
   */
  public void testIncrDecr() {
    {
      // these initial tests are miscellaneous one-off tests
      Byte originalBoxedByte = boxedByte;

      assertEquals(unboxedByte, (byte) boxedByte++);
      assertEquals(unboxedByte + 1, (byte) boxedByte);
      boxedByte = originalBoxedByte;

      Integer[] ary = new Integer[] {0, 10, 20, 30, 40, 50};
      Integer idx = 2;
      assertEquals(20, (int) ary[idx++]++);
      assertEquals(21, (int) ary[2]);
      assertEquals(3, (int) idx);
      assertEquals(40, (int) ary[idx += 1]);
      assertEquals(4, (int) idx);
    }
    // the rest of this method tests all boxed types under ++ and --
    {
      Byte originalBoxedByte = boxedByte;
      boxedByte++;
      assertNotSame("Boxed byte modified in place", boxedByte,
          originalBoxedByte);
      assertEquals(unboxedByte + 1, (byte) boxedByte);
      boxedByte = originalBoxedByte;
      ++boxedByte;
      assertNotSame("Boxed byte modified in place", boxedByte,
          originalBoxedByte);
      assertEquals(unboxedByte + 1, (byte) boxedByte);
      boxedByte = originalBoxedByte;
      boxedByte--;
      assertNotSame("Boxed byte modified in place", boxedByte,
          originalBoxedByte);
      assertEquals(unboxedByte - 1, (byte) boxedByte);
      boxedByte = originalBoxedByte;
      --boxedByte;
      assertNotSame("Boxed byte modified in place", boxedByte,
          originalBoxedByte);
      assertEquals(unboxedByte - 1, (byte) boxedByte);
      boxedByte = originalBoxedByte;
    }
    {
      Character originalBoxedChar = boxedChar;
      boxedChar++;
      assertNotSame("Boxed character modified in place", boxedChar,
          originalBoxedChar);
      assertEquals(unboxedChar + 1, (char) boxedChar);
      boxedChar = originalBoxedChar;
      ++boxedChar;
      assertNotSame("Boxed character modified in place", boxedChar,
          originalBoxedChar);
      assertEquals(unboxedChar + 1, (char) boxedChar);
      boxedChar = originalBoxedChar;
      boxedChar--;
      assertNotSame("Boxed character modified in place", boxedChar,
          originalBoxedChar);
      assertEquals(unboxedChar - 1, (char) boxedChar);
      boxedChar = originalBoxedChar;
      --boxedChar;
      assertNotSame("Boxed character modified in place", boxedChar,
          originalBoxedChar);
      assertEquals(unboxedChar - 1, (char) boxedChar);
      boxedChar = originalBoxedChar;
    }
    {
      Short originalBoxedShort = boxedShort;
      boxedShort++;
      assertNotSame("Boxed short modified in place", boxedShort,
          originalBoxedShort);
      assertEquals(unboxedShort + 1, (short) boxedShort);
      boxedShort = originalBoxedShort;
      ++boxedShort;
      assertNotSame("Boxed short modified in place", boxedShort,
          originalBoxedShort);
      assertEquals(unboxedShort + 1, (short) boxedShort);
      boxedShort = originalBoxedShort;
      boxedShort--;
      assertNotSame("Boxed short modified in place", boxedShort,
          originalBoxedShort);
      assertEquals(unboxedShort - 1, (short) boxedShort);
      boxedShort = originalBoxedShort;
      --boxedShort;
      assertNotSame("Boxed short modified in place", boxedShort,
          originalBoxedShort);
      assertEquals(unboxedShort - 1, (short) boxedShort);
      boxedShort = originalBoxedShort;
    }
    {
      Integer originalBoxedInt = boxedInt;
      boxedInt++;
      assertNotSame("Boxed int modified in place", boxedInt, originalBoxedInt);
      assertEquals(unboxedInt + 1, (int) boxedInt);
      boxedInt = originalBoxedInt;
      ++boxedInt;
      assertNotSame("Boxed int modified in place", boxedInt, originalBoxedInt);
      assertEquals(unboxedInt + 1, (int) boxedInt);
      boxedInt = originalBoxedInt;
      boxedInt--;
      assertNotSame("Boxed int modified in place", boxedInt, originalBoxedInt);
      assertEquals(unboxedInt - 1, (int) boxedInt);
      boxedInt = originalBoxedInt;
      --boxedInt;
      assertNotSame("Boxed int modified in place", boxedInt, originalBoxedInt);
      assertEquals(unboxedInt - 1, (int) boxedInt);
      boxedInt = originalBoxedInt;
    }
    {
      Long originalBoxedLong = boxedLong;
      boxedLong++;
      assertNotSame("Boxed long modified in place", boxedLong,
          originalBoxedLong);
      assertEquals(unboxedLong + 1, (long) boxedLong);
      boxedLong = originalBoxedLong;
      ++boxedLong;
      assertNotSame("Boxed long modified in place", boxedLong,
          originalBoxedLong);
      assertEquals(unboxedLong + 1, (long) boxedLong);
      boxedLong = originalBoxedLong;
      boxedLong--;
      assertNotSame("Boxed long modified in place", boxedLong,
          originalBoxedLong);
      assertEquals(unboxedLong - 1, (long) boxedLong);
      boxedLong = originalBoxedLong;
      --boxedLong;
      assertNotSame("Boxed long modified in place", boxedLong,
          originalBoxedLong);
      assertEquals(unboxedLong - 1, (long) boxedLong);
      boxedLong = originalBoxedLong;
    }
    {
      Float originalBoxedFloat = boxedFloat;
      boxedFloat++;
      assertNotSame("Boxed float modified in place", boxedFloat,
          originalBoxedFloat);
      assertEquals(unboxedFloat + 1, (float) boxedFloat);
      boxedFloat = originalBoxedFloat;
      ++boxedFloat;
      assertNotSame("Boxed float modified in place", boxedFloat,
          originalBoxedFloat);
      assertEquals(unboxedFloat + 1, (float) boxedFloat);
      boxedFloat = originalBoxedFloat;
      boxedFloat--;
      assertNotSame("Boxed float modified in place", boxedFloat,
          originalBoxedFloat);
      assertEquals(unboxedFloat - 1, (float) boxedFloat);
      boxedFloat = originalBoxedFloat;
      --boxedFloat;
      assertNotSame("Boxed float modified in place", boxedFloat,
          originalBoxedFloat);
      assertEquals(unboxedFloat - 1, (float) boxedFloat);
      boxedFloat = originalBoxedFloat;
    }
    {
      Double originalBoxedDouble = boxedDouble;
      boxedDouble++;
      assertNotSame("Boxed double modified in place", boxedDouble,
          originalBoxedDouble);
      assertEquals(unboxedDouble + 1, (double) boxedDouble);
      boxedDouble = originalBoxedDouble;
      ++boxedDouble;
      assertNotSame("Boxed double modified in place", boxedDouble,
          originalBoxedDouble);
      assertEquals(unboxedDouble + 1, (double) boxedDouble);
      boxedDouble = originalBoxedDouble;
      boxedDouble--;
      assertNotSame("Boxed double modified in place", boxedDouble,
          originalBoxedDouble);
      assertEquals(unboxedDouble - 1, (double) boxedDouble);
      boxedDouble = originalBoxedDouble;
      --boxedDouble;
      assertNotSame("Boxed double modified in place", boxedDouble,
          originalBoxedDouble);
      assertEquals(unboxedDouble - 1, (double) boxedDouble);
      boxedDouble = originalBoxedDouble;
    }
  }

  public void testUnboxing() {
    boolean boolean_ = boxedBoolean;
    assertTrue(boolean_ == boxedBoolean.booleanValue());
    byte byte_ = boxedByte;
    assertTrue(byte_ == boxedByte.byteValue());
    char char_ = boxedChar;
    assertTrue(char_ == boxedChar.charValue());
    short short_ = boxedShort;
    assertTrue(short_ == boxedShort.shortValue());
    int int_ = boxedInt;
    assertTrue(int_ == boxedInt.intValue());
    long long_ = boxedLong;
    assertTrue(long_ == boxedLong.longValue());
    float float_ = boxedFloat;
    assertTrue(float_ == boxedFloat.floatValue());
    double double_ = boxedDouble;
    assertTrue(double_ == boxedDouble.doubleValue());

    // test unboxing of return values
    assertTrue(unbox(boxedBoolean) == unboxedBoolean);
    assertTrue(unbox(boxedByte) == unboxedByte);
    assertTrue(unbox(boxedShort) == unboxedShort);
    assertTrue(unbox(boxedChar) == unboxedChar);
    assertTrue(unbox(boxedInt) == unboxedInt);
    assertTrue(unbox(boxedLong) == unboxedLong);
    assertTrue(unbox(boxedFloat) == unboxedFloat);
    assertTrue(unbox(boxedDouble) == unboxedDouble);

    // test unboxing of parameters
    assertTrue(box(boxedBoolean).booleanValue() == unboxedBoolean);
    assertTrue(box(boxedByte).byteValue() == unboxedByte);
    assertTrue(box(boxedShort).shortValue() == unboxedShort);
    assertTrue(box(boxedChar).charValue() == unboxedChar);
    assertTrue(box(boxedInt).intValue() == unboxedInt);
    assertTrue(box(boxedLong).longValue() == unboxedLong);
    assertTrue(box(boxedFloat).floatValue() == unboxedFloat);
    assertTrue(box(boxedDouble).doubleValue() == unboxedDouble);
  }

  public void testUnboxingDifferentType() {
    {
      short short_ = boxedByte;
      assertTrue(short_ == boxedByte.byteValue());
      int int_ = boxedByte;
      assertTrue(int_ == boxedByte.byteValue());
      long long_ = boxedByte;
      assertTrue(long_ == boxedByte.byteValue());
      float float_ = boxedByte;
      assertTrue(float_ == boxedByte.byteValue());
      double double_ = boxedByte;
      assertTrue(double_ == boxedByte.byteValue());
    }

    {
      int int_ = boxedShort;
      assertTrue(int_ == boxedShort.shortValue());
      long long_ = boxedShort;
      assertTrue(long_ == boxedShort.shortValue());
      float float_ = boxedShort;
      assertTrue(float_ == boxedShort.shortValue());
      double double_ = boxedShort;
      assertTrue(double_ == boxedShort.shortValue());
    }

    {
      int int_ = boxedChar;
      assertTrue(int_ == boxedChar.charValue());
      long long_ = boxedChar;
      assertTrue(long_ == boxedChar.charValue());
      float float_ = boxedChar;
      assertTrue(float_ == boxedChar.charValue());
      double double_ = boxedChar;
      assertTrue(double_ == boxedChar.charValue());
    }

    {
      long long_ = boxedInt;
      assertTrue(long_ == boxedInt.intValue());
      float float_ = boxedInt;
      assertTrue(float_ == boxedInt.intValue());
      double double_ = boxedInt;
      assertTrue(double_ == boxedInt.intValue());
    }
  }

  private Boolean box(boolean b) {
    return b;
  }

  private Byte box(byte b) {
    return b;
  }

  private Character box(char c) {
    return c;
  }

  private Double box(double d) {
    return d;
  }

  private Float box(float f) {
    return f;
  }

  private Integer box(int i) {
    return i;
  }

  private Long box(long l) {
    return l;
  }

  private Short box(short s) {
    return s;
  }

  private boolean unbox(Boolean b) {
    return b;
  }

  private byte unbox(Byte b) {
    return b;
  }

  private char unbox(Character c) {
    return c;
  }

  private double unbox(Double d) {
    return d;
  }

  private float unbox(Float f) {
    return f;
  }

  private int unbox(Integer i) {
    return i;
  }

  private long unbox(Long l) {
    return l;
  }

  private short unbox(Short s) {
    return s;
  }
}
