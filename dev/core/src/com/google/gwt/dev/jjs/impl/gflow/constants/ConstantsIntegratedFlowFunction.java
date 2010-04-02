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

import com.google.gwt.dev.jjs.impl.gflow.AssumptionMap;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedFlowFunction;
import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction.Transformation;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;

/**
 * Integrated flow function for ConstantsAnalysis. First try a transformation,
 * fall back to flow function if no transformation is possible.
 */
public class ConstantsIntegratedFlowFunction
    implements
    IntegratedFlowFunction<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, ConstantsAssumption> {
  private static final ConstantsFlowFunction FLOW_FUNCTION = new ConstantsFlowFunction();

  private final ConstantsTransformationFunction transformationFunction;

  public ConstantsIntegratedFlowFunction() {
    transformationFunction = new ConstantsTransformationFunction();
  }

  public Transformation<CfgTransformer, Cfg> interpretOrReplace(
      CfgNode<?> node, Cfg graph,
      AssumptionMap<CfgEdge, ConstantsAssumption> assumptionMap) {
    Transformation<CfgTransformer, Cfg> transformation = transformationFunction.transform(
        node, graph, assumptionMap);
    if (transformation != null) {
      return transformation;
    }
    FLOW_FUNCTION.interpret(node, graph, assumptionMap);
    return null;
  }

}
