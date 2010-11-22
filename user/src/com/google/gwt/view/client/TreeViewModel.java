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
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * A model of a tree.
 */
public interface TreeViewModel {

  /**
   * Default implementation of {@link NodeInfo}.
   */
  class DefaultNodeInfo<T> implements NodeInfo<T> {

    private final Cell<T> cell;
    private final AbstractDataProvider<T> dataProvider;
    private final CellPreviewEvent.Handler<T> selectionEventManager;
    private HandlerRegistration selectionEventManagerReg;
    private final SelectionModel<? super T> selectionModel;
    private final ValueUpdater<T> valueUpdater;
    private HasData<T> display;

    /**
     * Construct a new {@link DefaultNodeInfo}.
     * 
     * @param dataProvider the {@link AbstractDataProvider} that provides the
     *          child values
     * @param cell the {@link Cell} used to render the child values
     */
    public DefaultNodeInfo(AbstractDataProvider<T> dataProvider, Cell<T> cell) {
      this(dataProvider, cell, null, null);
    }

    /**
     * Construct a new {@link DefaultNodeInfo}.
     * 
     * @param dataProvider the {@link AbstractDataProvider} that provides the
     *          child values
     * @param cell the {@link Cell} used to render the child values update when
     *          the selection changes
     * @param selectionModel the {@link SelectionModel} used for selection
     * @param valueUpdater the {@link ValueUpdater}
     */
    public DefaultNodeInfo(AbstractDataProvider<T> dataProvider,
        final Cell<T> cell, SelectionModel<? super T> selectionModel,
        final ValueUpdater<T> valueUpdater) {
      this(dataProvider, cell, selectionModel,
          DefaultSelectionEventManager.<T> createDefaultManager(), valueUpdater);
    }

    /**
     * Construct a new {@link DefaultNodeInfo}.
     * 
     * @param dataProvider the {@link AbstractDataProvider} that provides the
     *          child values
     * @param cell the {@link Cell} used to render the child values update when
     *          the selection changes
     * @param selectionModel the {@link SelectionModel} used for selection
     * @param selectionEventManager the {@link CellPreviewEvent.Handler} that
     *          handles user selection interaction
     * @param valueUpdater the {@link ValueUpdater}
     */
    public DefaultNodeInfo(AbstractDataProvider<T> dataProvider,
        final Cell<T> cell, SelectionModel<? super T> selectionModel,
        CellPreviewEvent.Handler<T> selectionEventManager,
        final ValueUpdater<T> valueUpdater) {
      this.dataProvider = dataProvider;
      this.cell = cell;
      this.selectionModel = selectionModel;
      this.valueUpdater = valueUpdater;
      this.selectionEventManager = selectionEventManager;
    }

    public Cell<T> getCell() {
      return cell;
    }

    public ProvidesKey<T> getProvidesKey() {
      return dataProvider;
    }

    public SelectionModel<? super T> getSelectionModel() {
      return selectionModel;
    }

    public ValueUpdater<T> getValueUpdater() {
      return valueUpdater;
    }

    public void setDataDisplay(HasData<T> display) {
      this.display = display;
      if (selectionEventManager != null) {
        selectionEventManagerReg = display.addCellPreviewHandler(selectionEventManager);
      }
      dataProvider.addDataDisplay(display);
    }

    public void unsetDataDisplay() {
      if (display != null) {
        dataProvider.removeDataDisplay(display);
        if (selectionEventManagerReg != null) {
          selectionEventManagerReg.removeHandler();
          selectionEventManagerReg = null;
        }
        display = null;
      }
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
     * Set the display that is listening to this {@link NodeInfo}. The
     * implementation should attach the display to the source of data.
     * 
     * @param display the {@link HasData}
     */
    void setDataDisplay(HasData<T> display);

    /**
     * Unset the display from the {@link NodeInfo}. The implementation should
     * detach the display from the source of data.
     */
    void unsetDataDisplay();
  }

  /**
   * Get the {@link NodeInfo} that will provide the {@link ProvidesKey},
   * {@link Cell}, and {@link HasData} instances to retrieve and display the
   * children of the specified value.
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
