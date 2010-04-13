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

import com.google.gwt.bikeshed.list.client.HasCell;
import com.google.gwt.bikeshed.list.client.ListView;
import com.google.gwt.bikeshed.list.shared.DataChanged;
import com.google.gwt.bikeshed.list.shared.ProvidesKey;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.AbstractListViewAdapter.DefaultRange;
import com.google.gwt.bikeshed.tree.client.TreeViewModel.NodeInfo;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.UIObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A view of a tree node.
 *
 * @param <T> the type that this {@link TreeNodeView} contains
 */
public abstract class TreeNodeView<T> extends UIObject implements TreeNode<T> {

  /**
   * A NodeInfo and list of data items of matching type.
   *
   * @param <C> the data type
   */
  static class SavedInfo<C> {
    NodeInfo<C> nodeInfo;
    List<C> values;
  }

  /**
   * The element used in place of an image when a node has no children.
   */
  public static final String LEAF_IMAGE = "<div style='position:absolute;display:none;'></div>";

  /**
   * True during the time a node should be animated.
   */
  private boolean animate;

  /**
   * A reference to the element that contains the children.
   */
  private Element childContainer;

  /**
   * A reference to the element that contains the children.
   */
  private List<TreeNodeView<?>> children;

  /**
   * The list registration for the list of children.
   */
  private ListView<?> listView;

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
   * The parent {@link SideBySideTreeNodeView}.
   */
  private final TreeNodeView<?> parentNode;

  /**
   * The parent {@link SideBySideTreeNodeView}.
   */
  private final NodeInfo<T> parentNodeInfo;

  /**
   * The NodeInfo for this node along with saved child data values.
   */
  private SavedInfo<?> savedInfo;

  /**
   * The info about this node.
   */
  private final TreeView tree;

  /**
   * This node's value.
   */
  private T value;

  public TreeNodeView(TreeView tree, TreeNodeView<?> parent, NodeInfo<T> parentNodeInfo, T value) {
    this.tree = tree;
    this.parentNode = parent;
    this.parentNodeInfo = parentNodeInfo;
    this.value = value;
  }

  public int getChildCount() {
    return children == null ? 0 : children.size();
  }

  public TreeNode<?> getChildNode(int childIndex) {
    return children.get(childIndex);
  }

  public TreeNodeView<?> getChildTreeNodeView(int childIndex) {
    return children.get(childIndex);
  }

  public TreeNode<?> getParentNode() {
    return parentNode;
  }

