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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.resource.Resource;

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
          || typeToCheck.equals(JNullType.INSTANCE)) {
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
    JProgram program = JavaAstConstructor.construct(logger, state, compilerContext.getOptions(),
        null, entryType, "com.google.gwt.lang.Exceptions");
    return program;
  }
}
