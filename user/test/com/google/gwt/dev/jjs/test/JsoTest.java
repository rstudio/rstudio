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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link JavaScriptObject} and subclasses.
 */
public class JsoTest extends GWTTestCase {

  static class Bar extends JavaScriptObject {
    public static int field;

    public static native String staticNative() /*-{
      return "nativeBar";
    }-*/;

    public static String staticValue() {
      return "Bar" + field;
    }

    protected Bar() {
    }

    public final native String getBar() /*-{
      return this.bar;
    }-*/;

    public final String value() {
      return "Bar";
    }
  }

  static class Foo extends JavaScriptObject {
    public static int field;

    public static native String staticNative() /*-{
      return "nativeFoo";
    }-*/;
    
    /**
     * Ensure that a supertype can refer to members of a subtype.
     */
    public static native String staticNativeToSub() /*-{
      return @com.google.gwt.dev.jjs.test.JsoTest.FooSub::staticValueSub()();
    }-*/;

    public static String staticValue() {
      return "Foo" + field;
    }

    protected Foo() {
    }

    public final native String getFoo() /*-{
      return this.foo;
    }-*/;

    public final String value() {
      return "Foo";
    }
  }

  static class FooSub extends Foo {
    static String staticValueSub() {
      return "FooSub";
    }
    
    protected FooSub() {
    }

    public final String anotherValue() {
      return "Still Foo";
    }

    public final String superCall() {
      return super.value();
    }
  }

  static class JsArray<T> extends JavaScriptObject {
    public static native <T> JsArray<T> create() /*-{
      return [];
    }-*/;

    protected JsArray() {
    }

    public final native T get(int index) /*-{
      return this[index];
    }-*/;

    public final native int length() /*-{
      return this.length;
    }-*/;

    public final native void put(int index, T value) /*-{
      this[index] = value;
    }-*/;
  }

  static class MethodMangleClash {
    @SuppressWarnings("unused")
    public static String func(JavaScriptObject this_) {
      return "funcJavaScriptObject";
    }

    @SuppressWarnings("unused")
    public static String func(MethodMangleClash this_) {
      return "funcMethodMangleClash";
    }

    @SuppressWarnings("unused")
    public String func() {
      return "func";
    }
  }

  static class Overloads {
    private static volatile boolean FALSE = false;

    @SuppressWarnings("unused")
    static String sFunc(Bar b) {
      return "sFunc Bar";
    }

    @SuppressWarnings("unused")
    static String sFunc(Bar[][] b) {
      return "sFunc Bar[][]";
    }

    @SuppressWarnings("unused")
    static String sFunc(Foo f) {
      return "sFunc Foo";
    }

    @SuppressWarnings("unused")
    static String sFunc(Foo[][] f) {
      return "sFunc Foo[][]";
    }

    @SuppressWarnings("unused")
    public Overloads(Bar b) {
    }

    @SuppressWarnings("unused")
    public Overloads(Bar[][] b) {
    }

    @SuppressWarnings("unused")
    public Overloads(Foo f) {
    }

    @SuppressWarnings("unused")
    public Overloads(Foo[][] f) {
    }

    @SuppressWarnings("unused")
    String func(Bar b) {
      if (FALSE) {
        // prevent inlining
        return func(b);
      }
      return "func Bar";
    }

    @SuppressWarnings("unused")
    String func(Bar[][] b) {
      if (FALSE) {
        // prevent inlining
        return func(b);
      }
      return "func Bar[][]";
    }

    @SuppressWarnings("unused")
    String func(Foo f) {
      if (FALSE) {
        // prevent inlining
        return func(f);
      }
      return "func Foo";
    }

    @SuppressWarnings("unused")
    String func(Foo[][] f) {
      if (FALSE) {
        // prevent inlining
        return func(f);
      }
      return "func Foo[][]";
    }
  }

  private static native Bar makeBar() /*-{
    return {
      toString:function() {
        return "bar";
      },
      bar: "this is bar",
    };
  }-*/;

  private static native Foo makeFoo() /*-{
    return {
      toString:function() {
        return "foo";
      },
      foo: "this is foo",
    };
  }-*/;

  private static native JavaScriptObject makeJSO() /*-{
    return {
      toString:function() {
        return "jso";
      },
      foo: "jso foo",
      bar: "jso bar",
    };
  }-*/;