  public TreeNodeView<?> getParentTreeNodeView() {
    return parentNode;
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
   * Returns true if the node is open.
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Check if this is a root node at the top of the tree.
   *
   * @return true if a root node, false if not
   */
  public boolean isRootNode() {
    return getParentNode() == null;
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

    this.animate = true;
    this.open = open;
    if (open) {
      if (!nodeInfoLoaded) {
        nodeInfo = tree.getTreeViewModel().getNodeInfo(getValue(), this);
        nodeInfoLoaded = true;
      }

      preOpen();
      // If we don't have any nodeInfo, we must be a leaf node.
      if (nodeInfo != null) {
        onOpen(nodeInfo);
      }
    } else {
      cleanup();
      postClose();
    }

    // Update the image.
    updateImage();
  }

  /**
   * Unregister the list handler and destroy all child nodes.
   */
  protected void cleanup() {
    // Unregister the list handler.
    if (listView != null) {
      nodeInfo.unsetView();
      listView = null;
    }

    // Recursively kill children.
    if (children != null) {
      for (TreeNodeView<?> child : children) {
        child.cleanup();
      }
      children = null;
    }
  }

  protected boolean consumeAnimate() {
    boolean hasAnimate = animate;
    animate = false;
    return hasAnimate;
  }

  /**
   * Returns an instance of TreeNodeView of the same subclass as the
   * calling object.
   *
   * @param <C> the data type of the node's children
   * @param nodeInfo a NodeInfo object describing the child nodes
   * @param childElem the DOM element used to parent the new TreeNodeView
   * @param childValue the child's value
   * @param viewData view data associated with the node
   * @param idx the index of the child within its parent node
   * @return a TreeNodeView of suitable type
   */
  protected abstract <C> TreeNodeView<C> createTreeNodeView(NodeInfo<C> nodeInfo,
      Element childElem, C childValue, Void viewData, int idx);

  /**
   * Write the HTML for a list of child values into the given StringBuilder.
   *
   * @param <C> the data type of the child nodes
   * @param sb a StringBuilder to write to
   * @param childValues a List of child node values
   * @param hasCells the cells and values to use for rendering each child value
   * @param savedViews a List of TreeNodeView instances corresponding to
   *   the child values; a non-null value indicates a TreeNodeView previously
   *   associated with a given child value
   */
  protected abstract <C> void emitHtml(StringBuilder sb, List<C> childValues,
      List<HasCell<C, ?, Void>> hasCells, List<TreeNodeView<?>> savedViews);

  /**
   * Ensure that the animation frame exists and return it.
   *
   * @return the animation frame
   */
  protected Element ensureAnimationFrame() {
    return ensureChildContainer().getParentElement();
  }

  /**
   * Ensure that the child container exists and return it.
   *
   * @return the child container
   */
  protected abstract Element ensureChildContainer();

  /**
   * Fire an event to the {@link com.google.gwt.bikeshed.cells.client.Cell}.
   *
   * @param event the native event
   */
  protected void fireEventToCell(NativeEvent event) {
    if (parentNodeInfo != null) {
      Element cellParent = getCellParent();
      parentNodeInfo.onBrowserEvent(cellParent, value, event);
    } else {
      Window.alert("parentNodeInfo == null");
    }
  }

  /**
   * Returns the element that parents the cell contents of this node.
   */
  protected abstract Element getCellParent();

  /**
   * Returns the element that contains the children of this node.
   */
  protected Element getChildContainer() {
    return childContainer;
  }

  /**
   * Returns the element that contains this node.
   * The default implementation returns the value of getElement().
   */
  protected Element getContainer() {
    return getElement();
  }

  /**
   * Returns the element corresponding to the open/close image.
   */
  protected abstract Element getImageElement();

  /**
   * Returns the left position of the open/close image within a tree item node.
   * The default implementation returns 0.
   */
  protected int getImageLeft() {
    return 0;
  }

  /**
   * Returns the nodeInfo for this node's parent.
   */
  protected NodeInfo<T> getParentNodeInfo() {
    return parentNodeInfo;
  }

  /**
   * Returns the key for the value of this node using the parent's
   * implementation of NodeInfo.getKey().
   */
  protected Object getValueKey() {
    return getParentNodeInfo().getProvidesKey().getKey(getValue());
  }

  /**
   * Set up the node when it is opened.  Delegates to createMap(), emitHtml(),
   * and createTreeNodeView() for subclass-specific functionality.
   *
   * @param nodeInfo the {@link NodeInfo} that provides information about the
   *          child values
   * @param <C> the child data type of the node
   */
  protected <C> void onOpen(final NodeInfo<C> nodeInfo) {
    // Add a loading message.
    ensureChildContainer().setInnerHTML(tree.getLoadingHtml());
    ensureAnimationFrame().getStyle().setProperty("display", "");

    // Create a SavedInfo object to store the NodeInfo and data values
    final SavedInfo<C> localSavedInfo = new SavedInfo<C>();
    localSavedInfo.nodeInfo = nodeInfo;
    localSavedInfo.values = new ArrayList<C>();
    TreeNodeView.this.savedInfo = localSavedInfo;

    // Get the node info.
    final ProvidesKey<C> providesKey = nodeInfo.getProvidesKey();
    ListView<C> view = new ListView<C>() {
      public Range getRange() {
        return new DefaultRange(0, 100);
      }

      public void setData(DataChanged<C> event) {
        // TODO - handle event start and length

        int start = event.getStart();
        int end = start + event.getLength();
        // Ensure savedInfo has a place to store the data values
        while (localSavedInfo.values.size() < end) {
          savedInfo.values.add(null);
        }
        // Save child values into savedInfo
        int index = event.getStart();
        for (C childValue : event.getValues()) {
          localSavedInfo.values.set(index++, childValue);
        }

        // Construct a map of former child views based on their value keys.
        Map<Object, TreeNodeView<?>> map = new HashMap<Object, TreeNodeView<?>>();
        if (children != null) {
          for (TreeNodeView<?> child : children) {
            // Ignore child nodes that are closed
            if (child.getState()) {
              map.put(child.getValueKey(), child);
            }
          }
        }

        // Hide the child container so we can animate it.
        if (tree.isAnimationEnabled()) {
          ensureAnimationFrame().getStyle().setDisplay(Display.NONE);
        }

        List<TreeNodeView<?>> savedViews = new ArrayList<TreeNodeView<?>>();
        for (C childValue : event.getValues()) {
          // Remove any child elements that correspond to prior children
          // so the call to setInnerHtml will not destroy them
          TreeNodeView<?> savedView = map.get(providesKey.getKey(childValue));
          if (savedView != null) {
            savedView.getContainer().getFirstChild().removeFromParent();
          }
          savedViews.add(savedView);
        }

        // Construct the child contents.
        StringBuilder sb = new StringBuilder();
        emitHtml(sb, event.getValues(), nodeInfo.getHasCells(), savedViews);
        childContainer.setInnerHTML(sb.toString());

        // Create the child TreeNodeViews from the new elements.
        children = new ArrayList<TreeNodeView<?>>();
        Element childElem = childContainer.getFirstChildElement();
        int idx = 0;
        for (C childValue : event.getValues()) {
          TreeNodeView<C> child = createTreeNodeView(nodeInfo, childElem,
              childValue, null, idx);
          TreeNodeView<?> savedChild = map.get(providesKey.getKey(childValue));
          // Copy the saved child's state into the new child
          if (savedChild != null) {
            child.children = savedChild.children;
            child.childContainer = savedChild.childContainer;
            child.nodeInfo = savedChild.nodeInfo;
            child.nodeInfoLoaded = savedChild.nodeInfoLoaded;
            child.open = savedChild.getState();

            // Copy the child container element to the new child
            // TODO(rice) clean up this expression
            child.getContainer().appendChild(savedChild.childContainer.getParentElement());
          }

          children.add(child);
          childElem = childElem.getNextSiblingElement();

          idx++;
        }

        // Animate the child container open.
        if (tree.isAnimationEnabled()) {
          tree.maybeAnimateTreeNode(TreeNodeView.this);
        }
      }

      public void setDelegate(ListView.Delegate<C> delegate) {
        // Range never actually changes so no need to store the delegate
        if (delegate != null) {
          delegate.onRangeChanged(this);
        }
      }

      public void setSize(int size, boolean exact) {
        if (size == 0 && exact) {
          ensureChildContainer().setInnerHTML("<i>no data</i>");
          if (children != null) {
            for (TreeNodeView<?> child : children) {
              child.cleanup();
            }
            children = null;
          }
        }
      }
    };
    nodeInfo.setView(view);
    this.listView = view;
  }

  /**
   * Called from setState(boolean, boolean) following the call to cleanup().
   */
  protected void postClose() {
  }

  /**
   * Called from setState(boolean, boolean) prior to the call to onOpen().
   */
  protected void preOpen() {
  }

  /**
   * Refresh any cells that depend on the selection state. The default
   * implementation works for {@link StandardTreeNodeView} and
   * {@link SideBySideTreeNodeView}; other subclasses will need to implement
   * their own versions of this method.
   */
  protected void refreshSelection() {
    refreshSelection(savedInfo);
  }

  protected void setChildContainer(Element childContainer) {
    this.childContainer = childContainer;
  }

  /**
   * Update the image based on the current state.
   */
  protected void updateImage() {
    // Early out if this is a root node.
    if (isRootNode()) {
      return;
    }

    // Replace the image element with a new one.
    int imageLeft = getImageLeft();
    String html = open ? tree.getOpenImageHtml(imageLeft) : tree.getClosedImageHtml(imageLeft);
    if (nodeInfoLoaded && nodeInfo == null) {
      html = LEAF_IMAGE;
    }
    Element tmp = Document.get().createDivElement();
    tmp.setInnerHTML(html);
    Element imageElem = tmp.getFirstChildElement();
    getElement().replaceChild(imageElem, getImageElement());
  }

  TreeView getTree() {
    return tree;
  }

  /**
   * Helper method to render the contents of a cell without needing to know
   * the type of the cell.
   *
   * @param <C> the child value type
   * @param <X> the cell type
   * @param sb a StringBuilder instance
   * @param childValue the child value
   * @param hasCell a HasCell instance
   */
  <C, X> void render(StringBuilder sb, C childValue,
      HasCell<C, X, Void> hasCell) {
    hasCell.getCell().render(hasCell.getValue(childValue), null, sb);
  }

  /**
   * Refresh any cells that depend on the selection state.  Note that this
   * implementation is dependent on the particular DOM structure used by
   * {@link StandardTreeNodeView} and {@link SideBySideTreeNodeView}.
   *
   * @param <C> the child data type
   * @param savedInfo a SavedInfo object containing a NodeInfo instance
   * and a list of saved child data values
   */
  private <C> void refreshSelection(SavedInfo<C> savedInfo) {
    List<HasCell<C, ?, Void>> hasCells = savedInfo.nodeInfo.getHasCells();
    Element outerDiv = childContainer.getFirstChildElement();
    int index = 0;
    while (outerDiv != null) {
      C childValue = savedInfo.values.get(index);
      Element span = outerDiv.getFirstChildElement().getNextSiblingElement().getFirstChildElement();

      for (HasCell<C, ?, Void> hasCell : hasCells) {
        if (hasCell.dependsOnSelection()) {
          StringBuilder sb = new StringBuilder();
          render(sb, childValue, hasCell);
          span.setInnerHTML(sb.toString());
        }
        span = span.getNextSiblingElement();
      }
      outerDiv = outerDiv.getNextSibling().cast();
      index++;
    }
  }
}
