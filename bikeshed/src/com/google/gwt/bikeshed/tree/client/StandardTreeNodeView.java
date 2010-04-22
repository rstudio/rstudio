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
import com.google.gwt.bikeshed.list.client.ListView;
import com.google.gwt.bikeshed.list.client.impl.SimpleCellListImpl;
import com.google.gwt.bikeshed.list.shared.ProvidesKey;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.SelectionModel;
import com.google.gwt.bikeshed.tree.client.TreeViewModel.NodeInfo;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.user.client.ui.UIObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A view of a tree node.
 * 
 * @param <T> the type that this view contains
 */
class StandardTreeNodeView<T> extends UIObject {

  /**
   * The element used in place of an image when a node has no children.
   */
  private static final String LEAF_IMAGE = "<div style='position:absolute;display:none;'></div>";

  /**
   * Style name applied to selected rows.
   */
  private static final String STYLENNAME_SELECTED = "gwt-tree-selectedItem";

  /**
   * Returns the element that parents the cell contents of the node.
   * 
   * @param nodeElem the element that represents the node
   * @return the cell parent within the node
   */
  private static Element getCellParent(Element nodeElem) {
    return nodeElem.getChild(1).cast();
  }

  /**
   * Show or hide an element.
   * 
   * @param element the element to show or hide
   * @param show true to show, false to hide
   */
  private static void showOrHide(Element element, boolean show) {
    if (show) {
      element.getStyle().clearDisplay();
    } else {
      element.getStyle().setDisplay(Display.NONE);
    }
  }

  /**
   * The {@link ListView} used to show children.
   * 
   * @param <C> the child item type
   */
  private static class NodeListView<C> implements ListView<C> {

    private final SimpleCellListImpl<C> impl;
    private StandardTreeNodeView<?> nodeView;
    private Map<Object, StandardTreeNodeView<?>> savedViews;

