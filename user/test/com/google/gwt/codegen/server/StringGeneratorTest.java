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
package com.google.gwt.codegen.server;

import junit.framework.TestCase;

/**
 * Test for {@link StringGenerator}.
 */
public class StringGeneratorTest extends TestCase {

  public void testSafeHtmlArgOnlyPrimitive() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendExpression("name", false, true, true);
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().append(name).toSafeHtml()",
        buf.toString());
  }

  public void testSafeHtmlArgPrimitive() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendStringLiteral("Hello ");
    gen.appendExpression("name", false, true, true);
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().appendHtmlConstant("
       + "\"Hello \").append(name).toSafeHtml()", buf.toString());
  }

  public void testSafeHtmlArgSafeHtml() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendStringLiteral("Hello ");
    gen.appendExpression("name", true, false, false);
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().appendHtmlConstant("
       + "\"Hello \").append(name).toSafeHtml()", buf.toString());
  }

  public void testSafeHtmlArgString() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendStringLiteral("Hello ");
    gen.appendExpression("name", false, false, false);
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().appendHtmlConstant("
       + "\"Hello \").appendEscaped(name).toSafeHtml()", buf.toString());
  }

  public void testSafeHtmlArgStringFirst() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendExpression("name", false, false, false);
    gen.appendStringLiteral(" says hello");
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().appendEscaped(name)"
        + ".appendHtmlConstant(\" says hello\").toSafeHtml()", buf.toString());
  }

  public void testSafeHtmlArgUnknown() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendStringLiteral("Hello ");
    gen.appendExpression("name", false, false, true);
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().appendHtmlConstant("
        + "\"Hello \").appendEscaped(String.valueOf(name)).toSafeHtml()", buf.toString());
  }

  public void testSafeHtmlArgUnknownFirst() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendExpression("name", false, false, true);
    gen.appendStringLiteral(" says hello");
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().appendEscaped("
        + "String.valueOf(name)).appendHtmlConstant(\" says hello\").toSafeHtml()",
        buf.toString());
  }

  public void testSafeHtmlEmpty() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().toSafeHtml()",
        buf.toString());
  }

  public void testSafeHtmlLiteral() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendStringLiteral("Hello");
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().appendHtmlConstant("
        + "\"Hello\").toSafeHtml()", buf.toString());
  }

  public void testSafeHtmlLiteralLiteral() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, true);
    gen.appendStringLiteral("Hello ");
    gen.appendStringLiteral("there");
    gen.completeString();
    assertEquals("new com.google.gwt.safehtml.shared.SafeHtmlBuilder().appendHtmlConstant("
        + "\"Hello there\").toSafeHtml()", buf.toString());
  }

  public void testStringArgOnlyPrimitive() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.appendExpression("name", false, true, true);
    gen.completeString();
    assertEquals("\"\" + name", buf.toString());
  }

  public void testStringArgPrimitive() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.appendStringLiteral("Hello ");
    gen.appendExpression("name", false, true, true);
    gen.completeString();
    assertEquals("\"Hello \" + name", buf.toString());
  }

  public void testStringArgString() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.appendStringLiteral("Hello ");
    gen.appendExpression("name", false, false, false);
    gen.completeString();
    assertEquals("\"Hello \" + name", buf.toString());
  }

  public void testStringArgStringFirst() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.appendExpression("name", false, false, false);
    gen.appendStringLiteral(" says hello");
    gen.completeString();
    assertEquals("name + \" says hello\"", buf.toString());
  }

  public void testStringArgUnknown() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.appendStringLiteral("Hello ");
    gen.appendExpression("name", false, false, true);
    gen.completeString();
    assertEquals("\"Hello \" + name", buf.toString());
  }

  public void testStringArgUnknownFirst() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.appendExpression("name", false, false, true);
    gen.appendStringLiteral(" says hello");
    gen.completeString();
    assertEquals("\"\" + name + \" says hello\"", buf.toString());
  }

  public void testStringEmpty() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.completeString();
    assertEquals("\"\"", buf.toString());
  }

  public void testStringLiteral() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.appendStringLiteral("Hello");
    gen.completeString();
    assertEquals("\"Hello\"", buf.toString());
  }

  public void testStringLiteralLiteral() {
    StringBuilder buf = new StringBuilder();
    StringGenerator gen = StringGenerator.create(buf, false);
    gen.appendStringLiteral("Hello ");
    gen.appendStringLiteral("there");
    gen.completeString();
    assertEquals("\"Hello there\"", buf.toString());
  }
}
