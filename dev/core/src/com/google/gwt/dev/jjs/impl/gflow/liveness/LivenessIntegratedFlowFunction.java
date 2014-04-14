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
package com.google.gwt.dev.jjs.impl.gflow.liveness;

import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionMap;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionUtil;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedFlowFunction;
import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction.Transformation;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgWriteNode;

/**
 *
 */
public class LivenessIntegratedFlowFunction implements
    IntegratedFlowFunction<CfgNode<?>, CfgEdge, CfgTransformer, Cfg,
    LivenessAssumption> {
  private final LivenessFlowFunction flowFunction = new LivenessFlowFunction();

  @Override
  public Transformation<CfgTransformer, Cfg>
  interpretOrReplace(CfgNode<?> node, Cfg graph,
      AssumptionMap<CfgEdge, LivenessAssumption> assumptionMap) {
    LivenessAssumption assumptions = AssumptionUtil.join(
        graph.getOutEdges(node), assumptionMap);

    if (node instanceof CfgWriteNode) {
      CfgWriteNode write = (CfgWriteNode) node;
      JVariable variable = write.getTargetVariable();
      if ((variable instanceof JLocal || variable instanceof JParameter) &&
          !isLive(assumptions, variable) && write.getValue() != null) {
        return new LivenessTransformation(graph, write);
      }
    }

    flowFunction.interpret(node, graph, assumptionMap);
    return null;
  }

  private boolean isLive(LivenessAssumption assumptions, JVariable variable) {
    return assumptions != null && assumptions.isLive(variable);
  }
}
