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
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.list.client.HasCell;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A model of a tree.
 */
public interface TreeViewModel {

  /**
   * The info needed to create a {@link TreeNodeView}.
   */
  interface NodeInfo<T> {

    List<HasCell<T, ?, Void>> getHasCells();

    /**
     * Get the {@link ListModel} used to retrieve child node values.
     * 
     * @return the list model
     */
    ListModel<T> getListModel();

    /**
     * Handle an event that is fired on one of the children of this item.
     * 
     * @param elem the parent element of the item
     * @param object the data value of the item
     * @param event the event that was fired
     */
    void onBrowserEvent(Element elem, final T object, NativeEvent event);
  }

  /**
   * Default implementation of {@link NodeInfo}.
   */
  class DefaultNodeInfo<T> implements NodeInfo<T> {
    private List<HasCell<T, ?, Void>> hasCells = new ArrayList<HasCell<T, ?, Void>>();
    private ListModel<T> listModel;

    /**
     * Construct a new {@link DefaultNodeInfo} with a single cell and a
     * {@link ValueUpdater}.
     * 
     * @param listModel the {@link ListModel} that provides the child values
     * @param cell the {@link Cell} used to render the child values
     * @param valueUpdater the {@link ValueUpdater}
     */
    public DefaultNodeInfo(ListModel<T> listModel, final Cell<T, Void> cell,
        final ValueUpdater<T, Void> valueUpdater) {
      hasCells.add(new HasCell<T, T, Void>() {
        public Cell<T, Void> getCell() {
          return cell;
        }

        public FieldUpdater<T, T, Void> getFieldUpdater() {
          return valueUpdater == null ? null : new FieldUpdater<T, T, Void>() {
            public void update(int index, T object, T value, Void viewData) {
              valueUpdater.update(value, viewData);
            }
          };
        }

        public T getValue(T object) {
          return object;
        }
      });
      this.listModel = listModel;
    }

    /**
     * Construct a new {@link DefaultNodeInfo}.
     * 
     * @param listModel the {@link ListModel} that provides the child values
     * @param cell the {@link Cell} used to render the child values
     */
    public DefaultNodeInfo(ListModel<T> listModel, Cell<T, Void> cell) {
      this(listModel, cell, null);
    }

    public DefaultNodeInfo(ListModel<T> listModel,
        List<HasCell<T, ?, Void>> hasCells) {
      this.hasCells.addAll(hasCells);
      this.listModel = listModel;
    }

    public List<HasCell<T, ?, Void>> getHasCells() {
      return hasCells;
    }

    public ListModel<T> getListModel() {
      return listModel;
    }

    // TODO - dispatch into cells
    public void onBrowserEvent(Element elem, final T object, NativeEvent event) {
      Element target = event.getEventTarget().cast();
      String idxString = "";
      while ((target != null)
          && ((idxString = target.getAttribute("__idx")).length() == 0)) {
        target = target.getParentElement();
      }
      if (idxString.length() > 0) {
        int idx = Integer.parseInt(idxString);
        dispatch(target, object, event, hasCells.get(idx));
      }
    }

    private <X> void dispatch(Element target, final T object,
        NativeEvent event, HasCell<T, X, Void> hc) {
      final FieldUpdater<T, X, Void> fieldUpdater = hc.getFieldUpdater();
      hc.getCell().onBrowserEvent(target, hc.getValue(object), null, event,
          fieldUpdater == null ? null : new ValueUpdater<X, Void>() {
            public void update(X value, Void viewData) {
              fieldUpdater.update(0, object, value, viewData);
            }
          });
    }
  }

  /**
   * Get the {@link NodeInfo} that will provide the {@link ListModel} and
   * {@link Cell} to retrieve the children of the specified value.
   * 
   * @param value the value in the parent node
   * @param treeNode the {@link TreeNode} that contains the value
   * @return the {@link NodeInfo}
   */
  <T> NodeInfo<?> getNodeInfo(T value, TreeNode<T> treeNode);

  /**
   * Check if the value is known to be a leaf node.
   * 
   * @param value the value at the node
   * @param treeNode the {@link TreeNode} that contains the value
   * 
   * @return true if the node is known to be a leaf node, false otherwise
   */
  boolean isLeaf(Object value, TreeNode<?> treeNode);
}
