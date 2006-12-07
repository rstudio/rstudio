/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * A panel that stacks its children vertically, displaying only one at a time,
 * with a header for each child which the user can click to display.
 * <p>
 * <img class='gallery' src='StackPanel.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-StackPanel { the panel itself }</li>
 * <li>.gwt-StackPanel .gwt-StackPanelItem { unselected items }</li>
 * <li>.gwt-StackPanel .gwt-StackPanelItem-selected { selected items }</li>
 * </ul>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.StackPanelExample}
 * </p>
 */
public class StackPanel extends ComplexPanel implements IndexedPanel {

  private Element body;
  private int visibleStack = -1;

  /**
   * Creates an empty stack panel.
   */
  public StackPanel() {
    Element table = DOM.createTable();
    setElement(table);

    body = DOM.createTBody();
    DOM.appendChild(table, body);
    DOM.setIntAttribute(table, "cellSpacing", 0);
    DOM.setIntAttribute(table, "cellPadding", 0);

    DOM.sinkEvents(table, Event.ONCLICK);
    setStyleName("gwt-StackPanel");
  }

  /**
   * Adds a new child with the given widget.
   * 
   * @param w the widget to be added
   */
  public void add(Widget w) {
    // Call this early to ensure that the table doesn't end up partially
    // constructed when an exception is thrown from adopt().
    w.removeFromParent();

    int index = getWidgetCount();

    Element tr = DOM.createTR();
    Element td = DOM.createTD();
    DOM.appendChild(body, tr);
    DOM.appendChild(tr, td);
    setStyleName(td, "gwt-StackPanelItem", true);
    DOM.setIntAttribute(td, "__index", index);
    DOM.setAttribute(td, "height", "1px");

    tr = DOM.createTR();
    td = DOM.createTD();
    DOM.appendChild(body, tr);
    DOM.appendChild(tr, td);
    DOM.setAttribute(td, "height", "100%");
    DOM.setAttribute(td, "vAlign", "top");

    super.add(w, td);

    setStackVisible(index, false);
    if (visibleStack == -1) {
      showStack(0);
    }
  }

  /**
   * Adds a new child with the given widget and header.
   * 
   * @param w the widget to be added
   * @param stackText the header text associated with this widget
   */
  public void add(Widget w, String stackText) {
    add(w, stackText, false);
  }

  /**
   * Adds a new child with the given widget and header, optionally interpreting
   * the header as HTML.
   * 
   * @param w the widget to be added
   * @param stackText the header text associated with this widget
   * @param asHTML <code>true</code> to treat the specified text as HTML
   */
  public void add(Widget w, String stackText, boolean asHTML) {
    add(w);
    setStackText(getWidgetCount() - 1, stackText, asHTML);
  }

  /**
   * Gets the currently selected child index.
   * 
   * @return selected child
   */
  public int getSelectedIndex() {
    return visibleStack;
  }

  public Widget getWidget(int index) {
    return getChildren().get(index);
  }

  public int getWidgetCount() {
    return getChildren().size();
  }

  public int getWidgetIndex(Widget child) {
    return getChildren().indexOf(child);
  }

  public void onBrowserEvent(Event event) {
    if (DOM.eventGetType(event) == Event.ONCLICK) {
      int index = getDividerIndex(DOM.eventGetTarget(event));
      if (index != -1) {
        showStack(index);
      }
    }
  }

  public boolean remove(int index) {
    return remove(getWidget(index), index);
  }

  public boolean remove(Widget child) {
    return remove(child, getWidgetIndex(child));
  }

  /**
   * Sets the text associated with a child by its index.
   * 
   * @param index the index of the child whose text is to be set
   * @param text the text to be associated with it
   */
  public void setStackText(int index, String text) {
    setStackText(index, text, false);
  }

  /**
   * Sets the text associated with a child by its index.
   * 
   * @param index the index of the child whose text is to be set
   * @param text the text to be associated with it
   * @param asHTML <code>true</code> to treat the specified text as HTML
   */
  public void setStackText(int index, String text, boolean asHTML) {
    if (index >= getWidgetCount()) {
      return;
    }

    Element td = DOM.getChild(DOM.getChild(body, index * 2), 0);
    if (asHTML) {
      DOM.setInnerHTML(td, text);
    } else {
      DOM.setInnerText(td, text);
    }
  }

  /**
   * Shows the widget at the specified child index.
   * 
   * @param index the index of the child to be shown
   */
  public void showStack(int index) {
    if ((index >= getWidgetCount()) || (index == visibleStack)) {
      return;
    }

    if (visibleStack >= 0) {
      setStackVisible(visibleStack, false);
    }

    visibleStack = index;
    setStackVisible(visibleStack, true);
  }

  private int getDividerIndex(Element elem) {
    while ((elem != null) && !DOM.compare(elem, getElement())) {
      String expando = DOM.getAttribute(elem, "__index");
      if (expando != null) {
        return Integer.parseInt(expando);
      }

      elem = DOM.getParent(elem);
    }

    return -1;
  }

  private boolean remove(Widget child, int index) {
    if (child.getParent() != this) {
      return false;
    }

    // Correct visible stack for new location.
    if (visibleStack == index) {
      visibleStack = -1;
    } else if (visibleStack > index) {
      --visibleStack;
    }

    // Calculate which internal table elements to remove.
    int rowIndex = 2 * index;
    Element tr = DOM.getChild(body, rowIndex);
    DOM.removeChild(body, tr);
    tr = DOM.getChild(body, rowIndex);
    DOM.removeChild(body, tr);
    super.remove(child);
    int rows = getWidgetCount() * 2;

    // Update all the indexes.
    for (int i = rowIndex; i < rows; i = i + 2) {
      Element childTR = DOM.getChild(body, i);
      Element td = DOM.getFirstChild(childTR);
      int curIndex = DOM.getIntAttribute(td, "__index");
      assert (curIndex == (i / 2) - 1);
      DOM.setIntAttribute(td, "__index", index);
      ++index;
    }

    return true;
  }

  private void setStackVisible(int index, boolean visible) {
    // Get the first table row containing the widget's selector item.
    Element tr = DOM.getChild(body, (index * 2));
    if (tr == null) {
      return;
    }

    // Style the stack selector item.
    Element td = DOM.getFirstChild(tr);
    setStyleName(td, "gwt-StackPanelItem-selected", visible);

    // Show/hide the contained widget.
    tr = DOM.getChild(body, (index * 2) + 1);
    UIObject.setVisible(tr, visible);
    getWidget(index).setVisible(visible);
  }

}
