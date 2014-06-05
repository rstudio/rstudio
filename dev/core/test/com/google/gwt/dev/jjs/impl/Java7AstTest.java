/*
 * Copyright 2013 Google Inc.
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
import com.google.gwt.dev.javac.testing.impl.Java7MockResources;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.arg.SourceLevel;

/**
 * Tests that {@link GwtAstBuilder} correctly builds the AST for features introduced in Java 7.
 */
public class Java7AstTest extends JJSTestBase {

  // TODO(rluble): add similar tests to ensure that the AST construction is correct for all types
  // of nodes.
  @Override
  public void setUp() {
    sourceLevel = SourceLevel.JAVA7;
    addAll(JavaResourceBase.AUTOCLOSEABLE, Java7MockResources.TEST_RESOURCE,
        Java7MockResources.EXCEPTION1, Java7MockResources.EXCEPTION2);
  }

  public void testCompileNewStyleLiterals() throws Exception {
    assertEqualExpression("int", "10000000", "1_000_0000");
    assertEqualExpression("int", "5", "0b101");
    assertEqualExpression("int", "6", "0B110");
  }

  public void testCompileStringSwitch() throws Exception {
    assertEqualBlock(
        "String input = \"\";" +
        "switch (input) {" +
        "  case \"AA\": break;" +
        "  case \"BB\": break;" +
        "}",
        "String input = \"\";" +
        "switch (input) {" +
        "  case \"AA\": break;" +
        "  case \"BB\": break;" +
        "}");
  }

  public void testCompileDiamondOperator() throws Exception {
    addSnippetImport("java.util.List");
    addSnippetImport("java.util.ArrayList");
    assertEqualBlock(
        "List l = new ArrayList();",
        "List<String> l = new ArrayList<>();");
  }

  public void testCastingToPrimitiveTypes() throws UnableToCompleteException {
    assertEqualBlock(
        "Object o = null; byte s = (byte) ((Byte) o).byteValue();",
        "Object o = null; byte s = (byte) o;");

    assertEqualBlock(
        "Object o = null; short s = (short) ((Short) o).shortValue();",
        "Object o = null; short s = (short) o;");

    assertEqualBlock(
        "Object o = null; int s = (int) ((Integer) o).intValue();",
        "Object o = null; int s = (int) o;");

    assertEqualBlock(
        "Object o = null; long s = (long) ((Long) o).longValue();",
        "Object o = null; long s = (long) o;");

    assertEqualBlock(
        "Object o = null; float s = (float) ((Float) o).floatValue();",
        "Object o = null; float s = (float) o;");

    assertEqualBlock(
        "Object o = null; double s = (double) ((Double) o).doubleValue();",
        "Object o = null; double s = (double) o;");

    assertEqualBlock(
        "Object o = null; char s = (char) ((Character) o).charValue();",
        "Object o = null; char s = (char) o;");

    assertEqualBlock(
        "Object o = null; char s = (char) ((Character) o).charValue();",
        "Object o = null; char s = (char) o;");

    // This is the expected behaviour, however JDT 3.8.3 emits a compiler error.
    // TODO(rluble): uncomment when JDT is updated.
    // assertEqualBlock(
    //    "Number o = 1; int s = (int) ((Integer) o).intValue();",
    //    "Number o= 1 ; int s = (int) o;");
  }

  private void addAll(Resource... sourceFiles) {
    for (Resource sourceFile : sourceFiles) {
      sourceOracle.addOrReplace(sourceFile);
    }
  }

  private void assertEqualExpression(String type, String expected, String expression)
      throws UnableToCompleteException {
    JExpression testExpresssion = getExpression(type, expression);
    assertEquals(expected, testExpresssion.toSource());
  }

  private JExpression getExpression(String type, String expression)
      throws UnableToCompleteException {
    JProgram program = compileSnippet(type, "return " + expression + ";");
    JMethod mainMethod = findMainMethod(program);
    JMethodBody body = (JMethodBody) mainMethod.getBody();
    JReturnStatement returnStmt = (JReturnStatement) body.getStatements().get(0);
    return returnStmt.getExpr();
  }

  private void assertEqualBlock(String expected, String input)
      throws UnableToCompleteException {
    JBlock testExpression = getStatement(input);
    assertEquals(formatSource("{ " + expected + "}"),
        formatSource(testExpression.toSource()));
  }

  /**
   * Removes most whitespace while still leaving one space separating words.
   *
   * Used to make the assertEquals ignore whitespace (mostly) while still retaining meaningful
   * output when the test fails.
   */
  private String formatSource(String source) {
    return source.replaceAll("\\s+", " ") // substitutes multiple whitespaces into one.
      .replaceAll("\\s([\\p{Punct}&&[^$]])", "$1")  // removes whitespace preceding symbols
                                                    // (except $ which can be part of an identifier)
      .replaceAll("([\\p{Punct}&&[^$]])\\s", "$1"); // removes whitespace succeeding symbols.
  }

  private JBlock getStatement(String statement)
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", statement);
    JMethod mainMethod = findMainMethod(program);
    JMethodBody body = (JMethodBody) mainMethod.getBody();
    return body.getBlock();
  }
}
