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
/**********************
 *                    *
 *   DO NOT FORMAT    *
 *                    *
 **********************/
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//CHECKSTYLE_NAMING_OFF

/**
 * TODO: document me.
 */
public class HostedTest extends GWTTestCase {

  /**
   * Tests that we can use a source level name for a nested type instead of the
   * binary name.
   */
  protected static class A {
    
    /**
     * TODO: document me.
     */
    public static class B {
      int b = 1;
      public native int getUsingBinaryRef() /*-{
      return this.@com.google.gwt.dev.jjs.test.HostedTest$A$B::b;
    }-*/;
      
      public native int getUsingSourceRef() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest.A.B::b;
      }-*/;
    }
  }

  private static class GenericListTest extends AbstractList<Object> {

    @Override
    public Object get(int index) {
      return this;
    }

    @Override
    public int size() {
      return 42;
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
    VAL1, VAL2, VAL3
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
  
  private native static JavaScriptObject getBoxedStringAsObject(String v) /*-{
    return new String(v);
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

  private static native int passThroughInt(int val) /*-{
    return val;
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

  public void test32BitInt() {
    assertEquals(Integer.MAX_VALUE, passThroughInt(Integer.MAX_VALUE));
    assertEquals(Integer.MIN_VALUE, passThroughInt(Integer.MIN_VALUE));
  }

  /*
   * Test that returning JavaScript boxed primitives works as expected.
   * Note that Boolean and Number cannot be supported properly in web
   * mode, so we do not support it in hosted mode and therefore do not
   * test it here.
   */
  public void testAutoBoxing() {
    JavaScriptObject bvo = getBoxedBooleanAsObject(true);
    assertEquals(getJSOAsString(bvo), "true");
    JavaScriptObject nvo = getBoxedNumberAsObject(42);
    assertEquals(getJSOAsString(nvo), "42");
    JavaScriptObject svo = getBoxedStringAsObject("test");
    assertEquals(getJSOAsString(svo), "test");
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
  
  @Override
  public boolean catchExceptions() {
    return false;
  }

  public void testEnum() {
    TestEnum val = enumSimple(TestEnum.VAL2);
    assertEquals(TestEnum.VAL2, val);
    int ord = enumValue(val);
    assertEquals(TestEnum.VAL2.ordinal(), ord);
    String name = enumName(val);
    assertEquals(TestEnum.VAL2.name(), name);
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
    List<Object> list2 = genericSubtype(new GenericListTest());
    assertTrue(list2 instanceof GenericListTest);
    assertEquals(42, list2.size());
    assertEquals(list2, list2.get(0));
    String[] array = new String[] { "foo", "bar" };
    String[] ret = genericArray(array);
    assertEquals(2, ret.length);
    assertEquals("bar", ret[1]);
    assertTrue(Arrays.deepEquals(array, ret));
  }

  /**
   * Tests that we are able to use binary and source level names when referencing
   * a Java identifier from JSNI.
   */
  public void testJavaMemberRefResolution() {
    A.B b = new A.B();
    assertEquals(1, b.getUsingSourceRef());
    assertEquals(1, b.getUsingBinaryRef());
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
   * Tests that using the JavaScript toString method results in a call to 
   * the java.lang.Object::toString() method.
   *
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

  public void testLocalJsni() {
    
    class Foo {
      native String a() /*-{
        return "blah";
      }-*/;

      native String b() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest$2Foo::a()();
      }-*/;

      String c() {
        return a();
      }
    }
    
    Foo f = new Foo();
    assertEquals(f.a(), "blah");
    assertEquals(f.b(), "blah");
    assertEquals(f.c(), "blah");
    
    Foo fo = new Foo() {
      @Override
      native String a() /*-{
        return "oblah";
      }-*/;

      @Override
      native String b() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest$2Foo::a()();
      }-*/;

      @Override
      native String c() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest$1::a()();
      }-*/;
    };
    
    assertEquals(fo.a(), "oblah");
    assertEquals(fo.b(), "oblah");
    assertEquals(fo.c(), "oblah");
  }

  public void testLongMarshalling() {
    // a big number that cannot accurately be represented as a double
    long l = 1234567890123456789L;
    double d = l;
    assertTrue(isEq(l, d));
  }

  /*
   * Test that returning strings from methods declared as returning Object
   * works, and that returning a primitive does not.
   */
  public void testObjectReturns() {
    String str = (String)getStringAsObject();
    assertEquals(str, "test");
    try {
      getIntAsObject();
      // should have thrown an exception in hosted mode,
      // so fail unless we are in web mode
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
   * Since we can't generate a generic instance from within JS, K and V
   * have to actually be compatible.
   */
  private native <K,V> V genericGet(K key) /*-{
    return key;
  }-*/;

  @SuppressWarnings("unused") // called by JSNI
  private <T> T[] genericPassthrough(T[] array) {
    return array;
  }

  // generics helper methods
  private native <T> T genericSimple(T val) /*-{
    return val;
  }-*/;

  private native <T,U extends T> T genericSubtype(U val) /*-{
    return val;
  }-*/;

  private native Object genericWildcard(List<?> list) /*-{
    return list.@java.util.List::get(I)(0);
  }-*/;

  private native JavaScriptObject getFooFunc() /*-{
    return this.@com.google.gwt.dev.jjs.test.HostedTest::foo(Ljava/lang/String;);
  }-*/;

  private native boolean isEq(long l, double d) /*-{
    return l == d;
  }-*/;

  private native void jsniA()/*-{}-*/;

  private native void jsniB()/*-{
  }-*/;

  private native void jsniC()
  /*-{
  }-*/;

  private native void jsniD()
  /*-{}-*/;

  /**
   * comment.
   */
  private native void jsniE()/*-{}-*/;
  
  /** comment. */
  private native void jsniF()/*-{}-*/;

  /** comment */private native void jsniG()/*-{}-*/;

  /* 
   * comment
   */
  private native void jsniH()/*-{}-*/;

  /* comment */private native void jsniI()/*-{}-*/;

  // comment
  private native void jsniJ()/*-{}-*/;

  private
  native
  void 
  jsniK()
  /*-{
  }-*/;

  /*-{ try to mess with compiler }-*/
  private native void jsniL()/*-{}-*/ ;
  
  // test that JS can pass a series of arguments to a varargs function
  private native String[] varargsFromJS1() /*-{
    return this.@com.google.gwt.dev.jjs.test.HostedTest::varargsPassthrough([Ljava/lang/String;)("foo", "bar");
  }-*/;

  // test that JS can pass a Java-created array to a varargs function
  private native String[] varargsFromJS2(String[] arr) /*-{
    return this.@com.google.gwt.dev.jjs.test.HostedTest::varargsPassthrough([Ljava/lang/String;)(arr);
  }-*/;

  private native String[] varargsHelper(String... args) /*-{
    return args;
  }-*/;

  @SuppressWarnings("unused") // called from JSNI
  private String[] varargsPassthrough(String... args) {
    return args;
  }

}
