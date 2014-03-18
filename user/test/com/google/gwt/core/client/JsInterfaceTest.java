/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.client;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.impl.PrototypeOfJsInterface;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

/**
 * Tests JsInterface and JsExport.
 */
@DoNotRunWith({Platform.Devel, Platform.HtmlUnitBug})
public class JsInterfaceTest extends GWTTestCase {

  @JsInterface(prototype = "$wnd.MyClass")
  interface MyClass {

    @JsInterface(prototype = "MyClass")
    interface LocalMyClass {
    }

    @JsInterface
    interface ButtonLikeJso {
    }

    @JsProperty
    int x();

    @JsProperty
    MyClass x(int a);

    @JsProperty
    int getY();

    @JsProperty
    void setY(int a);

    int sum(int bias);

    @PrototypeOfJsInterface
    static class Prototype implements MyClass {

      @Override
      public int x() {
        return 0;
      }

      @Override
      public MyClass x(int a) {
        return this;
      }

      @Override
      public int getY() {
        return 0;
      }

      @Override
      public void setY(int a) {
      }

      @Override
      public int sum(int bias) {
        return 0;
      }
    }
  }

  static class MyClassImpl extends JsInterfaceTest.MyClass.Prototype {
    public static boolean calledFromJsHostPageWindow = false;
    public static boolean calledFromJsModuleWindow = false;

    MyClassImpl() {
      x(42).setY(7);
    }

    public int sum(int bias) {
      return super.sum(bias) + 100;
    }

    @JsExport("$wnd.exportedFromJava")
    public static void callMe() {
      calledFromJsHostPageWindow = true;
    }

    @JsExport("exportedFromJava2")
    public static void callMe2() {
      calledFromJsModuleWindow = true;
    }
  }

  @JsInterface(prototype = "HTMLElement")
  interface HTMLElement {
    @PrototypeOfJsInterface
    static class Prototype implements HTMLElement {
    }
  }

  @JsInterface(prototype = "HTMLElement")
  interface HTMLAnotherElement {
    @PrototypeOfJsInterface
    static class Prototype implements HTMLAnotherElement {
    }
  }

  @JsInterface(prototype = "HTMLButtonElement")
  interface HTMLButtonElement extends HTMLElement {
    @PrototypeOfJsInterface
    static class Prototype implements HTMLButtonElement {
    }
  }

  static class MyButtonWithIterator extends JsInterfaceTest.HTMLButtonElement.Prototype implements Iterable {

    @Override
    public Iterator iterator() {
      return null;
    }
  }

  @Override
  protected void gwtSetUp() throws Exception {
    ScriptInjector.fromString("function MyClass() {}\n" +
        "MyClass.prototype.sum = function sum(bias) { return this.x + this.y + bias; }\n" +
        "MyClass.prototype.go = function(cb) { cb('Hello'); }")
        .setWindow(ScriptInjector.TOP_WINDOW).inject();
    ScriptInjector.fromString("function MyClass() {}\n" +
        "MyClass.prototype.sum = function sum(bias) { return this.x + this.y + bias; }\n")
        .inject();
    patchPrototype(MyClassImpl.class);
  }

