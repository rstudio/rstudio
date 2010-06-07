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
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A single column list of cells.
 * 
 * @param <T> the data type of list items
 */
public class CellList<T> extends Widget implements PagingListView<T> {

  /**
   * A ClientBundle that provides images for this widget.
   */
  public static interface Resources extends ClientBundle {

    /**
     * The background used for selected items.
     */
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource cellListSelectedBackground();

    /**
     * The styles used in this widget.
     */
    @Source("CellList.css")
    Style cellListStyle();
  }

  /**
   * Styles used by this widget.
   */
  public static interface Style extends CssResource {

    /**
     * Applied to even items.
     */
    String evenItem();

    /**
     * Applied to odd items.
     */
    String oddItem();

    /**
     * Applied to selected items.
     */
    String selectedItem();
  }

  /**
   * The default page size.
   */
  private static final int DEFAULT_PAGE_SIZE = 25;

  private static Resources DEFAULT_RESOURCES;

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  private final Cell<T> cell;
  private final Element childContainer;
  private String emptyListMessage = "";
  private final Element emptyMessageElem;
  private final CellListImpl<T> impl;
  private final Style style;
  private ValueUpdater<T> valueUpdater;

  /**
   * Construct a new {@link CellList}.
   * 
   * @param cell the cell used to render each item
   */
  public CellList(final Cell<T> cell) {
    this(cell, getDefaultResources());
  }

  /**
   * Construct a new {@link CellList} with the specified {@link Resources}.
   * 
   * @param cell the cell used to render each item
   * @param resources the resources used for this widget
   */
  // TODO(jlabanca): Should cell support ViewData?
  public CellList(final Cell<T> cell, Resources resources) {
    this.cell = cell;
    this.style = resources.cellListStyle();
    this.style.ensureInjected();

    // Create the DOM hierarchy.
    childContainer = Document.get().createDivElement();

    emptyMessageElem = Document.get().createDivElement();
    showOrHide(emptyMessageElem, false);

    // TODO: find some way for cells to communicate what they're interested in.
    DivElement outerDiv = Document.get().createDivElement();
    outerDiv.appendChild(childContainer);
    outerDiv.appendChild(emptyMessageElem);
    setElement(outerDiv);
    sinkEvents(Event.ONCLICK | Event.ONCHANGE | Event.MOUSEEVENTS);

    // Create the implementation.
    impl = new CellListImpl<T>(this, DEFAULT_PAGE_SIZE, childContainer) {

      @Override
      protected boolean dependsOnSelection() {
        return cell.dependsOnSelection();
      }

      @Override
      protected void emitHtml(StringBuilder sb, List<T> values, int start,
          SelectionModel<? super T> selectionModel) {
        int length = values.size();
        int end = start + length;
        for (int i = start; i < end; i++) {
          T value = values.get(i - start);
          boolean isSelected = selectionModel == null ? false
              : selectionModel.isSelected(value);
          sb.append("<div onclick='' __idx='").append(i).append("'");
          sb.append(" class='");
          sb.append(i % 2 == 0 ? style.evenItem() : style.oddItem());
          if (isSelected) {
            sb.append(" ").append(style.selectedItem());
          }
          sb.append("'>");
          cell.render(value, null, sb);
          sb.append("</div>");
        }
      }

      @Override
      protected void onSizeChanged() {
        super.onSizeChanged();
        showOrHide(emptyMessageElem, impl.getDataSize() == 0);
      }

      @Override
      protected void setSelected(Element elem, boolean selected) {
        setStyleName(elem, style.selectedItem(), selected);
      }
    };
  }

  public int getDataSize() {
    return impl.getDataSize();
  }

  /**
   * Get the value of a displayed item.
   * 
   * @param indexOnPage the index on the page
   * @return the value
   */
  public T getDisplayedItem(int indexOnPage) {
    checkRowBounds(indexOnPage);
    return impl.getData().get(indexOnPage);
  }

