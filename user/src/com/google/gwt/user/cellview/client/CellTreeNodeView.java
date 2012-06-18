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
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTree.CellTreeMessages;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.CellPreviewEvent.Handler;
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
// TODO(jlabanca): Convert this to be the type of the child and create lazily.
class CellTreeNodeView<T> extends UIObject {

  interface Template extends SafeHtmlTemplates {
    @Template("<div onclick=\"\" style=\"{0}position:relative;\""
        + " class=\"{1}\">{2}<div class=\"{3}\">{4}</div></div>")
    SafeHtml innerDiv(SafeStyles cssString, String classes, SafeHtml image, String itemValueStyle,
        SafeHtml cellContents);

    @Template("<div><div style=\"{0}\" class=\"{1}\">{2}</div></div>")
    SafeHtml outerDiv(SafeStyles cssString, String classes, SafeHtml content);
  }

  /**
   * The {@link com.google.gwt.view.client.HasData} used to show children. This
   * class is intentionally static because we might move it to a new
   * {@link CellTreeNodeView}, and we don't want non-static references to the
   * old {@link CellTreeNodeView}.
   * 
   * @param <C> the child item type
   */
  static class NodeCellList<C> implements HasData<C> {

    /**
     * The view used by the NodeCellList.
     */
    private class View implements HasDataPresenter.View<C> {

      private final Element childContainer;

      public View(Element childContainer) {
        this.childContainer = childContainer;
      }

      @Override
      public <H extends EventHandler> HandlerRegistration addHandler(H handler, Type<H> type) {
        return handlerManger.addHandler(type, handler);
      }

      public void render(SafeHtmlBuilder sb, List<C> values, int start,
          SelectionModel<? super C> selectionModel) {
        // Cache the style names that will be used for each child.
        CellTree.Style style = nodeView.tree.getStyle();
        String itemValueStyle = style.cellTreeItemValue();
        String selectedStyle = " " + style.cellTreeSelectedItem();
        String itemStyle = style.cellTreeItem();
        String itemImageValueStyle = " " + style.cellTreeItemImageValue();
        String openStyle = " " + style.cellTreeOpenItem();
        String topStyle = " " + style.cellTreeTopItem();
        String topImageValueStyle = " " + style.cellTreeTopItemImageValue();
        boolean isRootNode = nodeView.isRootNode();
        SafeHtml openImage = nodeView.tree.getOpenImageHtml(isRootNode);
        SafeHtml closedImage = nodeView.tree.getClosedImageHtml(isRootNode);
        int imageWidth = nodeView.tree.getImageWidth();
        String paddingDirection = LocaleInfo.getCurrentLocale().isRTL() ? "right" : "left";
        int paddingAmount = imageWidth * nodeView.depth;

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
        ProvidesKey<C> keyProvider = nodeInfo.getProvidesKey();
        TreeViewModel model = nodeView.tree.getTreeViewModel();
        for (int i = start; i < end; i++) {
          C value = values.get(i - start);
          Object key = keyProvider.getKey(value);
          boolean isOpen = openNodes.contains(key);

          // Outer div contains image, value, and children (when open)
          StringBuilder outerClasses = new StringBuilder(itemStyle);
          if (isOpen) {
            outerClasses.append(openStyle);
          }
          if (isRootNode) {
            outerClasses.append(topStyle);
          }
          if (selectionModel != null && selectionModel.isSelected(value)) {
            outerClasses.append(selectedStyle);
          }

          // Inner div contains image and value
          StringBuilder innerClasses = new StringBuilder(itemStyle);
          innerClasses.append(itemImageValueStyle);
          if (isRootNode) {
            innerClasses.append(topImageValueStyle);
          }
          // Add the open/close icon.
          SafeHtml image;
          if (isOpen) {
            image = openImage;
          } else if (model.isLeaf(value)) {
            image = LEAF_IMAGE;
          } else {
            image = closedImage;
          }
          // Render cell contents
          SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
          Context context = new Context(i, 0, key);
          cell.render(context, value, cellBuilder);
          SafeStyles innerPadding =
              SafeStylesUtils.fromTrustedString("padding-" + paddingDirection + ": " + imageWidth
                  + "px;");
          SafeHtml innerDiv =
              template.innerDiv(innerPadding, innerClasses.toString(), image, itemValueStyle,
                  cellBuilder.toSafeHtml());

          SafeStyles outerPadding =
              SafeStylesUtils.fromTrustedString("padding-" + paddingDirection + ": "
                  + paddingAmount + "px;");
          sb.append(template.outerDiv(outerPadding, outerClasses.toString(), innerDiv));
        }
      }

