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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.impl.CloneExpressionVisitor;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgReadNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

/**
 * Replace variable read by its constant value.
 */
final class FoldConstantTransformer implements CfgTransformer {
  private final ConstantsAssumption assumption;
  private CloneExpressionVisitor cloner;
  private final CfgReadNode nodeToFold;

  public FoldConstantTransformer(ConstantsAssumption assumptions,
      CfgReadNode nodeToFold) {
    this.assumption = assumptions;
    this.nodeToFold = nodeToFold;
    cloner = new CloneExpressionVisitor();
  }

  @Override
  public boolean transform(CfgNode<?> node, Cfg cfgGraph) {
    Preconditions.checkArgument(nodeToFold == node);
    JModVisitor visitor = new JModVisitor() {
      @Override
      public boolean visit(JVariableRef x, Context ctx) {
        JNode newNode = transform(x);
        if (newNode != null) {
          ctx.replaceMe(newNode);
          return false;
        }
        return true;
      }
    };

    CfgNode<?> parentNode = nodeToFold.getParent();
    JNode jnode = parentNode.getJNode();
    Preconditions.checkNotNull(jnode);
    visitor.accept(jnode);
    Preconditions.checkState(visitor.didChange());
    return true;
  }

  private JNode transform(JVariableRef ref) {
    if (nodeToFold.getJNode() != ref) {
      return null;
    }
    JVariable var = ref.getTarget();
    JValueLiteral literal = assumption.get(var);
    Preconditions.checkNotNull(literal);
    return cloner.cloneExpression(literal);
  }
}