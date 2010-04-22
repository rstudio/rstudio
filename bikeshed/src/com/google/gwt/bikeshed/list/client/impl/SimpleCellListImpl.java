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
package com.google.gwt.bikeshed.list.client.impl;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.list.client.ListView;
import com.google.gwt.bikeshed.list.client.ListView.Delegate;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.SelectionModel;
import com.google.gwt.bikeshed.list.shared.AbstractListViewAdapter.DefaultRange;
import com.google.gwt.bikeshed.list.shared.SelectionModel.SelectionChangeEvent;
import com.google.gwt.bikeshed.list.shared.SelectionModel.SelectionChangeHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.shared.HandlerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link SimpleCellListImpl}. This class is subject to change
 * or deletion. Do not rely on this class.
 * 
 * @param <T> the data type of items in the list
 */
public abstract class SimpleCellListImpl<T> {

  private final Cell<T, Void> cell;
  private final Element childContainer;
  private final List<T> data = new ArrayList<T>();
  private final Set<Object> selectedKeys = new HashSet<Object>();
  private Delegate<T> delegate;
  private final Element emptyMessageElem;
  private final int increment;
  private final int initialMaxSize;
  private final ListView<T> listView;
  private int maxSize;
  private HandlerRegistration selectionHandler;
  private SelectionModel<? super T> selectionModel;
  private final Element showFewerElem;
  private final Element showMoreElem;
  private int size;
  private final Element tmpElem;

  public SimpleCellListImpl(ListView<T> listView, Cell<T, Void> cell,
      int maxSize, int increment, Element childContainer,
      Element emptyMessageElem, Element showMoreElem, Element showFewerElem) {
    this.cell = cell;
    this.childContainer = childContainer;
    this.emptyMessageElem = emptyMessageElem;
    this.increment = increment;
    this.initialMaxSize = maxSize;
    this.listView = listView;
    this.maxSize = maxSize;
    this.showFewerElem = showFewerElem;
    this.showMoreElem = showMoreElem;
    tmpElem = Document.get().createDivElement();

    showOrHide(showMoreElem, false);
    showOrHide(showFewerElem, false);
    showOrHide(emptyMessageElem, false);
  }

  public Range getRange() {
    return new DefaultRange(0, maxSize);
  }

  public SelectionModel<? super T> getSelectionModel() {
    return selectionModel;
  }

  public Element getShowFewerElem() {
    return showFewerElem;
  }

  public Element getShowMoreElem() {
    return showMoreElem;
  }

  public T getValue(int i) {
    return data.get(i);
  }

  /**
   * Set the data in the list.
   * 
   * @param values the new data
   * @param start the start index
   */
  public void setData(List<T> values, int start) {
    int oldSize = size;
    int len = values.size();
    int end = start + len;

    // The size must be at least as large as the data.
    if (end > oldSize) {
      size = end;
      sizeChanged();
    }

    // Create placeholders up to the specified index.
    while (data.size() < start) {
      data.add(null);
    }

    // Insert the new values into the data array.
    for (int i = start; i < end; i++) {
      T value = values.get(i - start);
      if (i < data.size()) {
        data.set(i, value);
      } else {
        data.add(value);

        // Update our local cache of selected values. We only need to consider
        // new values at this point. If any existing value changes its selection
        // state, we'll find out from the selection model.
        if (selectionModel != null && selectionModel.isSelected(value)) {
          selectedKeys.add(getKey(value));
        }
      }
    }

    // Construct a run of element from start (inclusive) to start + len
    // (exclusive)
    StringBuilder sb = new StringBuilder();
    emitHtml(sb, values, start, cell, selectionModel);

    // Replace the DOM elements with the new rendered cells.
    if (oldSize == 0 || (start == 0 && len >= oldSize)) {
      childContainer.setInnerHTML(sb.toString());
    } else {
      makeElements();
      tmpElem.setInnerHTML(sb.toString());
      Element toReplace = childContainer.getChild(start).cast();
      for (int i = start; i < end; i++) {
        // The child will be removed from tmpElem, so always use index 0.
        Element nextSibling = toReplace.getNextSiblingElement();
        childContainer.replaceChild(tmpElem.getChild(0), toReplace);
        toReplace = nextSibling;
      }
    }
  }

  /**
   * Set the overall size of the list.
   * 
   * @param size the overall size
   */
  public void setDataSize(int size) {
    this.size = size;
    int toRemove = data.size() - size;
    for (int i = 0; i < toRemove; i++) {
      removeLastItem();
    }
    sizeChanged();
  }

