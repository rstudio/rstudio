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

  /*
   * TODO: Determine whether we fully support the JLS spec in regards to caching
   * of autoboxed values.
   * 
   * public void testCaching() { }
   */

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
