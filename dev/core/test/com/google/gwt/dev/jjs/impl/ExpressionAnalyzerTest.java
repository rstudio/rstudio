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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;

/**
 * Tests {@link ExpressionAnalyzer}.
 */
public class ExpressionAnalyzerTest extends JJSTestBase {

  private static class Result {
    private final ExpressionAnalyzer ea;
    private boolean accessesField;
    private boolean accessesFieldNonFinal;
    private boolean accessesLocal;
    private boolean accessesParameter;
    private boolean canThrowException;
    private boolean createsObject;
    private boolean hasAssignment;
    private boolean hasAssignmentToField;
    private boolean hasAssignmentToLocal;
    private boolean hasAssignmentToParameter;

    public Result(ExpressionAnalyzer ea) {
      this.ea = ea;
    }

    public Result accessesField() {
      accessesField = true;
      return this;
    }

    public Result accessesFieldNonFinal() {
      accessesField = true;
      accessesFieldNonFinal = true;
      return this;
    }

    public Result accessesLocal() {
      accessesLocal = true;
      return this;
    }

    public Result accessesParameter() {
      accessesParameter = true;
      return this;
    }

    public Result canThrowException() {
      canThrowException = true;
      return this;
    }

    public Result createsObject() {
      createsObject = true;
      return this;
    }

    public Result hasAssignment() {
      hasAssignment = true;
      return this;
    }

    public Result hasAssignmentToField() {
      hasAssignment = true;
      hasAssignmentToField = true;
      return this;
    }

    public Result hasAssignmentToLocal() {
      hasAssignment = true;
      hasAssignmentToLocal = true;
      return this;
    }

    public Result hasAssignmentToParameter() {
      hasAssignment = true;
      hasAssignmentToParameter = true;
      return this;
    }

    public void check() {
      assertEquals(accessesField, ea.accessesField());
      assertEquals(accessesFieldNonFinal, ea.accessesFieldNonFinal());
      assertEquals(accessesLocal, ea.accessesLocal());
      assertEquals(accessesParameter, ea.accessesParameter());
      assertEquals(canThrowException, ea.canThrowException());
      assertEquals(createsObject, ea.createsObject());
      assertEquals(hasAssignment, ea.hasAssignment());
      assertEquals(hasAssignmentToField, ea.hasAssignmentToField());
      assertEquals(hasAssignmentToLocal, ea.hasAssignmentToLocal());
      assertEquals(hasAssignmentToParameter, ea.hasAssignmentToParameter());
    }
  }

  public void testEmpty() throws Exception {
    analyzeExpression("int", "0").check();
  }

  public void testFieldAccessClinit() throws Exception {
    sourceOracle.addOrReplace(new MockJavaResource("test.Foo") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public class Foo {\n");
        code.append("  static final boolean value = trueMethod();");
        code.append("  static boolean trueMethod() { return true; }");
        code.append("}\n");
        return code;
      }
    });
    analyzeExpression("boolean", "Foo.value").accessesFieldNonFinal().canThrowException().createsObject().hasAssignmentToField().check();
  }

  public void testFieldAccessInstance() throws Exception {
    sourceOracle.addOrReplace(new MockJavaResource("test.Foo") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
        code.append("public class Foo {\n");
        code.append("  public final boolean BOOL_CONST = true;\n");
        code.append("  public boolean FOO = true;\n");
        code.append("}\n");
        return code;
      }
    });
    addSnippetImport("test.Foo");
    addSnippetClassDecl("static final Foo f = new Foo();");

    analyzeExpression("boolean", "f.BOOL_CONST").accessesField().canThrowException().check();

    analyzeExpression("boolean", "f.FOO").accessesFieldNonFinal().canThrowException().check();

    analyzeExpression("boolean", "f.FOO = false").accessesFieldNonFinal().canThrowException().hasAssignment().hasAssignmentToField().check();
  }

  public void testFieldAccessStatic() throws Exception {
    addSnippetClassDecl("static boolean trueMethod() { return true; }");
    addSnippetClassDecl("static final boolean BOOL_CONST = trueMethod();");
    addSnippetClassDecl("static volatile boolean FOO;");

    analyzeExpression("boolean", "BOOL_CONST").accessesField().check();

    analyzeExpression("boolean", "FOO").accessesFieldNonFinal().check();

    analyzeExpression("boolean", "FOO = false").accessesFieldNonFinal().hasAssignmentToField().check();
  }

  public void testNewArray() throws Exception {
    analyzeExpression("float[]", "new float[3]").createsObject().check();
  }

  private Result analyzeExpression(String type, String expression)
      throws UnableToCompleteException {
    JProgram program = compileSnippet(type, "return " + expression + ";");
    ExpressionAnalyzer ea = new ExpressionAnalyzer();
    JMethod mainMethod = findMainMethod(program);
    JMethodBody body = (JMethodBody) mainMethod.getBody();
    JReturnStatement returnStmt = (JReturnStatement) body.getStatements().get(0);
    ea.accept(returnStmt.getExpr());
    return new Result(ea);
  }
}
