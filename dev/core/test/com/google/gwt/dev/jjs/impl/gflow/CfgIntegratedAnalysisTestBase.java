package com.google.gwt.dev.jjs.impl.gflow;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.OptimizerTestBase;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgBuilder;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;

public abstract class CfgIntegratedAnalysisTestBase<A extends Assumption<A>>
    extends OptimizerTestBase {
  protected boolean forward = true;
  
  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    JMethodBody body = (JMethodBody) method.getBody();
    Cfg cfgGraph = CfgBuilder.build(program, body.getBlock());

    assertNotNull(cfgGraph);

    return AnalysisSolver.solveIntegrated(cfgGraph, createIntegratedAnalysis(), forward);
  }
  
  protected Result transform(String returnType, String...snippet) 
      throws UnableToCompleteException {
    return optimize(returnType, snippet);
  }

  protected abstract IntegratedAnalysis<CfgNode<?>, CfgEdge, 
  CfgTransformer, Cfg, A> createIntegratedAnalysis();
}
