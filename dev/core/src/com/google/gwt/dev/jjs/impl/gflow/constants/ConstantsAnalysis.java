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

import com.google.gwt.dev.jjs.impl.gflow.Analysis;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionMap;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionUtil;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedAnalysis;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;

/**  
 * Constant propagation optimization.
 * 
 * Detects a situation when variable value is constant and replaces variable
 * access with constant value.
 * As of now supports only locals & parameters.
 */
public class ConstantsAnalysis implements 
    Analysis<CfgNode<?>, CfgEdge, Cfg, ConstantsAssumption>,
    IntegratedAnalysis<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, ConstantsAssumption> {

  public ConstantsFlowFunction getFlowFunction() {
    return new ConstantsFlowFunction();
  }

  public ConstantsIntegratedFlowFunction getIntegratedFlowFunction() {
    return new ConstantsIntegratedFlowFunction();
  }

  public void setInitialGraphAssumptions(Cfg graph,
      AssumptionMap<CfgEdge, ConstantsAssumption> assumptionMap) {
    AssumptionUtil.setAssumptions(graph.getGraphInEdges(), 
        ConstantsAssumption.TOP, assumptionMap);
  }
}
