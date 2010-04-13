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
import com.google.gwt.bikeshed.list.client.ListView;
import com.google.gwt.bikeshed.list.shared.AbstractListViewAdapter;
import com.google.gwt.bikeshed.list.shared.ProvidesKey;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A model of a tree.
 */
public interface TreeViewModel {

  /**
   * Default implementation of {@link NodeInfo}.
   */
  class DefaultNodeInfo<T> implements NodeInfo<T> {
    private List<HasCell<T, ?, Void>> hasCells = new ArrayList<HasCell<T, ?, Void>>();
    private AbstractListViewAdapter<T> listViewAdapter;
    private ListView<T> view;

    /**
     * Construct a new {@link DefaultNodeInfo}.
     *
     * @param adapter the {@link AbstractListViewAdapter} that provides the
     *          child values
     * @param cell the {@link Cell} used to render the child values
     * @param dependsOnSelection TODO
     */
    public DefaultNodeInfo(AbstractListViewAdapter<T> adapter,
        Cell<T, Void> cell, boolean dependsOnSelection) {
      this(adapter, cell, dependsOnSelection, null);
    }

    /**
     * Construct a new {@link DefaultNodeInfo} with a single cell and a
     * {@link ValueUpdater}.
     *
     * @param adapter the {@link AbstractListViewAdapter} that provides the
     *          child values
     * @param cell the {@link Cell} used to render the child values
     * @param dependsOnSelection TODO
     * @param valueUpdater the {@link ValueUpdater}
     */
    public DefaultNodeInfo(AbstractListViewAdapter<T> adapter,
        final Cell<T, Void> cell, final boolean dependsOnSelection,
        final ValueUpdater<T, Void> valueUpdater) {
      hasCells.add(new HasCell<T, T, Void>() {
        public boolean dependsOnSelection() {
          return dependsOnSelection;
        }

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
      this.listViewAdapter = adapter;
    }

    public DefaultNodeInfo(AbstractListViewAdapter<T> adapter,
        List<HasCell<T, ?, Void>> hasCells) {
      this.hasCells.addAll(hasCells);
      this.listViewAdapter = adapter;
    }

    public List<HasCell<T, ?, Void>> getHasCells() {
      return hasCells;
    }

    public ProvidesKey<T> getProvidesKey() {
      return listViewAdapter;
    }

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

    public void setView(ListView<T> view) {
      this.view = view;
      listViewAdapter.addView(view);
    }

    public void unsetView() {
      listViewAdapter.removeView(view);
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
   * The info needed to create a {@link TreeNodeView}.
   */
  interface NodeInfo<T> {

    List<HasCell<T, ?, Void>> getHasCells();

    ProvidesKey<T> getProvidesKey();

    /**
     * Handle an event that is fired on one of the children of this item.
     *
     * @param elem the parent element of the item
     * @param object the data value of the item
     * @param event the event that was fired
     */
    void onBrowserEvent(Element elem, final T object, NativeEvent event);

    void setView(ListView<T> view);

    void unsetView();
  }

  /**
   * Get the {@link NodeInfo} that will provide the {@link ProvidesKey},
   * {@link Cell}, and {@link ListView}s to retrieve and display the children of
   * the specified value.
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
