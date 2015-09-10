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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.PrecompileTaskOptionsImpl;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConditionNone;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Unit test for {@link GenerateJavaScriptAST}.
 */
public class GenerateJavaScriptAstTest extends FullCompileTestBase {

  // Compilation Configuration Properties.
  @Override
  public void setUp() throws Exception {
    // Compilation Configuration Properties.
    BindingProperty stackMode = new BindingProperty("compiler.stackMode");
    stackMode.addDefinedValue(new ConditionNone(), "STRIP");
    setProperties(new BindingProperty[]{stackMode}, new String[]{"STRIP"},
        new ConfigurationProperty[]{});
    super.setUp();
  }

  public void testInlineJSNIMethod() throws UnableToCompleteException {
    String code = Joiner.on('\n').join(
        "package test;",
        "import com.google.gwt.core.client.GWT;",
        "import com.google.gwt.core.client.RunAsyncCallback;",
        "public class EntryPoint {",
        "  public static native void inlinableJSNI() /*-{ $wnd; }-*/; ",
        "  public static void functionA() { onModuleLoad(); }",
        "  public static void onModuleLoad() {",
        "    inlinableJSNI();",
        "  }",
        "}");

    Set<JsNode> functionForJsInlining = compileSnippetToJS(code).getRight();
    assertContainsAll(functionForJsInlining, "onModuleLoad", "inlinableJSNI");
    assertDoesNotContainsAny(functionForJsInlining, "functionA");
  }

  public void testInlineFunctionDefinedInJSNI() throws UnableToCompleteException {
    String code = Joiner.on('\n').join(
        "package test;",
        "import com.google.gwt.core.client.GWT;",
        "import com.google.gwt.core.client.RunAsyncCallback;",
        "public class EntryPoint {",
        "  public static native void inlinableJSNI() /*-{ (function () { return $wnd;})(); }-*/;",
        "  public static void functionA() { onModuleLoad(); }",
        "  public static void onModuleLoad() {",
        "    inlinableJSNI();",
        "  }",
        "}");

    Set<JsNode> functionForJsInlining = compileSnippetToJS(code.toString()).getRight();
    assertContainsAll(functionForJsInlining, "onModuleLoad", "inlinableJSNI",
        "function(){ return $wnd; }");
    assertDoesNotContainsAny(functionForJsInlining, "functionA");
  }

  private void assertContainsAll(Set<JsNode> functionsForJsInlining,
      String... functionNamesorContents) {
    Set<String> remainingFunctions = Sets.newHashSet(functionNamesorContents);
    for (JsFunction function : Iterables.filter(functionsForJsInlining, JsFunction.class)) {
      JsName name = function.getName();
      if (name == null) {
        remainingFunctions.remove(function.toString().replaceAll("\\s+"," ").trim());
        continue;
      }
      remainingFunctions.remove(name.getShortIdent());
    }
    assertTrue("{" + (Joiner.on(",").join(remainingFunctions)) + "} not marked for consideration in "
        + "JsInliner", remainingFunctions.isEmpty());
  }

  private void assertDoesNotContainsAny(Set<JsNode> functionsForJsInlining,
      String... functionNames) {
    Set<String> remainingFunctions = Sets.newHashSet(functionNames);
    for (JsFunction function : Iterables.filter(functionsForJsInlining, JsFunction.class)) {
      JsName name = function.getName();
      if (name == null) {
        continue;
      }
      assertFalse(name.getShortIdent() + " should not be considered for JsInliner",
          remainingFunctions.contains(name.getShortIdent()));
    }
  }

  @Override
  protected void optimizeJava() {
  }

  @Override
  protected CompilerContext provideCompilerContext() {
    PrecompileTaskOptions options = new PrecompileTaskOptionsImpl();
    options.setOutput(JsOutputOption.PRETTY);
    options.setRunAsyncEnabled(false);
    return new CompilerContext.Builder().options(options).build();
  }
}
