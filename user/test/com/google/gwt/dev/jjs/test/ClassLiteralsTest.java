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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the Production Mode implementation of class literals.
 */
public class ClassLiteralsTest extends GWTTestCase {

  private static enum Bar {
    BAR, BAZ {
      @Override
      public String toString() {
        return "BAZ!";
      }
    };
  }

  private static class Foo implements IFoo {
  }

  private static interface IFoo {
  }

  private static void assertArrayEquals(Object[] expected, Object[] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], actual[i]);
    }
  }

  private static class My$Class {
    private static class My$InnerClass {
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArray() {
    Object o = new Foo[3];
    assertEquals(Foo[].class, o.getClass());
    if (expectClassMetadata()) {
      assertEquals(Object.class, o.getClass().getSuperclass());
    }

    assertEquals("[L" + Foo.class.getName() + ";", o.getClass().getName());
    assertEquals(Foo.class.getCanonicalName() + "[]", o.getClass().getCanonicalName());
    assertEquals(Foo.class.getSimpleName() + "[]", o.getClass().getSimpleName());
    assertEquals("class [L" + Foo.class.getName() + ";", o.getClass().toString());

    assertTrue(o.getClass().isArray());
    assertFalse(o.getClass().isEnum());
    assertFalse(o.getClass().isInterface());
    assertFalse(o.getClass().isPrimitive());
    assertNull(o.getClass().getEnumConstants());

    Foo[][] f = new Foo[3][3];
    assertEquals(Foo[][].class, f.getClass());
    assertEquals(Foo[].class, f[0].getClass());
  }

  public void testAssertionStatus() {
    boolean assertionStatus = ClassLiteralsTest.class.desiredAssertionStatus();
    try {
      assert false;
      assertFalse(assertionStatus);
    } catch (AssertionError e) {
      assertTrue(assertionStatus);
    }
  }

  public void testClass() {
    Object o = new Foo();
    assertEquals(Foo.class, o.getClass());
    if (expectClassMetadata()) {
      assertEquals(Object.class, o.getClass().getSuperclass());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest$Foo",
          Foo.class.getName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest.Foo",
          Foo.class.getCanonicalName());
      assertEquals("Foo", Foo.class.getSimpleName());
      assertEquals("class com.google.gwt.dev.jjs.test.ClassLiteralsTest$Foo",
          Foo.class.toString());
    }
    assertFalse(Foo.class.isArray());
    assertFalse(Foo.class.isEnum());
    assertFalse(Foo.class.isInterface());
    assertFalse(Foo.class.isPrimitive());
    assertNull(o.getClass().getEnumConstants());
  }

  public void testCloneClassLiteral() {
    // getBarClass() should inline, causing a clone of a class literal
    if (expectClassMetadata()) {
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest$Bar",
          getBarClass().getName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest.Bar",
          getBarClass().getCanonicalName());
    }
  }

  public void testDollarInName() {
    if (expectClassMetadata()) {
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest$My$Class",
          My$Class.class.getName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest.My$Class",
          My$Class.class.getCanonicalName());
      assertEquals("My$Class", My$Class.class.getSimpleName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest$My$Class$My$InnerClass",
          My$Class.My$InnerClass.class.getName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest.My$Class.My$InnerClass",
          My$Class.My$InnerClass.class.getCanonicalName());
      assertEquals("My$InnerClass",
          My$Class.My$InnerClass.class.getSimpleName());
      assertEquals("[Lcom.google.gwt.dev.jjs.test.ClassLiteralsTest$My$Class;",
          My$Class[].class.getName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest.My$Class[]",
          My$Class[].class.getCanonicalName());
      assertEquals("My$Class[]", My$Class[].class.getSimpleName());
      assertEquals("[Lcom.google.gwt.dev.jjs.test.ClassLiteralsTest$My$Class$My$InnerClass;",
          My$Class.My$InnerClass[].class.getName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest.My$Class.My$InnerClass[]",
          My$Class.My$InnerClass[].class.getCanonicalName());
      assertEquals("My$InnerClass[]", My$Class.My$InnerClass[].class.getSimpleName());
    }
  }

  public void testEnum() {
    Object o = Bar.BAR;
    assertEquals(Bar.class, o.getClass());
    if (expectClassMetadata()) {
      assertEquals(Enum.class, o.getClass().getSuperclass());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest$Bar",
          o.getClass().getName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest.Bar",
          o.getClass().getCanonicalName());
      assertEquals("Bar",
          o.getClass().getSimpleName());
      assertEquals("class com.google.gwt.dev.jjs.test.ClassLiteralsTest$Bar",
          o.getClass().toString());
    }
    assertFalse(o.getClass().isArray());
    assertTrue(o.getClass().isEnum());
    assertFalse(o.getClass().isInterface());
    assertFalse(o.getClass().isPrimitive());
    assertArrayEquals(Bar.values(), o.getClass().getEnumConstants());
  }

  public void testEnumSubclass() {
    Object o = Bar.BAZ;
    assertNotSame("Classes unexpectedly the same", Bar.class, o.getClass());
    if (expectClassMetadata()) {
      assertEquals(Bar.class, o.getClass().getSuperclass());
      /*
       * TODO: implement
       */
      // assertEquals(Bar.class, o.getClass().getDeclaringClass());
      assertTrue(o.getClass().getName().endsWith("$1"));
      assertTrue(o.getClass().toString().endsWith("$1"));
    }
    assertFalse("Should not be array", o.getClass().isArray());
    assertFalse("Should not be enum", o.getClass().isEnum());
    assertFalse("Should not be interface", o.getClass().isInterface());
    assertFalse("Should not be primitive", o.getClass().isPrimitive());
    assertNull("Constands should be null", o.getClass().getEnumConstants());
  }

  public void testInterface() {
    assertNull(IFoo.class.getSuperclass());
    if (expectClassMetadata()) {
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest$IFoo", IFoo.class.getName());
      assertEquals("com.google.gwt.dev.jjs.test.ClassLiteralsTest.IFoo",
          IFoo.class.getCanonicalName());
      assertEquals("IFoo", IFoo.class.getSimpleName());
      assertEquals("interface com.google.gwt.dev.jjs.test.ClassLiteralsTest$IFoo",
          IFoo.class.toString());
    }
    assertFalse(IFoo.class.isArray());
    assertFalse(IFoo.class.isEnum());
    assertTrue(IFoo.class.isInterface());
    assertFalse(IFoo.class.isPrimitive());
    assertNull(IFoo.class.getEnumConstants());
  }

  public void testPrimitive() {
    assertNull(int.class.getSuperclass());
    if (expectClassMetadata()) {
      assertEquals("int", int.class.getName());
      assertEquals("int", int.class.getCanonicalName());
      // TODO(rluble): getSimpleName() for primitives does not comp[y with the Java standard.
      // assertEquals("int", int.class.getSimpleName());
      assertEquals("int", int.class.toString());
    }
    assertFalse(int.class.isArray());
    assertFalse(int.class.isEnum());
    assertFalse(int.class.isInterface());
    assertTrue(int.class.isPrimitive());
    assertNull(int.class.getEnumConstants());
  }

  private static native Class<?> getClassLiteral(Object o) /*-{
    return o.@java.lang.Object::getClass()();
  }-*/;

  public void testGetClassCallThroughJsni() {
    assertEquals(Object.class, getClassLiteral(new Object()));
  }

  private static class JSO extends JavaScriptObject {
    protected JSO() {
    }
  }

  private static native JSO getJSO() /*-{
    return {};
  }-*/;

  public void testSpecialClassLiterals() {
    if (expectClassMetadata()) {
      assertEquals("com.google.gwt.core.client.JavaScriptObject$", JSO.class.getName());
      assertEquals("[[Lcom.google.gwt.core.client.JavaScriptObject$;", JSO[][].class.getName());
      assertEquals("[Lcom.google.gwt.core.client.JavaScriptObject$;",
          new JSO[3].getClass().getName());
      assertEquals("com.google.gwt.core.client.JavaScriptObject$", getJSO().getClass().getName());
    }
  }

  private boolean expectClassMetadata() {
    String name = Object.class.getName();

    if (name.equals("java.lang.Object")) {
      return true;
    } else if (name.startsWith("Class$")) {
      return false;
    }

    throw new RuntimeException("Unexpected class name " + name);
  }

  private Class<? extends Bar> getBarClass() {
    return Bar.class;
  }
}