  public List<T> getDisplayedItems() {
    return new ArrayList<T>(impl.getData());
  }

  /**
   * Get the message that is displayed when there is no data.
   * 
   * @return the empty message
   */
  public String getEmptyListMessage() {
    return emptyListMessage;
  }

  public int getPageSize() {
    return impl.getPageSize();
  }

  public int getPageStart() {
    return impl.getPageStart();
  }

  public Range getRange() {
    return impl.getRange();
  }

  /**
   * Get the {@link Element} for the specified index. If the element has not
   * been created, null is returned.
   * 
   * @param indexOnPage the index on the page
   * @return the element, or null if it doesn't exists
   * @throws IndexOutOfBoundsException if the index is outside of the current
   *           page
   */
  public Element getRowElement(int indexOnPage) {
    checkRowBounds(indexOnPage);
    if (childContainer.getChildCount() > indexOnPage) {
      return childContainer.getChild(indexOnPage).cast();
    }
    return null;
  }

  public boolean isDataSizeExact() {
    return impl.dataSizeIsExact();
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    // Forward the event to the cell.
    Element target = event.getEventTarget().cast();
    String idxString = "";
    while ((target != null)
        && ((idxString = target.getAttribute("__idx")).length() == 0)) {
      target = target.getParentElement();
    }
    if (idxString.length() > 0) {
      int idx = Integer.parseInt(idxString);
      T value = impl.getData().get(idx - impl.getPageStart());
      cell.onBrowserEvent(target, value, null, event, valueUpdater);
      if (event.getTypeInt() == Event.ONCLICK && !cell.consumesEvents()) {
        SelectionModel<? super T> selectionModel = impl.getSelectionModel();
        if (selectionModel != null) {
          selectionModel.setSelected(value, true);
        }
      }
    }
  }

  /**
   * Redraw the list using the existing data.
   */
  public void redraw() {
    impl.redraw();
  }

  /**
   * Redraw the list, requesting data from the delegate.
   */
  public void refresh() {
    impl.refresh();
  }

  public void setData(int start, int length, List<T> values) {
    impl.setData(values, start);
  }

  public void setDataSize(int size, boolean isExact) {
    impl.setDataSize(size, isExact);
  }

  public void setDelegate(Delegate<T> delegate) {
    impl.setDelegate(delegate);
  }

  /**
   * Set the message to display when there is no data.
   * 
   * @param html the message to display when there are no results
   */
  public void setEmptyListMessage(String html) {
    this.emptyListMessage = html;
    emptyMessageElem.setInnerHTML(html);
  }

  public void setPager(Pager<T> pager) {
    impl.setPager(pager);
  }

  public void setPageSize(int pageSize) {
    impl.setPageSize(pageSize);
  }

  public void setPageStart(int pageStart) {
    impl.setPageStart(pageStart);
  }

  public void setSelectionModel(final SelectionModel<? super T> selectionModel) {
    impl.setSelectionModel(selectionModel, true);
  }

  /**
   * Set the value updater to use when cells modify items.
   * 
   * @param valueUpdater the {@link ValueUpdater}
   */
  public void setValueUpdater(ValueUpdater<T> valueUpdater) {
    this.valueUpdater = valueUpdater;
  }

  /**
   * Checks that the row is within the correct bounds.
   * 
   * @param row row index to check
   * @throws IndexOutOfBoundsException
   */
  protected void checkRowBounds(int row) {
    int rowSize = impl.getDisplayedItemCount();
    if ((row >= rowSize) || (row < 0)) {
      throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: "
          + rowSize);
    }
  }

  /**
   * Show or hide an element.
   * 
   * @param element the element
   * @param show true to show, false to hide
   */
  private void showOrHide(Element element, boolean show) {
    if (show) {
      element.getStyle().clearDisplay();
    } else {
      element.getStyle().setDisplay(Display.NONE);
    }
  }
}
