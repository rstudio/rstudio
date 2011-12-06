/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;

import java.util.List;

/**
 * A visitor for iterating through and modifying an AST.
 */
public class JsModVisitor extends JsVisitor {

  @SuppressWarnings("unchecked")
  private class ListContext<T extends JsVisitable> implements JsContext {
    private List<T> collection;
    private int index;
    private boolean removed;
    private boolean replaced;

    @Override
    public boolean canInsert() {
      return true;
    }

    @Override
    public boolean canRemove() {
      return true;
    }

    @Override
    public void insertAfter(JsVisitable node) {
      checkRemoved();
      collection.add(index + 1, (T) node);
      didChange = true;
    }

    @Override
    public void insertBefore(JsVisitable node) {
      checkRemoved();
      collection.add(index++, (T) node);
      didChange = true;
    }

    @Override
    public boolean isLvalue() {
      return false;
    }

    @Override
    public void removeMe() {
      checkState();
      collection.remove(index--);
      didChange = removed = true;
    }

    @Override
    public void replaceMe(JsVisitable node) {
      checkState();
      checkReplacement(collection.get(index), node);
      collection.set(index, (T) node);
      didChange = replaced = true;
    }

    protected void traverse(List<T> collection) {
      this.collection = collection;
      for (index = 0; index < collection.size(); ++index) {
        removed = replaced = false;
        doTraverse(collection.get(index), this);
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

  private class LvalueContext extends NodeContext<JsExpression> {
    @Override
    public boolean isLvalue() {
      return true;
    }
  }

  @SuppressWarnings("unchecked")
  private class NodeContext<T extends JsVisitable> implements JsContext {
    private T node;
    private boolean replaced;

    @Override
    public boolean canInsert() {
      return false;
    }

    @Override
    public boolean canRemove() {
      return false;
    }

    @Override
    public void insertAfter(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insertBefore(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLvalue() {
      return false;
    }

    @Override
    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void replaceMe(JsVisitable node) {
      if (replaced) {
        throw new InternalCompilerException("Node was already replaced");
      }
      checkReplacement(this.node, node);
      this.node = (T) node;
      didChange = replaced = true;
    }

    protected T traverse(T node) {
      this.node = node;
      replaced = false;
      doTraverse(node, this);
      return this.node;
    }
  }

  protected static void checkReplacement(JsVisitable origNode, JsVisitable newNode) {
    if (newNode == null) {
      throw new InternalCompilerException("Cannot replace with null");
    }
    if (newNode == origNode) {
      throw new InternalCompilerException("The replacement is the same as the original");
    }
  }

  protected boolean didChange = false;

  @Override
  public boolean didChange() {
    return didChange;
  }

  @Override
  protected <T extends JsVisitable> T doAccept(T node) {
    return new NodeContext<T>().traverse(node);
  }

  @Override
  protected <T extends JsVisitable> void doAcceptList(List<T> collection) {
    NodeContext<T> ctx = new NodeContext<T>();
    for (int i = 0, c = collection.size(); i < c; ++i) {
      ctx.traverse(collection.get(i));
      if (ctx.replaced) {
        collection.set(i, ctx.node);
      }
    }
  }

  @Override
  protected JsExpression doAcceptLvalue(JsExpression expr) {
    return new LvalueContext().traverse(expr);
  }

  @Override
  protected <T extends JsVisitable> void doAcceptWithInsertRemove(List<T> collection) {
    new ListContext<T>().traverse(collection);
  }

}
