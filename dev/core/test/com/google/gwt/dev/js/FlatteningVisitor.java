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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class FlatteningVisitor extends JsVisitor {

  public static TreeNode exec(List<JsStatement> statements) {
    FlatteningVisitor visitor = new FlatteningVisitor();
    visitor.acceptList(statements);
    return visitor.root;
  }

  public static class TreeNode {
    public final JsVisitable node;
    public final List<TreeNode> children = new ArrayList<TreeNode>();

    public TreeNode(JsVisitable node) {
      this.node = node;
    }
  }

  private TreeNode root;

  private FlatteningVisitor() {
    root = new TreeNode(null);
  }

  protected <T extends JsVisitable> T doAccept(T node) {
    TreeNode oldRoot = root;
    root = new TreeNode(node);
    oldRoot.children.add(root);
    super.doAccept(node);
    root = oldRoot;
    return node;
  }

  protected <T extends JsVisitable> void doAcceptList(List<T> collection) {
    for (Iterator<T> it = collection.iterator(); it.hasNext();) {
      doAccept(it.next());
    }
  }
}
