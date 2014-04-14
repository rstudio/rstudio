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
package com.google.gwt.dev.jjs.impl.gflow.unreachable;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JLabeledStatement;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;

final class DeleteNodeVisitor extends JModVisitor {
  public static boolean delete(JNode node, JNode parentNode) {
    DeleteNodeVisitor visitor = new DeleteNodeVisitor(node);
    visitor.accept(parentNode);
    return visitor.didChange();
  }

  private final JNode node;

  public DeleteNodeVisitor(JNode node) {
    this.node = node;
  }

  @Override
  public boolean visit(JLabeledStatement x, Context ctx) {
    if (!super.visit(x, ctx)) {
      return false;
    }

    if (x.getBody() == node) {
      // Remove node with its label.
      ctx.removeMe();
      return false;
    }

    return true;
  }

  @Override
  public boolean visit(JNode x, Context ctx) {
    if (didChange()) {
      return false;
    }

    if (x == node) {
      ctx.removeMe();
      return false;
    }

    return true;
  }
}