  /**
   * Workaround for the fact that the script is injected after defineClass() has been called.
   */
  private native void patchPrototype(Class<MyClassImpl> myClass) /*-{
      @java.lang.Class::getPrototypeForClass(Ljava/lang/Class;)(myClass).prototype = $wnd.MyClass;
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public void testSubClassWithSuperCalls() {
    MyClassImpl mc = new MyClassImpl();
    assertEquals(150, mc.sum(1));

    // Test exported method can be called from JS in host page
    ScriptInjector.fromString("exportedFromJava();").setWindow(ScriptInjector.TOP_WINDOW).inject();
    assertTrue(MyClassImpl.calledFromJsHostPageWindow);

    // Test exported method can be called from JS in module window
    ScriptInjector.fromString("exportedFromJava2();").inject();
    assertTrue(MyClassImpl.calledFromJsModuleWindow);
  }

  public void testJsProperties() {
    MyClassImpl mc = new MyClassImpl();
    // test both fluent and non-fluent accessors
    mc.x(-mc.x()).setY(0);
    assertEquals(58, mc.sum(0));
    // TODO(cromwellian): Add test cases for property overriding of @JsProperty methods in java object
  }

  public void testJsExports() {
    // Test exported method can be called from JS in host page
    ScriptInjector.fromString("exportedFromJava();").setWindow(ScriptInjector.TOP_WINDOW).inject();
    assertTrue(MyClassImpl.calledFromJsHostPageWindow);

    // Test exported method can be called from JS in module window
    ScriptInjector.fromString("exportedFromJava2();").inject();
    assertTrue(MyClassImpl.calledFromJsModuleWindow);
  }

  public void testCasts() {
    MyClass doc1 = null;
    MyClass.LocalMyClass doc2 = null;
    MyClass.ButtonLikeJso doc3 = null;
    try {
      assertNotNull(doc1 = (MyClass) mainMyClass());
      assertNotNull(doc2 = (MyClass.LocalMyClass) localMyClass());
      assertNotNull(doc2 = (MyClass.LocalMyClass) mainMyClass());
    } catch (ClassCastException cce) {
      fail();
    }

    try {
      assertNotNull(doc1 = (MyClass) localMyClass());
      fail();
    } catch (ClassCastException cce) {
    }

    try {
      assertNotNull(doc3 = (MyClass.ButtonLikeJso) mainMyClass());
      assertNotNull(doc3 = (MyClass.ButtonLikeJso) localMyClass());
    } catch (ClassCastException cce) {
      fail();
    }

    /*
     * If full optimizations are turned on, it is possible for the compiler to dead-strip the
     * doc1/doc2/doc3 variables since they are not used, therefore the casts could potentially
     * be stripped
     */
    assertNotNull(doc1);
    assertNotNull(doc2);
    assertNotNull(doc3);
  }

  public void testInstanceOf() {
    // check that instanceof works between frames
    assertTrue(mainMyClass() instanceof MyClass);
    assertTrue(localMyClass() instanceof MyClass.LocalMyClass);
    assertTrue(mainMyClass() instanceof MyClass.LocalMyClass);

    // check that JsInterfaces without prototypes can cross-cast like JSOs
    assertTrue(mainMyClass() instanceof MyClass.ButtonLikeJso);
    assertTrue(localMyClass() instanceof MyClass.ButtonLikeJso);

    // check that it doesn't work if $wnd is forced
    assertFalse(localMyClass() instanceof MyClass);
  }

  public void testInstanceOfNative() {
    Object obj = makeNativeButton();
    assertTrue(obj instanceof Object);
    assertTrue(obj instanceof HTMLButtonElement);
    assertTrue(obj instanceof HTMLElement);
    assertFalse(obj instanceof Iterator);
    assertTrue(obj instanceof HTMLAnotherElement);
    assertFalse(obj instanceof MyClass.LocalMyClass);

    // to foil type tightening
    obj = alwaysTrue() ? new MyButtonWithIterator() : null;
    assertTrue(obj instanceof Object);
    assertTrue(obj instanceof HTMLButtonElement);
    assertTrue(obj instanceof HTMLElement);
    assertTrue(obj instanceof Iterable);
    /*
     * TODO: this works, but only because Object can't be type-tightened to HTMLElement. But it will
     * evaluate statically to false for HTMLElement instanceof HTMLAnotherElement. Depending on
     * what the spec decides, fix JTypeOracle so that canTheoreticallyCast returns the appropriate
     * result, as well as add a test here that can be type-tightened.
     */
    assertTrue(obj instanceof HTMLAnotherElement);
    assertFalse(obj instanceof MyClass.LocalMyClass);
  }

  private native boolean alwaysTrue() /*-{
    return !!$wnd;
  }-*/;

  private native Object makeNativeButton() /*-{
    return $doc.createElement("button");
  }-*/;

  private native Object localMyClass() /*-{
    return new MyClass();
  }-*/;

  private native Object mainMyClass() /*-{
    return new $wnd.MyClass();
  }-*/;
  /*
   * TODO (cromwellian): Add test case for following:
   * interface ANonJsInterface {
   *  void methodA()
   * }
   * interface AJsInterface {
   * void methodA()
   * }
   * class MyClass implements ANonJsInterface, AJsInterface {
   *   void methodA() { ... }
   * }
   * verify methodA() is dispatched properly from both interfaces.
   * Add similar test case with methodA implemented in parent JS class.
  */
}
