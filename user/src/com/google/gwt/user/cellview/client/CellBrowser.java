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

import com.google.gwt.animation.client.Animation;
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
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAnimation;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.gwt.view.client.TreeViewModel.NodeInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A "browsable" view of a tree in which only a single node per level may be
 * open at one time.
 *
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that
 * the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 */
public class CellBrowser extends AbstractCellTree
    implements ProvidesResize, RequiresResize, HasAnimation {

  interface Template extends SafeHtmlTemplates {
    @Template("<div style=\"position:relative;padding-right:{0}px;\" class="
        + "\"{1}\">{2}<div>{3}</div></div>")
    SafeHtml div(int imageWidth, String classes, SafeHtml image, SafeHtml cellContents);
  }

  /**
   * A ClientBundle that provides images for this widget.
   */
  public interface Resources extends ClientBundle {
    /**
     * An image indicating a closed branch.
     */
    @ImageOptions(flipRtl = true)
    ImageResource cellBrowserClosed();

    /**
     * An image indicating an open branch.
     */
    @ImageOptions(flipRtl = true)
    ImageResource cellBrowserOpen();

    /**
     * The background used for open items.
     */
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal, flipRtl = true)
    ImageResource cellBrowserOpenBackground();

    /**
     * The background used for selected items.
     */
    @Source("cellTreeSelectedBackground.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal, flipRtl = true)
    ImageResource cellBrowserSelectedBackground();

    /**
     * The styles used in this widget.
     */
    @Source(Style.DEFAULT_CSS)
    Style cellBrowserStyle();
  }

  /**
   * Styles used by this widget.
   */
  @ImportedWithPrefix("gwt-CellBrowser")
  public interface Style extends CssResource {
    /**
     * The path to the default CSS styles used by this resource.
     */
    String DEFAULT_CSS = "com/google/gwt/user/cellview/client/CellBrowser.css";

    /**
     * Applied to all columns.
     */
    String cellBrowserColumn();

    /**
     * Applied to the first column.
     */
    String cellBrowserFirstColumn();

    /**
     * Applied to all list items.
     */
    String cellBrowserItem();

    /***
     * Applied to open items.
     */
    String cellBrowserOpenItem();

    /***
     * Applied to selected items.
     */
    String cellBrowserSelectedItem();

    /**
     * Applied to the widget.
     */
    String cellBrowserWidget();
  }

  /**
   * We override the Resources in {@link CellList} so that the styles in
   * {@link CellList} don't conflict with the styles in {@link CellBrowser}.
   */
  interface CellListResources extends CellList.Resources {
    @Source("CellBrowserOverride.css")
    CellList.Style cellListStyle();
  }

  /**
   * A wrapper around a cell that adds an open button.
   *
   * @param <C> the data type of the cell
   */
  private class CellDecorator<C> implements Cell<C> {

    /**
     * The cell used to render the inner contents.
     */
    private final Cell<C> cell;

    /**
     * The events consumed by this cell.
     */
    private final Set<String> consumedEvents = new HashSet<String>();

    /**
     * The level of this list view.
     */
    private final int level;

    /**
     * The key of the currently open item.
     */
    private Object openKey;

    /**
     * The value of the currently open item.
     */
    private C openValue;

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

      // Save the consumed events.
      consumedEvents.add("mousedown");
      Set<String> cellEvents = cell.getConsumedEvents();
      if (cellEvents != null) {
        consumedEvents.addAll(cellEvents);
      }
    }

    public boolean dependsOnSelection() {
      return cell.dependsOnSelection();
    }

    public Set<String> getConsumedEvents() {
      return consumedEvents;
    }

    public boolean handlesSelection() {
      return cell.handlesSelection();
    }

    public boolean isEditing(Element element, C value, Object key) {
      return cell.isEditing(element, value, key);
    }

    public void onBrowserEvent(Element parent, C value, Object key,
        NativeEvent event, ValueUpdater<C> valueUpdater) {

      // Fire the event to the inner cell.
      cell.onBrowserEvent(
          getCellParent(parent), value, key, event, valueUpdater);

      // Open child nodes.
      if (Event.getTypeInt(event.getType()) == Event.ONMOUSEDOWN) {
        setChildState(this, value, true, true);
      }
    }

    public void render(C value, Object viewData, SafeHtmlBuilder sb) {
      boolean isOpen = (openKey == null) ? false
          : openKey.equals(getValueKey(value));
      boolean isSelected = (selectionModel == null) ? false
          : selectionModel.isSelected(value);

      StringBuilder classesBuilder = new StringBuilder();
      classesBuilder.append(style.cellBrowserItem());
      if (isOpen) {
        classesBuilder.append(" ").append(style.cellBrowserOpenItem());
      }
      if (isSelected) {
        classesBuilder.append(" ").append(style.cellBrowserSelectedItem());
      }
      String classes = classesBuilder.toString();

      SafeHtml image;
      if (isOpen) {
        image = openImageHtml;
      } else if (isLeaf(value)) {
        image = LEAF_IMAGE;
      } else {
        image = closedImageHtml;
      }
      SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
      cell.render(value, viewData, cellBuilder);
      sb.append(template.div(imageWidth, classes, image,
          cellBuilder.toSafeHtml()));
    }

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
     * Get the key for the specified value.
     *
     * @param value the value
     * @return the key
     */
    private Object getValueKey(C value) {
      return (providesKey == null) ? value : providesKey.getKey(value);
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
      if (LocaleInfo.getCurrentLocale().isRTL()) {
        targetScrollLeft *= -1;
      }

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
  private class TreeNodeImpl<C> implements TreeNode {
    private final CellDecorator<C> cell;
    private final AbstractHasData<C> display;
    private NodeInfo<C> nodeInfo;
    private final Object value;
    private final HandlerRegistration valueChangeHandler;
    private final Widget widget;

    /**
     * Construct a new {@link TreeNodeImpl}.
     *
     * @param nodeInfo the nodeInfo for the children nodes
     * @param value the value of the node
     * @param display the display associated with the node
     * @param cell the {@link Cell} used to render the data
     * @param widget the widget that represents the list view
     */
    public TreeNodeImpl(final NodeInfo<C> nodeInfo, Object value,
        AbstractHasData<C> display, final CellDecorator<C> cell,
        Widget widget) {
      this.cell = cell;
      this.display = display;
      this.nodeInfo = nodeInfo;
      this.value = value;
      this.widget = widget;

      // Trim to the current level if the open node disappears.
      valueChangeHandler = display.addValueChangeHandler(
          new ValueChangeHandler<List<C>>() {
            public void onValueChange(ValueChangeEvent<List<C>> event) {
              Object openKey = cell.openKey;
              if (openKey != null) {
                boolean stillExists = false;
                List<C> displayValues = event.getValue();
                for (C displayValue : displayValues) {
                  if (openKey.equals(cell.getValueKey(displayValue))) {
                    stillExists = true;
                    break;
                  }
                }
                if (!stillExists) {
                  trimToLevel(cell.level);
                }
              }
            }
          });
    }

    public int getChildCount() {
      assertNotDestroyed();
      return display.getChildCount();
    }

    public C getChildValue(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      return display.getDisplayedItem(index);
    }

    public int getIndex() {
      assertNotDestroyed();
      TreeNodeImpl<?> parent = getParent();
      return (parent == null) ? 0 : parent.getOpenIndex();
    }

    public TreeNodeImpl<?> getParent() {
      assertNotDestroyed();
      return (cell.level == 0) ? null : treeNodes.get(cell.level - 1);
    }

    public Object getValue() {
      return value;
    }

    public boolean isChildLeaf(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      return isLeaf(getChildValue(index));
    }

    public boolean isChildOpen(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      return (cell.openKey == null) ? false : cell.openKey.equals(
          cell.getValueKey(getChildValue(index)));
    }

    public boolean isDestroyed() {
      return nodeInfo == null;
    }

    public TreeNode setChildOpen(int index, boolean open) {
      return setChildOpen(index, open, true);
    }

    public TreeNode setChildOpen(int index, boolean open, boolean fireEvents) {
      assertNotDestroyed();
      checkChildBounds(index);
      return setChildState(cell, getChildValue(index), open, fireEvents);
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
     * Unregister the list view and remove it from the widget.
     */
    private void destroy() {
      valueChangeHandler.removeHandler();
      display.setSelectionModel(null);
      nodeInfo.unsetDataDisplay();
      getSplitLayoutPanel().remove(widget);
      nodeInfo = null;
    }

    /**
     * Get the index of the open item.
     *
     * @return the index of the open item, or -1 if not found
     */
    private int getOpenIndex() {
      return display.indexOf(cell.openValue);
    }
  }

  /**
   * The element used in place of an image when a node has no children.
   */
  private static final SafeHtml LEAF_IMAGE = SafeHtmlUtils.fromSafeConstant(
      "<div style='position:absolute;display:none;'></div>");

  private static Resources DEFAULT_RESOURCES;

  /**
   * The override styles used in {@link CellList}.
   */
  private static CellListResources cellListResource;

  private static Template template;

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
  private final SafeHtml closedImageHtml;

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
  private final SafeHtml openImageHtml;

  /**
   * The styles used by this widget.
   */
  private final Style style;

  /**
   * The element used to maintain the scrollbar when columns are removed.
   */
  private Element scrollLock;

  /**
   * The visible {@link TreeNodeImpl}s.
   */
  private final List<TreeNodeImpl<?>> treeNodes = new ArrayList<
      TreeNodeImpl<?>>();

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
  public <T> CellBrowser(
      TreeViewModel viewModel, T rootValue, Resources resources) {
    super(viewModel);
    if (template == null) {
      template = GWT.create(Template.class);
    }
    this.style = resources.cellBrowserStyle();
    this.style.ensureInjected();
    initWidget(new SplitLayoutPanel());
    getElement().getStyle().setOverflow(Overflow.AUTO);
    setStyleName(this.style.cellBrowserWidget());

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
    scrollLock.getStyle().setTop(0, Unit.PX);
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      scrollLock.getStyle().setRight(0, Unit.PX);
    } else {
      scrollLock.getStyle().setLeft(0, Unit.PX);
    }
    scrollLock.getStyle().setHeight(1, Unit.PX);
    scrollLock.getStyle().setWidth(1, Unit.PX);
    getElement().appendChild(scrollLock);

    // Associate the first view with the rootValue.
    appendTreeNode(getNodeInfo(rootValue), rootValue);

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

  @Override
  public TreeNode getRootTreeNode() {
    return treeNodes.get(0);
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
   * Create a {@link HasData} that will display items. The {@link HasData} must
   * extend {@link Widget}.
   *
   * @param <C> the item type in the list view
   * @param nodeInfo the node info with child data
   * @param cell the cell to use in the list view
   * @return the {@link HasData}
   */
  // TODO(jlabanca): Move createDisplay into constructor factory arg?
  protected <C> AbstractHasData<C> createDisplay(
      NodeInfo<C> nodeInfo, Cell<C> cell) {
    CellList<C> display = new CellList<C>(cell, getCellListResources(),
        nodeInfo.getProvidesKey());
    display.setValueUpdater(nodeInfo.getValueUpdater());
    return display;
  }

  /**
   * Create a pager to control the list view.
   *
   * @param <C> the item type in the list view
   * @param display the list view to add paging too
   * @return the pager
   */
  protected <C> Widget createPager(HasData<C> display) {
    PageSizePager pager = new PageSizePager(
        display.getVisibleRange().getLength());
    pager.setDisplay(display);
    return pager;
  }

  /**
   * Adjust the size of the scroll lock element based on the new position of the
   * scroll bar.
   */
  private void adjustScrollLock() {
    int scrollLeft = Math.abs(getElement().getScrollLeft());
    if (scrollLeft > 0) {
      int clientWidth = getElement().getClientWidth();
      scrollLock.getStyle().setWidth(scrollLeft + clientWidth, Unit.PX);
    } else {
      scrollLock.getStyle().setWidth(1.0, Unit.PX);
    }
  }

  /**
   * Create a new {@link TreeNodeImpl} and append it to the end of the
   * LayoutPanel.
   *
   * @param <C> the data type of the children
   * @param nodeInfo the info about the node
   * @param value the value of the open node
   */
  private <C> void appendTreeNode(final NodeInfo<C> nodeInfo, Object value) {
    // Create the list view.
    final int level = treeNodes.size();
    final CellDecorator<C> cell = new CellDecorator<C>(nodeInfo, level);
    final AbstractHasData<C> view = createDisplay(nodeInfo, cell);

    // Create a pager and wrap the components in a scrollable container.
    ScrollPanel scrollable = new ScrollPanel();
    final Widget pager = createPager(view);
    if (pager != null) {
      FlowPanel flowPanel = new FlowPanel();
      flowPanel.add(view);
      flowPanel.add(pager);
      scrollable.setWidget(flowPanel);
    } else {
      scrollable.setWidget(view);
    }
    scrollable.setStyleName(style.cellBrowserColumn());
    if (level == 0) {
      scrollable.addStyleName(style.cellBrowserFirstColumn());
    }

    // Create a TreeNode.
    TreeNodeImpl<C> treeNode = new TreeNodeImpl<C>(
        nodeInfo, value, view, cell, scrollable);
    treeNodes.add(treeNode);

    // Attach the view to the selection model and node info.
    view.setSelectionModel(nodeInfo.getSelectionModel());
    nodeInfo.setDataDisplay(view);

    // Add the view to the LayoutPanel.
    SplitLayoutPanel splitPanel = getSplitLayoutPanel();
    splitPanel.insertLineStart(scrollable, defaultWidth, null);
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
  private SafeHtml getImageHtml(ImageResource res) {
    // Right-justify image if LTR, left-justify if RTL

    // Note: templates can't handle the URL currently

    // Note: closing the tag with /> causes tests to fail
    // in dev mode with HTMLUnit -- the close tag is lost
    // when calling setInnerHTML on an Element.
    // TODO(rice) find and fix the root cause of this failure

    // CHECKSTYLE_OFF
    return SafeHtmlUtils.fromTrustedString("<div style=\"position:absolute;"
        + (LocaleInfo.getCurrentLocale().isRTL() ? "left" : "right")
        + ":0px;top:0px;height:100%;width:" + res.getWidth()
        + "px;background:url('" + res.getURL()
        + "') no-repeat scroll center center transparent;\"></div>");
    // CHECKSTYLE_ON
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
   * Set the open state of a tree node.
   *
   * @param cell the Cell that changed state.
   * @param value the value to open
   * @param open true to open, false to close
   * @param fireEvents true to fireEvents
   * @return the open {@link TreeNode}, or null if not opened
   */
  private <C> TreeNode setChildState(
      CellDecorator<C> cell, C value, boolean open, boolean fireEvents) {

    // Early exit if the node is a leaf.
    if (isLeaf(value)) {
      return null;
    }

    // Get the key of the value to open.
    Object newKey = cell.getValueKey(value);

    if (open) {
      if (newKey == null) {
        // Early exit if opening but the specified node has no key.
        return null;
      } else if (newKey.equals(cell.openKey)) {
        // Early exit if opening but the specified node is already open.
        return treeNodes.get(cell.level + 1);
      }

      // Close the currently open node.
      if (cell.openKey != null) {
        setChildState(cell, cell.openValue, false, fireEvents);
      }

      // Get the child node info.
      NodeInfo<?> childNodeInfo = getNodeInfo(value);
      if (childNodeInfo == null) {
        return null;
      }

      // Update the cell so it renders the styles correctly.
      cell.openValue = value;
      cell.openKey = cell.getValueKey(value);

      // Refresh the display to update the styles for this node.
      treeNodes.get(cell.level).display.redraw();

      // Add the child node.
      appendTreeNode(childNodeInfo, value);

      if (fireEvents) {
        OpenEvent.fire(this, treeNodes.get(cell.level + 1));
      }
      return treeNodes.get(cell.level + 1);
    } else {
      // Early exit if closing and the specified node or all nodes are closed.
      if (cell.openKey == null || !cell.openKey.equals(newKey)) {
        return null;
      }

      // Close the node.
      TreeNode closedNode = treeNodes.get(cell.level + 1);
      trimToLevel(cell.level);
      cell.openKey = null;
      cell.openValue = null;

      // Refresh the display to update the styles for this node.
      treeNodes.get(cell.level).display.redraw();

      if (fireEvents) {
        CloseEvent.fire(this, closedNode);
      }
    }

    return null;
  }

  /**
   * Reduce the number of {@link HasData}s down to the specified level.
   *
   * @param level the level to trim to
   */
  private void trimToLevel(int level) {
    // Add a placeholder to maintain the same scroll width.
    adjustScrollLock();

    // Remove the views that are no longer needed.
    int curLevel = treeNodes.size() - 1;
    while (curLevel > level) {
      TreeNodeImpl<?> removed = treeNodes.remove(curLevel);
      removed.destroy();
      curLevel--;
    }
  }
}