      @Override
      public void replaceAllChildren(List<C> values, SelectionModel<? super C> selectionModel,
          boolean stealFocus) {
        // Render the children.
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        render(sb, values, 0, selectionModel);

        // Hide the child container so we can animate it.
        if (nodeView.tree.isAnimationEnabled()) {
          nodeView.ensureAnimationFrame().getStyle().setDisplay(Display.NONE);
        }

        // Replace the child nodes.
        nodeView.tree.isRefreshing = true;
        Map<Object, CellTreeNodeView<?>> savedViews = saveChildState(values, 0);
        AbstractHasData.replaceAllChildren(nodeView.tree, childContainer, sb.toSafeHtml());
        nodeView.tree.isRefreshing = false;

        // Trim the list of children.
        int size = values.size();
        int childCount = nodeView.children.size();
        while (childCount > size) {
          childCount--;
          CellTreeNodeView<?> deleted = nodeView.children.remove(childCount);
          deleted.cleanup(true);
        }

        // Reattach the open nodes.
        loadChildState(values, 0, savedViews);

        // If this is the root node, move keyboard focus to the first child.
        if (nodeView.isRootNode() && nodeView.tree.getKeyboardSelectedNode() == nodeView
            && values.size() > 0) {
          nodeView.tree.keyboardSelect(nodeView.children.get(0), false);
        }

        // Animate the child container open.
        if (nodeView.tree.isAnimationEnabled()) {
          nodeView.tree.maybeAnimateTreeNode(nodeView);
        }
      }

      @Override
      public void replaceChildren(List<C> values, int start,
          SelectionModel<? super C> selectionModel, boolean stealFocus) {
        // Render the children.
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        render(sb, values, 0, selectionModel);

        Map<Object, CellTreeNodeView<?>> savedViews = saveChildState(values, start);

        nodeView.tree.isRefreshing = true;
        SafeHtml html = sb.toSafeHtml();
        Element newChildren = AbstractHasData.convertToElements(nodeView.tree, getTmpElem(), html);
        AbstractHasData
            .replaceChildren(nodeView.tree, childContainer, newChildren, start, html);
        nodeView.tree.isRefreshing = false;

        loadChildState(values, start, savedViews);
      }

      @Override
      public void resetFocus() {
        nodeView.tree.resetFocus();
      }

      @Override
      public void setKeyboardSelected(int index, boolean selected, boolean stealFocus) {
        // Keyboard selection is handled by CellTree.
        Element elem = childContainer.getChild(index).cast();
        setStyleName(getSelectionElement(elem), nodeView.tree.getStyle()
            .cellTreeKeyboardSelectedItem(), selected);
      }

