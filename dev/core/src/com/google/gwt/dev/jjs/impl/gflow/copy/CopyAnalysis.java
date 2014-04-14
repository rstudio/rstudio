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

import com.google.gwt.dev.jjs.impl.gflow.Analysis;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionMap;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionUtil;
import com.google.gwt.dev.jjs.impl.gflow.FlowFunction;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedAnalysis;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedFlowFunction;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;

/**
 * Analysis which detects that one variable is the copy of the other,
 * and uses older var instead.
 */
public class CopyAnalysis implements
    Analysis<CfgNode<?>, CfgEdge, Cfg, CopyAssumption>,
    IntegratedAnalysis<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, CopyAssumption> {
  @Override
  public FlowFunction<CfgNode<?>, CfgEdge, Cfg, CopyAssumption> getFlowFunction() {
    return new CopyFlowFunction();
  }

  @Override
  public IntegratedFlowFunction<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, CopyAssumption>
  getIntegratedFlowFunction() {
    return new CopyIntegratedFlowFunction();
  }

  @Override
  public void setInitialGraphAssumptions(Cfg graph,
      AssumptionMap<CfgEdge, CopyAssumption> assumptionMap) {
    AssumptionUtil.setAssumptions(graph.getGraphInEdges(),
        CopyAssumption.TOP, assumptionMap);
  }
}
