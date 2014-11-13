/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests for the JsniRestrictionChecker.
 */
public class JsniRestrictionCheckerTest extends OptimizerTestBase {

  public void testStaticJsoDispatchSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  static final class Foo extends com.google.gwt.core.client.JavaScriptObject {",
        "    protected Foo() { };",
        "    static void foo() { };",
        "  }",
        "  native void jsniMeth(Object o) /*-{",
        "    @Buggy.Foo::foo()();",
        "  }-*/;",
        "}");

    optimize("void", "new Buggy().jsniMeth(null);");
  }

  public void testJsoInterfaceDispatchFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  interface IFoo {",
        "    void foo();",
        "  }",
        "  static final class Foo extends JavaScriptObject implements IFoo {",
        "    protected Foo() { };",
        "    public void foo() { };",
        "  }",
        "  native void jsniMeth(Object o) /*-{",
        "    new Object().@Buggy.IFoo::foo()();",
        "  }-*/;",
        "}");

    try {
      optimize("void", "new Buggy().jsniMeth(null);");
      fail("JsniRestrictionChecker should have prevented JSO interface dispatch in a JSNI method.");
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof UnableToCompleteException);
    }
  }

  public void testNonstaticJsoDispatchFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  native void jsniMeth(Object o) /*-{",
        "    new Object().@com.google.gwt.core.client.JavaScriptObject::toString()();",
        "  }-*/;",
        "}");

    try {
      optimize("void", "new Buggy().jsniMeth(null);");
      fail("JsniRestrictionChecker should have prevented non-static JSO class "
          + "dispatch in a JSNI method.");
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof UnableToCompleteException);
    }
  }

  public void testNonstaticJsoSubclassDispatchFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  static final class Foo extends com.google.gwt.core.client.JavaScriptObject {",
        "    protected Foo() { };",
        "    void foo() { };",
        "  }",
        "  native void jsniMeth(Object o) /*-{",
        "    new Object().@Buggy.Foo::foo()();",
        "  }-*/;",
        "}");

    try {
      optimize("void", "new Buggy().jsniMeth(null);");
      fail("JsniRestrictionChecker should have prevented non-static JSO subclass "
          + "dispatch in a JSNI method.");
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof UnableToCompleteException);
    }
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    try {
      JsniRestrictionChecker.exec(TreeLogger.NULL, program);
    } catch (UnableToCompleteException e) {
      throw new RuntimeException(e);
    }
    return false;
  }
}
