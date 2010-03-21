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
import com.google.gwt.bikeshed.list.shared.ListEvent;
import com.google.gwt.bikeshed.list.shared.ListHandler;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.bikeshed.list.shared.ListRegistration;
import com.google.gwt.bikeshed.list.shared.SizeChangeEvent;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
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
public class SimpleCellList<T> extends Widget {

  private final Cell<T> cell;
  private final ArrayList<T> data = new ArrayList<T>();
  private int increment;
  private int maxSize;
  private ListModel<T> model;
  private final Element showMoreElem;
  private final Element tmpElem;
  private ListRegistration reg;
  private ValueUpdater<T> valueUpdater;
  
  public SimpleCellList(ListModel<T> model, Cell<T> cell, int maxSize, int increment) {
    this.maxSize = maxSize;
    this.increment = increment;
    this.model = model;
    this.cell = cell;
    
    tmpElem = Document.get().createDivElement();
    
    showMoreElem = Document.get().createDivElement();
    showMoreElem.setInnerHTML("<i>Show " + increment + " more</i>");
    showMoreElem.getStyle().setDisplay(Display.NONE);

    // TODO: find some way for cells to communicate what they're interested in.
    DivElement outerDiv = Document.get().createDivElement();
    DivElement innerDiv = Document.get().createDivElement();
    outerDiv.appendChild(innerDiv);
    outerDiv.appendChild(showMoreElem);
    setElement(outerDiv);
    sinkEvents(Event.ONCLICK);
    sinkEvents(Event.ONCHANGE);
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
      cell.onBrowserEvent(target, data.get(idx), event, valueUpdater);
    }
  }
  
  public void setValueUpdater(ValueUpdater<T> valueUpdater) {
    this.valueUpdater = valueUpdater;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    
    // Register for model events.
    this.reg = model.addListHandler(new ListHandler<T>() {
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
        if (size > maxSize) {
          showMoreElem.getStyle().clearDisplay();
        } else {
          showMoreElem.getStyle().setDisplay(Display.NONE);
        }
        
        int dataSize = data.size();
        if (size < dataSize) {
          while (size < dataSize) {
            data.remove(dataSize - 1);
            dataSize--;
          }
        } else {
          data.ensureCapacity(size);
          while (dataSize < size) {
            data.add(null);
            dataSize++;
          }
        }
        
        // TODO: This only grows. It needs to shrink as well.
        gc(size);
      }
    });

    // Request up to maxSize elements
    reg.setRangeOfInterest(0, maxSize);
  }
  
  @Override
  protected void onUnload() {
    this.reg.removeHandler();
    this.reg = null;
  }

  private void gc(int size) {
    // Remove unused children if the size shrinks.
    int childCount = getElement().getChildCount();
    while (size < childCount) {
      getElement().getChild(--childCount).removeFromParent();
    }
  }

  private void render(int start, int len, List<T> values) {
    Element parent = getElement().getFirstChildElement();
    int childCount = parent.getChildCount();

    // Create innerHTML for the new items.
    int end = start + len;
    StringBuilder html = new StringBuilder();

    // Empty items to fill any gaps.
    int totalToAdd = 0;
    for (int i = childCount; i < start; ++i) {
      html.append("<div __idx='" + i + "'>");
      cell.render(null, html);
      html.append("</div>");
      ++totalToAdd;
    }

    // Items rendered from data.
    for (int i = start; i < end; ++i) {
      html.append("<div __idx='" + i + "'>");
      cell.render(values.get(i - start), html);
      html.append("</div>");
      ++totalToAdd;
    }

    if (childCount == 0) {
      // Fast path: No cells existed, so we can just user innerHTML.
      parent.setInnerHTML(html.toString());
    } else {
      // Slower path: We can't clobber the existing cells, so we use innerHTML
      // in a temporary element, then move the cells back to the main element.
      tmpElem.setInnerHTML(html.toString());

      // Clear out old cells that overlap the new cells.
      if (start < childCount) {
        int toRemove = Math.min(end, childCount) - start;
        for (int i = 0; i < toRemove; ++i) {
          parent.removeChild(parent.getChild(start));
        }
        childCount = parent.getChildCount();
      }

      // Move the new cells over from the temp element.
      if (start >= childCount) {
        // Just append to the end.
        for (int i = 0; i < totalToAdd; ++i) {
          parent.appendChild(tmpElem.getChild(0));
        }
      } else {
        // Insert them in the middle somewhere.
        Node before = parent.getChild(start);
        for (int i = 0; i < totalToAdd; ++i) {
          parent.insertBefore(tmpElem.getChild(0), before);
        }
      }
    }
  }
}
