/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.javac.asm;

import com.google.gwt.dev.javac.typemodel.test.PrimitiveValuesAnnotation;
import com.google.gwt.dev.javac.typemodel.test.TestAnnotation;
import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.EmptyVisitor;
import com.google.gwt.dev.javac.asm.CollectAnnotationData.AnnotationData;
import com.google.gwt.dev.javac.asm.CollectClassData.ClassType;

import java.util.List;

/**
 * Tests for {@link CollectClassData}.
 */
public class CollectClassDataTest extends AsmTestCase {

  public static class LongDoubleArgs {

    @SuppressWarnings("unused")
    public LongDoubleArgs(int x, long y, double z, Object o) {
    }

    @SuppressWarnings("unused")
    public void longDoubleMethod(int xx, long yy, double zz, String s) {
    }
  }


  public static class One extends EmptyVisitor {

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      new EmptyVisitor() {
        @Override
        public void visit(int version, int access, String name,
            String signature, String superName, String[] interfaces) {
        }
      };
      return new CollectAnnotationData(desc, visible);
    }
  }

  @PrimitiveValuesAnnotation(b = 42, i = 42)
  protected static class Two {

    public class TwoInner {
    }

    private final String field;

    @TestAnnotation("field")
    private final String annotatedField;

    public Two(int a) {
      this(a, null);
    }

    @TestAnnotation("foo")
    public String foo(int a) throws IllegalStateException {
      return annotatedField;
    }

    public Two(int a, String b) {
      new TwoInner();
      field = b;
      annotatedField = field;
    }
  }

  /**
   * Test local classes.
   */
  public static class Three {

    public int foo;

    /**
     * Static method that has a local class in it.
     */
    public static void methodWithLocalStatic() {
      class Foo {
      }

      Foo x = new Foo();
    }

    /**
     * Method that has a local class in it.
     */
    public void methodWithLocal() {
      class Foo {
        Foo() {
          foo = 1;
        }
      }

      Foo x = new Foo();
    }
  }

  public void testAnonymous() {
    CollectClassData cd = collect(One.class.getName() + "$1");
    // Don't access on anonymous classes, it varies among compilers
    // assertEquals(0, cd.getAccess() & ~Opcodes.ACC_SUPER);
    assertEquals(ClassType.Anonymous, cd.getClassType());
    assertEquals(0, cd.getFields().size());
    List<CollectMethodData> methods = cd.getMethods();
    assertEquals(2, methods.size());
  }

  public void testOne() {
    CollectClassData cd = collect(One.class);
    // Don't check for super bit, as it will depend on the JDK used to compile.
    assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, cd.getAccess()
        & ~Opcodes.ACC_SUPER);
    assertEquals(ClassType.Nested, cd.getClassType());
    assertEquals(0, cd.getFields().size());
    assertEquals(0, cd.getInterfaces().length);
    assertEquals(0, cd.getAnnotations().size());
    assertEquals("com/google/gwt/dev/asm/commons/EmptyVisitor",
        cd.getSuperName());

    List<CollectMethodData> methods = cd.getMethods();
    assertEquals(2, methods.size());
    // TODO(jat): Is it safe to assume the implicit constructor is always first?
    CollectMethodData method = methods.get(0);
    Type[] argTypes = method.getArgTypes();
    String[] argNames = method.getArgNames();
    assertEquals("<init>", method.getName());
    assertEquals(0, argTypes.length);
    assertEquals(0, argNames.length);
    assertEquals(0, method.getArgAnnotations().length);
    assertEquals(0, method.getAnnotations().size());
    assertEquals(0, method.getExceptions().length);

    method = methods.get(1);
    argTypes = method.getArgTypes();
    argNames = method.getArgNames();
    assertEquals("visitAnnotation", method.getName());
    assertEquals(2, argTypes.length);
    assertEquals("java.lang.String", argTypes[0].getClassName());
    assertEquals("boolean", argTypes[1].getClassName());
    assertEquals(2, argNames.length);
    assertEquals("desc", argNames[0]);
    assertEquals("visible", argNames[1]);
    assertEquals(2, method.getArgAnnotations().length);
    assertEquals(0, method.getArgAnnotations()[0].size());
    assertEquals(0, method.getArgAnnotations()[1].size());
    // Note that @Override is a source-only annotation
    assertEquals(0, method.getAnnotations().size());
    assertEquals(0, method.getExceptions().length);
  }

  public void testTopLevel() {
    CollectClassData cd = collect(CollectClassDataTest.class);
    // Don't check for super bit, as it will depend on the JDK used to compile.
    assertEquals(Opcodes.ACC_PUBLIC, cd.getAccess() & ~Opcodes.ACC_SUPER);
    assertEquals(ClassType.TopLevel, cd.getClassType());
  }

  public void testTwo() {
    CollectClassData cd = collect(Two.class);
    // Don't check for super bit, as it will depend on the JDK used to compile.
    assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, cd.getAccess()
        & ~Opcodes.ACC_SUPER);
    assertEquals(ClassType.Nested, cd.getClassType());
    List<CollectFieldData> fields = cd.getFields();
    assertEquals(2, fields.size());
    CollectFieldData field = fields.get(0);
    assertEquals("field", field.getName());
    assertEquals("Ljava/lang/String;", field.getDesc());
    List<CollectAnnotationData> annotations = field.getAnnotations();
    assertEquals(0, annotations.size());
    field = fields.get(1);
    assertEquals("annotatedField", field.getName());
    assertEquals("Ljava/lang/String;", field.getDesc());
    annotations = field.getAnnotations();
    assertEquals(1, annotations.size());
    AnnotationData annotation = annotations.get(0).getAnnotation();
    assertEquals("Lcom/google/gwt/dev/javac/typemodel/test/TestAnnotation;",
        annotation.getDesc());
    assertEquals("field", annotation.getValues().get("value"));
    assertEquals(0, cd.getInterfaces().length);
    annotations = cd.getAnnotations();
    assertEquals(1, annotations.size());
    annotation = annotations.get(0).getAnnotation();
    assertEquals(
        "Lcom/google/gwt/dev/javac/typemodel/test/PrimitiveValuesAnnotation;",
        annotation.getDesc());
    assertEquals(Byte.valueOf((byte) 42), annotation.getValues().get("b"));
    assertEquals(42, annotation.getValues().get("i"));
    assertEquals("java/lang/Object", cd.getSuperName());

    List<CollectMethodData> methods = cd.getMethods();
    assertEquals(3, methods.size());
    // TODO(jat): Is it safe to assume the order?
    CollectMethodData method = methods.get(0);
    Type[] argTypes = method.getArgTypes();
    String[] argNames = method.getArgNames();
    assertEquals("<init>", method.getName());
    assertEquals(1, argTypes.length);
    assertEquals(1, argNames.length);
    assertEquals(1, method.getArgAnnotations().length);
    assertEquals(0, method.getAnnotations().size());
    assertEquals(0, method.getExceptions().length);

    method = methods.get(1);
    argTypes = method.getArgTypes();
    argNames = method.getArgNames();
    assertEquals("foo", method.getName());
    assertEquals(1, argTypes.length);
    assertEquals("int", argTypes[0].getClassName());
    assertEquals(1, argNames.length);
    assertEquals("a", argNames[0]);
    assertEquals(1, method.getArgAnnotations().length);
    assertEquals(0, method.getArgAnnotations()[0].size());
    assertEquals(1, method.getAnnotations().size());
    assertEquals(1, method.getExceptions().length);

    method = methods.get(2);
    argTypes = method.getArgTypes();
    argNames = method.getArgNames();
    assertEquals("<init>", method.getName());
    assertEquals(2, argTypes.length);
    assertEquals("int", argTypes[0].getClassName());
    assertEquals("java.lang.String", argTypes[1].getClassName());
    assertEquals(2, argNames.length);
    assertEquals("a", argNames[0]);
    assertEquals("b", argNames[1]);
    assertEquals(2, method.getArgAnnotations().length);
    assertEquals(0, method.getArgAnnotations()[0].size());
    assertEquals(0, method.getArgAnnotations()[1].size());
    assertEquals(0, method.getAnnotations().size());
    assertEquals(0, method.getExceptions().length);
  }

  public void testTwoInner() {
    CollectClassData cd = collect(Two.TwoInner.class);
    // Don't check for super bit, as it will depend on the JDK used to compile.
    assertEquals(Opcodes.ACC_PUBLIC, cd.getAccess() & ~Opcodes.ACC_SUPER);
    assertEquals(ClassType.Inner, cd.getClassType());
  }

  public void testLocal() {
    CollectClassData cd = collect(Three.class.getName() + "$2Foo");
    // Don't check for super bit, as it will depend on the JDK used to compile.
    assertEquals(0, cd.getAccess() & ~Opcodes.ACC_SUPER);
    assertEquals(ClassType.Local, cd.getClassType());
    assertEquals("methodWithLocal", cd.getOuterMethodName());
  }

  public void testLocalStatic() {
    CollectClassData cd = collect(Three.class.getName() + "$1Foo");
    // Don't check for super bit, as it will depend on the JDK used to compile.
    assertEquals(0, cd.getAccess() & ~Opcodes.ACC_SUPER);
    assertEquals(ClassType.Local, cd.getClassType());
    assertEquals("methodWithLocalStatic", cd.getOuterMethodName());
  }

  // See http://code.google.com/p/google-web-toolkit/issues/detail?id=6591
  // Argument names were incorrect in the presence of long/double args
  public void testLongDoubleArgs() {
    CollectClassData cd = collect(LongDoubleArgs.class);
    List<CollectMethodData> methods = cd.getMethods();
    assertEquals(2, methods.size());
    CollectMethodData method = methods.get(0);
    Type[] argTypes = method.getArgTypes();
    String[] argNames = method.getArgNames();
    assertEquals("<init>", method.getName());
    assertEquals("x", argNames[0]);
    assertEquals("y", argNames[1]);
    assertEquals("z", argNames[2]);
    assertEquals("o", argNames[3]);
    assertEquals("I", argTypes[0].toString());
    assertEquals("J", argTypes[1].toString());
    assertEquals("D", argTypes[2].toString());
    assertEquals("Ljava/lang/Object;", argTypes[3].toString());

    method = methods.get(1);
    argTypes = method.getArgTypes();
    argNames = method.getArgNames();
    assertEquals("longDoubleMethod", method.getName());
    assertEquals("xx", argNames[0]);
    assertEquals("yy", argNames[1]);
    assertEquals("zz", argNames[2]);
    assertEquals("s", argNames[3]);
    assertEquals("I", argTypes[0].toString());
    assertEquals("J", argTypes[1].toString());
    assertEquals("D", argTypes[2].toString());
    assertEquals("Ljava/lang/String;", argTypes[3].toString());
  }

  private CollectClassData collect(Class<?> clazz) {
    return collect(clazz.getName());
  }

  private CollectClassData collect(String className) {
    byte[] bytes = getClassBytes(className);
    assertNotNull("Couldn't load bytes for " + className, bytes);
    CollectClassData cv = new CollectClassData();
    ClassReader reader = new ClassReader(bytes);
    reader.accept(cv, 0);
    return cv;
  }
}
