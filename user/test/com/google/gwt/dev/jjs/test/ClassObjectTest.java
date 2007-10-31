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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the web mode implementation of class literals.
 */
public class ClassObjectTest extends GWTTestCase {

  private static enum Bar {
    BAR;
  }

  private static class Foo implements IFoo {
  }

  private static interface IFoo {
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArray() {
    Object o = new Foo[3];
    assertEquals("[Lcom.google.gwt.dev.jjs.test.ClassObjectTest$Foo;",
        o.getClass().getName());
    assertEquals("class [Lcom.google.gwt.dev.jjs.test.ClassObjectTest$Foo;",
        o.getClass().toString());
    assertTrue(o.getClass().isArray());
    assertFalse(o.getClass().isEnum());
    assertFalse(o.getClass().isInterface());
    assertFalse(o.getClass().isPrimitive());
  }

  public void testClass() {
    Object o = new Foo();
    assertEquals(Foo.class, o.getClass());
    assertEquals("com.google.gwt.dev.jjs.test.ClassObjectTest$Foo",
        Foo.class.getName());
    assertEquals("class com.google.gwt.dev.jjs.test.ClassObjectTest$Foo",
        Foo.class.toString());
    assertFalse(Foo.class.isArray());
    assertFalse(Foo.class.isEnum());
    assertFalse(Foo.class.isInterface());
    assertFalse(Foo.class.isPrimitive());
  }

  public void testEnum() {
    Object o = Bar.BAR;
    assertEquals(Bar.class, o.getClass());
    assertEquals("com.google.gwt.dev.jjs.test.ClassObjectTest$Bar",
        o.getClass().getName());
    assertEquals("class com.google.gwt.dev.jjs.test.ClassObjectTest$Bar",
        o.getClass().toString());
    assertFalse(o.getClass().isArray());
    assertTrue(o.getClass().isEnum());
    assertFalse(o.getClass().isInterface());
    assertFalse(o.getClass().isPrimitive());
  }

  public void testInterface() {
    assertEquals("com.google.gwt.dev.jjs.test.ClassObjectTest$IFoo",
        IFoo.class.getName());
    assertEquals("interface com.google.gwt.dev.jjs.test.ClassObjectTest$IFoo",
        IFoo.class.toString());
    assertFalse(IFoo.class.isArray());
    assertFalse(IFoo.class.isEnum());
    assertTrue(IFoo.class.isInterface());
    assertFalse(IFoo.class.isPrimitive());
  }

  public void testPrimitive() {
    assertEquals("int", int.class.getName());
    assertEquals("int", int.class.toString());
    assertFalse(int.class.isArray());
    assertFalse(int.class.isEnum());
    assertFalse(int.class.isInterface());
    assertTrue(int.class.isPrimitive());
  }

}
