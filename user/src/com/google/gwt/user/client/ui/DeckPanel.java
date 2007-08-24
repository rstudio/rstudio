/*
 * Copyright 2007 Google Inc.
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
 * 
 * <p>
 * Once a widget has been added to a DeckPanel, its visibility, width, and
 * height attributes will be manipulated. When the widget is removed from the
 * DeckPanel, it will be visible, and its width and height attributes will be
 * cleared.
 * </p>
 */
public class DeckPanel extends ComplexPanel {

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
  @Override
  public void add(Widget w) {
    super.add(w, getElement());
    initChildWidget(w);
  }

  /**
   * Gets the index of the currently-visible widget.
   * 
   * @return the visible widget's index
   */
  public int getVisibleWidget() {
    return getWidgetIndex(visibleWidget);
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
    super.insert(w, getElement(), beforeIndex, true);
    initChildWidget(w);
  }

  @Override
  public boolean remove(Widget w) {
    boolean removed = super.remove(w);
    if (removed) {
      resetChildWidget(w);

      if (visibleWidget == w) {
        visibleWidget = null;
      }
    }
    return removed;
  }

  /**
   * Shows the widget at the specified index. This causes the currently- visible
   * widget to be hidden.
   * 
   * @param index the index of the widget to be shown
   */
  public void showWidget(int index) {
    checkIndexBoundsForAccess(index);

    if (visibleWidget != null) {
      visibleWidget.setVisible(false);
    }
    visibleWidget = getWidget(index);
    visibleWidget.setVisible(true);
  }

  /**
   * Make the widget invisible, and set its width and height to full.
   */
  private void initChildWidget(Widget w) {
    Element child = w.getElement();
    DOM.setStyleAttribute(child, "width", "100%");
    DOM.setStyleAttribute(child, "height", "100%");
    w.setVisible(false);
  }

  /**
   * Make the widget visible, and clear the widget's width and height
   * attributes. This is done so that any changes to the visibility, height, or
   * width of the widget that were done by the panel are undone.
   */
  private void resetChildWidget(Widget w) {
    DOM.setStyleAttribute(w.getElement(), "width", "");
    DOM.setStyleAttribute(w.getElement(), "height", "");
    w.setVisible(true);
  }
}
