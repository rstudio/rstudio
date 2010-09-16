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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.HasDataPresenter.ElementIterator;
import com.google.gwt.user.cellview.client.HasDataPresenter.LoadingState;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.HasKeyProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract {@link Widget} that implements {@link HasData}.
 *
 * @param <T> the data type of each row
 */
public abstract class AbstractHasData<T> extends Widget
    implements HasData<T>, HasKeyProvider<T> {

  /**
   * Implementation of {@link HasDataPresenter.View} used by this widget.
   *
   * @param <T> the data type of the view
   */
  private static class View<T> implements HasDataPresenter.View<T> {

    private final AbstractHasData<T> hasData;

    public View(AbstractHasData<T> hasData) {
      this.hasData = hasData;
    }

    public <H extends EventHandler> HandlerRegistration addHandler(
        H handler, Type<H> type) {
      return hasData.addHandler(handler, type);
    }

    public boolean dependsOnSelection() {
      return hasData.dependsOnSelection();
    }

    public int getChildCount() {
      return hasData.getChildCount();
    }

    public ElementIterator getChildIterator() {
      return new HasDataPresenter.DefaultElementIterator(
          this, hasData.getChildContainer().getFirstChildElement());
    }

    public void onUpdateSelection() {
      hasData.onUpdateSelection();
    }

    public void render(SafeHtmlBuilder sb, List<T> values, int start,
        SelectionModel<? super T> selectionModel) {
      hasData.renderRowValues(sb, values, start, selectionModel);
    }

    public void replaceAllChildren(List<T> values, SafeHtml html) {
      hasData.replaceAllChildren(values, html);
      fireValueChangeEvent();
    }

    public void replaceChildren(List<T> values, int start, SafeHtml html) {
      hasData.replaceChildren(values, start, html);
      fireValueChangeEvent();
    }

    public void resetFocus() {
      hasData.resetFocus();
    }

    public void setLoadingState(LoadingState state) {
      hasData.setLoadingState(state);
    }

    /**
     * Update an element to reflect its selected state.
     *
     * @param elem the element to update
     * @param selected true if selected, false if not
     */
    public void setSelected(Element elem, boolean selected) {
      hasData.setSelected(elem, selected);
    }

    /**
     * Fire a value change event.
     */
    private void fireValueChangeEvent() {
      // Use an anonymous class to override ValueChangeEvents's protected
      // constructor. We can't call ValueChangeEvent.fire() because this class
      // doesn't implement HasValueChangeHandlers.
      hasData.fireEvent(
          new ValueChangeEvent<List<T>>(hasData.getDisplayedItems()) {
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
  static Element convertToElements(
      Widget widget, com.google.gwt.user.client.Element tmpElem, SafeHtml html) {
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
  static void replaceAllChildren(
      Widget widget, Element childContainer, SafeHtml html) {
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
   * @return the temporary element used to create elements
   */
  private static com.google.gwt.user.client.Element getTmpElem() {
    if (tmpElem == null) {
      tmpElem = Document.get().createDivElement().cast();
    }
    return tmpElem;
  }

  /**
   * The presenter.
   */
  private final HasDataPresenter<T> presenter;

  /**
   * If null, each T will be used as its own key.
   */
  private ProvidesKey<T> providesKey;

  /**
   * Constructs an {@link AbstractHasData} with the given page size.
   *
   * @param pageSize the page size
   */
  public AbstractHasData(Element elem, final int pageSize, final ProvidesKey<T> keyProvider) {
    setElement(elem);
    this.presenter = new HasDataPresenter<T>(this, new View<T>(this), pageSize);
    this.providesKey = keyProvider;
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
   * Get the row value at the specified visible index. Index 0 corresponds to
   * the first item on the page.
   *
   * @param indexOnPage the index on the page
   * @return the row vaule
   */
  public T getDisplayedItem(int indexOnPage) {
    checkRowBounds(indexOnPage);
    return presenter.getRowData().get(indexOnPage);
  }

  /**
   * Get the row values that the widget is currently displaying.
   */
  public List<T> getDisplayedItems() {
    return new ArrayList<T>(presenter.getRowData());
  }

  public ProvidesKey<T> getKeyProvider() {
    return providesKey;
  }

  /**
   * @return the range size
   * @see #getVisibleRange()
   */
  public final int getPageSize() {
    return getVisibleRange().getLength();
  }

  /**
   * @return the range start
   * @see #getVisibleRange()
   */
  public final int getPageStart() {
    return getVisibleRange().getStart();
  }

  public int getRowCount() {
    return presenter.getRowCount();
  }

  public SelectionModel<? super T> getSelectionModel() {
    return presenter.getSelectionModel();
  }

  public Range getVisibleRange() {
    return presenter.getVisibleRange();
  }

  public boolean isRowCountExact() {
    return presenter.isRowCountExact();
  }

  /**
   * Redraw the widget using the existing data.
   */
  public void redraw() {
    presenter.redraw();
  }

  /**
   * Set the number of rows per page and refresh the view.
   *
   * @param pageSize the page size
   * @see #setVisibleRange(Range)
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

  public void setRowData(int start, List<T> values) {
    presenter.setRowData(start, values);
  }

  public void setSelectionModel(SelectionModel<? super T> selectionModel) {
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
   * Checks that the row is within the correct bounds.
   *
   * @param row row index to check
   * @throws IndexOutOfBoundsException
   */
  protected void checkRowBounds(int row) {
    int rowCount = getChildCount();
    if ((row >= rowCount) || (row < 0)) {
      throw new IndexOutOfBoundsException(
          "Row index: " + row + ", Row size: " + rowCount);
    }
  }

  /**
   * Render all row values into the specified {@link SafeHtmlBuilder}.
   *
   * @param sb the {@link SafeHtmlBuilder} to render into
   * @param values the row values
   * @param start the start index of the values
   * @param selectionModel the {@link SelectionModel}
   */
  protected abstract void renderRowValues(SafeHtmlBuilder sb, List<T> values,
      int start, SelectionModel<? super T> selectionModel);

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

  /**
   * Convert the specified HTML into DOM elements and return the parent of the
   * DOM elements.
   *
   * @param html the HTML to convert
   * @return the parent element
   */
  // TODO(jlabanca): Which of the following methods should we expose.
  Element convertToElements(SafeHtml html) {
    return convertToElements(this, getTmpElem(), html);
  }

  /**
   * Check whether or not the cells in the view depend on the selection state.
   *
   * @return true if cells depend on selection, false if not
   */
  abstract boolean dependsOnSelection();

  /**
   * @return the element that holds the rendered cells
   */
  abstract Element getChildContainer();

  /**
   * @return the number of child elements
   */
  int getChildCount() {
    return getChildContainer().getChildCount();
  }

  /**
   * Get the first index of a displayed item according to its key.
   *
   * @param value the value
   * @return the index of the item, or -1 of not found
   */
  int indexOf(T value) {
    ProvidesKey<T> keyProvider = getKeyProvider();
    List<T> items = getDisplayedItems();

    // If no key provider is present, just compare the objets.
    if (keyProvider == null) {
      return items.indexOf(value);
    }

    // Compare the keys of each object.
    Object key = keyProvider.getKey(value);
    if (key == null) {
      return -1;
    }
    int itemCount = items.size();
    for (int i = 0; i < itemCount; i++) {
      if (key.equals(keyProvider.getKey(items.get(i)))) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Called when selection changes.
   */
  void onUpdateSelection() {
  }

  /**
   * Replace all children with the specified html.
   *
   * @param values the values of the new children
   * @param html the html to render in the child
   */
  void replaceAllChildren(List<T> values, SafeHtml html) {
    replaceAllChildren(this, getChildContainer(), html);
  }

  /**
   * Convert the specified HTML into DOM elements and replace the existing
   * elements starting at the specified index. If the number of children
   * specified exceeds the existing number of children, the remaining children
   * should be appended.
   *
   * @param values the values of the new children
   * @param start the start index to be replaced
   * @param html the HTML to convert
   */
  void replaceChildren(List<T> values, int start, SafeHtml html) {
    Element newChildren = convertToElements(html);
    replaceChildren(this, getChildContainer(), newChildren, start, html);
  }

  /**
   * Re-establish focus on an element within the view if desired.
   */
  void resetFocus() {
  }

  /**
   * Set the current loading state of the data.
   *
   * @param state the loading state
   */
  abstract void setLoadingState(LoadingState state);

  /**
   * Update an element to reflect its selected state.
   *
   * @param elem the element to update
   * @param selected true if selected, false if not
   */
  abstract void setSelected(Element elem, boolean selected);
}