    public NodeListView(final NodeInfo<C> nodeInfo,
        final StandardTreeNodeView<?> nodeView) {
      this.nodeView = nodeView;

      impl = new SimpleCellListImpl<C>(this, nodeInfo.getCell(), 100, 50,
          nodeView.ensureChildContainer(), nodeView.emptyMessageElem,
          nodeView.showMoreElem, nodeView.showFewerElem) {

        @Override
        public void setData(List<C> values, int start) {
          // Ensure that we have a children array.
          if (nodeView.children == null) {
            nodeView.children = new ArrayList<StandardTreeNodeView<?>>();
          }

          // Construct a map of former child views based on their value keys.
          int len = values.size();
          int end = start + len;
          int childCount = nodeView.getChildCount();
          Map<Object, StandardTreeNodeView<?>> openNodes = new HashMap<Object, StandardTreeNodeView<?>>();
          for (int i = start; i < end && i < childCount; i++) {
            StandardTreeNodeView<?> child = nodeView.getChildNode(i);
            // Ignore child nodes that are closed.
            if (child.isOpen()) {
              openNodes.put(child.getValueKey(), child);
            }
          }

          // Hide the child container so we can animate it.
          if (nodeView.tree.isAnimationEnabled()) {
            nodeView.ensureAnimationFrame().getStyle().setDisplay(Display.NONE);
          }

          // Trim the saved views down to the children that still exists.
          ProvidesKey<C> providesKey = nodeInfo.getProvidesKey();
          savedViews = new HashMap<Object, StandardTreeNodeView<?>>();
          for (C childValue : values) {
            // Remove any child elements that correspond to prior children
            // so the call to setInnerHtml will not destroy them
            Object key = providesKey.getKey(childValue);
            StandardTreeNodeView<?> savedView = openNodes.remove(key);
            if (savedView != null) {
              savedView.ensureAnimationFrame().removeFromParent();
              savedViews.put(key, savedView);
            }
          }

          // Create the new cells.
          super.setData(values, start);

          // Create the child TreeNodeViews from the new elements.
          Element childElem = nodeView.ensureChildContainer().getFirstChildElement();
          for (int i = start; i < end; i++) {
            C childValue = values.get(i - start);
            StandardTreeNodeView<C> child = nodeView.createTreeNodeView(
                nodeInfo, childElem, childValue, null);
            StandardTreeNodeView<?> savedChild = savedViews.remove(providesKey.getKey(childValue));
            // Copy the saved child's state into the new child
            if (savedChild != null) {
              child.animationFrame = savedChild.animationFrame;
              child.contentContainer = savedChild.contentContainer;
              child.childContainer = savedChild.childContainer;
              child.children = savedChild.children;
              child.emptyMessageElem = savedChild.emptyMessageElem;
              child.nodeInfo = savedChild.nodeInfo;
              child.nodeInfoLoaded = savedChild.nodeInfoLoaded;
              child.open = savedChild.open;
              child.showFewerElem = savedChild.showFewerElem;
              child.showMoreElem = savedChild.showMoreElem;

              // Swap the node view in the child. We reuse the same NodeListView
              // so that we don't have to unset and register a new view with the
              // NodeInfo.
              savedChild.listView.setNodeView(child);

              // Copy the child container element to the new child
              child.getElement().appendChild(savedChild.ensureAnimationFrame());
            }

            if (childCount > i) {
              if (savedChild == null) {
                // Cleanup the child node if we aren't going to reuse it.
                nodeView.children.get(i).cleanup();
              }
              nodeView.children.set(i, child);
            } else {
              nodeView.children.add(child);
            }
            childElem = childElem.getNextSiblingElement();
          }

          // Clear temporary state.
          savedViews = null;

          // Animate the child container open.
          if (nodeView.tree.isAnimationEnabled()) {
            nodeView.tree.maybeAnimateTreeNode(nodeView);
          }
        }

        @Override
        protected void emitHtml(StringBuilder sb, List<C> values, int start,
            Cell<C, Void> cell, SelectionModel<? super C> selectionModel) {
          ProvidesKey<C> providesKey = nodeInfo.getProvidesKey();
          TreeViewModel model = nodeView.tree.getTreeViewModel();
          int imageWidth = nodeView.tree.getImageWidth();
          for (C value : values) {
            Object key = providesKey.getKey(value);
            sb.append("<div style=\"position:relative;padding-left:");
            sb.append(imageWidth);
            sb.append("px;\">");
            if (savedViews.get(key) != null) {
              sb.append(nodeView.tree.getOpenImageHtml());
            } else if (model.isLeaf(value)) {
              sb.append(LEAF_IMAGE);
            } else {
              sb.append(nodeView.tree.getClosedImageHtml());
            }
            if (selectionModel != null && selectionModel.isSelected(value)) {
              sb.append("<div class='").append(STYLENNAME_SELECTED).append("'>");
            } else {
              sb.append("<div>");
            }
            cell.render(value, null, sb);
            sb.append("</div></div>");
          }
        }

        @Override
        protected void removeLastItem() {
          StandardTreeNodeView<?> child = nodeView.children.remove(nodeView.children.size() - 1);
          child.cleanup();
          super.removeLastItem();
        }

        @Override
        protected void setSelected(Element elem, boolean selected) {
          setStyleName(getCellParent(elem), STYLENNAME_SELECTED, selected);
        }
      };
    }

    /**
     * Cleanup this node view.
     */
    public void cleanup() {
      impl.setSelectionModel(null);
    }

    public Range getRange() {
      return impl.getRange();
    }

    public void setData(int start, int length, List<C> values) {
      impl.setData(values, start);
    }

    public void setDataSize(int size, boolean isExact) {
      impl.setDataSize(size);
    }

    public void setDelegate(Delegate<C> delegate) {
      impl.setDelegate(delegate);
    }

    public void setSelectionModel(final SelectionModel<? super C> selectionModel) {
      impl.setSelectionModel(selectionModel);
    }

    /**
     * Assign this {@link ListView} to a new {@link StandardTreeNodeView}.
     * 
     * @param nodeView the new node view
     */
    private void setNodeView(StandardTreeNodeView<?> nodeView) {
      this.nodeView.listView = null;
      this.nodeView = nodeView;
      nodeView.listView = this;
    }
  }

