/*
 * Copyright 2015 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests JsFunction functionality.
 */
public class JsFunctionTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  // separate java call and js calls into two tests to see if it works correctly.
  public void testJsFunctionBasic_js() {
    MyJsFunctionInterface jsFunctionInterface = new MyJsFunctionInterface() {
      @Override
      public int foo(int a) {
        return a + 2;
      }
    };
    assertEquals(12, callAsFunction(jsFunctionInterface, 10));
  }

  public void testJsFunctionBasic_java() {
    MyJsFunctionInterface jsFunctionInterface = new MyJsFunctionInterface() {
      @Override
      public int foo(int a) {
        return a + 2;
      }
    };
    assertEquals(12, jsFunctionInterface.foo(10));
  }

  public void testJsFunctionViaFunctionMethods() {
    MyJsFunctionInterface jsFunctionInterface = new MyJsFunctionInterface() {
      @Override
      public int foo(int a) {
        return a + 2;
      }
    };
    assertEquals(12, callWithFunctionApply(jsFunctionInterface, 10));
    assertEquals(12, callWithFunctionCall(jsFunctionInterface, 10));
  }

  public void testJsFunctionSubImpl_js() {
    MySubclassOfJsFunctionInterfaceImpl impl = new MySubclassOfJsFunctionInterfaceImpl();
    assertEquals(21, callAsFunction(impl, 10));
  }

  public void testJsFunctionSubImpl_java() {
    MySubclassOfJsFunctionInterfaceImpl impl = new MySubclassOfJsFunctionInterfaceImpl();
    assertEquals(21, impl.foo(10));
  }

  public void testJsFunctionIdentity_js() {
    MyJsFunctionIdentityInterface id = new MyJsFunctionIdentityInterface() {
      @Override
      public Object identity() {
        return this;
      }
    };
    assertEquals(id, callAsFunction(id));
  }

  public void testJsFunctionIdentity_java() {
    MyJsFunctionIdentityInterface id = new MyJsFunctionIdentityInterface() {
      @Override
      public Object identity() {
        return this;
      }
    };
    assertEquals(id, id.identity());
  }

  public void testJsFunctionAccess() {
    MyJsFunctionInterface intf = new MyJsFunctionInterface() {
      public int publicField;
      @Override
      public int foo(int a) {
        return a;
      }
    };
    JsTypeTest.assertJsTypeDoesntHaveFields(intf, "foo");
    JsTypeTest.assertJsTypeDoesntHaveFields(intf, "publicField");
  }

  public void testJsFunctionCallFromAMember() {
    MyJsFunctionInterfaceImpl impl = new MyJsFunctionInterfaceImpl();
    assertEquals(16, impl.callFoo(10));
  }

  public void testJsFunctionJs2Java() {
    MyJsFunctionInterface intf = createMyJsFunction();
    assertEquals(10, intf.foo(10));
  }

  public void testJsFunctionSuccessiveCalls() {
    assertEquals(12, new MyJsFunctionInterface() {
      @Override
      public int foo(int a) {
        return a + 2;
      }
    }.foo(10));
    assertEquals(10, createMyJsFunction().foo(10));
  }

  public void testJsFunctionCallbackPattern() {
    MyClassAcceptsJsFunctionAsCallBack c = new MyClassAcceptsJsFunctionAsCallBack();
    c.setCallBack(createMyJsFunction());
    assertEquals(10, c.triggerCallBack(10));
  }

  public void testJsFunctionReferentialIntegrity() {
    MyJsFunctionIdentityInterface intf = createReferentialFunction();
    assertEquals(intf, intf.identity());
  }

  public void testCast_fromJsFunction() {
    MyJsFunctionInterface c1 = (MyJsFunctionInterface) createFunction();
    assertNotNull(c1);
    MyJsFunctionIdentityInterface c2 = (MyJsFunctionIdentityInterface) createFunction();
    assertNotNull(c2);
    ElementLikeJsInterface i = (ElementLikeJsInterface) createFunction();
    assertNotNull(i);
    try {
      MyJsFunctionInterfaceImpl c3 = (MyJsFunctionInterfaceImpl) createFunction();
      assertNotNull(c3);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
  }

  public void testCast_fromJsObject() {
    ElementLikeJsInterface obj = (ElementLikeJsInterface) createObject();
    assertNotNull(obj);
    try {
      MyJsFunctionInterface c = (MyJsFunctionInterface) createObject();
      assertNotNull(c);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
    try {
      MyJsFunctionInterfaceImpl c = (MyJsFunctionInterfaceImpl) createObject();
      assertNotNull(c);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
    try {
      MyJsFunctionIdentityInterface c = (MyJsFunctionIdentityInterface) createObject();
      assertNotNull(c);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
  }

  public void testCast_inJava() {
    Object object = alwaysTrue() ? new MyJsFunctionInterfaceImpl() : new Object();
    MyJsFunctionInterface c1 = (MyJsFunctionInterface) object;
    assertNotNull(c1);
    MyJsFunctionInterfaceImpl c2 = (MyJsFunctionInterfaceImpl) c1;
    assertEquals(10, c2.publicField);
    MyJsFunctionInterfaceImpl c3 = (MyJsFunctionInterfaceImpl) object;
    assertNotNull(c3);
    MyJsFunctionIdentityInterface c4 = (MyJsFunctionIdentityInterface) object;
    assertNotNull(c4);
    ElementLikeJsInterface c5 = (ElementLikeJsInterface) object;
    assertNotNull(c5);
  }

  public void testCast_crossCastJavaInstance() {
    Object o = alwaysTrue() ? new MyJsFunctionInterfaceImpl() : new Object();
    assertEquals(11, ((MyOtherJsFunctionInterface) o).bar(10));
    assertSame((MyJsFunctionInterface) o, (MyOtherJsFunctionInterface) o);
  }

  public void testInstanceOf_jsFunction() {
    Object object = createFunction();
    assertTrue(object instanceof MyJsFunctionInterface);
    assertTrue(object instanceof MyJsFunctionIdentityInterface);
    assertFalse(object instanceof MyJsFunctionInterfaceImpl);
    assertTrue(object instanceof ElementLikeJsInterface);
  }

  public void testInstanceOf_jsObject() {
    Object object = createObject();
    assertFalse(object instanceof MyJsFunctionInterface);
    assertFalse(object instanceof MyJsFunctionIdentityInterface);
    assertFalse(object instanceof MyJsFunctionInterfaceImpl);
    assertTrue(object instanceof ElementLikeJsInterface);
  }

  public void testInstanceOf_javaInstance() {
    Object object = alwaysTrue() ? new MyJsFunctionInterfaceImpl() : new Object();
    assertTrue(object instanceof MyJsFunctionInterface);
    assertTrue(object instanceof MyJsFunctionInterfaceImpl);
    assertTrue(object instanceof MyJsFunctionIdentityInterface);
    assertTrue(object instanceof ElementLikeJsInterface);
  }

  // uncomment when Java8 is supported.
//  public void testJsFunctionLambda_JS() {
//    MyJsFunctionInterface jsFunctionInterface = a -> { return a + 2; };
//    assertEquals(12, callAsFunction(jsFunctionInterface, 10));
//    assertEquals(12, callAsCallBackFunction(jsFunctionInterface, 10));
//  }
//
//  public void testJsFunctionLambda_Java() {
//    MyJsFunctionInterface jsFunctionInterface = a -> { return a + 2; };
//    assertEquals(12, jsFunctionInterface.foo(10));
//  }
//
//  public void testJsFunctionDefaultMethod() {
//    MyJsFunctionSubInterfaceWithDefaultMethod impl =
//        new MyJsFunctionSubInterfaceWithDefaultMethod() {
//        };
//    assertEquals(10, impl.foo(10));
//    assertEquals(10, callAsFunction(impl, 10));
//  }

  private static native Object callAsFunction(Object fn) /*-{
    return fn();
  }-*/;

  private static native int callAsFunction(Object fn, int arg) /*-{
    return fn(arg);
  }-*/;

  private static native int callWithFunctionApply(Object fn, int arg) /*-{
    return fn.apply(this, [arg]);
  }-*/;

  private static native int callWithFunctionCall(Object fn, int arg) /*-{
    return fn.call(this, arg);
  }-*/;

  private static native void setField(Object object, String fieldName, int value) /*-{
    object[fieldName] = value;
  }-*/;

  private static native int getField(Object object, String fieldName) /*-{
    return object[fieldName];
  }-*/;

  private static native int callIntFunction(Object object, String functionName) /*-{
    return object[functionName]();
  }-*/;

  private static native MyJsFunctionInterface createMyJsFunction() /*-{
    var myFunction = function(a) { return a; };
    return myFunction;
  }-*/;

  private static native MyJsFunctionIdentityInterface createReferentialFunction() /*-{
    function myFunction() { return myFunction; }
    return myFunction;
  }-*/;

  private static native Object createFunction() /*-{
    var fun = function(a) { return a; };
    return fun;
  }-*/;

  private static native Object createObject() /*-{
    var a = {};
    return a;
  }-*/;

  private static native boolean alwaysTrue() /*-{
    return !!$wnd;
  }-*/;
}
