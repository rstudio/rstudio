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

import com.google.gwt.dev.jjs.impl.gflow.Analysis;
import com.google.gwt.dev.jjs.impl.gflow.CfgAnalysisTestBase;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;

public class ConstantsAnalysisTest extends CfgAnalysisTestBase<ConstantsAssumption> {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addSnippetClassDecl("static int i;");
    addSnippetClassDecl("static int j;");
    addSnippetClassDecl("static int k;");
    addSnippetClassDecl("static int l;");
    addSnippetClassDecl("static int m;");

    addSnippetClassDecl("static int foo() { return 0; };");
    addSnippetClassDecl("static void bar(Object o) { };");
    addSnippetClassDecl("static int baz(Object o) { return 0; };");
  }

  public void testDeclWithConstInit() throws Exception {
    analyze("void", "int i = 1;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* {i = 1}]",
        "END");
  }

  public void testDeclWithConstOps() throws Exception {
    analyze("void", "int i = 1 + 1;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 2) -> [* {i = 2}]",
        "END");
  }

  public void testDeclWithNonconstInit() throws Exception {
    analyze("void", "int i = foo();").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "OPTTHROW(foo()) -> [NOTHROW=* T, RE=1 T, E=1 T]",
        "CALL(foo) -> [* T]",
        "WRITE(i, EntryPoint.foo()) -> [* T]",
        "1: END");
  }

  public void testReassign() throws Exception {
    analyze("void", "int i = 1; i = 2;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* {i = 1}]",
        "STMT -> [* {i = 1}]",
        "WRITE(i, 2) -> [* {i = 2}]",
        "END");
    analyze("void", "int i; i = 3;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 3) -> [* {i = 3}]",
        "END");
  }

  public void test2Vars() throws Exception {
    analyze("void", "int i = 1; int j = 2;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* {i = 1}]",
        "STMT -> [* {i = 1}]",
        "WRITE(j, 2) -> [* {i = 1, j = 2}]",
        "END");
  }
  
  public void testSequence() throws Exception {
    analyze("void", "int i = 1; int j = i; int k = j; int l = k;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* {i = 1}]",
        "STMT -> [* {i = 1}]",
        "READ(i) -> [* {i = 1}]",
        "WRITE(j, i) -> [* {i = 1, j = 1}]",
        "STMT -> [* {i = 1, j = 1}]",
        "READ(j) -> [* {i = 1, j = 1}]",
        "WRITE(k, j) -> [* {i = 1, j = 1, k = 1}]",
        "STMT -> [* {i = 1, j = 1, k = 1}]",
        "READ(k) -> [* {i = 1, j = 1, k = 1}]",
        "WRITE(l, k) -> [* {i = 1, j = 1, k = 1, l = 1}]",
        "END");
  }
  
  public void testIfStatement() throws Exception {
    analyze("void", "int i = k; if (i == 1) { int j = i; } else { int j = i; } ").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "READ(k) -> [* T]",
        "WRITE(i, EntryPoint.k) -> [* T]",
        "STMT -> [* T]",
        "READ(i) -> [* T]",
        "COND (i == 1) -> [THEN=* {i = 1}, ELSE=1 T]",
        "BLOCK -> [* {i = 1}]",
        "STMT -> [* {i = 1}]",
        "READ(i) -> [* {i = 1}]",
        "WRITE(j, i) -> [2 {i = 1, j = 1}]",
        "1: BLOCK -> [* T]",
        "STMT -> [* T]",
        "READ(i) -> [* T]",
        "WRITE(j, i) -> [* T]",
        "2: END");

    analyze("int", "int j = 0; if (foo() == 1) j = 1; return j;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(j, 0) -> [* {j = 0}]",
        "STMT -> [* {j = 0}]",
        "OPTTHROW(foo()) -> [NOTHROW=* {j = 0}, RE=2 {j = 0}, E=2 {j = 0}]",
        "CALL(foo) -> [* {j = 0}]",
        "COND (EntryPoint.foo() == 1) -> [THEN=* {j = 0}, ELSE=1 {j = 0}]",
        "STMT -> [* {j = 0}]",
        "WRITE(j, 1) -> [* {j = 1}]",
        "1: STMT -> [* T]",
        "READ(j) -> [* T]",
        "GOTO -> [* T]",
        "2: END");

    analyze("int", "int j = 0; if (foo() == 1) j = foo(); return j;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(j, 0) -> [* {j = 0}]",
        "STMT -> [* {j = 0}]",
        "OPTTHROW(foo()) -> [NOTHROW=* {j = 0}, RE=2 {j = 0}, E=2 {j = 0}]",
        "CALL(foo) -> [* {j = 0}]",
        "COND (EntryPoint.foo() == 1) -> [THEN=* {j = 0}, ELSE=1 {j = 0}]",
        "STMT -> [* {j = 0}]",
        "OPTTHROW(foo()) -> [NOTHROW=* {j = 0}, RE=2 {j = 0}, E=2 {j = 0}]",
        "CALL(foo) -> [* {j = 0}]",
        "WRITE(j, EntryPoint.foo()) -> [* T]",
        "1: STMT -> [* T]",
        "READ(j) -> [* T]",
        "GOTO -> [* T]",
        "2: END");
  }
  
  public void testWhileLoop1() throws Exception {
    analyze("void", "int j = 1; while (j > 0) ++j;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(j, 1) -> [* {j = 1}]",
        "STMT -> [* {j = 1}]",
        "1: READ(j) -> [* T]",
        "COND (j > 0) -> [THEN=* T, ELSE=2 T]",
        "STMT -> [* T]",
        "READWRITE(j, null) -> [1 T]",
        "2: END");
  }
  
  public void testWhileLoop2() throws Exception {
    analyze("void", "int j = 0; while (j > 0) {};").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(j, 0) -> [* {j = 0}]",
        "STMT -> [* {j = 0}]",
        "1: READ(j) -> [* {j = 0}]",
        "COND (j > 0) -> [THEN=* {j = 0}, ELSE=2 {j = 0}]",
        "BLOCK -> [1 {j = 0}]",
        "2: END");
  }
  
  public void testConditionalExpressions() throws Exception {
    analyze("void", "boolean b1 = false; boolean b2 = false; if (b1 && (b2 = true)) b1 = true;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(b1, false) -> [* {b1 = false}]",
        "STMT -> [* {b1 = false}]",
        "WRITE(b2, false) -> [* {b1 = false, b2 = false}]",
        "STMT -> [* {b1 = false, b2 = false}]",
        "READ(b1) -> [* {b1 = false, b2 = false}]",
        "COND (b1) -> [THEN=* {b1 = false, b2 = false}, ELSE=1 {b1 = false, b2 = false}]",
        "WRITE(b2, true) -> [* {b1 = false, b2 = true}]",
        "1: COND (b1 && (b2 = true)) -> [THEN=* {b1 = false}, ELSE=2 {b1 = false}]",
        "STMT -> [* {b1 = false}]",
        "WRITE(b1, true) -> [* {b1 = true}]",
        "2: END");
  }

  // Various real-world stuff
  public void testVariousStuff() throws Exception {
    addSnippetClassDecl("static Object f = null;");
    
    analyze("void", 
        "Object e = null;" +
    		"if (f != null) if (e == null)" +
    		"  return;" +
    		"boolean b = e == null;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(e, null) -> [* {e = null}]",
        "STMT -> [* {e = null}]",
        "READ(f) -> [* {e = null}]",
        "COND (EntryPoint.f != null) -> [THEN=* {e = null}, ELSE=1 {e = null}]",
        "STMT -> [* {e = null}]",
        "READ(e) -> [* {e = null}]",
        "COND (e == null) -> [THEN=* {e = null}, ELSE=1 {e = null}]",
        "STMT -> [* {e = null}]",
        "GOTO -> [2 {e = null}]",
        "1: STMT -> [* {e = null}]",
        "READ(e) -> [* {e = null}]",
        "WRITE(b, e == null) -> [* {b = true, e = null}]",
        "2: END"
        );
  }

  /**
   * Parameters should have an initial assumption of non-constant.
   */
  public void testParamNonConstant() throws Exception {
    analyzeWithParams("void", "int i, int j", "if (j == 0) { i = 0; } j=i; j=0;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "READ(j) -> [* T]",
        "COND (j == 0) -> [THEN=* {j = 0}, ELSE=1 T]",
        "BLOCK -> [* {j = 0}]",
        "STMT -> [* {j = 0}]",
        "WRITE(i, 0) -> [* {i = 0, j = 0}]",
        "1: STMT -> [* T]",
        "READ(i) -> [* T]",
        "WRITE(j, i) -> [* T]",
        "STMT -> [* T]",
        "WRITE(j, 0) -> [* {j = 0}]",
        "END"
      );
  }

  @Override
  protected Analysis<CfgNode<?>, CfgEdge, Cfg, ConstantsAssumption> createAnalysis() {
    return new ConstantsAnalysis();
  }
}
