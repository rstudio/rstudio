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
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.impl.JJSTestBase;
import com.google.gwt.dev.jjs.impl.gflow.constants.AssumptionDeducer;
import com.google.gwt.dev.jjs.impl.gflow.constants.ConstantsAssumption;
import com.google.gwt.dev.jjs.impl.gflow.constants.ConstantsAssumption.Updater;

import java.util.List;

public class AssumptionsDeducerTest extends JJSTestBase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    addSnippetClassDecl("static class Foo { " +
    		"public int i;" +
    		"public Object o;" +
    		"}");
  }

  public void testBooleanVar() throws Exception {
    from("boolean b = false;", "b", true).deduce("{b = true}");
    from("boolean b = false;", "b", false).deduce("{b = false}");
  }
  
  public void testEq() throws Exception {
    from("int i = 0;", "i == 10", true).deduce("{i = 10}");
    from("int i = 0;", "i == 10", false).deduce("T");
  }

  public void testNeq() throws Exception {
    from("int i = 0;", "i != 10", true).deduce("T");
    from("int i = 0;", "i != 10", false).deduce("{i = 10}");
  }

  public void testInstanceof() throws Exception {
    from("Object o = null;", "o instanceof String", true).deduce("T");
    from("Object o = null;", "o instanceof String", false).deduce("T");
  }

  public void testReference() throws Exception {
    from("String s = null;", "s.length() == 0", true).deduce("T");
    from("String s = null;", "s.length() == 0", false).deduce("T");
    from("Foo f = null;", "f.o == null", true).deduce("T");
    from("Foo f = null;", "f.o == null", false).deduce("T");
  }

  public void testAnd() throws Exception {
    from("int i = 0; int j = 0;", "i == 10 && j == 11", true).deduce("{i = 10, j = 11}");
    from("int i = 0; int j = 0;", "i == 10 && j == 11", false).deduce("T");
  }

  public void testOr() throws Exception {
    from("int i = 0; int j = 0;", "i != 10 || j != 11", false).deduce("{i = 10, j = 11}");
    from("int i = 0; int j = 0;", "i != 10 || j != 11", true).deduce("T");
  }

  public void testFloatEq() throws Exception {
    from("float f = 0;", "f == 1.0", true).deduce("{f = 1.0}");
    // There are positive and negative zeros. Do not deduce anything in here
    from("float f = 0;", "f == 0.0", true).deduce("T");
  }

  public void testDoubleEq() throws Exception {
    from("double f = 0;", "f == 1.0", true).deduce("{f = 1.0}");
    // There are positive and negative zeros. Do not deduce anything in here
    from("double f = 0;", "f == 0.0", true).deduce("T");
  }

  public void testNullNotNull() throws Exception {
    from("String s = null;", "s == null", true).deduce("{s = null}");
    from("String s = null;", "s == null", false).deduce("T");
    from("String s = null;", "s != null", true).deduce("T");
    from("String s = null;", "s != null", false).deduce("{s = null}");
    from("String s = null;", "null == s", true).deduce("{s = null}");
    from("String s = null;", "null == s", false).deduce("T");
    from("String s = null;", "null != s", true).deduce("T");
    from("String s = null;", "null != s", false).deduce("{s = null}");
  }

  private Result from(String decls, String expr, boolean b) throws UnableToCompleteException {
    JProgram program = compileSnippet("void", decls + "\n if(" + expr + ") return;");
    JMethod mainMethod = findMainMethod(program);
    JBlock block = ((JMethodBody) mainMethod.getBody()).getBlock();
    List<JStatement> statements = block.getStatements();
    JIfStatement ifStatement = (JIfStatement) statements.get(statements.size() - 1);
    
    Updater assumptions = new Updater(ConstantsAssumption.TOP);
    AssumptionDeducer.deduceAssumption(ifStatement.getIfExpr(), 
        JBooleanLiteral.get(b), assumptions);
    return new Result(assumptions.unwrap());
  }
  
  private class Result {
    private final ConstantsAssumption assumptions;

    public Result(ConstantsAssumption assumptions) {
      this.assumptions = assumptions;
    }

    public void deduce(String expected) {
      assertEquals(expected, assumptions.toDebugString());
    }
    
  }
}
