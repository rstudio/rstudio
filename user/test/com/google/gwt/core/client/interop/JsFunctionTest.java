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

  public void testJsFunctionSubInterface_js() {
    MySubinterfaceOfJsFunctionInterface impl = new MySubinterfaceOfJsFunctionInterface() {
      @Override
      public int foo(int a) {
        return a + 3;
      }
    };
    assertEquals(13, callAsFunction(impl, 10));
  }

  public void testJsFunctionSubInterface_java() {
    MySubinterfaceOfJsFunctionInterface impl = new MySubinterfaceOfJsFunctionInterface() {
        @Override
      public int foo(int a) {
        return a + 3;
      }
    };
    assertEquals(13, impl.foo(10));
  }

  public void testJsFunctionSubImpl_js() {
    MySubclassOfJsFunctionInterfaceImpl impl = new MySubclassOfJsFunctionInterfaceImpl();
    assertEquals(21, callAsFunction(impl, 10));
  }

  public void testJsFunctionSubImpl_java() {
    MySubclassOfJsFunctionInterfaceImpl impl = new MySubclassOfJsFunctionInterfaceImpl();
    assertEquals(21, impl.foo(10));
  }

  public void testJsFunctionMultipleInheritance_js() {
    MyJsFunctionMultipleInheritance impl = new MyJsFunctionMultipleInheritance();
    assertEquals(21, callAsFunction(impl, 10));
  }

  public void testJsFunctionMultipleInheritance_java() {
    MyJsFunctionMultipleInheritance impl = new MyJsFunctionMultipleInheritance();
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
    MyJsFunctionInterface intf = createMyFunction();
    assertEquals(10, intf.foo(10));
  }

  public void testJsFunctionSuccessiveCalls() {
    assertEquals(12, new MyJsFunctionInterface() {
      @Override
      public int foo(int a) {
        return a + 2;
      }
    }.foo(10));
    assertEquals(10, createMyFunction().foo(10));
  }

  public void testJsFunctionCallbackPattern() {
    MyClassAcceptsJsFunctionAsCallBack c = new MyClassAcceptsJsFunctionAsCallBack();
    c.setCallBack(createMyFunction());
    assertEquals(10, c.triggerCallBack(10));
  }

  public void testJsFunctionReferentialIntegrity() {
    MyJsFunctionIdentityInterface intf = createReferentialFunction();
    assertEquals(intf, intf.identity());
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

  // uncomment when accidental overrides are correctly computed.
//  public void testJsFunctionAccidentalOverrides() {
//    MyJsFunctionAccidentalOverridesParent p = new MyJsFunctionAccidentalOverridesParent();
//    assertEquals(10, p.foo(10));
//    assertEquals(10, callAsFunction(p, 10));
//    MyJsFunctionAccidentalOverridesChild c = new MyJsFunctionAccidentalOverridesChild();
//    assertEquals(10, c.foo(10));
//    assertEquals(10, callAsFunction(c, 10));
//  }

  private static native Object callAsFunction(Object obj) /*-{
    return obj();
  }-*/;

  private static native int callAsFunction(Object obj, int arg) /*-{
    return obj(arg);
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

  private static native MyJsFunctionInterface createMyFunction() /*-{
    var myFunction = function(a) { return a; };
    return myFunction;
  }-*/;

  private static native MyJsFunctionIdentityInterface createReferentialFunction() /*-{
    function myFunction() { return myFunction; }
    return myFunction;
  }-*/;
}
