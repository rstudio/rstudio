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

  private final Cell<T, Void> cell;
  private final ArrayList<T> data = new ArrayList<T>();
  private int increment;
  private int initialMaxSize;
  private int maxSize;
  private ListModel<T> model;
  private ListRegistration<T> reg;
  private int seq; // for debugging - TODO: remove
  private final Element showFewerElem;
  private final Element showMoreElem;
  private int size;
  private final Element tmpElem;
  private ValueUpdater<T, Void> valueUpdater;
  public SimpleCellList(ListModel<T> model, Cell<T, Void> cell, int maxSize,
      int increment) {
    this.initialMaxSize = this.maxSize = maxSize;
    this.increment = increment;
    this.model = model;
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

  @Override
  public void onBrowserEvent(Event event) {
    Element target = event.getEventTarget().cast();
    if (target.getParentElement() == showMoreElem) {
      this.maxSize += increment;
      reg.setRangeOfInterest(0, maxSize);
    } else if (target.getParentElement() == showFewerElem) {
      this.maxSize = Math.max(initialMaxSize, maxSize - increment);
      reg.setRangeOfInterest(0, maxSize);
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
  
  public void setValueUpdater(ValueUpdater<T, Void> valueUpdater) {
    this.valueUpdater = valueUpdater;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    
    // Register for model events.
    this.reg = model.addListHandler(new ListHandler<T>() {
      public void onDataChanged(ListEvent<T> event) {
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

      public void onSizeChanged(SizeChangeEvent event) {
        size = event.getSize();
        showOrHide(showMoreElem, size > maxSize);
        showOrHide(showFewerElem, maxSize > initialMaxSize);
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
    });

    // Request up to maxSize elements
    reg.setRangeOfInterest(0, maxSize);
  }
  
  @Override
  protected void onUnload() {
    this.reg.removeHandler();
    this.reg = null;
  }

  private void showOrHide(Element element, boolean show) {
    if (show) {
      element.getStyle().clearDisplay();
    } else {
      element.getStyle().setDisplay(Display.NONE);
    }
  }
}
