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
package com.google.gwt.dev.jjs.impl.gflow.copy;

import com.google.gwt.dev.jjs.impl.gflow.CfgIntegratedAnalysisTestBase;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedAnalysis;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;

/**
 *
 */
public class CopyAnalysisTransformationTest extends CfgIntegratedAnalysisTestBase<CopyAssumption> {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addSnippetClassDecl("static boolean b;");
  }

  public void testLinearStatements() throws Exception {
    transform("void", "int i = 1; int j = i; int k = j;").into(
        "int i = 1;",
        "int j = i;",
        "int k = i;");
  }

  public void testIf() throws Exception {
    transform("void", "int i = 1; int j = i; if (b) { j = 1; } int k = j;").into(
        "int i = 1;",
        "int j = i;",
        "if (EntryPoint.b) {",
        "  j = 1;",
        "}",
        "int k = j;");
  }

  public void testRecursion() throws Exception {
    transform("int", "int i = 1; i = i; return i;").into(
        "int i = 1;",
        "i = i;",
        "return i;");
    transform("int", "int i = 1; int j = 1; j = i; i = j; return i;").into(
        "int i = 1;",
        "int j = 1;",
        "j = i;",
        "i = i;",
        "return i;");
  }

  public void testImplicitConversion() throws Exception {
    transform("long",
        "int bar = 0x12345678;",
        "bar = bar * 1234;",
        "long lng = bar;",
        "long lng8 = lng << 8;",
        "return lng8;"
        ).into(
            "  int bar = 305419896;",
            "  bar = bar * 1234;",
            "  long lng = bar;",
            "  long lng8 = lng << 8;",
            "  return lng8;");
  }

  @Override
  protected IntegratedAnalysis<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, CopyAssumption> createIntegratedAnalysis() {
    return new CopyAnalysis();
  }
}
