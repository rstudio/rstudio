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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionMap;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionUtil;
import com.google.gwt.dev.jjs.impl.gflow.IntegratedFlowFunction;
import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction.Transformation;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgReadNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgUtil;
import com.google.gwt.dev.util.Preconditions;

/**
 * Integrated flow function for CopyAnalysis. Tries to replace copied vars with
 * original ones.
 */
public class CopyIntegratedFlowFunction implements
    IntegratedFlowFunction<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, CopyAssumption> {
  private final class CopyTransformation implements
      Transformation<CfgTransformer, Cfg> {
    private final CfgNode<?> node;
    private final JVariable original;
    private final Cfg graph;

    private CopyTransformation(CfgNode<?> node, JVariable original,
        Cfg graph) {
      this.node = node;
      this.original = original;
      this.graph = graph;
    }

    public CfgTransformer getGraphTransformer() {
      return new CfgTransformer() {
        public boolean transform(final CfgNode<?> node, Cfg cfgGraph) {
          JModVisitor visitor = new JModVisitor() {
            @Override
            public void endVisit(JNode x, Context ctx) {
              if (x == node.getJNode()) {
                ctx.replaceMe(createRef(x.getSourceInfo(), original));
              }
            } 
          };
          CfgNode<?> parentNode = node.getParent();
          JNode parentJNode = parentNode.getJNode();
          visitor.accept(parentJNode);
          Preconditions.checkState(visitor.didChange());
          return true;
        }
      };
    }

    public Cfg getNewSubgraph() {
      CfgReadNode newNode = new CfgReadNode(node.getParent(), 
          createRef(node.getJNode().getSourceInfo(), original));
      return CfgUtil.createSingleNodeReplacementGraph(graph, node, newNode);
    }

    @Override
    public String toString() {
      return "CopyTransformation(" + node + "," + original + ")";
    }

    private JVariableRef createRef(SourceInfo sourceInfo, JVariable variable) {
      if (variable instanceof JLocal) {
        return new JLocalRef(sourceInfo, (JLocal) variable);
      } else if (variable instanceof JParameter) {
        return new JParameterRef(sourceInfo, (JParameter) variable);
      }
      throw new IllegalArgumentException("Unsupported variable: " + 
          variable.getClass());
    }
  }

  private static final CopyFlowFunction FLOW_FUNCTION = new CopyFlowFunction();

  public Transformation<CfgTransformer, Cfg> 
  interpretOrReplace(final CfgNode<?> node, final Cfg graph, 
      AssumptionMap<CfgEdge, CopyAssumption> assumptionMap) {
    CopyAssumption in = AssumptionUtil.join(
        graph.getInEdges(node), assumptionMap);
    
    if (in != null && node instanceof CfgReadNode) {
      JVariable v = ((CfgReadNode) node).getTarget();
      final JVariable original = in.getOriginal(v);
      Preconditions.checkState(v != original, 
          "Variable is a copy of itself: %s", v);
      if (original != null) {
        return new CopyTransformation(node, original, graph);
      }
    }
    
    FLOW_FUNCTION.interpret(node, graph, assumptionMap);
    return null;
  }
}
