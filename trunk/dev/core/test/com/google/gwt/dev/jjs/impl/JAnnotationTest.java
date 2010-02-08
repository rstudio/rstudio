/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.dev.jjs.ast.JAnnotation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExternalType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JAnnotation.Property;
import com.google.gwt.dev.jjs.ast.JAnnotation.SourceOnlyClassException;

/**
 * Tests AST setup of JAnnotation nodes as well as the reflective proxy for
 * accessing their data.
 */
public class JAnnotationTest extends OptimizerTestBase {

  /**
   * A test class for binary-only annotations.
   */
  public @interface BinaryAnnotation {
    Class<?> c() default Object.class;

    int i() default 2;

    int[] iOne() default 1;

    int[] iTwo() default {1, 2};

    int[] iZero() default {};

    OtherAnnotation[] o() default {
        @OtherAnnotation("Hello 1"), @OtherAnnotation("Hello 2")};

    String s() default "Hello String";
  }

  /**
   * Used to test binary-only class literal behavior.
   */
  public @interface ClassAnnotation {
    Class<?> value();
  }

  /**
   * A test case for meta-annotations.
   */
  public @interface OtherAnnotation {
    String value();
  }

  private static void assertEquals(int[] a, int[] b) {
    assertEquals(a.length, b.length);
    for (int i = 0, j = a.length; i < j; i++) {
      assertEquals(a[i], b[i]);
    }
  }

