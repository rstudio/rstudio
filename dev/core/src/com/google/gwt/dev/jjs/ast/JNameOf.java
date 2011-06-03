/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;

import java.util.List;

/**
 * An AST node whose evaluation results in the string name of its node.
 */
public class JNameOf extends JExpression {

  static boolean replacesNamedElement(JNode newElement, JNode oldElement) {
    if (newElement instanceof JType) {
      if (!((JType) newElement).replaces((JType) oldElement)) {
        return false;
      }
    } else if (newElement instanceof JField) {
      if (!((JField) newElement).replaces((JField) oldElement)) {
        return false;
      }
    } else if (newElement instanceof JMethod) {
      if (!((JMethod) newElement).replaces((JMethod) oldElement)) {
        return false;
      }
    } else {
      throw new InternalCompilerException("Unexpected node type.");
    }
    return true;
  }

  static boolean replacesNamedElements(List<JNode> newElements, List<JNode> oldElements) {
    if (newElements.size() != oldElements.size()) {
      return false;
    }
    for (int i = 0, c = newElements.size(); i < c; ++i) {
      JNode node = newElements.get(i);
      JNode oldNode = oldElements.get(i);
      if (!replacesNamedElement(node, oldNode)) {
        return false;
      }
    }
    return true;
  }

  private HasName node;
  private JClassType stringType;

  public JNameOf(SourceInfo info, JClassType stringType, HasName node) {
    super(info);
    this.node = node;
    this.stringType = stringType;
    assert stringType.getName().equals("java.lang.String");
  }

  public HasName getNode() {
    return node;
  }

  public JNonNullType getType() {
    return stringType.getNonNull();
  }

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  /**
   * Resolve external references during AST stitching.
   */
  public void resolve(HasName node, JClassType stringType) {
    assert replacesNamedElement((JNode) node, (JNode) this.node);
    this.node = node;
    assert stringType.replaces(this.stringType);
    this.stringType = stringType;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      // Intentionally not visiting referenced node
    }
    visitor.endVisit(this, ctx);
  }
}
