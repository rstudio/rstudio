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
 * A factory for generating tree nodes.
 * 
 * @param <C> the type of children in the tree node.
 */
public abstract class AbstractTreeNodeFactory<C> implements TreeNodeFactory<C> {

  private ListModel<C> listModel;
  private Cell<C> cell;

  /**
   * Construct a new {@link AbstractTreeNodeFactory}.
   * 
   * @param listModel the {@link ListModel} used to generate child values
   * @param cell the {@link Cell} used to render children
   */
  public AbstractTreeNodeFactory(ListModel<C> listModel, Cell<C> cell) {
    this.listModel = listModel;
    this.cell = cell;
  }

  /**
   * Get the {@link Cell} used to render child nodes.
   * 
   * @return the cell
   */
  public Cell<C> getCell() {
    return cell;
  }

  /**
   * Get the {@link ListModel} used to retrieve child node values.
   * 
   * @return the list model
   */
  public ListModel<C> getListModel() {
    return listModel;
  }
}
