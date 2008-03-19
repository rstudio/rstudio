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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;

import java.util.ArrayList;
import java.util.List;

/**
 * A visitor for iterating through and modifying an AST.
 */
@SuppressWarnings("unchecked")
public class JModVisitor extends JVisitor {

  private interface ContextFactory {
    Context create();
  }

  private class ContextPool extends ArrayList {

    private ContextFactory factory;
    private int pos = 0;

    public ContextPool(ContextFactory factory) {
      this.factory = factory;
    }

    public void release(Context ctx) {
      if (get(--pos) != ctx) {
        throw new InternalCompilerException(
            "Tried to release the wrong context");
      }
    }

    public Context take() {
      if (pos == size()) {
        add(factory.create());
      }
      return (Context) get(pos++);
    }
  }

  private class ListContext implements Context {
    private int index;
    private List list;
    private boolean removed;
    private boolean replaced;

    public boolean canInsert() {
      return true;
    }

    public boolean canRemove() {
      return true;
    }

    public void insertAfter(JNode node) {
      checkRemoved();
      list.add(index + 1, node);
      didChange = true;
    }

    public void insertBefore(JNode node) {
      checkRemoved();
      list.add(index++, node);
      didChange = true;
    }

    public void removeMe() {
      checkState();
      list.remove(index--);
      didChange = removed = true;
    }

    public void replaceMe(JNode node) {
      checkState();
      checkReplacement((JNode) list.get(index), node);
      list.set(index, node);
      didChange = replaced = true;
    }

    protected void traverse(List list) {
      this.list = list;
      for (index = 0; index < list.size(); ++index) {
        removed = replaced = false;
        doTraverse((JNode) list.get(index), this);
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

  private class NodeContext implements Context {
    private JNode node;
    private boolean replaced;

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return false;
    }

    public void insertAfter(JNode node) {
      throw new UnsupportedOperationException();
    }

    public void insertBefore(JNode node) {
      throw new UnsupportedOperationException();
    }

    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    public void replaceMe(JNode node) {
      if (replaced) {
        throw new InternalCompilerException("Node was already replaced");
      }
      checkReplacement(this.node, node);
      this.node = node;
      didChange = replaced = true;
    }

    protected JNode traverse(JNode node) {
      this.node = node;
      replaced = false;
      doTraverse(node, this);
      return this.node;
    }
  }

  protected static void checkReplacement(JNode origNode, JNode newNode) {
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
        public Context create() {
          return new ListContext();
        }
      });

  private final ContextPool nodeContextPool = new ContextPool(
      new ContextFactory() {
        public Context create() {
          return new NodeContext();
        }
      });

  public boolean didChange() {
    return didChange;
  }

  protected JNode doAccept(JNode node) {
    NodeContext ctx = (NodeContext) nodeContextPool.take();
    try {
      return ctx.traverse(node);
    } finally {
      nodeContextPool.release(ctx);
    }
  }

  protected void doAccept(List list) {
    NodeContext ctx = (NodeContext) nodeContextPool.take();
    try {
      for (int i = 0, c = list.size(); i < c; ++i) {
        list.set(i, ctx.traverse((JNode) list.get(i)));
      }
    } finally {
      nodeContextPool.release(ctx);
    }
  }

  protected void doAcceptWithInsertRemove(List list) {
    ListContext ctx = (ListContext) listContextPool.take();
    try {
      ctx.traverse(list);
    } finally {
      listContextPool.release(ctx);
    }
  }

}
