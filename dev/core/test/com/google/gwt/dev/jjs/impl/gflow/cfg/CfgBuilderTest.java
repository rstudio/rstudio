/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow.cfg;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.JJSTestBase;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import java.util.List;

/**
 * Test class for CfgBuilfer.
 */
public class CfgBuilderTest extends JJSTestBase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    addSnippetClassDecl("static boolean b;");
    addSnippetClassDecl("static boolean b1;");
    addSnippetClassDecl("static boolean b2;");
    addSnippetClassDecl("static boolean b3;");
    addSnippetClassDecl("static int i;");
    addSnippetClassDecl("static int j;");
    addSnippetClassDecl("static int k;");
    addSnippetClassDecl("static int l;");
    addSnippetClassDecl("static int m;");
    addSnippetClassDecl("static class CheckedException extends Exception {}");
    addSnippetClassDecl(
        "static class UncheckedException1 extends RuntimeException {}");
    addSnippetClassDecl(
        "static class UncheckedException2 extends RuntimeException {}");
    addSnippetClassDecl("static RuntimeException runtimeException;");
    addSnippetClassDecl("static CheckedException checkedException;");
    addSnippetClassDecl("static UncheckedException1 uncheckedException1;");
    addSnippetClassDecl("static UncheckedException2 uncheckedException2;");

    addSnippetClassDecl("static int[] ii;");
    addSnippetClassDecl("static int[] jj;");

    addSnippetClassDecl("static void throwCheckedException() " +
        "throws CheckedException {}");
    addSnippetClassDecl("static void throwUncheckedException() {}");
    addSnippetClassDecl("static void throwSeveralExceptions() " +
        "throws CheckedException, UncheckedException1 {}");

    addSnippetClassDecl("static class Foo { int i; int j; int k; }");
    addSnippetClassDecl("static Foo createFoo() {return null;}");
  }

  public void testConstantAssignment() throws Exception {
    assertCfg("void", "i = 1;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(i, 1) -> [*]",
        "END");
  }

  public void testReferenceAssignment() throws Exception {
    assertCfg("void", "i = j;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(j) -> [*]",
        "WRITE(i, EntryPoint.j) -> [*]",
        "END");
  }

  public void testModAssignment() throws Exception {
    assertCfg("void", "i += 1;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READWRITE(i, null) -> [*]",
        "END");
  }

  public void testDeclarationWithInitializer() throws Exception {
    assertCfg("void", "int i = 1;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(i, 1) -> [*]",
        "END");
  }

  public void testDeclarationWithInitializerRead() throws Exception {
    assertCfg("void", "int i = j + k;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(j) -> [*]",
        "READ(k) -> [*]",
        "WRITE(i, EntryPoint.j + EntryPoint.k) -> [*]",
        "END");
  }

  public void testDeclarationWithoutInitializer() throws Exception {
    assertCfg("void", "int i;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "END");
  }

  public void testBinopAssignment() throws Exception {
    assertCfg("void", "i = j + k;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(j) -> [*]",
        "READ(k) -> [*]",
        "WRITE(i, EntryPoint.j + EntryPoint.k) -> [*]",
        "END");
  }

  public void testPostIncrement() throws Exception {
    assertCfg("void", "i++;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READWRITE(i, null) -> [*]",
        "END");
  }

  public void testPreIncrement() throws Exception {
    assertCfg("void", "++i;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READWRITE(i, null) -> [*]",
        "END");
  }

  public void testConditional() throws Exception {
    assertCfg("void", "i = b1 ? j : k;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [THEN=*, ELSE=1]",
        "READ(j) -> [2]",
        "1: READ(k) -> [*]",
        "2: WRITE(i, EntryPoint.b1 ? EntryPoint.j : EntryPoint.k) -> [*]",
        "END");
  }

  public void testAnd() throws Exception {
    assertCfg("void", "b3 = b1 && b2;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [THEN=*, ELSE=1]",
        "READ(b2) -> [*]",
        "1: WRITE(b3, EntryPoint.b1 && EntryPoint.b2) -> [*]",
        "END");
  }

  public void testOr() throws Exception {
    assertCfg("void", "b3 = b1 || b2;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [ELSE=*, THEN=1]",
        "READ(b2) -> [*]",
        "1: WRITE(b3, EntryPoint.b1 || EntryPoint.b2) -> [*]",
        "END");
  }

  public void testMultipleExpressionStatements() throws Exception {
    assertCfg("void",
        "i = 1;",
        "j = 2;",
        "m = k = j;").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(i, 1) -> [*]",
            "STMT -> [*]",
            "WRITE(j, 2) -> [*]",
            "STMT -> [*]",
            "READ(j) -> [*]",
            "WRITE(k, EntryPoint.j) -> [*]",
            "WRITE(m, EntryPoint.k = EntryPoint.j) -> [*]",
            "END");
  }

  public void testIfStatement1() throws Exception {
    assertCfg("void",
        "if (i == 1) {",
        "  j = 2;",
        "}",
        "k = j;").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(j, 2) -> [*]",
            "1: STMT -> [*]",
            "READ(j) -> [*]",
            "WRITE(k, EntryPoint.j) -> [*]",
            "END");
  }

  public void testIfStatement2() throws Exception {
    assertCfg("void",
        "if ((i = 1) == 2) {",
        "  j = 2;",
        "} else {",
        "  k = j;",
        "}").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(i, 1) -> [*]",
            "COND ((EntryPoint.i = 1) == 2) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(j, 2) -> [2]",
            "1: BLOCK -> [*]",
            "STMT -> [*]",
            "READ(j) -> [*]",
            "WRITE(k, EntryPoint.j) -> [*]",
            "2: END");
  }

  public void testIfStatement3() throws Exception {
    assertCfg("void", "if (b1 || b2) {j = 2;}").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [ELSE=*, THEN=1]",
        "READ(b2) -> [*]",
        "1: COND (EntryPoint.b1 || EntryPoint.b2) -> [THEN=*, ELSE=2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(j, 2) -> [*]",
        "2: END");
  }

  public void testWhileStatement() throws Exception {
    assertCfg("void", "while (i == 1) {j = 2;} k = j;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "1: READ(i) -> [*]",
        "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(j, 2) -> [1]",
        "2: STMT -> [*]",
        "READ(j) -> [*]",
        "WRITE(k, EntryPoint.j) -> [*]",
        "END");
  }

  public void testDoStatement() throws Exception {
    assertCfg("void", "do { j = 2; } while (i == 1);").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "1: BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(j, 2) -> [*]",
        "READ(i) -> [*]",
        "COND (EntryPoint.i == 1) -> [THEN=1, ELSE=*]",
        "END");
  }

  public void testDoStatementBreakNoLabel() throws Exception {
    assertCfg("void", "do { if (b1) { break; } else { do { j = 2; } while (b2); } } while (i == 1);").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "1: BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [THEN=*, ELSE=2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "GOTO -> [4]",
        "2: BLOCK -> [*]",
        "STMT -> [*]",
        "3: BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(j, 2) -> [*]",
        "READ(b2) -> [*]",
        "COND (EntryPoint.b2) -> [THEN=3, ELSE=*]",
        "READ(i) -> [*]",
        "COND (EntryPoint.i == 1) -> [THEN=1, ELSE=*]",
        "4: END");
  }

  public void testDoStatementContinueNoLabel() throws Exception {
    assertCfg("void", "do { if (b1) { continue; } else { do { j = 2; } while (b2); } } while (i == 1);").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "1: BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [THEN=*, ELSE=2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "GOTO -> [1]",
        "2: BLOCK -> [*]",
        "STMT -> [*]",
        "3: BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(j, 2) -> [*]",
        "READ(b2) -> [*]",
        "COND (EntryPoint.b2) -> [THEN=3, ELSE=*]",
        "READ(i) -> [*]",
        "COND (EntryPoint.i == 1) -> [THEN=1, ELSE=*]",
        "END");
  }

  public void testReturn1() throws Exception {
    assertCfg("void", "return;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "GOTO -> [*]",
        "END");
  }

  public void testReturn2() throws Exception {
    assertCfg("boolean", "i = 1; return i == 2;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(i, 1) -> [*]",
        "STMT -> [*]",
        "READ(i) -> [*]",
        "GOTO -> [*]",
        "END");
  }

  public void testReturn3() throws Exception {
    assertCfg("boolean",
        "i = 1;",
        "if (i == 1) {",
        "  i = 2;",
        "  return true;",
        "} else {",
        "  i = 3;",
        "  return false;",
        "}").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(i, 1) -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(i, 2) -> [*]",
            "STMT -> [*]",
            "GOTO -> [2]",
            "1: BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(i, 3) -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "2: END");
  }

  public void testForStatement() throws Exception {
    assertCfg("int",
        "int j = 0;",
        "for (int i = 0; i < 10; ++i) { j++; }",
        "return j;").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(j, 0) -> [*]",
            "STMT -> [*]",
            "STMT -> [*]",
            "WRITE(i, 0) -> [*]",
            "1: READ(i) -> [*]",
            "COND (i < 10) -> [THEN=*, ELSE=2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "READWRITE(i, null) -> [1]",
            "2: STMT -> [*]",
            "READ(j) -> [*]",
            "GOTO -> [*]",
            "END");
  }

  public void testEmptyForStatement() throws Exception {
    assertCfg("void",
        "for (;;) { j++; }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "1: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(j, null) -> [1]",
            "END");
  }

  public void testThrowWithoutCatch1() throws Exception {
    assertCfg("void", "throw runtimeException;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(runtimeException) -> [*]",
        "THROW -> [*]",
        "END");
  }

  public void testThrowWithoutCatch2() throws Exception {
    assertCfg("void", "if (b1) { throw runtimeException; } b1 = true;").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [THEN=*, ELSE=1]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(runtimeException) -> [*]",
        "THROW -> [2]",
        "1: STMT -> [*]",
        "WRITE(b1, true) -> [*]",
        "2: END"
        );
  }

  public void testWhileContinueNoLabel() throws Exception {
    assertCfg("void", "while (b1) { if (b2) { continue; } i++; }").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "1: READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [THEN=*, ELSE=3]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b2) -> [*]",
        "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "GOTO -> [1]",
        "2: STMT -> [*]",
        "READWRITE(i, null) -> [1]",
        "3: END");
  }

  public void testWhileContinueWithLabel1() throws Exception {
    assertCfg("void",
        "nextLoop: while(b3)",
        "  while (b1) {",
        "    if (b2) { continue; }",
        "    i++;" +
        "  }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "1: READ(b3) -> [*]",
            "COND (EntryPoint.b3) -> [THEN=*, ELSE=4]",
            "STMT -> [*]",
            "2: READ(b1) -> [*]",
            "COND (EntryPoint.b1) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
           "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=3]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [2]",
            "3: STMT -> [*]",
            "READWRITE(i, null) -> [2]",
            "4: END");
  }


  public void testWhileContinueWithLabel2() throws Exception {
    assertCfg("void",
        "nextLoop: while(b3)",
        "  while (b1) {",
        "    if (b2) { continue nextLoop; }",
        "    i++;",
        "  }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "1: READ(b3) -> [*]",
            "COND (EntryPoint.b3) -> [THEN=*, ELSE=4]",
            "STMT -> [*]",
            "2: READ(b1) -> [*]",
            "COND (EntryPoint.b1) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=3]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [1]",
            "3: STMT -> [*]",
            "READWRITE(i, null) -> [2]",
            "4: END");
  }

  public void testWhileContinueWithLabel3() throws Exception {
    assertCfg("void",
        "nextLoop: while (b1) {",
        "  if (b2) { continue; }",
        "  i++;",
        "}").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "1: READ(b1) -> [*]",
            "COND (EntryPoint.b1) -> [THEN=*, ELSE=3]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [1]",
            "2: STMT -> [*]",
            "READWRITE(i, null) -> [1]",
            "3: END");
  }

  public void testWhileBreakNoLabel() throws Exception {
    assertCfg("void", "while (b1) { if (b2) { break; } i++; }").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "1: READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [THEN=*, ELSE=3]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b2) -> [*]",
        "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "GOTO -> [3]",
        "2: STMT -> [*]",
        "READWRITE(i, null) -> [1]",
        "3: END");
  }

  public void testWhileBreakNoLabel2() throws Exception {
    assertCfg("void", "while (b1) { if (b2) { break; } else { while (i < 10) { i++; } } }").is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "1: READ(b1) -> [*]",
        "COND (EntryPoint.b1) -> [THEN=*, ELSE=4]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b2) -> [*]",
        "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "GOTO -> [4]",
        "2: BLOCK -> [*]",
        "STMT -> [*]",
        "3: READ(i) -> [*]",
        "COND (EntryPoint.i < 10) -> [THEN=*, ELSE=1]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READWRITE(i, null) -> [3]",
        "4: END");
  }

  public void testWhileBreakWithLabel1() throws Exception {
    assertCfg("void",
        "nextLoop: while(b3)",
        "  while (b1) {",
        "    if (b2) { break; }",
        "    i++;",
        "  }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "1: READ(b3) -> [*]",
            "COND (EntryPoint.b3) -> [THEN=*, ELSE=4]",
            "STMT -> [*]",
            "2: READ(b1) -> [*]",
            "COND (EntryPoint.b1) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=3]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [1]",
            "3: STMT -> [*]",
            "READWRITE(i, null) -> [2]",
            "4: END");
  }

  public void testWhileBreakWithLabel2() throws Exception {
    assertCfg("void",
        "nextLoop: while(b3)",
        "  while (b1) {",
        "    if (b2) { break nextLoop; }",
        "    i++;",
        "  }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "1: READ(b3) -> [*]",
            "COND (EntryPoint.b3) -> [THEN=*, ELSE=4]",
            "STMT -> [*]",
            "2: READ(b1) -> [*]",
            "COND (EntryPoint.b1) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=3]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [4]",
            "3: STMT -> [*]",
            "READWRITE(i, null) -> [2]",
            "4: END");
  }

  public void testWhileBreakWithLabel3() throws Exception {
    assertCfg("void",
        "nextLoop: while (b1) { if (b2) { break; } i++; }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "1: READ(b1) -> [*]",
            "COND (EntryPoint.b1) -> [THEN=*, ELSE=3]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [3]",
            "2: STMT -> [*]",
            "READWRITE(i, null) -> [1]",
            "3: END");
  }

  public void testForBreakNoLabel() throws Exception {
    assertCfg("void",
        "for(int i = 0; i < 10; i++) { if (b2) { break; } i++; }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "STMT -> [*]",
            "WRITE(i, 0) -> [*]",
            "1: READ(i) -> [*]",
            "COND (i < 10) -> [THEN=*, ELSE=3]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [3]",
            "2: STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "READWRITE(i, null) -> [1]",
            "3: END");
  }

  public void testForContinueNoLabel() throws Exception {
    assertCfg("void",
        "for(int i = 0; i < 10; i++) { if (b2) { continue; } i++; }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "STMT -> [*]",
            "WRITE(i, 0) -> [*]",
            "1: READ(i) -> [*]",
            "COND (i < 10) -> [THEN=*, ELSE=4]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [3]",
            "2: STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "3: READWRITE(i, null) -> [1]",
            "4: END");
  }

  public void testForBreakNestedForWithLabel() throws Exception {
    assertCfg("int",
        "int j = 0; a: for(; ; ) { if (b2) { break a; } else { for (int i = 0; i < 1; i++) { j = i; } } } return j;").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(j, 0) -> [*]",
            "STMT -> [*]",
            "1: BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [4]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "STMT -> [*]",
            "WRITE(i, 0) -> [*]",
            "3: READ(i) -> [*]",
            "COND (i < 1) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "WRITE(j, i) -> [*]",
            "READWRITE(i, null) -> [3]",
            "4: STMT -> [*]",
            "READ(j) -> [*]",
            "GOTO -> [*]",
            "END");
  }

  public void testForBreakNestedForNoLabel() throws Exception {
    assertCfg("int",
        "int j = 0; for(; ; ) { if (b2) { break; } else { for (int i = 0; i < 1; i++) { j = i; } } } return j;").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(j, 0) -> [*]",
            "STMT -> [*]",
            "1: BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b2) -> [*]",
            "COND (EntryPoint.b2) -> [THEN=*, ELSE=2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [4]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "STMT -> [*]",
            "WRITE(i, 0) -> [*]",
            "3: READ(i) -> [*]",
            "COND (i < 1) -> [THEN=*, ELSE=1]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "WRITE(j, i) -> [*]",
            "READWRITE(i, null) -> [3]",
            "4: STMT -> [*]",
            "READ(j) -> [*]",
            "GOTO -> [*]",
            "END");
  }

  public void testCatchThrowException1() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throw checkedException;",
        "  k++;",
        "} catch (CheckedException e) {",
        "  i++;",
        "}",
        "j++;").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "READ(checkedException) -> [*]",
            "THROW -> [2]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [3]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "3: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "END"
        );
  }

  /**
   * Test case for issue 8115 (http://code.google.com/p/google-web-toolkit/issues/detail?id=8115)
   * @throws Exception
   */
  public void testCatchThrowExceptionFinally() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throw uncheckedException2;",
        "  k++;",
        "} catch (UncheckedException1 e) {",
        "  throw e;",
        "} finally {",
        "  j++;",
        "}").is(
        "BLOCK -> [*]",
        "TRY -> [*]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(b) -> [*]",
        "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
        "STMT -> [*]",
        "READ(uncheckedException2) -> [*]",
        "THROW -> [2]",
        "1: STMT -> [*]",
        "READWRITE(k, null) -> [2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(e) -> [*]",
        "THROW -> [*]",
        "2: BLOCK -> [*]",
        "STMT -> [*]",
        "READWRITE(j, null) -> [*, *, *]",
        "END"
    );
  }


  public void testCatchThrowUncaughtException() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throw uncheckedException2;",
        "  k++;",
        "} catch (UncheckedException1 e) {",
        "  i++;",
        "}",
        "j++;").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "READ(uncheckedException2) -> [*]",
            "THROW -> [3]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "2: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "3: END"
        );
  }

  public void testCatchThrowSupertype() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throw runtimeException;",
        "  k++;",
        "} catch (UncheckedException1 e) {",
        "  i++;",
        "}",
        "j++;").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "READ(runtimeException) -> [*]",
            "THROW -> [2, 4]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [3]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "3: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "4: END"
        );
  }

  public void testCatchSupertype() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throw uncheckedException1;",
        "  k++;",
        "} catch (RuntimeException e) {",
        "  i++;",
        "}",
        "j++;").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "READ(uncheckedException1) -> [*]",
            "THROW -> [2]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [3]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "3: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "END"
        );
  }

  public void testCatchReturn() throws Exception {
    assertCfg("void",
        "try { try {",
        "  if (b) return;",
        "  k++;",
        "} catch (RuntimeException e) {",
        "  i++;",
        "} } catch (UncheckedException1 e) { j++; }").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "GOTO -> [2]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "2: END"
        );
  }

  public void testRethrow() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throw uncheckedException1;",
        "  k++;",
        "} catch (UncheckedException1 e) {",
        "  i++;",
        "  throw e;",
        "}",
        "j++;").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "READ(uncheckedException1) -> [*]",
            "THROW -> [2]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [3]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "STMT -> [*]",
            "READ(e) -> [*]",
            "THROW -> [4]",
            "3: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "4: END"
        );
  }

  public void testCatchMethodCall1() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throwCheckedException();",
        "  k++;",
        "} catch (CheckedException e) {",
        "  i++;",
        "}",
        "j++;").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "OPTTHROW(throwCheckedException()) -> [NOTHROW=*, 2, RE=4, E=4]",
            "CALL(throwCheckedException) -> [*]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [3]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "3: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "4: END"
        );
  }

  public void testCatchMethodCall2() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throwCheckedException();",
        "  k++;",
        "} catch (CheckedException e) {",
        "  i++;",
        "} catch (RuntimeException e) {",
        "  l++;",
        "}",
        "j++;").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "OPTTHROW(throwCheckedException()) -> [NOTHROW=*, 2, RE=3, E=5]",
            "CALL(throwCheckedException) -> [*]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [4]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [4]",
            "3: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(l, null) -> [*]",
            "4: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "5: END"
        );
  }

  public void testCatchMethodCallUnchecked() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throwUncheckedException();",
        "  k++;",
        "} catch (UncheckedException1 e) {",
        "  i++;",
        "} catch (RuntimeException e) {",
        "  l++;",
        "}",
        "j++;").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "OPTTHROW(throwUncheckedException()) -> [NOTHROW=*, RE=2, RE=3, E=5]",
            "CALL(throwUncheckedException) -> [*]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [4]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [4]",
            "3: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(l, null) -> [*]",
            "4: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "5: END"
        );
  }

  public void testFinallyReturn1() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) return;",
        "  j++;",
        "} finally {",
        "  i++;",
        "}").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "GOTO -> [2]",
            "1: STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*, *]",
            "END"
        );
  }

  public void testThrowFromFinally() throws Exception {
    assertCfg("void",
        "try {",
        "  return;",
        "} finally {",
        "  throw runtimeException;",
        "}").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(runtimeException) -> [*]",
            "THROW -> [*]",
            "END"
        );
  }

  public void testFinallyReturn2() throws Exception {
    assertCfg("void",
        "try {",
        "  return;",
        "} finally {",
        "  i++;",
        "}").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*, *]",
            "END"
        );
  }

  public void testFinallyReturn3() throws Exception {
    assertCfg("void",
        "try {",
        "try {",
        "  if (b) return;",
        "  k++;",
        "} finally {",
        "  i++;",
        "} m++; } finally {",
        "  j++;",
        "}").is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "GOTO -> [2]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [*]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*, 3]",
            "STMT -> [*]",
            "READWRITE(m, null) -> [*]",
            "3: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(j, null) -> [*, *]",
            "END"
        );
  }


  public void testFinallyContinue() throws Exception {
    assertCfg("void",
        "while (b) {",
        "try {",
        "  if (b) continue;",
        "} finally {",
        "  i++;",
        "} j++; }").is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "1: READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=3]",
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=2]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*, 1]",
            "STMT -> [*]",
            "READWRITE(j, null) -> [1]",
            "3: END"
        );
  }

  public void testCatchFinally() throws Exception {
    assertCfg("void",
        "try {",
        "  if (b) throw checkedException;",
        "  k++;",
        "} catch (CheckedException e) {",
        "  i++;",
        "} finally {",
        "  j++;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "TRY -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "READ(checkedException) -> [*]",
            "THROW -> [2]",
            "1: STMT -> [*]",
            "READWRITE(k, null) -> [3]",
            "2: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(i, null) -> [*]",
            "3: BLOCK -> [*]",
            "STMT -> [*]",
            "READWRITE(j, null) -> [*]",
            "END"
        );
  }

  public void testFieldWrite() throws Exception {
    assertCfg("void",
        "Foo foo = createFoo();",
        "foo.i = 1;"
        ).is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "OPTTHROW(createFoo()) -> [NOTHROW=*, RE=1, E=1]",
        "CALL(createFoo) -> [*]",
        "WRITE(foo, EntryPoint.createFoo()) -> [*]",
        "STMT -> [*]",
        "READ(foo) -> [*]",
        "WRITE(i, 1) -> [*]",
        "1: END"
    );
  }

  public void testFieldUnary() throws Exception {
    assertCfg("void",
        "Foo foo = createFoo();",
        "++foo.i;"
        ).is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "OPTTHROW(createFoo()) -> [NOTHROW=*, RE=1, E=1]",
        "CALL(createFoo) -> [*]",
        "WRITE(foo, EntryPoint.createFoo()) -> [*]",
        "STMT -> [*]",
        "READ(foo) -> [*]",
        "READWRITE(i, null) -> [*]",
        "1: END"
    );
  }

  public void testArrayRead() throws Exception {
    assertCfg("void",
        "i = ii[j];"
        ).is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(ii) -> [*]",
        "READ(j) -> [*]",
        "WRITE(i, EntryPoint.ii[EntryPoint.j]) -> [*]",
        "END"
    );
  }

  public void testArrayWrite() throws Exception {
    assertCfg("void",
        "ii[i] = j;"
        ).is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(j) -> [*]",
        "READ(ii) -> [*]",
        "READ(i) -> [*]",
        "WRITE(EntryPoint.ii[EntryPoint.i], EntryPoint.j) -> [*]",
        "END"
    );
  }

  public void testArrayUnary() throws Exception {
    assertCfg("void",
        "++ii[i];"
        ).is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(ii) -> [*]",
        "READ(i) -> [*]",
        "READWRITE(EntryPoint.ii[EntryPoint.i], null) -> [*]",
        "END"
    );
  }

  public void testSwitch() throws Exception {
    assertCfg("void",
        "switch(i) {",
        "  case 1: ",
        "    return;",
        "  case 2: ",
        "  case 3: ",
        "    j = 1;",
        "    break;",
        "  case 4: ",
        "    j = 2;",
        "  default:",
        "    j = 4;",
        "  case 5: ",
        "    j = 3;",
        "    break;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "GOTO -> [*]",
            "STMT -> [*]",
            "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "GOTO -> [7]",
            "1: STMT -> [*]",
            "COND (EntryPoint.i == 2) -> [ELSE=*, THEN=2]",
            "STMT -> [*]",
            "COND (EntryPoint.i == 3) -> [THEN=*, ELSE=3]",
            "2: STMT -> [*]",
            "WRITE(j, 1) -> [*]",
            "STMT -> [*]",
            "GOTO -> [7]",
            "3: STMT -> [*]",
            "COND (EntryPoint.i == 4) -> [THEN=*, ELSE=5]",
            "STMT -> [*]",
            "WRITE(j, 2) -> [*]",
            "4: STMT -> [*]",
            "STMT -> [*]",
            "WRITE(j, 4) -> [6]",
            "5: STMT -> [*]",
            "COND (EntryPoint.i == 5) -> [THEN=*, ELSE=4]",
            "6: STMT -> [*]",
            "WRITE(j, 3) -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "7: END"
    );
  }

  public void testSwitch_FallThrough() throws Exception {
    assertCfg("void",
        "switch(i) {",
        "  case 1: ",
        "    j = 1;",
        "  case 2: ",
        "    j = 2;",
        "  case 3: ",
        "    j = 3;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "GOTO -> [*]",
            "STMT -> [*]",
            "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "WRITE(j, 1) -> [2]",
            "1: STMT -> [*]",
            "COND (EntryPoint.i == 2) -> [THEN=*, ELSE=3]",
            "2: STMT -> [*]",
            "WRITE(j, 2) -> [4]",
            "3: STMT -> [*]",
            "COND (EntryPoint.i == 3) -> [THEN=*, ELSE=5]",
            "4: STMT -> [*]",
            "WRITE(j, 3) -> [*]",
            "5: END");
  }

  public void testSwitch_FirstDefault() throws Exception {
    assertCfg("void",
        "switch(i) {",
        "  default: j = 1; return;",
        "  case 1: j = 2; return;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "GOTO -> [2]",
            "1: STMT -> [*]",
            "STMT -> [*]",
            "WRITE(j, 1) -> [*]",
            "STMT -> [*]",
            "GOTO -> [3]",
            "2: STMT -> [*]",
            "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "WRITE(j, 2) -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "3: END"
    );
  }

  public void testSwitch_Empty() throws Exception {
    assertCfg("void",
        "switch(i) {",
        "}"
        ).is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "GOTO -> [*]",
            "END"
    );
  }

  public void testSwitch_OnlyDefault() throws Exception {
    assertCfg("void",
        "switch(i) {",
        "  default: j = 0; return;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "GOTO -> [*]",
            "STMT -> [*]",
            "STMT -> [*]",
            "WRITE(j, 0) -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "END"
    );
  }
  public void testNestedSwitch() throws Exception {
    assertCfg("void",
        "switch(i) {",
        "  case 1: ",
        "    switch (j) {",
        "      case 0: k = 1; break;",
        "      case 1: k = 2; break;",
        "    }",
        "    break;",
        "  case 2: ",
        "    switch (j) {",
        "      case 0: k = 3; break;",
        "      case 1: k = 4; break;",
        "    }",
        "    break;",
        "  case 3: ",
        "    switch (j) {",
        "      case 0: k = 5; break;",
        "      case 1: k = 6; break;",
        "    }",
        "    break;",
        "}"
        ).is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(i) -> [*]",
        "GOTO -> [*]",
        "STMT -> [*]",
        "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=3]",
        "STMT -> [*]",
        "READ(j) -> [*]",
        "GOTO -> [*]",
        "STMT -> [*]",
        "COND (EntryPoint.j == 0) -> [THEN=*, ELSE=1]",
        "STMT -> [*]",
        "WRITE(k, 1) -> [*]",
        "STMT -> [*]",
        "GOTO -> [2]",
        "1: STMT -> [*]",
        "COND (EntryPoint.j == 1) -> [THEN=*, ELSE=2]",
        "STMT -> [*]",
        "WRITE(k, 2) -> [*]",
        "STMT -> [*]",
        "GOTO -> [*]",
        "2: STMT -> [*]",
        "GOTO -> [9]",
        "3: STMT -> [*]",
        "COND (EntryPoint.i == 2) -> [THEN=*, ELSE=6]",
        "STMT -> [*]",
        "READ(j) -> [*]",
        "GOTO -> [*]",
        "STMT -> [*]",
        "COND (EntryPoint.j == 0) -> [THEN=*, ELSE=4]",
        "STMT -> [*]",
        "WRITE(k, 3) -> [*]",
        "STMT -> [*]",
        "GOTO -> [5]",
        "4: STMT -> [*]",
        "COND (EntryPoint.j == 1) -> [THEN=*, ELSE=5]",
        "STMT -> [*]",
        "WRITE(k, 4) -> [*]",
        "STMT -> [*]",
        "GOTO -> [*]",
        "5: STMT -> [*]",
        "GOTO -> [9]",
        "6: STMT -> [*]",
        "COND (EntryPoint.i == 3) -> [THEN=*, ELSE=9]",
        "STMT -> [*]",
        "READ(j) -> [*]",
        "GOTO -> [*]",
        "STMT -> [*]",
        "COND (EntryPoint.j == 0) -> [THEN=*, ELSE=7]",
        "STMT -> [*]",
        "WRITE(k, 5) -> [*]",
        "STMT -> [*]",
        "GOTO -> [8]",
        "7: STMT -> [*]",
        "COND (EntryPoint.j == 1) -> [THEN=*, ELSE=8]",
        "STMT -> [*]",
        "WRITE(k, 6) -> [*]",
        "STMT -> [*]",
        "GOTO -> [*]",
        "8: STMT -> [*]",
        "GOTO -> [*]",
        "9: END"
    );
  }

  public void testSwitchWithLoopAndBreak() throws Exception {
    assertCfg("void",
        "switch(i) {",
        "  case 1: ",
        "    i = 1;" +
        "    break;",
        "  case 2: ",
        "    while (b) { i = 2; break; }",
        "    j = 3;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "READ(i) -> [*]",
            "GOTO -> [*]",
            "STMT -> [*]",
            "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "WRITE(i, 1) -> [*]",
            "STMT -> [*]",
            "GOTO -> [3]",
            "1: STMT -> [*]",
            "COND (EntryPoint.i == 2) -> [THEN=*, ELSE=3]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=2]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(i, 2) -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "2: STMT -> [*]",
            "WRITE(j, 3) -> [*]",
            "3: END"
    );
  }

  public void testBreakStatement1() throws Exception {
    assertCfg("void",
        "lbl: {",
        "  break lbl;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "END");
  }

  public void testBreakStatement2() throws Exception {
    assertCfg("void",
        "lbl: break lbl;"
        ).is(
            "BLOCK -> [*]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "END");
  }

  public void testBreakStatement3() throws Exception {
    assertCfg("void",
        "lbl: {",
        "  i = 1;",
        "  if (b) break lbl;",
        "  i = 2;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(i, 1) -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "GOTO -> [2]",
            "1: STMT -> [*]",
            "WRITE(i, 2) -> [*]",
            "2: END");
  }

  public void testBreakStatement4() throws Exception {
    assertCfg("void",
        "lbl1: {",
        "  i = 1;",
        "  lbl2: {",
        "    j = 1;",
        "    if (b) break lbl1;",
        "    j = 2;",
        "    if (b) break lbl2;",
        "  }",
        "  i = 2;",
        "}"
        ).is(
            "BLOCK -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(i, 1) -> [*]",
            "BLOCK -> [*]",
            "STMT -> [*]",
            "WRITE(j, 1) -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=1]",
            "STMT -> [*]",
            "GOTO -> [3]",
            "1: STMT -> [*]",
            "WRITE(j, 2) -> [*]",
            "STMT -> [*]",
            "READ(b) -> [*]",
            "COND (EntryPoint.b) -> [THEN=*, ELSE=2]",
            "STMT -> [*]",
            "GOTO -> [*]",
            "2: STMT -> [*]",
            "WRITE(i, 2) -> [*]",
            "3: END");
  }

  public void testBreakLoopAndSwitch() throws Exception {
    assertCfg("void",
        "loop: while (b) {",
        "  switch (i) {",
        "    case 1: ",
        "      if (j == 1) {",
        "        break loop;",
        "      }",
        "      break;",
        "    default: ",
        "      return;",
        "    case 2: ",
        "      break loop;",
        "    case 3: ",
        "      break;",
        "  }",
        "  i++;",
        "}",
        "k++;"
    ).is(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "1: READ(b) -> [*]",
        "COND (EntryPoint.b) -> [THEN=*, ELSE=7]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "READ(i) -> [*]",
        "GOTO -> [*]",
        "STMT -> [*]",
        "COND (EntryPoint.i == 1) -> [THEN=*, ELSE=4]",
        "STMT -> [*]",
        "READ(j) -> [*]",
        "COND (EntryPoint.j == 1) -> [THEN=*, ELSE=2]",
        "BLOCK -> [*]",
        "STMT -> [*]",
        "GOTO -> [7]",
        "2: STMT -> [*]",
        "GOTO -> [6]",
        "3: STMT -> [*]",
        "STMT -> [*]",
        "GOTO -> [8]",
        "4: STMT -> [*]",
        "COND (EntryPoint.i == 2) -> [THEN=*, ELSE=5]",
        "STMT -> [*]",
        "GOTO -> [7]",
        "5: STMT -> [*]",
        "COND (EntryPoint.i == 3) -> [THEN=*, ELSE=3]",
        "STMT -> [*]",
        "GOTO -> [*]",
        "6: STMT -> [*]",
        "READWRITE(i, null) -> [1]",
        "7: STMT -> [*]",
        "READWRITE(k, null) -> [*]",
        "8: END");
  }

  private CfgBuilderResult assertCfg(String returnType, String ...codeSnippet)
      throws UnableToCompleteException {
    JProgram program = compileSnippet(returnType, Joiner.on("\n").join(codeSnippet));
    JMethodBody body = (JMethodBody) findMainMethod(program).getBody();
    Cfg cfgGraph = CfgBuilder.build(program, body.getBlock());
    return new CfgBuilderResult(cfgGraph);
  }

  static class CfgBuilderResult {
    private final Cfg cfg;

    public CfgBuilderResult(Cfg cfgGraph) {
      assertNotNull("Can't build cfg", cfgGraph);
      this.cfg = cfgGraph;

      validateGraph();
    }

    private void validateGraph() {
      for (CfgNode<?> node : cfg.getNodes()) {
        List<CfgEdge> incomingEdges = cfg.getInEdges(node);
        for (CfgEdge e : incomingEdges) {
          CfgNode<?> start = e.getStart();
          if (cfg.getGraphInEdges().contains(e)) {
            assertNull(start);
            continue;
          }
          assertNotNull("No start in edge " + e.getRole() + " to " + node,
              start);
          assertTrue(start + " doesn't have outgoing edge to " + node,
              cfg.getOutEdges(start).contains(e));
        }

        List<CfgEdge> outcomingEdges = cfg.getOutEdges(node);
        for (CfgEdge e : outcomingEdges) {
          CfgNode<?> end = e.getEnd();
          assertNotNull("No end in edge " + e.getRole() + " from " + node, end);
          assertTrue(end + " doesn't have incoming edge from " + node,
              cfg.getInEdges(end).contains(e));
        }
      }
    }

    public void is(String... expected) {
      assertEquals(Joiner.on("\n").join(expected), cfg.print());
    }
  }
}
