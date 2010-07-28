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
 * An absolute panel positions all of its children absolutely, allowing them to
 * overlap.
 * 
 * <p>
 * Note that this panel will not automatically resize itself to allow enough
 * room for its absolutely-positioned children. It must be explicitly sized in
 * order to make room for them.
 * </p>
 * 
 * <p>
 * Once a widget has been added to an absolute panel, the panel effectively
 * "owns" the positioning of the widget. Any existing positioning attributes on
 * the widget may be modified by the panel.
 * </p>
 */
public class AbsolutePanel extends ComplexPanel implements InsertPanel {

  /**
   * Changes a DOM element's positioning to static.
   * 
   * @param elem the DOM element
   */
  private static void changeToStaticPositioning(Element elem) {
    DOM.setStyleAttribute(elem, "left", "");
    DOM.setStyleAttribute(elem, "top", "");
    DOM.setStyleAttribute(elem, "position", "");
  }

  /**
   * Creates an empty absolute panel.
   */
  public AbsolutePanel() {
    this(DOM.createDiv());

    // Setting the panel's position style to 'relative' causes it to be treated
    // as a new positioning context for its children.
    DOM.setStyleAttribute(getElement(), "position", "relative");
    DOM.setStyleAttribute(getElement(), "overflow", "hidden");
  }

  /**
   * Creates an AbsolutePanel with the given element. This is protected so that
   * it can be used by {@link RootPanel} or a subclass that wants to substitute
   * another element. The element is presumed to be a &lt;div&gt;.
   * 
   * @param elem the element to be used for this panel
   */
  protected AbsolutePanel(Element elem) {
    setElement(elem);
  }

  @Override
  public void add(Widget w) {
    super.add(w, getElement());
  }

  /**
   * Adds a widget to the panel at the specified position. Setting a position of
   * <code>(-1, -1)</code> will cause the child widget to be positioned
   * statically.
   * 
   * @param w the widget to be added
   * @param left the widget's left position
   * @param top the widget's top position
   */
  public void add(Widget w, int left, int top) {
    // In order to avoid the potential for a flicker effect, it is necessary
    // to set the position of the widget before adding it to the AbsolutePanel.
    // The Widget should be removed from its parent before any positional
    // changes are made to prevent flickering.
    w.removeFromParent();
    int beforeIndex = getWidgetCount();
    setWidgetPositionImpl(w, left, top);
    insert(w, beforeIndex);
  }

  /**
   * Gets the position of the left outer border edge of the widget relative to
   * the left outer border edge of the panel.
   * 
   * @param w the widget whose position is to be retrieved
   * @return the widget's left position
   */
  public int getWidgetLeft(Widget w) {
    checkWidgetParent(w);
    return DOM.getAbsoluteLeft(w.getElement())
        - DOM.getAbsoluteLeft(getElement());
  }

  /**
   * Gets the position of the top outer border edge of the widget relative to
   * the top outer border edge of the panel.
   * 
   * @param w the widget whose position is to be retrieved
   * @return the widget's top position
   */
  public int getWidgetTop(Widget w) {
    checkWidgetParent(w);
    return DOM.getAbsoluteTop(w.getElement())
        - DOM.getAbsoluteTop(getElement());
  }

  public void insert(Widget w, int beforeIndex) {
    insert(w, getElement(), beforeIndex, true);
  }

  /**
   * Inserts a child widget at the specified position before the specified
   * index. Setting a position of <code>(-1, -1)</code> will cause the child
   * widget to be positioned statically. If the widget is already a child of
   * this panel, it will be moved to the specified index.
   * 
   * @param w the child widget to be inserted
   * @param left the widget's left position
   * @param top the widget's top position
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public void insert(Widget w, int left, int top, int beforeIndex) {
    // In order to avoid the potential for a flicker effect, it is necessary
    // to set the position of the widget before adding it to the AbsolutePanel.
    // The Widget should be removed from its parent before any positional
    // changes are made to prevent flickering.
    w.removeFromParent();
    setWidgetPositionImpl(w, left, top);
    insert(w, beforeIndex);
  }

  /**
   * Overrides {@link ComplexPanel#remove(Widget)} to change the removed
   * Widget's element back to static positioning.This is done so that any
   * positioning changes to the widget that were done by the panel are undone
   * when the widget is disowned from the panel.
   */
  @Override
  public boolean remove(Widget w) {
    boolean removed = super.remove(w);
    if (removed) {
      changeToStaticPositioning(w.getElement());
    }
    return removed;
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
    setWidgetPositionImpl(w, left, top);
  }

  protected void setWidgetPositionImpl(Widget w, int left, int top) {
    Element h = w.getElement();
    if ((left == -1) && (top == -1)) {
      changeToStaticPositioning(h);
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
