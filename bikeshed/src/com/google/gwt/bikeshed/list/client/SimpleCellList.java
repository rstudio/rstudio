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
import com.google.gwt.bikeshed.list.client.impl.SimpleCellListImpl;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.SelectionModel;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * A single column list of cells.
 * 
 * @param <T> the data type of list items
 */
public class SimpleCellList<T> extends Widget implements ListView<T> {

  /**
   * Style name applied to even rows.
   * TODO(jlabanca): These style names only apply to SideBySideTreeView.
   */
  private static final String STYLENNAME_EVEN = "gwt-sstree-evenRow";

  /**
   * Style name applied to odd rows.
   * TODO(jlabanca): These style names only apply to SideBySideTreeView.
   */
  private static final String STYLENNAME_ODD = "gwt-sstree-oddRow";

  /**
   * Style name applied to selected rows.
   * TODO(jlabanca): These style names only apply to SideBySideTreeView.
   */
  private static final String STYLENNAME_SELECTED = "gwt-sstree-selectedItem";

  private final Cell<T, Void> cell;
  private final SimpleCellListImpl<T> impl;
  private ValueUpdater<T, Void> valueUpdater;

  public SimpleCellList(Cell<T, Void> cell, int maxSize, int increment) {
    this.cell = cell;

    // Create the DOM hierarchy.
    Element childContainer = Document.get().createDivElement();

    Element showMoreElem = Document.get().createPushButtonElement();
    showMoreElem.setInnerText("Show more");

    Element showFewerElem = Document.get().createPushButtonElement();
    showFewerElem.setInnerText("Show fewer");

    Element emptyMessageElem = Document.get().createDivElement();
    emptyMessageElem.setInnerHTML("<i>no data</i>");

    // TODO: find some way for cells to communicate what they're interested in.
    DivElement outerDiv = Document.get().createDivElement();
    outerDiv.appendChild(childContainer);
    outerDiv.appendChild(emptyMessageElem);
    outerDiv.appendChild(showFewerElem);
    outerDiv.appendChild(showMoreElem);
    setElement(outerDiv);
    sinkEvents(Event.ONCLICK | Event.ONCHANGE | Event.MOUSEEVENTS);

    // Create the implementation.
    impl = new SimpleCellListImpl<T>(this, cell, maxSize, increment,
        childContainer, emptyMessageElem, showMoreElem, showFewerElem) {

      @Override
      protected void emitHtml(StringBuilder sb, List<T> values, int start,
          Cell<T, Void> cell, SelectionModel<? super T> selectionModel) {
        int length = values.size();
        int end = start + length;
        for (int i = start; i < end; i++) {
          T value = values.get(i - start);
          boolean isSelected = selectionModel == null ? false
              : selectionModel.isSelected(value);
          sb.append("<div __idx='").append(i).append("'");
          sb.append(" class='");
          sb.append(i % 2 == 0 ? STYLENNAME_EVEN : STYLENNAME_ODD);
          if (isSelected) {
            sb.append(" ").append(STYLENNAME_SELECTED);
          }
          sb.append("'>");
          cell.render(value, null, sb);
          sb.append("</div>");
        }
      }

      @Override
      protected void setSelected(Element elem, boolean selected) {
        setStyleName(elem, STYLENNAME_SELECTED, selected);
      }
    };
  }

  public Range getRange() {
    return impl.getRange();
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    Element target = event.getEventTarget().cast();
    int type = event.getTypeInt();
    if (type == Event.ONCLICK) {
      // Open the node when the open image is clicked.
      Element showFewerElem = impl.getShowFewerElem();
      Element showMoreElem = impl.getShowMoreElem();
      if (showFewerElem != null && showFewerElem.isOrHasChild(target)) {
        impl.showFewer();
        return;
      } else if (showMoreElem != null && showMoreElem.isOrHasChild(target)) {
        impl.showMore();
        return;
      }
    }

    // Forward the event to the cell.
    String idxString = "";
    while ((target != null)
        && ((idxString = target.getAttribute("__idx")).length() == 0)) {
      target = target.getParentElement();
    }
    if (idxString.length() > 0) {
      int idx = Integer.parseInt(idxString);
      T value = impl.getValue(idx);
      cell.onBrowserEvent(target, value, null, event, valueUpdater);
      if (!cell.consumesEvents() && type == Event.ONMOUSEDOWN) {
        SelectionModel<? super T> selectionModel = impl.getSelectionModel();
        if (selectionModel != null) {
          selectionModel.setSelected(value, true);
        }
      }
    }
  }

  public void setData(int start, int length, List<T> values) {
    impl.setData(values, start);
  }

  public void setDataSize(int size, boolean isExact) {
    impl.setDataSize(size);
  }

  public void setDelegate(Delegate<T> delegate) {
    impl.setDelegate(delegate);
  }

  public void setSelectionModel(final SelectionModel<? super T> selectionModel) {
    impl.setSelectionModel(selectionModel);
  }

  /**
   * Set the value updater to use when cells modify items.
   * 
   * @param valueUpdater the {@link ValueUpdater}
   */
  public void setValueUpdater(ValueUpdater<T, Void> valueUpdater) {
    this.valueUpdater = valueUpdater;
  }
}
