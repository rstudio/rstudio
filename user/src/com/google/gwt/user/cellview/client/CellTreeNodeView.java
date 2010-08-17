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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.HasDataPresenter.ElementIterator;
import com.google.gwt.user.cellview.client.HasDataPresenter.LoadingState;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.gwt.view.client.TreeViewModel.NodeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A view of a tree node.
 *
 * @param <T> the type that this view contains
 */
class CellTreeNodeView<T> extends UIObject {

  /**
   * The element used in place of an image when a node has no children.
   */
  private static final String LEAF_IMAGE =
      "<div style='position:absolute;display:none;'></div>";

  /**
   * The temporary element used to render child items.
   */
  private static com.google.gwt.user.client.Element tmpElem;

  /**
   * Returns the element that parents the cell contents of the node.
   *
   * @param nodeElem the element that represents the node
   * @return the cell parent within the node
   */
  private static Element getCellParent(Element nodeElem) {
    return getSelectionElement(nodeElem).getFirstChildElement().getChild(
        1).cast();
  }

  /**
   * Returns the element that selection is applied to.
   *
   * @param nodeElem the element that represents the node
   * @return the cell parent within the node
   */
  private static Element getImageElement(Element nodeElem) {
    return getSelectionElement(nodeElem).getFirstChildElement().getFirstChildElement();
  }

  /**
   * Returns the element that selection is applied to.
   *
   * @param nodeElem the element that represents the node
   * @return the cell parent within the node
   */
  private static Element getSelectionElement(Element nodeElem) {
    return nodeElem.getFirstChildElement();
  }

