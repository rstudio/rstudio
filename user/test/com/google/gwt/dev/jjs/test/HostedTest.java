// Copyright 2006 Google Inc. All Rights Reserved.
/**********************
 *                    *
 *   DO NOT FORMAT    *
 *                    *
 **********************/
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

public class HostedTest extends GWTTestCase {

  static String sFoo(String s) {
    return s + "foo";
  }

  private static native JavaScriptObject getsFooFunc() /*-{
    return @com.google.gwt.dev.jjs.test.HostedTest::sFoo(Ljava/lang/String;);
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

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testByteMarshalling() {
    byte b = 100;
    assertEquals(100, byteAsInt(b));
    b = -125;
    assertEquals(-125, byteAsInt(b));
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

  public void testLocalJsni() {
    
    class Foo {
      native String a() /*-{
        return "blah";
      }-*/;

      native String b() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest$1$Foo::a()();
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
        return this.@com.google.gwt.dev.jjs.test.HostedTest$1$Foo::a()();
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

  /**
   * Tests that we can use a source level name for a nested type instead of the
   * binary name.
   */
  private static class A {
    public static class B {
      int b = 1;
      public native int getUsingSourceRef() /*-{
        return this.@com.google.gwt.dev.jjs.test.HostedTest.A.B::b;
      }-*/;
      
      public native int getUsingBinaryRef() /*-{
      return this.@com.google.gwt.dev.jjs.test.HostedTest$A$B::b;
    }-*/;
    }
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
  
  private native String callJSNIToString(Object obj) /*-{
    return obj.toString();
  }-*/;
  
  String foo(String s) {
    return s + "foo";
  }

  private native int byteAsInt(byte b) /*-{
    return b;
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
  private native void jsniL()/*-{}-*/;

}
