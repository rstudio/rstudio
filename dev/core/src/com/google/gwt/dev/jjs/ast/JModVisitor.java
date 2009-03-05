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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;

import java.util.List;

/**
 * A visitor for iterating through and modifying an AST.
 */
public class JModVisitor extends JVisitor {

  private static class ListContext implements Context {
    boolean didChange;
    int index;
    List<JNode> list;
    boolean removed;
    boolean replaced;

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
      checkReplacement(list.get(index), node);
      list.set(index, node);
      didChange = replaced = true;
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

  private static class NodeContext implements Context {
    boolean didChange;
    JNode node;
    boolean replaced;

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

  public JNode accept(JNode node) {
    NodeContext ctx = new NodeContext();
    try {
      ctx.node = node;
      node.traverse(this, ctx);
      didChange |= ctx.didChange;
      return ctx.node;
    } catch (Throwable e) {
      throw translateException(node, e);
    }
  }

  @SuppressWarnings("unchecked")
  public void accept(List<? extends JNode> list) {
    NodeContext ctx = new NodeContext();
    try {
      for (int i = 0, c = list.size(); i < c; ++i) {
        (ctx.node = list.get(i)).traverse(this, ctx);
        if (ctx.replaced) {
          ((List) list).set(i, ctx.node);
          ctx.replaced = false;
        }
      }
      didChange |= ctx.didChange;
    } catch (Throwable e) {
      throw translateException(ctx.node, e);
    }
  }

  @SuppressWarnings("unchecked")
  public void acceptWithInsertRemove(List<? extends JNode> list) {
    ListContext ctx = new ListContext();
    try {
      ctx.list = (List) list;
      for (ctx.index = 0; ctx.index < list.size(); ++ctx.index) {
        ctx.removed = ctx.replaced = false;
        list.get(ctx.index).traverse(this, ctx);
      }
      didChange |= ctx.didChange;
    } catch (Throwable e) {
      throw translateException(list.get(ctx.index), e);
    }
  }

  public boolean didChange() {
    return didChange;
  }

}