      @Override
      public void setLoadingState(LoadingState state) {
        nodeView.updateImage(state == LoadingState.LOADING);
        showOrHide(nodeView.emptyMessageElem, state == LoadingState.LOADED && presenter.isEmpty());
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
        ProvidesKey<C> keyProvider = nodeInfo.getProvidesKey();

        Element container = nodeView.ensureChildContainer();
        Element childElem = (values.size() == 0) ? null : Element.as(container.getChild(start));
        CellTreeNodeView<?> keyboardSelected = nodeView.tree.getKeyboardSelectedNode();
        for (int i = start; i < end; i++) {
          C childValue = values.get(i - start);
          CellTreeNodeView<C> child =
              nodeView.createTreeNodeView(nodeInfo, childElem, childValue, null);
          CellTreeNodeView<?> savedChild = savedViews.remove(keyProvider.getKey(childValue));
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

            // Transfer the tree node so that if the user has a handle to it, it
            // won't be destroyed.
            child.treeNode = savedChild.treeNode;
            if (child.treeNode != null) {
              child.treeNode.nodeView = child;
            }

            // Swap the node view in the child. We reuse the same NodeListView
            // so that we don't have to unset and register a new view with the
            // NodeInfo, which would inevitably cause the NodeInfo to push
            // new data.
            child.listView = savedChild.listView;
            if (child.listView != null) {
              child.listView.nodeView = child;
            }

            // Set the new parent of the grandchildren.
            if (child.children != null) {
              for (CellTreeNodeView<?> grandchild : child.children) {
                grandchild.parentNode = child;
              }
            }

            // Transfer the keyboard selected node.
            if (keyboardSelected == savedChild) {
              keyboardSelected = child;
            }

            // Copy the child container element to the new child
            child.getElement().appendChild(savedChild.ensureAnimationFrame());

            // Mark the old child as destroy without actually destroying it.
            savedChild.isDestroyed = true;
          }

          if (childCount > i) {
            nodeView.children.set(i, child);
          } else {
            nodeView.children.add(child);
          }
          childElem = childElem.getNextSiblingElement();
        }

        // Move the keyboard selected node if it is this node or a child of this
        // node.
        CellTreeNodeView<?> curNode = keyboardSelected;
        while (curNode != null) {
          if (curNode == nodeView) {
            nodeView.tree.keyboardSelect(keyboardSelected, false);
            break;
          }
          curNode = curNode.parentNode;
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
      private Map<Object, CellTreeNodeView<?>> saveChildState(List<C> values, int start) {
        // Ensure that we have a children array.
        if (nodeView.children == null) {
          nodeView.children = new ArrayList<CellTreeNodeView<?>>();
        }

        // Construct a map of former child views based on their value keys.
        int len = values.size();
        int end = start + len;
        int childCount = nodeView.getChildCount();
        CellTreeNodeView<?> keyboardSelected = nodeView.tree.getKeyboardSelectedNode();
        Map<Object, CellTreeNodeView<?>> openNodes = new HashMap<Object, CellTreeNodeView<?>>();
        for (int i = start; i < end && i < childCount; i++) {
          CellTreeNodeView<?> child = nodeView.getChildNode(i);
          if (child.isOpen() || child == keyboardSelected) {
            // Save child nodes that are open or keyboard selected.
            openNodes.put(child.getValueKey(), child);
          } else {
            // Cleanup child nodes that are closed.
            child.cleanup(true);
          }
        }

        // Trim the saved views down to the children that still exists.
        ProvidesKey<C> keyProvider = nodeInfo.getProvidesKey();
        Map<Object, CellTreeNodeView<?>> savedViews = new HashMap<Object, CellTreeNodeView<?>>();
        for (C childValue : values) {
          // Remove any child elements that correspond to prior children
          // so the call to setInnerHtml will not destroy them
          Object key = keyProvider.getKey(childValue);
          CellTreeNodeView<?> savedView = openNodes.remove(key);
          if (savedView != null) {
            savedView.ensureAnimationFrame().removeFromParent();
            savedViews.put(key, savedView);
          }
        }

        // Cleanup the remaining open nodes that are not in the new data set.
        for (CellTreeNodeView<?> lostNode : openNodes.values()) {
          lostNode.cleanup(true);
        }

        return savedViews;
      }
    }

    final HasDataPresenter<C> presenter;
    private final Cell<C> cell;
    private final int defaultPageSize;
    private HandlerManager handlerManger = new HandlerManager(this);
    private final NodeInfo<C> nodeInfo;
    private CellTreeNodeView<?> nodeView;

