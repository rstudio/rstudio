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
package com.google.gwt.uibinder.rebind;

import static com.google.gwt.uibinder.rebind.TypeOracleUtils.hasCompatibleConstructor;
import static com.google.gwt.uibinder.rebind.TypeOracleUtils.typeIsCompatible;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.shell.FailErrorLogger;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link TypeOracleUtils}.
 */
public class TypeOracleUtilsTest extends TestCase {

  protected static final MockJavaResource BAR = new MockJavaResource("my.Bar") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("public class Bar {\n");
      code.append("  public Bar(Child c) {}\n");
      code.append("  public Bar(int i) {}\n");
      code.append("  public Bar(boolean flag, Parent... ps) {}\n");
      code.append("}\n");
      return code;
    }
  };

  protected static final MockJavaResource BAZ = new MockJavaResource("my.Baz") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("public class Baz {\n");
      code.append("  public Baz(Parent[] ps) {}\n");
      code.append("  public Baz(ParentInt... ps) {}\n");
      code.append("}\n");
      return code;
    }
  };

  protected static final MockJavaResource CHILD = new MockJavaResource("my.Child") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("public class Child extends Parent implements ChildInt {\n");
      code.append("}\n");
      return code;
    }
  };

  protected static final MockJavaResource CHILD_INT = new MockJavaResource("my.ChildInt") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("public interface ChildInt extends ParentInt {\n");
      code.append("}\n");
      return code;
    }
  };

  protected static final MockJavaResource FOO = new MockJavaResource("my.Foo") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("public class Foo {\n");
      code.append("  public Foo() {}\n");
      code.append("  public Foo(Parent p) {}\n");
      code.append("  public Foo(double d) {}\n");
      code.append("}\n");
      return code;
    }
  };

  protected static final MockJavaResource PARENT = new MockJavaResource("my.Parent") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("public class Parent implements ParentInt {\n");
      code.append("}\n");
      return code;
    }
  };

  protected static final MockJavaResource PARENT_INT = new MockJavaResource("my.ParentInt") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("public interface ParentInt {\n");
      code.append("}\n");
      return code;
    }
  };

  private JClassType bar;

  private JClassType baz;

  private JClassType child;

  private JArrayType childArray;

  private JClassType childInt;

  private JClassType foo;

  private JClassType parent;

  private JArrayType parentArray;

  private JClassType parentInt;

  private TypeOracle typeOracle;

  /**
   * Test method for {@link TypeOracleUtils#hasCompatibleConstructor(JClassType, JType[])}.
   */
  public void testHasCompatibleConstructorBar() {
    assertHasCtor(bar, child);
    assertNoCtor(bar, childInt);
    assertNoCtor(bar, parent);
    assertHasCtor(bar, JPrimitiveType.INT);
    assertHasCtor(bar, JPrimitiveType.CHAR);
    assertNoCtor(bar, JPrimitiveType.FLOAT);
    assertHasCtor(bar, JPrimitiveType.BOOLEAN, parent);
    assertHasCtor(bar, JPrimitiveType.BOOLEAN, parent, child, parent);
    assertHasCtor(bar, JPrimitiveType.BOOLEAN);
    assertHasCtor(bar, JPrimitiveType.BOOLEAN, parentArray);
    assertNoCtor(bar, parentArray);
    assertHasCtor(bar, JPrimitiveType.BOOLEAN, childArray);
    assertNoCtor(bar);
  }

  /**
   * Test method for {@link TypeOracleUtils#hasCompatibleConstructor(JClassType, JType[])}.
   */
  public void testHasCompatibleConstructorBaz() {
    assertHasCtor(baz, parentArray);
    assertHasCtor(baz, parentInt);
    assertHasCtor(baz, parentInt, childInt);
    assertHasCtor(baz, parent, child);
    assertHasCtor(baz, childArray);
    assertNoCtor(baz, JPrimitiveType.INT);
  }

  /**
   * Test method for {@link TypeOracleUtils#hasCompatibleConstructor(JClassType, JType[])}.
   */
  public void testHasCompatibleConstructorFoo() {
    assertHasCtor(foo);
    assertHasCtor(foo, parent);
    assertHasCtor(foo, child);
    assertNoCtor(foo, foo);
    assertNoCtor(foo, child, child);
    assertNoCtor(foo, JPrimitiveType.BOOLEAN);
    assertHasCtor(foo, JPrimitiveType.DOUBLE);
    assertHasCtor(foo, JPrimitiveType.INT);
  }

  /**
   * Test method for {@link TypeOracleUtils#typeIsCompatible(JType, JType)}.
   */
  public void testTypeIsCompatible() {
    assertTrue("Parent should be compatible with itself", typeIsCompatible(parent, parent));
    assertTrue("Parent should be compatible with parentInt", typeIsCompatible(parentInt, parent));
    assertTrue("Child should be compatible with parent", typeIsCompatible(parent, child));
    assertFalse("Child should not be compatible with parent", typeIsCompatible(child, parent));
    assertTrue("Child should be compatible with itself", typeIsCompatible(child, child));
    assertFalse("Boolean should not be compatible with child", typeIsCompatible(child,
        JPrimitiveType.BOOLEAN));
    assertFalse("Parent[] should not be compatible with parent", typeIsCompatible(parent,
        parentArray));
    assertTrue("Parent[] should be compatible with itself", typeIsCompatible(parentArray,
        parentArray));
    assertFalse("Parent[] should not be compatible with Child[]", typeIsCompatible(childArray,
        parentArray));
    assertTrue("Child[] should be compatible with Parent[]", typeIsCompatible(parentArray,
        childArray));
  }

  @Override
  protected void setUp() throws Exception {
    Set<Resource> resources = new HashSet<Resource>();
    resources.addAll(Arrays.asList(JavaResourceBase.getStandardResources()));
    resources.add(FOO);
    resources.add(BAR);
    resources.add(BAZ);
    resources.add(PARENT);
    resources.add(PARENT_INT);
    resources.add(CHILD);
    resources.add(CHILD_INT);
    TreeLogger logger = new FailErrorLogger();
    CompilationState state =
        CompilationStateBuilder.buildFrom(logger, new CompilerContext(), resources);
    typeOracle = state.getTypeOracle();
    foo = typeOracle.getType("my.Foo");
    bar = typeOracle.getType("my.Bar");
    baz = typeOracle.getType("my.Baz");
    parent = typeOracle.getType("my.Parent");
    parentInt = typeOracle.getType("my.ParentInt");
    parentArray = typeOracle.getArrayType(parent);
    child = typeOracle.getType("my.Child");
    childInt = typeOracle.getType("my.ChildInt");
    childArray = typeOracle.getArrayType(child);
  }

  private void assertHasCtor(JClassType type, JType... argTypes) {
    if (hasCompatibleConstructor(type, argTypes)) {
      return;
    }
    fail(buildCtorName("Should have found ", type, argTypes));
  }

  private void assertNoCtor(JClassType type, JType... argTypes) {
    if (!hasCompatibleConstructor(type, argTypes)) {
      return;
    }
    fail(buildCtorName("Should not have found ", type, argTypes));
  }

  private String buildCtorName(String msg, JClassType type, JType... argTypes) {
    StringBuilder buf = new StringBuilder();
    buf.append(msg).append(type.getName()).append('(');
    boolean first = true;
    for (JType argType : argTypes) {
      if (first) {
        first = false;
      } else {
        buf.append(", ");
      }
      buf.append(argType.getSimpleSourceName());
    }
    buf.append(')');
    return buf.toString();
  }
}
