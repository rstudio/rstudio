/*
 * Copyright 2015 Google Inc.
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
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.PrecompileTaskOptionsImpl;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.jjs.JavaAstConstructor;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Test {@link com.google.gwt.dev.jjs.impl.GwtAstBuilder} correctly builds the AST.
 *
 * TODO(leafwang): Write tests for all other features.
 */
public class GwtAstBuilderTest extends JJSTestBase {
  Set<Resource> sources = Sets.newLinkedHashSet();

  /**
   * A Gwt AST verifier, which is used to verify that in Gwt AST, all JReferenceType instances that
   * are not in current compilation unit are external.
   */
  static class CompilationUnitJavaAstVerifier extends JVisitor {
    /**
     * Throws an assertion error if a ReferenceType that is not in current compilation unit is not
     * external.
     */
    public static void assertNonExternalOnlyInCurrentCU(CompilationUnit compilationUnit) {
      CompilationUnitJavaAstVerifier verifier =
          new CompilationUnitJavaAstVerifier(compilationUnit.getTypes());
      for (JDeclaredType type : compilationUnit.getTypes()) {
        verifier.accept(type);
      }
    }

    final List<JDeclaredType> typesInCurrentCud;

    final List<String> typeNames;

    public CompilationUnitJavaAstVerifier(List<JDeclaredType> typesInCurrentCud) {
      super();
      this.typesInCurrentCud = typesInCurrentCud;
      this.typeNames = Lists.newArrayList();
      for (JDeclaredType type : typesInCurrentCud) {
        typeNames.add(type.getName());
      }
    }

    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      /**
       * class literals only return a meaningful type after ImplementClassLiteralsAsFields has been
       * run.
       */
      if (x.getField() != null) {
        endVisit((JExpression) x, ctx);
      }
    }

    @Override
    public void endVisit(JExpression x, Context ctx) {
      if (x.getType() == null) {
        return;
      }
      assertExternal(x.getType().getUnderlyingType());
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      assertExternal(x.getType());
    }

    @Override
    public void endVisit(JVariable x, Context ctx) {
      assertExternal(x.getType());
    }

