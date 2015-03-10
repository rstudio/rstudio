/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Test for {@link UnifyAst}.
 */
public class UnifyAstTest extends OptimizerTestBase {

  public void testPackageInfo_defaultPackagePresent() throws Exception {
    final MockJavaResource packageInfo =
        JavaResourceBase.createMockJavaResource("package-info");

    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public class A {",
            "}");

    addAll(packageInfo, A);
    Result result = optimize("void", "");

    assertNotNull(result.findClass("package-info"));
  }

  public void testPackageInfo_defaultPackageAbsent() throws Exception {
    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("A",
            "public class A {",
            "}");

    addAll(A);
    Result result = optimize("void", "");

    assertNull(result.findClass("package-info"));
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    return false;
  }
}
