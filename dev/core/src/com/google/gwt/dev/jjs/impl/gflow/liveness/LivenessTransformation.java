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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction.Transformation;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNopNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgUtil;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgWriteNode;
import com.google.gwt.dev.util.Preconditions;

/**
 * Kill assignment. Leave rhs expression evaluation if it has side effects.
 */
public class LivenessTransformation implements 
    Transformation<CfgTransformer, Cfg> {
  private final Cfg graph;
  private final CfgWriteNode writeToKill;

  public LivenessTransformation(Cfg cfg, CfgWriteNode writeToKill) {
    this.graph = cfg;
    this.writeToKill = writeToKill;
  }

  public CfgTransformer getGraphTransformer() {
    return new CfgTransformer() {
      public boolean transform(CfgNode<?> node, Cfg cfgGraph) {
        JModVisitor visitor = new JModVisitor() {
          @Override
          public void endVisit(JBinaryOperation x, Context ctx) {
            if (!shouldKill(x)) {
              return;
            }

            ctx.replaceMe(x.getRhs());
          }
          
          @Override
          public void endVisit(JDeclarationStatement x, Context ctx) {
            if (writeToKill.getValue() != x.getInitializer() ||
                x != writeToKill.getJNode()) {
              return;
            }

            if (x.getInitializer().hasSideEffects()) {
              ctx.insertBefore(x.getInitializer().makeStatement());
            }
            
            x.initializer = null;
            madeChanges();
          }

          @Override
          public boolean visit(JExpressionStatement x, Context ctx) {
            JExpression expr = x.getExpr();
            if (expr instanceof JBinaryOperation) {
              JBinaryOperation binop = (JBinaryOperation) expr;
              if (shouldKill(binop) && 
                  !binop.getRhs().hasSideEffects()) {
                ctx.removeMe();
                return false;
              }
            }
            return true;
          }

          private boolean shouldKill(JBinaryOperation x) {
            return writeToKill.getJNode() == x;
          }
        };
        
        CfgNode<?> parentNode = CfgUtil.findParentOfContainingStatement(node);
        Preconditions.checkNotNull(parentNode, 
            "Can't find parent of stmt of %s", node);
        JNode parentJNode = parentNode.getJNode();
        visitor.accept(parentJNode);
        Preconditions.checkState(visitor.didChange(), 
            "Can't remove write in %s", node.getJNode());
        return visitor.didChange();
      }
    };
  }

  public Cfg getNewSubgraph() {
    CfgNode<?> newNode = new CfgNopNode(writeToKill.getParent(), 
        writeToKill.getJNode());
    return CfgUtil.createSingleNodeReplacementGraph(graph, writeToKill, 
        newNode);
  }
}
