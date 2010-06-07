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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

import java.util.Iterator;

/**
 * Abstract base class for {@link HorizontalSplitPanel} and
 * {@link VerticalSplitPanel}.
 * 
 * @deprecated Use {@link SplitLayoutPanel} instead, but understand that it is
 *             not a drop in replacement for this class. It requires standards
 *             mode, and is most easily used under a {@link RootLayoutPanel} (as
 *             opposed to a {@link RootPanel}
 */
@Deprecated
abstract class SplitPanel extends Panel {
  /**
   * The element that masks the screen so we can catch mouse events over
   * iframes.
   */
  private static Element glassElem = null;

  /**
   * Sets an elements positioning to absolute.
   * 
   * @param elem the element
   */
  static void addAbsolutePositoning(Element elem) {
    DOM.setStyleAttribute(elem, "position", "absolute");
  }

  /**
   * Adds clipping to an element.
   * 
   * @param elem the element
   */
  static final void addClipping(final Element elem) {
    DOM.setStyleAttribute(elem, "overflow", "hidden");
  }

  /**
   * Adds as-needed scrolling to an element.
   * 
   * @param elem the element
   */
  static final void addScrolling(final Element elem) {
    DOM.setStyleAttribute(elem, "overflow", "auto");
  }

  /**
   * Sizes and element to consume the full area of its parent using the CSS
   * properties left, right, top, and bottom. This method is used for all
   * browsers except IE6/7.
   * 
   * @param elem the element
   */
  static final void expandToFitParentUsingCssOffsets(Element elem) {
    final String zeroSize = "0px";

    addAbsolutePositoning(elem);
    setLeft(elem, zeroSize);
    setRight(elem, zeroSize);
    setTop(elem, zeroSize);
    setBottom(elem, zeroSize);
  }

  /**
   * Sizes an element to consume the full areas of its parent using 100% width
   * and height. This method is used on IE6/7 where CSS offsets don't work
   * reliably.
   * 
   * @param elem the element
   */
  static final void expandToFitParentUsingPercentages(Element elem) {
    final String zeroSize = "0px";
    final String fullSize = "100%";

    addAbsolutePositoning(elem);
    setTop(elem, zeroSize);
    setLeft(elem, zeroSize);
    setWidth(elem, fullSize);
    setHeight(elem, fullSize);
  }

  /**
   * Returns the offsetHeight element property.
   * 
   * @param elem the element
   * @return the offsetHeight property
   */
  static final int getOffsetHeight(Element elem) {
    return DOM.getElementPropertyInt(elem, "offsetHeight");
  }

  /**
   * Returns the offsetWidth element property.
   * 
   * @param elem the element
   * @return the offsetWidth property
   */
  static final int getOffsetWidth(Element elem) {
    return DOM.getElementPropertyInt(elem, "offsetWidth");
  }

  /**
   * Adds zero or none CSS values for padding, margin and border to prevent
   * stylesheet overrides. Returns the element for convenience to support
   * builder pattern.
   * 
   * @param elem the element
   * @return the element
   */
  static final Element preventBoxStyles(final Element elem) {
    DOM.setIntStyleAttribute(elem, "padding", 0);
    DOM.setIntStyleAttribute(elem, "margin", 0);
    DOM.setStyleAttribute(elem, "border", "none");
    return elem;
  }

  /**
   * Convenience method to set bottom offset of an element.
   * 
   * @param elem the element
   * @param size a CSS length value for bottom
   */
  static void setBottom(Element elem, String size) {
    DOM.setStyleAttribute(elem, "bottom", size);
  }

  /**
   * Sets the elements css class name.
   * 
   * @param elem the element
   * @param className the class name
   */
  static final void setClassname(final Element elem, final String className) {
    DOM.setElementProperty(elem, "className", className);
  }

  /**
   * Convenience method to set the height of an element.
   * 
   * @param elem the element
   * @param height a CSS length value for the height
   */
  static final void setHeight(Element elem, String height) {
    DOM.setStyleAttribute(elem, "height", height);
  }

