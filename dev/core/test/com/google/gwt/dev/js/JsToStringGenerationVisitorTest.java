/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.impl.NamedRange;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConditionNone;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.jjs.impl.FullCompileTestBase;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Tests for JsToStringGenerationVisitor.
 */
public class JsToStringGenerationVisitorTest extends FullCompileTestBase {

  // Compilation Configuration Properties.
  @Override
  public void setUp() throws Exception {
    // Compilation Configuration Properties.
    BindingProperty stackMode = new BindingProperty("compiler.stackMode");
    stackMode.addDefinedValue(new ConditionNone(), "STRIP");
    setProperties(new BindingProperty[] {stackMode}, new String[] {"STRIP"},
        new ConfigurationProperty[] {});
    super.setUp();
  }

  public void testClassRangeMarking() throws UnableToCompleteException {
    // Prepares the EntryPoint class to compile.
    StringBuilder code = new StringBuilder();
    code.append("package test;\n");
    code.append("public class EntryPoint {\n");
    code.append("  public interface SomeInterface {}\n");
    code.append("  public static void onModuleLoad() {}\n");
    code.append("}\n");

    // Compiles EntryPoint to JS.
    compileSnippetToJS(code.toString());
    TextOutput text = new DefaultTextOutput(true);
    JsSourceGenerationVisitor jsSourceGenerationVisitor = new JsSourceGenerationVisitor(text);
    jsSourceGenerationVisitor.accept(jsProgram);

    // Verifies that the EntryPoint class, SomeInterface interface and some other classes were
    // delimited in the output by getClassRanges().
    List<NamedRange> classRanges = jsSourceGenerationVisitor.getClassRanges();
    Map<String, NamedRange> classRangesByName = Maps.newHashMap();
    for (NamedRange classRange : classRanges) {
      classRangesByName.put(classRange.getName(), classRange);
    }
    assertTrue(classRangesByName.containsKey("test.EntryPoint"));
    assertTrue(classRangesByName.containsKey("test.EntryPoint$SomeInterface"));
    assertTrue(classRangesByName.size() > 2);

    NamedRange programClassRange = jsSourceGenerationVisitor.getProgramClassRange();
    // Verifies there is a preamble before the program class range.
    assertTrue(programClassRange.getStartPosition() > 0);
    // Verifies there is an epilogue after the program class range.
    assertTrue(programClassRange.getEndPosition() < text.getPosition());
  }

  @Override
  protected void optimizeJava() {
  }
}
