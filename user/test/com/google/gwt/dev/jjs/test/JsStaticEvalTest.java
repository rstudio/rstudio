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

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Most of these tests (the "do" ones guarded by isScript tests) verify that
 * declarations in pruned code still happen. Those tests do not run reliably in
 * Development Mode due to browser inconsistencies; however it should run in
 * Production Mode due to our normalizations.
 */
public class JsStaticEvalTest extends GWTTestCase {
  @SuppressWarnings("unused")
  private static volatile boolean TRUE = true;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testAfterReturn() {
    if (GWT.isScript()) {
      doTestAfterReturn();
    }
  }

  public void testAfterReturnInBlock() {
    if (GWT.isScript()) {
      doTestAfterReturnInBlock();
    }
  }

  public void testConditionalFalse() {
    if (GWT.isScript()) {
      doTestConditionalFalse();
    }
  }

  public void testConditionalTrue() {
    if (GWT.isScript()) {
      doTestConditionalTrue();
    }
  }

  public void testForFalse() {
    if (GWT.isScript()) {
      doTestForFalse();
    }
  }

  public void testIfFalse() {
    if (GWT.isScript()) {
      doTestIfFalse();
    }
  }

  public void testOrder1() {
    if (GWT.isScript()) {
      doTestOrder1();
    }
  }

  public void testOrder2() {
    if (GWT.isScript()) {
      doTestOrder2();
    }
  }

  public void testShortCircuitAnd() {
    if (GWT.isScript()) {
      doTestShortCircuitAnd();
    }
  }

  public void testShortCircuitOr() {
    if (GWT.isScript()) {
      doTestShortCircuitOr();
    }
  }

  public native void testTripleNegate() /*-{
    @junit.framework.Assert::assertFalse(Z)(
      !!!@com.google.gwt.dev.jjs.test.JsStaticEvalTest::TRUE);
  }-*/;

  public void testWhileFalse() {
    if (GWT.isScript()) {
      doTestWhileFalse();
    }
  }

  private native void doTestAfterReturn() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    var result;
    function func() {
      result = true;
    }
  }-*/;

  private native void doTestAfterReturnInBlock() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    {
      var result;
      function func() {
        result = true;
      }
    }
  }-*/;

  private native void doTestConditionalFalse() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    var result;
    var toss = false ? function func() {
      result = true;
    } : false;
  }-*/;

  private native void doTestConditionalTrue() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    var result;
    var toss = true ? true : function func() {
      result = true;
    };
  }-*/;

  private native void doTestForFalse() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    for (;false;) {
      var result;
      function func() {
        result = true;
      }
    }
  }-*/;

  private native void doTestIfFalse() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    if (false) {
      var result;
      function func() {
        result = true;
      }
    }
  }-*/;

  private native void doTestOrder1() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    var result;
    var toss = function func() {
    };
    var toss = function func() {
      result = true;
    };
  }-*/;

  private native void doTestOrder2() /*-{
    var toss = function func() {
    };
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    var result;
    var toss = function func() {
      result = true;
    };
  }-*/;

  private native void doTestShortCircuitAnd() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    var result;
    var toss = false && function func() {
      result = true;
    };
  }-*/;

  private native void doTestShortCircuitOr() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    var result;
    var toss = true || function func() {
      result = true;
    };
  }-*/;

  private native void doTestWhileFalse() /*-{
    func();
    @junit.framework.Assert::assertTrue(Z)(result);
    return;
    while (false) {
      var result;
      function func() {
        result = true;
      }
    }
  }-*/;
}
