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

import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

/**
 * Tests JsType and JsExport.
 */
public class JsTypeTest extends GWTTestCase {

  @Override
  protected void gwtSetUp() throws Exception {
    ScriptInjector.fromString("function MyJsInterface() {}\n" +
      "MyJsInterface.prototype.sum = function sum(bias) { return this.x + this.y + bias; }\n" +
      "MyJsInterface.prototype.go = function(cb) { cb('Hello'); }")
        .setWindow(ScriptInjector.TOP_WINDOW).inject();
    ScriptInjector.fromString("function MyJsInterface() {}\n" +
        "MyJsInterface.prototype.sum = function sum(bias) { return this.x + this.y +   bias; }\n")
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

  private native int callShouldBeAvailable(Object ref) /*-{
    return ref.shouldBeAvailable();
  }-*/;

  private native String getEnumNameViaJs(NestedTest.NestedEnum ref) /*-{
      return ref.name2();
  }-*/;

  private native int getFooBAR() /*-{
      return $wnd && $wnd.foo && $wnd.foo.NamespaceTester && $wnd.foo.NamespaceTester.BAR || 0;
  }-*/;

  private native int getWOO() /*-{
      return $wnd && $wnd.woo && $wnd.woo.PackageNamespaceTester
          && $wnd.woo.PackageNamespaceTester.WOO || 0;
  }-*/;

  private native Object getStaticInitializerStaticField() /*-{
      return $wnd && $wnd.woo && $wnd.woo.StaticInitializerStaticField
          && $wnd.woo.StaticInitializerStaticField.STATIC;
  }-*/;

  private native Object getStaticInitializerStaticMethod() /*-{
      return $wnd && $wnd.woo && $wnd.woo.StaticInitializerStaticMethod
          && $wnd.woo.StaticInitializerStaticMethod.getInstance();
  }-*/;

  private native Object getStaticInitializerVirtualMethod() /*-{
      if($wnd && $wnd.woo && $wnd.woo.StaticInitializerVirtualMethod) {
          var obj = new $wnd.woo.StaticInitializerVirtualMethod();
          return obj.getInstance();
      }
      return null;
  }-*/;

  public void testClassNamespace() {
    assertEquals(NamespaceTester.BAR, getFooBAR());
  }

  public void testPackageNamespace() {
    assertEquals(PackageNamespaceTester.WOO, getWOO());
  }

  public void testStaticInitializerStaticField() {
    assertNotNull(getStaticInitializerStaticField());
  }

  public void testStaticInitializerStaticMethod() {
    assertNotNull(getStaticInitializerStaticMethod());
  }

  public void testStaticInitializerVirtualMethod() {
    assertNotNull(getStaticInitializerVirtualMethod());
  }

  public void testVirtualUpRefs() {
    ListImpl l2 = new ListImpl();
    FooImpl f2 = new FooImpl(); // both inherit .add(), but this one shouldn't be exported
    // prevent type tightening, force c to be Collection holding l2
    Collection c = localMyClass() != null ? l2 : f2;
    // should invoke obfuscated method
    c.add("Hello");
    assertEquals("HelloListImpl", l2.x);
    // force ListImpl to be assigned to collection without tightening
    Collection c2 = localMyClass() != null ? f2 : l2;
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

  public void testJsTypeNestedEnum() {
      assertEquals(NestedTest.NestedEnum.FOO.name(),
        getEnumNameViaJs(NestedTest.NestedEnum.FOO));
  }

  public void testJsTypeCallableFromJs() {
    MyJsTypeClass jsType = new MyJsTypeClass();
    assertEquals(1138, callShouldBeAvailable(jsType));
  }

  public void testSubClassWithSuperCalls() {
    MyClassImpl mc = new MyClassImpl();
    assertEquals(150, mc.sum(1));
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
    assertTrue(MyClassImpl2.calledFromJsHostPageWindow);

    // Test exported method can be called from JS in module window
    ScriptInjector.fromString("exportedFromJava2();").inject();
    assertTrue(MyClassImpl2.calledFromJsModuleWindow);

    // Test exported constructor called from JS in module window
    ScriptInjector.fromString("new $wnd.MyClassImpl3();").inject();
    assertTrue(MyClassImpl3.calledFromJsModuleWindow);

    // This is to reproduce the problem where we incorrectly type-tighten an exported method params.
    ScriptInjector.fromString("$wnd.MyClassImpl3.foo($wnd.newA());").inject();
    assertTrue(MyClassImpl3.calledFromBar);
  }

  public void testCasts() {
    MyJsInterface doc1 = null;
    MyJsInterface.LocalMyClass doc2 = null;
    MyJsInterface.ButtonLikeJso doc3 = null;
    try {
      assertNotNull(doc1 = (MyJsInterface) mainMyClass());
      assertNotNull(doc2 = (MyJsInterface.LocalMyClass) localMyClass());
      assertNotNull(doc2 = (MyJsInterface.LocalMyClass) mainMyClass());
    } catch (ClassCastException cce) {
      fail();
    }

    try {
      assertNotNull(doc1 = (MyJsInterface) localMyClass());
      fail();
    } catch (ClassCastException cce) {
    }

    try {
      assertNotNull(doc3 = (MyJsInterface.ButtonLikeJso) mainMyClass());
      assertNotNull(doc3 = (MyJsInterface.ButtonLikeJso) localMyClass());
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
    assertTrue(mainMyClass() instanceof MyJsInterface);
    assertTrue(localMyClass() instanceof MyJsInterface.LocalMyClass);
    assertTrue(mainMyClass() instanceof MyJsInterface.LocalMyClass);

    // check that JsTypes without prototypes can cross-cast like JSOs
    assertTrue(mainMyClass() instanceof MyJsInterface.ButtonLikeJso);
    assertTrue(localMyClass() instanceof MyJsInterface.ButtonLikeJso);

    // check that it doesn't work if $wnd is forced
    assertFalse(localMyClass() instanceof MyJsInterface);
  }

  static native boolean isIE8() /*-{
    return $wnd.navigator.userAgent.toLowerCase().indexOf('msie') != -1 && $doc.documentMode == 8;
  }-*/;

  static native boolean isFirefox40OrEarlier() /*-{
    return @com.google.gwt.dom.client.DOMImplMozilla::isGecko2OrBefore()();
  }-*/;

  // TODO: re-enable after removing $wnd support in casts.
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testInstanceOfNative() {
    if (isIE8() || isFirefox40OrEarlier()) {
      return;
    }
    Object obj = makeNativeButton();
    assertTrue(obj instanceof Object);
    assertTrue(obj instanceof HTMLButtonElement);
    assertTrue(obj instanceof HTMLElement);
    assertFalse(obj instanceof Iterator);
    assertTrue(obj instanceof HTMLAnotherElement);
    assertFalse(obj instanceof MyJsInterface.LocalMyClass);

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
    assertFalse(obj instanceof MyJsInterface.LocalMyClass);
  }

  private native boolean alwaysTrue() /*-{
    return !!$wnd;
  }-*/;

  private native Object makeNativeButton() /*-{
    return $doc.createElement("button");
  }-*/;

  private native Object localMyClass() /*-{
    return new MyJsInterface();
  }-*/;

  private native Object mainMyClass() /*-{
    return new $wnd.MyJsInterface();
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
