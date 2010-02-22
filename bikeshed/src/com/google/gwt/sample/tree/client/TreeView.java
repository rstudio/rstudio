/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.tree.client;

import com.google.gwt.sample.tree.client.TreeNodeView.ExtraTreeItem;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;

/**
 * A view of a tree.
 */
public class TreeView extends Composite {

  /**
   * Construct a new {@link TreeView}.
   * 
   * @param treeModel the model that supplies the data
   * @param rootData the data object that will be passed to the
   *          {@link TreeModel} to create the first factory
   */
  public TreeView(TreeModel treeModel, Object rootData) {
    this(treeModel.createTreeNodeFactory(rootData));
  }

  /**
   * Construct a new {@link TreeView}.
   * 
   * @param factory the factory that supplies the data
   */
  public TreeView(TreeNodeFactory<?> factory) {
    Tree tree = new Tree();
    initWidget(tree);

    // Add the root item.
    ExtraTreeItem rootItem = new ExtraTreeItem("Dummy UI Root");
    tree.addItem(rootItem);

    // Associate a view with the item.
    new TreeNodeView(rootItem, factory);
  }
}