  public void setDelegate(Delegate<T> delegate) {
    this.delegate = delegate;
  }

  public void setSelectionModel(final SelectionModel<? super T> selectionModel) {
    // Remove the old selection model.
    if (selectionHandler != null) {
      selectionHandler.removeHandler();
      selectionHandler = null;
    }

    // Set the new selection model.
    this.selectionModel = selectionModel;
    if (selectionModel != null) {
      selectionHandler = selectionModel.addSelectionChangeHandler(new SelectionChangeHandler() {
        public void onSelectionChange(SelectionChangeEvent event) {
          // Determine if our selection states are stale.
          boolean dependsOnSelection = cell.dependsOnSelection();
          boolean refreshRequired = false;
          Element cellElem = childContainer.getFirstChildElement();
          for (T value : data) {
            boolean selected = selectionModel.isSelected(value);
            Object key = getKey(value);
            if (selected != selectedKeys.contains(key)) {
              refreshRequired = true;
              if (selected) {
                selectedKeys.add(key);
              } else {
                selectedKeys.remove(key);
              }
              if (!dependsOnSelection) {
                // The cell doesn't depend on Selection, so we only need to
                // update the style.
                setSelected(cellElem, selected);
              }
            }
            cellElem = cellElem.getNextSiblingElement();
          }

          // Refresh the entire list if needed.
          if (refreshRequired && dependsOnSelection) {
            setData(data, 0);
          }
        }
      });
    }
  }

  /**
   * Show fewer items.
   */
  public void showFewer() {
    this.maxSize = Math.max(initialMaxSize, maxSize - increment);
    sizeChanged();
    if (delegate != null) {
      delegate.onRangeChanged(listView);
    }
  }

  /**
   * Show more items.
   */
  public void showMore() {
    this.maxSize += increment;
    sizeChanged();
    if (delegate != null) {
      delegate.onRangeChanged(listView);
    }
  }

  /**
   * Construct the HTML that represents the list of items.
   * 
   * @param sb the {@link StringBuilder} to build into
   * @param values the values to render
   * @param start the start index
   * @param cell the cell to use as a renderer
   * @param selectionModel the {@link SelectionModel}
   */
  protected abstract void emitHtml(StringBuilder sb, List<T> values, int start,
      Cell<T, Void> cell, SelectionModel<? super T> selectionModel);

  /**
   * Remove the last element from the list.
   */
  protected void removeLastItem() {
    data.remove(data.size() - 1);
    childContainer.getLastChild().removeFromParent();
  }

  /**
   * Mark an element as selected or unselected. This is called when a cells
   * selection state changes, but the cell does not depend on selection.
   * 
   * @param elem the element to modify
   * @param selected true if selected, false if not
   */
  protected abstract void setSelected(Element elem, boolean selected);

  /**
   * Get the key for a given item.
   * 
   * @param value the item
   * @return the key, or null if there is no selection model
   */
  private Object getKey(T value) {
    return selectionModel == null ? null
        : selectionModel.getKeyProvider().getKey(value);
  }

  /**
   * Create placeholder elements that will be replaced with data. This is used s
   * when replacing a subset of the list.
   */
  private void makeElements() {
    int childCount = childContainer.getChildCount();
    int actualSize = Math.min(data.size(), maxSize);
    if (actualSize > childCount) {
      // Create new elements with a "loading..." message
      StringBuilder sb = new StringBuilder();
      int newElements = actualSize - childCount;
      for (int i = 0; i < newElements; i++) {
        // TODO(jlabanca): Make this I18N friendly.
        sb.append("<div __idx='" + (childCount + i)
            + "'><i>loading...</i></div>");
      }

      if (childCount == 0) {
        childContainer.setInnerHTML(sb.toString());
      } else {
        tmpElem.setInnerHTML(sb.toString());
        for (int i = 0; i < newElements; i++) {
          childContainer.appendChild(tmpElem.getChild(0));
        }
      }
    } else if (actualSize < childCount) {
      // Remove excess elements
      while (actualSize < childCount) {
        removeLastItem();
        childCount--;
      }
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

  /**
   * Called when the size of the list changes.
   */
  private void sizeChanged() {
    showOrHide(showMoreElem, size > maxSize);
    showOrHide(showFewerElem, maxSize > initialMaxSize);
    showOrHide(emptyMessageElem, size == 0);
  }
}
