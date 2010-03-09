package com.google.gwt.dev.jjs.impl.gflow;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.OptimizerTestBase;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgBuilder;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.dev.util.Strings;

public abstract class CfgIntegratedAnalysisTestBase<A extends Assumption<A>>
    extends OptimizerTestBase {
  protected boolean forward = true;
  
  protected Result transform(String returnType, String... codeSnippet)
      throws UnableToCompleteException {
    JProgram program = compileSnippet(returnType, Strings.join(codeSnippet,
        "\n"));
    JMethodBody body = (JMethodBody) findMainMethod(program).getBody();
    Cfg cfgGraph = CfgBuilder.build(program, body.getBlock());

    assertNotNull(cfgGraph);

    AnalysisSolver.solveIntegrated(cfgGraph, createIntegratedAnalysis(program), forward);
    return new Result(program);
  }

  protected static class Result {
    private JProgram program;

    public Result(JProgram program) {
      this.program = program;
    }

    public void into(String... expected) {
      String joinedE = "";
      for (int i = 0; i < expected.length; ++i) {
        if (i > 0) {
          joinedE += "\n";
        }
        joinedE += expected[i];
      }
      JBlock block = ((JMethodBody) findMainMethod(program).getBody()).getBlock();

      String actual = block.toSource();
      assertTrue(actual.startsWith("{\n"));
      assertTrue(actual.endsWith("\n}"));
      actual = actual.substring(2, actual.length() - 2).trim();
      actual = actual.replaceAll("\\n  ", "\n");
      assertEquals(joinedE, actual);
    }
  }

  protected abstract IntegratedAnalysis<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, A> createIntegratedAnalysis(
      JProgram program);
}
