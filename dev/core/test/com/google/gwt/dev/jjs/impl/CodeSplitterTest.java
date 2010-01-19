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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests class {@link CodeSplitter}.
 */
public class CodeSplitterTest extends OptimizerTestBase {
  /**
   * Tests that everything in the magic Array class is considered initially
   * live.
   */
  public void testArrayIsInitial() throws UnableToCompleteException {
    sourceOracle.addOrReplace(new MockJavaResource("com.google.gwt.lang.Array") {
      @Override
      protected CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.lang;\n");
        code.append("public class Array {\n");
        code.append("  private Class type;\n");
        code.append("  public Class getClass() { return type; }\n");
        code.append("}\n");
        return code;
      }
    });

    JProgram program = compileSnippet("void", "");
    ControlFlowAnalyzer cfa = CodeSplitter.computeInitiallyLive(program);

    assertTrue(cfa.getInstantiatedTypes().contains(
        findType(program, "com.google.gwt.lang.Array")));
    assertTrue(cfa.getLiveFieldsAndMethods().contains(
        findMethod(findType(program, "com.google.gwt.lang.Array"), "getClass")));
  }
}
