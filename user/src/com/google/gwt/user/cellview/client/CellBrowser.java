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
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
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
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safecss.shared.SafeStylesUtils;
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
import com.google.gwt.view.client.HasRows;
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
public class CellBrowser extends AbstractCellTree implements ProvidesResize, RequiresResize,
    HasAnimation {

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
    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\""
        + " style=\"{2}position:relative;outline:none;\">{3}<div>{4}</div></div>")
    SafeHtml div(int idx, String classes, SafeStyles padding, SafeHtml imageHtml,
        SafeHtml cellContents);

    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\""
        + " style=\"{2}position:relative;outline:none;\" tabindex=\"{3}\">{4}<div>{5}</div></div>")
    SafeHtml divFocusable(int idx, String classes, SafeStyles padding, int tabIndex,
        SafeHtml imageHtml, SafeHtml cellContents);

    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\""
        + " style=\"{2}position:relative;outline:none;\" tabindex=\"{3}\" accessKey=\"{4}\">{5}<div>{6}</div></div>")
    SafeHtml divFocusableWithKey(int idx, String classes, SafeStyles padding, int tabIndex,
        char accessKey, SafeHtml imageHtml, SafeHtml cellContents);

    @Template("<div style=\"{0}position:absolute;\">{1}</div>")
    SafeHtml imageWrapper(SafeStyles css, SafeHtml image);
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
     * The currently selected value in this list.
     */
    private T selectedValue;

    /**
     * A boolean indicating that this widget is no longer used.
     */
    private boolean isDestroyed;

    /**
     * Indicates whether or not the focused value is open.
     */
    private boolean isFocusedOpen;

    /**
     * Temporary element used to create elements from HTML.
     */
    private final Element tmpElem = Document.get().createDivElement();

    public BrowserCellList(final Cell<T> cell, int level, ProvidesKey<T> keyProvider) {
      super(cell, cellListResources, keyProvider);
      this.level = level;
    }

    protected void deselectValue() {
      SelectionModel<? super T> selectionModel = getSelectionModel();
      if (selectionModel != null && selectedValue != null) {
        selectionModel.setSelected(selectedValue, false);
      }
    }

    @Override
    protected Element getCellParent(Element item) {
      return item.getFirstChildElement().getNextSiblingElement();
    }

    @Override
    protected boolean isKeyboardNavigationSuppressed() {
      /*
       * Keyboard selection is never disabled in this list because we use it to
       * track the open node, but we want to suppress keyboard navigation if the
       * user disables it.
       */
      return KeyboardSelectionPolicy.DISABLED == CellBrowser.this.getKeyboardSelectionPolicy()
          || super.isKeyboardNavigationSuppressed();
    }

    @Override
    protected void onBrowserEvent2(Event event) {
      super.onBrowserEvent2(event);

      // Handle keyboard navigation between lists.
      String eventType = event.getType();
      if (BrowserEvents.KEYDOWN.equals(eventType) && !isKeyboardNavigationSuppressed()) {
        int keyCode = event.getKeyCode();
        boolean isRtl = LocaleInfo.getCurrentLocale().isRTL();
        keyCode = KeyCodes.maybeSwapArrowKeysForRtl(keyCode, isRtl);
        switch (keyCode) {
          case KeyCodes.KEY_LEFT:
            keyboardNavigateShallow();
            return;
          case KeyCodes.KEY_RIGHT:
            keyboardNavigateDeep();
            return;
        }
      }
    }

    @Override
    protected void renderRowValues(SafeHtmlBuilder sb, List<T> values, int start,
        SelectionModel<? super T> selectionModel) {
      Cell<T> cell = getCell();
      String keyboardSelectedItem = " " + style.cellBrowserKeyboardSelectedItem();
      String selectedItem = " " + style.cellBrowserSelectedItem();
      String openItem = " " + style.cellBrowserOpenItem();
      String evenItem = style.cellBrowserEvenItem();
      String oddItem = style.cellBrowserOddItem();
      int keyboardSelectedRow = getKeyboardSelectedRow() + getPageStart();
      int length = values.size();
      int end = start + length;
      for (int i = start; i < end; i++) {
        T value = values.get(i - start);
        boolean isSelected = selectionModel == null ? false : selectionModel.isSelected(value);
        boolean isOpen = isOpen(i);
        StringBuilder classesBuilder = new StringBuilder();
        classesBuilder.append(i % 2 == 0 ? evenItem : oddItem);
        if (isOpen) {
          classesBuilder.append(openItem);
        }
        if (isSelected) {
          classesBuilder.append(selectedItem);
        }

        SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
        Context context = new Context(i, 0, getValueKey(value));
        cell.render(context, value, cellBuilder);

        // Figure out which image to use.
        SafeHtml image;
        if (isOpen) {
          image = openImageHtml;
        } else if (isLeaf(value)) {
          image = LEAF_IMAGE;
        } else {
          image = closedImageHtml;
        }

        SafeStyles padding =
            SafeStylesUtils.fromTrustedString("padding-right: " + imageWidth + "px;");
        if (i == keyboardSelectedRow) {
          // This is the focused item.
          if (isFocused) {
            classesBuilder.append(keyboardSelectedItem);
          }
          char accessKey = getAccessKey();
          if (accessKey != 0) {
            sb.append(template.divFocusableWithKey(i, classesBuilder.toString(), padding,
                getTabIndex(), getAccessKey(), image, cellBuilder.toSafeHtml()));
          } else {
            sb.append(template.divFocusable(i, classesBuilder.toString(), padding, getTabIndex(),
                image, cellBuilder.toSafeHtml()));
          }
        } else {
          sb.append(template.div(i, classesBuilder.toString(), padding, image, cellBuilder
              .toSafeHtml()));
        }
      }

      // Update the child state.
      updateChildState(this, true);
    }

    @Override
    protected void setKeyboardSelected(int index, boolean selected, boolean stealFocus) {
      super.setKeyboardSelected(index, selected, stealFocus);
      if (!isRowWithinBounds(index)) {
        return;
      }

      // Update the style.
      Element elem = getRowElement(index);
      T value = getPresenter().getVisibleItem(index);
      boolean isOpen = selected && isOpen(index);
      setStyleName(elem, style.cellBrowserOpenItem(), isOpen);

      // Update the image.
      SafeHtml image = null;
      if (isOpen) {
        image = openImageHtml;
      } else if (getTreeViewModel().isLeaf(value)) {
        image = LEAF_IMAGE;
      } else {
        image = closedImageHtml;
      }
      tmpElem.setInnerSafeHtml(image);
      elem.replaceChild(tmpElem.getFirstChildElement(), elem.getFirstChildElement());

      // Update the open state.
      updateChildState(this, true);
    }

    /**
     * Set the selected value in this list. If there is already a selected
     * value, the old value will be deselected.
     * 
     * @param value the selected value
     */
    protected void setSelectedValue(T value) {
      // Early exit if the value is unchanged.
      Object oldKey = getValueKey(selectedValue);
      Object newKey = getValueKey(value);
      if (newKey != null && newKey.equals(oldKey)) {
        return;
      }

      // Deselect the current value. Only one thing is selected at a time.
      deselectValue();

      // Select the new value.
      SelectionModel<? super T> selectionModel = getSelectionModel();
      if (selectionModel != null) {
        selectedValue = value;
        selectionModel.setSelected(selectedValue, true);
      }
    }

    /**
     * Check if the specified index is currently open. An index is open if it is
     * the keyboard selected index, there is an associated keyboard selected
     * value, and the value is not a leaf.
     * 
     * @param index the index
     * @return true if open, false if not
     */
    private boolean isOpen(int index) {
      T value = getPresenter().getKeyboardSelectedRowValue();
      return index == getKeyboardSelectedRow() && value != null
          && !getTreeViewModel().isLeaf(value);
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
        treeNode.display.getPresenter().setKeyboardSelectedRow(
            treeNode.display.getKeyboardSelectedRow(), true, true);
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
    public TreeNodeImpl(final NodeInfo<C> nodeInfo, Object value, final BrowserCellList<C> display,
        Widget widget) {
      this.display = display;
      this.nodeInfo = nodeInfo;
      this.value = value;
      this.widget = widget;

      // Trim to the current level if the open node disappears.
      valueChangeHandler = display.addValueChangeHandler(new ValueChangeHandler<List<C>>() {
        @Override
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

    @Override
    public int getChildCount() {
      assertNotDestroyed();
      return display.getPresenter().getVisibleItemCount();
    }

    @Override
    public C getChildValue(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      return display.getVisibleItem(index);
    }

    @Override
    public int getIndex() {
      assertNotDestroyed();
      TreeNodeImpl<?> parent = getParent();
      return (parent == null) ? 0 : parent.getOpenIndex();
    }

    @Override
    public TreeNodeImpl<?> getParent() {
      assertNotDestroyed();
      return getParentImpl();
    }

    @Override
    public Object getValue() {
      return value;
    }

    @Override
    public boolean isChildLeaf(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      return isLeaf(getChildValue(index));
    }

    @Override
    public boolean isChildOpen(int index) {
      assertNotDestroyed();
      checkChildBounds(index);
      return (display.focusedKey == null || !display.isFocusedOpen) ? false : display.focusedKey
          .equals(display.getValueKey(getChildValue(index)));
    }

    @Override
    public boolean isDestroyed() {
      if (nodeInfo != null) {
        /*
         * Flush the parent display because the user may have replaced this
         * node, which would destroy it.
         */
        TreeNodeImpl<?> parent = getParentImpl();
        if (parent != null && !parent.isDestroyed()) {
          parent.display.getPresenter().flush();
        }
      }
      return nodeInfo == null;
    }

    @Override
    public TreeNode setChildOpen(int index, boolean open) {
      return setChildOpen(index, open, true);
    }

    @Override
    public TreeNode setChildOpen(int index, boolean open, boolean fireEvents) {
      assertNotDestroyed();
      checkChildBounds(index);
      if (open) {
        // Open the child node.
        display.getPresenter().setKeyboardSelectedRow(index, false, true);
        return updateChildState(display, fireEvents);
      } else {
        // Close the child node if it is currently open.
        if (index == display.getKeyboardSelectedRow()) {
          display.getPresenter().clearKeyboardSelectedRowValue();
          updateChildState(display, fireEvents);
        }
        return null;
      }
    }

    BrowserCellList<C> getDisplay() {
      return display;
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
      display.isDestroyed = true;
      valueChangeHandler.removeHandler();
      display.deselectValue();
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
      return display.isFocusedOpen ? display.getKeyboardSelectedRow() : -1;
    }

    /**
     * Get the parent node without checking if this node is destroyed.
     * 
     * @return the parent node, or null if the node has no parent
     */
    private TreeNodeImpl<?> getParentImpl() {
      return (display.level == 0) ? null : treeNodes.get(display.level - 1);
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

    @Override
    public ImageResource cellListSelectedBackground() {
      return delegate.cellBrowserSelectedBackground();
    }

    @Override
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

    @Override
    public String cellListEvenItem() {
      return delegate.cellBrowserEvenItem();
    }

    @Override
    public String cellListKeyboardSelectedItem() {
      return delegate.cellBrowserKeyboardSelectedItem();
    }

    @Override
    public String cellListOddItem() {
      return delegate.cellBrowserOddItem();
    }

    @Override
    public String cellListSelectedItem() {
      return delegate.cellBrowserSelectedItem();
    }

    @Override
    public String cellListWidget() {
      // Do not apply any style to the list itself.
      return null;
    }

    @Override
    public boolean ensureInjected() {
      return delegate.ensureInjected();
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
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
        run(250, elem);
      } else {
        // Scroll instantly.
        onComplete();
      }
    }
  }

  /**
   * Pager factory used to create pagers for each {@link CellList} of the
   * {@link CellBrowser}.
   */
  public static interface PagerFactory {
    AbstractPager create(HasRows display);
  }

  /**
   * Default pager.
   */
  private static class PageSizePagerFactory implements PagerFactory {
    @Override
    public AbstractPager create(HasRows display) {
      return new PageSizePager(display.getVisibleRange().getLength());
    }
  }

  /**
   * Builder object to create CellBrowser.
   *
   * @param <T> the type of data in the root node
   */
  public static class Builder<T> {
    private final TreeViewModel viewModel;
    private final T rootValue;
    private Widget loadingIndicator;
    private PagerFactory pagerFactory = new PageSizePagerFactory();
    private Integer pageSize;
    private Resources resources;

    /**
    * Construct a new {@link Builder}.
    *
    * @param viewModel the {@link TreeViewModel} that backs the tree
    * @param rootValue the hidden root value of the tree
    */
    public Builder(TreeViewModel viewModel, T rootValue) {
      this.viewModel = viewModel;
      this.rootValue = rootValue;
    }

    /**
     * Creates a new {@link CellBrowser}.
     *
     * @return new {@link CellBrowser}
     */
    public CellBrowser build() {
      return new CellBrowser(this);
    }

    /**
     * Set the widget to display when the data is loading.
     *
     * @param widget the loading indicator
     * @return this
     */
    public Builder<T> loadingIndicator(Widget widget) {
      this.loadingIndicator = widget;
      return this;
    }

    /**
     * Set the pager factory used to create pagers for each {@link CellList}.
     * Defaults to {@link PageSizePagerFactory} if not set.
     *
     * Can be set to null if no pager should be used. You should also set pageSize
     * big enough to hold all your data then.
     *
     * @param factory the pager factory
     * @return this
     */
    public Builder<T> pagerFactory(PagerFactory factory) {
      this.pagerFactory = factory;
      return this;
    }

    /**
     * Set the pager size for each {@link CellList}.
     *
     * @param pageSize the page size
     * @return this
     */
    public Builder<T> pageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    /**
     * Set resources used for images.
     *
     * @param resources the {@link Resources} used for images
     * @return this
     */
    public Builder<T> resources(Resources resources) {
      this.resources = resources;
      return this;
    }

    private Resources resources() {
      if (resources == null) {
        resources = getDefaultResources();
      }
      return resources;
    }
  }

  private static Resources DEFAULT_RESOURCES;

  /**
   * The element used in place of an image when a node has no children.
   */
  private static final SafeHtml LEAF_IMAGE = SafeHtmlUtils
      .fromSafeConstant("<div style='position:absolute;display:none;'></div>");

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
   * Widget passed to CellLists.
   */
  private final Widget loadingIndicator;

  /**
   * The minimum width of new columns.
   */
  private int minWidth;

  /**
   * The HTML used to generate the open image.
   */
  private final SafeHtml openImageHtml;

  /**
   * Factory used to create pagers for CellLists.
   */
  private final PagerFactory pagerFactory;

  /**
   * Page size for CellLists.
   */
  private final Integer pageSize;

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
   *
   * @deprecated please use {@link Builder}
   */
  @Deprecated
  public <T> CellBrowser(TreeViewModel viewModel, T rootValue) {
    this(new Builder<T>(viewModel, rootValue));
  }

  /**
   * Construct a new {@link CellBrowser} with the specified {@link Resources}.
   * 
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   * @param resources the {@link Resources} used for images
   *
   * @deprecated please use {@link Builder}
   */
  @Deprecated
  public <T> CellBrowser(TreeViewModel viewModel, T rootValue, Resources resources) {
    this(new Builder<T>(viewModel, rootValue).resources(resources));
  }

  protected <T> CellBrowser(Builder<T> builder) {
    super(builder.viewModel);
    if (template == null) {
      template = GWT.create(Template.class);
    }
    Resources resources = builder.resources();
    this.style = resources.cellBrowserStyle();
    this.style.ensureInjected();
    this.cellListResources = new CellListResourcesImpl(resources);
    this.loadingIndicator = builder.loadingIndicator;
    this.pagerFactory = builder.pagerFactory;
    this.pageSize = builder.pageSize;
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
    appendTreeNode(getNodeInfo(builder.rootValue), builder.rootValue);

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

  @Override
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

  @Override
  public void onResize() {
    getSplitLayoutPanel().onResize();
  }

  @Override
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
    if (pagerFactory == null) {
      return null;
    }
    AbstractPager pager = pagerFactory.create(display);
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
  private <C> TreeNode appendTreeNode(final NodeInfo<C> nodeInfo, Object value) {
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
    TreeNodeImpl<C> treeNode = new TreeNodeImpl<C>(nodeInfo, value, view, scrollable);
    treeNodes.add(treeNode);

    /*
     * Attach the view to the selection model and node info. Nullify the default
     * selection manager because it is provided by the node info.
     */
    view.setSelectionModel(nodeInfo.getSelectionModel(), null);
    nodeInfo.setDataDisplay(view);

    // Add the view to the LayoutPanel.
    SplitLayoutPanel splitPanel = getSplitLayoutPanel();
    splitPanel.insertLineStart(scrollable, defaultWidth, null);
    splitPanel.setWidgetMinSize(scrollable, minWidth);
    splitPanel.forceLayout();

    // Scroll to the right.
    animation.scrollToEnd();
    return treeNode;
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
    BrowserCellList<C> display =
        new BrowserCellList<C>(nodeInfo.getCell(), level, nodeInfo.getProvidesKey());
    if (loadingIndicator != null) {
      display.setLoadingIndicator(loadingIndicator);
    }
    if (pageSize != null) {
      display.setPageSize(pageSize);
    }
    display.setValueUpdater(nodeInfo.getValueUpdater());

    /*
     * A CellBrowser has a single keyboard selection policy and multiple lists,
     * so we're not using the selection policy in each list. Leave them on all
     * the time because we use keyboard selection to keep track of which item is
     * open (selected) at each level.
     */
    display.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
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

    SafeStylesBuilder cssBuilder = new SafeStylesBuilder();
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      cssBuilder.appendTrustedString("left:0px;");
    } else {
      cssBuilder.appendTrustedString("right:0px;");
    }
    cssBuilder.appendTrustedString("width: " + res.getWidth() + "px;");
    cssBuilder.appendTrustedString("height: " + res.getHeight() + "px;");
    return template.imageWrapper(cssBuilder.toSafeStyles(), image);
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
      node.display.isFocusedOpen = false;
    }
  }

  /**
   * Update the state of a child node based on the keyboard selection of the
   * specified {@link BrowserCellList}. This method will open/close child
   * {@link TreeNode}s as needed.
   * 
   * @param cellList the CellList that changed state.
   * @param fireEvents true to fireEvents
   * @return the open {@link TreeNode}, or null if not opened
   */
  private <C> TreeNode updateChildState(BrowserCellList<C> cellList, boolean fireEvents) {
    /*
     * Verify that the specified list is still in the browser. It possible for
     * the list to receive deferred updates after it has been removed
     */
    if (cellList.isDestroyed) {
      return null;
    }

    // Get the key of the value to open.
    C newValue = cellList.getPresenter().getKeyboardSelectedRowValue();
    Object newKey = cellList.getValueKey(newValue);

    // Close the current open node.
    TreeNode closedNode = null;
    if (cellList.focusedKey != null && cellList.isFocusedOpen
        && !cellList.focusedKey.equals(newKey)) {
      // Get the node to close.
      closedNode =
          (treeNodes.size() > cellList.level + 1) ? treeNodes.get(cellList.level + 1) : null;

      // Close the node.
      trimToLevel(cellList.level);
    }

    // Open the new node.
    TreeNode openNode = null;
    boolean justOpenedNode = false;
    if (newKey != null) {
      if (newKey.equals(cellList.focusedKey)) {
        // The node is already open.
        openNode = cellList.isFocusedOpen ? treeNodes.get(cellList.level + 1) : null;
      } else {
        // Select this value.
        if (KeyboardSelectionPolicy.BOUND_TO_SELECTION == getKeyboardSelectionPolicy()) {
          cellList.setSelectedValue(newValue);
        }

        // Add the child node if this node has children.
        cellList.focusedKey = newKey;
        NodeInfo<?> childNodeInfo = isLeaf(newValue) ? null : getNodeInfo(newValue);
        if (childNodeInfo != null) {
          cellList.isFocusedOpen = true;
          justOpenedNode = true;
          openNode = appendTreeNode(childNodeInfo, newValue);
        }
      }
    }

    /*
     * Fire event. We fire events after updating the view in case user event
     * handlers modify the open state of nodes, which would interrupt the
     * process.
     */
    if (fireEvents) {
      if (closedNode != null) {
        CloseEvent.fire(this, closedNode);
      }
      if (openNode != null && justOpenedNode) {
        OpenEvent.fire(this, openNode);
      }
    }

    // Return the open node if it is still open.
    return (openNode == null || openNode.isDestroyed()) ? null : openNode;
  }
}
