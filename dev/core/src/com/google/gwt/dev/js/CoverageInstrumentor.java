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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Instruments the generated JavaScript to record code coverage information
 * about the original Java source.
 *
 * We maintain a global coverage object, whose keys are Java source filenames
 * and whose values are objects mapping line numbers to 1 (executed) or 0 (not
 * executed).
 */
public class CoverageInstrumentor {
  /**
   * This class does the actual instrumentation. It replaces
   * {@code expr} with {@code ($coverage[file][line] = 1, expr)}.
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
      JsStringLiteral fileName = new JsStringLiteral(info, info.getFileName());
      JsNumberLiteral lineNumber = new JsNumberLiteral(info, info.getStartLine());
      JsArrayAccess lhs = new JsArrayAccess(info, new JsArrayAccess(info,
          coverageObject.makeRef(info), fileName), lineNumber);
      JsBinaryOperation update = new JsBinaryOperation(info, JsBinaryOperator.ASG,
          lhs, new JsNumberLiteral(info, 1));
      ctx.replaceMe(new JsBinaryOperation(info, JsBinaryOperator.COMMA, update, x));
    }
  }

  public static void exec(JsProgram jsProgram, Multimap<String, Integer> instrumentableLines) {
    new CoverageInstrumentor(jsProgram, instrumentableLines).execImpl();
  }

  private Multimap<String, Integer> instrumentableLines;
  private JsProgram jsProgram;
  private JsName coverageObject;

  private CoverageInstrumentor(JsProgram jsProgram, Multimap<String, Integer> instrumentableLines) {
    this.instrumentableLines = instrumentableLines;
    this.jsProgram = jsProgram;
  }

  private void addBeforeUnloadListener() {
    try {
      parseIntoGlobalBlock(Utility.getFileFromClassPath("com/google/gwt/dev/js/coverage.js"));
    } catch (IOException e) {
      throw new InternalCompilerException("Error during coverage instrumentation", e);
    }
  }

  /**
   * Creates the baseline coverage object, with an entry mapping to 0 for every
   * instrumented line.
   */
  private JsObjectLiteral baselineCoverage() {
    SourceInfo info = dummySourceInfo();
    JsObjectLiteral baseline = new JsObjectLiteral(info);
    List<JsPropertyInitializer> properties = baseline.getPropertyInitializers();
    for (String filename : instrumentableLines.keySet()) {
      JsPropertyInitializer pair = new JsPropertyInitializer(info);
      pair.setLabelExpr(new JsStringLiteral(info, filename));
      JsObjectLiteral lines = new JsObjectLiteral(info);
      List<JsPropertyInitializer> coverage = lines.getPropertyInitializers();
      for (int line : instrumentableLines.get(filename)) {
        coverage.add(new JsPropertyInitializer(info,
            new JsNumberLiteral(info, line), new JsNumberLiteral(info, 0)));
      }
      pair.setValueExpr(lines);
      properties.add(pair);
    }
    return baseline;
  }

  private SourceInfo dummySourceInfo() {
    return jsProgram.createSourceInfoSynthetic(getClass());
  }

  private void execImpl() {
    coverageObject = global("$coverage", baselineCoverage());
    new JsModVisitor() {
      @Override
      public void endVisit(JsFunction x, JsContext ctx) {
        new Instrumentor().accept(x.getBody());
      }
    }.accept(jsProgram);
    addBeforeUnloadListener();
  }

  /**
   * Declares a global variable with the given name initialized to the given
   * expression. Returns a reference to the name object, useful for making
   * references to the variable.
   */
  private JsName global(String name, JsExpression initExpr) {
    JsName jsName = jsProgram.getScope().declareName(name);
    jsName.setObfuscatable(false);
    JsVar var = new JsVar(dummySourceInfo(), jsName);
    var.setInitExpr(initExpr);
    makeGlobal(var);
    return jsName;
  }

  private void makeGlobal(JsVar var) {
    JsVars vars;
    JsStatement first = jsProgram.getGlobalBlock().getStatements().get(0);
    if (first instanceof JsVars) {
      vars = (JsVars) first;
    } else {
      vars = new JsVars(dummySourceInfo());
      jsProgram.getGlobalBlock().getStatements().add(0, vars);
    }
    vars.add(var);
  }

  private void makeGlobal(JsStatement statement) {
    jsProgram.getGlobalBlock().getStatements().add(0, statement);
  }

  /**
   * Parses the given code and adds it to the global block. Any names introduced
   * are not obfuscatable.
   */
  private void parseIntoGlobalBlock(String code) {
    List<JsStatement> statements = null;
    try {
      statements = JsParser.parse(SourceOrigin.UNKNOWN, jsProgram.getScope(),
          new StringReader(code));
    } catch (Exception e) {
      throw new InternalCompilerException("Unexpected exception parsing '" + code + "'", e);
    }

    for (JsStatement statement : statements) {
      new JsVisitor() {
        @Override public void endVisit(JsParameter x, JsContext ctx) {
          x.getName().setObfuscatable(false);
        }

        @Override public void endVisit(JsForIn x, JsContext ctx) {
          x.getIterVarName().setObfuscatable(false);
        }

        @Override public void endVisit(JsVar x, JsContext ctx) {
          x.getName().setObfuscatable(false);
        }
      }.accept(statement);
      makeGlobal(statement);
    }
  }
}