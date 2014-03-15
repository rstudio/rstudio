/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

/**
 * Tests enum with names obfuscated functionality.
 *
 * Note: If running in an environment with WARN logging enabled, check to see
 * that calls to name(), toString(), and valueOf() below, are logged in the
 * precompile phase.
 */
@DoNotRunWith(Platform.Devel)
public class EnumsWithNameObfuscationTest extends EnumsTest {

  enum Fruit {
    APPLE, BERRY, CHERRY
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.EnumsWithNameObfuscationSuite";
  }

  public void testObfuscatedFruit() {
    /*
     * Note: Also check that the strings "APPLE" or "BERRY" never appear
     * anywhere in the compiled output, if compilation is done with
     * optimization and obfuscation.  "CHERRY" will appear, since it
     * is used as a string literal in the comparison tests below.
     */
    assertEquals(0, Fruit.APPLE.ordinal());
    assertEquals(1, Fruit.BERRY.ordinal());
    assertEquals(2, Fruit.CHERRY.ordinal());

    assertFalse(Fruit.CHERRY.toString().equals("CHERRY"));
    assertFalse(Fruit.CHERRY.name().equals("CHERRY"));

    assertTrue(Fruit.CHERRY.toString().equals("" + Fruit.CHERRY.ordinal()));
    assertTrue(Fruit.CHERRY.name().equals("" + Fruit.CHERRY.ordinal()));

    try {
      Fruit.valueOf("CHERRIESONTOP");
      fail("Enum.valueOf(), expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      Fruit.valueOf("CHERRY");
      fail("Enum.valueOf(), expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      Enum.valueOf(Fruit.class,"CHERRIESONTOP");
      fail("Enum.valueOf(), expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      Enum.valueOf(Fruit.class,"CHERRY");
      fail("Enum.valueOf(), expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Override
  public void testCompareTo() {

    try {
      assertTrue(Basic.A.compareTo(Basic.valueOf("A")) == 0);
      fail("Basic.valueOf(), expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    assertTrue(Basic.B.compareTo(Basic.A) > 0);
    assertTrue(Basic.A.compareTo(Basic.B) < 0);

    try {
      assertTrue(Complex.A.compareTo(Complex.valueOf("A")) == 0);
      fail("Complex.valueOf(), expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    assertTrue(Complex.B.compareTo(Complex.A) > 0);
    assertTrue(Complex.A.compareTo(Complex.B) < 0);

    try {
      assertTrue(Subclassing.A.compareTo(Subclassing.valueOf("A")) == 0);
      fail("Subclassing.valueOf(), expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    assertTrue(Subclassing.B.compareTo(Subclassing.A) > 0);
    assertTrue(Subclassing.A.compareTo(Subclassing.B) < 0);
  }

  @Override
  public void testName() {
    assertFalse("A".equals(Basic.A.name()));
    assertFalse("B".equals(Basic.B.name()));
    assertFalse("C".equals(Basic.C.name()));

    assertFalse("A".equals(Complex.A.name()));
    assertFalse("B".equals(Complex.B.name()));
    assertFalse("C".equals(Complex.C.name()));

    assertFalse("A".equals(Subclassing.A.name()));
    assertFalse("B".equals(Subclassing.B.name()));
    assertFalse("C".equals(Subclassing.C.name()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void testValueOf() {

    try {
      Basic.valueOf("D");
      fail("Basic.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      Complex.valueOf("D");
      fail("Complex.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      Subclassing.valueOf("D");
      fail("Subclassing.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    enumValuesTest(Basic.class);
    enumValuesTest(Complex.class);
    enumValuesTest(Subclassing.class);

    try {
      Enum.valueOf(Basic.class, "foo");
      fail("Passed an invalid enum constant name to Enum.valueOf; expected "
          + "IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      Class fakeEnumClass = String.class;
      Enum.valueOf(fakeEnumClass, "foo");
      fail("Passed a non enum class to Enum.valueOf; expected "
              + "IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      Class<Basic> nullEnumClass = null;
      Enum.valueOf(nullEnumClass, "foo");
      fail("Passed a null enum class to Enum.valueOf; expected "
          + "NullPointerException");
    } catch (JavaScriptException expected) {
    } catch (NullPointerException expected) {
    }

    try {
      Enum.valueOf(Basic.class, null);
      fail("Passed a null enum constant to Enum.valueOf; expected "
          + "NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  @Override
  public void testValueOfOverload() {
    BasicWithOverloadedValueOf valById1 = BasicWithOverloadedValueOf.valueOf(1);
    BasicWithOverloadedValueOf valById2 = BasicWithOverloadedValueOf.valueOf(2);
    assertEquals(valById1.ordinal(),0);
    assertEquals(valById2.ordinal(),1);
  }

  private <T extends Enum<T>> void enumValuesTest(Class<T> enumClass) {
    T[] constants = enumClass.getEnumConstants();
    for (T constant : constants) {
      assertEquals(constant, Enum.valueOf(enumClass, constant.name()));
    }
  }
}
