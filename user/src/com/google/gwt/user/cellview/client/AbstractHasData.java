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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.HasDataPresenter.LoadingState;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.HasKeyProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An abstract {@link Widget} that implements {@link HasData}.
 *
 * @param <T> the data type of each row
 */
public abstract class AbstractHasData<T> extends Widget implements HasData<T>,
    HasKeyProvider<T>, Focusable, HasKeyboardPagingPolicy {

  /**
   * Implementation of {@link HasDataPresenter.View} used by this widget.
   *
   * @param <T> the data type of the view
   */
  private static class View<T> implements HasDataPresenter.View<T> {

    private final AbstractHasData<T> hasData;
    private boolean wasFocused;

    public View(AbstractHasData<T> hasData) {
      this.hasData = hasData;
    }

    public <H extends EventHandler> HandlerRegistration addHandler(H handler,
        Type<H> type) {
      return hasData.addHandler(handler, type);
    }

    public void render(SafeHtmlBuilder sb, List<T> values, int start,
        SelectionModel<? super T> selectionModel) {
      hasData.renderRowValues(sb, values, start, selectionModel);
    }

    public void replaceAllChildren(List<T> values, SafeHtml html,
        boolean stealFocus) {
      // Removing elements can fire a blur event, which we ignore.
      hasData.isFocused = hasData.isFocused || stealFocus;
      wasFocused = hasData.isFocused;
      hasData.isRefreshing = true;
      hasData.replaceAllChildren(values, html);
      hasData.isRefreshing = false;
      fireValueChangeEvent();
    }

    public void replaceChildren(List<T> values, int start, SafeHtml html,
        boolean stealFocus) {
      // Removing elements can fire a blur event, which we ignore.
      hasData.isFocused = hasData.isFocused || stealFocus;
      wasFocused = hasData.isFocused;
      hasData.isRefreshing = true;
      hasData.replaceChildren(values, start, html);
      hasData.isRefreshing = false;
      fireValueChangeEvent();
    }

    public void resetFocus() {
      if (wasFocused) {
        CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
          public void execute() {
            if (!hasData.resetFocusOnCell()) {
              Element elem = hasData.getKeyboardSelectedElement();
              if (elem != null) {
                elem.focus();
              }
            }
          }
        });
      }
    }

    public void setKeyboardSelected(int index, boolean seleted,
        boolean stealFocus) {
      hasData.isFocused = hasData.isFocused || stealFocus;
      hasData.setKeyboardSelected(index, seleted, stealFocus);
    }

    public void setLoadingState(LoadingState state) {
      hasData.isRefreshing = true;
      hasData.setLoadingState(state);
      hasData.isRefreshing = false;
    }

    /**
     * Fire a value change event.
     */
    private void fireValueChangeEvent() {
      // Use an anonymous class to override ValueChangeEvents's protected
      // constructor. We can't call ValueChangeEvent.fire() because this class
      // doesn't implement HasValueChangeHandlers.
      hasData.fireEvent(new ValueChangeEvent<List<T>>(
          hasData.getVisibleItems()) {
      });
    }
  }

  /**
   * The temporary element use to convert HTML to DOM.
   */
  private static com.google.gwt.user.client.Element tmpElem;

  /**
   * Convenience method to convert the specified HTML into DOM elements and
   * return the parent of the DOM elements.
   *
   * @param html the HTML to convert
   * @param tmpElem a temporary element
   * @return the parent element
   */
  static Element convertToElements(Widget widget,
      com.google.gwt.user.client.Element tmpElem, SafeHtml html) {
    // Attach an event listener so we can catch synchronous load events from
    // cached images.
    DOM.setEventListener(tmpElem, widget);

    tmpElem.setInnerHTML(html.asString());

    // Detach the event listener.
    DOM.setEventListener(tmpElem, null);

    return tmpElem;
  }

  /**
   * Convenience method to replace all children of a Widget.
   *
   * @param widget the widget who's contents will be replaced
   * @param childContainer the container that holds the contents
   * @param html the html to set
   */
  static void replaceAllChildren(Widget widget, Element childContainer,
      SafeHtml html) {
    // If the widget is not attached, attach an event listener so we can catch
    // synchronous load events from cached images.
    if (!widget.isAttached()) {
      DOM.setEventListener(widget.getElement(), widget);
    }

    // Render the HTML.
    childContainer.setInnerHTML(CellBasedWidgetImpl.get().processHtml(html).asString());

    // Detach the event listener.
    if (!widget.isAttached()) {
      DOM.setEventListener(widget.getElement(), null);
    }
  }

  /**
   * Convenience method to convert the specified HTML into DOM elements and
   * replace the existing elements starting at the specified index. If the
   * number of children specified exceeds the existing number of children, the
   * remaining children should be appended.
   *
   * @param widget the widget who's contents will be replaced
   * @param childContainer the container that holds the contents
   * @param newChildren an element containing the new children
   * @param start the start index to replace
   * @param html the HTML to convert
   */
  static void replaceChildren(Widget widget, Element childContainer,
      Element newChildren, int start, SafeHtml html) {
    // Get the first element to be replaced.
    int childCount = childContainer.getChildCount();
    Element toReplace = null;
    if (start < childCount) {
      toReplace = childContainer.getChild(start).cast();
    }

    // Replace the elements.
    int count = newChildren.getChildCount();
    for (int i = 0; i < count; i++) {
      if (toReplace == null) {
        // The child will be removed from tmpElem, so always use index 0.
        childContainer.appendChild(newChildren.getChild(0));
      } else {
        Element nextSibling = toReplace.getNextSiblingElement();
        childContainer.replaceChild(newChildren.getChild(0), toReplace);
        toReplace = nextSibling;
      }
    }
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
   * A boolean indicating that the widget has focus.
   */
  boolean isFocused;

  private char accessKey = 0;

  /**
   * A boolean indicating that the widget is refreshing, so all events should be
   * ignored.
   */
  private boolean isRefreshing;

  private final HasDataPresenter<T> presenter;
  private HandlerRegistration selectionManagerReg; 
  private int tabIndex;

  /**
   * Constructs an {@link AbstractHasData} with the given page size.
   * 
   * @param elem the parent {@link Element}
   * @param pageSize the page size
   * @param keyProvider the key provider, or null
   */
  public AbstractHasData(Element elem, final int pageSize,
      final ProvidesKey<T> keyProvider) {
    setElement(elem);
    this.presenter = new HasDataPresenter<T>(this, new View<T>(this), pageSize,
        keyProvider);

    // Sink events.
    Set<String> eventTypes = new HashSet<String>();
    eventTypes.add("focus");
    eventTypes.add("blur");
    eventTypes.add("keydown"); // Used for keyboard navigation.
    eventTypes.add("keyup"); // Used by subclasses for selection.
    eventTypes.add("click"); // Used by subclasses for selection.
    eventTypes.add("mousedown"); // No longer used, but here for legacy support.
    CellBasedWidgetImpl.get().sinkEvents(this, eventTypes);

    // Add a default selection event manager.
    selectionManagerReg = addCellPreviewHandler(
        DefaultSelectionEventManager.<T> createDefaultManager());
  }

  public HandlerRegistration addCellPreviewHandler(
      CellPreviewEvent.Handler<T> handler) {
    return presenter.addCellPreviewHandler(handler);
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
   * Get the access key.
   *
   * @return the access key, or -1 if not set
   * @see #setAccessKey(char)
   */
  public char getAccessKey() {
    return accessKey;
  }

  /**
   * Get the row value at the specified visible index. Index 0 corresponds to
   * the first item on the page.
   *
   * @param indexOnPage the index on the page
   * @return the row value
   * @deprecated use {@link #getVisibleItem(int)} instead
   */
  @Deprecated
  public T getDisplayedItem(int indexOnPage) {
    return getVisibleItem(indexOnPage);
  }

  /**
   * Return the row values that the widget is currently displaying as an
   * immutable list.
   * 
   * @return a List of displayed items
   * @deprecated use {@link #getVisibleItems()} instead
   */
  @Deprecated
  public List<T> getDisplayedItems() {
    return getVisibleItems();
  }

  public KeyboardPagingPolicy getKeyboardPagingPolicy() {
    return presenter.getKeyboardPagingPolicy();
  }

  public KeyboardSelectionPolicy getKeyboardSelectionPolicy() {
    return presenter.getKeyboardSelectionPolicy();
  }

  public ProvidesKey<T> getKeyProvider() {
    return presenter.getKeyProvider();
  }

  /**
   * Return the range size.
   * 
   * @return the size of the range as an int
   *
   * @see #getVisibleRange()
   * @see #setPageSize(int)
   */
  public final int getPageSize() {
    return getVisibleRange().getLength();
  }

  /**
   * Return the range start.
   *
   * @return the start of the range as an int
   *
   * @see #getVisibleRange()
   * @see #setPageStart(int)
   */
  public final int getPageStart() {
    return getVisibleRange().getStart();
  }

  /**
   * Return the outer element that contains all of the rendered row values. This
   * method delegates to {@link #getChildContainer()};
   * 
   * @return the {@link Element} that contains the rendered row values
   */
  public Element getRowContainer() {
    presenter.flush();
    return getChildContainer();
  }

  public int getRowCount() {
    return presenter.getRowCount();
  }

  public SelectionModel<? super T> getSelectionModel() {
    return presenter.getSelectionModel();
  }

  public int getTabIndex() {
    return tabIndex;
  }

  public T getVisibleItem(int indexOnPage) {
    checkRowBounds(indexOnPage);
    return presenter.getVisibleItem(indexOnPage);
  }

  public int getVisibleItemCount() {
    return presenter.getVisibleItemCount();
  }

  /**
   * Return the row values that the widget is currently displaying as an
   * immutable list.
   * 
   * @return a List of displayed items
   */
  public List<T> getVisibleItems() {
    return presenter.getVisibleItems();
  }

  public Range getVisibleRange() {
    return presenter.getVisibleRange();
  }

  public boolean isRowCountExact() {
    return presenter.isRowCountExact();
  }

  /**
   * Handle browser events. Subclasses should override
   * {@link #onBrowserEvent2(Event)} if they want to extend browser event
   * handling.
   *
   * @see #onBrowserEvent2(Event)
   */
  @Override
  public final void onBrowserEvent(Event event) {
    CellBasedWidgetImpl.get().onBrowserEvent(this, event);

    // Ignore spurious events (such as onblur) while we refresh the table.
    if (isRefreshing) {
      return;
    }

    // Verify that the target is still a child of this widget. IE fires focus
    // events even after the element has been removed from the DOM.
    EventTarget eventTarget = event.getEventTarget();
    if (!Element.is(eventTarget)
        || !getElement().isOrHasChild(Element.as(eventTarget))) {
      return;
    }
    super.onBrowserEvent(event);

    String eventType = event.getType();
    if ("focus".equals(eventType)) {
      // Remember the focus state.
      isFocused = true;
      onFocus();
    } else if ("blur".equals(eventType)) {
      // Remember the blur state.
      isFocused = false;
      onBlur();
    } else if ("keydown".equals(eventType) && !isKeyboardNavigationSuppressed()) {
      // A key event indicates that we have focus.
      isFocused = true;

      // Handle keyboard navigation. Prevent default on navigation events to
      // prevent default scrollbar behavior.
      int keyCode = event.getKeyCode();
      switch (keyCode) {
        case KeyCodes.KEY_DOWN:
          presenter.keyboardNext();
          event.preventDefault();
          return;
        case KeyCodes.KEY_UP:
          presenter.keyboardPrev();
          event.preventDefault();
          return;
        case KeyCodes.KEY_PAGEDOWN:
          presenter.keyboardNextPage();
          event.preventDefault();
          return;
        case KeyCodes.KEY_PAGEUP:
          presenter.keyboardPrevPage();
          event.preventDefault();
          return;
        case KeyCodes.KEY_HOME:
          presenter.keyboardHome();
          event.preventDefault();
          return;
        case KeyCodes.KEY_END:
          presenter.keyboardEnd();
          event.preventDefault();
          return;
        case 32:
          // Prevent the list box from scrolling.
          event.preventDefault();
          return;
      }
    }

    // Let subclasses handle the event now.
    onBrowserEvent2(event);
  }

  /**
   * Redraw the widget using the existing data.
   */
  public void redraw() {
    presenter.redraw();
  }

  /**
   * {@inheritDoc}
   *
   * @see #getAccessKey()
   */
  public void setAccessKey(char key) {
    this.accessKey = key;
    setKeyboardSelected(getKeyboardSelectedRow(), true, false);
  }

  public void setFocus(boolean focused) {
    Element elem = getKeyboardSelectedElement();
    if (elem != null) {
      if (focused) {
        elem.focus();
      } else {
        elem.blur();
      }
    }
  }

  public void setKeyboardPagingPolicy(KeyboardPagingPolicy policy) {
    presenter.setKeyboardPagingPolicy(policy);
  }

  public void setKeyboardSelectionPolicy(KeyboardSelectionPolicy policy) {
    presenter.setKeyboardSelectionPolicy(policy);
  }

  /**
   * Set the number of rows per page and refresh the view.
   *
   * @param pageSize the page size
   * @see #setVisibleRange(Range)
   * @see #getPageSize()
   */
  public final void setPageSize(int pageSize) {
    setVisibleRange(getPageStart(), pageSize);
  }

  /**
   * Set the starting index of the current visible page. The actual page start
   * will be clamped in the range [0, getSize() - 1].
   *
   * @param pageStart the index of the row that should appear at the start of
   *          the page
   * @see #setVisibleRange(Range)
   * @see #getPageStart()
   */
  public final void setPageStart(int pageStart) {
    setVisibleRange(pageStart, getPageSize());
  }

  public final void setRowCount(int count) {
    setRowCount(count, true);
  }

  public void setRowCount(int size, boolean isExact) {
    presenter.setRowCount(size, isExact);
  }

  /**
   * <p>
   * Set the complete list of values to display on one page.
   * </p>
   * <p>
   * Equivalent to calling {@link #setRowCount(int)} with the length of the list
   * of values, {@link #setVisibleRange(Range)} from 0 to the size of the list
   * of values, and {@link #setRowData(int, List)} with a start of 0 and the
   * specified list of values.
   * </p>
   * 
   * @param values
   */
  public final void setRowData(List<? extends T> values) {
    setRowCount(values.size());
    setVisibleRange(0, values.size());
    setRowData(0, values);
  }

  public void setRowData(int start, List<? extends T> values) {
    presenter.setRowData(start, values);
  }

  public void setSelectionModel(SelectionModel<? super T> selectionModel) {
    presenter.setSelectionModel(selectionModel);
  }

  /**
   * Set the {@link SelectionModel} that defines which items are selected and
   * the {@link com.google.gwt.view.client.CellPreviewEvent.Handler} that
   * controls how user selection is handled.
   * 
   * @param selectionModel the {@link SelectionModel} that defines selection
   * @param selectionEventManager the handler that controls user selection
   */
  public void setSelectionModel(SelectionModel<? super T> selectionModel,
      CellPreviewEvent.Handler<T> selectionEventManager) {
    // Remove the old manager.
    if (this.selectionManagerReg != null) {
      this.selectionManagerReg.removeHandler();
      this.selectionManagerReg = null;
    }

    // Add the new manager.
    if (selectionEventManager != null) {
      this.selectionManagerReg = addCellPreviewHandler(selectionEventManager);
    }

    // Set the selection model.
    setSelectionModel(selectionModel);
  }

  public void setTabIndex(int index) {
    this.tabIndex = index;
    setKeyboardSelected(getKeyboardSelectedRow(), true, false);
  }

  public final void setVisibleRange(int start, int length) {
    setVisibleRange(new Range(start, length));
  }

  public void setVisibleRange(Range range) {
    presenter.setVisibleRange(range);
  }

  public void setVisibleRangeAndClearData(Range range,
      boolean forceRangeChangeEvent) {
    presenter.setVisibleRangeAndClearData(range, forceRangeChangeEvent);
  }

  /**
   * Check if a cell consumes the specified event type.
   *
   * @param cell the cell
   * @param eventType the event type to check
   * @return true if consumed, false if not
   */
  protected boolean cellConsumesEventType(Cell<?> cell, String eventType) {
    Set<String> consumedEvents = cell.getConsumedEvents();
    return consumedEvents != null && consumedEvents.contains(eventType);
  }

  /**
   * Check that the row is within the correct bounds.
   *
   * @param row row index to check
   * @throws IndexOutOfBoundsException
   */
  protected void checkRowBounds(int row) {
    if (!isRowWithinBounds(row)) {
      throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: "
          + getRowCount());
    }
  }

  /**
   * Convert the specified HTML into DOM elements and return the parent of the
   * DOM elements.
   *
   * @param html the HTML to convert
   * @return the parent element
   */
  protected Element convertToElements(SafeHtml html) {
    return convertToElements(this, getTmpElem(), html);
  }

  /**
   * Check whether or not the cells in the view depend on the selection state.
   *
   * @return true if cells depend on selection, false if not
   */
  protected abstract boolean dependsOnSelection();

  /**
   * Return the element that holds the rendered cells.
   * 
   * @return the container {@link Element}
   */
  protected abstract Element getChildContainer();

  /**
   * Get the element that has keyboard selection.
   *
   * @return the keyboard selected element
   */
  protected abstract Element getKeyboardSelectedElement();

  /**
   * Get the row index of the keyboard selected row.
   *
   * @return the row index
   */
  protected int getKeyboardSelectedRow() {
    return presenter.getKeyboardSelectedRow();
  }

  /**
   * Get the key for the specified value.
   *
   * @param value the value
   * @return the key
   */
  protected Object getValueKey(T value) {
    ProvidesKey<T> keyProvider = getKeyProvider();
    return (keyProvider == null || value == null) ? value
        : keyProvider.getKey(value);
  }

  /**
   * Check if keyboard navigation is being suppressed, such as when the user is
   * editing a cell.
   *
   * @return true if suppressed, false if not
   */
  protected abstract boolean isKeyboardNavigationSuppressed();

  /**
   * Checks that the row is within bounds of the view.
   *
   * @param row row index to check
   * @return true if within bounds, false if not
   */
  protected boolean isRowWithinBounds(int row) {
    return row >= 0 && row < presenter.getVisibleItemCount();
  }

  /**
   * Called when the widget is blurred.
   */
  protected void onBlur() {
  }

  /**
   * Called after {@link #onBrowserEvent(Event)} completes.
   *
   * @param event the event that was fired
   */
  protected void onBrowserEvent2(Event event) {
  }

  /**
   * Called when the widget is focused.
   */
  protected void onFocus() {
  }

  @Override
  protected void onUnload() {
    isFocused = false;
    super.onUnload();
  }

  /**
   * Called when selection changes.
   * 
   * @deprecated this method is never called by AbstractHasData, render the
   *             selected styles in
   *             {@link #renderRowValues(SafeHtmlBuilder, List, int, SelectionModel)}
   */
  @Deprecated
  protected void onUpdateSelection() {
  }

  /**
   * Render all row values into the specified {@link SafeHtmlBuilder}.
   *
   * @param sb the {@link SafeHtmlBuilder} to render into
   * @param values the row values
   * @param start the absolute start index of the values
   * @param selectionModel the {@link SelectionModel}
   */
  protected abstract void renderRowValues(SafeHtmlBuilder sb, List<T> values,
      int start, SelectionModel<? super T> selectionModel);

  /**
   * Replace all children with the specified html.
   *
   * @param values the values of the new children
   * @param html the html to render in the child
   */
  protected void replaceAllChildren(List<T> values, SafeHtml html) {
    replaceAllChildren(this, getChildContainer(), html);
  }

  /**
   * Convert the specified HTML into DOM elements and replace the existing
   * elements starting at the specified index. If the number of children
   * specified exceeds the existing number of children, the remaining children
   * should be appended.
   *
   * @param values the values of the new children
   * @param start the start index to be replaced, relative to the page start
   * @param html the HTML to convert
   */
  protected void replaceChildren(List<T> values, int start, SafeHtml html) {
    Element newChildren = convertToElements(html);
    replaceChildren(this, getChildContainer(), newChildren, start, html);
  }

  /**
   * Reset focus on the currently focused cell.
   *
   * @return true if focus is taken, false if not
   */
  protected abstract boolean resetFocusOnCell();

  /**
   * Make an element focusable or not.
   *
   * @param elem the element
   * @param focusable true to make focusable, false to make unfocusable
   */
  protected void setFocusable(Element elem, boolean focusable) {
    if (focusable) {
      FocusImpl focusImpl = FocusImpl.getFocusImplForWidget();
      com.google.gwt.user.client.Element rowElem = elem.cast();
      focusImpl.setTabIndex(rowElem, getTabIndex());
      if (accessKey != 0) {
        focusImpl.setAccessKey(rowElem, accessKey);
      }
    } else {
      // Chrome: Elements remain focusable after removing the tabIndex, so set
      // it to -1 first.
      elem.setTabIndex(-1);
      elem.removeAttribute("tabIndex");
      elem.removeAttribute("accessKey");
    }
  }

  /**
   * Update an element to reflect its keyboard selected state.
   *
   * @param index the index of the element
   * @param selected true if selected, false if not
   * @param stealFocus true if the row should steal focus, false if not
   */
  protected abstract void setKeyboardSelected(int index, boolean selected,
      boolean stealFocus);

  /**
   * Update an element to reflect its selected state.
   *
   * @param elem the element to update
   * @param selected true if selected, false if not
   * @deprecated this method is never called by AbstractHasData, render the
   *             selected styles in
   *             {@link #renderRowValues(SafeHtmlBuilder, List, int, SelectionModel)}
   */
  @Deprecated
  protected void setSelected(Element elem, boolean selected) {
    // Never called.
  }

  /**
   * Add a {@link ValueChangeHandler} that is called when the display values
   * change. Used by {@link CellBrowser} to detect when the displayed data
   * changes.
   *
   * @param handler the handler
   * @return a {@link HandlerRegistration} to remove the handler
   */
  final HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<List<T>> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  HasDataPresenter<T> getPresenter() {
    return presenter;
  }

  /**
   * Set the current loading state of the data.
   *
   * @param state the loading state
   */
  void setLoadingState(LoadingState state) {
  }
}
