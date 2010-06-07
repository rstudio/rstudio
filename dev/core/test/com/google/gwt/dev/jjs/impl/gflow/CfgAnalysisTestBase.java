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
import com.google.gwt.dev.util.Strings;

import java.util.Map;

public abstract class CfgAnalysisTestBase<A extends Assumption<A>> 
    extends JJSTestBase {
  protected boolean forward = true;

  protected AnalysisResult analyze(String returnType, String... codeSnippet)
      throws UnableToCompleteException {
    return analyzeWithParams(returnType, "", codeSnippet);
  }

  protected AnalysisResult analyzeWithParams(String returnType, String params,
      String... codeSnippet) throws UnableToCompleteException {
    JProgram program = compileSnippet(returnType, params, Strings.join(
        codeSnippet, "\n"));
    JMethodBody body = (JMethodBody) findMainMethod(program).getBody();
    Cfg cfgGraph = CfgBuilder.build(program, body.getBlock());

    assertNotNull(cfgGraph);

    Map<CfgEdge, A> map = AnalysisSolver.solve(cfgGraph, createAnalysis(), forward);
    return new AnalysisResult(cfgGraph, map);
  }

  protected abstract Analysis<CfgNode<?>, CfgEdge, Cfg, A> createAnalysis();

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
      assertEquals(Strings.join(expected, "\n"), actual);
    }
  }
}
