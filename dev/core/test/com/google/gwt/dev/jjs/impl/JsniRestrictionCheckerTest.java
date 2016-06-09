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

  public void testInvocationInDevirtualizedTypesSucceeds() throws Exception {
    addSnippetClassDecl(
        "static class Buggy {",
        "  interface IBar {",
        "    void bar();",
        "  }",
        "  static final class Foo ",
        "      extends com.google.gwt.core.client.JavaScriptObject implements IBar {",
        "    protected Foo() { };",
        "    void foo() { };",
        "    public void bar() { };",
        "    static void staticFoo() { };",
        "  }",
        "  native void jsniMethod(Object o) /*-{",
        "    @java.lang.Double::new(D)();",
        "    @java.lang.Boolean::new(Z)();",
        "    new Object().@java.lang.Number::doubleValue()();",
        "    new Object().@java.lang.Double::doubleValue()();",
        "    new Object().@Buggy.Foo::foo()();",
        "    new Object().@Buggy.IBar::bar()();",
        "    @Buggy.Foo::staticFoo()();",
        "  }-*/;",
        "}");
    assertCompileSucceeds("new Buggy().jsniMethod(null);");
  }

  public void testReferenceToDevirtualizedInstanceMethodFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  native void jsniMethod(Object o) /*-{",
        "    var a = new Object().@java.lang.Double::doubleValue();",
        "  }-*/;",
        "}");

    assertCompileFails("new Buggy().jsniMethod(null);",
        "Line 6: Method 'double Double.doubleValue()' is implemented by devirtualized "
            + "type 'Double' JSO and can only be used in calls within a JSNI method body.");
  }

  public void testReferenceToTrampolineWarns() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  native void jsniMethod(Object o) /*-{",
        "    var a = new Object().@java.lang.Number::doubleValue();",
        "    var a = new Object().@java.lang.CharSequence::charAt(I);",
        "    var a = \"Hello\".@java.lang.Object::toString();",
        "  }-*/;",
        "}");

    assertCompileSucceeds("new Buggy().jsniMethod(null);",
        "Line 6: Unsafe reference to method 'double Number.doubleValue()'. "
            + "Instance methods from 'Number' should not be called on Boolean, Double, String, "
            + "Array or JSO instances from  within a JSNI method body.",
        "Line 7: Unsafe reference to method 'char CharSequence.charAt(int)'. Instance methods from"
            + " 'CharSequence' should not be called on Boolean, Double, String, Array or JSO"
            + " instances from  within a JSNI method body.",
        "Line 8: Unsafe reference to method 'String Object.toString()'. Instance methods from "
            + "'Object' should not be called on Boolean, Double, String, Array or JSO instances "
            + "from  within a JSNI method body.");
  }

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

    assertCompileSucceeds("new Buggy().jsniMeth(null);");
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
        "  native void jsniMethod(Object o) /*-{",
        "    var a = new Object().@Buggy.IFoo::foo();",
        "  }-*/;",
        "}");

    assertCompileFails("new Buggy().jsniMethod(null);",
        "Line 13: Method 'void EntryPoint.Buggy.IFoo.foo()' is implemented by a JSO and can only "
            + "be used in calls within a JSNI method body.");
  }

  public void testNonstaticJsoDispatchFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  native void jsniMethod(Object o) /*-{",
        "    var a = new Object().@com.google.gwt.core.client.JavaScriptObject::toString();",
        "  }-*/;",
        "}");

    assertCompileFails("new Buggy().jsniMethod(null);",
        "Line 6: Method 'String JavaScriptObject.toString()' is implemented by a JSO and can "
            + "only be used in calls within a JSNI method body.");
  }

  public void testNonstaticJsoSubclassDispatchFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  static final class Foo extends com.google.gwt.core.client.JavaScriptObject {",
        "    protected Foo() { };",
        "    void foo() { };",
        "  }",
        "  native void jsniMethod(Object o) /*-{",
        "    var a = new Object().@Buggy.Foo::foo();",
        "  }-*/;",
        "}");

    assertCompileFails("new Buggy().jsniMethod(null);",
        "Line 10: Method 'void EntryPoint.Buggy.Foo.foo()' is implemented by a JSO and can "
            + "only be used in calls within a JSNI method body.");
  }

  public void testStringInstanceMethodCallFail() throws Exception {
    addSnippetClassDecl(
        "static class Buggy {",
        "  static String foo;",
        "  native void jsniMethod(Object o) /*-{",
        "    var a = \"Hello\".@java.lang.String::length();",
        "  }-*/;",
        "}");

    assertCompileFails("new Buggy().jsniMethod(null);",
        "Line 6: Method 'int String.length()' is implemented by devirtualized type 'String' "
            + "JSO and can only be used in calls within a JSNI method body.");
  }

  public void testStringStaticMethodCallSucceeds() throws Exception {
    addSnippetClassDecl(
        "static class Buggy {",
        "  static String foo;",
        "  native void jsniMethod(Object o) /*-{",
        "    var a = @java.lang.String::valueOf(Z);",
        "  }-*/;",
        "}");

    assertCompileSucceeds("new Buggy().jsniMethod(null);");
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method)
      throws UnableToCompleteException {
    JsniRestrictionChecker.exec(logger, program);
    return false;
  }
}
