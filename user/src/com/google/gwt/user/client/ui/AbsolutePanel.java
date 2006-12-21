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
 * An absolute panel positions all of its children absolutely, allowing them to
 * overlap.
 * 
 * <p>
 * Note that this panel will not automatically resize itself to allow enough
 * room for its absolutely-positioned children. It must be explicitly sized in
 * order to make room for them.
 * </p>
 */
public class AbsolutePanel extends ComplexPanel {

  /**
   * Creates an empty absolute panel.
   */
  public AbsolutePanel() {
    setElement(DOM.createDiv());

    // Setting the panel's position style to 'relative' causes it to be treated
    // as a new positioning context for its children.
    DOM.setStyleAttribute(getElement(), "position", "relative");
    DOM.setStyleAttribute(getElement(), "overflow", "hidden");
  }

  /**
   * Adds a child widget to this panel.
   * 
   * @param w the child widget to be added
   */
  public void add(Widget w) {
    super.add(w, getElement());
  }

  /**
   * Adds a widget to the panel at the specified position.
   * 
   * @param w the widget to be added
   * @param left the widget's left position
   * @param top the widget's top position
   */
  public void add(Widget w, int left, int top) {
    add(w);
    setWidgetPosition(w, left, top);
  }

  /**
   * Gets the left position of the specified widget within the panel.
   * 
   * @param w the widget whose position is to be retrieved
   * @return the widget's left position
   */
  public int getWidgetLeft(Widget w) {
    checkWidgetParent(w);
    return DOM.getIntAttribute(w.getElement(), "offsetLeft");
  }

  /**
   * Gets the top position of the specified widget within the panel.
   * 
   * @param w the widget whose position is to be retrieved
   * @return the widget's top position
   */
  public int getWidgetTop(Widget w) {
    checkWidgetParent(w);
    return DOM.getIntAttribute(w.getElement(), "offsetTop");
  }

  /**
   * Sets the position of the specified child widget. Setting a position of
   * <code>(-1, -1)</code> will cause the child widget to be positioned
   * statically.
   * 
   * @param w the child widget to be positioned
   * @param left the widget's left position
   * @param top the widget's top position
   */
  public void setWidgetPosition(Widget w, int left, int top) {
    checkWidgetParent(w);

    Element h = w.getElement();
    if ((left == -1) && (top == -1)) {
      DOM.setStyleAttribute(h, "left", "");
      DOM.setStyleAttribute(h, "top", "");
      DOM.setStyleAttribute(h, "position", "static");
    } else {
      DOM.setStyleAttribute(h, "position", "absolute");
      DOM.setStyleAttribute(h, "left", left + "px");
      DOM.setStyleAttribute(h, "top", top + "px");
    }
  }

  private void checkWidgetParent(Widget w) {
    if (w.getParent() != this) {
      throw new IllegalArgumentException(
          "Widget must be a child of this panel.");
    }
  }
}
