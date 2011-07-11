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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests enum functionality.
 */
public class EnumsTest extends GWTTestCase {

  enum Basic {
    A, B, C
  }

  enum Complex {
    A("X"), B("Y"), C("Z");

    String value;

    Complex(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  enum Subclassing {
    A {
      @Override
      public String value() {
        return "X";
      }
    },
    B {
      @Override
      public String value() {
        return "Y";
      }
    },
    C {
      @Override
      public String value() {
        return "Z";
      }
    };

    public abstract String value();
  }
  
  enum BasicWithOverloadedValueOf {
    A(1), B(2), C(3);
    
    private final int id;
    
    private BasicWithOverloadedValueOf(int id) {
      this.id = id;
    }
    
    public static BasicWithOverloadedValueOf valueOf(Integer id) {
      for (BasicWithOverloadedValueOf val : BasicWithOverloadedValueOf.values()) {
        if (val.id == id) {
          return val;
        }
      }
      throw new IllegalArgumentException();
    }
  }

  /*
   * An enum that will have a static impl generated, which will allow it to
   * become ordinalizable. Once ordinalized, need to make sure it's clinit
   * is handled properly.
   */
  enum EnumWithStaticImpl {
    VALUE1;

    // a mock formatter, to simulate the case that inspired this test
    private static class MockFormat {
      private String pattern;
      
      public MockFormat(String pattern) {
        this.pattern = pattern;
      }
    
      public String format(Long date) {
        return pattern + date.toString();
      }
    }

    private static final MockFormat FORMATTER = new MockFormat("asdf");

    public String formatDate(long date) {
      switch (this) {
        case VALUE1:
          return FORMATTER.format(new Long(date)); 
        default:
          return null;
      }
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.EnumsSuite";
  }

  public void testCompareTo() {
    assertTrue(Basic.A.compareTo(Basic.valueOf("A")) == 0);
    assertTrue(Basic.B.compareTo(Basic.A) > 0);
    assertTrue(Basic.A.compareTo(Basic.B) < 0);

    assertTrue(Complex.A.compareTo(Complex.valueOf("A")) == 0);
    assertTrue(Complex.B.compareTo(Complex.A) > 0);
    assertTrue(Complex.A.compareTo(Complex.B) < 0);

    assertTrue(Subclassing.A.compareTo(Subclassing.valueOf("A")) == 0);
    assertTrue(Subclassing.B.compareTo(Subclassing.A) > 0);
    assertTrue(Subclassing.A.compareTo(Subclassing.B) < 0);
  }

  public void testField() {
    assertEquals("X", Complex.A.value);
    assertEquals("Y", Complex.B.value);
    assertEquals("Z", Complex.C.value);
  }

  public void testGetDeclaringClass() {
    assertEquals(Basic.class, Basic.A.getDeclaringClass());
    assertEquals(Complex.class, Complex.A.getDeclaringClass());
    assertEquals(Subclassing.class, Subclassing.A.getDeclaringClass());
  }

  public void testMethod() {
    assertEquals("X", Complex.A.value());
    assertEquals("Y", Complex.B.value());
    assertEquals("Z", Complex.C.value());

    assertEquals("X", Subclassing.A.value());
    assertEquals("Y", Subclassing.B.value());
    assertEquals("Z", Subclassing.C.value());
  }

  public void testName() {
    assertEquals("A", Basic.A.name());
    assertEquals("B", Basic.B.name());
    assertEquals("C", Basic.C.name());

    assertEquals("A", Complex.A.name());
    assertEquals("B", Complex.B.name());
    assertEquals("C", Complex.C.name());

    assertEquals("A", Subclassing.A.name());
    assertEquals("B", Subclassing.B.name());
    assertEquals("C", Subclassing.C.name());
  }

  public void testOrdinals() {
    assertEquals(0, Basic.A.ordinal());
    assertEquals(1, Basic.B.ordinal());
    assertEquals(2, Basic.C.ordinal());

    assertEquals(0, Complex.A.ordinal());
    assertEquals(1, Complex.B.ordinal());
    assertEquals(2, Complex.C.ordinal());

    assertEquals(0, Subclassing.A.ordinal());
    assertEquals(1, Subclassing.B.ordinal());
    assertEquals(2, Subclassing.C.ordinal());
  }

  public void testSwitch() {
    switch (Basic.A) {
      case A:
        break;
      case B:
        fail("Switch failed");
        break;
      case C:
        fail("Switch failed");
        break;
      default:
        fail("Switch failed");
        break;
    }
    switch (Complex.B) {
      case A:
        fail("Switch failed");
        break;
      case B:
        break;
      case C:
        fail("Switch failed");
        break;
      default:
        fail("Switch failed");
        break;
    }
    switch (Subclassing.C) {
      case A:
        fail("Switch failed");
        break;
      case B:
        fail("Switch failed");
        break;
      default:
        break;
    }
  }

  @SuppressWarnings("unchecked")
  public void testValueOf() {
    assertEquals(Basic.A, Basic.valueOf("A"));
    assertEquals(Basic.B, Basic.valueOf("B"));
    assertEquals(Basic.C, Basic.valueOf("C"));
    try {
      Basic.valueOf("D");
      fail("Basic.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }

    assertEquals(Complex.A, Complex.valueOf("A"));
    assertEquals(Complex.B, Complex.valueOf("B"));
    assertEquals(Complex.C, Complex.valueOf("C"));
    try {
      Complex.valueOf("D");
      fail("Complex.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }

    assertEquals(Subclassing.A, Subclassing.valueOf("A"));
    assertEquals(Subclassing.B, Subclassing.valueOf("B"));
    assertEquals(Subclassing.C, Subclassing.valueOf("C"));
    try {
      Subclassing.valueOf("D");
      fail("Subclassing.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    
    enumValuesTest(Basic.class);
    enumValuesTest(Complex.class);
    enumValuesTest(Subclassing.class);
    
    try {
      Enum.valueOf(Basic.class, "foo");
      fail("Passed an invalid enum constant name to Enum.valueOf; expected "
          + "IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    
    try {
      @SuppressWarnings("all")
      Class fakeEnumClass = String.class;
      Enum.valueOf(fakeEnumClass, "foo");
      fail("Passed a non enum class to Enum.valueOf; expected " 
              + "IllegalArgumentException");
    } catch (IllegalArgumentException e) {       
    }
     
    try {
      Class<Basic> nullEnumClass = null;
      Enum.valueOf(nullEnumClass, "foo");
      fail("Passed a null enum class to Enum.valueOf; expected "
          + "NullPointerException");
    } catch (JavaScriptException e) {
    } catch (NullPointerException e) {
    }
      
    try {
      Enum.valueOf(Basic.class, null);
      fail("Passed a null enum constant to Enum.valueOf; expected "
          + "NullPointerException");
    } catch (NullPointerException e) {
    }
  }
  
  public void testValueOfOverload() {
    BasicWithOverloadedValueOf val1 = Enum.valueOf(BasicWithOverloadedValueOf.class,"A");
    BasicWithOverloadedValueOf val2 = BasicWithOverloadedValueOf.valueOf("B");
    BasicWithOverloadedValueOf valById1 = BasicWithOverloadedValueOf.valueOf(1);
    BasicWithOverloadedValueOf valById2 = BasicWithOverloadedValueOf.valueOf(2);
    assertEquals(val1, valById1);
    assertEquals(val2, valById2);
  }

  public void testValues() {
    Basic[] simples = Basic.values();
    assertEquals(3, simples.length);
    assertEquals(Basic.A, simples[0]);
    assertEquals(Basic.B, simples[1]);
    assertEquals(Basic.C, simples[2]);

    Complex[] complexes = Complex.values();
    assertEquals(3, complexes.length);
    assertEquals(Complex.A, complexes[0]);
    assertEquals(Complex.B, complexes[1]);
    assertEquals(Complex.C, complexes[2]);

    Subclassing[] subs = Subclassing.values();
    assertEquals(3, subs.length);
    assertEquals(Subclassing.A, subs[0]);
    assertEquals(Subclassing.B, subs[1]);
    assertEquals(Subclassing.C, subs[2]);
  }

  /*
   * Test that a call to an enum instance method, which gets transformed to a
   * static impl, produces valid executable javascript, once the enum gets
   * ordinalized. This test is in response to a case where invalid javascript
   * was being generated, by not generating a clinit for the enum prior to
   * referencing a static impl method.
   */
  public void testEnumWithStaticImpl() {
    EnumWithStaticImpl ewsi = EnumWithStaticImpl.VALUE1;
    Long submitTime = new Long(1234567890);
    String fmtDate = ewsi.formatDate(submitTime);
    // we just need to make sure we get to this point without timing out
    assertTrue(fmtDate != null);
  }

  private <T extends Enum<T>> void enumValuesTest(Class<T> enumClass) {
    T[] constants = enumClass.getEnumConstants();
    for (T constant : constants) {
      assertEquals(constant, Enum.valueOf(enumClass, constant.name()));
    }
  }
}
