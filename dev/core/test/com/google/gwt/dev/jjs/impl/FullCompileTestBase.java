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
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.cfg.BindingProperties;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConditionNone;
import com.google.gwt.dev.cfg.ConfigurationProperties;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.PermutationProperties;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.JavaAstConstructor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.IntTypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeOrder;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Base class for all tests that require end to end compilation.
 */
public abstract class FullCompileTestBase extends JJSTestBase {

  // Compilation Configuration Properties.
  private BindingProperty[] orderedProps = null;
  private String[] orderedPropValues = null;
  private ConfigurationProperty[] configurationProperties = {};

  protected JProgram jProgram = null;
  protected JsProgram jsProgram = null;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // Stack mode is a required property.
    BindingProperty stackMode = new BindingProperty("compiler.stackMode");
    stackMode.addDefinedValue(new ConditionNone(), "STRIP");
    setProperties(new BindingProperty[]{stackMode}, new String[]{"STRIP"},
        new ConfigurationProperty[]{});

    jsProgram = new JsProgram();
  }

  /**
   * Compiles a Java class <code>test.EntryPoint</code> and use the code splitter on it.
   */
  protected Pair<JavaToJavaScriptMap, Set<JsNode>> compileSnippetToJS(final String code)
      throws UnableToCompleteException {
    sourceOracle.addOrReplace(new MockJavaResource("test.EntryPoint") {
      @Override
      public CharSequence getContent() {
        return code;
      }
    });

    CompilerContext compilerContext = provideCompilerContext();
    CompilationState state =
        CompilationStateBuilder.buildFrom(logger, compilerContext,
            sourceOracle.getResources(), getAdditionalTypeProviderDelegate());
    ConfigurationProperties config = new ConfigurationProperties(Lists.newArrayList(
        configurationProperties));

    jProgram =
        JavaAstConstructor.construct(logger, state, compilerContext, config,
            "test.EntryPoint", "com.google.gwt.lang.Exceptions");
    jProgram.addEntryMethod(findMethod(jProgram, "onModuleLoad"));

    optimizeJava();
    ComputeCastabilityInformation.exec(jProgram, false);
    ComputeInstantiatedJsoInterfaces.exec(jProgram);
    ImplementCastsAndTypeChecks.exec(jProgram, false);
    ArrayNormalizer.exec(jProgram, false);
    TypeTightener.exec(jProgram);
    MethodCallTightener.exec(jProgram);

    TypeMapper<Integer> typeMapper = new IntTypeMapper();
    ResolveRuntimeTypeReferences.exec(jProgram, typeMapper, TypeOrder.FREQUENCY);

    Map<StandardSymbolData, JsName> symbolTable =
        new TreeMap<StandardSymbolData, JsName>(new SymbolData.ClassIdentComparator());

    PermutationProperties props = new PermutationProperties(Arrays.asList(
        new BindingProperties(orderedProps, orderedPropValues, config)
    ));
    return GenerateJavaScriptAST.exec(logger, jProgram, jsProgram, compilerContext,
        typeMapper, symbolTable, props);
  }

  abstract protected void optimizeJava();

  protected CompilerContext provideCompilerContext() {
    CompilerContext compilerContext = new CompilerContext.Builder().build();
    compilerContext.getOptions().setSourceLevel(sourceLevel);
    return compilerContext;
  }

  public void setProperties(BindingProperty[] orderedProps, String[] orderedValues,
      ConfigurationProperty[] configurationProperties) {
    this.orderedProps = orderedProps;
    this.orderedPropValues = orderedValues;
    this.configurationProperties = configurationProperties;
  }
}
