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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.PrecompileTaskOptionsImpl;
import com.google.gwt.dev.cfg.BindingProperties;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConditionNone;
import com.google.gwt.dev.cfg.ConfigurationProperties;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.PermutationProperties;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.jjs.AstConstructor;
import com.google.gwt.dev.jjs.JavaAstConstructor;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.ComputeCastabilityInformation;
import com.google.gwt.dev.jjs.impl.FullCompileTestBase;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.ImplementCastsAndTypeChecks;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.MethodInliner;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.StringTypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeOrder;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import java.util.Arrays;
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

  private boolean inline = false;

  public void testEmptyMethod() throws Exception {
    recordFileNamesProp.setValue("true");
    recordLineNumbersProp.setValue("true");

    JsProgram program = compileClass(
        "package test;",
        "public class EntryPoint {",
        "  public static void onModuleLoad() {",
        "  }",
        "}");

    checkOnModuleLoad(program, "function onModuleLoad(){" +
        "var stackIndex;$stack[stackIndex=++$stackDepth]=onModuleLoad;" +
        "$location[stackIndex]='EntryPoint.java:'+'3',$clinit_EntryPoint();" +
        "$stackDepth=stackIndex-1}");
  }

  public void testCallWithNoArguments() throws Exception {
    recordFileNamesProp.setValue("true");
    recordLineNumbersProp.setValue("true");

    JsProgram program = compileClass(
        "package test;",
        "public class EntryPoint {",
        "  static void foo() {}",
        "  public static void onModuleLoad() {",
        "    foo();",
        "  }",
        "}");

    checkOnModuleLoad(program, "function onModuleLoad(){" +
        "var stackIndex;$stack[stackIndex=++$stackDepth]=onModuleLoad;" +
        "$location[stackIndex]='EntryPoint.java:'+'4',$clinit_EntryPoint();" +
        "$location[stackIndex]='EntryPoint.java:'+'5',foo();" +
        "$stackDepth=stackIndex-1}");
  }

  public void testCallWithArguments() throws Exception {
    recordFileNamesProp.setValue("true");
    recordLineNumbersProp.setValue("true");

    JsProgram program = compileClass(
        "package test;",
        "public class EntryPoint {",
        "  static void foo(int x) {}",
        "  public static void onModuleLoad() {",
        "    foo(123);",
        "  }",
        "}");

    checkOnModuleLoad(program, "function onModuleLoad(){" +
        "var stackIndex;$stack[stackIndex=++$stackDepth]=onModuleLoad;" +
        "$location[stackIndex]='EntryPoint.java:'+'4',$clinit_EntryPoint();" +
        "foo(($tmp=123,$location[stackIndex]='EntryPoint.java:'+'5',$tmp));" +
        "$stackDepth=stackIndex-1}");
  }

  public void testSimpleThrow() throws Exception {
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
        "throw unwrap(($location[stackIndex]='EntryPoint.java:'+'4',new RuntimeException))" +
        "}");
  }

  public void testThrowWithInlineMethodCall() throws Exception {
    recordFileNamesProp.setValue("true");
    recordLineNumbersProp.setValue("true");
    inline = true;

    JsProgram program = compileClass(
        "package test;",
        "public class EntryPoint {",
        "  static Object thing = \"hello\";",
        "  private static String message() { return thing", // line 4
        "    .toString(); }",
        "  public static void onModuleLoad() {", // line 6
        "    throw new RuntimeException(message());", // line 7
        "  }",
        "}");

    // Line 7 should be current when the RuntimeException constructor is called.
    checkOnModuleLoad(program, "function onModuleLoad(){" +
        "var stackIndex;$stack[stackIndex=++$stackDepth]=onModuleLoad;" +
        "$location[stackIndex]='EntryPoint.java:'+'6',$clinit_EntryPoint();" +
        "throw unwrap(new RuntimeException(" +
        "($tmp=($location[stackIndex]='EntryPoint.java:'+'4',thing).toString()," +
        "$location[stackIndex]='EntryPoint.java:'+'7',$tmp)))" +
        "}");
  }

  public void testThrowWithChainedMethodCall() throws Exception {
    recordFileNamesProp.setValue("true");
    recordLineNumbersProp.setValue("true");
    inline = true;

    JsProgram program = compileClass(
        "package test;",
        "public class EntryPoint {",
        "  static Factory factory;",
        "  static Factory getFactory() {",
        "    return factory;", // line 5
        "  }",
        "  public static void onModuleLoad() {", // line 7
        "    throw getFactory().makeException();", // line 8
        "  }",
        "  static class Factory {",
        "    RuntimeException makeException() {",
        "      return new RuntimeException();",
        "    }",
        "  }",
        "}");

    checkOnModuleLoad(program, "function onModuleLoad(){" +
        "var stackIndex;$stack[stackIndex=++$stackDepth]=onModuleLoad;" +
        "$location[stackIndex]='EntryPoint.java:'+'7',$clinit_EntryPoint();" +
        "throw unwrap(($tmp=($location[stackIndex]='EntryPoint.java:'+'5',factory)," +
        "$location[stackIndex]='EntryPoint.java:'+'8',$tmp).makeException())" +
        "}");
  }

  public void testTryCatch() throws Exception {
    recordFileNamesProp.setValue("true");
    recordLineNumbersProp.setValue("true");

    JsProgram program = compileClass(
        "package test;",
        "public class EntryPoint {",
        "  public static void onModuleLoad() {",
        "    try {",
        "      throw new RuntimeException();",
        "    } catch (RuntimeException e) {" ,
        "      String s = e.getMessage();",
        "    }",
        "  }",
        "}");

    // Note: it's up to the catch block to fix $stackDepth.
    checkOnModuleLoad(program, "function onModuleLoad(){" +
        "var stackIndex;$stack[stackIndex=++$stackDepth]=onModuleLoad;" +
        "$location[stackIndex]='EntryPoint.java:'+'3',$clinit_EntryPoint();var e,s;" +
        "try{throw unwrap(($location[stackIndex]='EntryPoint.java:'+'5',new RuntimeException))" +
        "}catch($e0){$e0=wrap($e0);" +
        "$stackDepth=($location[stackIndex]='EntryPoint.java:'+'6',stackIndex);" +
        "if(instanceOf($e0,'java.lang.RuntimeException')){" +
        "e=$e0;s=($location[stackIndex]='EntryPoint.java:'+'7',e).getMessage()}" +
        "else throw unwrap(($location[stackIndex]='EntryPoint.java:'+'6',$e0))}" +
        "$stackDepth=stackIndex-1" +
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
    CompilerContext context = new CompilerContext.Builder().options(options)
        .minimalRebuildCache(new MinimalRebuildCache()).build();

    ConfigurationProperties config = new ConfigurationProperties(Arrays.asList(recordFileNamesProp,
        recordLineNumbersProp));

    CompilationState state =
        CompilationStateBuilder.buildFrom(logger, context,
            sourceOracle.getResources(), null);
    JProgram jProgram = AstConstructor.construct(logger, state, options, config);
    jProgram.addEntryMethod(findMethod(jProgram, "onModuleLoad"));

    if (inline) {
      MethodInliner.exec(jProgram);
    }

    CatchBlockNormalizer.exec(jProgram);
    // Construct the JavaScript AST.

    // These passes are needed by GenerateJavaScriptAST.
    ComputeCastabilityInformation.exec(jProgram, false);
    ImplementCastsAndTypeChecks.exec(jProgram, false);
    ArrayNormalizer.exec(jProgram);

    StringTypeMapper typeMapper = new StringTypeMapper(jProgram);
    ResolveRuntimeTypeReferences.exec(jProgram, typeMapper, TypeOrder.FREQUENCY);
    Map<StandardSymbolData, JsName> symbolTable =
        new TreeMap<StandardSymbolData, JsName>(new SymbolData.ClassIdentComparator());

    BindingProperty stackMode = new BindingProperty("compiler.stackMode");
    stackMode.addDefinedValue(new ConditionNone(), "EMULATED");

    PermutationProperties properties = new PermutationProperties(Arrays.asList(
        new BindingProperties(new BindingProperty[]{stackMode}, new String[]{"EMULATED"}, config)
    ));

    JsProgram jsProgram = new JsProgram();
    JavaToJavaScriptMap jjsmap = GenerateJavaScriptAST.exec(
        logger, jProgram, jsProgram, context, typeMapper,
        symbolTable, properties).getLeft();

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
