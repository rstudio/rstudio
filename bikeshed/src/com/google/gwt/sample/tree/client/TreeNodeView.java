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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListModel;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.sample.tree.client.TreeViewModel.NodeInfo;
import com.google.gwt.user.client.ui.Composite;

import java.util.ArrayList;
import java.util.List;

/**
 * A view of a tree node.
 * 
 * @param <T> the type that this {@link TreeNodeView} contains
 */
public class TreeNodeView<T> extends Composite {

  /**
   * The element used in place of an image when a node has no children.
   */
  private static final String LEAF_IMAGE = "<div style='position:absolute;display:none;'></div>";

  /**
   * The children of this {@link TreeNodeView}.
   */
  private List<TreeNodeView<?>> children;

  /**
   * A reference to the element that contains the children.
   */
  private Element childContainer;

  /**
   * The list registration for the list of children.
   */
  private ListRegistration listReg;

  /**
   * The info about children of this node.
   */
  private NodeInfo<?> nodeInfo;

  /**
   * Indicates whether or not we've loaded the node info.
   */
  private boolean nodeInfoLoaded;

  /**
   * Indicates whether or not this node is open.
   */
  private boolean open;

  /**
   * The parent {@link TreeNodeView}.
   */
  private TreeNodeView<?> parent;

  /**
   * The info about this node.
   */
  private NodeInfo<T> parentNodeInfo;

  /**
   * The containing {@link TreeView}.
   */
  private TreeView tree;

  /**
   * This node's value.
   */
  private T value;

  /**
   * Construct a {@link TreeNodeView}.
   * 
   * @param tree the parent {@link TreeView}
   * @param parent the parent {@link TreeNodeView}
   * @param parentNodeInfo the {@link NodeInfo} of the parent
   * @param elem the outer element of this {@link TreeNodeView}.
   * @param value the value of this node
   */
  TreeNodeView(final TreeView tree, final TreeNodeView<?> parent,
      NodeInfo<T> parentNodeInfo, Element elem, T value) {
    this.value = value;
    this.tree = tree;
    this.parent = parent;
    // We pass in parentNodeInfo so we know that it is type T.
    this.parentNodeInfo = parentNodeInfo;
    setElement(elem);
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
   * Check whether or not this {@link TreeNodeView} is open.
   * 
   * @return true if open, false if closed
   */
  public boolean getState() {
    return open;
  }

  /**
   * Get the value contained in this node.
   * 
   * @return the value of the node
   */
  public T getValue() {
    return value;
  }

  /**
   * Sets whether this item's children are displayed.
   * 
   * @param open whether the item is open
   */
  public void setState(boolean open) {
    setState(open, true);
  }

  /**
   * Sets whether this item's children are displayed.
   * 
   * @param open whether the item is open
   * @param fireEvents <code>true</code> to allow open/close events to be
   */
  public void setState(boolean open, boolean fireEvents) {
    // TODO(jlabanca) - allow people to add open/close handlers.

    // Early out.
    if (this.open == open) {
      return;
    }

    this.open = open;
    if (open) {
      if (!nodeInfoLoaded) {
        nodeInfo = tree.getTreeViewModel().getNodeInfo(value, this);
        nodeInfoLoaded = true;
      }
      if (nodeInfo != null) {
        onOpen(nodeInfo);
      }
    } else {
      // Unregister the list handler.
      if (listReg != null) {
        listReg.removeHandler();
        listReg = null;
      }

      // Remove the children.
      childContainer.setInnerHTML("");
      children.clear();
    }

    // Update the image.
    updateImage();
  }

  /**
   * Fire an event to the {@link Cell}.
   * 
   * @param event the native event
   */
  void fireEventToCell(NativeEvent event) {
    parentNodeInfo.onBrowserEvent(getCellParent(), value, event);
  }

  /**
   * @return the element that contains the rendered cell
   */
  Element getCellParent() {
    return getElement().getChild(1).cast();
  }

  /**
   * @return the image element
   */
  Element getImageElement() {
    return getElement().getFirstChildElement();
  }

  NodeInfo<T> getParentNodeInfo() {
    return parentNodeInfo;
  }

  /**
   * Set the {@link Element} that will contain the children. Used by
   * {@link TreeView}.
   * 
   * @param elem the child container element
   */
  void initChildContainer(Element elem) {
    assert this.childContainer == null : "childContainer already initialized.";
    this.childContainer = elem;
  }

  /**
   * Setup the node when it is opened.
   * 
   * @param nodeInfo the {@link NodeInfo} that provides information about the
   *          child values
   * @param <C> the child data type of the node.
   */
  private <C> void onOpen(final NodeInfo<C> nodeInfo) {
    // Get the node info.
    ListModel<C> listModel = nodeInfo.getListModel();
    listReg = listModel.addListHandler(new ListHandler<C>() {
      public void onDataChanged(ListEvent<C> event) {
        // TODO - handle event start and length

        // Construct the child contents.
        TreeViewModel model = tree.getTreeViewModel();
        int imageWidth = tree.getImageWidth();
        Cell<C> theCell = nodeInfo.getCell();
        StringBuilder sb = new StringBuilder();
        children = new ArrayList<TreeNodeView<?>>();
        for (C childValue : event.getValues()) {
          sb.append("<div style=\"position:relative;padding-left:");
          sb.append(imageWidth);
          sb.append("px;\">");
          if (model.isLeaf(childValue)) {
            sb.append(LEAF_IMAGE);
          } else {
            sb.append(tree.getClosedImageHtml());
          }
          sb.append("<div>");
          theCell.render(childValue, sb);
          sb.append("</div>");
          sb.append("</div>");
        }

        // Replace contents of the child container.
        if (childContainer == null) {
          Element elem = getElement();
          initChildContainer(Document.get().createDivElement());
          elem.appendChild(childContainer);
        }
        childContainer.setInnerHTML(sb.toString());

        // Create the child TreeNodeViews from the new elements.
        children = new ArrayList<TreeNodeView<?>>();
        Element childElem = childContainer.getFirstChildElement();
        for (C childValue : event.getValues()) {
          TreeNodeView<C> child = new TreeNodeView<C>(tree, TreeNodeView.this,
              nodeInfo, childElem, childValue);
          children.add(child);
          childElem = childElem.getNextSiblingElement();
        }
      }

      public void onSizeChanged(SizeChangeEvent event) {
        if (children == null) {
          return;
        }

        // Shrink the list based on the new size.
        int size = event.getSize();
        int currentSize = children.size();
        for (int i = currentSize - 1; i >= size; i--) {
          childContainer.getLastChild().removeFromParent();
          children.remove(i);
        }
      }
    });
    listReg.setRangeOfInterest(0, 100);
  }

  /**
   * Update the image based on the current state.
   */
  private void updateImage() {
    // Early out if this is a root node.
    if (getParentTreeNodeView() == null) {
      return;
    }

    // Replace the image element with a new one.
    String html = open ? tree.getOpenImageHtml() : tree.getClosedImageHtml();
    if (nodeInfoLoaded && nodeInfo == null) {
      html = LEAF_IMAGE;
    }
    Element tmp = Document.get().createDivElement();
    tmp.setInnerHTML(html);
    Element imageElem = tmp.getFirstChildElement();
    getElement().replaceChild(imageElem, getImageElement());
  }
}
