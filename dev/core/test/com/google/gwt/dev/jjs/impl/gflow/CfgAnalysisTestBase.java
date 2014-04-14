/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.JJSTestBase;
import com.google.gwt.dev.jjs.impl.gflow.cfg.AssumptionsPrinter;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgBuilder;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import java.util.Map;

/**
 * A base class for tests of {@link DataflowAnalyzer} components.
 */
public abstract class CfgAnalysisTestBase<A extends Assumption<A>>
    extends JJSTestBase {
  protected boolean forward = true;

  protected AnalysisResult analyze(String returnType, String... codeSnippet)
      throws UnableToCompleteException {
    return analyzeWithParams(returnType, "", codeSnippet);
  }

  protected AnalysisResult analyzeWithParams(String returnType, String params,
      String... codeSnippet) throws UnableToCompleteException {
    JProgram program = compileSnippet(returnType, params, Joiner.on("\n").join(codeSnippet), true);
    JMethodBody body = (JMethodBody) findMainMethod(program).getBody();
    Cfg cfgGraph = CfgBuilder.build(program, body.getBlock());

    assertNotNull(cfgGraph);

    Map<CfgEdge, A> map = AnalysisSolver.solve(cfgGraph, createAnalysis(), forward);
    return new AnalysisResult(cfgGraph, map);
  }

  protected abstract Analysis<CfgNode<?>, CfgEdge, Cfg, A> createAnalysis();

  /**
   * The result of an analysis.
   */
  protected class AnalysisResult {
    private final Map<CfgEdge, A> assumptions;
    private final Cfg graph;

    public AnalysisResult(Cfg graph,
        Map<CfgEdge, A> assumptions) {
      this.graph = graph;
      this.assumptions = assumptions;
    }

    public void into(String... expected) {
      String actual = new AssumptionsPrinter<A>(graph, assumptions).print();
      assertEquals(Joiner.on("\n").join(expected), actual);
    }
  }
}
