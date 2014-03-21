/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.user.datepicker.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.impl.ElementMapperImpl;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Highlighting, selectable cell grid. Used to help construct the default
 * calendar view.
 * 
 * @param <V> type of value in grid.
 */
abstract class CellGridImpl<V> extends Grid {

  abstract class Cell extends Widget {
    private boolean enabled = true;
    private V value;
    private int index;

    Cell(V value) {
      this(Document.get().createDivElement(), value);
    }

    Cell(Element elem, V value) {
      this.value = value;
      index = cellList.size();
      cellList.add(this);

      if (elem != null) {
        setElement(elem);
      }

      elementToCell.put(this);
      addDomHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER ||
             event.getNativeKeyCode() == ' ') {
            if (isActive(Cell.this)) {
              setSelected(Cell.this);
            }
          }
        }
      }, KeyDownEvent.getType());

      addDomHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (isActive(Cell.this)) {
              setSelected(Cell.this);
            }
          }
        }, ClickEvent.getType());
    }

    public V getValue() {
      return value;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public boolean isHighlighted() {
      return this == highlightedCell;
    }

    public boolean isSelected() {
      return selectedCell == this;
    }

    public final void setEnabled(boolean enabled) {
      this.enabled = enabled;
      onEnabled(enabled);
    }

    public void verticalNavigation(int keyCode) {
      switch (keyCode) {
        case KeyCodes.KEY_UP:
          setHighlighted(previousItem());
          break;
        case KeyCodes.KEY_DOWN:
          setHighlighted(nextItem());
          break;
        case KeyCodes.KEY_ESCAPE:
          // Figure out new event for this.
          break;
        case KeyCodes.KEY_ENTER:
          setSelected(this);
          break;
      }
    }

    protected Cell nextItem() {
      if (index == getLastIndex()) {
        return cellList.get(0);
      } else {
        return cellList.get(index + 1);
      }
    }

    /**
     * @param enabled
     */
    protected void onEnabled(boolean enabled) {
      updateStyle();
    }

    /**
     * @param highlighted
     */
    protected void onHighlighted(boolean highlighted) {
      updateStyle();
    }

    /**
     * @param selected
     */
    protected void onSelected(boolean selected) {
      updateStyle();
    }

    protected Cell previousItem() {
      if (index != 0) {
        return cellList.get(index - 1);
      } else {
        return cellList.get(getLastIndex());
      }
    }

    protected abstract void updateStyle();

    private int getLastIndex() {
      return cellList.size() - 1;
    }
  }

  private Cell highlightedCell;

  private Cell selectedCell;
  private ElementMapperImpl<Cell> elementToCell = new ElementMapperImpl<Cell>();
  private ArrayList<Cell> cellList = new ArrayList<Cell>();

  protected CellGridImpl() {
    setCellPadding(0);
    setCellSpacing(0);
    setBorderWidth(0);
    sinkEvents(Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT);
  }

  public Cell getCell(Element element) {
    return elementToCell.get(element);
  }

  public Cell getCell(Event e) {
    // Find out which cell was actually clicked.
    Element td = getEventTargetCell(e);
    return td != null ? elementToCell.get(td) : null;
  }

  public Cell getCell(int i) {
    return cellList.get(i);
  }

  public Iterator getCells() {
    return cellList.iterator();
  }

  public Cell getHighlightedCell() {
    return highlightedCell;
  }

  public int getNumCells() {
    return cellList.size();
  }

  public Cell getSelectedCell() {
    return selectedCell;
  }

  public V getSelectedValue() {
    return getValue(selectedCell);
  }

  public V getValue(Cell cell) {
    return (cell == null ? null : cell.getValue());
  }

  @Override
  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK: {
        Cell cell = getCell(event);
        if (isActive(cell)) {
          setSelected(cell);
        }
        break;
      }
      case Event.ONMOUSEOUT: {
        Element e = DOM.eventGetFromElement(event);
        if (e != null) {
          Cell cell = elementToCell.get(e);
          if (cell == highlightedCell) {
            setHighlighted(null);
          }
        }
        break;
      }
      case Event.ONMOUSEOVER: {
        Element e = DOM.eventGetToElement(event);
        if (e != null) {
          Cell cell = elementToCell.get(e);
          if (isActive(cell)) {
            setHighlighted(cell);
          }
        }
        break;
      }
    }
  }

  @Override
  public void onUnload() {
    setHighlighted(null);
  }

  public final void setHighlighted(Cell nextHighlighted) {
    if (nextHighlighted == highlightedCell) {
      return;
    }
    Cell oldHighlighted = highlightedCell;
    highlightedCell = nextHighlighted;
    if (oldHighlighted != null) {
      oldHighlighted.onHighlighted(false);
    }
    if (highlightedCell != null) {
      highlightedCell.onHighlighted(true);
    }
  }

  public final void setSelected(Cell cell) {
    Cell last = getSelectedCell();
    selectedCell = cell;

    if (last != null) {
      last.onSelected(false);
    }
    if (selectedCell != null) {
      selectedCell.onSelected(true);
    }
    onSelected(last, selectedCell);
  }

  protected abstract void onSelected(Cell lastSelected, Cell cell);

  private boolean isActive(Cell cell) {
    return cell != null && cell.isEnabled();
  }
}
