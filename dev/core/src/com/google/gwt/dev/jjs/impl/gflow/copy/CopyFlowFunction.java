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

import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionMap;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionUtil;
import com.google.gwt.dev.jjs.impl.gflow.FlowFunction;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgReadWriteNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgVisitor;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgWriteNode;
import com.google.gwt.dev.jjs.impl.gflow.copy.CopyAssumption.Updater;

/**
 * Flow function for CopyAnalysis.
 */
public class CopyFlowFunction implements
    FlowFunction<CfgNode<?>, CfgEdge, Cfg, CopyAssumption> {
  @Override
  public void interpret(CfgNode<?> node,
      Cfg g, AssumptionMap<CfgEdge, CopyAssumption> assumptionMap) {
    CopyAssumption in = AssumptionUtil.join(g.getInEdges(node), assumptionMap);
    final Updater result = new Updater(in);

    node.accept(new CfgVisitor() {
      @Override
      public void visitReadWriteNode(CfgReadWriteNode node) {
        JVariable targetVariable = node.getTargetVariable();
        if (isSupportedVar(targetVariable)) {
          result.kill(targetVariable);
        }
      }

      @Override
      public void visitWriteNode(CfgWriteNode node) {
        JVariable targetVariable = node.getTargetVariable();
        if (!isSupportedVar(targetVariable)) {
          return;
        }

        if (!(node.getValue() instanceof JVariableRef)) {
          result.kill(targetVariable);
          return;
        }

        JVariable original = ((JVariableRef) node.getValue()).getTarget();
        original = result.getMostOriginal(original);

        if (original != targetVariable) {
          result.kill(targetVariable);
          if (isSupportedVar(original) &&
              original.getType() == targetVariable.getType()) {
            result.addCopy(original, targetVariable);
          }
        } else {
          // We don't have to kill any assumptions after i = i assignment.
        }
      }

      private boolean isSupportedVar(JVariable targetVariable) {
        return targetVariable instanceof JParameter ||
            targetVariable instanceof JLocal;
      }
    });

    AssumptionUtil.setAssumptions(g.getOutEdges(node), result.unwrap(), assumptionMap);
  }
}
