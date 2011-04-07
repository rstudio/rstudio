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

import com.google.gwt.dev.jjs.impl.gflow.CfgIntegratedAnalysisTestBase;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedAnalysis;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;

/**
 * 
 */
public class ConstantsAnalysisTransformationTest extends CfgIntegratedAnalysisTestBase<ConstantsAssumption> {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    runDeadCodeElimination = true;
    addSnippetClassDecl("static int i_;");
    addSnippetClassDecl("static int foo() { return 0; }");
    addSnippetClassDecl("static int bar(int i) {return 0;}");
    addSnippetClassDecl("static String baz() {return null;}");
  }

  public void testLinearStatements() throws Exception {
    transform("void", "int i = 1; int j = i;").into(
        "int i = 1;",
        "int j = 1;");
    transform("void", "int i = 1; int j = i; i = 2; j = i;").into(
        "int i = 1;",
        "int j = 1;",
        "i = 2;",
        "j = 2;");
    transform("void", "int i = 1; i = i + 1; int j = i;").into(
        "int i = 1;",
        "i = 1 + 1;",
        "int j = 2;");
  }
  
  public void testSequence() throws Exception {
    transform("void", "int i = 1; int j = i; int k = j;").into(
        "int i = 1;",
        "int j = 1;",
        "int k = 1;");
  }
  
  public void testIfStatement() throws Exception {
    transform("void", "int i = 1; if (i_ == i) { i = 2; int j = i;} ").into(
        "int i = 1;",
        "if (EntryPoint.i_ == 1) {",
        "  i = 2;",
        "  int j = 2;",
        "}");
    transform("void", "int i = foo(); if (i == 1) { int j = i; } else { int j = i; } ").into(
        "int i = EntryPoint.foo();",
        "if (i == 1) {",
        "  int j = 1;",
        "} else {",
        "  int j = i;",
        "}");
  }
  
  public void testReplaceInMethodCall() throws Exception {
    transform("void", "int i = 1; bar(i);").into(
        "int i = 1;",
        "EntryPoint.bar(1);");
  }
  
  public void testExpressionEvaluation() throws Exception {
    transform("void", "int i = 1; int j = i + 1;").into(
        "int i = 1;",
        "int j = 1 + 1;");
    transform("void", "int i = 1; int j = i - 1;").into(
        "int i = 1;",
        "int j = 1 - 1;");
    transform("void", "int i = 1; boolean b = i == 1;").into(
        "int i = 1;",
        "boolean b = 1 == 1;");
  }
  
  public void testWhile() throws Exception {
    transform("void", "int j = 0; while (j > 0) { }").intoString(
        "int j = 0;");

  }

  public void testConstantCondition() throws Exception {
    transform("void", "while (true) { }").into(
        "while (true) {",
        "}" );

  }

  public void testNullValue() throws Exception {
    transform("void", "Object e = null; boolean b = e == null;").into(
        "Object e = null;",
        "boolean b = true;" );

    transform("void", "Object e = null; boolean b = e != null;").into(
        "Object e = null;",
        "boolean b = false;" );
  }
  
  public void testIncDec() throws Exception {
    transform("void",
        "int i = 0;",
        "i++;",
        "i++;",
        "++i;"
        ).into(
            "int i = 0;",
            "i++;",
            "i++;",
            "++i;"
        );
  }
  
  public void testNotNull() throws Exception {
    transform("boolean", "String s = baz(); if (s == null) return false; return s != null;").into(
        "String s = EntryPoint.baz();", "if (s == null)", "  return false;", "return s != null;");
  }
  
  public void testImplicitCasts() throws Exception {
    transform("long", 
        "int bar = 0x12345678;",
        "bar = bar * 1234;",
        "long lng = bar;",
        "long lng8 = lng << 8;",
        "return lng8;"
        ).into(
            "  int bar = 305419896;", 
            "  bar = -1068970384;",
            "  long lng = -1068970384;", 
            "  long lng8 = lng << 8;", 
            "  return lng8;");
  }

  @Override
  protected IntegratedAnalysis<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, 
  ConstantsAssumption> createIntegratedAnalysis() {
    return new ConstantsAnalysis();
  }
}
