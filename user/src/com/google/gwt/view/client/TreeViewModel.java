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
package com.google.gwt.view.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;

/**
 * A model of a tree.
 * 
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 */
public interface TreeViewModel {

  /**
   * Default implementation of {@link NodeInfo}.
   */
  class DefaultNodeInfo<T> implements NodeInfo<T> {

    private Cell<T> cell;
    private AbstractListViewAdapter<T> listViewAdapter;
    private SelectionModel<? super T> selectionModel;
    private ValueUpdater<T> valueUpdater;
    private HasData<T> view;

    /**
     * Construct a new {@link DefaultNodeInfo}.
     * 
     * @param adapter the {@link AbstractListViewAdapter} that provides the
     *          child values
     * @param cell the {@link Cell} used to render the child values
     */
    public DefaultNodeInfo(AbstractListViewAdapter<T> adapter, Cell<T> cell) {
      this(adapter, cell, null, null);
    }

    /**
     * Construct a new {@link DefaultNodeInfo}.
     * 
     * @param adapter the {@link AbstractListViewAdapter} that provides the
     *          child values
     * @param cell the {@link Cell} used to render the child values update when
     *          the selection changes
     * @param valueUpdater the {@link ValueUpdater}
     */
    public DefaultNodeInfo(AbstractListViewAdapter<T> adapter,
        final Cell<T> cell, SelectionModel<? super T> selectionModel,
        final ValueUpdater<T> valueUpdater) {
      this.listViewAdapter = adapter;
      this.cell = cell;
      this.selectionModel = selectionModel;
      this.valueUpdater = valueUpdater;
    }

    public Cell<T> getCell() {
      return cell;
    }

    public ProvidesKey<T> getProvidesKey() {
      return listViewAdapter;
    }

    public SelectionModel<? super T> getSelectionModel() {
      return selectionModel;
    }

    public ValueUpdater<T> getValueUpdater() {
      return valueUpdater;
    }

    public void setView(HasData<T> view) {
      this.view = view;
      listViewAdapter.addView(view);
    }

    public void unsetView() {
      listViewAdapter.removeView(view);
    }
  }

  /**
   * The info needed to create the children of a tree node.
   */
  interface NodeInfo<T> {

    /**
     * Get the {@link Cell} used to render the children of this node.
     * 
     * @return the {@link Cell}
     */
    Cell<T> getCell();

    /**
     * Return the key provider for children of this node.
     * 
     * @return the {@link ProvidesKey}
     */
    ProvidesKey<T> getProvidesKey();

    /**
     * Get the {@link SelectionModel} used for the children of this node. To
     * unify selection across all items of the same type, or across the entire
     * tree, return the same instance of {@link SelectionModel} from all
     * {@link NodeInfo}.
     * 
     * @return the {@link SelectionModel}
     */
    SelectionModel<? super T> getSelectionModel();

    /**
     * Get the value updater associated with the cell.
     * 
     * @return the value updater
     */
    ValueUpdater<T> getValueUpdater();

    /**
     * Set the view that is listening to this {@link NodeInfo}. The
     * implementation should attach the view to the source of data.
     * 
     * @param view the {@link HasData}
     */
    void setView(HasData<T> view);

    /**
     * Unset the view from the {@link NodeInfo}. The implementation should
     * detach the view from the source of data.
     */
    void unsetView();
  }

  /**
   * Get the {@link NodeInfo} that will provide the {@link ProvidesKey},
   * {@link Cell}, and {@link HasData}s to retrieve and display the children of
   * the specified value.
   * 
   * @param value the value in the parent node
   * @return the {@link NodeInfo}
   */
  <T> NodeInfo<?> getNodeInfo(T value);

  /**
   * Check if the value is known to be a leaf node.
   * 
   * @param value the value at the node
   * 
   * @return true if the node is known to be a leaf node, false otherwise
   */
  boolean isLeaf(Object value);
}