    public NodeCellList(final NodeInfo<C> nodeInfo, final CellTreeNodeView<?> nodeView, int pageSize) {
      this.defaultPageSize = pageSize;
      this.nodeInfo = nodeInfo;
      this.nodeView = nodeView;
      cell = nodeInfo.getCell();

      // Create a presenter.
      presenter =
          new HasDataPresenter<C>(this, new View(nodeView.ensureChildContainer()), pageSize,
              nodeInfo.getProvidesKey());

      // Disable keyboard selection because it is handled by CellTree.
      presenter.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

      // Use a pager to update buttons.
      presenter.addRowCountChangeHandler(new RowCountChangeEvent.Handler() {
        @Override
        public void onRowCountChange(RowCountChangeEvent event) {
          int rowCount = event.getNewRowCount();
          boolean isExact = event.isNewRowCountExact();
          int pageSize = getVisibleRange().getLength();
          showOrHide(nodeView.showMoreElem, isExact && rowCount > pageSize);
        }
      });
    }

    @Override
    public HandlerRegistration addCellPreviewHandler(Handler<C> handler) {
      return presenter.addCellPreviewHandler(handler);
    }

    @Override
    public HandlerRegistration addRangeChangeHandler(RangeChangeEvent.Handler handler) {
      return presenter.addRangeChangeHandler(handler);
    }

    @Override
    public HandlerRegistration addRowCountChangeHandler(RowCountChangeEvent.Handler handler) {
      return presenter.addRowCountChangeHandler(handler);
    }

    /**
     * Cleanup this node view.
     */
    public void cleanup() {
      presenter.clearSelectionModel();
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
      handlerManger.fireEvent(event);
    }

    public int getDefaultPageSize() {
      return defaultPageSize;
    }

    @Override
    public int getRowCount() {
      return presenter.getRowCount();
    }

    @Override
    public SelectionModel<? super C> getSelectionModel() {
      return presenter.getSelectionModel();
    }

    @Override
    public C getVisibleItem(int indexOnPage) {
      return presenter.getVisibleItem(indexOnPage);
    }

    @Override
    public int getVisibleItemCount() {
      return presenter.getVisibleItemCount();
    }

    @Override
    public List<C> getVisibleItems() {
      return presenter.getVisibleItems();
    }

    @Override
    public Range getVisibleRange() {
      return presenter.getVisibleRange();
    }

    @Override
    public boolean isRowCountExact() {
      return presenter.isRowCountExact();
    }

    @Override
    public final void setRowCount(int count) {
      setRowCount(count, true);
    }

    @Override
    public void setRowCount(int size, boolean isExact) {
      presenter.setRowCount(size, isExact);
    }

    @Override
    public void setRowData(int start, List<? extends C> values) {
      presenter.setRowData(start, values);
    }

    @Override
    public void setSelectionModel(final SelectionModel<? super C> selectionModel) {
      presenter.setSelectionModel(selectionModel);
    }

    @Override
    public final void setVisibleRange(int start, int length) {
      setVisibleRange(new Range(start, length));
    }

    @Override
    public void setVisibleRange(Range range) {
      presenter.setVisibleRange(range);
    }

    @Override
    public void setVisibleRangeAndClearData(Range range, boolean forceRangeChangeEvent) {
      presenter.setVisibleRangeAndClearData(range, forceRangeChangeEvent);
    }
  }

  /**
   * An implementation of {@link TreeNode} that delegates to a
   * {@link CellTreeNodeView}. This class is intentionally static because we
   * might move it to a new {@link CellTreeNodeView}, and we don't want
   * non-static references to the old {@link CellTreeNodeView}.
   */
  private static class TreeNodeImpl implements TreeNode {

    private CellTreeNodeView<?> nodeView;

    public TreeNodeImpl(CellTreeNodeView<?> nodeView) {
      this.nodeView = nodeView;
    }