  /**
   * True during the time a node should be animated.
   */
  private boolean animate;

  /**
   * A reference to the element that is used to animate nodes. Parent of the
   * contentContainer.
   */
  private Element animationFrame;

  /**
   * A reference to the element that contains the children. Parent to the actual
   * child nodes.
   */
  private Element childContainer;

  /**
   * A list of child views.
   */
  private List<StandardTreeNodeView<?>> children;

  /**
   * A reference to the element that contains all content. Parent of the
   * childContainer and the show/hide elements.
   */
  private Element contentContainer;

  /**
   * The element used when there are no children to display.
   */
  private Element emptyMessageElem;

  /**
   * The list view used to display the nodes.
   */
  private NodeListView<?> listView;

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
   * The parent {@link StandardTreeNodeView}.
   */
  private final StandardTreeNodeView<?> parentNode;

  /**
   * The {@link NodeInfo} of the parent node.
   */
  private final NodeInfo<T> parentNodeInfo;

  /**
   * The element used to display less children.
   */
  private Element showFewerElem;

  /**
   * The element used to display more children.
   */
  private Element showMoreElem;

  /**
   * The {@link TreeView} that this node belongs to.
   */
  private final StandardTreeView tree;

  /**
   * This node's value.
   */
  private T value;

  /**
   * Construct a {@link StandardTreeNodeView}.
   * 
   * @param tree the parent {@link StandardTreeNodeView}
   * @param parent the parent {@link StandardTreeNodeView}
   * @param parentNodeInfo the {@link NodeInfo} of the parent
   * @param elem the outer element of this {@link StandardTreeNodeView}
   * @param value the value of this node
   */
  StandardTreeNodeView(final StandardTreeView tree,
      final StandardTreeNodeView<?> parent, NodeInfo<T> parentNodeInfo,
      Element elem, T value) {
    this.tree = tree;
    this.parentNode = parent;
    this.parentNodeInfo = parentNodeInfo;
    this.value = value;
    setElement(elem);
  }

  public int getChildCount() {
    return children == null ? 0 : children.size();
  }

  public StandardTreeNodeView<?> getChildNode(int childIndex) {
    return children.get(childIndex);
  }

  /**
   * Check whether or not this node is open.
   * 
   * @return true if open, false if closed
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Select this node.
   */
  public void select() {
    SelectionModel<? super T> selectionModel = parentNodeInfo.getSelectionModel();
    if (selectionModel != null) {
      selectionModel.setSelected(value, true);
    }
  }

