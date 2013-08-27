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

import com.google.gwt.dev.javac.testing.impl.MockJavaResource;

import java.util.List;

/**
 * Test class for GwtIncompatible annotations in {@link com.google.gwt.dev.javac.JdtCompiler}.
 */
public class GwtIncompatibleJdtCompilerTest extends JdtCompilerTestBase {

  public void testCompileError() throws Exception {
    List<CompilationUnit> units = compile(GWTINCOMPATIBLE_ANNOTATION,
        GWTINCOMPATIBLE_METHOD_MISSING_ANNOTATION);
    assertOnlyLastUnitHasErrors(units,
        "The method gwtIncompatibleMethod() is undefined for the type GwtIncompatibleTest");
  }

  public void testCompileGwtIncompatible() throws Exception {
    assertResourcesCompileSuccessfully(GWTINCOMPATIBLE_ANNOTATION,
        GWTINCOMPATIBLE_METHOD, GWTINCOMPATIBLE_FIELD, GWTINCOMPATIBLE_INNERCLASS,
        GWTINCOMPATIBLE_ANONYMOUS_INNERCLASS);
  }

  public void testCompileGwtIncompatibleClass() throws Exception {
    assertResourcesCompileSuccessfully(GWTINCOMPATIBLE_ANNOTATION, GWTINCOMPATIBLE_TOPCLASS);
  }

  public void testCompileExtendsGwtIncompatibleClass() throws Exception {
    List<CompilationUnit> units = compile(GWTINCOMPATIBLE_ANNOTATION, GWTINCOMPATIBLE_TOPCLASS,
        EXTENDS_GWTINCOMPATIBLE);
    assertOnlyLastUnitHasErrors(units, "The type ExtendsGwtIncompatibleClass cannot "
        + "subclass the final class GwtIncompatibleClass");
  }

  public void testCompileInstantiateGwtIncompatibleClass() throws Exception {
    List<CompilationUnit> units = compile(GWTINCOMPATIBLE_ANNOTATION, GWTINCOMPATIBLE_TOPCLASS,
        INSTANTIATES_GWTINCOMPATIBLE);
    assertOnlyLastUnitHasErrors(units,
        "The constructor GwtIncompatibleClass() is not visible");
  }

  public void testCompileGwtIncompatibleClassWithInnerClass() throws Exception {
    assertResourcesCompileSuccessfully(GWTINCOMPATIBLE_ANNOTATION, GWTINCOMPATIBLE_WITH_INNERCLASS);
  }

  public void testCompileGwtIncompatibleClassWithInnerClassTest() throws Exception {
    List<CompilationUnit> units = compile(GWTINCOMPATIBLE_ANNOTATION,
        GWTINCOMPATIBLE_WITH_INNERCLASS, GWTINCOMPATIBLE_WITH_INNERCLASS_TEST);
    assertOnlyLastUnitHasErrors(units,
        "The constructor GwtIncompatibleWithInnerClass() is not visible",
        "GwtIncompatibleWithInnerClass.Child cannot be resolved to a type");
  }

  public void testCompileGwtIncompatibleClassWithStaticInnerClass() throws Exception {
    assertResourcesCompileSuccessfully(GWTINCOMPATIBLE_ANNOTATION,
        GWTINCOMPATIBLE_WITH_STATIC_INNERCLASS);
  }

  public void testCompileGwtIncompatibleClassWithStaticInnerClassTest() throws Exception {
    List<CompilationUnit> units = compile(GWTINCOMPATIBLE_ANNOTATION,
        GWTINCOMPATIBLE_WITH_STATIC_INNERCLASS, GWTINCOMPATIBLE_WITH_STATIC_INNERCLASS_TEST);
    assertOnlyLastUnitHasErrors(units,
        "GwtIncompatibleWithStaticInnerClass.Child cannot be resolved to a type");
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
      code.append("public class GwtIncompatibleTest {\n");
      code.append("  int test() { return gwtIncompatibleMethod(); }  \n");
      code.append("  @GwtIncompatible(\" not compatible \") \n");
      code.append("  int gwtIncompatibleMethod() { return -1; }  \n");
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

  public static final MockJavaResource GWTINCOMPATIBLE_TOPCLASS = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleClass") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("@GwtIncompatible(\" not compatible \") \n");
      code.append("public class GwtIncompatibleClass {\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource EXTENDS_GWTINCOMPATIBLE = new MockJavaResource(
      "com.google.gwt.ExtendsGwtIncompatibleClass") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("public class ExtendsGwtIncompatibleClass\n");
      code.append("   extends GwtIncompatibleClass {\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource INSTANTIATES_GWTINCOMPATIBLE = new MockJavaResource(
      "com.google.gwt.InstantiatesGwtIncompatibleClass") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("public class InstantiatesGwtIncompatibleClass {\n");
      code.append("    Object test() { return new GwtIncompatibleClass(); }  \n");
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

  public static final MockJavaResource GWTINCOMPATIBLE_WITH_INNERCLASS = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleWithInnerClass") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("@GwtIncompatible(\" not compatible \") \n");
      code.append("public class GwtIncompatibleWithInnerClass {\n");
      code.append("  public class Child {\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_WITH_INNERCLASS_TEST = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleWithInnerClassTest") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("public class GwtIncompatibleWithInnerClassTest {\n");
      code.append("  void test() {\n");
      code.append("    (new GwtIncompatibleWithInnerClass()).new Child();\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_WITH_STATIC_INNERCLASS =
      new MockJavaResource("com.google.gwt.GwtIncompatibleWithStaticInnerClass") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("@GwtIncompatible(\" not compatible \") \n");
      code.append("public class GwtIncompatibleWithStaticInnerClass {\n");
      code.append("  public static class Child {\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_WITH_STATIC_INNERCLASS_TEST =
      new MockJavaResource("com.google.gwt.GwtIncompatibleWithStaticInnerClassTest") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt;\n");
      code.append("public class GwtIncompatibleWithStaticInnerClassTest {\n");
      code.append("  void test() {\n");
      code.append("    (new GwtIncompatibleWithStaticInnerClass()).new Child();\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
}

