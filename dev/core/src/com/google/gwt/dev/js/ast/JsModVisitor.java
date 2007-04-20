/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;

import java.util.ArrayList;

/**
 * A visitor for iterating through and modifying an AST.
 */
public class JsModVisitor extends JsVisitor {

  private interface ContextFactory {
    JsContext create();
  }

  private class ContextPool extends ArrayList {

    private ContextFactory factory;
    private int pos = 0;

    public ContextPool(ContextFactory factory) {
      this.factory = factory;
    }

    public void release(JsContext ctx) {
      if (get(--pos) != ctx) {
        throw new InternalCompilerException(
            "Tried to release the wrong context");
      }
    }

    public JsContext take() {
      if (pos == size()) {
        add(factory.create());
      }
      return (JsContext) get(pos++);
    }
  }

  private class ListContext implements JsContext {
    private int index;
    private JsCollection collection;
    private boolean removed;
    private boolean replaced;

    public boolean canInsert() {
      return true;
    }

    public boolean canRemove() {
      return true;
    }

    public void insertAfter(JsNode node) {
      checkRemoved();
      collection.addNode(index + 1, node);
      didChange = true;
    }

    public void insertBefore(JsNode node) {
      checkRemoved();
      collection.addNode(index++, node);
      didChange = true;
    }

    public void removeMe() {
      checkState();
      collection.removeNode(index--);
      didChange = removed = true;
    }

    public void replaceMe(JsNode node) {
      checkState();
      checkReplacement(collection.getNode(index), node);
      collection.setNode(index, node);
      didChange = replaced = true;
    }

    protected void doReplace(Class targetClass, JsNode x) {
      checkState();
      checkReplacement(collection.getNode(index), x);
      collection.setNode(index, x);
      didChange = replaced = true;
    }

    protected void traverse(JsCollection collection) {
      this.collection = collection;
      for (index = 0; index < collection.size(); ++index) {
        removed = replaced = false;
        doTraverse(collection.getNode(index), this);
      }
    }

    private void checkRemoved() {
      if (removed) {
        throw new InternalCompilerException("Node was already removed");
      }
    }

    private void checkState() {
      checkRemoved();
      if (replaced) {
        throw new InternalCompilerException("Node was already replaced");
      }
    }
  }

  private class NodeContext implements JsContext {
    private JsNode node;
    private boolean replaced;

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return false;
    }

    public void insertAfter(JsNode node) {
      throw new UnsupportedOperationException();
    }

    public void insertBefore(JsNode node) {
      throw new UnsupportedOperationException();
    }

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(JsNode node) {
      if (replaced) {
        throw new InternalCompilerException("Node was already replaced");
      }
      checkReplacement(this.node, node);
      this.node = node;
      didChange = replaced = true;
    }

    protected JsNode traverse(JsNode node) {
      this.node = node;
      replaced = false;
      doTraverse(node, this);
      return this.node;
    }
  }

  protected static void checkReplacement(JsNode origNode, JsNode newNode) {
    if (newNode == null) {
      throw new InternalCompilerException("Cannot replace with null");
    }
    if (newNode == origNode) {
      throw new InternalCompilerException(
          "The replacement is the same as the original");
    }
  }

  protected boolean didChange = false;

  private final ContextPool listContextPool = new ContextPool(
      new ContextFactory() {
        public JsContext create() {
          return new ListContext();
        }
      });

  private final ContextPool nodeContextPool = new ContextPool(
      new ContextFactory() {
        public JsContext create() {
          return new NodeContext();
        }
      });

  public boolean didChange() {
    return didChange;
  }

  protected JsNode doAccept(JsNode node) {
    NodeContext ctx = (NodeContext) nodeContextPool.take();
    try {
      return ctx.traverse(node);
    } finally {
      nodeContextPool.release(ctx);
    }
  }

  protected void doAccept(JsCollection collection) {
    NodeContext ctx = (NodeContext) nodeContextPool.take();
    try {
      for (int i = 0, c = collection.size(); i < c; ++i) {
        collection.setNode(i, ctx.traverse(collection.getNode(i)));
      }
    } finally {
      nodeContextPool.release(ctx);
    }
  }

  protected void doAcceptWithInsertRemove(JsCollection collection) {
    ListContext ctx = (ListContext) listContextPool.take();
    try {
      ctx.traverse(collection);
    } finally {
      listContextPool.release(ctx);
    }
  }

}
