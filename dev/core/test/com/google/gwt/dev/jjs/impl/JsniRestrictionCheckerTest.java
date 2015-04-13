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
        "  native void jsniMeth(Object o) /*-{",
        "    new Object().@Buggy.IFoo::foo()();",
        "  }-*/;",
        "}");

    assertCompileFails("new Buggy().jsniMeth(null);",
        "JSNI method test.EntryPoint$Buggy.jsniMeth(Ljava/lang/Object;)V attempts to call " +
            "method test.EntryPoint$Buggy$IFoo.foo()V on an instance which might be a " +
            "JavaScriptObject. Such a method call is only allowed in pure Java " +
            "(non-JSNI) functions.");
  }

  public void testNonstaticJsoDispatchFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "static class Buggy {",
        "  native void jsniMeth(Object o) /*-{",
        "    new Object().@com.google.gwt.core.client.JavaScriptObject::toString()();",
        "  }-*/;",
        "}");

    assertCompileFails("new Buggy().jsniMeth(null);",
        "JSNI method test.EntryPoint$Buggy.jsniMeth(Ljava/lang/Object;)V  attempts to call " +
            "non-static method " +
            "com.google.gwt.core.client.JavaScriptObject.toString()Ljava/lang/String; on an " +
            "instance which is a subclass of JavaScriptObject. Only static method calls on " +
            "JavaScriptObject subclasses are allowed in JSNI.");
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

    assertCompileFails("new Buggy().jsniMeth(null);",
        "JSNI method test.EntryPoint$Buggy.jsniMeth(Ljava/lang/Object;)V  attempts to call " +
            "non-static method test.EntryPoint$Buggy$Foo.foo()V on an instance which is a " +
            "subclass of JavaScriptObject. Only static method calls on JavaScriptObject " +
            "subclasses are allowed in JSNI.");
  }

  public void testStringInstanceMethodCallFail() throws Exception {
    addSnippetClassDecl(
        "static class Buggy {",
        "  static String foo;",
        "  native void jsniMeth(Object o) /*-{",
        "    \"Hello\".@java.lang.String::length()();",
        "  }-*/;",
        "}");

    assertCompileFails("new Buggy().jsniMeth(null);",
        "JSNI method test.EntryPoint$Buggy.jsniMeth(Ljava/lang/Object;)V attempts to call method " +
            "java.lang.String.length()I. Only static methods from java.lang.String can be called " +
            "from JSNI.");
  }

  public void testStringStaticMethodCallSucceeds() throws Exception {
    addSnippetClassDecl(
        "static class Buggy {",
        "  static String foo;",
        "  native void jsniMeth(Object o) /*-{",
        "    @java.lang.String::valueOf(Z)();",
        "  }-*/;",
        "}");

    assertCompileSucceeds("new Buggy().jsniMeth(null);");
  }

  public void testObjectInstanceMethodCallWarn() throws Exception {
    addSnippetClassDecl(
        "static class Buggy {",
        "  static String foo;",
        "  native void jsniMeth(Object o) /*-{",
        "    \"Hello\".@java.lang.Object::toString()();",
        "  }-*/;",
        "}");

    assertCompileSucceeds("new Buggy().jsniMeth(null);",
        "JSNI method test.EntryPoint$Buggy.jsniMeth(Ljava/lang/Object;)V calls method " +
            "java.lang.Object.toString()Ljava/lang/String;. " +
            "Instance java.lang.Object methods should not be called on String, Array or " +
            "JSO instances.");
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method)
      throws UnableToCompleteException {
    JsniRestrictionChecker.exec(logger, program);
    return false;
  }
}