  /**
   * @return the temporary element used to create elements
   */
  private static com.google.gwt.user.client.Element getTmpElem() {
    if (tmpElem == null) {
      tmpElem = Document.get().createDivElement().cast();
    }
    return tmpElem;
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
   * The {@link com.google.gwt.view.client.HasData} used to show children.
   *
   * @param <C> the child item type
   */
  private static class NodeCellList<C> implements HasData<C> {

    private HandlerManager handlerManger = new HandlerManager(this);

    /**
     * The view used by the NodeCellList.
     */
    private class View implements HasDataPresenter.View<C> {

      private final Element childContainer;

      public View(Element childContainer) {
        this.childContainer = childContainer;
      }

      public <H extends EventHandler> HandlerRegistration addHandler(
          H handler, Type<H> type) {
        return handlerManger.addHandler(type, handler);
      }

      public boolean dependsOnSelection() {
        return cell.dependsOnSelection();
      }

      public int getChildCount() {
        return childContainer.getChildCount();
      }

      public ElementIterator getChildIterator() {
        return new HasDataPresenter.DefaultElementIterator(
            this, childContainer.getFirstChildElement());
      }

      public void onUpdateSelection() {
      }

      public void render(StringBuilder sb, List<C> values, int start,
          SelectionModel<? super C> selectionModel) {
        // Cache the style names that will be used for each child.
        CellTree.Style style = nodeView.tree.getStyle();
        String selectedStyle = style.selectedItem();
        String itemStyle = style.item();
        String itemImageValueStyle = style.itemImageValue();
        String itemValueStyle = style.itemValue();
        String openStyle = style.openItem();
        String topStyle = style.topItem();
        String topImageValueStyle = style.topItemImageValue();
        boolean isRootNode = nodeView.isRootNode();
        String openImage = nodeView.tree.getOpenImageHtml(isRootNode);
        String closedImage = nodeView.tree.getClosedImageHtml(isRootNode);
        int imageWidth = nodeView.tree.getImageWidth();
        int paddingLeft = imageWidth * nodeView.depth;

        // Create a set of currently open nodes.
        Set<Object> openNodes = new HashSet<Object>();
        int childCount = nodeView.getChildCount();
        int end = start + values.size();
        for (int i = start; i < end && i < childCount; i++) {
          CellTreeNodeView<?> child = nodeView.getChildNode(i);
          // Ignore child nodes that are closed.
          if (child.isOpen()) {
            openNodes.add(child.getValueKey());
          }
        }

        // Render the child nodes.
        ProvidesKey<C> providesKey = nodeInfo.getProvidesKey();
        TreeViewModel model = nodeView.tree.getTreeViewModel();
        for (C value : values) {
          Object key = providesKey.getKey(value);
          boolean isOpen = openNodes.contains(key);

          // Outer div contains image, value, and children (when open).
          sb.append("<div>");

          // The selection pads the content based on the depth.
          sb.append("<div style='padding-left:");
          sb.append(paddingLeft);
          sb.append("px;' class='").append(itemStyle);
          if (isOpen) {
            sb.append(" ").append(openStyle);
          }
          if (isRootNode) {
            sb.append(" ").append(topStyle);
          }
          if (selectionModel != null && selectionModel.isSelected(value)) {
            sb.append(" ").append(selectedStyle);
          }
          sb.append("'>");

          // Inner div contains image and value.
          sb.append("<div onclick='' style='position:relative;padding-left:");
          sb.append(imageWidth);
          sb.append("px;' class='").append(itemImageValueStyle);
          if (isRootNode) {
            sb.append(" ").append(topImageValueStyle);
          }
          sb.append("'>");

          // Add the open/close icon.
          if (isOpen) {
            sb.append(openImage);
          } else if (model.isLeaf(value)) {
            sb.append(LEAF_IMAGE);
          } else {
            sb.append(closedImage);
          }

          // Content div contains value.
          sb.append("<div class='").append(itemValueStyle).append("'>");
          cell.render(value, null, sb);
          sb.append("</div></div></div></div>");
        }
      }

      public void replaceAllChildren(List<C> values, String html) {
        // Hide the child container so we can animate it.
        if (nodeView.tree.isAnimationEnabled()) {
          nodeView.ensureAnimationFrame().getStyle().setDisplay(Display.NONE);
        }

        // Replace the child nodes.
        Map<Object, CellTreeNodeView<?>> savedViews = saveChildState(values, 0);
        AbstractHasData.replaceAllChildren(nodeView.tree, childContainer, html);
        loadChildState(values, 0, savedViews);

        // Animate the child container open.
        if (nodeView.tree.isAnimationEnabled()) {
          nodeView.tree.maybeAnimateTreeNode(nodeView);
        }
      }

      public void replaceChildren(List<C> values, int start, String html) {
        Map<Object, CellTreeNodeView<?>> savedViews = saveChildState(values, 0);

        Element newChildren = AbstractHasData.convertToElements(
            nodeView.tree, getTmpElem(), html);
        AbstractHasData.replaceChildren(
            nodeView.tree, childContainer, newChildren, start, html);

        loadChildState(values, 0, savedViews);
      }

      public void resetFocus() {
      }

      public void setLoadingState(LoadingState state) {
        nodeView.updateImage(state == LoadingState.LOADING);
        showOrHide(nodeView.emptyMessageElem, state == LoadingState.EMPTY);
      }

      public void setSelected(Element elem, boolean selected) {
        setStyleName(getSelectionElement(elem),
            nodeView.tree.getStyle().selectedItem(), selected);
      }

      /**
       * Reload the open children after rendering new items in this node.
       *
       * @param values the values being replaced
       * @param start the start index
       * @param savedViews the open nodes
       */
      private void loadChildState(List<C> values, int start,
          Map<Object, CellTreeNodeView<?>> savedViews) {
        int len = values.size();
        int end = start + len;
        int childCount = nodeView.getChildCount();
        ProvidesKey<C> providesKey = nodeInfo.getProvidesKey();
            Element childElem = nodeView.ensureChildContainer().getFirstChildElement();
        for (int i = start; i < end; i++) {
          C childValue = values.get(i - start);
          CellTreeNodeView<C> child = nodeView.createTreeNodeView(
              nodeInfo, childElem, childValue, null);
          CellTreeNodeView<?> savedChild = savedViews.remove(
              providesKey.getKey(childValue));
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
      }

      /**
       * Save the state of the open child nodes within the range of the
       * specified values. Use {@link #loadChildState(List, int, Map)} to
       * re-attach the open nodes after they have been replaced.
       *
       * @param values the values being replaced
       * @param start the start index
       * @return the map of open nodes
       */
      private Map<Object, CellTreeNodeView<?>> saveChildState(
          List<C> values, int start) {
        // Ensure that we have a children array.
        if (nodeView.children == null) {
          nodeView.children = new ArrayList<CellTreeNodeView<?>>();
        }

        // Construct a map of former child views based on their value keys.
        int len = values.size();
        int end = start + len;
        int childCount = nodeView.getChildCount();
        Map<Object, CellTreeNodeView<?>> openNodes = new HashMap<
            Object, CellTreeNodeView<?>>();
        for (int i = start; i < end && i < childCount; i++) {
          CellTreeNodeView<?> child = nodeView.getChildNode(i);
          // Ignore child nodes that are closed.
          if (child.isOpen()) {
            openNodes.put(child.getValueKey(), child);
          }
        }

        // Trim the saved views down to the children that still exists.
        ProvidesKey<C> providesKey = nodeInfo.getProvidesKey();
        Map<Object, CellTreeNodeView<?>> savedViews = new HashMap<
            Object, CellTreeNodeView<?>>();
        for (C childValue : values) {
          // Remove any child elements that correspond to prior children
          // so the call to setInnerHtml will not destroy them
          Object key = providesKey.getKey(childValue);
          CellTreeNodeView<?> savedView = openNodes.remove(key);
          if (savedView != null) {
            savedView.ensureAnimationFrame().removeFromParent();
            savedViews.put(key, savedView);
          }
        }
        return savedViews;
      }
    }

    private final Cell<C> cell;
    private final int defaultPageSize;
    private final NodeInfo<C> nodeInfo;
    private CellTreeNodeView<?> nodeView;
    private final HasDataPresenter<C> presenter;

    public NodeCellList(final NodeInfo<C> nodeInfo,
        final CellTreeNodeView<?> nodeView, int pageSize) {
      this.defaultPageSize = pageSize;
      this.nodeInfo = nodeInfo;
      this.nodeView = nodeView;
      cell = nodeInfo.getCell();

      presenter = new HasDataPresenter<C>(
          this, new View(nodeView.ensureChildContainer()), pageSize);

      // Use a pager to update buttons.
      presenter.addRowCountChangeHandler(new RowCountChangeEvent.Handler() {
        public void onRowCountChange(RowCountChangeEvent event) {
          int rowCount = event.getNewRowCount();
          boolean isExact = event.isNewRowCountExact();
          int pageSize = getVisibleRange().getLength();
          showOrHide(nodeView.showMoreElem, isExact && rowCount > pageSize);
        }
      });
    }

    public HandlerRegistration addRangeChangeHandler(
        RangeChangeEvent.Handler handler) {
      return presenter.addRangeChangeHandler(handler);
    }

    public HandlerRegistration addRowCountChangeHandler(
        RowCountChangeEvent.Handler handler) {
      return presenter.addRowCountChangeHandler(handler);
    }

    /**
     * Cleanup this node view.
     */
    public void cleanup() {
      presenter.clearSelectionModel();
    }

    public void fireEvent(GwtEvent<?> event) {
      handlerManger.fireEvent(event);
    }

    public int getDefaultPageSize() {
      return defaultPageSize;
    }

    public int getRowCount() {
      return presenter.getRowCount();
    }

    public SelectionModel<? super C> getSelectionModel() {
      return presenter.getSelectionModel();
    }

    public Range getVisibleRange() {
      return presenter.getVisibleRange();
    }

    public boolean isRowCountExact() {
      return presenter.isRowCountExact();
    }

    public final void setRowCount(int count) {
      setRowCount(count, true);
    }

    public void setRowCount(int size, boolean isExact) {
      presenter.setRowCount(size, isExact);
    }

    public void setRowData(int start, List<C> values) {
      presenter.setRowData(start, values);
    }

    public void setSelectionModel(
        final SelectionModel<? super C> selectionModel) {
      presenter.setSelectionModel(selectionModel);
    }

    public final void setVisibleRange(int start, int length) {
      setVisibleRange(new Range(start, length));
    }

    public void setVisibleRange(Range range) {
      presenter.setVisibleRange(range);
    }

    public void setVisibleRangeAndClearData(
        Range range, boolean forceRangeChangeEvent) {
      presenter.setVisibleRangeAndClearData(range, forceRangeChangeEvent);
    }

    /**
     * Assign this {@link HasData} to a new {@link CellTreeNodeView}.
     *
     * @param nodeView the new node view
     */
    private void setNodeView(CellTreeNodeView<?> nodeView) {
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
  private List<CellTreeNodeView<?>> children;

  /**
   * A reference to the element that contains all content. Parent of the
   * childContainer and the show/hide elements.
   */
  private Element contentContainer;

  /**
   * The depth of this node in the tree.
   */
  private final int depth;

  /**
   * The element used when there are no children to display.
   */
  private Element emptyMessageElem;

  /**
   * The list view used to display the nodes.
   */
  private NodeCellList<?> listView;

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
   * The parent {@link CellTreeNodeView}.
   */
  private final CellTreeNodeView<?> parentNode;

  /**
   * The {@link NodeInfo} of the parent node.
   */
  private final NodeInfo<T> parentNodeInfo;

  /**
   * The element used to display more children.
   */
  private AnchorElement showMoreElem;

  /**
   * The {@link CellTree} that this node belongs to.
   */
  private final CellTree tree;

  /**
   * This node's value.
   */
  private T value;

  /**
   * Construct a {@link CellTreeNodeView}.
   *
   * @param tree the parent {@link CellTreeNodeView}
   * @param parent the parent {@link CellTreeNodeView}
   * @param parentNodeInfo the {@link NodeInfo} of the parent
   * @param elem the outer element of this {@link CellTreeNodeView}
   * @param value the value of this node
   */
  CellTreeNodeView(final CellTree tree, final CellTreeNodeView<?> parent,
      NodeInfo<T> parentNodeInfo, Element elem, T value) {
    this.tree = tree;
    this.parentNode = parent;
    this.parentNodeInfo = parentNodeInfo;
    this.depth = parentNode == null ? 0 : parentNode.depth + 1;
    this.value = value;
    setElement(elem);
  }

  public int getChildCount() {
    return children == null ? 0 : children.size();
  }

  public CellTreeNodeView<?> getChildNode(int childIndex) {
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

        // Sink events for the new node.
        if (nodeInfo != null) {
          Set<String> consumedEvents = nodeInfo.getCell().getConsumedEvents();
          if (consumedEvents != null) {
            CellBasedWidgetImpl.get().sinkEvents(tree, consumedEvents);
          }
        }
      }

      // If we don't have any nodeInfo, we must be a leaf node.
      if (nodeInfo != null) {
        // Add a loading message.
        ensureChildContainer();
        showOrHide(showMoreElem, false);
        showOrHide(emptyMessageElem, false);
        if (!isRootNode()) {
          setStyleName(getCellParent(), tree.getStyle().openItem(), true);
        }
        ensureAnimationFrame().getStyle().setProperty("display", "");
        onOpen(nodeInfo);
      }
    } else {
      if (!isRootNode()) {
        setStyleName(getCellParent(), tree.getStyle().openItem(), false);
      }
      cleanup();
      tree.maybeAnimateTreeNode(this);
      updateImage(false);
    }
  }

  /**
   * Unregister the list handler and destroy all child nodes.
   */
  protected void cleanup() {
    // Unregister the list handler.
    if (listView != null) {
      listView.cleanup();
      nodeInfo.unsetDataDisplay();
      listView = null;
    }

    // Recursively kill children.
    if (children != null) {
      for (CellTreeNodeView<?> child : children) {
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
  protected <C> CellTreeNodeView<C> createTreeNodeView(
      NodeInfo<C> nodeInfo, Element childElem, C childValue, Object viewData) {
    return new CellTreeNodeView<C>(tree, this, nodeInfo, childElem, childValue);
  }

  /**
   * Fire an event to the {@link com.google.gwt.cell.client.AbstractCell}.
   *
   * @param event the native event
   */
  protected void fireEventToCell(NativeEvent event) {
    if (parentNodeInfo != null) {
      Cell<T> parentCell = parentNodeInfo.getCell();
      String eventType = event.getType();
          SelectionModel<? super T> selectionModel = parentNodeInfo.getSelectionModel();

      // Update selection.
      if (selectionModel != null && "click".equals(eventType)
          && !parentCell.handlesSelection()) {
        // TODO(jlabanca): Should we toggle? Only when ctrl is pressed?
        selectionModel.setSelected(value, true);
      }

      // Forward the event to the cell.
      Element cellParent = getCellParent();
      Object key = getValueKey();
      Set<String> consumedEvents = parentCell.getConsumedEvents();
      if (consumedEvents != null && consumedEvents.contains(eventType)) {
        parentCell.onBrowserEvent(
            cellParent, value, key, event, parentNodeInfo.getValueUpdater());
      }
    }
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
    return getImageElement(getElement());
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
    NodeCellList<C> view = new NodeCellList<C>(
        nodeInfo, this, tree.getDefaultNodeSize());
    listView = view;
    view.setSelectionModel(nodeInfo.getSelectionModel());
    nodeInfo.setDataDisplay(view);
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

      // TODO(jlabanca): I18N no data string.
      emptyMessageElem = Document.get().createDivElement();
      emptyMessageElem.setInnerHTML("no data");
      setStyleName(emptyMessageElem, tree.getStyle().emptyMessage(), true);
      showOrHide(emptyMessageElem, false);
      contentContainer.appendChild(emptyMessageElem);

      showMoreElem = Document.get().createAnchorElement();
      showMoreElem.setHref("javascript:;");
      showMoreElem.setInnerText("Show more");
      setStyleName(showMoreElem, tree.getStyle().showMoreButton(), true);
      showOrHide(showMoreElem, false);
      contentContainer.appendChild(showMoreElem);
    }
    return contentContainer;
  }

  Element getShowMoreElement() {
    return showMoreElem;
  }

  /**
   * Check if this node is a root node.
   *
   * @return true if a root node
   */
  boolean isRootNode() {
    return parentNode == null;
  }

  void showFewer() {
    Range range = listView.getVisibleRange();
    int defaultPageSize = listView.getDefaultPageSize();
    int maxSize = Math.max(
        defaultPageSize, range.getLength() - defaultPageSize);
    listView.setVisibleRange(range.getStart(), maxSize);
  }

  void showMore() {
    Range range = listView.getVisibleRange();
    int pageSize = range.getLength() + listView.getDefaultPageSize();
    listView.setVisibleRange(range.getStart(), pageSize);
  }

  /**
   * Update the image based on the current state.
   *
   * @param isLoading true if still loading data
   */
  private void updateImage(boolean isLoading) {
    // Early out if this is a root node.
    if (isRootNode()) {
      return;
    }

    // Replace the image element with a new one.
    boolean isTopLevel = parentNode.isRootNode();
    String html = tree.getClosedImageHtml(isTopLevel);
    if (open) {
      html = isLoading ? tree.getLoadingImageHtml() : tree.getOpenImageHtml(
          isTopLevel);
    }
    if (nodeInfoLoaded && nodeInfo == null) {
      html = LEAF_IMAGE;
    }
    Element tmp = Document.get().createDivElement();
    tmp.setInnerHTML(html);
    Element imageElem = tmp.getFirstChildElement();

    Element oldImg = getImageElement();
    oldImg.getParentElement().replaceChild(imageElem, oldImg);
  }
}
