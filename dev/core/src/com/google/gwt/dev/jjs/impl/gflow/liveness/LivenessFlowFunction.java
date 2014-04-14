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
import com.google.gwt.dev.jjs.impl.gflow.FlowFunction;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgReadNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgReadWriteNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgVisitor;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgWriteNode;
import com.google.gwt.dev.jjs.impl.gflow.liveness.LivenessAssumption.Updater;

/**
 * Flow function for Liveness Analysis.
 */
public class LivenessFlowFunction implements FlowFunction<CfgNode<?>, CfgEdge,
    Cfg, LivenessAssumption> {
  @Override
  public void interpret(CfgNode<?> node, Cfg g,
      AssumptionMap<CfgEdge, LivenessAssumption> assumptionMap) {
    final Updater result = new Updater(
        AssumptionUtil.join(g.getOutEdges(node), assumptionMap));

    node.accept(new CfgVisitor() {
      @Override
      public void visitReadNode(CfgReadNode node) {
        JVariable target = node.getTarget();
        if (target instanceof JLocal || target instanceof JParameter) {
          result.use(target);
        }
      }

      @Override
      public void visitReadWriteNode(CfgReadWriteNode node) {
        JVariable target = node.getTargetVariable();
        if (target instanceof JLocal || target instanceof JParameter) {
          result.use(target);
        }
      }

      @Override
      public void visitWriteNode(CfgWriteNode node) {
        JVariable target = node.getTargetVariable();
        if (target instanceof JLocal || target instanceof JParameter) {
          result.kill(target);
        }
      }
    });

    AssumptionUtil.setAssumptions(g.getInEdges(node), result.unwrap(), assumptionMap);
  }
}
