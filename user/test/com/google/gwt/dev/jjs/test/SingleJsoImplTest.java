/*
 * Copyright 2009 Google Inc.
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
import com.google.gwt.dev.jjs.test.SingleJsoImplTest.JsoHasInnerJsoType.InnerType;
import com.google.gwt.dev.jjs.test.jsointfs.JsoInterfaceWithUnreferencedImpl;
import com.google.gwt.junit.client.GWTTestCase;

import java.io.IOException;

/**
 * Ensures that JavaScriptObjects may implement interfaces with methods.
 */
public class SingleJsoImplTest extends GWTTestCase {

  interface Adder {
    int ADDER_CONST = 6;

    /**
     * @see SameDescriptors#SAME_NAME
     */
    String SAME_NAME = "Same Name";

    Object DIFFERENT_OBJECT = new Object();
    Object SAME_OBJECT = new Object();
    Object SAME_OBJECT2 = SAME_OBJECT;

    /*
     * NB: Picking a mix of double and int because doubles have size 2 on the
     * stack, so this ensures that we're handling that case correctly.
     */
    double add(double a, int b);

    /*
     * Ensure that we can return types whose sizes are larger than the size of
     * the arguments.
     */
    long returnLong();
  }

  interface CallsMethodInInnerType {
    interface InnerInterface {
      void call(int a);

      int get();
    }

    InnerInterface call(InnerInterface o, int a);
  }

  interface CallsStaticMethodInSubclass {
    String call(int a, int b);
  }

  interface Divider extends Multiplier {
    int divide(int a, int b);
  }

  static class JavaAdder implements Adder {
    public double add(double a, int b) {
      return a + b;
    }

    public long returnLong() {
      return 5L;
    }
  }

  /**
   * The extra declaration of implementing Multiplier should still be legal.
   */
  static class JavaDivider extends JavaMultiplier implements Divider,
      Multiplier, Tag {
    public int divide(int a, int b) {
      return a / b;
    }
  }

  /**
   * Ensure that SingleJsoImpl interfaces can be extended and implemented by
   * regular Java types.
   */
  static class JavaLog2 extends JavaDivider implements Log2 {
    public double log2(int a) {
      return Math.log(a) / Math.log(2);
    }
  }

  static class JavaMultiplier implements Multiplier {
    public int multiply(int a, int b) {
      return a * b;
    }
  }

  static class JsoAdder extends JavaScriptObject implements Adder {
    protected JsoAdder() {
    }

    public final native double add(double a, int b) /*-{
      return this.offset * (a + b);
    }-*/;

    public final long returnLong() {
      return 5L;
    }
  }

  static class JsoCallsStaticMethodInSubclass extends JavaScriptObject
      implements CallsStaticMethodInSubclass {
    protected JsoCallsStaticMethodInSubclass() {
    }

    public final native String call(int a, int b) /*-{
      return "foo" + @com.google.gwt.dev.jjs.test.SingleJsoImplTest.JsoCallsStaticMethodInSubclassSubclass::actual(II)(a, b);
    }-*/;
  }

  static class JsoCallsStaticMethodInSubclassSubclass extends
      JsoCallsStaticMethodInSubclass {
    public static String actual(int a, int b) {
      return String.valueOf(a + b);
    }

    protected JsoCallsStaticMethodInSubclassSubclass() {
    }
  }

  static class JsoDivider extends JsoMultiplier implements Divider, Tag {
    protected JsoDivider() {
    }

    public final native int divide(int a, int b) /*-{
      return this.offset * a / b;
    }-*/;
  }

  static class JsoHasInnerJsoType extends JavaScriptObject implements
      CallsMethodInInnerType {
    static class InnerType extends JavaScriptObject implements InnerInterface {
      protected InnerType() {
      }

      public final native void call(int a) /*-{
        this.foo = a;
      }-*/;

      public final native int get() /*-{
        return this.foo;
      }-*/;
    }

    protected JsoHasInnerJsoType() {
    }

    public final InnerInterface call(InnerInterface o, int a) {
      o.call(a);
      return o;
    }
  }

  static class JsoMultiplier extends JavaScriptObject implements Multiplier {
    protected JsoMultiplier() {
    }

    public final native int multiply(int a, int b) /*-{
      return this.offset * a * b;
    }-*/;
  }

  /**
   * Just a random JSO type for testing cross-casting.
   */
  final static class JsoRandom extends JavaScriptObject implements
      SameDescriptors, Tag {
    protected JsoRandom() {
    }

    public int add(int a, int b) {
      return -1;
    }

    public int divide(int a, int b) {
      return -1;
    }

    public int multiply(int a, int b) {
      return -1;
    }

    String b() {
      return "b";
    }
  }

  final static class JsoSimple extends JavaScriptObject implements Simple {
    protected JsoSimple() {
    }

