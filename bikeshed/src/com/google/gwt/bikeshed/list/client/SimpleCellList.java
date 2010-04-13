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
package com.google.gwt.bikeshed.list.client;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.list.shared.DataChanged;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.AbstractListViewAdapter.DefaultRange;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * A single column list of cells.
 *
 * @param <T> the data type of list items
 */
public class SimpleCellList<T> extends Widget implements ListView<T> {

  private final Cell<T, Void> cell;
  private final ArrayList<T> data = new ArrayList<T>();
  private Delegate<T> delegate;
  private int increment;
  private int initialMaxSize;
  private int maxSize;
  private int seq; // for debugging - TODO: remove
  private final Element showFewerElem;
  private final Element showMoreElem;
  private int size;
  private final Element tmpElem;
  private ValueUpdater<T, Void> valueUpdater;

  public SimpleCellList(Cell<T, Void> cell, int maxSize, int increment) {
    this.initialMaxSize = this.maxSize = maxSize;
    this.increment = increment;
    this.cell = cell;
    this.seq = 0;

    tmpElem = Document.get().createDivElement();

    showMoreElem = Document.get().createDivElement();
    showMoreElem.setInnerHTML("<button>Show more</button>");

    showFewerElem = Document.get().createDivElement();
    showFewerElem.setInnerHTML("<button>Show fewer</button>");

    showOrHide(showMoreElem, false);
    showOrHide(showFewerElem, false);

    // TODO: find some way for cells to communicate what they're interested in.
    DivElement outerDiv = Document.get().createDivElement();
    DivElement innerDiv = Document.get().createDivElement();
    outerDiv.appendChild(innerDiv);
    outerDiv.appendChild(showFewerElem);
    outerDiv.appendChild(showMoreElem);
    setElement(outerDiv);
    sinkEvents(Event.ONCLICK);
    sinkEvents(Event.ONCHANGE);
  }

  public Range getRange() {
    return new DefaultRange(0, maxSize);
  }

  @Override
  public void onBrowserEvent(Event event) {
    Element target = event.getEventTarget().cast();
    if (target.getParentElement() == showMoreElem) {
      this.maxSize += increment;
      sizeChanged();
      if (delegate != null) {
        delegate.onRangeChanged(this);
      }
    } else if (target.getParentElement() == showFewerElem) {
      this.maxSize = Math.max(initialMaxSize, maxSize - increment);
      sizeChanged();
      if (delegate != null) {
        delegate.onRangeChanged(this);
      }
    } else {
      String idxString = "";
      while ((target != null)
          && ((idxString = target.getAttribute("__idx")).length() == 0)) {
        target = target.getParentElement();
      }
      if (idxString.length() > 0) {
        int idx = Integer.parseInt(idxString);
        cell.onBrowserEvent(target, data.get(idx), null, event, valueUpdater);
      }
    }
  }

  public void setData(DataChanged<T> event) {
    int start = event.getStart();
    int len = event.getLength();
    List<T> values = event.getValues();

    // Construct a run of element from start (inclusive) to start + len (exclusive)
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      sb.append("<div __idx='" + (start + i) + "' __seq='" + seq++ + "'>");
      cell.render(values.get(i), null, sb);
      sb.append("</div>");
    }

    Element parent = getElement().getFirstChildElement();
    if (start == 0 && len == maxSize) {
      parent.setInnerHTML(sb.toString());
    } else {
      makeElements();
      tmpElem.setInnerHTML(sb.toString());
      for (int i = 0; i < len; i++) {
        Element child = parent.getChild(start + i).cast();
        parent.replaceChild(tmpElem.getChild(0), child);
      }
    }
  }

  public void setDelegate(Delegate<T> delegate) {
    this.delegate = delegate;
    if (delegate != null) {
      delegate.onRangeChanged(this);
    }
  }

  public void setSize(int size, boolean exact) {
    this.size = size;
    sizeChanged();
  }

  public void setValueUpdater(ValueUpdater<T, Void> valueUpdater) {
    this.valueUpdater = valueUpdater;
  }

  private void makeElements() {
    Element parent = getElement().getFirstChildElement();
    int childCount = parent.getChildCount();

    int actualSize = Math.min(size, maxSize);
    if (actualSize > childCount) {
      // Create new elements with a "loading..." message
      StringBuilder sb = new StringBuilder();
      int newElements = actualSize - childCount;
      for (int i = 0; i < newElements; i++) {
        sb.append("<div __idx='" + (childCount + i) + "'><i>loading...</i></div>");
      }

      if (childCount == 0) {
        parent.setInnerHTML(sb.toString());
      } else {
        tmpElem.setInnerHTML(sb.toString());
        for (int i = 0; i < newElements; i++) {
          parent.appendChild(tmpElem.getChild(0));
        }
      }
    } else if (actualSize < childCount) {
      // Remove excess elements
      while (actualSize < childCount) {
        parent.getChild(--childCount).removeFromParent();
      }
    }
  }

  private void showOrHide(Element element, boolean show) {
    if (show) {
      element.getStyle().clearDisplay();
    } else {
      element.getStyle().setDisplay(Display.NONE);
    }
  }

  private void sizeChanged() {
    showOrHide(showMoreElem, size > maxSize);
    showOrHide(showFewerElem, maxSize > initialMaxSize);
  }
}
