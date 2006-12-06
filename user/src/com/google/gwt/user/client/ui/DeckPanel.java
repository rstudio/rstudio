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

/**
 * A panel that displays all of its child widgets in a 'deck', where only one
 * can be visible at a time. It is used by
 * {@link com.google.gwt.user.client.ui.TabPanel}.
 */
public class DeckPanel extends ComplexPanel implements IndexedPanel {

  private Widget visibleWidget;

  /**
   * Creates an empty deck panel.
   */
  public DeckPanel() {
    setElement(DOM.createDiv());
  }

  /**
   * Adds the specified widget to the deck.
   * 
   * @param w the widget to be added
   */
  public void add(Widget w) {
    insert(w, getWidgetCount());
  }

  /**
   * Gets the index of the currently-visible widget.
   * 
   * @return the visible widget's index
   */
  public int getVisibleWidget() {
    return getWidgetIndex(visibleWidget);
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

  /**
   * Inserts a widget before the specified index.
   * 
   * @param w the widget to be inserted
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public void insert(Widget w, int beforeIndex) {
    if ((beforeIndex < 0) || (beforeIndex > getWidgetCount())) {
      throw new IndexOutOfBoundsException();
    }

    super.insert(w, getElement(), beforeIndex);

    Element child = w.getElement();
    DOM.setStyleAttribute(child, "width", "100%");
    DOM.setStyleAttribute(child, "height", "100%");
    w.setVisible(false);
  }

  public boolean remove(int index) {
    return remove(getWidget(index));
  }

  public boolean remove(Widget w) {
    if (!super.remove(w)) {
      return false;
    }

    if (visibleWidget == w) {
      visibleWidget = null;
    }

    return true;
  }

  /**
   * Shows the widget at the specified index. This causes the currently- visible
   * widget to be hidden.
   * 
   * @param index the index of the widget to be shown
   */
  public void showWidget(int index) {
    checkIndex(index);

    if (visibleWidget != null) {
      visibleWidget.setVisible(false);
    }
    visibleWidget = getWidget(index);
    visibleWidget.setVisible(true);
  }

  private void checkIndex(int index) {
    if ((index < 0) || (index >= getWidgetCount())) {
      throw new IndexOutOfBoundsException();
    }
  }
}
