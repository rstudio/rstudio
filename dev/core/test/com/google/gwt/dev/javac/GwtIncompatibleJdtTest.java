/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test class for GwtIncompatible annotations in {@link com.google.gwt.dev.javac.JdtCompiler}.
 */
public class GwtIncompatibleJdtTest extends TestCase {

  static void assertUnitHasErrors(CompilationUnit unit, int numErrors) {
    assertTrue(unit.isError());
    assertEquals(numErrors, unit.getProblems().length);
  }

  static void assertUnitsCompiled(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      assertFalse(unit.isError());
      assertTrue(unit.getCompiledClasses().size() > 0);
    }
  }

  public void testCompileError() throws Exception {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, GWTINCOMPATIBLE_ANNOTATION, GWTINCOMPATIBLE_METHOD_MISSING_ANNOTATION);
    List<CompilationUnit> units = JdtCompiler.compile(TreeLogger.NULL, builders);
    assertUnitsCompiled(units.subList(0, units.size() - 1));
    assertUnitHasErrors(units.get(units.size() - 1), 1);
  }

  public void testCompileGwtIncompatible() throws Exception {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    addAll(builders, JavaResourceBase.getStandardResources());
    addAll(builders, GWTINCOMPATIBLE_ANNOTATION, GWTINCOMPATIBLE_METHOD,
        GWTINCOMPATIBLE_FIELD, GWTINCOMPATIBLE_INNERCLASS, GWTINCOMPATIBLE_ANONYMOUS_INNERCLASS);
    Collection<CompilationUnit> units = JdtCompiler.compile(TreeLogger.NULL, builders);
    assertUnitsCompiled(units);
  }

  public static final MockJavaResource GWTINCOMPATIBLE_ANNOTATION = new MockJavaResource(
      "com.google.gwt.GwtIncompatible") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("public @interface GwtIncompatible {\n");
      code.append("  String[] value();\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_METHOD_MISSING_ANNOTATION =
      new MockJavaResource("com.google.gwt.GwtIncompatibleTest") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("import com.google.gwt.GwtIncompatible;\n");
      code.append("public class GwtIncompatibleTest {\n");
      code.append("  int test() { return methodDoesNotExist(); }  \n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_METHOD = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleMethodTest") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("import com.google.gwt.GwtIncompatible;\n");
      code.append("public class GwtIncompatibleMethodTest {\n");
      code.append("  @GwtIncompatible(\" not compatible \") \n");
      code.append("  int test() { return methodDoesNotExist(); }  \n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_FIELD = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleFieldTest") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("import com.google.gwt.GwtIncompatible;\n");
      code.append("public class GwtIncompatibleFieldTest {\n");
      code.append("  @GwtIncompatible(\" not compatible \") \n");
      code.append("  int test = methodDoesNotExist();   \n");
      code.append("  int test() { return 31; }  \n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_INNERCLASS = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleInnerTest") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("import com.google.gwt.GwtIncompatible;\n");
      code.append("public class GwtIncompatibleInnerTest {\n");
      code.append("  @GwtIncompatible(\" not compatible \") \n");
      code.append("  public class Inner {\n");
      code.append("    int test() { return methodDoesNotExist(); }  \n");
      code.append("  }\n");
      code.append("  int test() { return 31; }  \n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_ANONYMOUS_INNERCLASS = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleAInnerTest") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("import com.google.gwt.GwtIncompatible;\n");
      code.append("public class GwtIncompatibleAInnerTest {\n");
      code.append("  Object createAnonymous() {\n");
      code.append("    return new Object() {\n");
      code.append("      @GwtIncompatible(\" not compatible \") \n");
      code.append("      int test() { return methodDoesNotExist(); }  \n");
      code.append("    };\n");
      code.append("  }\n");
      code.append("  int test() { return 31; }  \n");
      code.append("}\n");
      return code;
    }
  };
  private void addAll(Collection<CompilationUnitBuilder> units,
                      Resource... sourceFiles) {
    for (Resource sourceFile : sourceFiles) {
      units.add(CompilationUnitBuilder.create(sourceFile));
    }
  }

}
