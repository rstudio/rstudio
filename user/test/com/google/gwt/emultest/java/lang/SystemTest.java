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

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.testing.TestUtils;

import java.util.Arrays;

/**
 * Tests java.lang.System.
 */
public class SystemTest extends GWTTestCase {

  private static class Bar extends Foo {
    public Bar() {
    }
  }

  private enum EnumImpl implements Interfaz {
    FOO,
    BAR,
    BAZ
  }

  private static class Foo {
    public Foo() {
    }
  }

  private interface Interfaz {
  }

  private static class InterfazImpl implements Interfaz {

    private final String data;

    /**
     * @param data non-null string
     */
    InterfazImpl(String data) {
      this.data = data;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof InterfazImpl) && ((InterfazImpl) obj).data.equals(
          data);
    }

    @Override
    public int hashCode() {
      return data.hashCode();
    }

    @Override
    public String toString() {
      return "InterfazImpl[data=" + data + "]";
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testArraycopyEnumToInterface() {
    EnumImpl[] src = new EnumImpl[]{ EnumImpl.FOO, null, EnumImpl.BAZ };
    Interfaz[] dest = new Interfaz[5];
    Arrays.fill(dest, null);  // undefined != null, weird.

    System.arraycopy(src, 0, dest, 1, 3);
    assertEquals(
        Arrays.asList(null, EnumImpl.FOO, null, EnumImpl.BAZ, null),
        Arrays.asList(dest));
  }

  public void testArraycopyEnumToObject() {
    EnumImpl[] src = new EnumImpl[]{ EnumImpl.FOO, null, EnumImpl.BAZ };
    Object[] dest = new Object[5];
    Arrays.fill(dest, null);  // undefined != null, weird.

    System.arraycopy(src, 0, dest, 1, 3);
    assertEquals(
        Arrays.asList(null, EnumImpl.FOO, null, EnumImpl.BAZ, null),
        Arrays.asList(dest));
  }

  public void testArraycopyFailures() {
    int[] src = new int[4];
    int[] dest = new int[] {1, 1, 1, 1};
    double[] destDouble = new double[4];
    String[] strings = new String[4];
    try {
      System.arraycopy(src, 5, dest, 0, 7);
      fail("Should have thrown IndexOutOfBoundsException: src past end");
    } catch (IndexOutOfBoundsException e) {
    }
    try {
      System.arraycopy(src, 0, dest, 5, 7);
      fail("Should have thrown IndexOutOfBoundsException: dest past end");
    } catch (IndexOutOfBoundsException e) {
    }
    try {
      System.arraycopy(src, -1, dest, 0, 4);
      fail("Should have thrown IndexOutOfBoundsException: src ofs negative");
    } catch (IndexOutOfBoundsException e) {
    }
    try {
      System.arraycopy(src, 0, dest, -1, 4);
      fail("Should have thrown IndexOutOfBoundsException: dest ofs negative");
    } catch (IndexOutOfBoundsException e) {
    }
    try {
      System.arraycopy(src, 0, dest, 0, -1);
      fail("Should have thrown IndexOutOfBoundsException: negative length");
    } catch (IndexOutOfBoundsException e) {
    }
    try {
      System.arraycopy("test", 0, dest, 0, 4);
      fail("Should have thrown ArrayStoreException: src not array");
    } catch (ArrayStoreException e) {
    }
    try {
      System.arraycopy(src, 0, "test", 0, 4);
      fail("Should have thrown ArrayStoreException: dest not array");
    } catch (ArrayStoreException e) {
    }
    try {
      System.arraycopy(src, 0, destDouble, 0, 4);
      fail("Should have thrown ArrayStoreException: different primitive types");
    } catch (ArrayStoreException e) {
    }
    try {
      System.arraycopy(strings, 0, dest, 0, 4);
      fail("Should have thrown ArrayStoreException: reference/primitive mismatch");
    } catch (ArrayStoreException e) {
    }
    try {
      System.arraycopy(src, 0, strings, 0, 4);
      fail("Should have thrown ArrayStoreException: primitive/reference mismatch");
    } catch (ArrayStoreException e) {
    }
  }

  public void testArraycopyInterfaceToObject() {
    Interfaz[] src = new Interfaz[]{
        new InterfazImpl("foo"), null, new InterfazImpl("bar") };
    Object[] dest = new Object[5];
    Arrays.fill(dest, null);  // undefined != null, weird.

    System.arraycopy(src, 0, dest, 1, 3);
    assertEquals(Arrays.asList(null, new InterfazImpl("foo"), null,
        new InterfazImpl("bar"), null), Arrays.asList(dest));
  }

  public void testArraycopyMultidim() {
    Object[][] objArray = new Object[1][1];
    String[][] strArray = new String[1][1];
    strArray[0][0] = "Test";
    Integer[][] intArray = new Integer[1][1];
    intArray[0][0] = new Integer(1);
    System.arraycopy(strArray, 0, objArray, 0, 1);
    assertEquals("Test", objArray[0][0]);
    try {
      System.arraycopy(strArray, 0, intArray, 0, 1);
      fail("Should have thrown ArrayStoreException: incompatible multidimensional arrays");
    } catch (ArrayStoreException e) {
    }
    try {
      System.arraycopy(new String[] {"T2"}, 0, objArray, 0, 1);
      fail("Should have thrown ArrayStoreException: store string array in multi-dim Object array");
    } catch (ArrayStoreException e) {
    }
  }

  public void testArraycopyNulls() {
    int[] src = new int[4];
    int[] dest = new int[] {1, 1, 1, 1};
    try {
      System.arraycopy(null, 0, dest, 0, 4);
      fail("Should have thrown NullPointerException: src null");
    } catch (NullPointerException e) {
      // verify dest unchanged
      for (int i = 0; i < dest.length; ++i) {
        assertEquals(1, dest[i]);
      }
    }
    try {
      System.arraycopy(src, 0, null, 0, 4);
      fail("Should have thrown NullPointerException: dest null");
    } catch (NullPointerException e) {
    }
  }

  public void testArraycopyObjects() {
    Foo[] fooArray = new Foo[4];
    Bar[] barArray = new Bar[4];
    Object[] src = new Object[] {new Bar(), new Bar(), new Foo(), new Bar()};
    System.arraycopy(src, 0, fooArray, 0, src.length);
    for (int i = 0; i < src.length; ++i) {
      assertEquals(src[i], fooArray[i]);
    }
    try {
      System.arraycopy(src, 0, barArray, 0, 4);
      fail("Should have thrown ArrayStoreException: foo into bar");
    } catch (ArrayStoreException e) {
      // verify we changed only up to the element causing the exception
      assertEquals(src[0], barArray[0]);
      assertEquals(src[1], barArray[1]);
      assertNull(barArray[2]);
      assertNull(barArray[3]);
    }
  }

  public void testArraycopyOverlap() {
    int[] intArray = new int[] {0, 1, 2, 3};
    String[] strArray = new String[] {"0", "1", "2", "3"};

    System.arraycopy(intArray, 0, intArray, 1, intArray.length - 1);
    assertEquals(0, intArray[0]);
    for (int i = 1; i < intArray.length; ++i) {
      assertEquals("rev int copy index " + i, i - 1, intArray[i]);
    }
    System.arraycopy(intArray, 1, intArray, 0, intArray.length - 1);
    for (int i = 0; i < intArray.length - 1; ++i) {
      assertEquals("fwd int copy index " + i, i, intArray[i]);
    }
    assertEquals("fwd int copy index " + (intArray.length - 2),
        intArray.length - 2, intArray[intArray.length - 1]);
    System.arraycopy(strArray, 0, strArray, 1, strArray.length - 1);
    assertEquals(0, Integer.valueOf(strArray[0]).intValue());
    for (int i = 1; i < strArray.length; ++i) {
      assertEquals("rev str copy index " + i, i - 1, Integer.valueOf(
          strArray[i]).intValue());
    }
    System.arraycopy(strArray, 1, strArray, 0, strArray.length - 1);
    for (int i = 0; i < strArray.length - 1; ++i) {
      assertEquals("fwd str copy index " + i, i, Integer.valueOf(
          strArray[i]).intValue());
    }
    assertEquals("fwd str copy index " + (strArray.length - 2),
        strArray.length - 2,
        Integer.valueOf(strArray[strArray.length - 1]).intValue());
    /*
     * TODO(jat): how is it supposed to behave with overlapped copies if there
     * is a failure in the middle of the copy? We should figure that out and
     * test for it.
     */
  }

  public void testArraycopyPrimitives() {
    int[] src = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
    int[] dest = new int[8];

    System.arraycopy(src, 0, dest, 0, src.length);
    for (int i = 0; i < src.length; ++i) {
      assertEquals(src[i], dest[i]);
    }
    System.arraycopy(src, 2, dest, 1, 5);
    assertEquals(0, dest[0]);
    for (int i = 1; i < 6; ++i) {
      assertEquals(src[i + 1], dest[i]);
    }
  }

  public void testArraycopyLargeArray() {
    char[] largeCharArrayValue = C.getLargeCharArrayValue();
    char[] charDest = new char[largeCharArrayValue.length];
    System.arraycopy(largeCharArrayValue, 0, charDest, 0, largeCharArrayValue.length);
    for (int i = 0; i < largeCharArrayValue.length; ++i) {
      assertEquals("index " + i, largeCharArrayValue[i], charDest[i]);
    }

    int offset = largeCharArrayValue.length / 2;
    char[] manyNulls = new char[offset / 2];
    System.arraycopy(manyNulls, 0, charDest, offset, manyNulls.length);
    assertEquals('f', charDest[offset - 1]);
    for (int i = offset; i < offset + manyNulls.length; ++i) {
      assertEquals("index " + i, '\0', charDest[i]);
    }
    assertEquals('a', charDest[offset + manyNulls.length]);
  }

  public void testIdentityHashCode() {
    String s = "str";
    assertEquals(System.identityHashCode(s), System.identityHashCode(s));

    Double d = 42d;
    assertEquals(System.identityHashCode(d), System.identityHashCode(d));

    Boolean b = true;
    assertEquals(System.identityHashCode(b), System.identityHashCode(b));

    Object o = new Object();
    assertEquals(System.identityHashCode(o), System.identityHashCode(o));
    assertNotSame(System.identityHashCode(o), System.identityHashCode(new Object()));
  }

  @DoNotRunWith(Platform.Devel)
  public void testGetProperty() {
    if (TestUtils.isJvm()) {
      return;
    }
    assertEquals("conf", System.getProperty("someConfigurationProperty"));
    assertEquals("conf", System.getProperty("someConfigurationProperty", "default"));

    String someConf = System.getProperty("nonExistent", "default");
    assertEquals("default", someConf);

    // Note that default is not a String literal.
    assertEquals("default", System.getProperty("otherNonExistent", someConf));
  }
}
