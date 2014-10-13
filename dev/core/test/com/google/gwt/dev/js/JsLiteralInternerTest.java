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

  private static final String ONES = "1111111111111";
  private static final String TWOS = "2222222222222";
  private static final String THREES = "3333333333333";

  private static final String AS_STRING = "'aaaaaaaaaaaaa'";
  private static final String BS_STRING = "'bbbbbbbbbbbbb'";
  private static final String CS_STRING = "'ccccccccccccc'";

  private static final String AS_REGEX = "/aaaaaaaaaaaaa/";
  private static final String BS_REGEX = "/bbbbbbbbbbbbb/";
  private static final String CS_REGEX = "/ccccccccccccc/";

  private static final String BIG = "'bigbigbigbigbigbigbigbigbigbigbigbigbigbig'";
  private static final String ALSO_BIG = "'alsobigbigbigbigbigbigbigbigbigbigbigbigbigbig'";

  private static final String SMALL_BUT_INTERNABLE = "'aaa'";

  public void testSimpleIntern() throws Exception {
    // Numbers
    checkTranslation(
        String.format("var x = %1$s, y = %1$s;", ONES),
        String.format("var $intern_0=%1$s;var x=$intern_0,y=$intern_0;", ONES));

    // Objects
    checkTranslation(
        String.format("var x={a:%1$s,b:%2$s}, y={a:%1$s,b:%2$s}, z={a:%1$s,b:%3$s};",
            ONES, TWOS, THREES),
        String.format("var $intern_0={a:%1$s,b:%2$s};var x=$intern_0,y=$intern_0,z={a:%1$s,b:%3$s};",
            ONES, TWOS, THREES));

    checkTranslation(
        String.format("var x={a:%1$s,b:%2$s}, y={a:%1$s,b:%2$s}, z={a:%1$s,b:%3$s};",
            ONES, TWOS, THREES),
        String.format("var $intern_0=%1$s,$intern_1=%2$s;var x={a:$intern_0,b:$intern_1}," +
        "y={a:$intern_0,b:$intern_1},z={a:$intern_0,b:%3$s};", ONES, TWOS, THREES), false);

    // Strings
    checkTranslation(
        String.format("var x = %1$s, y = %1$s + %2$s, z = %3$s;", AS_STRING, BS_STRING, CS_STRING),
        String.format("var $intern_0=%1$s;var x=$intern_0,y=$intern_0+%2$s,z=%3$s;", AS_STRING,
            BS_STRING, CS_STRING));

    // Regexes are not internalizable.
    checkTranslation(
        String.format("var x = %1$s, y = %1$s + %2$s, z = %3$s;", AS_REGEX, BS_REGEX, CS_REGEX),
        String.format("var x=%1$s,y=%1$s+%2$s,z=%3$s;", AS_REGEX, BS_REGEX, CS_REGEX));

    // Arrays
    checkTranslation(
        String.format("var x = [%1$s,%2$s], y = [%1$s, %2$s], z = [%1$s,%3$s];",
            ONES, TWOS, THREES),
        String.format("var $intern_0=[%1$s,%2$s];var x=$intern_0,y=$intern_0,z=[%1$s,%3$s];",
            ONES, TWOS, THREES));

    checkTranslation(String.format("var x = [%1$s,%2$s], y = [%1$s, %2$s], z = [%1$s, %3$s];",
        ONES, TWOS, THREES),
        String.format("var $intern_0=%1$s,$intern_1=%2$s;var x=[$intern_0,$intern_1]," +
            "y=[$intern_0,$intern_1],z=[$intern_0,%3$s];", ONES, TWOS, THREES), false);
  }

  public void testDoNotInternSmallNumbers() throws Exception {
    checkTranslation("var x = 2, y = 2;", "var x=2,y=2;");
  }

  public void testDoNotInternSingleOccurrence() throws Exception {
    checkTranslation(
        String.format("var x = %1$s, y = %2$s;", BIG, ALSO_BIG),
        String.format("var x=%1$s,y=%2$s;", BIG, ALSO_BIG));
  }

  public void testDoNotInternEmptyObjectsOrArrayLiterals() throws Exception {
    checkTranslation("var x = {}, y = {};", "var x={},y={};");
    checkTranslation("var x = [], y = [];", "var x=[],y=[];");
  }

  public void testInternInLhs() throws Exception {
    checkTranslation(
        String.format("var a = %1$s;a[%1$s]++;", AS_STRING),
        String.format("var $intern_0=%1$s;var a=$intern_0;a[$intern_0]++;", AS_STRING));
    checkTranslation(
        String.format("var a = %1$s;++a[%1$s];", AS_STRING),
        String.format("var $intern_0=%1$s;var a=$intern_0;++a[$intern_0];", AS_STRING));
  }

  public void testDoNotInternIllegalLhs() throws Exception {
    checkTranslation(String.format("var a = %1$s;%1$s++;", AS_STRING),
        String.format("var a=%1$s;%1$s++;", AS_STRING));
    checkTranslation(String.format("var a = %1$s;++%1$s;", AS_STRING),
        String.format("var a=%1$s;++%1$s;", AS_STRING));
    // TODO(rluble): Should also check for literal on lhs of a binary op, but it seems that
    // the JsParser already checks validity so it must be arrived at by a transformation.
  }

  public void testProfitability() throws Exception {
    // Non profitable
    checkTranslation(
        String.format("var x = %1$s, y = %1$s, z = %1$s;", SMALL_BUT_INTERNABLE),
        String.format("var x=%1$s,y=%1$s,z=%1$s;", SMALL_BUT_INTERNABLE));

    // Becomes profitable as the number of occurrences increases
    checkTranslation(
        String.format("var x = %1$s, y = %1$s, z = %1$s, z = %1$s, z = %1$s, z = %1$s, z = %1$s, " +
            "z = %1$s;", SMALL_BUT_INTERNABLE),
        String.format("var $intern_0=%1$s;var x=$intern_0,y=$intern_0,z=$intern_0,z=$intern_0," +
            "z=$intern_0,z=$intern_0,z=$intern_0,z=$intern_0;", SMALL_BUT_INTERNABLE));

    // With a big string also becomes profitable.
    checkTranslation(
        String.format("var x = %1$s, y = %1$s, z = %1$s;", BIG),
        String.format("var $intern_0=%1$s;var x=$intern_0,y=$intern_0,z=$intern_0;", BIG));
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
