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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.JsniRefLookup.ErrorReporter;
import com.google.gwt.dev.util.JsniRef;

/**
 * Tests class {@link JsniRefLookup}.
 */
public class JsniRefLookupTest extends JJSTestBase {
  private class MockErrorReporter implements ErrorReporter {
    private String error = null;

    public void assertHasError() {
      assertTrue("Expected a lookup failure", error != null);
    }

    public void assertNoError() {
      assertTrue("Unexpected error: " + error, error == null);
    }

    @Override
    public void reportError(String error) {
      this.error = error;
    }
  }

  private JProgram program;

  @Override
  public void setUp() {
    sourceOracle.addOrReplace(new MockJavaResource("test.Intf") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public interface Intf {\n");
        code.append("  public int addTwoOverloaded(int x);\n");
        code.append("  public int addOne(int x);\n");
        code.append("  public int foo(int x);\n");
        code.append("  public double foo(double x);\n");
        code.append("}\n");
        return code;
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.Foo") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public class Foo implements Intf {\n");
        code.append("  public Foo() { }\n");
        code.append("  public Foo(int x) { }\n");
        code.append("  public static int intStatic;\n");
        code.append("  public int intInstance;\n");
        code.append("  public int addOne(int x) { return x+1; }\n");
        code.append("  public int addTwoOverloaded(int x) { return x+2; }\n");
        code.append("  public double addTwoOverloaded(double x) { return x+2; }\n");
        code.append("  public int foo(int x) { return x+1; }\n");
        code.append("  public double foo(double x) { return x+1; }\n");
        code.append("  public int bar(int x) { return x+1; }\n");
        code.append("}\n");
        return code;
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.Bar") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public class Bar extends Foo {\n");
        code.append("  public Bar() { }\n");
        code.append("  public int foo(int x) { return x+1; }\n");
        code.append("  public int bar(int x) { return x+1; }\n");
        code.append("  public double bar(double x) { return x+1; }\n");
        code.append("}\n");
        return code;
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.GenericClass") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public abstract class GenericClass<T> {\n");
        code.append("  abstract void set(T x);\n");
        code.append("}\n");
        return code;
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.ClassWithBridge") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("class ClassWithBridge extends GenericClass<String> {\n");
        code.append("  void set(String x) { }\n");
        code.append("}\n");
        return code;
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.PrivateSup") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public class PrivateSup {\n");
        code.append("  private static int field;\n");
        code.append("  private static int method() { return 0; }\n");
        code.append("  private static int fieldSup;\n");
        code.append("  private static int methodSuP() { return 0; }\n");
        code.append("}\n");
        return code;
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.PrivateSub") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public class PrivateSub extends PrivateSup {\n");
        code.append("  private static float field;\n");
        code.append("  private static float method() { return 0; }\n");
        code.append("  private static float methodSub() { return 0; }\n");
        code.append("}\n");
        return code;
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.DiffRetSuper") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public interface DiffRetSuper {\n");
        code.append("  Object foo();\n");
        code.append("}\n");
        return code;
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.DiffRetSub") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public interface DiffRetSub extends DiffRetSuper {\n");
        code.append("  String foo();\n");
        code.append("}\n");
        return code;
      }
    });

    addSnippetImport("test.DiffRetSub");

    try {
      // The snippet must reference the classes so they will be compiled in
      program = compileSnippet("void",
          "new test.Foo(); new test.Bar(); new ClassWithBridge(); new PrivateSub();");
    } catch (UnableToCompleteException e) {
      throw new RuntimeException(e);
    }
  }

  public void testBasicLookups() {
    {
      MockErrorReporter errors = new MockErrorReporter();
      JField res = (JField) lookup("test.Foo::intStatic", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("intStatic", res.getName());
      assertTrue(res.isStatic());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JField res = (JField) lookup("test.Foo::intInstance", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("intInstance", res.getName());
      assertFalse(res.isStatic());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Foo::addOne(I)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("addOne", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Foo::addTwoOverloaded(I)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("addTwoOverloaded", res.getName());
      assertEquals(JPrimitiveType.INT, res.getParams().get(0).getType());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Foo::addTwoOverloaded(D)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("addTwoOverloaded", res.getName());
      assertEquals(JPrimitiveType.DOUBLE, res.getParams().get(0).getType());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Foo::new()", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("Foo", res.getName());
    }

    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Foo::bogoField", errors);
      errors.assertHasError();
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Foo::bogoMethod()", errors);
      errors.assertHasError();
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Foo::new(J)", errors);
      errors.assertHasError();
    }
  }

  public void testBridgeMethods() {
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup(
          "test.ClassWithBridge::set(Ljava/lang/String;)", errors);
      errors.assertNoError();
      assertEquals("test.ClassWithBridge", res.getEnclosingType().getName());
      assertEquals("set", res.getName());
      assertEquals("java.lang.String",
          res.getParams().get(0).getType().getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.ClassWithBridge::set(*)", errors);
      errors.assertNoError();
      assertEquals("test.ClassWithBridge", res.getEnclosingType().getName());
      assertEquals("set", res.getName());
      assertEquals("java.lang.String",
          res.getParams().get(0).getType().getName());
    }
    {
      // For backward compatibility, allow calling a bridge method directly
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup(
          "test.ClassWithBridge::set(Ljava/lang/Object;)", errors);
      errors.assertNoError();
      assertEquals("test.ClassWithBridge", res.getEnclosingType().getName());
      assertEquals("set", res.getName());
      assertEquals("java.lang.Object",
          res.getParams().get(0).getType().getName());
    }
  }

  public void testConstructors() {
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Foo::new()", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("Foo", res.getName());
      assertEquals(0, res.getParams().size());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Foo::new(I)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("Foo", res.getName());
      assertEquals(1, res.getParams().size());
      assertSame(JPrimitiveType.INT, res.getParams().get(0).getType());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Foo::new(*)", errors);
      errors.assertHasError();
    }

    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::new()", errors);
      errors.assertNoError();
      assertEquals("test.Bar", res.getEnclosingType().getName());
      assertEquals("Bar", res.getName());
      assertEquals(0, res.getParams().size());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::new(*)", errors);
      errors.assertNoError();
      assertEquals("test.Bar", res.getEnclosingType().getName());
      assertEquals("Bar", res.getName());
      assertEquals(0, res.getParams().size());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Bar::new(I)", errors);
      errors.assertHasError();
    }
  }

  public void testInheritance() {
    // test lookups of methods where the subtype is specified
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::addOne(I)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("addOne", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::addTwoOverloaded(I)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("addTwoOverloaded", res.getName());
      assertEquals(JPrimitiveType.INT, res.getParams().get(0).getType());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::addTwoOverloaded(D)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("addTwoOverloaded", res.getName());
      assertEquals(JPrimitiveType.DOUBLE, res.getParams().get(0).getType());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::addOne(*)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("addOne", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Bar::addTwoOverloaded(*)", errors);
      errors.assertHasError();
    }

    /*
     * test wildcard lookups when the subtype overloads but the supertype does
     * not, and vice versa
     */
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::foo(I)", errors);
      errors.assertNoError();
      assertEquals("test.Bar", res.getEnclosingType().getName());
      assertEquals("foo", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::bar(I)", errors);
      errors.assertNoError();
      assertEquals("test.Bar", res.getEnclosingType().getName());
      assertEquals("bar", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Bar::bar(D)", errors);
      errors.assertNoError();
      assertEquals("test.Bar", res.getEnclosingType().getName());
      assertEquals("bar", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Bar::foo(*)", errors);
      errors.assertHasError();
    }

    /*
     * Test a lookup where the subtype has a narrower return type than the
     * supertype.
     */
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.DiffRetSub::foo()", errors);
      errors.assertNoError();
      assertEquals("test.DiffRetSub", res.getEnclosingType().getName());
      assertEquals("foo", res.getName());
    }
  }

  public void testInterfaces() {
    // Test lookups in the interface that specify the types
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Intf::addTwoOverloaded(I)", errors);
      errors.assertNoError();
      assertEquals("test.Intf", res.getEnclosingType().getName());
      assertEquals("addTwoOverloaded", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Intf::addOne(I)", errors);
      errors.assertNoError();
      assertEquals("test.Intf", res.getEnclosingType().getName());
      assertEquals("addOne", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Intf::foo(I)", errors);
      errors.assertNoError();
      assertEquals("test.Intf", res.getEnclosingType().getName());
      assertEquals("foo", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Intf::foo(D)", errors);
      errors.assertNoError();
      assertEquals("test.Intf", res.getEnclosingType().getName());
      assertEquals("foo", res.getName());
    }

    // Test lookups that use wildcards
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Intf::addTwoOverloaded(*)", errors);
      errors.assertNoError();
      assertEquals("test.Intf", res.getEnclosingType().getName());
      assertEquals("addTwoOverloaded", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Intf::addOne(*)", errors);
      errors.assertNoError();
      assertEquals("test.Intf", res.getEnclosingType().getName());
      assertEquals("addOne", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Intf::foo(*)", errors);
      errors.assertHasError();
    }
  }

  public void testPrivate() {
    // test private entries in the requested class
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.PrivateSub::method()", errors);
      errors.assertNoError();
      assertEquals("test.PrivateSub", res.getEnclosingType().getName());
      assertEquals("method", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      JField res = (JField) lookup("test.PrivateSub::field", errors);
      errors.assertNoError();
      assertEquals("test.PrivateSub", res.getEnclosingType().getName());
      assertEquals("field", res.getName());
    }

    // test private entries in the superclass
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.PrivateSub::methodSup()", errors);
      errors.assertHasError();
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.PrivateSub::fieldSup", errors);
      errors.assertHasError();
    }
  }

  public void testWildcardLookups() {
    {
      MockErrorReporter errors = new MockErrorReporter();
      JMethod res = (JMethod) lookup("test.Foo::addOne(*)", errors);
      errors.assertNoError();
      assertEquals("test.Foo", res.getEnclosingType().getName());
      assertEquals("addOne", res.getName());
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Foo::addTwoOverloaded(*)", errors);
      errors.assertHasError();
    }
    {
      MockErrorReporter errors = new MockErrorReporter();
      lookup("test.Foo::bogoMethod(*)", errors);
      errors.assertHasError();
    }
  }

  private JNode lookup(String refString, MockErrorReporter errors) {
    return JsniRefLookup.findJsniRefTarget(JsniRef.parse(refString), program,
        errors);
  }
}
