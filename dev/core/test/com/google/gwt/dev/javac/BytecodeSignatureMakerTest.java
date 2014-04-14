/*
 * Copyright 2011 Google Inc.
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

import org.eclipse.jdt.core.compiler.CategorizedProblem;

/**
 * Tests for {@link BytecodeSignatureMaker}
 */
public class BytecodeSignatureMakerTest extends CompilationStateTestBase {
  static final String CLASS_DEP_TYPE_NAME = "test.ClassDependency";

  public void testClassDependencySignature() {
    final MockJavaResource CLASS_DEP_ORIG =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    // A verbatim copy of CLASS_DEP_ORIG
    final MockJavaResource CLASS_DEP_NO_CHANGE =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_NO_PRIVATE =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            // Missing fieldPrivate
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            // Missing methodPrivate
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_NO_PROTECTED_FIELD =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  public int fieldPublic;\n");
            // missing fieldProtected
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_NO_DEFAULT_FIELD =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            // missing fieldDefault
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_NO_PUBLIC_FIELD =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            // missing public field
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_FIELD_VALUE_CHANGE =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            // Value was 100
            code.append("  static public final int fieldPublicStatic = 99;\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_ORDER =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            // re-ordered this field
            code.append("  public int fieldPublic;\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            // re-ordered this method
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_INNER =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            // Added an inner class definition
            code.append("  public static class IgnoreMe {\n");
            code.append("    private int ignoreThisMember;\n");
            code.append("  }\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_DEPRECATED_FIELD =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  @Deprecated\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_DEPRECATED_METHOD =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  @Deprecated\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };

    final MockJavaResource CLASS_DEP_ANNOTATED_FIELD =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  @TestAnnotation(\"Foo\")\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_ANNOTATED_METHOD =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  @TestAnnotation(\"Foo\")\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };
    final MockJavaResource CLASS_DEP_JAVADOC =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  /** a static field */\n");
            code.append("  static public final int fieldPublicStatic = 100;\n");
            code.append("  /** a public field */\n");
            code.append("  public int fieldPublic;\n");
            code.append("  protected int fieldProtected;\n");
            code.append("  int fieldDefault;\n");
            code.append("  private int fieldPrivate;\n");
            code.append("  /** a public method */\n");
            code.append("  public int methodPublic() {return 1;};\n");
            code.append("  protected int methodProtected(String arg) {return 1;};\n");
            code.append("  int methodDefault() {return 1;};\n");
            code.append("  private int methodPrivate(){return 1;};\n");
            code.append("}");
            return code;
          }
        };

    final MockJavaResource TEST_ANNOTATION =
        new MockJavaResource("test.TestAnnotation") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public @interface TestAnnotation {\n");
            code.append("  String value();");
            code.append("}\n");
            return code;
          }
        };
    CompiledClass originalClass = buildClass(CLASS_DEP_ORIG);
    assertNotNull(originalClass);

    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_NO_CHANGE));
    assertSignaturesNotEqual(originalClass, buildClass(CLASS_DEP_NO_PRIVATE));
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_NO_PUBLIC_FIELD));
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_NO_PROTECTED_FIELD));
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_NO_DEFAULT_FIELD));
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_FIELD_VALUE_CHANGE));
    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_ORDER));
    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_INNER));
    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_DEPRECATED_FIELD));
    assertSignaturesEqual(originalClass,
        buildClass(CLASS_DEP_DEPRECATED_METHOD));

    oracle.add(TEST_ANNOTATION);
    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_ANNOTATED_FIELD));
    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_ANNOTATED_METHOD));
    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_JAVADOC));
  }

  public void testClassDependencySignatureWithExceptions() {
    MockJavaResource ILLEGAL_STATE_EXCEPTION =
        new MockJavaResource("java.lang.IllegalStateException") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package java.lang;\n");
            code.append("public class IllegalStateException extends Throwable {}\n");
            return code;
          }
        };
    MockJavaResource NUMBER_FORMAT_EXCEPTION =
        new MockJavaResource("java.lang.NumberFormatException") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package java.lang;\n");
            code.append("public class NumberFormatException extends Throwable {}\n");
            return code;
          }
        };
    MockJavaResource CLASS_DEP_EXCEPTION_ORIG =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  public int methodPublic(String arg) ");
            code.append("      throws IllegalStateException, NumberFormatException {");
            code.append("    return 1;\n");
            code.append("  }\n");
            code.append("}\n");
            return code;
          }
        };
    MockJavaResource CLASS_DEP_EXCEPTION_MOD1 =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            // no exceptions declared
            code.append("  public int methodPublic(String arg) {return 1;};\n");
            code.append("}");
            return code;
          }
        };
    MockJavaResource CLASS_DEP_EXCEPTION_MOD2 =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            // one exception declared
            code.append("  public int methodPublic(String arg)");
            code.append("     throws IllegalStateException {");
            code.append("    return 1;\n");
            code.append("  }\n");
            code.append("}");
            return code;
          }
        };
    MockJavaResource CLASS_DEP_EXCEPTION_MOD3 =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency {\n");
            code.append("  public int methodPublic(String arg)");
            // order of declared exceptions is flipped
            code.append("     throws NumberFormatException, IllegalStateException {");
            code.append("    return 1;\n");
            code.append("  }\n");
            code.append("}");
            return code;
          }
        };

    oracle.add(ILLEGAL_STATE_EXCEPTION);
    oracle.add(NUMBER_FORMAT_EXCEPTION);
    CompiledClass originalClass = buildClass(CLASS_DEP_EXCEPTION_ORIG);
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_EXCEPTION_MOD1));
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_EXCEPTION_MOD2));
    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_EXCEPTION_MOD3));
  }

  public void testClassDependencySignatureWithGenerics() {
    MockJavaResource CLASS_DEP_GENERIC_ORIG =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("public class ClassDependency<T> {\n");
            code.append("  public int methodPublic(T arg) {return 1;};\n");
            code.append("}");
            return code;
          }
        };
    MockJavaResource CLASS_DEP_GENERIC_PARAMETERIZED =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("import java.util.Map;");
            code.append("public class ClassDependency<T extends Map> {\n");
            code.append("  public int methodPublic(T arg) {return 1;};\n");
            code.append("}");
            return code;
          }
        };
    CompiledClass originalClass = buildClass(CLASS_DEP_GENERIC_ORIG);
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_GENERIC_PARAMETERIZED));
  }

  public void testClassDependencySignatureWithInterfaces() {
    MockJavaResource CLASS_DEP_INTERFACE_ORIG =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("import java.util.Map;");
            code.append("import java.util.Collection;");
            code.append("public class ClassDependency implements Map, Collection {\n");
            code.append("  public int methodPublic(String arg) { return 1;}\n");
            code.append("}\n");
            return code;
          }
        };
    MockJavaResource CLASS_DEP_INTERFACE_MOD1 =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("import java.util.Map;");
            code.append("import java.util.Collection;");
            // no interfaces
            code.append("public class ClassDependency {\n");
            code.append("  public int methodPublic(String arg) { return 1;}\n");
            code.append("}\n");
            return code;
          }
        };
    MockJavaResource CLASS_DEP_INTERFACE_MOD2 =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("import java.util.Map;");
            code.append("import java.util.Collection;");
            // only one interface
            code.append("public class ClassDependency implements Map {\n");
            code.append("  public int methodPublic(String arg) { return 1;}\n");
            code.append("}\n");
            return code;
          }
        };
    MockJavaResource CLASS_DEP_INTERFACE_MOD3 =
        new MockJavaResource(CLASS_DEP_TYPE_NAME) {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package test;\n");
            code.append("import java.util.Map;");
            code.append("import java.util.Collection;");
            // flipped order of interface decls
            code.append("public class ClassDependency implements Collection, Map {\n");
            code.append("  public int methodPublic(String arg) { return 1;}\n");
            code.append("}\n");
            return code;
          }
        };
    CompiledClass originalClass = buildClass(CLASS_DEP_INTERFACE_ORIG);
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_INTERFACE_MOD1));
    assertSignaturesNotEqual(originalClass,
        buildClass(CLASS_DEP_INTERFACE_MOD2));
    assertSignaturesEqual(originalClass, buildClass(CLASS_DEP_INTERFACE_MOD3));
  }

  private void assertSignaturesEqual(CompiledClass original,
      CompiledClass updated) {
    String originalSignature =
        BytecodeSignatureMaker.getCompileDependencySignature(original.getBytes());
    String updatedSignature =
        BytecodeSignatureMaker.getCompileDependencySignature(updated.getBytes());
    if (!originalSignature.equals(updatedSignature)) {
      String originalRaw =
          BytecodeSignatureMaker.getCompileDependencyRawSignature(original.getBytes());
      String updatedRaw =
          BytecodeSignatureMaker.getCompileDependencyRawSignature(updated.getBytes());
      fail("Signatures don't match.  raw data expected=<" + originalRaw
          + "> actual=<" + updatedRaw + ">");
    }
  }

  private void assertSignaturesNotEqual(CompiledClass original,
      CompiledClass updated) {
    String originalSignature =
        BytecodeSignatureMaker.getCompileDependencySignature(original.getBytes());
    String updatedSignature =
        BytecodeSignatureMaker.getCompileDependencySignature(updated.getBytes());
    if (originalSignature.equals(updatedSignature)) {
      String originalRaw =
          BytecodeSignatureMaker.getCompileDependencyRawSignature(original.getBytes());
      String updatedRaw =
          BytecodeSignatureMaker.getCompileDependencyRawSignature(updated.getBytes());
      fail("Signatures should not match.  raw data expected=<" + originalRaw
          + "> actual=<" + updatedRaw + ">");
    }
  }

  private CompiledClass buildClass(MockJavaResource resource) {
    oracle.addOrReplace(resource);
    this.rebuildCompilationState();
    CompilationUnit unit =
        state.getCompilationUnitMap().get(resource.getTypeName());
    assertNotNull(unit);
    String internalName = resource.getTypeName().replace(".", "/");
    CategorizedProblem[] problems = unit.getProblems();
    if (problems != null && problems.length != 0) {
      fail(problems[0].toString());
    }
    for (CompiledClass cc : unit.getCompiledClasses()) {
      if (cc.getInternalName().equals(internalName)) {
        return cc;
      }
    }
    fail("Couldn't find class " + internalName + " after compiling.");
    return null;
  }
}
