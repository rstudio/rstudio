package com.google.gwt.dev.jjs.impl.gflow.copy;

import com.google.gwt.dev.jjs.impl.gflow.Analysis;
import com.google.gwt.dev.jjs.impl.gflow.CfgAnalysisTestBase;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;

public class CopyAnalysisTest extends CfgAnalysisTestBase<CopyAssumption> {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addSnippetClassDecl("static boolean b;");
  }

  public void testCopyCreation() throws Exception {
    analyze("void", "int i = 1; int j = i;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* T]",
        "STMT -> [* T]",
        "READ(i) -> [* T]",
        "WRITE(j, i) -> [* {j = i}]",
        "END");
  }

  public void testCopyKill1() throws Exception {
    analyze("void", "int i = 1; int j = i; j = 1;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* T]",
        "STMT -> [* T]",
        "READ(i) -> [* T]",
        "WRITE(j, i) -> [* {j = i}]",
        "STMT -> [* {j = i}]",
        "WRITE(j, 1) -> [* {j = T}]",
        "END");
  }

  public void testCopyKill2() throws Exception {
    analyze("void", "int i = 1; int j = i; i = 2;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* T]",
        "STMT -> [* T]",
        "READ(i) -> [* T]",
        "WRITE(j, i) -> [* {j = i}]",
        "STMT -> [* {j = i}]",
        "WRITE(i, 2) -> [* {i = T, j = T}]",
        "END");
  }
  
  public void testConditionalKill() throws Exception {
    analyze("void", "int i = 1; int j = i; if (b) { j = 1; } int k = j;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* T]",
        "STMT -> [* T]",
        "READ(i) -> [* T]",
        "WRITE(j, i) -> [* {j = i}]",
        "STMT -> [* {j = i}]",
        "READ(b) -> [* {j = i}]",
        "COND (EntryPoint.b) -> [THEN=* {j = i}, ELSE=1 {j = i}]",
        "BLOCK -> [* {j = i}]",
        "STMT -> [* {j = i}]",
        "WRITE(j, 1) -> [* {j = T}]",
        "1: STMT -> [* {j = T}]",
        "READ(j) -> [* {j = T}]",
        "WRITE(k, j) -> [* {j = T, k = j}]",
        "END");
  }

  public void testRecursion() throws Exception {
    analyze("void", "int i = 1; int j = 1; j = i; i = j;").into(
        "BLOCK -> [* T]",
        "STMT -> [* T]",
        "WRITE(i, 1) -> [* T]",
        "STMT -> [* T]",
        "WRITE(j, 1) -> [* T]",
        "STMT -> [* T]",
        "READ(i) -> [* T]",
        "WRITE(j, i) -> [* {j = i}]",
        "STMT -> [* {j = i}]",
        "READ(j) -> [* {j = i}]",
        "WRITE(i, j) -> [* {j = i}]",
        "END");
  }

  @Override
  protected Analysis<CfgNode<?>, CfgEdge, Cfg, CopyAssumption> createAnalysis() {
    return new CopyAnalysis();
  }
}
