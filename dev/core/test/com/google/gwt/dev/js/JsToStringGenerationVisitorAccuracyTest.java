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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.List;

public class JsToStringGenerationVisitorAccuracyTest extends TestCase {

  private JsParser parser = new JsParser();

  public void testAdditionPositive() throws Exception {
    // x plus positive 3
    doTest("x + +3");
  }

  public void testArithmetic() throws Exception {
    doTest("a + (b * (c - d)) / (e / f) % x");
  }

  public void testArrayDeclarationArrayAccess() throws Exception {
    doTest("[1,2,3,4][2]");
  }

  public void testArrayLiteralParentheses() throws Exception {
    doTest("var x = [a, (b, c), d]");
  }

  public void testComplexConstruction() throws Exception {
    doTest("(new (new (a(({a : 'b', c : 'd'}),[1,2,3,x,y,z]))())())()");
  }

  public void testConditionalInvocation() throws Exception {
    doTest("(flag?f:g)()");
  }

  public void testConditionals() throws Exception {
    doTest("(a?b:c)?d:e");
    doTest("a?b:c?d:e");
    doTest("a?b?c:d:e?f:g");
  }

  public void testConstructionInvocation() throws Exception {
    doTest("(new a())()");
  }

  public void testDecrement() throws Exception {
    doTest("(x--)-(-(--y))");
  }

  public void testFunctionDeclarationInvocation() throws Exception {
    doTest("(function () {})()");
  }

  public void testInvocationConstruction() throws Exception {
    doTest("new ((a.b.c()).d.e)(1,2,3)");
  }

  public void testNestedConstruction() throws Exception {
    doTest("new (new (new MyClass()))");
  }

  public void testObjectDeclarationArrayAccess() throws Exception {
    doTest("({ a : 'b'})['a']");
  }

  public void testObjectDeclarationMemberAccess() throws Exception {
    doTest("({ a : 'b'}).a");
  }

  public void testObjectLiteral() throws Exception {
    // declaring an object requires parentheses
    doTest("({ 'property' : 'value'})");
  }

  public void testObjectLiteralConditional() throws Exception {
    doTest("var x = {a : ((b(), c) ? d : e)}");
  }

  public void testObjectLiteralDeclaration() throws Exception {
    // quotes are necessary around some property variables
    doTest("var x = {'abc\\'' : 'value'}");
    doTest("var x = {\"a.1\" : 'value'}");
    doTest("var x = {\"\\'\\\"\" : 'value'}");
  }

  public void testObjectLiteralParentheses() throws Exception {
    doTest("var x = {a : (c, d), b : 3}");
  }

  public void testUnaryOperations() throws Exception {
    // spaces or parentheses are necessary to separate negation and decrement
    doTest("var x = -(-(--y))");
  }

  public void testEmptyStatements() throws Exception {
    doTest("function f() {if (x);}");
    doTest("function f() {while (x);}");
    doTest("function f() {label:;}");
    doTest("function f() {for (i=0;i<n;i++);}");
    doTest("function f() {for (var x in s);}");
  }

  private void doTest(String js) throws Exception {
    List<JsStatement> expected = parser.parse(new JsProgram().getScope(),
        new StringReader(js), 0);
    List<JsStatement> actual = parse(expected, true);
    ComparingVisitor.exec(expected, actual);

    actual = parse(expected, false);
    ComparingVisitor.exec(expected, actual);
  }

  private List<JsStatement> parse(List<JsStatement> expected, boolean compact)
      throws Exception {
    TextOutput text = new DefaultTextOutput(compact);
    JsVisitor generator = new JsSourceGenerationVisitor(text);
    generator.acceptList(expected);
    return parser.parse(new JsProgram().getScope(), new StringReader(
        text.toString()), 0);
  }
}
