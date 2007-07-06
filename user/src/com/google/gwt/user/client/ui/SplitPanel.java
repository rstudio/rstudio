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
import com.google.gwt.user.client.Event;

import java.util.Iterator;

/**
 * Abstract base class for {@link HorizontalSplitPanel} and
 * {@link VerticalSplitPanel}.
 */
abstract class SplitPanel extends Panel {

  /**
   * Adds clipping to an element.
   * 
   * @param elem the element
   */
  static final void addElementClipping(final Element elem) {
    DOM.setStyleAttribute(elem, "overflow", "hidden");
  }

  /**
   * Adds as-needed scrolling to an element.
   * 
   * @param elem the element
   */
  static final void addElementScrolling(final Element elem) {
    DOM.setStyleAttribute(elem, "overflow", "auto");
  }

  /**
   * Adds zero or none css values for padding, margin and border to prevent
   * stylesheet overrides. Returns the element for convienence to support
   * builder pattern.
   * 
   * @param elem the element
   * @return the element
   */
  static final Element preventElementBoxStyles(final Element elem) {
    DOM.setIntStyleAttribute(elem, "padding", 0);
    DOM.setIntStyleAttribute(elem, "margin", 0);
    DOM.setStyleAttribute(elem, "border", "none");
    return elem;
  }

  /**
   * Adds zero size padding to an element.
   * 
   * @param elem the element.
   */
  static final void preventElementPadding(final Element elem) {
    DOM.setStyleAttribute(elem, "padding", "0");
  }

  /**
   * Sets the elements css class name.
   * 
   * @param elem the element
   * @param className the class name
   */
  static final void setElementClassname(final Element elem,
      final String className) {
    DOM.setElementProperty(elem, "className", className);
  }

  // The enclosed widgets.
  private final Widget[] widgets = new Widget[2];

  // The elements containing the widgets.
  private final Element[] elements = new Element[2];

  // The element that acts as the splitter.
  private final Element splitElem;

  // Indicates whether drag resizing is active.
  private boolean isResizing = false;

  /**
   * Initializes the split panel.
   * 
   * @param mainElem the root element for the split panel
   * @param splitElem the element that acts as the splitter
   * @param headElem the element to contain the top or left most widget
   * @param tailElem the element to contain the bottom or right most widget
   */
  SplitPanel(Element mainElem, Element splitElem, Element headElem,
      Element tailElem) {
    setElement(mainElem);
    this.splitElem = splitElem;
    elements[0] = headElem;
    elements[1] = tailElem;
    sinkEvents(Event.MOUSEEVENTS);
  }

  public void add(Widget w) {
    if (getWidget(0) == null) {
      setWidget(0, w);
    } else if (getWidget(1) == null) {
      setWidget(1, w);
    } else {
      throw new IllegalStateException(
          "A Splitter can only contain two Widgets.");
    }
  }

  /**
   * Indicates whether the split panel is being resized.
   * 
   * @return <code>true</code> if the user is dragging the splitter,
   *         <code>false</code> otherwise
   */
  public boolean isResizing() {
    return isResizing;
  }

  public Iterator iterator() {
    return WidgetIterators.createWidgetIterator(this, widgets);
  }

  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {

      case Event.ONMOUSEDOWN: {
        Element target = DOM.eventGetTarget(event);
        if (DOM.isOrHasChild(splitElem, target)) {
          startResizingFrom(DOM.eventGetClientX(event) - getAbsoluteLeft(),
              DOM.eventGetClientY(event) - getAbsoluteTop());
          DOM.setCapture(getElement());
          DOM.eventPreventDefault(event);
        }
        break;
      }

      case Event.ONMOUSEUP: {
        DOM.releaseCapture(getElement());
        stopResizing();
        break;
      }

      case Event.ONMOUSEMOVE: {
        if (isResizing()) {
          assert DOM.getCaptureElement() != null;
          onSplitterResize(DOM.eventGetClientX(event) - getAbsoluteLeft(),
              DOM.eventGetClientY(event) - getAbsoluteTop());
          DOM.eventPreventDefault(event);
        }
        break;
      }
    }
  }

  public boolean remove(Widget widget) {
    if (widget == null) {
      throw new IllegalArgumentException("Widget must not be null");
    }

    if (widgets[0] == widget) {
      setWidget(0, null);
      return true;
    } else if (widgets[1] == widget) {
      setWidget(1, null);
      return true;
    }

    return false;
  }

  /**
   * Moves the position of the splitter.
   * 
   * @param size the new size of the left region in CSS units (e.g. "10px",
   *          "1em")
   */
  public abstract void setSplitPosition(String size);

  /**
   * Gets the content element for the given index.
   * 
   * @param index the index of the element, only 0 and 1 are valid.
   * @return the element
   */
  protected Element getElement(int index) {
    return elements[index];
  }

  /**
   * Gets the element that is acting as the splitter.
   * 
   * @return the element
   */
  protected Element getSplitElement() {
    return splitElem;
  }

  /**
   * Gets one of the contained widgets.
   * 
   * @param index the index of the widget, only 0 and 1 are valid.
   * @return the widget
   */
  protected Widget getWidget(int index) {
    return widgets[index];
  }

  /**
   * Sets one of the contained widgets.
   * 
   * @param index the index, only 0 and 1 are valid
   * @param w the widget
   */
  protected final void setWidget(int index, Widget w) {
    if (widgets[index] != null) {
      disown(widgets[index]);
    }

    widgets[index] = w;

    if (w != null) {
      adopt(w, elements[index]);
    }
  }

  /**
   * Called on each mouse drag event as the user is dragging the splitter.
   * 
   * @param x the x coordinate of the mouse relative to the panel's extent
   * @param y the y coordinate of the mosue relative to the panel's extent
   */
  abstract void onSplitterResize(int x, int y);

  /**
   * Called when the user starts dragging the splitter.
   * 
   * @param x the x coordinate of the mouse relative to the panel's extent
   * @param y the y coordinate of the mouse relative to the panel's extent
   */
  abstract void onSplitterResizeStarted(int x, int y);

  private void startResizingFrom(int x, int y) {
    isResizing = true;
    onSplitterResizeStarted(x, y);
  }

  private void stopResizing() {
    isResizing = false;
  }
}