  /**
   * Convenience method to set the left offset of an element.
   * 
   * @param elem the element
   * @param left a CSS length value for left
   */
  static final void setLeft(Element elem, String left) {
    DOM.setStyleAttribute(elem, "left", left);
  }

  /**
   * Convenience method to set the right offset of an element.
   * 
   * @param elem the element
   * @param right a CSS length value for right
   */
  static final void setRight(Element elem, String right) {
    DOM.setStyleAttribute(elem, "right", right);
  }

  /**
   * Convenience method to set the top offset of an element.
   * 
   * @param elem the element
   * @param top a CSS length value for top
   */
  static final void setTop(Element elem, String top) {
    DOM.setStyleAttribute(elem, "top", top);
  }

  /**
   * Convenience method to set the width of an element.
   * 
   * @param elem the element
   * @param width a CSS length value for the width
   */
  static final void setWidth(Element elem, String width) {
    DOM.setStyleAttribute(elem, "width", width);
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
    sinkEvents(Event.MOUSEEVENTS | Event.ONLOSECAPTURE);

    if (glassElem == null) {
      glassElem = DOM.createDiv();
      glassElem.getStyle().setProperty("position", "absolute");
      glassElem.getStyle().setProperty("top", "0px");
      glassElem.getStyle().setProperty("left", "0px");
      glassElem.getStyle().setProperty("margin", "0px");
      glassElem.getStyle().setProperty("padding", "0px");
      glassElem.getStyle().setProperty("border", "0px");

      // We need to set the background color or mouse events will go right
      // through the glassElem. If the SplitPanel contains an iframe, the
      // iframe will capture the event and the slider will stop moving.
      glassElem.getStyle().setProperty("background", "white");
      glassElem.getStyle().setProperty("opacity", "0.0");
      glassElem.getStyle().setProperty("filter", "alpha(opacity=0)");
    }
  }

  @Override
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

  public Iterator<Widget> iterator() {
    return WidgetIterators.createWidgetIterator(this, widgets);
  }

  @Override
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
        if (isResizing()) {
          // The order of these two lines is important. If we release capture
          // first, then we might trigger an onLoseCapture event before we set
          // isResizing to false.
          stopResizing();
          DOM.releaseCapture(getElement());
        }
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

      // IE automatically releases capture if the user switches windows, so we
      // need to catch the event and stop resizing.
      case Event.ONLOSECAPTURE: {
        if (isResizing()) {
          stopResizing();
        }
        break;
      }
    }
    super.onBrowserEvent(event);
  }

  @Override
  public boolean remove(Widget widget) {
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
   *            "1em")
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
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-splitter = the container containing the splitter element.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    ensureDebugId(splitElem, baseID, "splitter");
  }

  /**
   * Sets one of the contained widgets.
   * 
   * @param index the index, only 0 and 1 are valid
   * @param w the widget
   */
  protected final void setWidget(int index, Widget w) {
    Widget oldWidget = widgets[index];

    // Validate.
    if (oldWidget == w) {
      return;
    }

    // Detach the new child.
    if (w != null) {
      w.removeFromParent();
    }

    // Remove the old child.
    if (oldWidget != null) {
      // Orphan old.
      try {
        orphan(oldWidget);
      } finally {
        // Physical detach old.
        DOM.removeChild(elements[index], oldWidget.getElement());
        widgets[index] = null;
      }
    }

    // Logical attach new.
    widgets[index] = w;

    if (w != null) {
      // Physical attach new.
      DOM.appendChild(elements[index], w.getElement());

      // Adopt new.
      adopt(w);
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

    // Resize glassElem to take up the entire scrollable window area
    int height = RootPanel.getBodyElement().getScrollHeight() - 1;
    int width = RootPanel.getBodyElement().getScrollWidth() - 1;
    glassElem.getStyle().setProperty("height", height + "px");
    glassElem.getStyle().setProperty("width", width + "px");
    RootPanel.getBodyElement().appendChild(glassElem);
  }

  private void stopResizing() {
    isResizing = false;
    RootPanel.getBodyElement().removeChild(glassElem);
  }
}