  /**
   * Sets whether this item's children are displayed.
   * 
   * @param open whether the item is open
   */
  public void setOpen(boolean open) {
    // Early out.
    if (this.open == open) {
      return;
    }

    this.animate = true;
    this.open = open;
    if (open) {
      if (!nodeInfoLoaded) {
        nodeInfoLoaded = true;
        nodeInfo = tree.getTreeViewModel().getNodeInfo(value);
      }

      // If we don't have any nodeInfo, we must be a leaf node.
      if (nodeInfo != null) {
        // Add a loading message.
        ensureChildContainer().setInnerHTML(tree.getLoadingHtml());
        showOrHide(showFewerElem, false);
        showOrHide(showMoreElem, false);
        showOrHide(emptyMessageElem, false);
        ensureAnimationFrame().getStyle().setProperty("display", "");
        onOpen(nodeInfo);
      }
    } else {
      cleanup();
      tree.maybeAnimateTreeNode(this);
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
      listView.cleanup();
      nodeInfo.unsetView();
      listView = null;
    }

    // Recursively kill children.
    if (children != null) {
      for (StandardTreeNodeView<?> child : children) {
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
   * Returns an instance of TreeNodeView of the same subclass as the calling
   * object.
   * 
   * @param <C> the data type of the node's children
   * @param nodeInfo a NodeInfo object describing the child nodes
   * @param childElem the DOM element used to parent the new TreeNodeView
   * @param childValue the child's value
   * @param viewData view data associated with the node
   * @return a TreeNodeView of suitable type
   */
  protected <C> StandardTreeNodeView<C> createTreeNodeView(
      NodeInfo<C> nodeInfo, Element childElem, C childValue, Void viewData) {
    return new StandardTreeNodeView<C>(tree, this, nodeInfo, childElem,
        childValue);
  }

  /**
   * Fire an event to the {@link com.google.gwt.bikeshed.cells.client.Cell}.
   * 
   * @param event the native event
   * @return true if the cell consumes the event, false if not
   */
  protected boolean fireEventToCell(NativeEvent event) {
    if (parentNodeInfo != null) {
      Element cellParent = getCellParent();
      Cell<T, Void> parentCell = parentNodeInfo.getCell();
      parentCell.onBrowserEvent(cellParent, value, null, event,
          parentNodeInfo.getValueUpdater());
      return parentCell.consumesEvents();
    }
    return false;
  }

  /**
   * Returns the element that parents the cell contents of this node.
   */
  protected Element getCellParent() {
    return getCellParent(getElement());
  }

  /**
   * Returns the element corresponding to the open/close image.
   * 
   * @return the open/close image element
   */
  protected Element getImageElement() {
    return getElement().getFirstChildElement();
  }

  /**
   * Returns the key for the value of this node using the parent's
   * implementation of NodeInfo.getKey().
   */
  protected Object getValueKey() {
    return parentNodeInfo.getProvidesKey().getKey(value);
  }

  /**
   * Set up the node when it is opened.
   * 
   * @param nodeInfo the {@link NodeInfo} that provides information about the
   *          child values
   * @param <C> the child data type of the node
   */
  protected <C> void onOpen(final NodeInfo<C> nodeInfo) {
    NodeListView<C> view = new NodeListView<C>(nodeInfo, this);
    listView = view;
    view.setSelectionModel(nodeInfo.getSelectionModel());
    nodeInfo.setView(view);
  }

  /**
   * Update the image based on the current state.
   */
  protected void updateImage() {
    // Early out if this is a root node.
    if (parentNode == null) {
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

    Element oldImg = getImageElement();
    oldImg.getParentElement().replaceChild(imageElem, oldImg);
  }

  /**
   * Ensure that the animation frame exists and return it.
   * 
   * @return the animation frame
   */
  Element ensureAnimationFrame() {
    if (animationFrame == null) {
      animationFrame = Document.get().createDivElement();
      animationFrame.getStyle().setPosition(Position.RELATIVE);
      animationFrame.getStyle().setOverflow(Overflow.HIDDEN);
      animationFrame.setId("animFrame");
      getElement().appendChild(animationFrame);
    }
    return animationFrame;
  }

  /**
   * Ensure that the child container exists and return it.
   * 
   * @return the child container
   */
  Element ensureChildContainer() {
    if (childContainer == null) {
      childContainer = Document.get().createDivElement();
      ensureContentContainer().insertFirst(childContainer);
    }
    return childContainer;
  }

  /**
   * Ensure that the content container exists and return it.
   * 
   * @return the content container
   */
  Element ensureContentContainer() {
    if (contentContainer == null) {
      contentContainer = Document.get().createDivElement();
      ensureAnimationFrame().appendChild(contentContainer);

      emptyMessageElem = Document.get().createDivElement();
      emptyMessageElem.setInnerHTML("<i>no data</i>");
      showOrHide(emptyMessageElem, false);
      contentContainer.appendChild(emptyMessageElem);

      showMoreElem = Document.get().createPushButtonElement();
      showMoreElem.setInnerText("Show more");
      showOrHide(showMoreElem, false);
      contentContainer.appendChild(showMoreElem);

      showFewerElem = Document.get().createPushButtonElement();
      showFewerElem.setInnerText("Show fewer");
      showOrHide(showFewerElem, false);
      contentContainer.appendChild(showFewerElem);
    }
    return contentContainer;
  }

  Element getShowFewerElement() {
    return showFewerElem;
  }

  Element getShowMoreElement() {
    return showMoreElem;
  }

  void showFewer() {
    listView.impl.showFewer();
  }

  void showMore() {
    listView.impl.showMore();
  }
}
