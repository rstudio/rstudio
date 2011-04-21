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
package com.google.gwt.resources.ext;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Test cases for methods in ResourceGeneratorUtil.
 */
public class ResourceGeneratorUtilTest extends TestCase {

  JMethod intMethod;
  TypeOracle oracle;
  JClassType rootType;
  JMethod stringMethod;
  JMethod voidMethod;

  @Override
  public void setUp() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err));
    logger.setMaxDetail(TreeLogger.ERROR);
    CompilationState cs = CompilationStateBuilder.buildFrom(logger,
        getResources());
    oracle = cs.getTypeOracle();
    rootType = oracle.findType("test.A");
    intMethod = rootType.findMethod("i", new JType[0]);
    stringMethod = oracle.findType("test.D").findMethod("string", new JType[0]);
    voidMethod = rootType.findMethod("v", new JType[0]);
  }

  public void testMethodOnArray() {
    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("array",
          "Q"), oracle.getType("java.lang.String"));
      fail();
    } catch (NotFoundException e) {
      assertEquals("Cannot resolve member Q on type test.A[]", e.getMessage());
      // OK
    }
  }

  public void testMethodOnPrimitive() {
    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("i", "Q"),
          oracle.getType("java.lang.String"));
      fail();
    } catch (NotFoundException e) {
      assertEquals("Cannot resolve member Q on type int", e.getMessage());
      // OK
    }
  }

  public void testMethodOnVoid() {
    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("v", "Q"),
          oracle.getType("java.lang.String"));
      fail();
    } catch (NotFoundException e) {
      assertEquals("Cannot resolve member Q on type void", e.getMessage());
      // OK
    }
  }

  public void testMethodWithArgs() {
    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("b", "d",
          "string"), oracle.getType("java.lang.String"));
      fail();
    } catch (NotFoundException e) {
      assertEquals("Could not find no-arg method named d in type test.B",
          e.getMessage());
      // OK
    }
  }

  public void testNoSuchMethod() {
    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("b", "Q",
          "d", "string"), oracle.getType("java.lang.String"));
      fail();
    } catch (NotFoundException e) {
      assertEquals("Could not find no-arg method named Q in type test.B",
          e.getMessage());
      // OK
    }
  }

  public void testObjectReturnType() throws NotFoundException {
    assertSame(stringMethod, ResourceGeneratorUtil.getMethodByPath(rootType,
        Arrays.asList("b", "c", "d", "string"), null));
    assertSame(stringMethod, ResourceGeneratorUtil.getMethodByPath(rootType,
        Arrays.asList("b", "c", "d", "string"),
        oracle.getType("java.lang.String")));
    assertSame(stringMethod, ResourceGeneratorUtil.getMethodByPath(rootType,
        Arrays.asList("b", "c", "d", "string"),
        oracle.getType("java.lang.Object")));
  }

  public void testPrimitiveReturnType() throws NotFoundException {
    assertSame(intMethod, ResourceGeneratorUtil.getMethodByPath(rootType,
        Arrays.asList("i"), null));
    assertSame(intMethod, ResourceGeneratorUtil.getMethodByPath(rootType,
        Arrays.asList("i"), JPrimitiveType.INT));
  }

  public void testReturnTypeMismatch() {
    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("b", "c",
          "d", "string"), oracle.getType("java.lang.Throwable"));
      fail();
    } catch (NotFoundException e) {
      assertEquals("Expecting return type java.lang.Throwable"
          + " found java.lang.String", e.getMessage());
      // OK
    }

    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("b", "c",
          "d", "string"), JPrimitiveType.INT);
      fail();
    } catch (NotFoundException e) {
      assertEquals("Expecting return type int" + " found java.lang.String",
          e.getMessage());
      // OK
    }

    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("i"),
          JPrimitiveType.DOUBLE);
      fail();
    } catch (NotFoundException e) {
      assertEquals("Expecting return type double" + " found int",
          e.getMessage());
      // OK
    }

    try {
      ResourceGeneratorUtil.getMethodByPath(rootType, Arrays.asList("i"),
          oracle.getType("java.lang.Throwable"));
      fail();
    } catch (NotFoundException e) {
      assertEquals("Expecting return type java.lang.Throwable" + " found int",
          e.getMessage());
      // OK
    }
  }

  public void testVoidReturnType() throws NotFoundException {
    assertSame(voidMethod, ResourceGeneratorUtil.getMethodByPath(rootType,
        Arrays.asList("v"), null));
    assertSame(voidMethod, ResourceGeneratorUtil.getMethodByPath(rootType,
        Arrays.asList("v"), JPrimitiveType.VOID));
  }

  private Set<Resource> getResources() {
    Set<Resource> res = new HashSet<Resource>(
        Arrays.asList(JavaResourceBase.getStandardResources()));
    res.add(new MockJavaResource("test.A") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package test;");
        code.append("interface A {");
        code.append(" A[] array();");
        code.append(" B b();");
        code.append(" D d(String withArg);");
        code.append(" int i();");
        code.append(" void v();");
        code.append("}");
        return code;
      }
    });
    res.add(new MockJavaResource("test.B") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package test;");
        code.append("abstract class B {");
        code.append(" abstract C c();");
        code.append(" abstract Object c(String withOverload);");
        code.append("}");
        return code;
      }
    });
    res.add(new MockJavaResource("test.C") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package test;");
        code.append("interface C {");
        code.append(" D d();");
        code.append("}");
        return code;
      }
    });
    res.add(new MockJavaResource("test.D") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package test;");
        code.append("interface D {");
        code.append(" String string();");
        code.append("}");
        return code;
      }
    });
    return res;
  }
}
