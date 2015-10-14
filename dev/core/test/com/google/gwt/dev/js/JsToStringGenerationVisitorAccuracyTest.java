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

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.List;

/**
 * Test the accuracy of the JavaScript parser.
 */
public class JsToStringGenerationVisitorAccuracyTest extends TestCase {

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

  public void testBinaryBinaryUnary() throws Exception {
    // there needs to be a space between the subtraction and negation
    doTest("var x = a - (-b / c)");
    doTest("var x = a - (-b * c)");
    doTest("var x = a - (-b % c)");
  }

  public void testBinaryConditionalUnary() throws Exception {
    // the subtraction operator has to be separated from the negation
    doTest("var x = a - (-b ? c : d)");
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

  public void testEmptyStatements() throws Exception {
    doTest("function f() {if (x);}");
    doTest("function f() {while (x);}");
    doTest("function f() {label:;}");
    doTest("function f() {for (i=0;i<n;i++);}");
    doTest("function f() {for (var x in s);}");
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

  public void testNumberLiteralNameRef() throws Exception {
    doTest("(42).nameRef");
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
    // + prefix not stripped when operand is not literal number
    doTest("var x = +y", "var x = +y");
    // + prefix stripped when operand is literal number
    doTest("var x = +42", "var x = 42");
    // + prefix not stripped when operand is not literal number
    doTest("var x = +y", "var x = +y");
    // + prefix stripped when operand is literal number
    doTest("var x = +42","var x = 42");
    // + <blank> + should not become ++
    doTest("var x = 10+ +\"2\"", "var x = 10+ +\"2\"");
  }

  public void testEscapes() {
    doTestEscapes("\u00006", "'\\x006'");
    doTestEscapes("\u00006\u0000", "'\\x006\\x00'");

    // Single-digit special cases (\b,\t,\n\,f\,\r)
    doTestEscapes("\u0008\u0009\n\u000c\r",
        "'\\b\\t\\n\\f\\r'");

    // Use hexadecimal even when octal would have been suitable.
    doTestEscapes("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u000B\u000E\u000F",
        "'\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07\\x0B\\x0E\\x0F'");
    doTestEscapes("\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017",
        "'\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17'");
    doTestEscapes("\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f",
        "'\\x18\\x19\\x1A\\x1B\\x1C\\x1D\\x1E\\x1F'");

    // Use two-digit hex escapes for characters up to 0xff
    doTestEscapes("\u007f\u00ab", "'\\x7F\\xAB'");

    // Use four-digit unicode escapes for characters from 0x100 up
    doTestEscapes("\u0100\u117f\u2345", "'\\u0100\\u117F\\u2345'");
  }

  private void doTest(String js) throws Exception {
    List<JsStatement> expected = JsParser.parse(SourceOrigin.UNKNOWN,
        new JsProgram().getScope(), new StringReader(js));
    List<JsStatement> actual = parse(expected, true);
    ComparingVisitor.exec(expected, actual);

    actual = parse(expected, false);
    ComparingVisitor.exec(expected, actual);
  }

  private void doTest(String js, String expectedJs) throws Exception {
    List<JsStatement> actual = JsParser.parse(SourceOrigin.UNKNOWN,
        new JsProgram().getScope(), new StringReader(js));
    List<JsStatement> expected = JsParser.parse(SourceOrigin.UNKNOWN,
        new JsProgram().getScope(), new StringReader(expectedJs));
    ComparingVisitor.exec(expected, actual);
  }

  private void doTestEscapes(String value, String expected) {
    String actual = new JsStringLiteral(SourceOrigin.UNKNOWN, value).toString();
    assertEquals(expected, actual);
  }

  private List<JsStatement> parse(List<JsStatement> expected, boolean compact)
      throws Exception {
    TextOutput text = new DefaultTextOutput(compact);
    JsVisitor generator = new JsSourceGenerationVisitor(text);
    generator.acceptList(expected);
    return JsParser.parse(SourceOrigin.UNKNOWN, new JsProgram().getScope(),
        new StringReader(text.toString()));
  }
}