  private static native JavaScriptObject makeMixedArray() /*-{
    return [
      @com.google.gwt.dev.jjs.test.JsoTest::makeJSO()(),
      "foo",
      @com.google.gwt.dev.jjs.test.JsoTest::makeObject()(),
      null
    ];
  }-*/;

  private static Object makeObject() {
    return new Object() {
      @Override
      public String toString() {
        return "myObject";
      }
    };
  }

  private static native JavaScriptObject returnMe(JavaScriptObject jso) /*-{
    return jso;
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArrayInit() {
    Object[] array = {makeJSO(), new Object(), ""};
    assertTrue(array[0] instanceof JavaScriptObject);
    assertFalse(array[1] instanceof JavaScriptObject);
    assertFalse(array[2] instanceof JavaScriptObject);
  }

  public void testArrayStore() {
    JavaScriptObject[] jsoArray = new JavaScriptObject[1];
    jsoArray[0] = makeJSO();
    jsoArray[0] = makeFoo();
    jsoArray[0] = makeBar();

    Foo[] fooArray = new Foo[1];
    fooArray[0] = (Foo) makeJSO();
    fooArray[0] = makeFoo();
    fooArray[0] = makeBar().cast();

    Bar[] barArray = new Bar[1];
    barArray[0] = (Bar) makeJSO();
    barArray[0] = makeBar();
    barArray[0] = makeFoo().cast();

    Object[] objArray = jsoArray;
    try {
      objArray[0] = new Object();
      fail("Expected ArrayStoreException");
    } catch (ArrayStoreException expected) {
    }

    objArray = new Object[1];
    objArray[0] = makeJSO();
    objArray[0] = makeFoo();
    objArray[0] = makeBar();
  }

  public void testBasic() {
    JavaScriptObject jso = makeJSO();
    assertEquals("jso", jso.toString());

    Foo foo = (Foo) jso;
    assertEquals("jso", foo.toString());
    assertEquals("jso foo", foo.getFoo());
    assertEquals("Foo", foo.value());

    Bar bar = (Bar) jso;
    assertEquals("jso", bar.toString());
    assertEquals("jso bar", bar.getBar());
    assertEquals("Bar", bar.value());

    foo = makeFoo();
    assertEquals("foo", foo.toString());
    assertEquals("this is foo", foo.getFoo());
    assertEquals("Foo", foo.value());

    bar = makeBar();
    assertEquals("bar", bar.toString());
    assertEquals("this is bar", bar.getBar());
    assertEquals("Bar", bar.value());
  }

  @SuppressWarnings("cast")
  public void testCasts() {
    JavaScriptObject jso = makeJSO();
    assertTrue(jso instanceof JavaScriptObject);
    assertTrue(jso instanceof Foo);
    assertTrue(jso instanceof Bar);

    Foo foo = (Foo) jso;
    foo = makeFoo();
    assertTrue((JavaScriptObject) foo instanceof Bar);
    Bar bar = (Bar) (JavaScriptObject) makeFoo();
    bar = makeFoo().cast();

    bar = (Bar) jso;
    bar = makeBar();
    assertTrue((JavaScriptObject) bar instanceof Foo);
    foo = (Foo) (JavaScriptObject) makeBar();
    foo = makeBar().cast();

    // Implicit
    jso = foo;
    jso = bar;

    Object o = new Object();
    assertFalse(o instanceof JavaScriptObject);
    assertFalse(o instanceof Foo);
    assertFalse(o instanceof Bar);
    try {
      jso = (JavaScriptObject) o;
      fail("Expected ClassCastException");
    } catch (ClassCastException expected) {
    }

    o = "foo";
    assertFalse(o instanceof JavaScriptObject);
    assertFalse(o instanceof Foo);
    assertFalse(o instanceof Bar);
    try {
      jso = (JavaScriptObject) o;
      fail("Expected ClassCastException");
    } catch (ClassCastException expected) {
    }

    o = jso;
    assertFalse(o instanceof String);
    try {
      String s = (String) o;
      s.toString();
      fail("Expected ClassCastException");
    } catch (ClassCastException expected) {
    }
  }

  @SuppressWarnings("cast")
  public void testCastsArray() {
    JavaScriptObject[][] jso = new JavaScriptObject[0][0];
    assertTrue(jso instanceof JavaScriptObject[][]);
    assertTrue(jso instanceof Foo[][]);
    assertTrue(jso instanceof Bar[][]);

    Foo[][] foo = (Foo[][]) jso;
    foo = new Foo[0][0];
    assertTrue((JavaScriptObject[][]) foo instanceof Bar[][]);
    Bar[][] bar = (Bar[][]) (JavaScriptObject[][]) new Foo[0][0];

    bar = (Bar[][]) jso;
    bar = new Bar[0][0];
    assertTrue((JavaScriptObject[][]) bar instanceof Foo[][]);
    foo = (Foo[][]) (JavaScriptObject[][]) new Bar[0][0];

    Object[][] o = new Object[0][0];
    assertFalse(o instanceof JavaScriptObject[][]);
    assertFalse(o instanceof Foo[][]);
    assertFalse(o instanceof Bar[][]);
    try {
      jso = (JavaScriptObject[][]) o;
      fail("Expected ClassCastException");
    } catch (ClassCastException expected) {
    }

    o = jso;
    assertFalse(o instanceof String[][]);
    try {
      String[][] s = (String[][]) o;
      s.toString();
      fail("Expected ClassCastException");
    } catch (ClassCastException expected) {
    }
  }

  public void testClassLiterals() {
    JavaScriptObject jso = makeJSO();
    Foo foo = makeFoo();
    Bar bar = makeBar();
    assertEquals(JavaScriptObject.class, jso.getClass());
    assertEquals(Foo.class, jso.getClass());
    assertEquals(Bar.class, jso.getClass());
    assertEquals(JavaScriptObject.class, foo.getClass());
    assertEquals(Foo.class, foo.getClass());
    assertEquals(Bar.class, foo.getClass());
    assertEquals(JavaScriptObject.class, bar.getClass());
    assertEquals(Foo.class, bar.getClass());
    assertEquals(Bar.class, bar.getClass());
    assertEquals(JavaScriptObject.class, Foo.class);
    assertEquals(JavaScriptObject.class, Bar.class);
    assertEquals(Foo.class, Bar.class);

    if (!JavaScriptObject.class.getName().startsWith("Class$")) {
      // Class metadata could be disabled
      assertEquals("com.google.gwt.core.client.JavaScriptObject$",
          JavaScriptObject.class.getName());
    }
  }

  public void testClassLiteralsArray() {
    JavaScriptObject[][] jso = new JavaScriptObject[0][0];
    Foo[][] foo = new Foo[0][0];
    Bar[][] bar = new Bar[0][0];
    assertEquals(JavaScriptObject[][].class, jso.getClass());
    assertEquals(Foo[][].class, jso.getClass());
    assertEquals(Bar[][].class, jso.getClass());
    assertEquals(JavaScriptObject[][].class, foo.getClass());
    assertEquals(Foo[][].class, foo.getClass());
    assertEquals(Bar[][].class, foo.getClass());
    assertEquals(JavaScriptObject[][].class, bar.getClass());
    assertEquals(Foo[][].class, bar.getClass());
    assertEquals(Bar[][].class, bar.getClass());
    assertEquals(JavaScriptObject[][].class, Foo[][].class);
    assertEquals(JavaScriptObject[][].class, Bar[][].class);
    assertEquals(Foo[][].class, Bar[][].class);

    if (!JavaScriptObject.class.getName().startsWith("Class$")) {
      // Class metadata could be disabled
      assertEquals("[[Lcom.google.gwt.core.client.JavaScriptObject$;",
          JavaScriptObject[][].class.getName());
    }
  }

  public void testEquality() {
    JavaScriptObject jso = makeJSO();
    assertEquals(jso, jso);

    JavaScriptObject jso2 = makeJSO();
    assertFalse(jso.equals(jso2));
    assertFalse(jso2.equals(jso));

    jso2 = returnMe(jso);
    assertEquals(jso, jso2);
  }

  public void testGenericsJsos() {
    JsArray<JavaScriptObject> a = JsArray.create();
    a.put(0, makeJSO());
    a.put(1, makeFoo());
    a.put(2, makeBar());
    a.put(3, null);
    assertEquals(4, a.length());
    assertEquals("jso", a.get(0).toString());
    assertEquals("foo", a.get(1).toString());
    assertEquals("bar", a.get(2).toString());
    assertEquals(null, a.get(3));
  }

  public void testGenericsMixed() {
    JsArray<Object> a = JsArray.create();
    a.put(0, makeJSO());
    a.put(1, "foo");
    a.put(2, makeObject());
    a.put(3, null);
    assertEquals(4, a.length());
    assertEquals("jso", a.get(0).toString());
    assertEquals("foo", a.get(1));
    assertEquals("myObject", a.get(2).toString());
    assertEquals(null, a.get(3));
  }

  @SuppressWarnings("unchecked")
  public void testGenericsRawJson() {
    JsArray a = (JsArray) makeMixedArray();
    assertEquals(4, a.length());
    assertEquals("jso", a.get(0).toString());
    assertEquals("foo", a.get(1));
    assertEquals("myObject", a.get(2).toString());
    assertEquals(null, a.get(3));
  }

  public void testGenericsStrings() {
    JsArray<String> a = JsArray.create();
    a.put(0, "foo");
    a.put(1, "bar");
    a.put(2, "baz");
    a.put(3, null);
    assertEquals(4, a.length());
    assertEquals("foo", a.get(0));
    assertEquals("bar", a.get(1));
    assertEquals("baz", a.get(2));
    assertEquals(null, a.get(3));
  }

  public void testHashCode() {
    // TODO: make this better.
    JavaScriptObject jso = makeJSO();
    int jsoHashCode = jso.hashCode();
    Foo foo = makeFoo();
    Bar bar = makeBar();
    Object o = new Object() {
      @Override
      public int hashCode() {
        // Return something unlikely so as not to collide with the JSOs.
        return 0xDEADBEEF;
      }
    };

    assertEquals(jsoHashCode, jso.hashCode());
    assertFalse(jsoHashCode == foo.hashCode());
    assertFalse(jsoHashCode == bar.hashCode());
    assertFalse(jsoHashCode == o.hashCode());
    assertFalse(foo.hashCode() == bar.hashCode());
    assertFalse(foo.hashCode() == o.hashCode());
    assertFalse(bar.hashCode() == o.hashCode());

    o = jso;
    assertEquals(jsoHashCode, o.hashCode());

    String s = "foo";
    int stringHashCode = s.hashCode();
    o = s;
    assertEquals(stringHashCode, o.hashCode());
  }

  public void testIdentity() {
    JavaScriptObject jso = makeJSO();
    assertSame(jso, jso);

    JavaScriptObject jso2 = makeJSO();
    assertNotSame(jso, jso2);

    jso2 = returnMe(jso);
    assertSame(jso, jso2);
  }

  public void testInheritance() {
    Foo foo = makeFoo();
    FooSub fooSub = (FooSub) foo;
    assertEquals("Foo", fooSub.value());
    assertEquals("Still Foo", fooSub.anotherValue());
    assertEquals("Foo", fooSub.superCall());
  }

  public void testMethodMangleClash() {
    assertEquals("funcJavaScriptObject",
        MethodMangleClash.func((JavaScriptObject) null));
    assertEquals("funcMethodMangleClash",
        MethodMangleClash.func((MethodMangleClash) null));
    assertEquals("func", new MethodMangleClash().func());
  }

  public void testOverloads() {
    Foo foo = makeFoo();
    assertEquals("func Foo", new Overloads(foo).func(foo));
    assertEquals("sFunc Foo", Overloads.sFunc(foo));

    Bar bar = makeBar();
    assertEquals("func Bar", new Overloads(bar).func(bar));
    assertEquals("sFunc Bar", Overloads.sFunc(bar));
  }

  public void testOverloadsArray() {
    Foo[][] foo = new Foo[0][0];
    assertEquals("func Foo[][]", new Overloads(foo).func(foo));
    assertEquals("sFunc Foo[][]", Overloads.sFunc(foo));

    Bar[][] bar = new Bar[0][0];
    assertEquals("func Bar[][]", new Overloads(bar).func(bar));
    assertEquals("sFunc Bar[][]", Overloads.sFunc(bar));
  }

  public native void testOverloadsArrayNative() /*-{
    var o = @com.google.gwt.dev.jjs.test.JsoTest.Overloads::new([[Lcom/google/gwt/dev/jjs/test/JsoTest$Foo;)(null);
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("func Foo[][]", o.@com.google.gwt.dev.jjs.test.JsoTest.Overloads::func([[Lcom/google/gwt/dev/jjs/test/JsoTest$Foo;)(null));
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("sFunc Foo[][]", @com.google.gwt.dev.jjs.test.JsoTest.Overloads::sFunc([[Lcom/google/gwt/dev/jjs/test/JsoTest$Foo;)(null));

    var o = @com.google.gwt.dev.jjs.test.JsoTest.Overloads::new([[Lcom/google/gwt/dev/jjs/test/JsoTest$Bar;)(null);
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("func Bar[][]", o.@com.google.gwt.dev.jjs.test.JsoTest.Overloads::func([[Lcom/google/gwt/dev/jjs/test/JsoTest$Bar;)(null));
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("sFunc Bar[][]", @com.google.gwt.dev.jjs.test.JsoTest.Overloads::sFunc([[Lcom/google/gwt/dev/jjs/test/JsoTest$Bar;)(null));
  }-*/;

