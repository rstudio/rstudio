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
package com.google.gwt.list.client;

import com.google.gwt.cells.client.Cell;
import com.google.gwt.cells.client.Mutator;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListModel;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * A single column list of cells.
 * 
 * @param <T> the data type of list items
 */
public class SimpleCellList<T> extends Widget {

  private final Cell<T> cell;
  private final ArrayList<T> data = new ArrayList<T>();
  private final Element tmpElem;
  private ListRegistration reg;
  private Mutator<T, T> mutator;

  public SimpleCellList(ListModel<T> model, Cell<T> cell) {
    this.cell = cell;
    tmpElem = Document.get().createDivElement();

    // TODO: find some way for cells to communicate what they're interested in.
    setElement(Document.get().createDivElement());
    sinkEvents(Event.ONCLICK);
    sinkEvents(Event.ONCHANGE);

    // Register for model events.
    reg = model.addListHandler(new ListHandler<T>() {
      public void onDataChanged(ListEvent<T> event) {
        int start = event.getStart(), len = event.getLength();
        List<T> values = event.getValues();
        for (int i = 0; i < len; ++i) {
          data.set(start + i, values.get(i));
        }
        render(start, len, values);
      }

      public void onSizeChanged(SizeChangeEvent event) {
        int size = event.getSize();

        // Is there no better way than this mess?
        data.ensureCapacity(size);
        while (data.size() < size) {
          data.add(null);
        }
        // TODO: This only grows. It needs to shrink as well.

        gc(size);
        reg.setRangeOfInterest(0, size);
      }
    });

    // Start with no range of interest. This will be updated as soon as the
    // list size changes.
    reg.setRangeOfInterest(0, 0);
  }

  @Override
  public void onBrowserEvent(Event event) {
    Element target = event.getEventTarget().cast();
    String idxString = "";
    while ((target != null)
        && ((idxString = target.getAttribute("__idx")).length() == 0)) {
      target = target.getParentElement();
    }
    if (idxString.length() > 0) {
      int idx = Integer.parseInt(idxString);
      cell.onBrowserEvent(target, data.get(idx), event, mutator);
    }
  }

  public void setMutator(Mutator<T, T> mutator) {
    this.mutator = mutator;
  }

  private void gc(int size) {
    // Remove unused children if the size shrinks.
    int childCount = getElement().getChildCount();
    while (size < childCount) {
      getElement().getChild(--childCount).removeFromParent();
    }
  }

  private void render(int start, int len, List<T> values) {
    Element parent = getElement();
    int childCount = parent.getChildCount();

    // Update existing cells with new values.
    int i, existing = Math.min(len, childCount);
    for (i = start; i < existing; ++i) {
      Element elem = parent.getChild(i).cast();
      cell.setValue(elem, values.get(i));
    }

    // Create new cells if necessary.
    StringBuilder html = new StringBuilder();
    for (; i < len; ++i) {
      html.append("<div __idx='" + i + "'>");
      cell.render(values.get(i), html);
      html.append("</div>");
    }

    if (childCount == 0) {
      // Fast path: No cells existed, so we can just user innerHTML.
      parent.setInnerHTML(html.toString());
    } else {
      // Slower path: We can't clobber the existing cells, so we use innerHTML
      // in a temporary element, then move the cells back to the main element.
      tmpElem.setInnerHTML(html.toString());

      // Move the new cells over from the temp element.
      for (i = 0; i < len - childCount; ++i) {
        parent.appendChild(tmpElem.getChild(0));
      }
    }
  }
}
