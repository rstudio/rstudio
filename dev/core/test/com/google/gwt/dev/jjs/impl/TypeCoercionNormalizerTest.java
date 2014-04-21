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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests for class {@link TypeCoercionNormalizer}.
 */
public class TypeCoercionNormalizerTest extends OptimizerTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    runDeadCodeElimination = false;
  }

  public void testCoerceChar_NonAssignment() throws Exception {
    addSnippetImport("com.google.gwt.lang.Cast");
    optimize("void", "char c = 'a'; String a = \"a\" + c;").into(
        "char c = 'a';",
        "String a = \"a\" + Cast.charToString(c);"
    );
    optimize("void", "char c = 'a'; String a = c + \"a\";").into(
        "char c = 'a';",
        "String a = Cast.charToString(c) + \"a\";"
    );
  }

  public void testCoerceChar_Assignment() throws Exception {
    addSnippetImport("com.google.gwt.lang.Cast");
    optimize("void", "char c = 'a'; String a = \"a\"; a += c ;").into(
        "char c = 'a';",
        "String a = \"a\";",
        "a += Cast.charToString(c) ;"
    );
  }

  public void testCoerceCharLiteral_NonAssignment() throws Exception {
    optimize("void", "String a = \"a\" + 'b' ;").into("String a = \"a\" + \"b\" ;");
    optimize("void", "String a =  'b' + \"a\" ;").into("String a = \"b\" + \"a\";");
  }

  public void testCoerceCharLiteral_Assignment() throws Exception {
    optimize("void", "String a = \"a\"; a += 'b' ;").into(
        "String a = \"a\";",
        "a += \"b\" ;"
    );
  }

  public void testCoerceLong_NonAssignment() throws Exception {
    addSnippetImport("com.google.gwt.lang.LongLib");

    optimize("void", "long l = 1L; String a = \"a\"; a = a + l;").into(
        "long l = 1L;",
        "String a = \"a\";",
        "a = a + LongLib.toString(l);"
    );
    optimize("void", "long l = 1L; String a = \"a\"; a = l + a;").into(
        "long l = 1L;",
        "String a = \"a\";",
        "a = LongLib.toString(l) + a;"
    );
  }

  public void testCoerceLong_Assignment() throws Exception {
    addSnippetImport("com.google.gwt.lang.LongLib");

    optimize("void", "long l = 1L; String a = \"a\"; a += l;").into(
        "long l = 1L;",
        "String a = \"a\";",
        "a += LongLib.toString(l);"
    );
  }

  public void testCoerceLongLiteral_NonAssignment() throws Exception {
    addSnippetImport("com.google.gwt.lang.LongLib");

    optimize("void", "String a = \"a\"; a = a + 1L;").into(
        "String a = \"a\";",
        "a = a + \"1\";"
    );
    optimize("void", "String a = \"a\"; a = 1L + a;").into(
        "String a = \"a\";",
        "a = \"1\" + a;"
    );
  }

  public void testCoerceLongLiteral_Assignment() throws Exception {
    addSnippetImport("com.google.gwt.lang.LongLib");

    optimize("void", "String a = \"a\"; a += 1L ;").into(
        "String a = \"a\";",
        "a += \"1\";"
    );
  }

  public void testDiv_NoRewrite() throws Exception {
    optimize("void", "float a = 0.2f; double b = 3.1;  double c = a / b;").into(
        "float a = 0.2f;",
        "double b = 3.1;",
        "double c = a / b;"
    );
  }

  public void testDiv_NarrowResult() throws Exception {
    addSnippetImport("com.google.gwt.lang.Cast");

    optimize("void", "long a = 2; byte b = 3;  double c = a / b;").into(
        "long a = 2;",
        "byte b = 3;",
        "double c = Cast.narrow_long(a / b);"
    );
    optimize("void", "char a = 2; char b = 3;  double c = a / b;").into(
        "char a = 2;",
        "char b = 3;",
        "double c = Cast.narrow_int(a / b);"
    );
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    TypeCoercionNormalizer.exec(program);
    return true;
  }
}
