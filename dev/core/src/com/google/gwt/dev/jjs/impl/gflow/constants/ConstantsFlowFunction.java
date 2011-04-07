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

import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionMap;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionUtil;
import com.google.gwt.dev.jjs.impl.gflow.FlowFunction;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgConditionalNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgReadWriteNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgVisitor;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgWriteNode;
import com.google.gwt.dev.jjs.impl.gflow.constants.ConstantsAssumption.Updater;
import com.google.gwt.dev.util.Preconditions;

import java.util.ArrayList;

/**
 * Flow function for ConstantsAnalysis.
 */
public class ConstantsFlowFunction implements
    FlowFunction<CfgNode<?>, CfgEdge, Cfg, ConstantsAssumption> {
  public void interpret(CfgNode<?> node,
      final Cfg graph, AssumptionMap<CfgEdge, ConstantsAssumption> assumptionMap) {
    ConstantsAssumption in = AssumptionUtil.join(graph.getInEdges(node), assumptionMap);

    final int outSize = graph.getOutEdges(node).size();
    final ArrayList<ConstantsAssumption> result = 
      new ArrayList<ConstantsAssumption>(outSize);
    
    final Updater assumption = new Updater(in);
    node.accept(new CfgVisitor() {
      @Override
      public void visitConditionalNode(CfgConditionalNode<?> x) {
        JExpression condition = x.getCondition();

        Updater thenAssumptions = assumption.copy();
        Updater elseAssumptions = assumption.copy(); 
          
        Preconditions.checkNotNull(condition, "Null condition in %s", x);
        AssumptionDeducer.deduceAssumption(condition, JBooleanLiteral.TRUE, 
            thenAssumptions);
        AssumptionDeducer.deduceAssumption(condition, JBooleanLiteral.FALSE, 
            elseAssumptions);
        
        for (CfgEdge e : graph.getOutEdges(x)) {
          if (CfgConditionalNode.THEN.equals(e.getRole())) {
            result.add(thenAssumptions.unwrap());
          } else if (CfgConditionalNode.ELSE.equals(e.getRole())) {
            result.add(elseAssumptions.unwrap());
          } else {
            result.add(assumption.unwrap());
          }
        }
      }

      @Override
      public void visitNode(CfgNode<?> node) {
        // We can't deduce any assumptions from the node. Just copy incoming
        // assumptions further.
        for (int i = 0; i < graph.getOutEdges(node).size(); ++i) {
          result.add(assumption.unwrap());
        }
      }

      @Override
      public void visitReadWriteNode(CfgReadWriteNode node) {
        processWrite(assumption, node.getTargetVariable(), node.getValue());
        super.visitReadWriteNode(node);
      }

      @Override
      public void visitWriteNode(CfgWriteNode node) {
        processWrite(assumption, node.getTargetVariable(), node.getValue());
        super.visitWriteNode(node);
      }

      private void processWrite(final Updater assumption,
          JVariable var, JExpression expression) {
        if (var == null) {
          return;
        }
        
        if (var instanceof JParameter || var instanceof JLocal) {
          if (expression != null) {
            JValueLiteral valueLiteral = 
              ExpressionEvaluator.evaluate(expression, assumption.unwrap());
            if (valueLiteral != null && 
                (valueLiteral.getType() == var.getType() || 
                 valueLiteral instanceof JNullLiteral)) {
              assumption.set(var, valueLiteral);
            } else {
              // Don't bother to try to get conversions right.
              assumption.set(var, null);
            }
          } else {
            assumption.set(var, null);
          }
        }
      }
    });

    AssumptionUtil.setAssumptions(graph.getOutEdges(node), result, assumptionMap);
  }
}
