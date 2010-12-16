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
/**********************
 * * DO NOT FORMAT * *
 **********************/
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests several tricky aspects of Development Mode.
 */
public class HostedTest extends GWTTestCase {

  /**
   * Tests that we can use a source level name for a nested type instead of the
   * binary name.
   */
  static class A {

    static class B {
      int b = 1;

      public native int getUsingBinaryRef() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest$A$B::b;
      }-*/;

      public native int getUsingSourceRef() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest.A.B::b;
      }-*/;
    }
  }

  private static class TestCovariantChild extends TestCovariantSuper {
    @Override
    public native String foo(String val) /*-{
      return val;
    }-*/;
  }

  private static class TestCovariantSuper {
    public native Object foo(String val) /*-{
      return val;
    }-*/;
  }

  private static enum TestEnum {
    VAL1, VAL2, VAL3() {
      @Override
      public native String foo() /*-{
        return "VAL3-foo";
      }-*/;
    };

    public static native String sFoo() /*-{
      return "sFoo";
    }-*/;

    public native String foo() /*-{
      return "foo";
    }-*/;
  }

  private static class TestGenericList extends AbstractList<Object> {
    @Override
    public Object get(int index) {
      return this;
    }

    @Override
    public int size() {
      return 42;
    }
  }

  static String sFoo(String s) {
    return s + "foo";
  }

  private native static JavaScriptObject getBoxedBooleanAsObject(boolean v) /*-{
    return new Boolean(v);
  }-*/;

  private native static JavaScriptObject getBoxedNumberAsObject(double v) /*-{
    return new Number(v);
  }-*/;

  private native static String getBoxedStringAsString(String v) /*-{
    return new String(v);
  }-*/;

  private native static double getDouble(double v) /*-{
    return -v;
  }-*/;

  private static native float getFloat() /*-{
    return myFloatValue;
  }-*/;

  private static native float getFloatString() /*-{
    return Number(myFloatValue.toString());
  }-*/;

  private native static int getInt(int v) /*-{
    return -v;
  }-*/;

  // this should cause an exception
  private static native Object getIntAsObject() /*-{
    return 5;
  }-*/;

  private native static String getJSOAsString(JavaScriptObject jso) /*-{
    return "" + jso;
  }-*/;

  private native static Object getObject(Object o) /*-{
    return o;
  }-*/;

  private static native JavaScriptObject getsFooFunc() /*-{
    return @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;);
  }-*/;

  private native static String getString(String s) /*-{
    return s + "me";
  }-*/;

  // ok to return JS string from an Object method
  private static native Object getStringAsObject() /*-{
    return "test";
  }-*/;

  private native static int getStringLength(String s) /*-{
    return s.length;
  }-*/;

  private static native boolean jsIdentical(Object o1, Object o2) /*-{
    return o1 === o2;
  }-*/;

  private static native boolean jsniInstanceFunctionsIdentical(Object o) /*-{
    return o.@java.lang.Object::toString() === o.@java.lang.Object::toString();
  }-*/;

  private static native boolean jsniStaticFunctionsIdentical() /*-{
    return @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;) === @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;);
  }-*/;

  private static native String sFooCall(String s) /*-{
    var func = @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;);
    return func.call(null, s);
  }-*/;

  private static native String sFooDirect(String s) /*-{
    return @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;)(s);
  }-*/;

  private static native String sFooFuncAsStr() /*-{
    var f = @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;);
    return "" + f;
  }-*/;

  private static native String sFooFuncToString() /*-{
    var f = @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;);
    return f.toString();
  }-*/;

  private static native String sFooInvoke(String s) /*-{
    var func = @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;);
    return func(s);
  }-*/;

  private static native String sFooRoundTrip(JavaScriptObject fooFunc, String s) /*-{
    return fooFunc(s);
  }-*/;

  private static native void storeFloat(float f) /*-{
    myFloatValue = f;
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testAssertionsAlwaysOn() {
    if (!GWT.isScript()) {
      assertTrue(HostedTest.class.desiredAssertionStatus());
    }
  }

  /*
   * Test that returning JavaScript boxed primitives works as expected. Note
   * that Boolean and Number cannot be supported properly in Production Mode, so
   * we do not support it in Development Mode and therefore do not test it here.
   */
  public void testAutoBoxing() {
    JavaScriptObject bvo = getBoxedBooleanAsObject(true);
    assertEquals(getJSOAsString(bvo), "true");
    JavaScriptObject nvo = getBoxedNumberAsObject(42);
    assertEquals(getJSOAsString(nvo), "42");
    String sv = getBoxedStringAsString("test");
    assertEquals(sv, "test");
  }

  public void testBasic() {
    int iv = getInt(14);
    assertEquals(iv, -14);
    double dv = getDouble(31.5);
    assertEquals(dv, -31.5, 0.0);
    String sv = getString("test");
    assertEquals(sv, "testme");
    Object oin = String.class;
    Object oout = getObject(oin);
    assertEquals(oin, oout);
  }

  public void testByteMarshalling() {
    byte b = 100;
    assertEquals(100, byteAsInt(b));
    b = -125;
    assertEquals(-125, byteAsInt(b));
  }

  public void testCovariant() {
    TestCovariantSuper parent = new TestCovariantSuper();
    TestCovariantChild child = new TestCovariantChild();
    Object val1 = parent.foo("bar");
    assertTrue(val1 instanceof String);
    assertEquals("bar", val1);
    String val2 = child.foo("bar");
    assertEquals("bar", val2);
  }

  public void testEmbeddedNullsInStrings() {
    String s = "Pre\u0000Post";
    assertEquals(s.length(), getStringLength(s));
    assertEquals(s + "me", getString(s));
  }

  public void testEnum() {
    TestEnum val = enumSimple(TestEnum.VAL2);
    assertEquals(TestEnum.VAL2, val);
    int ord = enumValue(val);
    assertEquals(TestEnum.VAL2.ordinal(), ord);
    String name = enumName(val);
    assertEquals(TestEnum.VAL2.name(), name);
  }

  public void testEnumJsni() {
    assertEquals("sFoo", TestEnum.sFoo());
    assertEquals("sFoo", TestEnum.VAL1.sFoo());
    assertEquals("foo", TestEnum.VAL1.foo());
    assertEquals("VAL3-foo", TestEnum.VAL3.foo());
  }

  public void testFloat() {
    storeFloat(Float.MIN_VALUE);
    float f = getFloat();
    assertTrue(f == Float.MIN_VALUE);
    f = getFloatString();
    assertTrue(f == Float.MIN_VALUE);
    storeFloat(Float.MAX_VALUE);
    f = getFloat();
    assertTrue(f == Float.MAX_VALUE);
    f = getFloatString();
    assertTrue(f == Float.MAX_VALUE);
  }

  public void testFunctionCaching() {
    assertEquals("barfoo", sFooCall("bar"));
    assertEquals("barfoo", sFooDirect("bar"));
    assertEquals("barfoo", sFooInvoke("bar"));
    assertEquals("barfoo", sFooRoundTrip(getsFooFunc(), "bar"));

    assertEquals("barfoo", fooCall("bar"));
    assertEquals("barfoo", fooDirect("bar"));
    assertEquals("barfoo", fooRoundTrip(getFooFunc(), "bar"));

    String sFooString = getsFooFunc().toString();
    assertEquals(sFooString, sFooFuncAsStr());
    assertEquals(sFooString, sFooFuncToString());
    String fooString = getFooFunc().toString();
    assertEquals(fooString, fooFuncAsStr());
    assertEquals(fooString, fooFuncToString());
  }

  public void testGenerics() {
    String s = genericSimple("test");
    assertEquals("test", s);
    String v = genericGet(s);
    assertEquals("test", v);
    List<String> list = new ArrayList<String>();
    list.add("foo");
    Object obj = genericWildcard(list);
    assertTrue(obj instanceof String);
    assertEquals("foo", obj);
    obj = genericSubtype("test");
    List<Object> list2 = genericSubtype(new TestGenericList());
    assertTrue(list2 instanceof TestGenericList);
    assertEquals(42, list2.size());
    assertEquals(list2, list2.get(0));
    String[] array = new String[] {"foo", "bar"};
    String[] ret = genericArray(array);
    assertEquals(2, ret.length);
    assertEquals("bar", ret[1]);
    assertTrue(Arrays.deepEquals(array, ret));
  }

  /**
   * Tests that we are able to use binary and source level names when
   * referencing a Java identifier from JSNI.
   */
  public void testJavaMemberRefResolution() {
    A.B b = new A.B();
    assertEquals(1, b.getUsingSourceRef());
    assertEquals(1, b.getUsingBinaryRef());
  }

  public void testJavaObjectIdentityInJS() {
    Object o = new Object();
    assertTrue(jsIdentical(o, o));
    assertTrue(jsniInstanceFunctionsIdentical(o));
    assertTrue(jsniStaticFunctionsIdentical());
  }

  public void testJsniFormats() {
    jsniA();
    jsniB();
    jsniC();
    jsniD();
    jsniE();
    jsniF();
    jsniG();
    jsniH();
    jsniI();
    jsniJ();
    jsniK();
    jsniL();
  }

  /**
   * Tests passing primitive type arguments to JSNI methods. See issue 2426.
   */
  public void testJsniParamUnboxedPrimitives() {
    class Inner {
      native boolean nativeJsniParamUnboxedBoolean(boolean param) /*-{
        return (param == true);
      }-*/;

      native boolean nativeJsniParamUnboxedByte(byte param) /*-{
        return (param == 99);
      }-*/;

      native boolean nativeJsniParamUnboxedCharacter(char param) /*-{
        return (param == 77);
      }-*/;

      native boolean nativeJsniParamUnboxedDouble(double param) /*-{
        return (param == 1234.56789);
      }-*/;

      native boolean nativeJsniParamUnboxedFloat(float param) /*-{
        return (param == 1234.5);
      }-*/;

      native boolean nativeJsniParamUnboxedInteger(int param) /*-{
        return (param == 9876543);
      }-*/;

      native boolean nativeJsniParamUnboxedShort(short param) /*-{
        return (param == 1234);
      }-*/;
    }
    Inner inner = new Inner();

    assertTrue("Unboxed boolean", inner.nativeJsniParamUnboxedBoolean(true));
    assertTrue("Unboxed byte", inner.nativeJsniParamUnboxedByte((byte) 99));
    assertTrue("Unboxed char", inner.nativeJsniParamUnboxedCharacter((char) 77));
    assertTrue("Unboxed double", inner.nativeJsniParamUnboxedDouble(1234.56789));
    assertTrue("Unboxed float",
        inner.nativeJsniParamUnboxedFloat((float) 1234.5));
    assertTrue("Unboxed int", inner.nativeJsniParamUnboxedInteger(9876543));
    // long type intentionally omitted - it is emulated and not usable in JS
    assertTrue("Unboxed short", inner.nativeJsniParamUnboxedShort((short) 1234));
  }

  /**
   * More test cases resulting from issue 2426 to show that primitives can be
   * passed through JSNI methods unmolested.
   */
  public void testJsniPassthroughPrimitives() {
    class Inner {
      native boolean nativeBoolean(boolean param) /*-{
        return param;
      }-*/;

      native byte nativeByte(byte param) /*-{
        return param;
      }-*/;

      native char nativeCharacter(char param) /*-{
        return param;
      }-*/;

      native double nativeDouble(double param) /*-{
        return param;
      }-*/;

      native float nativeFloat(float param) /*-{
        return param;
      }-*/;

      native int nativeInteger(int param) /*-{
        return param;
      }-*/;

      @UnsafeNativeLong
      native long nativeLong(long param) /*-{
        return param;
      }-*/;

      native short nativeShort(short param) /*-{
        return param;
      }-*/;
    }
    Inner inner = new Inner();

    assertEquals("nativeBoolean", inner.nativeBoolean(true), true);
    assertEquals("nativeBoolean", inner.nativeBoolean(false), false);

    assertEquals("nativeByte", inner.nativeByte((byte) 0), (byte) 0);
    assertEquals("nativeByte", inner.nativeByte((byte) 1), (byte) 1);
    assertEquals("nativeByte", inner.nativeByte((byte) -1), (byte) -1);
    assertEquals("nativeByte", inner.nativeByte((byte) 127), (byte) 127);
    assertEquals("nativeByte", inner.nativeByte((byte) -127), (byte) -127);
    assertEquals("nativeByte", inner.nativeByte(Byte.MAX_VALUE), Byte.MAX_VALUE);
    assertEquals("nativeByte", inner.nativeByte(Byte.MIN_VALUE), Byte.MIN_VALUE);

    assertEquals("nativeCharacter", inner.nativeCharacter((char) 0), (char) 0);
    assertEquals("nativeCharacter", inner.nativeCharacter((char) 1), (char) 1);
    assertEquals("nativeCharacter", inner.nativeCharacter((char) -1), (char) -1);
    assertEquals("nativeCharacter", inner.nativeCharacter((char) 32767),
        (char) 32767);
    assertEquals("nativeCharacter", inner.nativeCharacter((char) -32767),
        (char) -32767);
    assertEquals("nativeCharacter", inner.nativeCharacter(Character.MAX_VALUE),
        Character.MAX_VALUE);
    assertEquals("nativeCharacter", inner.nativeCharacter(Character.MIN_VALUE),
        Character.MIN_VALUE);

    assertEquals("nativeDouble", inner.nativeDouble(0.0), 0.0);
    assertEquals("nativeDouble", inner.nativeDouble(1.0), 1.0);
    assertEquals("nativeDouble", inner.nativeDouble(-1.0), -1.0);
    assertEquals("nativeDouble", inner.nativeDouble(100000000000.0),
        100000000000.0);
    assertEquals("nativeDouble", inner.nativeDouble(-100000000000.0),
        -100000000000.0);
    assertEquals("nativeDouble MAX", inner.nativeDouble(Double.MAX_VALUE),
        Double.MAX_VALUE);
    assertEquals("nativeDouble MIN", inner.nativeDouble(Double.MIN_VALUE),
        Double.MIN_VALUE);

    assertEquals("nativeFloat", inner.nativeFloat((float) 0.0), (float) 0.0);
    assertEquals("nativeFloat", inner.nativeFloat((float) 1.0), (float) 1.0);
    assertEquals("nativeFloat", inner.nativeFloat((float) -1.0), (float) -1.0);
    assertEquals("nativeFloat", inner.nativeFloat((float) 1000000.0),
        (float) 1000000.0);
    assertEquals("nativeFloat", inner.nativeFloat((float) -1000000.0),
        (float) -1000000.0);
    assertEquals("nativeFloat", inner.nativeFloat(Float.MAX_VALUE),
        Float.MAX_VALUE);
    assertEquals("nativeFloat", inner.nativeFloat(Float.MIN_VALUE),
        Float.MIN_VALUE);

    assertEquals("nativeInteger", inner.nativeInteger(0), 0);
    assertEquals("nativeInteger", inner.nativeInteger(1), 1);
    assertEquals("nativeInteger", inner.nativeInteger(-1), -1);
    assertEquals("nativeInteger", inner.nativeInteger(2147483647), 2147483647);
    assertEquals("nativeInteger", inner.nativeInteger(-2147483647), -2147483647);
    assertEquals("nativeInteger MAX", inner.nativeInteger(Integer.MAX_VALUE),
        Integer.MAX_VALUE);
    assertEquals("nativeInteger MIN", inner.nativeInteger(Integer.MIN_VALUE),
        Integer.MIN_VALUE);

    assertEquals("nativeLong", inner.nativeLong(0L), 0L);
    assertEquals("nativeLong", inner.nativeLong(1L), 1L);
    assertEquals("nativeLong", inner.nativeLong(-1L), -1L);
    assertEquals("nativeLong", inner.nativeLong(9223372036854775807L),
        9223372036854775807L);
    assertEquals("nativeLong", inner.nativeLong(-9223372036854775807L),
        -9223372036854775807L);
    assertEquals("nativeLong", inner.nativeLong(Long.MAX_VALUE), Long.MAX_VALUE);
    assertEquals("nativeLong", inner.nativeLong(Long.MIN_VALUE), Long.MIN_VALUE);

    assertEquals("nativeShort", inner.nativeShort((short) 0), (short) 0);
    assertEquals("nativeShort", inner.nativeShort((short) 1), (short) 1);
    assertEquals("nativeShort", inner.nativeShort((short) -1), (short) -1);
    assertEquals("nativeShort", inner.nativeShort((short) 32767), (short) 32767);
    assertEquals("nativeShort", inner.nativeShort((short) -32767),
        (short) -32767);
    assertEquals("nativeShort MAX", inner.nativeShort(Short.MAX_VALUE),
        Short.MAX_VALUE);
    assertEquals("nativeShort MIN", inner.nativeLong(Short.MIN_VALUE),
        Short.MIN_VALUE);
  }

  /**
   * Tests returning primitive type arguments from JSNI methods. See issue 2426.
   */
  public void testJsniReturnUnboxedPrimitives() {
    class Inner {
      native boolean nativeJsniReturnUnboxedBoolean() /*-{
        return true;
      }-*/;

      native byte nativeJsniReturnUnboxedByte() /*-{
        return 99;
      }-*/;

      native char nativeJsniReturnUnboxedCharacter() /*-{
        return 77;
      }-*/;

      native double nativeJsniReturnUnboxedDouble() /*-{
        return 1234.56789;
      }-*/;

      native float nativeJsniReturnUnboxedFloat() /*-{
        return 1234.5;
      }-*/;

      native int nativeJsniReturnUnboxedInteger() /*-{
        return 9876543;
      }-*/;

      native short nativeJsniReturnUnboxedShort() /*-{
        return 1234;
      }-*/;
    }
    Inner inner = new Inner();

    assertTrue("Unboxed boolean",
        inner.nativeJsniReturnUnboxedBoolean() == true);
    assertTrue("Unboxed byte", inner.nativeJsniReturnUnboxedByte() == (byte) 99);
    assertTrue("Unboxed char",
        inner.nativeJsniReturnUnboxedCharacter() == (char) 77);
    assertTrue("Unboxed double",
        inner.nativeJsniReturnUnboxedDouble() == 1234.56789);
    assertTrue("Unboxed float",
        inner.nativeJsniReturnUnboxedFloat() == (float) 1234.5);
    assertTrue("Unboxed int", inner.nativeJsniReturnUnboxedInteger() == 9876543);
    // long type intentionally omitted - it is emulated and not usable in JS
    assertTrue("Unboxed short",
        inner.nativeJsniReturnUnboxedShort() == (short) 1234);
  }

  /**
   * Tests that using the JavaScript toString method results in a call to the
   * java.lang.Object::toString() method.
   */
  public void testJSNIToStringResolution() {
    class Foo {
      @Override
      public String toString() {
        return "FOO";
      }
    }

    assertEquals("FOO", callJSNIToString(new Foo()));
  }

  /*
   * Test that returning strings from methods declared as returning Object
   * works, and that returning a primitive does not.
   */
  public void testObjectReturns() {
    String str = (String) getStringAsObject();
    assertEquals(str, "test");
    try {
      getIntAsObject();
      // should have thrown an exception in Development Mode,
      // so fail unless we are in Production Mode
      assertTrue(GWT.isScript());
    } catch (IllegalArgumentException e) {
      // expected exception
    }
  }

  public void testVarargs() {
    String[] strs = varargsHelper("foo", "bar");
    assertEquals(2, strs.length);
    assertEquals("bar", strs[1]);
    // TODO: not sure if we want to support this or not.
    // strs = varargsFromJS1();
    // assertEquals(2, strs.length);
    // assertEquals("bar", strs[1]);
    strs = varargsFromJS2(strs);
    assertEquals(2, strs.length);
    assertEquals("bar", strs[1]);
  }

  String foo(String s) {
    return s + "foo";
  }

  private native int byteAsInt(byte b) /*-{
    return b;
  }-*/;

  private native String callJSNIToString(Object obj) /*-{
    return obj.toString();
  }-*/;

  private native String enumName(TestEnum val) /*-{
    return val.@java.lang.Enum::name()();
  }-*/;

  private native TestEnum enumSimple(TestEnum val) /*-{
    return val;
  }-*/;

  private native int enumValue(TestEnum val) /*-{
    return val.@java.lang.Enum::ordinal()();
  }-*/;

  private native String fooCall(String s) /*-{
    var f = this.@com.google.gwt.dev.jjs.test.HostedTest::foo(Ljava/lang/String;);
    return f.call(this, s);
  }-*/;

  private native String fooDirect(String s) /*-{
    return this.@com.google.gwt.dev.jjs.test.HostedTest::foo(Ljava/lang/String;)(s);
  }-*/;

  private native String fooFuncAsStr() /*-{
    var f = this.@com.google.gwt.dev.jjs.test.HostedTest::foo(Ljava/lang/String;);
    return "" + f;
  }-*/;

  private native String fooFuncToString() /*-{
    var f = this.@com.google.gwt.dev.jjs.test.HostedTest::foo(Ljava/lang/String;);
    return f.toString();
  }-*/;

  private native String fooRoundTrip(JavaScriptObject fooFunc, String s) /*-{
    return fooFunc.call(this, s);
  }-*/;

  // Make this a JSNI method calling the Java method when that is implemented.
  private native <T> T[] genericArray(T[] array) /*-{
    // return genericPassthrough(array);
    return this.@com.google.gwt.dev.jjs.test.HostedTest::genericPassthrough([Ljava/lang/Object;)(array);
  }-*/;

  /*
   * Since we can't generate a generic instance from within JS, K and V have to
   * actually be compatible.
   */
  private native <K, V> V genericGet(K key) /*-{
    return key;
  }-*/;

  @SuppressWarnings("unused")
  // called by JSNI
  private <T> T[] genericPassthrough(T[] array) {
    return array;
  }

  // generics helper methods
  private native <T> T genericSimple(T val) /*-{
    return val;
  }-*/;

  private native <T, U extends T> T genericSubtype(U val) /*-{
    return val;
  }-*/;

  private native Object genericWildcard(List<?> list) /*-{
    return list.@java.util.List::get(I)(0);
  }-*/;

  private native JavaScriptObject getFooFunc() /*-{
    return this.@com.google.gwt.dev.jjs.test.HostedTest::foo(Ljava/lang/String;);
  }-*/;

  private native void jsniA()/*-{

  }-*/;

  private native void jsniB()/*-{

  }-*/;

  private native void jsniC()
  /*-{

  }-*/;

  private native void jsniD()
  /*-{

  }-*/;

  /**
   * comment.
   */
  private native void jsniE()/*-{

  }-*/;

  /** comment. */
  private native void jsniF()/*-{

  }-*/;

  /** comment. */
  private native void jsniG()/*-{

  }-*/;

  /*
   * comment
   */
  private native void jsniH()/*-{

  }-*/;

  /* comment */private native void jsniI()/*-{

  }-*/;

  // comment
  private native void jsniJ()/*-{

  }-*/;

  private native void jsniK()
  /*-{

  }-*/;

  /*-{
    try to mess with compiler
  }-*/
  private native void jsniL()/*-{

  }-*/;

  // test that JS can pass a series of arguments to a varargs function
  // TODO: not sure if we want to support this
  // private native String[] varargsFromJS1() /*-{
  // return
  // this.@com.google.gwt.dev.jjs.test.HostedTest::varargsPassthrough([Ljava/lang
  // /String;)("foo",
  // "bar");
  // }-*/;

  // test that JS can pass a Java-created array to a varargs function
  private native String[] varargsFromJS2(String[] arr) /*-{
    return this.@com.google.gwt.dev.jjs.test.HostedTest::varargsPassthrough([Ljava/lang/String;)(arr);
  }-*/;

  private native String[] varargsHelper(String... args) /*-{
    return args;
  }-*/;

  @SuppressWarnings("unused")
  // called from JSNI
  private String[] varargsPassthrough(String... args) {
    return args;
  }

}
