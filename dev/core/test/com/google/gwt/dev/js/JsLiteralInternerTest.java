/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsLiteral;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 * Verifies that {@link JsNamespaceChooser} can put globals into namespaces.
 */
public class JsLiteralInternerTest extends TestCase {

  public void testSimpleIntern() throws Exception {
    // Numbers
    checkTranslation("var x = 22222, y = 22222;", "var $intern_0=22222;var x=$intern_0,y=$intern_0;");

    // Objects
    checkTranslation("var x = {a:12, b:14}, y = {a:12, b: 14}, z = {a:12, b:13};",
        "var $intern_0={a:12,b:14};var x=$intern_0,y=$intern_0,z={a:12,b:13};");

    checkTranslation("var x = {a:12, b:14}, y = {a:12, b: 14}, z = {a:12, b:13};",
        "var $intern_0=12,$intern_1=14;var x={a:$intern_0,b:$intern_1}," +
        "y={a:$intern_0,b:$intern_1},z={a:$intern_0,b:13};",false);

    // Strings
    checkTranslation("var x = 'abx', y = \"abx\" + 'abc', z = 'ab';",
        "var $intern_0='abx';var x=$intern_0,y=$intern_0+'abc',z='ab';");

    // Regexes
    checkTranslation("var x = /abx/, y = /abx/ + /abc/, z = /ab/;",
        "var $intern_0=/abx/;var x=$intern_0,y=$intern_0+/abc/,z=/ab/;");

    // Arrays
    checkTranslation("var x = [12,14], y = [12, 14], z = [12,13];",
        "var $intern_0=[12,14];var x=$intern_0,y=$intern_0,z=[12,13];");

    checkTranslation("var x = [12,14], y = [12, 14], z = [12, 13];",
        "var $intern_0=12,$intern_1=14;var x=[$intern_0,$intern_1]," +
            "y=[$intern_0,$intern_1],z=[$intern_0,13];",false);
  }

  public void testDoNotInternSmallNumbers() throws Exception {
    checkTranslation("var x = 2, y = 2;", "var x=2,y=2;");
  }

  public void testDoNotInternSingleOccurrence() throws Exception {
    checkTranslation("var x = 222222, y = 333333;", "var x=222222,y=333333;");
  }

  public void testDoNotInternEmptyObjectsOrArrayLiterals() throws Exception {
    checkTranslation("var x = {}, y = {};", "var x={},y={};");
    checkTranslation("var x = [], y = [];", "var x=[],y=[];");
  }

  public void testInternInLhs() throws Exception {
    checkTranslation("var a = 'xxxx';a['xxxx']++;",
        "var $intern_0='xxxx';var a=$intern_0;a[$intern_0]++;");
    checkTranslation("var a = 'xxxx';++a['xxxx'];",
        "var $intern_0='xxxx';var a=$intern_0;++a[$intern_0];");
  }

  public void testDoNotInternIllegalLhs() throws Exception {
    checkTranslation("var a = 'xxxx';'xxxx'++;", "var a='xxxx';'xxxx'++;");
    checkTranslation("var a = 'xxxx';++'xxxx';", "var a='xxxx';++'xxxx';");
    // TODO(rluble): Should also check for literal on lhs of a binary op, but it seems that
    // the JsParser already checks validity so it must be arrived at by a transformation.
  }

  private void checkTranslation(String source, String expectedJs)
      throws IOException, JsParserException {
    checkTranslation(source, expectedJs, true);
  }

  private void checkTranslation(String source, String expectedJs,
      boolean internObjectAndArrayLiterals)
      throws IOException, JsParserException {
    JsProgram program = parseJs(source);
    // Mark all object literals as internable
    if (internObjectAndArrayLiterals) {
      new JsModVisitor() {
        @Override
        public void endVisit(JsObjectLiteral x, JsContext ctx) {
          x.setInternable();
        }

        @Override
        public void endVisit(JsArrayLiteral x, JsContext ctx) {
          x.setInternable();
        }
      }.accept(program);
    }
    exec(program);
    String actual = serializeJs(program);
    assertEquals(expectedJs, actual);
  }

  private Map<JsName, JsLiteral> exec(JsProgram program) {
    // Prerequisite: resolve name references.
    JsSymbolResolver.exec(program);
    return JsLiteralInterner.exec(null, program, JsLiteralInterner.INTERN_ALL);
  }

  private static JsProgram parseJs(String js) throws IOException, JsParserException {
    JsProgram program = new JsProgram();
    List<JsStatement> statements = JsParser.parse(SourceOrigin.UNKNOWN, program.getScope(),
        new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(statements);
    return program;
  }

  private static String serializeJs(JsProgram program1) {
    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);
    generator.accept(program1);
    return text.toString();
  }
}
