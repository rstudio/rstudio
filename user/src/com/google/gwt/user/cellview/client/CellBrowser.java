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
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.KeyCodes;
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
import com.google.gwt.user.client.ui.AbstractImagePrototype;
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
import java.util.List;

/**
 * A "browsable" view of a tree in which only a single node per level may be
 * open at one time.
 * 
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that
 * the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 *
 * <p>
 * <h3>Example</h3>
 * <dl>
 * <dt>Trivial example</dt>
 * <dd>{@example com.google.gwt.examples.cellview.CellBrowserExample}</dd>
 * <dt>Complex example</dt>
 * <dd>{@example com.google.gwt.examples.cellview.CellBrowserExample2}</dd>
 * </dl>
 */
public class CellBrowser extends AbstractCellTree implements ProvidesResize,
    RequiresResize, HasAnimation {

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
    // Use RepeatStyle.BOTH to ensure that we do not bundle the image.
    @ImageOptions(repeatStyle = RepeatStyle.Both, flipRtl = true)
    ImageResource cellBrowserOpenBackground();

    /**
     * The background used for selected items.
     */
    // Use RepeatStyle.BOTH to ensure that we do not bundle the image.
    @Source("cellTreeSelectedBackground.png")
    @ImageOptions(repeatStyle = RepeatStyle.Both, flipRtl = true)
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
     * Applied to even list items.
     */
    String cellBrowserEvenItem();

    /**
     * Applied to the first column.
     */
    String cellBrowserFirstColumn();

    /***
     * Applied to keyboard selected items.
     */
    String cellBrowserKeyboardSelectedItem();

    /**
     * Applied to odd list items.
     */
    String cellBrowserOddItem();

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

  interface Template extends SafeHtmlTemplates {
    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\" style=\"position:relative;padding-right:{2}px;outline:none;\">{3}<div>{4}</div></div>")
    SafeHtml div(int idx, String classes, int imageWidth, SafeHtml imageHtml,
        SafeHtml cellContents);

    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\" style=\"position:relative;padding-right:{2}px;outline:none;\" tabindex=\"{3}\">{4}<div>{5}</div></div>")
    SafeHtml divFocusable(int idx, String classes, int imageWidth,
        int tabIndex, SafeHtml imageHtml, SafeHtml cellContents);

    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\" style=\"position:relative;padding-right:{2}px;outline:none;\" tabindex=\"{3}\" accessKey=\"{4}\">{5}<div>{6}</div></div>")
    SafeHtml divFocusableWithKey(int idx, String classes, int imageWidth,
        int tabIndex, char accessKey, SafeHtml imageHtml, SafeHtml cellContents);

    @Template("<div style=\"position:absolute;{0}:0px;width:{1}px;"
        + "height:{2}px;\">{3}</div>")
    SafeHtml imageWrapper(String direction, int width, int height,
        SafeHtml image);
  }

  /**
   * A custom version of cell list used by the browser. Visible for testing.
   *
   * @param <T> the data type of list items
   */
  class BrowserCellList<T> extends CellList<T> {

    /**
     * The level of this list view.
     */
    private final int level;

    /**
     * The key of the currently focused item.
     */
    private Object focusedKey;

    /**
     * The value of the currently focused item.
     */
    private T focusedValue;

    /**
     * Indicates whether or not the focused value is open.
     */
    private boolean isFocusedOpen;

    public BrowserCellList(final Cell<T> cell, int level,
        ProvidesKey<T> keyProvider) {
      super(cell, cellListResources, keyProvider);
      this.level = level;
    }

    @Override
    protected Element getCellParent(Element item) {
      return item.getFirstChildElement().getNextSiblingElement();
    }

    @Override
    protected void onBrowserEvent2(Event event) {
      super.onBrowserEvent2(event);

      // Handle keyboard navigation between lists.
      String eventType = event.getType();
      if ("keydown".equals(eventType) && !isKeyboardNavigationSuppressed()) {
        int keyCode = event.getKeyCode();
        switch (keyCode) {
          case KeyCodes.KEY_LEFT:
            if (LocaleInfo.getCurrentLocale().isRTL()) {
              keyboardNavigateDeep();
            } else {
              keyboardNavigateShallow();
            }
            return;
          case KeyCodes.KEY_RIGHT:
            if (LocaleInfo.getCurrentLocale().isRTL()) {
              keyboardNavigateShallow();
            } else {
              keyboardNavigateDeep();
            }
            return;
        }
      }
    }

    @Override
    protected void renderRowValues(SafeHtmlBuilder sb, List<T> values,
        int start, SelectionModel<? super T> selectionModel) {
      Cell<T> cell = getCell();
      String keyboardSelectedItem = " "
          + style.cellBrowserKeyboardSelectedItem();
      String selectedItem = " " + style.cellBrowserSelectedItem();
      String openItem = " " + style.cellBrowserOpenItem();
      String evenItem = style.cellBrowserEvenItem();
      String oddItem = style.cellBrowserOddItem();
      int keyboardSelectedRow = getKeyboardSelectedRow() + getPageStart();
      int length = values.size();
      int end = start + length;
      for (int i = start; i < end; i++) {
        T value = values.get(i - start);
        Object key = getValueKey(value);
        boolean isSelected = selectionModel == null ? false
            : selectionModel.isSelected(value);
        boolean isOpen = (focusedKey == null || !isFocusedOpen) ? false
            : focusedKey.equals(key);
        StringBuilder classesBuilder = new StringBuilder();
        classesBuilder.append(i % 2 == 0 ? evenItem : oddItem);
        if (isOpen) {
          classesBuilder.append(openItem);
        }
        if (isSelected) {
          classesBuilder.append(selectedItem);
        }

        SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
        cell.render(value, null, cellBuilder);

        // Figure out which image to use.
        SafeHtml image;
        if (isOpen) {
          image = openImageHtml;
        } else if (isLeaf(value)) {
          image = LEAF_IMAGE;
        } else {
          image = closedImageHtml;
        }

        if (i == keyboardSelectedRow) {
          // This is the focused item.
          if (isFocused) {
            classesBuilder.append(keyboardSelectedItem);
          }
          char accessKey = getAccessKey();
          if (accessKey != 0) {
            sb.append(template.divFocusableWithKey(i,
                classesBuilder.toString(), imageWidth, getTabIndex(),
                getAccessKey(), image, cellBuilder.toSafeHtml()));
          } else {
            sb.append(template.divFocusable(i, classesBuilder.toString(),
                imageWidth, getTabIndex(), image, cellBuilder.toSafeHtml()));
          }
        } else {
          sb.append(template.div(i, classesBuilder.toString(), imageWidth,
              image, cellBuilder.toSafeHtml()));
        }
      }
    }

    @Override
    void doKeyboardSelection(Event event, T value, int indexOnPage) {
      super.doKeyboardSelection(event, value, indexOnPage);

      // Open the selected row. If keyboard selection updates the selection
      // model, this is a no-op.
      setChildState(this, value, true, true, true);
    }

    /**
     * Navigate to a deeper node.
     */
    private void keyboardNavigateDeep() {
      if (isKeyboardSelectionDisabled()) {
        return;
      }

      // Move to the child node.
      if (level < treeNodes.size() - 1) {
        TreeNodeImpl<?> treeNode = treeNodes.get(level + 1);
        treeNode.display.setFocus(true);

        // Select the element.
        int selected = getKeyboardSelectedRow();
        if (isRowWithinBounds(selected)) {
          T value = getDisplayedItem(selected);
          setChildState(this, value, true, true, true);
        }
      }
    }

    /**
     * Navigate to a shallower node.
     */
    private void keyboardNavigateShallow() {
      if (isKeyboardSelectionDisabled()) {
        return;
      }

      // Move to the parent node.
      if (level > 0) {
        TreeNodeImpl<?> treeNode = treeNodes.get(level - 1);
        treeNode.display.setFocus(true);
      }
    }
  }

  /**
   * A node in the tree.
   *
   * @param <C> the data type of the children of the node
   */
  class TreeNodeImpl<C> implements TreeNode {
    private final BrowserCellList<C> display;
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
     * @param widget the widget that wraps the display
     */
    public TreeNodeImpl(final NodeInfo<C> nodeInfo, Object value,
        final BrowserCellList<C> display, Widget widget) {
      this.display = display;
      this.nodeInfo = nodeInfo;
      this.value = value;
      this.widget = widget;

      // Trim to the current level if the open node disappears.
      valueChangeHandler = display.addValueChangeHandler(new ValueChangeHandler<List<C>>() {
        public void onValueChange(ValueChangeEvent<List<C>> event) {
          Object focusedKey = display.focusedKey;
          if (focusedKey != null) {
            boolean stillExists = false;
            List<C> displayValues = event.getValue();
            for (C displayValue : displayValues) {
              if (focusedKey.equals(display.getValueKey(displayValue))) {
                stillExists = true;
                break;
              }
            }
            if (!stillExists) {
              trimToLevel(display.level);
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
      return (display.level == 0) ? null : treeNodes.get(display.level - 1);
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
      return (display.focusedKey == null || !display.isFocusedOpen)
          ? false
          : display.focusedKey.equals(display.getValueKey(getChildValue(index)));
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
      return setChildState(display, getChildValue(index), open, fireEvents,
          true);
    }

    /**
     * Return the key of the value that is focused in this node's display.
     */
    Object getFocusedKey() {
      return display.focusedKey;
    }

    /**
     * Return true if the focused value is open, false if not.
     */
    boolean isFocusedOpen() {
      return display.isFocusedOpen;
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
      return display.isFocusedOpen ? display.indexOf(display.focusedValue)
          : null;
    }
  }

  /**
   * An implementation of {@link CellList.Resources} that delegates to
   * {@link CellBrowser.Resources}.
   */
  private static class CellListResourcesImpl implements CellList.Resources {

    private final CellBrowser.Resources delegate;
    private final CellListStyleImpl style;

    public CellListResourcesImpl(CellBrowser.Resources delegate) {
      this.delegate = delegate;
      this.style = new CellListStyleImpl(delegate.cellBrowserStyle());
    }

    public ImageResource cellListSelectedBackground() {
      return delegate.cellBrowserSelectedBackground();
    }

    public CellList.Style cellListStyle() {
      return style;
    }
  }

  /**
   * An implementation of {@link CellList.Style} that delegates to
   * {@link CellBrowser.Style}.
   */
  private static class CellListStyleImpl implements CellList.Style {

    private final CellBrowser.Style delegate;

    public CellListStyleImpl(CellBrowser.Style delegate) {
      this.delegate = delegate;
    }

    public String cellListEvenItem() {
      return delegate.cellBrowserEvenItem();
    }

    public String cellListKeyboardSelectedItem() {
      return delegate.cellBrowserKeyboardSelectedItem();
    }

    public String cellListOddItem() {
      return delegate.cellBrowserOddItem();
    }

    public String cellListSelectedItem() {
      return delegate.cellBrowserSelectedItem();
    }

    public String cellListWidget() {
      // Do not apply any style to the list itself.
      return null;
    }

    public boolean ensureInjected() {
      return delegate.ensureInjected();
    }

    public String getName() {
      return delegate.getName();
    }

    public String getText() {
      return delegate.getText();
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

  private static Resources DEFAULT_RESOURCES;

  /**
   * The element used in place of an image when a node has no children.
   */
  private static final SafeHtml LEAF_IMAGE = SafeHtmlUtils.fromSafeConstant("<div style='position:absolute;display:none;'></div>");

  private static Template template;

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  /**
   * The visible {@link TreeNodeImpl}s. Visible for testing.
   */
  final List<TreeNodeImpl<?>> treeNodes = new ArrayList<TreeNodeImpl<?>>();

  /**
   * The animation used for scrolling.
   */
  private final ScrollAnimation animation = new ScrollAnimation();

  /**
   * The resources used by the {@link CellList}.
   */
  private final CellList.Resources cellListResources;

  /**
   * The HTML used to generate the closed image.
   */
  private final SafeHtml closedImageHtml;

  /**
   * The default width of new columns.
   */
  private int defaultWidth = 200;

  /**
   * The maximum width of the open and closed images.
   */
  private final int imageWidth;

  /**
   * A boolean indicating whether or not animations are enabled.
   */
  private boolean isAnimationEnabled;

  /**
   * The minimum width of new columns.
   */
  private int minWidth;

  /**
   * The HTML used to generate the open image.
   */
  private final SafeHtml openImageHtml;

  /**
   * The element used to maintain the scrollbar when columns are removed.
   */
  private Element scrollLock;

  /**
   * The styles used by this widget.
   */
  private final Style style;

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
    super(viewModel);
    if (template == null) {
      template = GWT.create(Template.class);
    }
    this.style = resources.cellBrowserStyle();
    this.style.ensureInjected();
    this.cellListResources = new CellListResourcesImpl(resources);
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
   * @see #setDefaultColumnWidth(int)
   */
  public int getDefaultColumnWidth() {
    return defaultWidth;
  }

  /**
   * Get the minimum width of columns.
   *
   * @return the minimum width in pixels
   * @see #setMinimumColumnWidth(int)
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
   * @see #getDefaultColumnWidth()
   */
  public void setDefaultColumnWidth(int width) {
    this.defaultWidth = width;
  }

  @Override
  public void setKeyboardSelectionPolicy(KeyboardSelectionPolicy policy) {
    super.setKeyboardSelectionPolicy(policy);
    for (TreeNodeImpl<?> treeNode : treeNodes) {
      treeNode.display.setKeyboardSelectionPolicy(policy);
    }
  }

  /**
   * Set the minimum width of columns.
   *
   * @param minWidth the minimum width in pixels
   * @see #getMinimumColumnWidth()
   */
  public void setMinimumColumnWidth(int minWidth) {
    this.minWidth = minWidth;
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
    final BrowserCellList<C> view = createDisplay(nodeInfo, level);

    // Create a pager and wrap the components in a scrollable container. Set the
    // tabIndex to -1 so the user can tab between lists without going through
    // the scrollable.
    ScrollPanel scrollable = new ScrollPanel();
    scrollable.getElement().setTabIndex(-1);
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
    TreeNodeImpl<C> treeNode = new TreeNodeImpl<C>(nodeInfo, value, view,
        scrollable);
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
   * Create a {@link HasData} that will display items. The {@link HasData} must
   * extend {@link Widget}.
   *
   * @param <C> the item type in the list view
   * @param nodeInfo the node info with child data
   * @param level the level of the list
   * @return the {@link HasData}
   */
  private <C> BrowserCellList<C> createDisplay(NodeInfo<C> nodeInfo, int level) {
    BrowserCellList<C> display = new BrowserCellList<C>(nodeInfo.getCell(),
        level, nodeInfo.getProvidesKey());
    display.setValueUpdater(nodeInfo.getValueUpdater());
    display.setKeyboardSelectionPolicy(getKeyboardSelectionPolicy());
    return display;
  }

  /**
   * Get the HTML representation of an image.
   *
   * @param res the {@link ImageResource} to render as HTML
   * @return the rendered HTML
   */
  private SafeHtml getImageHtml(ImageResource res) {
    // Right-justify image if LTR, left-justify if RTL
    AbstractImagePrototype proto = AbstractImagePrototype.create(res);
    SafeHtml image = SafeHtmlUtils.fromTrustedString(proto.getHTML());
    return template.imageWrapper((LocaleInfo.getCurrentLocale().isRTL()
        ? "left" : "right"), res.getWidth(), res.getHeight(), image);
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
   * @param cellList the CellList that changed state.
   * @param value the value to open
   * @param open true to open, false to close
   * @param fireEvents true to fireEvents
   * @return the open {@link TreeNode}, or null if not opened
   */
  private <C> TreeNode setChildState(BrowserCellList<C> cellList, C value,
      boolean open, boolean fireEvents, boolean redraw) {

    // Get the key of the value to open.
    Object newKey = cellList.getValueKey(value);

    if (open) {
      if (newKey == null) {
        // Early exit if opening but the specified node has no key.
        return null;
      } else if (newKey.equals(cellList.focusedKey)) {
        // Early exit if opening but the specified node is already open.
        return cellList.isFocusedOpen ? treeNodes.get(cellList.level + 1)
            : null;
      }

      // Close the currently open node.
      if (cellList.focusedKey != null) {
        setChildState(cellList, cellList.focusedValue, false, fireEvents, false);
      }

      // Update the cell so it renders the styles correctly.
      cellList.focusedValue = value;
      cellList.focusedKey = cellList.getValueKey(value);

      // Add the child node.
      NodeInfo<?> childNodeInfo = isLeaf(value) ? null : getNodeInfo(value);
      if (childNodeInfo != null) {
        cellList.isFocusedOpen = true;
        appendTreeNode(childNodeInfo, value);
      } else {
        cellList.isFocusedOpen = false;
      }

      // Refresh the display to update the styles for this node.
      if (redraw) {
        treeNodes.get(cellList.level).display.redraw();
      }

      if (cellList.isFocusedOpen) {
        TreeNodeImpl<?> node = treeNodes.get(cellList.level + 1);
        if (fireEvents) {
          OpenEvent.fire(this, node);
        }
        return node.isDestroyed() ? null : node;
      }
      return null;
    } else {
      // Early exit if closing and the specified node or all nodes are closed.
      if (cellList.focusedKey == null || !cellList.focusedKey.equals(newKey)) {
        return null;
      }

      // Close the node.
      TreeNode closedNode = (cellList.isFocusedOpen && (treeNodes.size() > cellList.level + 1))
          ? treeNodes.get(cellList.level + 1) : null;
      trimToLevel(cellList.level);

      // Refresh the display to update the styles for this node.
      if (redraw) {
        treeNodes.get(cellList.level).display.redraw();
      }

      if (fireEvents && closedNode != null) {
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

    // Nullify the focused key at the level.
    if (level < treeNodes.size()) {
      TreeNodeImpl<?> node = treeNodes.get(level);
      node.display.focusedKey = null;
      node.display.focusedValue = null;
      node.display.isFocusedOpen = false;
    }
  }
}
