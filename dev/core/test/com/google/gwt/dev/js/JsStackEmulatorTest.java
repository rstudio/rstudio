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
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.PrecompileTaskOptionsImpl;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConditionNone;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.jjs.AstConstructor;
import com.google.gwt.dev.jjs.JavaAstConstructor;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.ComputeCastabilityInformation;
import com.google.gwt.dev.jjs.impl.FullCompileTestBase;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.ImplementCastsAndTypeChecks;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import java.util.Map;
import java.util.TreeMap;

/**
 * Tests that {@link JsStackEmulator} generates the expected JavaScript code.
 */
public class JsStackEmulatorTest extends FullCompileTestBase {

  private final ConfigurationProperty recordFileNamesProp =
      new ConfigurationProperty("compiler.emulatedStack.recordFileNames", false);

  private final ConfigurationProperty recordLineNumbersProp =
      new ConfigurationProperty("compiler.emulatedStack.recordLineNumbers", false);

  public void testGeneratedJavaScriptForSimpleThrow() throws Exception {
    recordFileNamesProp.setValue("true");
    recordLineNumbersProp.setValue("true");

    JsProgram program = compileClass(
        "package test;",
        "public class EntryPoint {",
        "  public static void onModuleLoad() {",
        "    throw new RuntimeException();",
        "  }",
        "}");

    // Note: it's up to the catch block to fix $stackDepth.
    checkOnModuleLoad(program, "function onModuleLoad(){" +
        "var stackIndex;$stack[stackIndex=++$stackDepth]=onModuleLoad;" +
        "$location[stackIndex]='EntryPoint.java:'+'3',$clinit_EntryPoint();" +
        "throw new ($location[stackIndex]='EntryPoint.java:'+'4',RuntimeException)" +
        "}");
  }

  /**
   * Given the source code to a Java class named <code>test.EntryPoint</code>,
   * compiles it with emulated stack traces turned on and returns the JavaScript.
   */
  private JsProgram compileClass(String... lines) throws UnableToCompleteException {

    // Gather the Java source code to compile.

    final String code = Joiner.on("\n").join(lines);

    MockResourceOracle sourceOracle = new MockResourceOracle();
    sourceOracle.addOrReplace(new MockJavaResource("test.EntryPoint") {
      @Override
      public CharSequence getContent() {
        return code;
      }
    });
    sourceOracle.add(JavaAstConstructor.getCompilerTypes());

    PrecompileTaskOptions options = new PrecompileTaskOptionsImpl();
    options.setOutput(JsOutputOption.PRETTY);
    options.setRunAsyncEnabled(false);
    CompilerContext context = new CompilerContext.Builder().options(options).build();

    CompilationState state =
        CompilationStateBuilder.buildFrom(logger, context,
            sourceOracle.getResources(), null);
    JProgram jProgram = AstConstructor.construct(logger, state, options, new Properties());
    jProgram.addEntryMethod(findMethod(jProgram, "onModuleLoad"));

    // Construct the JavaScript AST.

    // These passes are needed by GenerateJavaScriptAST.
    ComputeCastabilityInformation.exec(jProgram, false);
    ImplementCastsAndTypeChecks.exec(jProgram, false);
    ArrayNormalizer.exec(jProgram, false);

    Map<JType, JLiteral> typeIdsByType =
        ResolveRuntimeTypeReferences.IntoIntLiterals.exec(jProgram);
    Map<StandardSymbolData, JsName> symbolTable =
        new TreeMap<StandardSymbolData, JsName>(new SymbolData.ClassIdentComparator());

    BindingProperty stackMode = new BindingProperty("compiler.stackMode");
    stackMode.addDefinedValue(new ConditionNone(), "EMULATED");

    PropertyOracle[] properties = {new StaticPropertyOracle(
        new BindingProperty[]{stackMode}, new String[]{"EMULATED"},
        new ConfigurationProperty[]{recordFileNamesProp, recordLineNumbersProp}
    )};

    JsProgram jsProgram = new JsProgram();
    JavaToJavaScriptMap jjsmap = GenerateJavaScriptAST.exec(
        jProgram, jsProgram, context, typeIdsByType, symbolTable,
        properties).getLeft();

    // Finally, run the pass we care about.
    JsStackEmulator.exec(jProgram, jsProgram, properties, jjsmap);

    return jsProgram;
  }

  /**
   * Verifies the JavaScript function corresponding to <code>test.EntryPoint.onModuleLoad</code>.
   */
  private static void checkOnModuleLoad(JsProgram program, String expectedJavascript) {
    JsName onModuleLoad = program.getScope().findExistingName("test_EntryPoint_onModuleLoad__V");
    assertNotNull(onModuleLoad);
    assert onModuleLoad.getStaticRef() instanceof JsFunction;
    assertEquals(expectedJavascript, serializeJs(onModuleLoad.getStaticRef()));
  }

  private static String serializeJs(JsVisitable node) {
    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);
    generator.accept(node);
    return text.toString();
  }

  @Override
  protected void optimizeJava() {
  }
}
