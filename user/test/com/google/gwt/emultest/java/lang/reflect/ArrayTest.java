/*
 * Copyright 2017 Google Inc.
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
package com.google.gwt.emultest.java.lang.reflect;

import com.google.gwt.junit.client.GWTTestCase;

import java.lang.reflect.Array;

/** Tests for java.lang.reflect.Array. */
public final class ArrayTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testGet() {
    try {
      Array.get(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.get(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertEquals("1", Array.get(new Object[] {"1"}, 0));
    assertEquals(Boolean.TRUE, Array.get(new boolean[] {true}, 0));
    assertEquals(new Byte((byte) 1), Array.get(new byte[] {1}, 0));
    assertEquals(new Character('1'), Array.get(new char[] {'1'}, 0));
    assertEquals(new Double(1.0d), Array.get(new double[] {1}, 0));
    assertEquals(new Float(1.0f), Array.get(new float[] {1.0f}, 0));
    assertEquals(new Integer(1), Array.get(new int[] {1}, 0));
    assertEquals(new Long(1L), Array.get(new long[] {1}, 0));
    assertEquals(new Short((short) 1), Array.get(new short[] {1}, 0));
  }

  public void testGetBoolean() {
    try {
      Array.getBoolean(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getBoolean(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getBoolean(new Boolean[] {true}, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertTrue(Array.getBoolean(new boolean[] {true}, 0));
    assertFalse(Array.getBoolean(new boolean[] {false}, 0));
    assertTrue(Array.getBoolean(new boolean[] {Boolean.TRUE}, 0));
    assertFalse(Array.getBoolean(new boolean[] {Boolean.FALSE}, 0));
  }

  public void testGetByte() {
    try {
      Array.getByte(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getByte(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getByte(new Byte[] {(byte) 0}, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertEquals((byte) 1, Array.getByte(new byte[] {(byte) 1}, 0));
    assertEquals((byte) 1, Array.getByte(new byte[] {(byte) 1}, 0));
    assertEquals((byte) 1, Array.getByte(new byte[] {(byte) 1}, 0));
    assertEquals((byte) 1, Array.getByte(new byte[] {(byte) 1}, 0));
  }

  public void testGetChar() {
    try {
      Array.getChar(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getChar(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getChar(new Character[] {'0'}, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertEquals('1', Array.getChar(new char[] {'1'}, 0));
  }

  public void testGetDouble() {
    try {
      Array.getDouble(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getDouble(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getDouble(new Double[] {0d}, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertEquals(1.0d, Array.getDouble(new double[] {1.0d}, 0));
    assertEquals(1.0d, Array.getDouble(new float[] {1.0f}, 0));
    assertEquals(1.0d, Array.getDouble(new byte[] {1}, 0));
    assertEquals(1.0d, Array.getDouble(new char[] {1}, 0));
    assertEquals(1.0d, Array.getDouble(new int[] {1}, 0));
    assertEquals(1.0d, Array.getDouble(new long[] {1}, 0));
    assertEquals(1.0d, Array.getDouble(new short[] {1}, 0));
  }

  public void testGetFloat() {
    try {
      Array.getFloat(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getFloat(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getFloat(new Float[] {0f}, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertEquals(1.0f, Array.getFloat(new float[] {1.0f}, 0));
    assertEquals(1.0f, Array.getFloat(new byte[] {1}, 0));
    assertEquals(1.0f, Array.getFloat(new char[] {1}, 0));
    assertEquals(1.0f, Array.getFloat(new int[] {1}, 0));
    assertEquals(1.0f, Array.getFloat(new long[] {1}, 0));
    assertEquals(1.0f, Array.getFloat(new short[] {1}, 0));
  }

  public void testGetInt() {
    try {
      Array.getInt(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getInt(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getInt(new Integer[] {0}, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertEquals(1, Array.getInt(new int[] {1}, 0));
    assertEquals(1, Array.getInt(new byte[] {1}, 0));
    assertEquals(1, Array.getInt(new char[] {1}, 0));
    assertEquals(1, Array.getInt(new short[] {1}, 0));
  }

  public void testGetLength() {
    try {
      Array.getLength(null);
      fail();
    } catch (RuntimeException expected) {
    }
    assertEquals(0, Array.getLength(new Object[0]));
    assertEquals(1, Array.getLength(new Object[1]));

    assertEquals(0, Array.getLength(new int[0]));
    assertEquals(1, Array.getLength(new int[1]));
  }

  public void testGetLong() {
    try {
      Array.getLong(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getLong(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getLong(new Long[] {0L}, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertEquals(1L, Array.getLong(new long[] {1L}, 0));
    assertEquals(1L, Array.getLong(new byte[] {1}, 0));
    assertEquals(1L, Array.getLong(new char[] {1}, 0));
    assertEquals(1L, Array.getLong(new int[] {1}, 0));
    assertEquals(1L, Array.getLong(new short[] {1}, 0));
  }

  public void testGetShort() {
    try {
      Array.getShort(null, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getShort(new Object(), 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.getShort(new Short[] {(short) 1}, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    assertEquals((short) 1, Array.getShort(new short[] {(short) 1}, 0));
    assertEquals((short) 1, Array.getShort(new short[] {1}, 0));
  }

  public void testSet() {
    try {
      Array.set(null, 0, true);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.set(new Object(), 0, true);
      fail();
    } catch (RuntimeException expected) {
    }

    Object[] objectArray = new Object[1];
    Array.set(objectArray, 0, "1");
    assertEquals("1", objectArray[0]);

    boolean[] booleanArray = new boolean[1];
    Array.set(booleanArray, 0, true);
    assertTrue(booleanArray[0]);

    byte[] byteArray = new byte[1];
    Array.set(byteArray, 0, (byte) 1);
    assertEquals((byte) 1, byteArray[0]);

    char[] charArray = new char[1];
    Array.set(charArray, 0, 'a');
    assertEquals('a', charArray[0]);

    double[] doubleArray = new double[1];
    Array.set(doubleArray, 0, 1.0d);
    assertEquals(1.0d, doubleArray[0]);

    float[] floatArray = new float[1];
    Array.set(floatArray, 0, 1.0f);
    assertEquals(1.0f, floatArray[0]);

    int[] intArray = new int[1];
    Array.set(intArray, 0, 1);
    assertEquals(1, intArray[0]);

    long[] longArray = new long[1];
    Array.set(longArray, 0, 1L);
    assertEquals(1L, longArray[0]);

    short[] shortArray = new short[1];
    Array.set(shortArray, 0, (short) 1);
    assertEquals((short) 1, shortArray[0]);

    // test widening
    long[] otherLongArray = new long[10];
    Array.set(otherLongArray, 0, (byte) 2);
    assertEquals(2L, otherLongArray[0]);

    Array.set(otherLongArray, 1, (short) 3);
    assertEquals(3L, otherLongArray[1]);

    Array.set(otherLongArray, 2, (char) 4);
    assertEquals(4L, otherLongArray[2]);

    Array.set(otherLongArray, 3, 5);
    assertEquals(5L, otherLongArray[3]);

    double[] otherDoubleArray = new double[10];
    Array.set(otherDoubleArray, 0, (byte) 2);
    assertEquals(2d, otherDoubleArray[0], 0.00001);

    Array.set(otherDoubleArray, 1, (short) 3);
    assertEquals(3d, otherDoubleArray[1], 0.00001);

    Array.set(otherDoubleArray, 2, (char) 4);
    assertEquals(4d, otherDoubleArray[2], 0.00001);

    Array.set(otherDoubleArray, 3, 5);
    assertEquals(5d, otherDoubleArray[3], 0.00001);

    Array.set(otherDoubleArray, 4, (float) 6);
    assertEquals(6d, otherDoubleArray[4], 0.00001);

    Array.set(otherDoubleArray, 5, (double) 7);
    assertEquals(7d, otherDoubleArray[5], 0.00001);
  }

  public void testSetBoolean() {
    try {
      Array.setBoolean(null, 0, true);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.setBoolean(new Object(), 0, true);
      fail();
    } catch (RuntimeException expected) {
    }

    boolean[] array = new boolean[1];

    Array.setBoolean(array, 0, true);
    assertTrue(array[0]);
  }

  public void testSetByte() {
    try {
      Array.setByte(null, 0, (byte) 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.setByte(new Object(), 0, (byte) 0);
      fail();
    } catch (RuntimeException expected) {
    }

    byte[] bArray = new byte[1];
    Array.setByte(bArray, 0, (byte) 1);
    assertEquals((byte) 1, bArray[0]);

    double[] dArray = new double[1];
    Array.setByte(dArray, 0, (byte) 1);
    assertEquals((byte) 1, (byte) dArray[0]);

    float[] fArray = new float[1];
    Array.setByte(fArray, 0, (byte) 1);
    assertEquals((byte) 1, (byte) fArray[0]);

    int[] iArray = new int[1];
    Array.setByte(iArray, 0, (byte) 1);
    assertEquals((byte) 1, (byte) iArray[0]);

    long[] lArray = new long[1];
    Array.setByte(lArray, 0, (byte) 1);
    assertEquals((byte) 1, (byte) lArray[0]);

    short[] sArray = new short[1];
    Array.setByte(sArray, 0, (byte) 1);
    assertEquals((byte) 1, (byte) sArray[0]);
  }

  public void testSetChar() {
    try {
      Array.setChar(null, 0, 'a');
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.setChar(new Object(), 0, 'a');
      fail();
    } catch (RuntimeException expected) {
    }

    char[] array = new char[1];

    Array.setChar(array, 0, 'a');
    assertEquals('a', array[0]);

    double[] dArray = new double[1];
    Array.setChar(dArray, 0, (char) 1);
    assertEquals((byte) 1, (byte) dArray[0]);

    float[] fArray = new float[1];
    Array.setChar(fArray, 0, (char) 1);
    assertEquals((byte) 1, (byte) fArray[0]);

    int[] iArray = new int[1];
    Array.setChar(iArray, 0, (char) 1);
    assertEquals((byte) 1, (byte) iArray[0]);

    long[] lArray = new long[1];
    Array.setChar(lArray, 0, (char) 1);
    assertEquals((byte) 1, (byte) lArray[0]);
  }

  public void testSetDouble() {
    try {
      Array.setDouble(null, 0, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.setDouble(new Object(), 0, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    double[] array = new double[1];

    Array.setDouble(array, 0, 1d);
    assertEquals(1d, array[0]);
  }

  public void testSetFloat() {
    try {
      Array.setFloat(null, 0, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.setFloat(new Object(), 0, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    float[] array = new float[1];

    Array.setFloat(array, 0, 1.0f);
    assertEquals(1.0f, array[0]);

    double[] dArray = new double[1];
    Array.setFloat(dArray, 0, 1);
    assertEquals((float) 1, (float) dArray[0]);
  }

  public void testSetInt() {
    try {
      Array.setInt(null, 0, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.setInt(new Object(), 0, 0);
      fail();
    } catch (RuntimeException expected) {
    }

    int[] array = new int[1];

    Array.setInt(array, 0, 1);
    assertEquals(1, array[0]);

    double[] dArray = new double[1];
    Array.setInt(dArray, 0, 1);
    assertEquals(1, (int) dArray[0]);

    float[] fArray = new float[1];
    Array.setInt(fArray, 0, 1);
    assertEquals(1, (int) fArray[0]);

    long[] lArray = new long[1];
    Array.setInt(lArray, 0, 1);
    assertEquals(1, (int) lArray[0]);
  }

  public void testSetLong() {
    try {
      Array.setLong(null, 0, 0L);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.setLong(new Object(), 0, 0L);
      fail();
    } catch (RuntimeException expected) {
    }

    long[] array = new long[1];

    Array.setLong(array, 0, 1L);
    assertEquals(1L, array[0]);

    double[] dArray = new double[1];
    Array.setLong(dArray, 0, 1);
    assertEquals(1L, (long) dArray[0]);

    float[] fArray = new float[1];
    Array.setLong(fArray, 0, 1);
    assertEquals(1L, (long) fArray[0]);
  }

  public void testSetShort() {
    try {
      Array.setShort(null, 0, (short) 1);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      Array.setShort(new Object(), 0, (short) 1);
      fail();
    } catch (RuntimeException expected) {
    }

    short[] array = new short[1];

    Array.setShort(array, 0, (short) 1);
    assertEquals((short) 1, array[0]);

    int[] iArray = new int[1];
    Array.setShort(iArray, 0, (short) 1);
    assertEquals((short) 1, (short) iArray[0]);

    double[] dArray = new double[1];
    Array.setShort(dArray, 0, (short) 1);
    assertEquals((short) 1, (short) dArray[0]);

    float[] fArray = new float[1];
    Array.setShort(fArray, 0, (short) 1);
    assertEquals((short) 1, (short) fArray[0]);

    long[] lArray = new long[1];
    Array.setShort(lArray, 0, (short) 1);
    assertEquals((short) 1, (short) lArray[0]);
  }
}
