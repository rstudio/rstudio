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

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.sample.tree.shared.TreeNode;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.HashMap;

/**
 * A tree view.
 * 
 * @param <T> the data type of each tree node.
 */
public class TreeView<T> extends Composite {
  
  private HashMap<TreeItem, TreeNode<T>> nodeMap =
    new HashMap<TreeItem, TreeNode<T>>();
  private Tree tree;
  
  public TreeView(TreeNode<T> root) {
    tree = new Tree();
    tree.setAnimationEnabled(true);
    
    TreeItem rootItem = new TreeItem("root");
    nodeMap.put(rootItem, root);
    rootItem.addItem("");
    tree.addItem(rootItem);

    addHandler(rootItem).setRangeOfInterest(0, 10);
    
    tree.addOpenHandler(new OpenHandler<TreeItem>() {
      public void onOpen(OpenEvent<TreeItem> event) {
        TreeItem treeItem = event.getTarget();
        addHandler(treeItem).setRangeOfInterest(0, 10);
      }
    });
    tree.addCloseHandler(new CloseHandler<TreeItem>() {
      public void onClose(CloseEvent<TreeItem> event) {
        // TODO - remove list handler
      }
    });
    initWidget(tree);
  }

  protected TreeNode<T> getNode(TreeItem treeItem) {
    return nodeMap.get(treeItem);
  }

  private ListRegistration addHandler(final TreeItem item) {
    TreeNode<T> node = getNode(item);
    return node.addListHandler(new ListHandler<TreeNode<T>>() {
      public void onDataChanged(ListEvent<TreeNode<T>> event) {
        int idx = event.getStart();
        for (TreeNode<T> node : event.getValues()) {
          TreeItem treeItem = item.getChild(idx++);
          nodeMap.put(treeItem, node);
          treeItem.setText(node.getNodeData().toString());
        }
      }

      public void onSizeChanged(SizeChangeEvent event) {
        int size = event.getSize();
        item.removeItems();
        for (int i = 0; i < size; i++) {
          TreeItem child = new TreeItem("");
          item.addItem(child);
          child.addItem("");
        }
      }
    });
  }
}
