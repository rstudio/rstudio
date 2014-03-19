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
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsProgram;
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
    jsProgram = new JsProgram();
  }

  public void testInlineJSNIMethod() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("package test;\n");
    code.append("import com.google.gwt.core.client.GWT;\n");
    code.append("import com.google.gwt.core.client.RunAsyncCallback;\n");
    code.append("public class EntryPoint {\n");
    code.append("  public static native void inlinableJSNI() /*-{ $wnd; }-*/; ");
    code.append("  public static void functionA() { main(); }\n");
    code.append("  public static void main() {\n");
    code.append("    inlinableJSNI();");
    code.append("  }\n");
    code.append("}\n");

    Set<JsNode> functionForJsInlining = compileSnippet(code.toString()).getRight();
    assertContainsAll(functionForJsInlining, "main", "inlinableJSNI");
    assertDoesNotContainsAny(functionForJsInlining, "functionA");
  }

  private void assertContainsAll(Set<JsNode> functionsForJsInlining,
      String... functionNames) {
    Set<String> remainingFunctions = Sets.newHashSet(functionNames);
    for (JsFunction function : Iterables.filter(functionsForJsInlining, JsFunction.class)) {
      remainingFunctions.remove(function.getName().getShortIdent());
    }
    assertTrue("{" + (Joiner.on(",").join(remainingFunctions)) + "} marked for consideration in "
        + "JsInliner", remainingFunctions.isEmpty());
  }

  private void assertDoesNotContainsAny(Set<JsNode> functionsForJsInlining,
      String... functionNames) {
    Set<String> remainingFunctions = Sets.newHashSet(functionNames);
    for (JsFunction function : Iterables.filter(functionsForJsInlining, JsFunction.class)) {
      assertFalse(function.getName().getShortIdent() + " should not be considered for JsInliner",
        remainingFunctions.contains(function.getName().getShortIdent()));
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
