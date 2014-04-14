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
package com.google.gwt.dev.jjs.impl.gflow.liveness;

import com.google.gwt.dev.jjs.impl.gflow.Analysis;
import com.google.gwt.dev.jjs.impl.gflow.CfgAnalysisTestBase;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;

/**
 *
 */
public class LivenessAnalysisTest extends CfgAnalysisTestBase<LivenessAssumption> {
  @Override
  protected void setUp() throws Exception {
    forward = false;
    super.setUp();
  }

  public void testSingleStatement() throws Exception {
    analyze("void", "int i = 1;").into(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(i, 1) -> [*]",
        "END");
  }

  public void testMultipleLinearStatements() throws Exception {
    analyze("int", "int i = 1; int j = 2; int k = i * j; i = 3; j = 4; return i;").into(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(i, 1) -> [* {i}]",
        "STMT -> [* {i}]",
        "WRITE(j, 2) -> [* {i, j}]",
        "STMT -> [* {i, j}]",
        "READ(i) -> [* {j}]",
        "READ(j) -> [*]",
        "WRITE(k, i * j) -> [*]",
        "STMT -> [*]",
        "WRITE(i, 3) -> [* {i}]",
        "STMT -> [* {i}]",
        "WRITE(j, 4) -> [* {i}]",
        "STMT -> [* {i}]",
        "READ(i) -> [*]",
        "GOTO -> [*]",
        "END");
  }

  public void testNonAsignmentWriteTest() throws Exception {
    analyze("int", "int i = 1; i += 2; return i;").into(
        "BLOCK -> [*]",
        "STMT -> [*]",
        "WRITE(i, 1) -> [* {i}]",
        "STMT -> [* {i}]",
        "READWRITE(i, null) -> [* {i}]",
        "STMT -> [* {i}]",
        "READ(i) -> [*]",
        "GOTO -> [*]",
        "END");
  }

  @Override
  protected Analysis<CfgNode<?>, CfgEdge, Cfg, LivenessAssumption> createAnalysis() {
    return new LivenessAnalysis();
  }
}
