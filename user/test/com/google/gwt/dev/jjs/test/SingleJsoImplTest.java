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
import com.google.gwt.core.client.SingleJsoImpl;
import com.google.gwt.dev.jjs.test.SingleJsoImplTest.JsoHasInnerJsoType.InnerType;
import com.google.gwt.dev.jjs.test.jsointfs.JsoInterfaceWithUnreferencedImpl;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  @SingleJsoImpl(JsoCreatedWithCast.class)
  interface CreatedWithCast {
    String foo();
  }

  @SingleJsoImpl(JsoCreatedWithCastToTag.class)
  interface CreatedWithCastToTag {
  }

  interface CreatedWithCastToTagSub extends CreatedWithCastToTag {
    String foo();
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
   * Even though this type is never instantiated, it's necessary to ensure that
   * the CreatedWithCast test isn't short-circuited due to type tightening.
   */
  static class JavaCreatedWithCast implements CreatedWithCast {
    public String foo() {
      return "foo";
    }
  }

  static class JavaCreatedWithCastToTag implements CreatedWithCastToTagSub {
    public String foo() {
      return "foo";
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

  static class JavaUsesArrays implements UsesArrays {
    public void acceptInt3Array(int[][][] arr) {
      assertTrue(arr.length == 3);
    }

    public void acceptIntArray(int[] arr) {
      assertTrue(arr.length == 1);
    }

    public void acceptObject3Array(Object[][][] arr) {
      assertTrue(arr.length == 3);
    }

    public void acceptObjectArray(Object[] arr) {
      assertTrue(arr.length == 1);
    }

    public void acceptString3Array(String[][][] arr) {
      assertTrue(arr.length == 3);
    }

    public void acceptStringArray(String[] arr) {
      assertTrue(arr.length == 1);
    }

    public int[][][] returnInt3Array() {
      return new int[3][2][1];
    }

    public int[] returnIntArray() {
      return new int[1];
    }

    public Object[][][] returnObject3Array() {
      return new Object[3][2][1];
    }

    public Object[] returnObjectArray() {
      return new Object[1];
    }

    public String[][][] returnString3Array() {
      return new String[3][2][1];
    }

    public String[] returnStringArray() {
      return new String[1];
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

  static class JsoCreatedWithCast extends JavaScriptObject implements
      CreatedWithCast {
    protected JsoCreatedWithCast() {
    }

    public final String foo() {
      return "foo";
    }
  }

  static class JsoCreatedWithCastToTag extends JavaScriptObject implements
      CreatedWithCastToTagSub {
    protected JsoCreatedWithCastToTag() {
    }

    public final String foo() {
      return "foo";
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

    public String a(boolean overload) {
      return overload ? "Kaboom!" : "OK";
    }

    public String ex() throws IOException {
      throw new IOException();
    }

    public String rte() {
      throw new IllegalArgumentException();
    }
  }

  static final class JsoUsesArrays extends JavaScriptObject implements
      UsesArrays {
    protected JsoUsesArrays() {
    }

    public void acceptInt3Array(int[][][] arr) {
      assertTrue(arr.length == 3);
    }

    public void acceptIntArray(int[] arr) {
      assertTrue(arr.length == 1);
    }

    public void acceptObject3Array(Object[][][] arr) {
      assertTrue(arr.length == 3);
    }

    public void acceptObjectArray(Object[] arr) {
      assertTrue(arr.length == 1);
    }

    public void acceptString3Array(String[][][] arr) {
      assertTrue(arr.length == 3);
    }

    public void acceptStringArray(String[] arr) {
      assertTrue(arr.length == 1);
    }

    public int[][][] returnInt3Array() {
      return new int[3][2][1];
    }

    public int[] returnIntArray() {
      return new int[1];
    }

    public Object[][][] returnObject3Array() {
      return new Object[3][2][1];
    }

    public Object[] returnObjectArray() {
      return new Object[1];
    }

    public String[][][] returnString3Array() {
      return new String[3][2][1];
    }

    public String[] returnStringArray() {
      return new String[1];
    }
  }

  static class JsoUsesGeneric extends JavaScriptObject implements UsesGenerics {
    public static native <T extends CharSequence> JsoUsesGeneric create() /*-{
      return {suffix : "42"};
    }-*/;

    protected JsoUsesGeneric() {
    }

    public final native <T> String acceptsGeneric(T chars) /*-{
      return chars + this.suffix;
    }-*/;

    public final native <T> void callback(AsyncCallback<T> callback, T chars) /*-{
      callback.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(chars + this.suffix);
    }-*/;

    /**
     * What you're seeing here is would be achieved by an unsafe (T) cast and
     * would break with a ClassCastException if accessed via JsoIsGenericFinal
     * in normal Java.
     */
    public final native <T> T returnsGeneric(String chars) /*-{
      return chars + this.suffix;
    }-*/;
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

    String a(boolean overload);

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

  /**
   * Ensure that arrays are valid return and parameter types.
   */
  interface UsesArrays {
    void acceptInt3Array(int[][][] arr);

    void acceptIntArray(int[] arr);

    void acceptObject3Array(Object[][][] arr);

    void acceptObjectArray(Object[] arr);

    void acceptString3Array(String[][][] arr);

    void acceptStringArray(String[] arr);

    int[][][] returnInt3Array();

    int[] returnIntArray();

    Object[][][] returnObject3Array();

    Object[] returnObjectArray();

    String[][][] returnString3Array();

    String[] returnStringArray();
  }

  interface UsesGenerics {
    <T extends Object> String acceptsGeneric(T chars);

    <T> void callback(AsyncCallback<T> callback, T chars);

    <T> T returnsGeneric(String chars);
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

  private Object asObject;

  private Simple asSimple;

  private Adder asAdder;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testCallsToInnerTypes() {
    JsoHasInnerJsoType o = JavaScriptObject.createObject().cast();
    CallsMethodInInnerType a = (CallsMethodInInnerType) JavaScriptObject.createObject();
    InnerType i = (InnerType) JavaScriptObject.createObject();
    assertEquals(5, a.call(i, 5).get());
    assertEquals(5, i.get());
  }

  public void testCallsWithArrays() {
    JsoUsesArrays toss = JavaScriptObject.createObject().cast();
    UsesArrays a = JavaScriptObject.createObject().<JsoUsesArrays> cast();
    a.acceptIntArray(a.returnIntArray());
    a.acceptInt3Array(a.returnInt3Array());
    a.acceptStringArray(a.returnStringArray());
    a.acceptString3Array(a.returnString3Array());
    a.acceptObjectArray(a.returnStringArray());
    a.acceptObject3Array(a.returnString3Array());

    a = new JavaUsesArrays();
    a.acceptIntArray(a.returnIntArray());
    a.acceptInt3Array(a.returnInt3Array());
    a.acceptStringArray(a.returnStringArray());
    a.acceptString3Array(a.returnString3Array());
    a.acceptObjectArray(a.returnStringArray());
    a.acceptObject3Array(a.returnString3Array());
  }

  public void testClassLiterals() {
    JavaScriptObject jso = makeSimple();
    assertEquals(JavaScriptObject.class, jso.getClass());
    assertEquals(JavaScriptObject.class, asSimple((JsoSimple) jso).getClass());
  }

  /**
   * Ensure that SingleJSO types that are referred to only via a cast to the
   * interface type are retained. If the JsoCreatedWithCast type isn't rescued
   * correctly, the cast in this test will throw a ClassCastException since the
   * compiler would assume there are types that implement the interface.
   */
  public void testCreatedWithCast() {
    // This can't work in hosted mode, we need something to load the JSO
    try {
      Object a = (CreatedWithCast) JavaScriptObject.createObject();
    } catch (ClassCastException e) {
      fail("a");
    }
    try {
      Object b = (CreatedWithCastToTag) JavaScriptObject.createObject();
    } catch (ClassCastException e) {
      fail("b");
    }
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
      // assertTrue(a instanceof Tag);
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

  public void testGenerics() {
    UsesGenerics j = JsoUsesGeneric.create();
    assertEquals("Hello42", j.acceptsGeneric("Hello"));
    assertEquals("Hello42", j.returnsGeneric("Hello"));
    j.callback(new AsyncCallback<CharSequence>() {
      public void onFailure(Throwable caught) {
      }

      public void onSuccess(CharSequence result) {
        assertEquals("Hello42", result);
      }
    }, "Hello");
  }

  /**
   * Test identity with arrays.
   */
  public void testIdentityArrays() {
    // XXX Really need @SingleJsoImpl annotation here
    makeDivider(1);
    makeAdder(1);
    JavaScriptObject obj = makeSimple();
    Adder[] adders = {(Adder) obj};
    Divider[] dividers = {(Divider) obj};
    assertSame(adders[0], dividers[0]);

    Object[] arr = {adders[0], dividers[0]};
    assertSame(arr[0], arr[1]);

    List<Adder> adderList = Arrays.asList(adders);
    List<Divider> dividerList = Arrays.asList(dividers);
    assertSame(adderList.get(0), dividerList.get(0));

    @SuppressWarnings("unchecked")
    List adderListRaw = Arrays.asList(adders);
    @SuppressWarnings("unchecked")
    List dividerListRaw = Arrays.asList(dividers);
    assertSame(adderListRaw.get(0), dividerListRaw.get(0));
  }

  /**
   * Pass some SingleJsoImpl types into JRE collections.
   */
  public void testIdentityJRE() {
    IdentityHashMap<JavaScriptObject, Boolean> jMap = new IdentityHashMap<JavaScriptObject, Boolean>();
    IdentityHashMap<Simple, Boolean> sMap = new IdentityHashMap<Simple, Boolean>();

    JsoSimple jso = makeSimple();
    jMap.put(jso, true);
    sMap.put(jso, true);

    assertTrue(jMap.get(jso));
    assertTrue(sMap.get(jso));

    assertTrue(jMap.get(asSimple(jso)));
    assertTrue(sMap.get(asSimple(jso)));
    assertTrue(jMap.get(asSimple));
    assertTrue(sMap.get(asSimple));

    assertTrue(jMap.get(asObject(jso)));
    assertTrue(sMap.get(asObject(jso)));
    assertTrue(jMap.get(asObject));
    assertTrue(sMap.get(asObject));

    assertNull(jMap.get(JavaScriptObject.createObject()));
    assertNull(sMap.get(JavaScriptObject.createObject()));
  }

  /**
   * Pass some SingleJsoImpl types into an IdentityHashMap.
   */
  public void testIdentityJRE2() {
    final JavaScriptObject jso = JavaScriptObject.createObject();
    IdentityHashMap<Object, Boolean> map = new IdentityHashMap<Object, Boolean>();

    // First with a proper JSO subtype
    map.putAll(new AbstractMap<JsoAdder, Boolean>() {
      @Override
      public Set<Entry<JsoAdder, Boolean>> entrySet() {
        Entry<JsoAdder, Boolean> entry = new Entry<JsoAdder, Boolean>() {
          public JsoAdder getKey() {
            return (JsoAdder) jso;
          }

          public Boolean getValue() {
            return true;
          }

          public Boolean setValue(Boolean value) {
            throw new RuntimeException("unimplemented");
          }
        };
        return Collections.singleton(entry);
      }
    });

    assertEquals(1, map.size());
    assertNotNull(map.get(jso));
    assertTrue(map.get(jso));
    assertTrue(map.get((Adder) jso));
    assertTrue(map.get(asAdder((Adder) jso)));
    assertTrue(map.get((JsoAdder) jso));
    assertTrue(map.get((JsoSimple) jso));

    // Now with an interface type
    map.putAll(new AbstractMap<Adder, Boolean>() {
      @Override
      public Set<Entry<Adder, Boolean>> entrySet() {
        Entry<Adder, Boolean> entry = new Entry<Adder, Boolean>() {
          public Adder getKey() {
            return (Adder) jso;
          }

          public Boolean getValue() {
            return false;
          }

          public Boolean setValue(Boolean value) {
            throw new RuntimeException("unimplemented");
          }
        };
        return Collections.singleton(entry);
      }
    });

    assertEquals(1, map.size());
    assertNotNull(map.get(jso));
    assertFalse(map.get(jso));

    assertNull(map.get(JavaScriptObject.createObject()));

    map.put(JavaScriptObject.createObject(), true);
    assertFalse(map.remove(jso));
    assertEquals(1, map.size());
    assertTrue(map.values().iterator().next());
  }

  public void testIdentityMethodsAndFields() {
    JsoSimple jso = makeSimple();
    JsoSimple jso2 = makeSimple();
    assertNotSame(jso, jso2);
    assertTrue(jso != jso2);
    assertFalse(jso == jso2);

    Object o = asObject(jso);
    Object o2 = asObject(jso2);
    assertNotSame(o, o2);
    assertTrue(o != o2);
    assertFalse(o == o2);

    assertSame(jso, o);
    assertTrue(jso == o);
    assertFalse(jso != o);

    assertSame(jso2, o2);
    assertTrue(jso2 == o2);
    assertFalse(jso2 != o2);

    assertSame(asObject, jso2);
    assertTrue(asObject == jso2);
    assertFalse(asObject != jso2);

    assertSame(asObject, o2);
    assertTrue(asObject == o2);
    assertFalse(asObject != o2);

    Simple s = asSimple(jso);
    Simple s2 = asSimple(jso2);
    assertNotSame(s, s2);
    assertTrue(s != s2);
    assertFalse(s == s2);

    assertSame(jso, s);
    assertTrue(jso == s);
    assertFalse(jso != s);

    assertSame(s, o);
    assertTrue(s == o);
    assertFalse(s != o);

    assertSame(jso2, s2);
    assertTrue(jso2 == s2);
    assertFalse(jso2 != s2);

    assertSame(o2, s2);
    assertTrue(o2 == s2);
    assertFalse(o2 != s2);

    assertSame(asSimple, jso2);
    assertTrue(asSimple == jso2);
    assertFalse(asSimple != jso2);

    assertSame(asSimple, o2);
    assertTrue(asSimple == o2);
    assertFalse(asSimple != o2);

    assertSame(asSimple, s2);
    assertTrue(asSimple == s2);
    assertFalse(asSimple != s2);

    JsoRandom r = (JsoRandom) o2;
    assertSame(r, jso2);
    // Can't legally compare r == jso2

    assertSame(r, o2);
    assertTrue(r == o2);
    assertFalse(r != o2);

    assertSame(r, asObject);
    assertTrue(r == asObject);
    assertFalse(r != asObject);

    assertSame(r, s2);
    // Can't legally compare r == s2

    assertSame(r, asSimple);
    // Can't legally compare r == asSimple
  }

  public void testIdentityWithDualTypes() {
    JsoAdder jso = makeAdder(0);
    JavaAdder java = new JavaAdder();

    assertNotSame(jso, java);

    IdentityHashMap<Adder, Integer> map = new IdentityHashMap<Adder, Integer>();
    map.put(jso, 0);
    map.put(java, 1);
    assertEquals(2, map.size());

    assertEquals(new Integer(0), map.get(jso));
    assertEquals(new Integer(1), map.get(java));

    // Use unambiguous dispatch
    assertEquals(new Integer(0), map.get(asAdder(jso)));
    assertEquals(new Integer(0), map.get(asAdder));
    assertEquals(new Integer(1), map.get(asAdder(java)));
    assertEquals(new Integer(1), map.get(asAdder));

    // Use ambiguous dispatch
    assertEquals(new Integer(0), map.get(asAdder((Adder) jso)));
    assertEquals(new Integer(0), map.get(asAdder));
    assertEquals(new Integer(1), map.get(asAdder((Adder) java)));
    assertEquals(new Integer(1), map.get(asAdder));

    // Test with plain Object
    assertEquals(new Integer(0), map.get(asObject(jso)));
    assertEquals(new Integer(0), map.get(asObject));
    assertEquals(new Integer(1), map.get(asObject(java)));
    assertEquals(new Integer(1), map.get(asObject));

    // Behavior of keys
    for (Map.Entry<Adder, Integer> entry : map.entrySet()) {
      if (entry.getKey() == jso) {
        assertEquals(new Integer(0), entry.getValue());
      }
      if (entry.getKey() == java) {
        assertEquals(new Integer(1), entry.getValue());
      }
    }
  }

  public void testIdentityWithNativeMethods() {
    JsoSimple s = makeSimple();
    Simple s2 = asSimpleNative(s);
    assertSame(s, s2);
    assertSame(s, asSimple);
    assertSame(s2, asSimple);
  }

  /**
   * Called from asSimpleNative to continue the above test.
   */
  public void testIdentityWithNativeMethods(JsoSimple o) {
    assertSame(o, asSimple);
  }

  @SuppressWarnings("cast")
  public void testSimpleCase() {
    {
      JsoSimple asJso = makeSimple();
      assertTrue(asJso instanceof Object);
      assertTrue(asJso instanceof Simple);
      assertEquals("a", asJso.a());
      assertEquals("OK", asJso.a(false));
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
      assertEquals("OK", asSimple.a(false));
      assertEquals("a", ((JsoSimple) asSimple).a());
      assertEquals("OK", ((JsoSimple) asSimple).a(false));
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
      assertEquals("OK", ((Simple) asObject).a(false));
      assertEquals("OK", ((JsoSimple) asObject).a(false));

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
    JsoCallsStaticMethodInSubclass a = JavaScriptObject.createObject().cast();
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

  /**
   * This test relies on the SingleJsoimpl annotation in dev mode.
   */
  public void testUnreferencedType() {
    JsoInterfaceWithUnreferencedImpl o = (JsoInterfaceWithUnreferencedImpl) JavaScriptObject.createObject();
    assertNotNull(o);
    assertTrue(o.isOk());
  }

  protected Adder asAdder(Adder o) {
    asAdder = o;
    return o;
  }

  /*
   * These asXYZ methods are to test cast-via-return and cast-via-assignment
   * rewriting.
   */

  protected Adder asAdder(JavaAdder o) {
    asAdder = o;
    return o;
  }

  protected Adder asAdder(JsoAdder o) {
    asAdder = o;
    return o;
  }

  protected Object asObject(JavaAdder jso) {
    asObject = jso;
    return jso;
  }

  protected Object asObject(JsoAdder jso) {
    asObject = jso;
    return jso;
  }

  protected Object asObject(JsoSimple jso) {
    asObject = jso;
    return jso;
  }

  protected Simple asSimple(JsoSimple jso) {
    asSimple = jso;
    return jso;
  }

  private native Simple asSimpleNative(Object o) /*-{
    // Make sure glue code does the interface cast
    this.@com.google.gwt.dev.jjs.test.SingleJsoImplTest::asSimple = o;
    this.@com.google.gwt.dev.jjs.test.SingleJsoImplTest::testIdentityWithNativeMethods(Lcom/google/gwt/dev/jjs/test/SingleJsoImplTest$JsoSimple;)(o);
    return o;
  }-*/;
}
