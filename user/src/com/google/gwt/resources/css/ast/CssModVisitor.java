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
package com.google.gwt.resources.css.ast;

import java.util.List;

/**
 * A visitor that can make structural modifications to a CSS tree.
 */
@SuppressWarnings("unchecked")
public class CssModVisitor extends CssVisitor {
  private class ListContext<T extends CssNode> implements Context {
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

    public void insertAfter(CssNode node) {
      checkRemoved();
      list.add(index + 1, node);
      didChange = true;
    }

    public void insertBefore(CssNode node) {
      checkRemoved();
      list.add(index++, node);
      didChange = true;
    }

    public void removeMe() {
      checkState();
      list.remove(index--);
      didChange = removed = true;
    }

    public void replaceMe(CssNode node) {
      checkState();
      checkReplacement((CssNode) list.get(index), node);
      list.set(index, node);
      didChange = replaced = true;
    }

    protected void traverse(List<? extends CssNode> list) {
      this.list = list;
      for (index = 0; index < list.size(); ++index) {
        removed = replaced = false;
        doTraverse(list.get(index), this);
      }
    }

    private void checkRemoved() {
      if (removed) {
        throw new CssCompilerException("Node was already removed");
      }
    }

    private void checkState() {
      checkRemoved();
      if (replaced) {
        throw new CssCompilerException("Node was already replaced");
      }
    }
  }

  protected static void checkReplacement(CssNode origNode, CssNode newNode) {
    if (newNode == null) {
      throw new CssCompilerException("Cannot replace with null");
    }
    if (newNode == origNode) {
      throw new CssCompilerException(
          "The replacement is the same as the original");
    }
  }

  private boolean didChange;

  public boolean didChange() {
    return didChange;
  }

  @Override
  protected void doAcceptWithInsertRemove(List<? extends CssNode> list) {
    ListContext context = new ListContext();
    context.traverse(list);
  }
}
