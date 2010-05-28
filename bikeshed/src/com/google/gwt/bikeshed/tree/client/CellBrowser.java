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

import com.google.gwt.animation.client.Animation;
import com.google.gwt.bikeshed.list.client.CellList;
import com.google.gwt.bikeshed.list.client.PageSizePager;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAnimation;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListView;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.gwt.view.client.PagingListView.Pager;
import com.google.gwt.view.client.TreeViewModel.NodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A "browsable" view of a tree in which only a single node per level may be
 * open at one time.
 */
public class CellBrowser extends Composite implements ProvidesResize,
    RequiresResize, HasAnimation {

  /**
   * A ClientBundle that provides images for this widget.
   */
  public static interface Resources extends ClientBundle {

    /**
     * An image indicating a closed branch.
     */
    ImageResource cellBrowserClosed();

    /**
     * An image indicating an open branch.
     */
    ImageResource cellBrowserOpen();

    /**
     * The background used for open items.
     */
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource cellBrowserOpenBackground();

    /**
     * The background used for selected items.
     */
    @Source("../../list/client/cellListSelectedBackground.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource cellBrowserSelectedBackground();

    /**
     * The styles used in this widget.
     */
    @Source("CellBrowser.css")
    Style cellBrowserStyle();
  }

  /**
   * Styles used by this widget.
   */
  public static interface Style extends CssResource {

    /**
     * Applied to all columns.
     */
    String column();

    /**
     * Applied to the first column.
     */
    String firstColumn();

    /**
     * Applied to all list items.
     */
    String item();

    /***
     * Applied to open items.
     */
    String openItem();

    /***
     * Applied to selected items.
     */
    String selectedItem();
  }

  /**
   * We override the Resources in {@link CellList} so that the styles in
   * {@link CellList} don't conflict with the styles in {@link CellBrowser}.
   */
  static interface CellListResources extends CellList.Resources {
    @Source("CellBrowserOverride.css")
    CellList.Style cellListStyle();
  }

  /**
   * A wrapper around a cell that adds an open button.
   * 
   * @param <C> the data type of the cell
   */
  private class CellDecorator<C> extends AbstractCell<C> {

    /**
     * The cell used to render the inner contents.
     */
    private final Cell<C> cell;

    /**
     * The level of this list view.
     */
    private final int level;

    /**
     * The key of the currently open item.
     */
    private Object openKey;

    /**
     * The key provider for the node.
     */
    private final ProvidesKey<C> providesKey;

    /**
     * The selection model for the node.
     */
    private final SelectionModel<? super C> selectionModel;

    /**
     * Construct a new {@link CellDecorator}.
     * 
     * @param nodeInfo the {@link NodeInfo} associated with the cell
     * @param level the level of items rendered by this decorator
     */
    public CellDecorator(NodeInfo<C> nodeInfo, int level) {
      this.cell = nodeInfo.getCell();
      this.level = level;
      this.providesKey = nodeInfo.getProvidesKey();
      this.selectionModel = nodeInfo.getSelectionModel();
    }

    @Override
    public boolean consumesEvents() {
      return cell.consumesEvents();
    }

    @Override
    public boolean dependsOnSelection() {
      return cell.dependsOnSelection();
    }

    @Override
    public Object onBrowserEvent(Element parent, C value, Object viewData,
        NativeEvent event, ValueUpdater<C> valueUpdater) {

      // Fire the event to the inner cell.
      viewData = cell.onBrowserEvent(getCellParent(parent), value, viewData,
          event, valueUpdater);

      // Open child nodes.
      if (Event.getTypeInt(event.getType()) == Event.ONMOUSEDOWN) {
        trimToLevel(level);

        // Remove style from currently open item.
        Element curOpenItem = Document.get().getElementById(getOpenId());
        if (curOpenItem != null) {
          setElementOpenState(curOpenItem.getParentElement(), false);
        }
        openKey = null;

        // Save the key of the new open item and update the Element.
        if (!viewModel.isLeaf(value)) {
          NodeInfo<?> nodeInfo = viewModel.getNodeInfo(value);
          if (nodeInfo != null) {
            openKey = providesKey.getKey(value);
            setElementOpenState(parent, true);
            appendTreeNode(nodeInfo);
          }
        }
      }

      return viewData;
    }

    @Override
    public void render(C value, Object viewData, StringBuilder sb) {
      boolean isOpen = (openKey == null) ? false
          : openKey.equals(providesKey.getKey(value));
      boolean isSelected = (selectionModel == null) ? false
          : selectionModel.isSelected(value);
      sb.append("<div style='position:relative;padding-right:");
      sb.append(imageWidth);
      sb.append("px;'");
      sb.append(" class='").append(style.item());
      if (isOpen) {
        sb.append(" ").append(style.openItem());
      }
      if (isSelected) {
        sb.append(" ").append(style.selectedItem());
      }
      sb.append("'");
      if (isOpen) {
        sb.append(" id='").append(getOpenId()).append("'");
      }
      sb.append(">");
      if (isOpen) {
        sb.append(openImageHtml);
      } else if (viewModel.isLeaf(value)) {
        sb.append(LEAF_IMAGE);
      } else {
        sb.append(closedImageHtml);
      }
      sb.append("<div>");
      cell.render(value, viewData, sb);
      sb.append("</div></div>");
    }

    @Override
    public void setValue(Element parent, C value, Object viewData) {
      cell.setValue(getCellParent(parent), value, viewData);
    }

    /**
     * Get the parent element of the decorated cell.
     * 
     * @param parent the parent of this cell
     * @return the decorated cell's parent
     */
    private Element getCellParent(Element parent) {
      return parent.getFirstChildElement().getChild(1).cast();
    }

    /**
     * Get the image element of the decorated cell.
     * 
     * @param parent the parent of this cell
     * @return the image element
     */
    private Element getImageElement(Element parent) {
      return parent.getFirstChildElement().getFirstChildElement();
    }

    /**
     * Get the ID of the open element.
     * 
     * @return the ID
     */
    private String getOpenId() {
      return uniqueId + "-" + level;
    }

    /**
     * Replace the image element of a cell and update the styles associated with
     * the open state.
     * 
     * @param parent the parent element of the cell
     * @param open true if open, false if closed
     */
    private void setElementOpenState(Element parent, boolean open) {
      // Update the style name and ID.
      Element wrapper = parent.getFirstChildElement();
      if (open) {
        wrapper.addClassName(style.openItem());
        wrapper.setId(getOpenId());
      } else {
        wrapper.removeClassName(style.openItem());
        wrapper.setId("");
      }

      // Replace the image element.
      String html = open ? openImageHtml : closedImageHtml;
      Element tmp = Document.get().createDivElement();
      tmp.setInnerHTML(html);
      Element imageElem = tmp.getFirstChildElement();
      Element oldImg = getImageElement(parent);
      wrapper.replaceChild(imageElem, oldImg);
    }
  }

  /**
   * The animation used to scroll to the newly added list view.
   */
  private class ScrollAnimation extends Animation {

    /**
     * The starting scroll position.
     */
    private int startScrollLeft;

    /**
     * The ending scroll position.
     */
    private int targetScrollLeft;

    @Override
    protected void onComplete() {
      getElement().setScrollLeft(targetScrollLeft);
    }

    @Override
    protected void onUpdate(double progress) {
      int diff = targetScrollLeft - startScrollLeft;
      getElement().setScrollLeft(startScrollLeft + (int) (diff * progress));
    }

    void scrollToEnd() {
      Element elem = getElement();
      targetScrollLeft = elem.getScrollWidth() - elem.getClientWidth();

      if (isAnimationEnabled()) {
        // Animate the scrolling.
        startScrollLeft = elem.getScrollLeft();
        run(250);
      } else {
        // Scroll instantly.
        onComplete();
      }
    }
  }

  /**
   * A node in the tree.
   * 
   * @param <C> the data type of the children of the node
   */
  private class TreeNode<C> {
    private CellDecorator<C> cell;
    private ListView<C> listView;
    private NodeInfo<C> nodeInfo;
    private Widget widget;

    /**
     * Construct a new {@link TreeNode}.
     * 
     * @param nodeInfo the nodeInfo for the children nodes
     * @param listView the list view assocated with the node
     * @param widget the widget that represents the list view
     */
    public TreeNode(NodeInfo<C> nodeInfo, ListView<C> listView,
        CellDecorator<C> cell, Widget widget) {
      this.cell = cell;
      this.listView = listView;
      this.nodeInfo = nodeInfo;
      this.widget = widget;
    }

    /**
     * Get the {@link CellDecorator} used to render the node.
     * 
     * @return the cell decorator
     */
    public CellDecorator<C> getCell() {
      return cell;
    }

    /**
     * Unregister the list view and remove it from the widget.
     */
    void cleanup() {
      listView.setSelectionModel(null);
      nodeInfo.unsetView();
      getSplitLayoutPanel().remove(widget);
    }
  }

  /**
   * The element used in place of an image when a node has no children.
   */
  private static final String LEAF_IMAGE = "<div style='position:absolute;display:none;'></div>";

  private static Resources DEFAULT_RESOURCES;

  /**
   * The override styles used in {@link CellList}.
   */
  private static CellListResources cellListResource;

  /**
   * Get the {@link CellList.Resources} overrides.
   */
  private static CellListResources getCellListResources() {
    if (cellListResource == null) {
      cellListResource = GWT.create(CellListResources.class);
    }
    return cellListResource;
  }

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  /**
   * The animation used for scrolling.
   */
  private final ScrollAnimation animation = new ScrollAnimation();

  /**
   * The default width of new columns.
   */
  private int defaultWidth = 200;

  /**
   * The HTML used to generate the closed image.
   */
  private final String closedImageHtml;

  /**
   * The unique ID assigned to this tree widget.
   */
  private final String uniqueId = Document.get().createUniqueId();

  /**
   * A boolean indicating whether or not animations are enabled.
   */
  private boolean isAnimationEnabled;

  /**
   * The minimum width of new columns.
   */
  private int minWidth;

  /**
   * The maximum width of the open and closed images.
   */
  private final int imageWidth;

  /**
   * The HTML used to generate the open image.
   */
  private final String openImageHtml;

  /**
   * The styles used by this widget.
   */
  private final Style style;

  /**
   * The element used to maintain the scrollbar when columns are removed.
   */
  private Element scrollLock;

  /**
   * The visible {@link TreeNode}.
   */
  private final List<TreeNode<?>> treeNodes = new ArrayList<TreeNode<?>>();

  /**
   * The {@link TreeViewModel} that backs the tree.
   */
  private final TreeViewModel viewModel;

  /**
   * Construct a new {@link CellBrowser}.
   * 
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   */
  public <T> CellBrowser(TreeViewModel viewModel, T rootValue) {
    this(viewModel, rootValue, getDefaultResources());
  }

  /**
   * Construct a new {@link CellBrowser} with the specified {@link Resources}.
   * 
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   * @param resources the {@link Resources} used for images
   */
  public <T> CellBrowser(TreeViewModel viewModel, T rootValue,
      Resources resources) {
    this.viewModel = viewModel;
    this.style = resources.cellBrowserStyle();
    this.style.ensureInjected();
    initWidget(new SplitLayoutPanel());
    getElement().getStyle().setOverflow(Overflow.AUTO);
    setStyleName("gwt-SideBySideTreeView");

    // Initialize the open and close images strings.
    ImageResource treeOpen = resources.cellBrowserOpen();
    ImageResource treeClosed = resources.cellBrowserClosed();
    openImageHtml = getImageHtml(treeOpen);
    closedImageHtml = getImageHtml(treeClosed);
    imageWidth = Math.max(treeOpen.getWidth(), treeClosed.getWidth());
    minWidth = imageWidth + 20;

    // Add a placeholder to maintain the scroll width.
    scrollLock = Document.get().createDivElement();
    scrollLock.getStyle().setPosition(Position.ABSOLUTE);
    scrollLock.getStyle().setVisibility(Visibility.HIDDEN);
    scrollLock.getStyle().setZIndex(-32767);
    scrollLock.getStyle().setBackgroundColor("red");
    scrollLock.getStyle().setTop(0, Unit.PX);
    scrollLock.getStyle().setLeft(0, Unit.PX);
    scrollLock.getStyle().setHeight(1, Unit.PX);
    scrollLock.getStyle().setWidth(1, Unit.PX);
    getElement().appendChild(scrollLock);

    // Associate the first ListView with the rootValue.
    appendTreeNode(viewModel.getNodeInfo(rootValue));

    // Catch scroll events.
    sinkEvents(Event.ONSCROLL);
  }

  /**
   * Get the default width of new columns.
   * 
   * @return the default width in pixels
   */
  public int getDefaultColumnWidth() {
    return defaultWidth;
  }

  /**
   * Get the minimum width of columns.
   * 
   * @return the minimum width in pixels
   */
  public int getMinimumColumnWidth() {
    return minWidth;
  }

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONSCROLL:
        // Shorten the scroll bar is possible.
        adjustScrollLock();
        break;
    }
    super.onBrowserEvent(event);
  }

  public void onResize() {
    getSplitLayoutPanel().onResize();
  }

  public void setAnimationEnabled(boolean enable) {
    this.isAnimationEnabled = enable;
  }

  /**
   * Set the default width of new columns.
   * 
   * @param width the default width in pixels
   */
  public void setDefaultColumnWidth(int width) {
    this.defaultWidth = width;
  }

  /**
   * Set the minimum width of columns.
   * 
   * @param minWidth the minimum width in pixels
   */
  public void setMinimumColumnWidth(int minWidth) {
    this.minWidth = minWidth;
  }

  /**
   * Create a Pager to control the list view. The {@link ListView} must extend
   * {@link Widget}.
   * 
   * @param <C> the item type in the list view
   * @param listView the list view to add paging too
   * @return the {@link Pager}
   */
  protected <C> Pager<C> createPager(PagingListView<C> listView) {
    return new PageSizePager<C>(listView, listView.getPageSize());
  }

  /**
   * Create a {@link PagingListView} that will display items. The
   * {@link PagingListView} must extend {@link Widget}.
   * 
   * @param <C> the item type in the list view
   * @param nodeInfo the node info with child data
   * @param cell the cell to use in the list view
   * @return the {@link ListView}
   */
  protected <C> PagingListView<C> createPagingListView(NodeInfo<C> nodeInfo,
      Cell<C> cell) {
    CellList<C> pagingListView = new CellList<C>(cell, getCellListResources());
    pagingListView.setValueUpdater(nodeInfo.getValueUpdater());
    return pagingListView;
  }

  /**
   * Adjust the size of the scroll lock element based on the new position of the
   * scroll bar.
   */
  private void adjustScrollLock() {
    int scrollLeft = getElement().getScrollLeft();
    if (scrollLeft > 0) {
      int clientWidth = getElement().getClientWidth();
      scrollLock.getStyle().setWidth(scrollLeft + clientWidth, Unit.PX);
    } else {
      scrollLock.getStyle().setWidth(1.0, Unit.PX);
    }
  }

  /**
   * Create a new {@link TreeNode} and append it to the end of the LayoutPanel.
   * 
   * @param <C> the data type of the children
   * @param nodeInfo the info about the node
   */
  private <C> void appendTreeNode(final NodeInfo<C> nodeInfo) {
    // Create the list view.
    final int level = treeNodes.size();
    final CellDecorator<C> cell = new CellDecorator<C>(nodeInfo, level);
    final PagingListView<C> listView = createPagingListView(nodeInfo, cell);
    assert (listView instanceof Widget) : "createPagingListView() must return a widget";

    // Create a pager and wrap the components in a scrollable container.
    ScrollPanel scrollable = new ScrollPanel();
    final Pager<C> pager = createPager(listView);
    if (pager != null) {
      assert (pager instanceof Widget) : "createPager() must return a widget";
      FlowPanel flowPanel = new FlowPanel();
      flowPanel.add((Widget) listView);
      flowPanel.add((Widget) pager);
      scrollable.setWidget(flowPanel);
    } else {
      scrollable.setWidget((Widget) listView);
    }
    scrollable.setStyleName(style.column());
    if (level == 0) {
      scrollable.addStyleName(style.firstColumn());
    }

    // Create a delegate list view so we can trap data changes.
    ListView<C> listViewDelegate = new ListView<C>() {
      public Range getRange() {
        return listView.getRange();
      }

      public void setData(int start, int length, List<C> values) {
        // Trim to the current level if the open node no longer exists.
        TreeNode<?> node = treeNodes.get(level);
        Object openKey = node.getCell().openKey;
        if (openKey != null) {
          boolean stillExists = false;
          ProvidesKey<C> keyProvider = nodeInfo.getProvidesKey();
          for (C value : values) {
            if (openKey.equals(keyProvider.getKey(value))) {
              stillExists = true;
              break;
            }
          }
          if (!stillExists) {
            trimToLevel(level);
          }
        }

        // Refresh the list.
        listView.setData(start, length, values);
      }

      public void setDataSize(int size, boolean isExact) {
        listView.setDataSize(size, isExact);
      }

      public void setDelegate(Delegate<C> delegate) {
        listView.setDelegate(delegate);
      }

      public void setSelectionModel(SelectionModel<? super C> selectionModel) {
        listView.setSelectionModel(selectionModel);
      }
    };

    // Create a TreeNode.
    TreeNode<C> treeNode = new TreeNode<C>(nodeInfo, listViewDelegate, cell,
        scrollable);
    treeNodes.add(treeNode);

    // Attach the view to the selection model and node info.
    listView.setSelectionModel(nodeInfo.getSelectionModel());
    nodeInfo.setView(listViewDelegate);

    // Add the ListView to the LayoutPanel.
    SplitLayoutPanel splitPanel = getSplitLayoutPanel();
    splitPanel.insertWest(scrollable, defaultWidth, null);
    splitPanel.setWidgetMinSize(scrollable, minWidth);
    splitPanel.forceLayout();

    // Scroll to the right.
    animation.scrollToEnd();
  }

  /**
   * Get the HTML representation of an image.
   * 
   * @param res the {@link ImageResource} to render as HTML
   * @return the rendered HTML
   */
  private String getImageHtml(ImageResource res) {
    // Add the position and dimensions.
    StringBuilder sb = new StringBuilder();
    sb.append("<div style=\"position:absolute;right:0px;top:0px;height:100%;");
    sb.append("width:").append(res.getWidth()).append("px;");

    // Add the background, vertically centered.
    sb.append("background:url('").append(res.getURL()).append("') ");
    sb.append("no-repeat scroll center center transparent;");

    // Close the div and return.
    sb.append("\"></div>");
    return sb.toString();
  }

  /**
   * Get the {@link SplitLayoutPanel} used to lay out the views.
   * 
   * @return the {@link SplitLayoutPanel}
   */
  private SplitLayoutPanel getSplitLayoutPanel() {
    return (SplitLayoutPanel) getWidget();
  }

  /**
   * Reduce the number of {@link ListView}s down to the specified level.
   * 
   * @param level the level to trim to
   */
  private void trimToLevel(int level) {
    // Add a placeholder to maintain the same scroll width.
    adjustScrollLock();

    // Remove the listViews that are no longer needed.
    int curLevel = treeNodes.size() - 1;
    while (curLevel > level) {
      TreeNode<?> removed = treeNodes.remove(curLevel);
      removed.cleanup();
      curLevel--;
    }
  }
}
