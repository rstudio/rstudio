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
package com.google.gwt.core.client.interop;

import static com.google.gwt.core.client.ScriptInjector.TOP_WINDOW;

import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

/**
 * Tests JsType and JsExport.
 */
public class JsTypeTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    ScriptInjector.fromString("function MyJsInterface() {}\n"
        + "MyJsInterface.prototype.sum = function sum(bias) { return this.x + this.y + bias; }\n"
        + "MyJsInterface.prototype.go = function(cb) { cb('Hello'); }")
        .setWindow(TOP_WINDOW).inject();
    patchPrototype(MyClassExtendsJsPrototype.class);
  }

  /**
   * Workaround for the fact that the script is injected after defineClass() has been called.
   */
  private native void patchPrototype(Class<MyClassExtendsJsPrototype> myClass) /*-{
      @java.lang.Class::getPrototypeForClass(Ljava/lang/Class;)(myClass).prototype = $wnd.MyClass;
  }-*/;

  public void testVirtualUpRefs() {
    ListImpl l2 = new ListImpl();
    FooImpl f2 = new FooImpl(); // both inherit .add(), but this one shouldn't be exported
    // prevent type tightening, force c to be Collection holding l2
    Collection c = alwaysTrue() ? l2 : f2;

    // should invoke obfuscated method
    c.add("Hello");
    assertEquals("HelloListImpl", l2.x);
    // force ListImpl to be assigned to collection without tightening
    Collection c2 = alwaysTrue() ? f2 : l2;
    c2.add("World");
    assertEquals("WorldCollectionBaseFooImpl", f2.x);

    // should call not through bridge
    f2.add("One");
    assertEquals("OneCollectionBaseFooImpl", f2.x);

    // TODO: fix me
    if (isIE8()) {
      return;
    }

    // call through bridge
    l2.add("Two");
    assertEquals("TwoListImpl", l2.x);
  }

  public void testJsTypeCallableFromJs() {
    MyJsTypeClass jsType = new MyJsTypeClass();
    assertEquals(1138, callShouldBeAvailable(jsType));
  }

  private static native int callShouldBeAvailable(Object ref) /*-{
    return ref.shouldBeAvailable();
  }-*/;

  public void testSubClassWithSuperCalls() {
    MyClassExtendsJsPrototype mc = new MyClassExtendsJsPrototype();
    assertEquals(150, mc.sum(1));
  }

  public void testJsProperties() {
    MyClassExtendsJsPrototype mc = new MyClassExtendsJsPrototype();
    // test both fluent and non-fluent accessors
    mc.x(-mc.x()).setY(0);
    assertEquals(58, mc.sum(0));
    // TODO(cromwellian): Add test cases for property overriding of @JsProperty methods in java object
  }

  public void testCasts() {
    MyJsInterface myClass;
    assertNotNull(myClass = (MyJsInterface) createMyJsInterface());

    try {
      assertNotNull(myClass = (MyJsInterface) createNativeButton());
      fail();
    } catch (ClassCastException cce) {
      // Expected.
    }

    ElementLikeJsInterface button;
    // JsTypes without prototypes can cross-cast like JSOs
    assertNotNull(button = (ElementLikeJsInterface) createMyJsInterface());

    /*
     * If the optimizations are turned on, it is possible for the compiler to dead-strip the
     * variables since they are not used. Therefore the casts could potentially be stripped.
     */
    assertNotNull(myClass);
    assertNotNull(button);
  }

  public void testInstanceOf() {
    assertTrue(createMyJsInterface() instanceof MyJsInterface);

    // JsTypes without prototypes can cross-cast like JSOs
    assertTrue(createMyJsInterface() instanceof ElementLikeJsInterface);
  }

  public void testInstanceOfNative() {
    Object obj = createNativeButton();
    assertTrue(obj instanceof Object);
    assertTrue(obj instanceof HTMLButtonElement);
    assertTrue(obj instanceof HTMLElement);
    assertFalse(obj instanceof Iterator);
    assertTrue(obj instanceof HTMLAnotherElement);
    assertFalse(obj instanceof MyJsInterface);

    // to foil type tightening
    obj = alwaysTrue() ? new MyCustomHtmlButtonWithIterator() : null;
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
    assertFalse(obj instanceof MyJsInterface);
  }

  public void testInstanceOfWithNameSpace() {
    Object obj1 = createMyNamespacedJsInterface();
    Object obj2 = createMyWrongNamespacedJsInterface();

    assertTrue(obj1 instanceof MyNamespacedJsInterface);
    assertFalse(obj1 instanceof MyJsInterface);

    assertFalse(obj2 instanceof MyNamespacedJsInterface);
  }

  private static native boolean alwaysTrue() /*-{
    return !!$wnd;
  }-*/;

  private static native Object createNativeButton() /*-{
    return $doc.createElement("button");
  }-*/;

  private static native Object createMyJsInterface() /*-{
    return new $wnd.MyJsInterface();
  }-*/;

  private static native Object createMyNamespacedJsInterface() /*-{
    $wnd.testfoo = {};
    $wnd.testfoo.bar = {};
    $wnd.testfoo.bar.MyJsInterface = function(){};
    return new $wnd.testfoo.bar.MyJsInterface();
  }-*/;

  private static native Object createMyWrongNamespacedJsInterface() /*-{
    $wnd["testfoo.bar.MyJsInterface"] = function(){};
    return new $wnd['testfoo.bar.MyJsInterface']();
  }-*/;

  private static native boolean isIE8() /*-{
    return $wnd.navigator.userAgent.toLowerCase().indexOf('msie') != -1 && $doc.documentMode == 8;
  }-*/;

  private static native boolean isFirefox40OrEarlier() /*-{
    return @com.google.gwt.dom.client.DOMImplMozilla::isGecko2OrBefore()();
  }-*/;

  /*
   * TODO (cromwellian): Add test case for following:
   * interface ANonJsType {
   *  void methodA()
   * }
   * interface AJsType {
   * void methodA()
   * }
   * class MyJsInterface implements ANonJsType, AJsType {
   *   void methodA() { ... }
   * }
   * verify methodA() is dispatched properly from both interfaces.
   * Add similar test case with methodA implemented in parent JS class.
   */
}
