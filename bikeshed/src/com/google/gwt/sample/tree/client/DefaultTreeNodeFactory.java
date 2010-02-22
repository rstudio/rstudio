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

import com.google.gwt.cells.client.Cell;
import com.google.gwt.list.shared.ListModel;

/**
 * Default implementation of a {@link TreeNodeFactory}.
 * 
 * @param <C> the type of children in the tree node.
 */
public class DefaultTreeNodeFactory<C> extends AbstractTreeNodeFactory<C> {

  private TreeModel treeModel;

  /**
   * Construct a new {@link DefaultTreeNodeFactory}.
   * 
   * @param listModel the {@link ListModel} used to generate child values
   * @param cell the {@link Cell} used to render children
   * @param treeModel the {@link TreeModel} used to generate factories
   */
  public DefaultTreeNodeFactory(ListModel<C> listModel, Cell<C> cell,
      TreeModel treeModel) {
    super(listModel, cell);
    this.treeModel = treeModel;
  }

  public TreeNodeFactory<?> createChildFactory(C value) {
    return treeModel.createTreeNodeFactory(value);
  }
}
