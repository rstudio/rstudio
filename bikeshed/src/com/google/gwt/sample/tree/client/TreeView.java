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

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.sample.tree.client.TreeNodeView.ExtraTreeItem;
import com.google.gwt.sample.tree.client.TreeViewModel.NodeInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.ArrayList;

/**
 * A view of a tree.
 */
public class TreeView extends Composite {

  /**
   * The {@link TreeViewModel} that backs the tree.
   */
  private TreeViewModel model;

  /**
   * The hidden root node in the tree.
   */
  private TreeNodeView<?> rootNode;

  /**
   * Construct a new {@link TreeView}.
   * 
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   */
  public <T> TreeView(TreeViewModel viewModel, T rootValue) {
    this.model = viewModel;

    // Initialize the widget.
    Tree tree = new Tree() {
      @Override
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

        switch (event.getTypeInt()) {
          case Event.ONMOUSEUP: {
            if ((DOM.eventGetCurrentTarget(event) == getElement())
                && (event.getButton() == Event.BUTTON_LEFT)) {
              elementClicked(DOM.eventGetTarget(event), event);
            }
            break;
          }
        }
      }
    };
    initWidget(tree);
    sinkEvents(Event.ONMOUSEUP);

    // Add the root item.
    ExtraTreeItem rootItem = new ExtraTreeItem("Dummy UI Root");
    tree.addItem(rootItem);

    // Associate a view with the item.
    rootNode = createChildView(rootValue, null, rootItem, null);
  }

  public TreeViewModel getTreeViewModel() {
    return model;
  }

  /**
   * Create a child view for this tree.
   * 
   * @param <C> the data type of the child
   * @param childValue the value of the child
   * @param parentTreeNodeView the parent {@link TreeNodeView}
   * @param childItem the DOM view of the child
   * @return a {@link TreeNodeView} for the child
   */
  <C> TreeNodeView<C> createChildView(C childValue,
      TreeNodeView<?> parentTreeNodeView, ExtraTreeItem childItem,
      NodeInfo<C> parentNodeInfo) {
    TreeNodeView<C> childView = new TreeNodeView<C>(childValue, this,
        parentTreeNodeView, childItem, parentNodeInfo);
    NodeInfo<?> nodeInfo = model.getNodeInfo(childValue, childView);
    childView.initNodeInfo(nodeInfo);
    return childView;
  }

  /**
   * Collects parents going up the element tree, terminated at the tree root.
   */
  private void collectElementChain(ArrayList<Element> chain, Element hRoot,
      Element hElem) {
    if ((hElem == null) || (hElem == hRoot)) {
      return;
    }

    collectElementChain(chain, hRoot, DOM.getParent(hElem));
    chain.add(hElem);
  }

  private boolean elementClicked(Element hElem, NativeEvent event) {
    ArrayList<Element> chain = new ArrayList<Element>();
    collectElementChain(chain, getElement(), hElem);

    TreeNodeView<?> nodeView = findItemByChain(chain, 0, rootNode);
    if (nodeView != null && nodeView != rootNode) {
      TreeItem item = nodeView.getTreeItem();
      if (DOM.isOrHasChild(item.getElement(), hElem)) {
        fireEvent(nodeView, event);
        return true;
      }
    }

    return false;
  }

  private TreeNodeView<?> findItemByChain(ArrayList<Element> chain, int idx,
      TreeNodeView<?> parent) {
    if (idx == chain.size()) {
      return parent;
    }

    Element hCurElem = chain.get(idx);
    for (int i = 0, n = parent.getChildCount(); i < n; ++i) {
      TreeNodeView<?> child = parent.getChild(i);
      if (child.getTreeItem().getElement() == hCurElem) {
        TreeNodeView<?> retItem = findItemByChain(chain, idx + 1, child);
        if (retItem == null) {
          return child;
        }
        return retItem;
      }
    }

    return findItemByChain(chain, idx + 1, parent);
  }

  private <T> void fireEvent(TreeNodeView<T> nodeView, NativeEvent event) {
    nodeView.getParentNodeInfo().onBrowserEvent(
        nodeView.getTreeItem().getElement(), nodeView.getValue(), event);
  }
}
