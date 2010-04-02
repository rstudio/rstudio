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

import com.google.gwt.dev.jjs.impl.gflow.CfgIntegratedAnalysisTestBase;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedAnalysis;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;

/**
 * 
 */
public class LivenessTransformationTest extends CfgIntegratedAnalysisTestBase<LivenessAssumption> {
  @Override
  protected void setUp() throws Exception {
    forward = false;
    super.setUp();
  }

  public void testLinearStatements() throws Exception {
    transform("void", "int i = 1; int j = 2;").into(
        "int i;",
        "int j;");
  }
  
  public void testUsage() throws Exception {
    transform("void", "int i = 1; int j = i;").into(
        "int i = 1;",
        "int j;");

    transform("int", "int i = 1; int j = 2; return i;").into(
        "int i = 1;",
        "int j;",
        "return i;");
  }
  
  public void testDeadAssignments() throws Exception {
    transform("void", "int i = 1; i = 2; i = 3; int j = i;").into(
        "int i;",
        "i = 3;",
        "int j;");
  }
  
  public void testSomeDeadAssignment() throws Exception {
    transform("int", "int i = 1; i = 2; i = 3; int j = i; return j;").into(
        "int i;",
        "i = 3;",
        "int j = i;",
        "return j;");
  }
  
  public void testFieldAssignment() throws Exception {
    addSnippetClassDecl("static int i;");
    
    transform("void", "i = 1;").into("EntryPoint.i = 1;");
  }

  public void testSideEffects() throws Exception {
    addSnippetClassDecl("static int foo() { return 0; };");
    
    transform("void", "int i = foo();").into(
        "EntryPoint.foo();",
        "int i;");
  }

  public void testMultiAssignments() throws Exception {
    transform("void", "int i = 1, j = 1; i = j = 2;").into(
        "int i;",
        "int j;");
  }
  
  @Override
  protected IntegratedAnalysis<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, LivenessAssumption> createIntegratedAnalysis() {
    return new LivenessAnalysis();
  }
}