    private void assertExternal(JType type) {
      JType typeToCheck = type;
      if (type instanceof JArrayType) {
        typeToCheck = (((JArrayType) type).getLeafType());
      }
      if (typeToCheck == null || !(typeToCheck instanceof JReferenceType)
          || typeToCheck.equals(JReferenceType.NULL_TYPE)) {
        return;
      }
      if (!typeNames.contains(typeToCheck.getName())) {
        assert (typeToCheck.isExternal());
      }
    }
  }

  @Override
  public void setUp() {
    sources.addAll(sourceOracle.getResources());
    sources.add(JavaResourceBase.createMockJavaResource("test.DalNavigationTile",
        "package test;",
        "public class DalNavigationTile extends DalTile {",
        "}"
    ));

    sources.add(JavaResourceBase.createMockJavaResource("test.DalTile",
        "package test;",
        "public class DalTile {"
        + "{ new DalRow().getTiles();"
        + "}",
        "}"
    ));

    sources.add(JavaResourceBase.createMockJavaResource("test.DalGrid",
        "package test;",
        "public class DalGrid {",
        "  public DalNavigationTile getNavigationTile() {"
        + "  DalRow row = new DalRow();"
        + "  DalNavigationTile found = null;"
        + "  for (DalTile dalTile : row.getTiles()) {"
        + "    if (dalTile instanceof DalNavigationTile) {"
        + "      found = (DalNavigationTile) dalTile;"
        + "      break;"
        + "    }"
        + "  }"
        + "  return found;"
        + "}",
        "}"
    ));

    sources.add(JavaResourceBase.createMockJavaResource("test.DalRow",
        "package test;",
        "public class DalRow {"
        + "  public DalTile[] getTiles() {"
        + "    int length = 5;"
        + "    DalTile[] result = new DalTile[length];"
        + "    for (int i = 0; i < length; i++) {"
        + "      result[i] = new DalTile();"
        + "    }"
        + "    return result;"
        + "  }",
        "}"
    ));

    sourceLevel = SourceLevel.DEFAULT_SOURCE_LEVEL;
  }

  public void testNestedClassDisposition() throws UnableToCompleteException {
    sourceLevel = SourceLevel.JAVA8;

    sources.add(JavaResourceBase.createMockJavaResource("test.NestedClasses",
        "package test;",
        "public class NestedClasses {",
        "  static class StaticNestedClass {}",
        "  class InnerNestedClass {}",
        "  interface Lambda { void run(); }",
        "  public static void referencedMethod() {}",
        "  public void m() {",
        "    class LocalClass {}",
        "    Object anonymousInner = new Object(){};",
        "    Lambda lambda = () -> {};",
        "    Lambda methodRef = NestedClasses::referencedMethod;",
        "    new Lambda(){",
        "      public void run(){",
        "        Lambda lambda = () -> {};",
        "      }",
        "    };",
        "  }",
        "}"
    ));

    JProgram program = compileProgram("test.NestedClasses");
    JDeclaredType staticNested = program.getFromTypeMap("test.NestedClasses$StaticNestedClass");
    assertEquals(JDeclaredType.NestedClassDisposition.STATIC, staticNested.getClassDisposition());

    JDeclaredType innerNested = program.getFromTypeMap("test.NestedClasses$InnerNestedClass");
    assertEquals(JDeclaredType.NestedClassDisposition.INNER, innerNested.getClassDisposition());

    JDeclaredType localNested = program.getFromTypeMap("test.NestedClasses$1LocalClass");
    assertEquals(JDeclaredType.NestedClassDisposition.LOCAL, localNested.getClassDisposition());

    JDeclaredType anonymousNested = program.getFromTypeMap("test.NestedClasses$1");
    assertEquals(JDeclaredType.NestedClassDisposition.ANONYMOUS,
        anonymousNested.getClassDisposition());

    JDeclaredType lambdaNested = program.getFromTypeMap("test.NestedClasses$lambda$0$Type");
    assertEquals(JDeclaredType.NestedClassDisposition.LAMBDA, lambdaNested.getClassDisposition());


    JDeclaredType referenceNested =
        program.getFromTypeMap("test.NestedClasses$0methodref$referencedMethod$Type");
    assertEquals(JDeclaredType.NestedClassDisposition.LAMBDA,
        referenceNested.getClassDisposition());

    JDeclaredType topLevel = program.getFromTypeMap("test.NestedClasses");
    assertEquals(JDeclaredType.NestedClassDisposition.TOP_LEVEL, topLevel.getClassDisposition());

    JDeclaredType lambdaNestedInner = program.getFromTypeMap("test.NestedClasses$2$lambda$0$Type");
    assertEquals(JDeclaredType.NestedClassDisposition.LAMBDA, lambdaNestedInner
        .getClassDisposition());
  }

  public void testUniqueArrayTypeInstance() throws UnableToCompleteException {
    JProgram program = compileProgram("test.DalGrid");
    Set<String> arrayTypeNames = Sets.newHashSet();
    for (JArrayType type : program.getAllArrayTypes()) {
      arrayTypeNames.add(type.getName());
    }
    assertEquals(arrayTypeNames.size(), program.getAllArrayTypes().size());
  }

  public void testNonExternalOnlyInCurrentCud() throws UnableToCompleteException {
    CompilationState state = buildCompilationState();
    for (CompilationUnit compilationUnit : state.getCompilationUnits()) {
      CompilationUnitJavaAstVerifier.assertNonExternalOnlyInCurrentCU(compilationUnit);
    }
  }

  private CompilationState buildCompilationState() throws UnableToCompleteException {
    CompilerContext compilerContext =
        new CompilerContext.Builder().options(new PrecompileTaskOptionsImpl() {
            @Override
          public boolean shouldJDTInlineCompileTimeConstants() {
            return false;
          }
        }).build();
    compilerContext.getOptions().setSourceLevel(sourceLevel);
    compilerContext.getOptions().setStrict(true);
    CompilationState state = CompilationStateBuilder.buildFrom(logger, compilerContext, sources,
        getAdditionalTypeProviderDelegate());
    return state;
  }

  private JProgram compileProgram(String entryType) throws UnableToCompleteException {
    CompilerContext compilerContext = provideCompilerContext();
;
    CompilationState state = CompilationStateBuilder.buildFrom(logger, compilerContext, sources,
        getAdditionalTypeProviderDelegate());
    JProgram program = JavaAstConstructor.construct(logger, state, compilerContext,
        null, entryType, "com.google.gwt.lang.Exceptions");
    return program;
  }
}