  public native void testOverloadsNative() /*-{
    var o = @com.google.gwt.dev.jjs.test.JsoTest.Overloads::new(Lcom/google/gwt/dev/jjs/test/JsoTest$Foo;)(null);
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("func Foo", o.@com.google.gwt.dev.jjs.test.JsoTest.Overloads::func(Lcom/google/gwt/dev/jjs/test/JsoTest$Foo;)(null));
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("sFunc Foo", @com.google.gwt.dev.jjs.test.JsoTest.Overloads::sFunc(Lcom/google/gwt/dev/jjs/test/JsoTest$Foo;)(null));

    var o = @com.google.gwt.dev.jjs.test.JsoTest.Overloads::new(Lcom/google/gwt/dev/jjs/test/JsoTest$Bar;)(null);
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("func Bar", o.@com.google.gwt.dev.jjs.test.JsoTest.Overloads::func(Lcom/google/gwt/dev/jjs/test/JsoTest$Bar;)(null));
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("sFunc Bar", @com.google.gwt.dev.jjs.test.JsoTest.Overloads::sFunc(Lcom/google/gwt/dev/jjs/test/JsoTest$Bar;)(null));
  }-*/;

  public void testStaticAccess() {
    Foo.field = 3;
    assertEquals(3, Foo.field--);
    assertEquals("Foo2", Foo.staticValue());
    assertEquals("nativeFoo", Foo.staticNative());
    assertEquals("FooSub", Foo.staticNativeToSub());

    Bar.field = 10;
    assertEquals(11, ++Bar.field);
    assertEquals("Bar11", Bar.staticValue());
    assertEquals("nativeBar", Bar.staticNative());
  }

