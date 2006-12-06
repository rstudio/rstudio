// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

public class IntegerTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testConstructor() {
    assertEquals(12345, new Integer(12345).intValue());
    assertEquals(12345, new Integer("12345").intValue());
  }

  public void testToString() {
    assertEquals("12345", new Integer(12345).toString());
    assertEquals("-12345", new Integer("-12345").toString());
  }

  public void testCompareTo() {
    assertEquals(-1, new Integer(12345).compareTo(new Integer(12346)));
    assertEquals(1, new Integer("12345").compareTo(new Integer(12344)));
    assertEquals(0, new Integer("12345").compareTo(new Integer(12345)));
  }

  public void testEquals() {
    assertFalse(new Integer(12345).equals(new Integer(12346)));
    assertEquals(new Integer("12345"), new Integer(12345));
  }

  public void testDecode() {
    assertEquals(12345, Integer.decode("12345").intValue());
    try {
      Integer.decode("abx");
      fail();
    } catch (NumberFormatException e) {
      // pass
    }
  }

  public void testHashCode() {
    assertEquals(1234, new Integer(1234).hashCode());
  }

  public void testValueOf() {
    assertEquals(new Integer(12345), Integer.valueOf("12345"));
    assertEquals(new Integer(1865), Integer.valueOf("12345", 6));
    assertEquals(12345, Integer.parseInt("12345"));
    assertEquals(1865, Integer.parseInt("12345", 6));
  }

  public void testHexString() {
    assertEquals("3039", Integer.toHexString(12345));
    assertEquals("0", Integer.toHexString(0));
    assertEquals("ffffcfc7", Integer.toHexString(-12345));
  }

  public void testBinaryString() {
    assertEquals("11000000111001", Integer.toBinaryString(12345));
    assertEquals("0", Integer.toBinaryString(0));
    assertEquals("11111111111111111100111111000111", Integer.toBinaryString(-12345));
  }

  public void testXValue() {
    assertEquals("short",(short) 12345, new Integer(12345).shortValue());
    assertEquals("long", 1234567890l, new Integer(1234567890).longValue());
    assertEquals("double", 12345d, new Integer(12345).doubleValue(),0.001);
    assertEquals("float",12345f, new Integer(12345).floatValue(),0.01);
    assertEquals("byte", (byte) 123, new Integer(123).byteValue());
    assertEquals("integer",123, new Integer(123).intValue());
    assertEquals("short overflow", (short) 10713, new Integer(1234512345).shortValue());
    assertEquals("double2", 1234512345d, new Integer(1234512345).doubleValue(), 0.001);
    // Invalid test right now; we don't coerce to single precision
    // assertEquals("float2",1234512345f, new Integer(1234512345).floatValue(),0.001);
    assertEquals("byte overflow",(byte) -13, new Integer(123123).byteValue());
  }

}