    @Override
    public int getChildCount() {
      assertNotDestroyed();
      flush();
      return nodeView.getChildCount();
    }

    @Override
    public Object getChildValue(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      flush();
      return nodeView.getChildNode(index).value;
    }

    @Override
    public int getIndex() {
      assertNotDestroyed();
      return (nodeView.parentNode == null) ? 0 : nodeView.parentNode.children.indexOf(nodeView);
    }

    @Override
    public TreeNode getParent() {
      assertNotDestroyed();
      return getParentImpl();
    }

    @Override
    public Object getValue() {
      return nodeView.value;
    }

    @Override
    public boolean isChildLeaf(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      flush();
      return nodeView.getChildNode(index).isLeaf();
    }

    @Override
    public boolean isChildOpen(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      flush();
      return nodeView.getChildNode(index).isOpen();
    }

    @Override
    public boolean isDestroyed() {
      if (!nodeView.isDestroyed) {
        /*
         * Flush the parent display because the user may have replaced this
         * node, which would destroy it.
         */
        TreeNodeImpl parent = getParentImpl();
        if (parent != null && !parent.isDestroyed()) {
          parent.flush();
        }
      }
      return nodeView.isDestroyed || !nodeView.isOpen();
    }

    @Override
    public TreeNode setChildOpen(int index, boolean open) {
      return setChildOpen(index, open, true);
    }

    @Override
    public TreeNode setChildOpen(int index, boolean open, boolean fireEvents) {
      assertNotDestroyed();
      checkChildBounds(index);
      CellTreeNodeView<?> child = nodeView.getChildNode(index);
      return child.setOpen(open, fireEvents) ? child.treeNode : null;
    }

    /**
     * Assert that the node has not been destroyed.
     */
    private void assertNotDestroyed() {
      if (isDestroyed()) {
        throw new IllegalStateException("TreeNode no longer exists.");
      }
    }

    /**
     * Check the child bounds.
     * 
     * @param index the index of the child
     * @throws IndexOutOfBoundsException if the child is not in range
     */
    private void checkChildBounds(int index) {
      if ((index < 0) || (index >= getChildCount())) {
        throw new IndexOutOfBoundsException();
      }
    }

    /**
     * Flush pending changes in the view.
     */
    private void flush() {
      if (nodeView.listView != null) {
        nodeView.listView.presenter.flush();
      }
    }

    /**
     * Get the parent node without checking if this node is destroyed.
     * 
     * @return the parent node, or null if the node has no parent
     */
    private TreeNodeImpl getParentImpl() {
      return nodeView.isRootNode() ? null : nodeView.parentNode.treeNode;
    }
  }

  /**
   * The element used in place of an image when a node has no children.
   */
  private static final SafeHtml LEAF_IMAGE = SafeHtmlUtils
      .fromSafeConstant("<div style='position:absolute;display:none;'></div>");

  private static final Template template = GWT.create(Template.class);

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
    return getSelectionElement(nodeElem).getFirstChildElement().getChild(1).cast();
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
   * Return the temporary element used to create elements.
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
   * The list view used to display the nodes.
   */
  NodeCellList<?> listView;

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
   * Set to true when the node is destroyed.
   */
  private boolean isDestroyed;

  /**
   * Messages used for translation.
   */
  private final CellTreeMessages messages;

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
  private CellTreeNodeView<?> parentNode;

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
   * The publicly visible tree node. The {@link CellTreeNodeView} doesn't
   * implement {@link TreeNode} directly because we want to transfer the user's
   * handle to the {@link TreeNode} to the new {@link CellTreeNodeView}.
   */
  private TreeNodeImpl treeNode;

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
   * @param messages tranlation messages
   */
  CellTreeNodeView(final CellTree tree, final CellTreeNodeView<?> parent,
      NodeInfo<T> parentNodeInfo, Element elem, T value, CellTreeMessages messages) {
    this.tree = tree;
    this.parentNode = parent;
    this.parentNodeInfo = parentNodeInfo;
    this.depth = parentNode == null ? 0 : parentNode.depth + 1;
    this.value = value;
    this.messages = messages;
    setElement(elem);
  }