  public native void testStaticAccessNative() /*-{
    @com.google.gwt.dev.jjs.test.JsoTest.Foo::field = 3;
    @junit.framework.Assert::assertEquals(II)(3, @com.google.gwt.dev.jjs.test.JsoTest.Foo::field--);
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("Foo2", @com.google.gwt.dev.jjs.test.JsoTest.Foo::staticValue()());
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("nativeFoo", @com.google.gwt.dev.jjs.test.JsoTest.Foo::staticNative()());

    @com.google.gwt.dev.jjs.test.JsoTest.Bar::field = 10;
    @junit.framework.Assert::assertEquals(II)(11, ++@com.google.gwt.dev.jjs.test.JsoTest.Bar::field);
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("Bar11", @com.google.gwt.dev.jjs.test.JsoTest.Bar::staticValue()());
    @junit.framework.Assert::assertEquals(Ljava/lang/Object;Ljava/lang/Object;)("nativeBar", @com.google.gwt.dev.jjs.test.JsoTest.Bar::staticNative()());
  }-*/;

  public void testStaticAccessSubclass() {
    FooSub.field = 3;
    assertEquals(3, FooSub.field--);
    assertEquals("Foo2", FooSub.staticValue());
    assertEquals("nativeFoo", FooSub.staticNative());
  }
}