    public String a() {
      return "a";
    }

    public String ex() throws IOException {
      throw new IOException();
    }

    public String rte() {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Ensure that SingleJsoImpl interfaces can be extended and implemented by
   * regular Java types.
   */
  interface Log2 extends Divider {
    double log2(int a);
  }

  interface Multiplier {
    int multiply(int a, int b);
  }

  /**
   * This interface makes sure that types with identical method signatures will
   * still dispatch correctly.
   */
  interface SameDescriptors {
    int SAME_NAME = 6;

    int add(int a, int b);

    int divide(int a, int b);

    int multiply(int a, int b);
  }

  interface Simple {
    String a();

    String ex() throws IOException;

    String rte();
  }

  /**
   * Ensure that a Java-only implementation of a SingleJsoImpl interface works.
   */
  static class SimpleOnlyJava implements SimpleOnlyJavaInterface {
    public String simpleOnlyJava() {
      return "simpleOnlyJava";
    }
  }

  interface SimpleOnlyJavaInterface {
    String simpleOnlyJava();
  }

  interface Tag {
  }

  private static native JsoAdder makeAdder(int offset) /*-{
    return {offset:offset};
  }-*/;

  private static native JsoDivider makeDivider(int offset) /*-{
    return {offset:offset};
  }-*/;

  private static native JsoMultiplier makeMultiplier(int offset) /*-{
    return {offset:offset};
  }-*/;

  private static native JsoSimple makeSimple() /*-{
    return {};
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testCallsToInnerTypes() {
    CallsMethodInInnerType a = (CallsMethodInInnerType) JavaScriptObject.createObject();
    InnerType i = (InnerType) JavaScriptObject.createObject();
    assertEquals(5, a.call(i, 5).get());
    assertEquals(5, i.get());
  }

  public void testDualCase() {
    // Direct dispatch
    {
      JavaAdder java = new JavaAdder();
      assertEquals(2.0, java.add(1, 1));

      JsoAdder jso = makeAdder(2);
      assertEquals(4.0, jso.add(1, 1));
      assertEquals(5L, jso.returnLong());
    }

    // Just check dispatch via the interface
    {
      Adder a = new JavaAdder();
      assertEquals(2.0, a.add(1, 1));

      a = makeAdder(2);
      assertEquals(4.0, a.add(1, 1));
      assertEquals(5L, a.returnLong());
    }

    // Check casting
    {
      Object a = new JavaAdder();
      assertEquals(2.0, ((Adder) a).add(1, 1));
      assertEquals(2.0, ((JavaAdder) a).add(1, 1));
      assertEquals(5L, ((Adder) a).returnLong());
      assertEquals(5L, ((JavaAdder) a).returnLong());
      assertTrue(a instanceof JavaAdder);
      assertFalse(a instanceof JsoAdder);
      assertFalse(a instanceof Tag);
      try {
        ((JsoAdder) a).add(1, 1);
        fail("Should have thrown CCE");
      } catch (ClassCastException e) {
        // OK
      }

      a = makeAdder(2);
      assertEquals(4.0, ((Adder) a).add(1, 1));
      assertEquals(4.0, ((JsoAdder) a).add(1, 1));
      assertEquals(5L, ((Adder) a).returnLong());
      assertEquals(5L, ((JsoAdder) a).returnLong());
      assertTrue(a instanceof JsoAdder);
      assertFalse(a instanceof JavaAdder);
      // NB: This is unexpected until you consider JSO$ as a roll-up type
      assertTrue(a instanceof Tag);
      try {
        ((JavaAdder) a).add(1, 1);
        fail("Should have thrown CCE");
      } catch (ClassCastException e) {
        // OK
      }
    }
  }

  public void testFields() {
    assertEquals(6, Adder.ADDER_CONST);
    assertEquals("Same Name", Adder.SAME_NAME);
    assertEquals(6, SameDescriptors.SAME_NAME);
    assertSame(Adder.SAME_OBJECT, Adder.SAME_OBJECT2);
    assertNotSame(Adder.DIFFERENT_OBJECT, Adder.SAME_OBJECT);
  }

  @SuppressWarnings("cast")
  public void testSimpleCase() {
    {
      JsoSimple asJso = makeSimple();
      assertTrue(asJso instanceof Object);
      assertTrue(asJso instanceof Simple);
      assertEquals("a", asJso.a());
      try {
        asJso.ex();
        fail("Should have thrown IOException");
      } catch (IOException e) {
        // OK
      }
      try {
        asJso.rte();
        fail("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        // OK
      }
    }

    {
      Simple asSimple = makeSimple();
      assertTrue(asSimple instanceof Object);
      assertTrue(asSimple instanceof JavaScriptObject);
      assertTrue(asSimple instanceof JsoSimple);
      assertEquals("a", asSimple.a());
      assertEquals("a", ((JsoSimple) asSimple).a());
      try {
        asSimple.ex();
        fail("Should have thrown IOException");
      } catch (IOException e) {
        // OK
      }
      try {
        asSimple.rte();
        fail("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        // OK
      }
    }

    {
      Object asObject = "Defeat type-tightening";
      assertTrue(asObject instanceof String);

      asObject = makeSimple();
      assertTrue(asObject instanceof Object);
      assertTrue(asObject instanceof JavaScriptObject);
      assertTrue(asObject instanceof JsoSimple);
      assertTrue(asObject instanceof Simple);
      assertEquals("a", ((Simple) asObject).a());
      assertEquals("a", ((JsoSimple) asObject).a());

      // Test a cross-cast that's normally not allowed by the type system
      assertTrue(asObject instanceof JsoRandom);
      assertEquals("b", ((JsoRandom) asObject).b());
      assertEquals(-1, ((JsoRandom) asObject).add(1, 1));
    }

    {
      Object o = "Defeat type-tightening";
      assertTrue(o instanceof String);
      o = new SimpleOnlyJava();
      assertTrue(o instanceof SimpleOnlyJavaInterface);
      assertEquals("simpleOnlyJava", ((SimpleOnlyJava) o).simpleOnlyJava());
    }
  }

  public void testStaticCallsToSubclasses() {
    Object o = "String";
    assertEquals(String.class, o.getClass());
    o = JavaScriptObject.createObject();
    assertTrue(o instanceof CallsStaticMethodInSubclass);
    assertEquals("foo5", ((CallsStaticMethodInSubclass) o).call(2, 3));
  }

  @SuppressWarnings("cast")
  public void testSubclassing() {
    {
      JsoDivider d = makeDivider(1);
      assertTrue(d instanceof Divider);
      assertTrue(d instanceof Multiplier);
      assertTrue(d instanceof Tag);
      assertEquals(5, d.divide(10, 2));
      assertEquals(10, d.multiply(5, 2));
      assertEquals(10, ((JsoMultiplier) d).multiply(5, 2));
    }

    {
      Object d = makeDivider(1);
      assertTrue(d instanceof Divider);
      assertTrue(d instanceof Multiplier);
      assertTrue(d instanceof JsoDivider);
      assertTrue(d instanceof JsoMultiplier);
      assertFalse(d instanceof JavaDivider);
      assertFalse(d instanceof JavaMultiplier);
      assertTrue(d instanceof Tag);

      assertEquals(5, ((Divider) d).divide(10, 2));
      assertEquals(10, ((Divider) d).multiply(5, 2));
      assertEquals(10, ((Multiplier) d).multiply(5, 2));
      assertEquals(5, ((JsoDivider) d).divide(10, 2));
      assertEquals(10, ((JsoMultiplier) d).multiply(5, 2));

      d = new JavaDivider();
      assertTrue(d instanceof Divider);
      assertTrue(d instanceof Multiplier);
      assertTrue(d instanceof JavaDivider);
      assertTrue(d instanceof JavaMultiplier);
      assertFalse(d instanceof JsoDivider);
      assertFalse(d instanceof JsoMultiplier);
      assertTrue(d instanceof Tag);

      assertEquals(5, ((Divider) d).divide(10, 2));
      assertEquals(10, ((Divider) d).multiply(5, 2));
      assertEquals(10, ((Multiplier) d).multiply(5, 2));
      assertEquals(5, ((JavaDivider) d).divide(10, 2));
      assertEquals(10, ((JavaMultiplier) d).multiply(5, 2));
    }

    {
      Object m = makeMultiplier(1);
      // This only works because JSO$ implements every SingleJsoImpl interface
      assertTrue(m instanceof Divider);
      assertEquals(2, ((Divider) m).divide(10, 5));

      m = new JavaMultiplier();
      // but this should fail, since there's proper type information
      assertFalse(m instanceof Divider);
      try {
        assertEquals(2, ((Divider) m).divide(10, 5));
        fail("Should have thrown CCE");
      } catch (ClassCastException e) {
        // OK
      }
    }

    {
      Object l2 = "Prevent type-tightening";
      assertTrue(l2 instanceof String);

      l2 = new JavaLog2();
      assertTrue(l2 instanceof Log2);
      assertTrue(l2 instanceof Multiplier);
      assertTrue(l2 instanceof Divider);
      assertEquals(4.0, ((Log2) l2).log2(16));
    }
  }

  public void testUnreferencedType() {
    JsoInterfaceWithUnreferencedImpl o = (JsoInterfaceWithUnreferencedImpl) JavaScriptObject.createObject();
    assertNotNull(o);
    assertTrue(o.isOk());
  }
}
