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
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListModel;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * A view of a tree node.
 */
public class TreeNodeView extends Composite {

  /**
   * A {@link TreeItem} that fires value change events when the state changes.
   */
  public static class ExtraTreeItem extends TreeItem implements
      HasValueChangeHandlers<Boolean> {

    private HandlerManager handlerManager = new HandlerManager(this);

    public ExtraTreeItem(String value) {
      super(value);
    }

    public HandlerRegistration addValueChangeHandler(
        ValueChangeHandler<Boolean> handler) {
      return handlerManager.addHandler(ValueChangeEvent.getType(), handler);
    }

    public void fireEvent(GwtEvent<?> event) {
      handlerManager.fireEvent(event);
    }

    @Override
    public void setState(boolean open, boolean fireEvents) {
      super.setState(open, fireEvents);
      if (open) {
        ValueChangeEvent.fire(this, true);
      } else {
        ValueChangeEvent.fire(this, false);
      }
    }
  }

  /**
   * The list registration for the list of children.
   */
  private ListRegistration listReg;

  /**
   * The TreeItem that displays this node.
   */
  private ExtraTreeItem treeItem;

  /**
   * Construct a {@link TreeNodeView}.
   * 
   * @param treeItem this nodes view
   * @param factory the factory used to generate child nodes
   */
  public TreeNodeView(ExtraTreeItem treeItem, final TreeNodeFactory<?> factory) {
    this.treeItem = treeItem;

    // Force a + icon if this node might have children.
    if (factory != null) {
      treeItem.addItem("loading...");
      treeItem.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
        public void onValueChange(ValueChangeEvent<Boolean> event) {
          if (event.getValue()) {
            onOpen(factory);
          } else {
            onClose();
          }
        }
      });
    }
  }

  TreeItem getTreeItem() {
    return treeItem;
  }

  /**
   * Cleanup when the node is closed.
   */
  private void onClose() {
    if (listReg != null) {
      listReg.removeHandler();
      listReg = null;
    }
  }

  /**
   * Setup the node when it is opened.
   * 
   * @param factory the factory used to generate child nodes
   * @param <C> the child data type of the node.
   */
  private <C> void onOpen(final TreeNodeFactory<C> factory) {
    ListModel<C> listModel = factory.getListModel();
    listReg = listModel.addListHandler(new ListHandler<C>() {
      public void onDataChanged(ListEvent<C> event) {
        // TODO - handle event start and length
        treeItem.removeItems();

        // Add child tree items.
        Cell<C> cell = factory.getCell();
        for (C value : event.getValues()) {
          // TODO(jlabanca): Use one StringBuilder.
          StringBuilder sb = new StringBuilder();
          cell.render(value, sb);
          ExtraTreeItem child = new ExtraTreeItem(sb.toString());
          treeItem.addItem(child);
          new TreeNodeView(child, factory.createChildFactory(value));
        }
      }

      public void onSizeChanged(SizeChangeEvent event) {
        // TODO (jlabanca): Handle case when item is over.
        int size = event.getSize();
        treeItem.removeItems();
        if (size > 0) {
          // Add a placeholder to force a + icon.
          treeItem.addItem("loading...");
        }
      }
    });
    listReg.setRangeOfInterest(0, 100);
  }
}
