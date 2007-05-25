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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;

/**
 * A panel that arranges two widgets in a single vertical column and allows the
 * user to interactively change the proportion of the height dedicated to each
 * of the two widgets. Widgets contained within a
 * <code>VerticalSplitterPanel</code> will be automatically decorated with
 * scrollbars when neccessary.
 *
 * <p>
 * <img class='gallery' src='VerticalSplitPanel.png'/>
 * </p>
 *
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-VerticalSplitPanel { the panel itself }</li>
 * <li>.gwt-VerticalSplitPanel top { the top container }</li>
 * <li>.gwt-VerticalSplitPanel bottom { the bottom container }</li>
 * <li>.gwt-VerticalSplitPanel splitter { the splitter }</li>
 * </ul>
 */
public final class VerticalSplitPanel extends SplitPanel {

  private static final int TOP = 0;
  private static final int BOTTOM = 1;
  private static final Impl impl = (Impl) GWT.create(Impl.class);

  /**
   * Provides different implementations for retrieving an element's height. The
   * default binding is based on DOM1 clientHeight.
   */
  private static class Impl {
    /**
     * Gets an element's height.
     *
     * @param elem an element
     * @return the height of the element
     */
    protected int getElementHeight(Element elem) {
      return DOM.getElementPropertyInt(elem, "clientHeight");
    }
  }

  /**
   * Provides an implementation for IE6 based on Element.getBoundingClientRect.
   */
  private static class ImplIE6 extends Impl {
    protected native int getElementHeight(Element elem) /*-{
      var box = elem.getBoundingClientRect();
      return box.bottom - box.top;
    }-*/;
  }

  private static int getOffsetTop(Element elem) {
    return DOM.getElementPropertyInt(elem, "offsetTop");
  }

  private static Element lockStyles(final Element elem) {
    DOM.setIntStyleAttribute(elem, "height", 0);
    return preventElementBoxStyles(elem);
  }

  private static void setHeight(Element elem, int px) {
    DOM.setStyleAttribute(elem, "height", Math.max(0, px) + "px");
  }

  // Element is added below bottom container element to make it possible to
  // infer the bottom element's height.
  private final Element probeElem;

  // Captures the height of the top container when drag resizing starts.
  private int initialTopHeight = 0;

  // Captures the offset of a user's mouse pointer during drag resizing.
  private int initialThumbPos = 0;

  /**
   * Creates an empty vertical split panel.
   */
  public VerticalSplitPanel() {
    super(DOM.createDiv(), DOM.createDiv(), DOM.createDiv(), DOM.createDiv());

    final Element thisElem = getElement();
    final Element splitElem = getSplitElement();
    final Element topElem = getElement(TOP);
    final Element bottomElem = getElement(BOTTOM);
    probeElem = lockStyles(DOM.createDiv());

    DOM.appendChild(thisElem, topElem);
    DOM.appendChild(thisElem, splitElem);
    DOM.appendChild(thisElem, bottomElem);
    DOM.appendChild(thisElem, probeElem);

    addElementClipping(thisElem);
    addElementScrolling(topElem);
    addElementScrolling(bottomElem);

    // Prevent padding on container elements.
    preventElementPadding(thisElem);
    preventElementPadding(topElem);
    preventElementPadding(bottomElem);

    setElementClassname(topElem, "top");
    setElementClassname(splitElem, "splitter");
    setElementClassname(bottomElem, "bottom");

    setStyleName("gwt-VerticalSplitPanel");

    // Must wait on layout to do the initial layout.
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        updateBottomHeight();
      }
    });
  }

  /**
   * Gets the widget in the bottom of the panel.
   *
   * @return the widget, <code>null</code> if there is not one
   */
  public final Widget getBottomWidget() {
    return getWidget(BOTTOM);
  }

  /**
   * Gets the widget in the top of the panel.
   *
   * @return the widget, <code>null</code> if there is not one
   */
  public final Widget getTopWidget() {
    return getWidget(TOP);
  }

  /**
   * Sets the widget in the bottom of the panel.
   *
   * @param w the widget
   */
  public final void setBottomWidget(Widget w) {
    setWidget(BOTTOM, w);
  }

  public final void setSplitPosition(String size) {
    DOM.setStyleAttribute(getElement(TOP), "height", size);
    updateBottomHeight();
  }

  /**
   * Sets the widget in the top of the panel.
   *
   * @param w the widget
   */
  public final void setTopWidget(Widget w) {
    setWidget(TOP, w);
  }

  final void onSplitterResize(int x, int y) {
    /*
     * When dragging starts we record the thumb position and the current height
     * of the top div. On each subsequent resize event, we compute how far the
     * thumb has moved and adjust the top and bottom div by that offset.
     */
    final Element topElem = getElement(TOP);
    final Element botElem = getElement(BOTTOM);

    // Compute what the new top height should be.
    final int newTopHeight = initialTopHeight + (y - initialThumbPos);
    final int newBotHeight = impl.getElementHeight(botElem)
        + impl.getElementHeight(topElem) - newTopHeight;

    /*
     * NOTE: The bottom must be adjusted before the top due to FF bug which
     * leaves scrollbar artifacts in the overflow region.
     * https://bugzilla.mozilla.org/show_bug.cgi?id=368190
     */
    if (newBotHeight < 0) {
      setHeight(botElem, 0);
      setHeight(topElem, newTopHeight + newBotHeight);
    } else {
      setHeight(botElem, newBotHeight);
      setHeight(topElem, newTopHeight);
    }

    updateBottomHeight();
  }

  final void onSplitterResizeStarted(int x, int y) {
    initialThumbPos = y;
    initialTopHeight = impl.getElementHeight(getElement(TOP));
  }

  /**
   * Updates to the height on the bottom div so that it remains within the outer
   * container.
   */
  private void updateBottomHeight() {
    final Element thisElem = getElement();
    final Element bottomElem = getElement(BOTTOM);

    /*
     * This is the definitive check that tells us how far (in pixels) the height
     * of the bottom div must change. We do this by comparing the clientHeight
     * of the root div with the offsetTop of a probe div under the bottom div.
     */
    final int adjust = impl.getElementHeight(thisElem)
        - (getOffsetTop(probeElem) - getOffsetTop(thisElem));

    /*
     * In the case where the user is dragging the splitter, resizeTopBy should
     * generally guess the right adjustment based on how far the top div was
     * adjusted. So for the most common case, we find we do not need adjustment
     * and exit here.
     */
    if (adjust == 0) {
      return;
    }

    /*
     * We don't know what margins and borders are in play on the bottom div, so
     * we naively guess they are all zero, which would mean that the CSS height
     * property will be equal to the clientHeight attribute. After we set the
     * height in css, we take the difference between what we set and the
     * reported clientHeight. If that is non-zero, it tells us how much to
     * accomodate for margin, border and what not.
     */
    final int curHeight = impl.getElementHeight(bottomElem);
    final int newHeight = curHeight + adjust;
    setHeight(bottomElem, newHeight);
    final int error = impl.getElementHeight(bottomElem) - newHeight;

    if (error == 0) {
      return;
    }

    setHeight(bottomElem, newHeight - error);
  }
}
