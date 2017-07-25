/*
 * Copyright 2012 Google Inc.
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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

/**
 * Instruments the generated JavaScript to record code coverage information
 * about the original Java source.
 *
 * We maintain a global coverage object, whose keys are Java source filenames
 * and whose values are objects mapping line numbers to 1 (executed) or 0 (not
 * executed).
 */
public class CoverageInstrumentor {

  public static final String GWT_COVERAGE_SYSTEM_PROPERTY = "gwt.coverage";

  public static boolean isCoverageEnabled() {
    return System.getProperty(GWT_COVERAGE_SYSTEM_PROPERTY) != null;
  }

  /**
   * This class does the actual instrumentation. It replaces
   * {@code expr} with {@code (CoverageUtil.cover(file, line), expr)}.
   */
  private class Instrumentor extends CoverageVisitor {
    public Instrumentor() {
      super(instrumentableLines.keySet());
    }

    @Override
    public void endVisit(JsExpression x, JsContext ctx) {
      SourceInfo info = x.getSourceInfo();
      if (!instrumentableLines.containsEntry(info.getFileName(), info.getStartLine())) {
        return;
      }
      JsInvocation update = new JsInvocation(info,
          coverFnName.makeRef(info),
          new JsStringLiteral(info, info.getFileName()),
          new JsNumberLiteral(info, info.getStartLine()));
      ctx.replaceMe(new JsBinaryOperation(info, JsBinaryOperator.COMMA, update, x));
    }
  }

  public static void exec(JProgram jprogram, JsProgram jsProgram, JavaToJavaScriptMap jjsmap,
      Multimap<String, Integer> instrumentableLines) {

    exec(jsProgram, instrumentableLines,
        JsUtils
            .getJsNameForMethod(jjsmap, jprogram, RuntimeConstants.COVERAGE_UTIL_ON_BEFORE_UNLOAD),
        JsUtils.getJsNameForMethod(jjsmap, jprogram, RuntimeConstants.COVERAGE_UTIL_COVER),
        JsUtils.getJsNameForField(jjsmap, jprogram, RuntimeConstants.COVERAGE_UTIL_COVERAGE));
  }

  @VisibleForTesting
  static void exec(JsProgram jsProgram,
      Multimap<String, Integer> instrumentableLines, JsName onBeforeUnloadFnName,
      JsName coverFnName, JsName coverageFieldName) {

    new CoverageInstrumentor(jsProgram, instrumentableLines, onBeforeUnloadFnName, coverFnName,
        coverageFieldName).execImpl();
  }

  /**
   * Creates the baseline coverage object, with an entry mapping to 0 for every
   * instrumented line.
   */
  @VisibleForTesting
  static JsObjectLiteral baselineCoverage(SourceInfo info,
      Multimap<String, Integer> instrumentableLines) {
    JsObjectLiteral.Builder baselineBuilder = JsObjectLiteral.builder(info);
    for (String filename : instrumentableLines.keySet()) {
      JsObjectLiteral.Builder linesBuilder = JsObjectLiteral.builder(info);
      for (int line : instrumentableLines.get(filename)) {
        linesBuilder.add(new JsNumberLiteral(info, line), new JsNumberLiteral(info, 0));
      }
      baselineBuilder.add(new JsStringLiteral(info, filename), linesBuilder.build());
    }
    return baselineBuilder.build();
  }

  private Multimap<String, Integer> instrumentableLines;
  private JsProgram jsProgram;
  private JsName onBeforeUnloadFnName;
  private JsName coverFnName;
  private JsName coverageFieldName;

  private CoverageInstrumentor(JsProgram jsProgram, Multimap<String, Integer> instrumentableLines,
      JsName onBeforeUnloadFnName, JsName coverFnName, JsName coverageFieldName) {
    this.instrumentableLines = instrumentableLines;
    this.jsProgram = jsProgram;
    this.onBeforeUnloadFnName = onBeforeUnloadFnName;
    this.coverFnName = coverFnName;
    this.coverageFieldName = coverageFieldName;
  }

  private void addBeforeUnloadListener(SourceInfo info) {
    JsNameRef onbeforeunload = new JsNameRef(info, "onbeforeunload");
    onbeforeunload.setQualifier(new JsNameRef(info, "window"));
    JsNameRef handler = onBeforeUnloadFnName.makeRef(info);
    JsBinaryOperation assignment = new JsBinaryOperation(info, JsBinaryOperator.ASG,
        onbeforeunload, handler);
    jsProgram.getGlobalBlock().getStatements().add(assignment.makeStmt());
  }

  private void execImpl() {
    SourceInfo info = jsProgram.createSourceInfoSynthetic(getClass());
    addBeforeUnloadListener(info);
    initializeBaselineCoverage(info);
    new JsModVisitor() {
      @Override
      public void endVisit(JsFunction x, JsContext ctx) {
        new Instrumentor().accept(x.getBody());
      }
    }.accept(jsProgram);
  }

  private void initializeBaselineCoverage(SourceInfo info) {
    JsNameRef coverageObject = coverageFieldName.makeRef(info);
    JsBinaryOperation init = new JsBinaryOperation(info, JsBinaryOperator.ASG, coverageObject,
        baselineCoverage(info, instrumentableLines));
    jsProgram.getGlobalBlock().getStatements().add(0, init.makeStmt());
  }
}