  public int getChildCount() {
    return children == null ? 0 : children.size();
  }

  public CellTreeNodeView<?> getChildNode(int childIndex) {
    return children.get(childIndex);
  }

  public boolean isLeaf() {
    return tree.isLeaf(value);
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
   * @param fireEvents true to fire events if the state changes
   * @return true if successfully opened, false otherwise.
   */
  public boolean setOpen(boolean open, boolean fireEvents) {
    // Early out.
    if (this.open == open) {
      return this.open;
    }

    // If this node is a leaf node, do not call TreeViewModel.getNodeInfo().
    if (open && isLeaf()) {
      return false;
    }

    // The animation clears the innerHtml of the childContainer. If we reopen a
    // node as its closing, it is possible that the new data will be set
    // synchronously, so we have to cancel the animation before attaching the
    // data display to the node info.
    tree.cancelTreeNodeAnimation();
    this.animate = true;
    this.open = open;
    if (open) {
      if (!nodeInfoLoaded) {
        nodeInfoLoaded = true;
        nodeInfo = tree.getNodeInfo(value);

        // Sink events for the new node.
        if (nodeInfo != null) {
          Set<String> eventsToSink = new HashSet<String>();
          // Listen for focus and blur for keyboard navigation
          eventsToSink.add(BrowserEvents.FOCUS);
          eventsToSink.add(BrowserEvents.BLUR);

          Set<String> consumedEvents = nodeInfo.getCell().getConsumedEvents();
          if (consumedEvents != null) {
            eventsToSink.addAll(consumedEvents);
          }
          CellBasedWidgetImpl.get().sinkEvents(tree, eventsToSink);
        }
      }

      // If we don't have any nodeInfo, we must be a leaf node.
      if (nodeInfo != null) {
        // Add a loading message.
        ensureChildContainer();
        showOrHide(showMoreElem, false);
        showOrHide(emptyMessageElem, false);
        if (!isRootNode()) {
          setStyleName(getCellParent(), tree.getStyle().cellTreeOpenItem(), true);
        }
        ensureAnimationFrame().getStyle().setProperty("display", "");
        onOpen(nodeInfo);

        // Fire an event.
        if (fireEvents) {
          OpenEvent.fire(tree, getTreeNode());
        }
      } else {
        this.open = false;
      }
    } else {
      if (!isRootNode()) {
        setStyleName(getCellParent(), tree.getStyle().cellTreeOpenItem(), false);
      }
      cleanup(false);
      tree.maybeAnimateTreeNode(this);
      updateImage(false);

      // Keyboard select this node if the open node was a child.
      CellTreeNodeView<?> keySelected = tree.getKeyboardSelectedNode();
      while (keySelected != null) {
        if (keySelected == this) {
          tree.keyboardSelect(this, true);
          break;
        }
        keySelected = keySelected.getParentNode();
      }

      // Fire an event.
      if (fireEvents) {
        CloseEvent.fire(tree, getTreeNode());
      }
    }

    return this.open;
  }

  /**
   * Unregister the list handler and destroy all child nodes.
   * 
   * @param destroy true to destroy this node
   */
  protected void cleanup(boolean destroy) {
    // Unregister the list handler.
    if (listView != null) {
      listView.cleanup();
      nodeInfo.unsetDataDisplay();
      listView = null;
    }

    // Recursively destroy children.
    if (children != null) {
      for (CellTreeNodeView<?> child : children) {
        child.cleanup(true);
      }
      children = null;
    }

    // Destroy this node.
    if (destroy) {
      isDestroyed = true;

      // If this is the keyboard selected node, select the parent. The children
      // have already been cleaned, so the selected node cannot be under this
      // node.
      if (this == tree.getKeyboardSelectedNode()) {
        tree.keyboardSelect(parentNode, false);
      }
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
  protected <C> CellTreeNodeView<C> createTreeNodeView(NodeInfo<C> nodeInfo, Element childElem,
      C childValue, Object viewData) {
    return new CellTreeNodeView<C>(tree, this, nodeInfo, childElem, childValue, messages);
  }

  /**
   * Fire an event to the {@link com.google.gwt.cell.client.AbstractCell}.
   * 
   * @param event the native event
   */
  @SuppressWarnings("unchecked")
  protected void fireEventToCell(NativeEvent event) {
    if (parentNodeInfo == null) {
      return;
    }

    Cell<T> parentCell = parentNodeInfo.getCell();
    String eventType = event.getType();
    Element cellParent = getCellParent();
    Object key = getValueKey();
    Context context = new Context(getIndex(), 0, key);
    boolean cellWasEditing = parentCell.isEditing(context, cellParent, value);

    // Update selection.
    boolean isSelectionHandled =
        parentCell.handlesSelection()
            || KeyboardSelectionPolicy.BOUND_TO_SELECTION == tree.getKeyboardSelectionPolicy();
    HasData<T> display = (HasData<T>) parentNode.listView;
    CellPreviewEvent<T> previewEvent =
        CellPreviewEvent.fire(display, event, display, context, value, cellWasEditing,
            isSelectionHandled);

    // Forward the event to the cell.
    if (previewEvent.isCanceled() || !cellParent.isOrHasChild(Element.as(event.getEventTarget()))) {
      return;
    }
    Set<String> consumedEvents = parentCell.getConsumedEvents();
    if (consumedEvents != null && consumedEvents.contains(eventType)) {
      parentCell
          .onBrowserEvent(context, cellParent, value, event, parentNodeInfo.getValueUpdater());
      tree.cellIsEditing = parentCell.isEditing(context, cellParent, value);
      if (cellWasEditing && !tree.cellIsEditing) {
        CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            tree.setFocus(true);
          }
        });
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
   * Returns the element that selection styles are applied to. The element
   * includes the open/close image and the rendered value and spans the width of
   * the tree.
   * 
   * @return the selection element
   */
  protected Element getSelectionElement() {
    return getSelectionElement(getElement());
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
    NodeCellList<C> view = new NodeCellList<C>(nodeInfo, this, tree.getDefaultNodeSize());
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
      animationFrame.getStyle().setOverflow(Overflow.HIDDEN);
      animationFrame.getStyle().setDisplay(Display.NONE);
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
      emptyMessageElem.setInnerHTML(messages.emptyTree());
      setStyleName(emptyMessageElem, tree.getStyle().cellTreeEmptyMessage(), true);
      showOrHide(emptyMessageElem, false);
      contentContainer.appendChild(emptyMessageElem);

      showMoreElem = Document.get().createAnchorElement();
      showMoreElem.setHref("javascript:;");
      showMoreElem.setInnerText(messages.showMore());
      setStyleName(showMoreElem, tree.getStyle().cellTreeShowMoreButton(), true);
      showOrHide(showMoreElem, false);
      contentContainer.appendChild(showMoreElem);
    }
    return contentContainer;
  }

  /**
   * Return the index of this node in its parent.
   */
  int getIndex() {
    return parentNode == null ? 0 : parentNode.indexOf(this);
  }

  /**
   * Return the parent node, or null if this node is the root.
   */
  CellTreeNodeView<?> getParentNode() {
    return parentNode;
  }

  Element getShowMoreElement() {
    return showMoreElem;
  }

  /**
   * Get a {@link TreeNode} with a public API for this node view.
   * 
   * @return the {@link TreeNode}
   */
  TreeNode getTreeNode() {
    if (treeNode == null) {
      treeNode = new TreeNodeImpl(this);
    }
    return treeNode;
  }

  int indexOf(CellTreeNodeView<?> child) {
    return children.indexOf(child);
  }

  boolean isDestroyed() {
    return isDestroyed;
  }

  /**
   * Check if this node is a root node.
   * 
   * @return true if a root node
   */
  boolean isRootNode() {
    return parentNode == null;
  }

  /**
   * Check if the value of this node is selected.
   * 
   * @return true if selected, false if not
   */
  boolean isSelected() {
    if (parentNodeInfo != null) {
      SelectionModel<? super T> selectionModel = parentNodeInfo.getSelectionModel();
      if (selectionModel != null) {
        return selectionModel.isSelected(value);
      }
    }
    return false;
  }

  /**
   * Reset focus on this node.
   * 
   * @return true of the cell takes focus, false if not
   */
  boolean resetFocusOnCell() {
    if (parentNodeInfo != null) {
      Context context = new Context(getIndex(), 0, getValueKey());
      Cell<T> cell = parentNodeInfo.getCell();
      return cell.resetFocus(context, getCellParent(), value);
    }
    return false;
  }

  /**
   * Select or deselect this node with the keyboard.
   * 
   * @param selected true if selected, false if not
   * @param stealFocus true to steal focus
   */
  void setKeyboardSelected(boolean selected, boolean stealFocus) {
    if (tree.isKeyboardSelectionDisabled()) {
      return;
    }

    // Apply the selected style.
    if (!selected || tree.isFocused || stealFocus) {
      setKeyboardSelectedStyle(selected);
    }

    // Make the node focusable or not.
    Element cellParent = getCellParent();
    if (!selected) {
      // Chrome: Elements remain focusable after removing the tabIndex, so set
      // it to -1 first.
      cellParent.setTabIndex(-1);
      cellParent.removeAttribute("tabIndex");
      cellParent.removeAttribute("accessKey");
    } else {
      FocusImpl focusImpl = FocusImpl.getFocusImplForWidget();
      com.google.gwt.user.client.Element cellElem = cellParent.cast();
      focusImpl.setTabIndex(cellElem, tree.getTabIndex());
      char accessKey = tree.getAccessKey();
      if (accessKey != 0) {
        focusImpl.setAccessKey(cellElem, accessKey);
      }
      if (stealFocus && !tree.cellIsEditing) {
        cellElem.focus();
      }
    }

    // Update the selection model.
    if (KeyboardSelectionPolicy.BOUND_TO_SELECTION == tree.getKeyboardSelectionPolicy()) {
      setSelected(selected);
    }
  }

  /**
   * Add or remove the keyboard selected style.
   * 
   * @param selected true if selected, false if not
   */
  void setKeyboardSelectedStyle(boolean selected) {
    if (!isRootNode()) {
      Element selectionElem = getSelectionElement(getElement());
      if (selectionElem != null) {
        setStyleName(selectionElem, tree.getStyle().cellTreeKeyboardSelectedItem(), selected);
      }
    }
  }

  /**
   * Select or deselect this node.
   * 
   * @param selected true to select, false to deselect
   */
  void setSelected(boolean selected) {
    if (parentNodeInfo != null) {
      SelectionModel<? super T> selectionModel = parentNodeInfo.getSelectionModel();
      if (selectionModel != null) {
        selectionModel.setSelected(value, selected);
      }
    }
  }

  void showFewer() {
    Range range = listView.getVisibleRange();
    int defaultPageSize = listView.getDefaultPageSize();
    int maxSize = Math.max(defaultPageSize, range.getLength() - defaultPageSize);
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
    SafeHtml html = tree.getClosedImageHtml(isTopLevel);
    if (open) {
      html = isLoading ? tree.getLoadingImageHtml() : tree.getOpenImageHtml(isTopLevel);
    }
    if (nodeInfoLoaded && nodeInfo == null) {
      html = LEAF_IMAGE;
    }
    Element tmp = Document.get().createDivElement();
    tmp.setInnerHTML(html.asString());
    Element imageElem = tmp.getFirstChildElement();

    Element oldImg = getImageElement();
    oldImg.getParentElement().replaceChild(imageElem, oldImg);
  }
}