  public void setUp() {
    // These packages have annotations required by this test
    JProgram.RECORDED_ANNOTATION_PACKAGES.add("com.google.gwt.dev.jjs");
    JProgram.RECORDED_ANNOTATION_PACKAGES.add("test");

    sourceOracle.addOrReplace(new MockJavaResource("test.SourceClassAnnotation") {
      @Override
      protected CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public @interface SourceClassAnnotation {\n");
        code.append("  Class<?> value();\n");
        code.append("}\n");
        return code;
      }
    });
    sourceOracle.addOrReplace(new MockJavaResource("test.Tag") {
      @Override
      protected CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("@Tag\n");
        code.append("public @interface Tag {\n");
        code.append("}\n");
        return code;
      }
    });
    sourceOracle.addOrReplace(new MockJavaResource("test.WithTag") {
      @Override
      protected CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("@Tag\n");
        code.append("public class WithTag {\n");
        code.append("  @Tag String field;\n");
        code.append("  @Tag WithTag(){}\n");
        code.append("  @Tag void method(@Tag String p) {@Tag String local;}\n");
        code.append("}\n");
        return code;
      }
    });
    sourceOracle.addOrReplace(new MockJavaResource("test.WithBinary") {
      @Override
      protected CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("import " + BinaryAnnotation.class.getCanonicalName()
            + ";\n");
        code.append("import " + ClassAnnotation.class.getCanonicalName()
            + ";\n");
        code.append("import " + OtherAnnotation.class.getCanonicalName()
            + ";\n");
        code.append("public class WithBinary {\n");
        code.append("  public static final String EXPR = \"Expression\";\n");
        code.append("  @ClassAnnotation("
            + JAnnotationTest.class.getCanonicalName() + ".class)\n");
        code.append("  void useBinaryClassReference() {}\n");
        code.append("  @BinaryAnnotation\n");
        code.append("  void useDefaults() {}\n");
        code.append("  @SourceClassAnnotation("
            + JAnnotationTest.class.getCanonicalName() + ".class)\n");
        code.append("  void useSourceClassAnnotation() {}\n");
        code.append("  @BinaryAnnotation(c=Tag.class, i=42, s=\"foo\", o= @OtherAnnotation(\"Hello \" + EXPR))\n");
        code.append("  void useValues() {}\n");
        code.append("}\n");
        return code;
      }
    });
  }

  public void testAllElementTypes() throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "new test.WithTag();");

    // ANNOTATION_TYPE
    JDeclaredType tag = findType(program, "test.Tag");
    assertEquals(1, tag.getAnnotations().size());

    // TYPE
    JDeclaredType withTag = findType(program, "test.WithTag");
    assertEquals(1, withTag.getAnnotations().size());

    // CONSTRUCTOR
    JMethod constructor = findMethod(withTag, "WithTag");
    assertEquals(1, constructor.getAnnotations().size());

    // METHOD
    JMethod method = findMethod(withTag, "method");
    assertEquals(1, method.getAnnotations().size());

    // FIELD
    JField field = findField(withTag, "field");
    assertEquals(1, field.getAnnotations().size());

    // LOCAL_VARIABLE
    JLocal local = findLocal(method, "local");
    assertEquals(1, local.getAnnotations().size());

    // PARAMETER
    JParameter param = method.getParams().get(0);
    assertEquals(1, param.getAnnotations().size());

    // There are no representations for PACKAGE in our AST
  }

  public void testAnnotationBinaryOnlyClassLiterals()
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "new test.WithBinary();");

    JDeclaredType t = findType(program, "test.WithBinary");
    JMethod m = findMethod(t, "useBinaryClassReference");
    JAnnotation a = JAnnotation.findAnnotation(m,
        ClassAnnotation.class.getName());
    assertNotNull(a);

    ClassAnnotation instance = JAnnotation.createAnnotation(
        ClassAnnotation.class, a);

    assertSame(JAnnotationTest.class, instance.value());
  }

  public void testAnnotationProxyCustomValues()
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "new test.WithBinary();");

    JDeclaredType t = findType(program, "test.WithBinary");
    JMethod useValues = findMethod(t, "useValues");
    JAnnotation a = JAnnotation.findAnnotation(useValues,
        BinaryAnnotation.class.getName());
    assertNotNull(a);
    BinaryAnnotation instance = JAnnotation.createAnnotation(
        BinaryAnnotation.class, a);
    assertNotNull(instance);

    // A source-only annotation, unavailable to the JVM
    try {
      instance.c();
    } catch (SourceOnlyClassException e) {
      // Expected
      assertEquals(findType(program, "test.Tag"), e.getLiteral().getRefType());
    }
    assertEquals(42, instance.i());
    assertEquals("foo", instance.s());
    assertEquals(1, instance.o().length);
    assertEquals("Hello Expression", instance.o()[0].value());
  }

  public void testAnnotationProxyDefaultValues()
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "new test.WithBinary();");

    JDeclaredType t = findType(program, "test.WithBinary");
    JMethod useDefaults = findMethod(t, "useDefaults");
    JAnnotation a = JAnnotation.findAnnotation(useDefaults,
        BinaryAnnotation.class.getName());
    assertNotNull(a);
    assertTrue(a.getType() instanceof JExternalType);

    BinaryAnnotation instance = JAnnotation.createAnnotation(
        BinaryAnnotation.class, a);
    assertNotNull(instance);

    // Test Object methods
    assertEquals(instance.hashCode(), instance.hashCode());
    assertNotNull(instance.toString());

    // Test default-valued
    assertSame(Object.class, instance.c());
    assertEquals(2, instance.i());
    assertEquals(new int[] {1}, instance.iOne());
    assertEquals(new int[] {1, 2}, instance.iTwo());
    assertEquals(new int[] {}, instance.iZero());
    assertNotNull(instance.o());
    assertEquals(2, instance.o().length);
    assertEquals("Hello 1", instance.o()[0].value());
    assertEquals("Hello 2", instance.o()[1].value());
    assertEquals("Hello String", instance.s());
  }

  public void testSourceAnnotationWithBinaryClass()
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "new test.WithBinary();");

    JDeclaredType t = findType(program, "test.WithBinary");
    JMethod m = findMethod(t, "useSourceClassAnnotation");
    JAnnotation a = JAnnotation.findAnnotation(m, "test.SourceClassAnnotation");
    assertNotNull(a);

    Property p = a.getProperty("value");
    JClassLiteral literal = (JClassLiteral) p.getSingleValue();
    JExternalType externalType = (JExternalType) literal.getRefType();
    assertEquals(JAnnotationTest.class.getName(), externalType.getName());
  }
}
