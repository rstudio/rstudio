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
import com.google.gwt.thirdparty.guava.common.base.Joiner;

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
      return Joiner.on("\n").join("package com.google.gwt;",
      "public @interface GwtIncompatible {",
      "  String[] value();",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_METHOD_MISSING_ANNOTATION =
      new MockJavaResource("com.google.gwt.GwtIncompatibleTest") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class GwtIncompatibleTest {",
      "  int test() { return gwtIncompatibleMethod(); }  ",
      "  @GwtIncompatible(\" not compatible \") ",
      "  int gwtIncompatibleMethod() { return -1; }  ",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_METHOD = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleMethodTest") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class GwtIncompatibleMethodTest {",
      "  @GwtIncompatible(\" not compatible \") ",
      "  int test() { return methodDoesNotExist(); }  ",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_FIELD = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleFieldTest") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class GwtIncompatibleFieldTest {",
      "  @GwtIncompatible(\" not compatible \") ",
      "  int test = methodDoesNotExist();   ",
      "  int test() { return 31; }  ",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_INNERCLASS = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleInnerTest") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class GwtIncompatibleInnerTest {",
      "  @GwtIncompatible(\" not compatible \") ",
      "  public class Inner {",
      "    int test() { return methodDoesNotExist(); }  ",
      "  }",
      "  int test() { return 31; }  ",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_TOPCLASS = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleClass") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "@GwtIncompatible(\" not compatible \") ",
      "public class GwtIncompatibleClass {",
      "}");
    }
  };

  public static final MockJavaResource EXTENDS_GWTINCOMPATIBLE = new MockJavaResource(
      "com.google.gwt.ExtendsGwtIncompatibleClass") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class ExtendsGwtIncompatibleClass",
      "   extends GwtIncompatibleClass {",
      "}");
    }
  };

  public static final MockJavaResource INSTANTIATES_GWTINCOMPATIBLE = new MockJavaResource(
      "com.google.gwt.InstantiatesGwtIncompatibleClass") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class InstantiatesGwtIncompatibleClass {",
      "    Object test() { return new GwtIncompatibleClass(); }  ",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_ANONYMOUS_INNERCLASS = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleAInnerTest") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class GwtIncompatibleAInnerTest {",
      "  Object createAnonymous() {",
      "    return new Object() {",
      "      @GwtIncompatible(\" not compatible \") ",
      "      int test() { return methodDoesNotExist(); }  ",
      "    };",
      "  }",
      "  int test() { return 31; }  ",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_WITH_INNERCLASS = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleWithInnerClass") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "@GwtIncompatible(\" not compatible \") ",
      "public class GwtIncompatibleWithInnerClass {",
      "  public class Child {",
      "  }",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_WITH_INNERCLASS_TEST = new MockJavaResource(
      "com.google.gwt.GwtIncompatibleWithInnerClassTest") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class GwtIncompatibleWithInnerClassTest {",
      "  void test() {",
      "    (new GwtIncompatibleWithInnerClass()).new Child();",
      "  }",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_WITH_STATIC_INNERCLASS =
      new MockJavaResource("com.google.gwt.GwtIncompatibleWithStaticInnerClass") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "@GwtIncompatible(\" not compatible \") ",
      "public class GwtIncompatibleWithStaticInnerClass {",
      "  public static class Child {",
      "  }",
      "}");
    }
  };

  public static final MockJavaResource GWTINCOMPATIBLE_WITH_STATIC_INNERCLASS_TEST =
      new MockJavaResource("com.google.gwt.GwtIncompatibleWithStaticInnerClassTest") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join("package com.google.gwt;",
      "public class GwtIncompatibleWithStaticInnerClassTest {",
      "  void test() {",
      "    new GwtIncompatibleWithStaticInnerClass.Child();",
      "  }",
      "}");
    }
  };
}

