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
package com.google.gwt.bikeshed.tree.client;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A model of a tree.
 */
public interface TreeViewModel {

  /**
   * The info needed to create a {@link TreeNodeView}.
   */
  interface NodeInfo<C> {
    /**
     * Get the {@link Cell} used to render child nodes.
     * 
     * @return the cell
     */
    Cell<C> getCell();

    /**
     * Get the {@link ListModel} used to retrieve child node values.
     * 
     * @return the list model
     */
    ListModel<C> getListModel();

    /**
     * Handle an event that is fired on one of the children of this item.
     * 
     * @param elem the parent element of the item
     * @param object the data value of the item
     * @param event the event that was fired
     */
    void onBrowserEvent(Element elem, final C object, NativeEvent event);
  }

  /**
   * Default implementation of {@link NodeInfo}.
   */
  class DefaultNodeInfo<C> implements NodeInfo<C> {

    private Cell<C> cell;
    private ListModel<C> listModel;
    private ValueUpdater<C> valueUpdater;

    /**
     * Construct a new {@link DefaultNodeInfo}.
     * 
     * @param listModel the {@link ListModel} that provides the child values
     * @param cell the {@link Cell} used to render the child values
     */
    public DefaultNodeInfo(ListModel<C> listModel, Cell<C> cell) {
      this.cell = cell;
      this.listModel = listModel;
    }

    /**
     * Construct a new {@link DefaultNodeInfo}.
     * 
     * @param listModel the {@link ListModel} that provides the child values
     * @param cell the {@link Cell} used to render the child values
     * @param valueUpdater the {@link ValueUpdater}
     */
    public DefaultNodeInfo(ListModel<C> listModel, Cell<C> cell,
        ValueUpdater<C> valueUpdater) {
      this(listModel, cell);
      this.valueUpdater = valueUpdater;
    }

    public Cell<C> getCell() {
      return cell;
    }

    public ListModel<C> getListModel() {
      return listModel;
    }

    public void onBrowserEvent(Element elem, final C object, NativeEvent event) {
      cell.onBrowserEvent(elem, object, event, valueUpdater);
    }
  }

  /**
   * Get the {@link NodeInfo} that will provide the {@link ListModel} and
   * {@link Cell} to retrieve the children of the specified value.
   * 
   * @param value the value in the parent node
   * @param treeNodeView the {@link TreeNodeView} that contains the value
   * @return the {@link NodeInfo}
   */
  <T> NodeInfo<?> getNodeInfo(T value, TreeNodeView<T> treeNodeView);

  /**
   * Check if the value is known to be a leaf node.
   * 
   * @param value the value at the node
   * @return true if the node is known to be a leaf node, false if not
   */
  boolean isLeaf(Object value);
}
