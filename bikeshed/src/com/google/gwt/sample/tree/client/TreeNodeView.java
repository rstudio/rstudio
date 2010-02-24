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
import com.google.gwt.sample.tree.client.TreeViewModel.NodeInfo;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A view of a tree node.
 * 
 * @param <T> the type that this {@link TreeNodeView} contains
 */
public class TreeNodeView<T> extends Composite {

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
   * This node's value.
   */
  private T value;

  /**
   * The parent {@link TreeNodeView}.
   */
  private TreeNodeView<?> parent;

  /**
   * The containing {@link TreeView}.
   */
  private TreeView tree;

  /**
   * The children of this {@link TreeNodeView}.
   */
  private List<TreeNodeView<?>> children;

  /**
   * The info about the child nodes.
   */
  private NodeInfo<?> nodeInfo;

  /**
   * The info about this node.
   */
  private NodeInfo<T> parentNodeInfo;

  /**
   * Construct a {@link TreeNodeView}.
   * 
   * @param value the value of this node
   * @param tree the parent {@link TreeView}
   * @param treeItem this nodes view
   */
  TreeNodeView(T value, final TreeView tree, final TreeNodeView<?> parent,
      ExtraTreeItem treeItem, NodeInfo<T> parentNodeInfo) {
    this.value = value;
    this.tree = tree;
    this.parent = parent;
    this.treeItem = treeItem;
    this.parentNodeInfo = parentNodeInfo;
  }

  /**
   * Get the child at the specified index.
   * 
   * @return the child node
   */
  public TreeNodeView<?> getChild(int index) {
    if ((index < 0) || (index >= getChildCount())) {
      return null;
    }
    return children.get(index);
  }

  /**
   * Get the number of children under this node.
   * 
   * @return the child count
   */
  public int getChildCount() {
    return children == null ? 0 : children.size();
  }

  /**
   * Get the parent {@link TreeNodeView}.
   */
  public TreeNodeView<?> getParentTreeNodeView() {
    return parent;
  }

  /**
   * Get the value contained in this node.
   * 
   * @return the value of the node
   */
  public T getValue() {
    return value;
  }

  NodeInfo<?> getNodeInfo() {
    return nodeInfo;
  }

  NodeInfo<T> getParentNodeInfo() {
    return parentNodeInfo;
  }

  TreeItem getTreeItem() {
    return treeItem;
  }

  /**
   * Initialize the node info.
   * 
   * @param nodeInfo the {@link NodeInfo} that provides information about the
   *          child values
   */
  void initNodeInfo(final NodeInfo<?> nodeInfo) {
    // Force a + icon if this node might have children.
    if (nodeInfo != null) {
      this.nodeInfo = nodeInfo;
      treeItem.addItem("loading...");
      treeItem.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
        public void onValueChange(ValueChangeEvent<Boolean> event) {
          if (event.getValue()) {
            onOpen(tree, nodeInfo);
          } else {
            onClose();
          }
        }
      });
    }
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
   * @param tree the parent {@link TreeView}
   * @param nodeInfo the {@link NodeInfo} that provides information about the
   *          child values
   * @param <C> the child data type of the node.
   */
  private <C> void onOpen(final TreeView tree, final NodeInfo<C> nodeInfo) {
    ListModel<C> listModel = nodeInfo.getListModel();
    listReg = listModel.addListHandler(new ListHandler<C>() {
      public void onDataChanged(ListEvent<C> event) {
        // TODO - handle event start and length
        treeItem.removeItems();

        // Add child tree items.
        Cell<C> theCell = nodeInfo.getCell();
        children = new ArrayList<TreeNodeView<?>>();
        for (C childValue : event.getValues()) {
          // TODO(jlabanca): Use one StringBuilder.
          StringBuilder sb = new StringBuilder();
          theCell.render(childValue, sb);
          ExtraTreeItem child = new ExtraTreeItem(sb.toString());
          treeItem.addItem(child);
          children.add(tree.createChildView(childValue, TreeNodeView.this,
              child, nodeInfo));
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
