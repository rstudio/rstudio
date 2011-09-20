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
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An abstract {@link Widget} that implements {@link HasData}.
 * 
 * @param <T> the data type of each row
 */
public abstract class AbstractHasData<T> extends Composite implements HasData<T>,
    HasKeyProvider<T>, Focusable, HasKeyboardPagingPolicy {

  /**
   * Default implementation of a keyboard navigation handler.
   * 
   * @param <T> the data type of each row
   */
  public static class DefaultKeyboardSelectionHandler<T> implements CellPreviewEvent.Handler<T> {

    /**
     * The number of rows to jump when PAGE_UP or PAGE_DOWN is pressed and the
     * {@link HasKeyboardPagingPolicy.KeyboardPagingPolicy} is
     * {@link HasKeyboardPagingPolicy.KeyboardPagingPolicy#INCREASE_RANGE}.
     */
    private static final int PAGE_INCREMENT = 30;

    private final AbstractHasData<T> display;

    /**
     * Construct a new keyboard selection handler for the specified view.
     * 
     * @param display the display being handled
     */
    public DefaultKeyboardSelectionHandler(AbstractHasData<T> display) {
      this.display = display;
    }

    public AbstractHasData<T> getDisplay() {
      return display;
    }

    @Override
    public void onCellPreview(CellPreviewEvent<T> event) {
      NativeEvent nativeEvent = event.getNativeEvent();
      String eventType = event.getNativeEvent().getType();
      if (BrowserEvents.KEYDOWN.equals(eventType) && !event.isCellEditing()) {
        /*
         * Handle keyboard navigation, unless the cell is being edited. If the
         * cell is being edited, we do not want to change rows.
         * 
         * Prevent default on navigation events to prevent default scrollbar
         * behavior.
         */
        switch (nativeEvent.getKeyCode()) {
          case KeyCodes.KEY_DOWN:
            nextRow();
            handledEvent(event);
            return;
          case KeyCodes.KEY_UP:
            prevRow();
            handledEvent(event);
            return;
          case KeyCodes.KEY_PAGEDOWN:
            nextPage();
            handledEvent(event);
            return;
          case KeyCodes.KEY_PAGEUP:
            prevPage();
            handledEvent(event);
            return;
          case KeyCodes.KEY_HOME:
            home();
            handledEvent(event);
            return;
          case KeyCodes.KEY_END:
            end();
            handledEvent(event);
            return;
          case 32:
            // Prevent the list box from scrolling.
            handledEvent(event);
            return;
        }
      } else if (BrowserEvents.CLICK.equals(eventType)) {
        /*
         * Move keyboard focus to the clicked row, even if the Cell is being
         * edited. Unlike key events, we aren't moving the currently selected
         * row, just updating it based on where the user clicked.
         */
        int relRow = event.getIndex() - display.getPageStart();

        // If a natively focusable element was just clicked, then do not steal
        // focus.
        boolean isFocusable = false;
        Element target = Element.as(event.getNativeEvent().getEventTarget());
        isFocusable = CellBasedWidgetImpl.get().isFocusable(target);
        display.setKeyboardSelectedRow(relRow, !isFocusable);

        // Do not cancel the event as the click may have occurred on a Cell.
      } else if (BrowserEvents.FOCUS.equals(eventType)) {
        // Move keyboard focus to match the currently focused element.
        int relRow = event.getIndex() - display.getPageStart();
        if (display.getKeyboardSelectedRow() != relRow) {
          // Do not steal focus as this was a focus event.
          display.setKeyboardSelectedRow(event.getIndex(), false);

          // Do not cancel the event as the click may have occurred on a Cell.
          return;
        }
      }
    }

    // Visible for testing.
    void end() {
      setKeyboardSelectedRow(display.getRowCount() - 1);
    }

    void handledEvent(CellPreviewEvent<?> event) {
      event.setCanceled(true);
      event.getNativeEvent().preventDefault();
    }

    // Visible for testing.
    void home() {
      setKeyboardSelectedRow(-display.getPageStart());
    }

    // Visible for testing.
    void nextPage() {
      KeyboardPagingPolicy keyboardPagingPolicy = display.getKeyboardPagingPolicy();
      if (KeyboardPagingPolicy.CHANGE_PAGE == keyboardPagingPolicy) {
        // 0th index of next page.
        setKeyboardSelectedRow(display.getPageSize());
      } else if (KeyboardPagingPolicy.INCREASE_RANGE == keyboardPagingPolicy) {
        setKeyboardSelectedRow(display.getKeyboardSelectedRow() + PAGE_INCREMENT);
      }
    }

    // Visible for testing.
    void nextRow() {
      setKeyboardSelectedRow(display.getKeyboardSelectedRow() + 1);
    }

    // Visible for testing.
    void prevPage() {
      KeyboardPagingPolicy keyboardPagingPolicy = display.getKeyboardPagingPolicy();
      if (KeyboardPagingPolicy.CHANGE_PAGE == keyboardPagingPolicy) {
        // 0th index of previous page.
        setKeyboardSelectedRow(-display.getPageSize());
      } else if (KeyboardPagingPolicy.INCREASE_RANGE == keyboardPagingPolicy) {
        setKeyboardSelectedRow(display.getKeyboardSelectedRow() - PAGE_INCREMENT);
      }
    }

    // Visible for testing.
    void prevRow() {
      setKeyboardSelectedRow(display.getKeyboardSelectedRow() - 1);
    }

    // Visible for testing.
    void setKeyboardSelectedRow(int row) {
      display.setKeyboardSelectedRow(row, true);
    }
  }

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

    @Override
    public <H extends EventHandler> HandlerRegistration addHandler(H handler, Type<H> type) {
      return hasData.addHandler(handler, type);
    }

    @Override
    public void replaceAllChildren(List<T> values, SelectionModel<? super T> selectionModel,
        boolean stealFocus) {
      SafeHtml html = renderRowValues(values, hasData.getPageStart(), selectionModel);

      // Removing elements can fire a blur event, which we ignore.
      hasData.isFocused = hasData.isFocused || stealFocus;
      wasFocused = hasData.isFocused;
      hasData.isRefreshing = true;
      hasData.replaceAllChildren(values, html);
      hasData.isRefreshing = false;

      // Ensure that the keyboard selected element is focusable.
      Element elem = hasData.getKeyboardSelectedElement();
      if (elem != null) {
        hasData.setFocusable(elem, true);
        if (hasData.isFocused) {
          hasData.onFocus();
        }
      }

      fireValueChangeEvent();
    }

    @Override
    public void replaceChildren(List<T> values, int start,
        SelectionModel<? super T> selectionModel, boolean stealFocus) {
      SafeHtml html = renderRowValues(values, hasData.getPageStart() + start, selectionModel);

      // Removing elements can fire a blur event, which we ignore.
      hasData.isFocused = hasData.isFocused || stealFocus;
      wasFocused = hasData.isFocused;
      hasData.isRefreshing = true;
      hasData.replaceChildren(values, start, html);
      hasData.isRefreshing = false;

      // Ensure that the keyboard selected element is focusable.
      Element elem = hasData.getKeyboardSelectedElement();
      if (elem != null) {
        hasData.setFocusable(elem, true);
        if (hasData.isFocused) {
          hasData.onFocus();
        }
      }

      fireValueChangeEvent();
    }

    @Override
    public void resetFocus() {
      if (wasFocused) {
        CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
          @Override
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

    @Override
    public void setKeyboardSelected(int index, boolean seleted, boolean stealFocus) {
      hasData.isFocused = hasData.isFocused || stealFocus;
      hasData.setKeyboardSelected(index, seleted, stealFocus);
    }

    @Override
    public void setLoadingState(LoadingState state) {
      hasData.isRefreshing = true;
      hasData.onLoadingStateChanged(state);
      hasData.isRefreshing = false;
    }

    /**
     * Fire a value change event.
     */
    private void fireValueChangeEvent() {
      // Use an anonymous class to override ValueChangeEvents's protected
      // constructor. We can't call ValueChangeEvent.fire() because this class
      // doesn't implement HasValueChangeHandlers.
      hasData.fireEvent(new ValueChangeEvent<List<T>>(hasData.getVisibleItems()) {
      });
    }

    /**
     * Render a list of row values.
     * 
     * @param values the row values
     * @param start the absolute start index of the values
     * @param selectionModel the {@link SelectionModel}
     * @return null, unless the implementation renders using SafeHtml
     */
    private SafeHtml renderRowValues(List<T> values, int start,
        SelectionModel<? super T> selectionModel) {
      try {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        hasData.renderRowValues(sb, values, start, selectionModel);
        return sb.toSafeHtml();
      } catch (UnsupportedOperationException e) {
        // If renderRowValues throws, the implementation will render directly in
        // the replaceChildren method.
        return null;
      }
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
  static Element convertToElements(Widget widget, com.google.gwt.user.client.Element tmpElem,
      SafeHtml html) {
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
  static void replaceAllChildren(Widget widget, Element childContainer, SafeHtml html) {
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
  static void replaceChildren(Widget widget, Element childContainer, Element newChildren,
      int start, SafeHtml html) {
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
  private HandlerRegistration keyboardSelectionReg;
  private HandlerRegistration selectionManagerReg;
  private int tabIndex;

  /**
   * Constructs an {@link AbstractHasData} with the given page size.
   * 
   * @param elem the parent {@link Element}
   * @param pageSize the page size
   * @param keyProvider the key provider, or null
   */
  public AbstractHasData(final Element elem, final int pageSize, final ProvidesKey<T> keyProvider) {
    this(new Widget() {
      {
        setElement(elem);
      }
    }, pageSize, keyProvider);
  }

  /**
   * Constructs an {@link AbstractHasData} with the given page size.
   * 
   * @param widget the parent {@link Widget}
   * @param pageSize the page size
   * @param keyProvider the key provider, or null
   */
  public AbstractHasData(Widget widget, final int pageSize, final ProvidesKey<T> keyProvider) {
    initWidget(widget);
    this.presenter = new HasDataPresenter<T>(this, new View<T>(this), pageSize, keyProvider);

    // Sink events.
    Set<String> eventTypes = new HashSet<String>();
    eventTypes.add(BrowserEvents.FOCUS);
    eventTypes.add(BrowserEvents.BLUR);
    eventTypes.add(BrowserEvents.KEYDOWN); // Used for keyboard navigation.
    eventTypes.add(BrowserEvents.KEYUP); // Used by subclasses for selection.
    eventTypes.add(BrowserEvents.CLICK); // Used by subclasses for selection.
    eventTypes.add(BrowserEvents.MOUSEDOWN); // No longer used, but here for legacy support.
    CellBasedWidgetImpl.get().sinkEvents(this, eventTypes);

    // Add a default selection event manager.
    selectionManagerReg =
        addCellPreviewHandler(DefaultSelectionEventManager.<T> createDefaultManager());

    // Add a default keyboard selection handler.
    setKeyboardSelectionHandler(new DefaultKeyboardSelectionHandler<T>(this));
  }

  @Override
  public HandlerRegistration addCellPreviewHandler(CellPreviewEvent.Handler<T> handler) {
    return presenter.addCellPreviewHandler(handler);
  }

  /**
   * Add a {@link LoadingStateChangeEvent.Handler} to be notified of changes in
   * the loading state.
   * 
   * @param handler the handle
   * @return the registration for the handler
   */
  public HandlerRegistration addLoadingStateChangeHandler(LoadingStateChangeEvent.Handler handler) {
    return presenter.addLoadingStateChangeHandler(handler);
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

  @Override
  public KeyboardPagingPolicy getKeyboardPagingPolicy() {
    return presenter.getKeyboardPagingPolicy();
  }

  /**
   * Get the index of the row that is currently selected via the keyboard,
   * relative to the page start index.
   * 
   * <p>
   * This is not same as the selected row in the {@link SelectionModel}. The
   * keyboard selected row refers to the row that the user navigated to via the
   * keyboard or mouse.
   * </p>
   * 
   * @return the currently selected row, or -1 if none selected
   */
  public int getKeyboardSelectedRow() {
    return presenter.getKeyboardSelectedRow();
  }

  @Override
  public KeyboardSelectionPolicy getKeyboardSelectionPolicy() {
    return presenter.getKeyboardSelectionPolicy();
  }

  @Override
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

  @Override
  public int getRowCount() {
    return presenter.getRowCount();
  }

  @Override
  public SelectionModel<? super T> getSelectionModel() {
    return presenter.getSelectionModel();
  }

  @Override
  public int getTabIndex() {
    return tabIndex;
  }

  /**
   * Get the key for the specified value. If a keyProvider is not specified or the value is null,
   * the value is returned. If the key provider is specified, it is used to get the key from
   * the value.
   * 
   * @param value the value
   * @return the key
   */
  public Object getValueKey(T value) {
    ProvidesKey<T> keyProvider = getKeyProvider();
    return (keyProvider == null || value == null) ? value : keyProvider.getKey(value);
  }
  
  @Override
  public T getVisibleItem(int indexOnPage) {
    checkRowBounds(indexOnPage);
    return presenter.getVisibleItem(indexOnPage);
  }

  @Override
  public int getVisibleItemCount() {
    return presenter.getVisibleItemCount();
  }

  /**
   * Return the row values that the widget is currently displaying as an
   * immutable list.
   * 
   * @return a List of displayed items
   */
  @Override
  public List<T> getVisibleItems() {
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
    if (!Element.is(eventTarget)) {
      return;
    }
    Element target = Element.as(eventTarget);
    if (!getElement().isOrHasChild(Element.as(eventTarget))) {
      return;
    }
    super.onBrowserEvent(event);

    String eventType = event.getType();
    if (BrowserEvents.FOCUS.equals(eventType)) {
      // Remember the focus state.
      isFocused = true;
      onFocus();
    } else if (BrowserEvents.BLUR.equals(eventType)) {
      // Remember the blur state.
      isFocused = false;
      onBlur();
    } else if (BrowserEvents.KEYDOWN.equals(eventType)) {
      // A key event indicates that we already have focus.
      isFocused = true;
    } else if (BrowserEvents.MOUSEDOWN.equals(eventType)
        && CellBasedWidgetImpl.get().isFocusable(Element.as(target))) {
      // If a natively focusable element was just clicked, then we must have
      // focus.
      isFocused = true;
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
   * Redraw a single row using the existing data.
   * 
   * @param absRowIndex the absolute row index to redraw
   */
  public void redrawRow(int absRowIndex) {
    int relRowIndex = absRowIndex - getPageStart();
    checkRowBounds(relRowIndex);
    setRowData(absRowIndex, Collections.singletonList(getVisibleItem(relRowIndex)));
  }

  /**
   * {@inheritDoc}
   * 
   * @see #getAccessKey()
   */
  @Override
  public void setAccessKey(char key) {
    this.accessKey = key;
    setKeyboardSelected(getKeyboardSelectedRow(), true, false);
  }

  @Override
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

  @Override
  public void setKeyboardPagingPolicy(KeyboardPagingPolicy policy) {
    presenter.setKeyboardPagingPolicy(policy);
  }

  /**
   * Set the keyboard selected row. The row index is the index relative to the
   * current page start index.
   * 
   * <p>
   * If keyboard selection is disabled, this method does nothing.
   * </p>
   * 
   * <p>
   * If the keyboard selected row is outside of the range of the current page
   * (that is, less than 0 or greater than or equal to the page size), the page
   * or range will be adjusted depending on the keyboard paging policy. If the
   * keyboard paging policy is limited to the current range, the row index will
   * be clipped to the current page.
   * </p>
   * 
   * @param row the row index relative to the page start
   */
  public final void setKeyboardSelectedRow(int row) {
    setKeyboardSelectedRow(row, true);
  }

  /**
   * Set the keyboard selected row and optionally focus on the new row.
   * 
   * @param row the row index relative to the page start
   * @param stealFocus true to focus on the new row
   * @see #setKeyboardSelectedRow(int)
   */
  public void setKeyboardSelectedRow(int row, boolean stealFocus) {
    presenter.setKeyboardSelectedRow(row, stealFocus, true);
  }

  /**
   * Set the handler that handles keyboard selection/navigation.
   */
  public void setKeyboardSelectionHandler(CellPreviewEvent.Handler<T> keyboardSelectionReg) {
    // Remove the old manager.
    if (this.keyboardSelectionReg != null) {
      this.keyboardSelectionReg.removeHandler();
      this.keyboardSelectionReg = null;
    }

    // Add the new manager.
    if (keyboardSelectionReg != null) {
      this.keyboardSelectionReg = addCellPreviewHandler(keyboardSelectionReg);
    }
  }

  @Override
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

  @Override
  public final void setRowCount(int count) {
    setRowCount(count, true);
  }

  @Override
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

  @Override
  public void setRowData(int start, List<? extends T> values) {
    presenter.setRowData(start, values);
  }

  /**
   * Set the {@link SelectionModel} used by this {@link HasData}.
   * 
   * <p>
   * By default, selection occurs when the user clicks on a Cell or presses the
   * spacebar. If you need finer control over selection, you can specify a
   * {@link DefaultSelectionEventManager} using
   * {@link #setSelectionModel(SelectionModel, com.google.gwt.view.client.CellPreviewEvent.Handler)}. {@link DefaultSelectionEventManager} provides some default
   * implementations to handle checkbox based selection, as well as a blacklist
   * or whitelist of columns to prevent or allow selection.
   * </p>
   * 
   * @param selectionModel the {@link SelectionModel}
   * @see #setSelectionModel(SelectionModel,
   *      com.google.gwt.view.client.CellPreviewEvent.Handler)
   * @see #getSelectionModel()
   */
  @Override
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

  @Override
  public void setTabIndex(int index) {
    this.tabIndex = index;
    setKeyboardSelected(getKeyboardSelectedRow(), true, false);
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
      throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: " + getRowCount());
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
   * Get the element that represents the specified index.
   * 
   * @param index the index of the row value
   * @return the the child element, or null if it does not exist
   */
  protected Element getChildElement(int index) {
    Element childContainer = getChildContainer();
    int childCount = childContainer.getChildCount();
    return (index < childCount) ? childContainer.getChild(index).<Element> cast() : null;
  }

  /**
   * Get the element that has keyboard selection.
   * 
   * @return the keyboard selected element
   */
  protected abstract Element getKeyboardSelectedElement();

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

  /**
   * Called when the loading state changes. By default, this implementation
   * fires a {@link LoadingStateChangeEvent}.
   * 
   * @param state the new loading state
   */
  protected void onLoadingStateChanged(LoadingState state) {
    fireEvent(new LoadingStateChangeEvent(state));
  }

  @Override
  protected void onUnload() {
    isFocused = false;
    super.onUnload();
  }

  /**
   * Render all row values into the specified {@link SafeHtmlBuilder}.
   * 
   * <p>
   * Subclasses can optionally throw an {@link UnsupportedOperationException} if
   * they prefer to render the rows in
   * {@link #replaceAllChildren(List, SafeHtml)} and
   * {@link #replaceChildren(List, int, SafeHtml)}. In this case, the
   * {@link SafeHtml} argument will be null. Though a bit hacky, this is
   * designed to supported legacy widgets that use {@link SafeHtmlBuilder}, and
   * newer widgets that use other builders, such as the ElementBuilder API.
   * </p>
   * 
   * @param sb the {@link SafeHtmlBuilder} to render into
   * @param values the row values
   * @param start the absolute start index of the values
   * @param selectionModel the {@link SelectionModel}
   * @throws UnsupportedOperationException if the values will be rendered in
   *           {@link #replaceAllChildren(List, SafeHtml)} and
   *           {@link #replaceChildren(List, int, SafeHtml)}
   */
  protected abstract void renderRowValues(SafeHtmlBuilder sb, List<T> values, int start,
      SelectionModel<? super T> selectionModel) throws UnsupportedOperationException;

  /**
   * Replace all children with the specified html.
   * 
   * @param values the values of the new children
   * @param html the html to render, or null if
   *          {@link #renderRowValues(SafeHtmlBuilder, List, int, SelectionModel)}
   *          throws an {@link UnsupportedOperationException}
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
   * @param html the html to render, or null if
   *          {@link #renderRowValues(SafeHtmlBuilder, List, int, SelectionModel)}
   *          throws an {@link UnsupportedOperationException}
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
  protected abstract void setKeyboardSelected(int index, boolean selected, boolean stealFocus);

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
  final HandlerRegistration addValueChangeHandler(ValueChangeHandler<List<T>> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  /**
   * Adopt the specified widget.
   * 
   * @param child the child to adopt
   */
  native void adopt(Widget child) /*-{
    child.@com.google.gwt.user.client.ui.Widget::setParent(Lcom/google/gwt/user/client/ui/Widget;)(this);
  }-*/;

  /**
   * Attach a child.
   * 
   * @param child the child to attach
   */
  native void doAttach(Widget child) /*-{
    child.@com.google.gwt.user.client.ui.Widget::onAttach()();
  }-*/;

  /**
   * Detach a child.
   * 
   * @param child the child to detach
   */
  native void doDetach(Widget child) /*-{
    child.@com.google.gwt.user.client.ui.Widget::onDetach()();
  }-*/;

  HasDataPresenter<T> getPresenter() {
    return presenter;
  }

  /**
   * Show or hide an element.
   * 
   * @param element the element
   * @param show true to show, false to hide
   */
  void showOrHide(Element element, boolean show) {
    if (element == null) {
      return;
    }
    if (show) {
      element.getStyle().clearDisplay();
    } else {
      element.getStyle().setDisplay(Display.NONE);
    }
  }
}
