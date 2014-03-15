/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow.constants;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.impl.JJSTestBase;

import java.util.List;

/**
 * Tests for ExpressionEvaluator - testing evaluation expressions based on
 * assumptions.
 */
public class ExpressionEvaluatorTest extends JJSTestBase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addSnippetClassDecl("static String foo() { return null; };");
  }

 public void testVariableRef() throws Exception {
    assertThat("i", "int",
             "int i = 1;").evaluatesInto("1");
  }

  public void testEq() throws Exception {
    assertThat("i == 1", "boolean",
             "int i = 1;").evaluatesInto("true");
    assertThat("i != 1", "boolean",
             "int i = 1;").evaluatesInto("false");
  }

  public void testNullNotNull() throws Exception {
    assertThat("s == null", "boolean",
             "String s = null;").evaluatesInto("true");
    assertThat("s != null", "boolean",
             "String s = null;").evaluatesInto("false");
    assertThat("null == s", "boolean",
             "String s = null;").evaluatesInto("true");
    assertThat("null != s", "boolean",
             "String s = null;").evaluatesInto("false");
  }

  public void testBinaryExpr() throws Exception {
    assertThat("i + 1", "int", "int i; i = 1;").evaluatesInto("<null>");
    assertThat("1 + i", "int", "int i; i = 1;").evaluatesInto("<null>");
    assertThat("2 + i", "int", "int i = 1;").evaluatesInto("3");
    assertThat("i + 3", "int", "int i = 1;").evaluatesInto("4");
    assertThat("i / 2", "int", "int i = 6;").evaluatesInto("3");
    assertThat("i / 0", "int", "int i = 1;").evaluatesInto("<null>");
    assertThat("4 / 0", "int", "int i = 0;").evaluatesInto("<null>");
  }

  private static class Result {
    private final JValueLiteral literal;

    public Result(JValueLiteral literal) {
      this.literal = literal;
    }

    public void evaluatesInto(String string) {
      String actual = literal == null ? "<null>" : literal.toSource();
      assertEquals(string, actual);
    }
  }

  private Result assertThat(String expr, String type,
      String decls) throws UnableToCompleteException {
    ConstantsAssumption.Updater updater =
      new ConstantsAssumption.Updater(new ConstantsAssumption());

    String codeSnippet = decls;
    codeSnippet += "return " + expr + ";";
    JProgram program = compileSnippet(type, codeSnippet);
    JMethod mainMethod = findMainMethod(program);
    JBlock block = ((JMethodBody) mainMethod.getBody()).getBlock();

    List<JStatement> statements = block.getStatements();
    // TODO: not a pretty assumption detection.
    for (JStatement stmt : statements) {
      if (!(stmt instanceof JDeclarationStatement)) {
        continue;
      }
      JDeclarationStatement decl = (JDeclarationStatement) stmt;
      if (decl.getInitializer() != null) {
        updater.set(decl.getVariableRef().getTarget(),
            (JValueLiteral) decl.getInitializer());
      }
    }

    JReturnStatement returnStatement =
      (JReturnStatement) statements.get(statements.size() - 1);
    return new Result(ExpressionEvaluator.evaluate(returnStatement.getExpr(),
        updater.unwrap()));
  }
}
