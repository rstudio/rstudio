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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

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

  static String sFoo(String s) {
    return s + "foo";
  }
  
  private native static boolean getBoxedBooleanAsBool(boolean v) /*-{
    return new Boolean(v);
  }-*/;
  
  private native static JavaScriptObject getBoxedBooleanAsObject(boolean v) /*-{
    return new Boolean(v);
  }-*/;
  
  private native static double getBoxedNumberAsDouble(double v) /*-{
    return new Number(v);
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

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void test32BitInt() {
    assertEquals(Integer.MAX_VALUE, passThroughInt(Integer.MAX_VALUE));
    assertEquals(Integer.MIN_VALUE, passThroughInt(Integer.MIN_VALUE));
  }

  /*
   * Test that returning JavaScript boxed primitives works as expected.
   * Currently only String is automatically unboxed, so Boolean and Number
   * are currently disabled.
   */
  public void testAutoBoxing() {
    JavaScriptObject bvo = getBoxedBooleanAsObject(true);
    assertEquals(getJSOAsString(bvo), "true");
    // boolean bv = getBoxedBooleanAsBool(true);
    // assertEquals(bv, true);
    JavaScriptObject nvo = getBoxedNumberAsObject(42);
    assertEquals(getJSOAsString(nvo), "42");
    // double nv = getBoxedNumberAsDouble(42);
    // assertEquals(nv, 42, 0);
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
        return this.@com.google.gwt.dev.jjs.test.HostedTest$2$Foo::a()();
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
      native String a() /*-{
        return "oblah";
      }-*/;

      native String b() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest$2$Foo::a()();
      }-*/;

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
  
  String foo(String s) {
    return s + "foo";
  }
  
  private native int byteAsInt(byte b) /*-{
    return b;
  }-*/;

  private native String callJSNIToString(Object obj) /*-{
    return obj.toString();
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
   * comment
   */
  private native void jsniE()/*-{}-*/;

  /** comment */
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

}
