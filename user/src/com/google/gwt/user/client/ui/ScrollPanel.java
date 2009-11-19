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

import com.google.gwt.event.dom.client.HasScrollHandlers;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * A simple panel that wraps its contents in a scrollable area.
 */
@SuppressWarnings("deprecation")
public class ScrollPanel extends SimplePanel implements SourcesScrollEvents,
    HasScrollHandlers, RequiresResize, ProvidesResize {

  private Element containerElem;

  /**
   * Creates an empty scroll panel.
   */
  public ScrollPanel() {
    setAlwaysShowScrollBars(false);

    containerElem = DOM.createDiv();
    getElement().appendChild(containerElem);

    // Prevent IE standard mode bug when a AbsolutePanel is contained.
    DOM.setStyleAttribute(getElement(), "position", "relative");
    DOM.setStyleAttribute(containerElem, "position", "relative");

    // Hack to account for the IE6/7 scrolling bug described here:
    //   http://stackoverflow.com/questions/139000/div-with-overflowauto-and-a-100-wide-table-problem
    DOM.setStyleAttribute(getElement(), "zoom", "1");
    DOM.setStyleAttribute(containerElem, "zoom", "1");
  }

  /**
   * Creates a new scroll panel with the given child widget.
   * 
   * @param child the widget to be wrapped by the scroll panel
   */
  public ScrollPanel(Widget child) {
    this();
    setWidget(child);
  }

  public HandlerRegistration addScrollHandler(ScrollHandler handler) {
    return addDomHandler(handler, ScrollEvent.getType());
  }

  /**
   * @deprecated Use {@link #addScrollHandler} instead
   */
  @Deprecated
  public void addScrollListener(ScrollListener listener) {
    ListenerWrapper.WrappedScrollListener.add(this, listener);
  }

  /**
   * Ensures that the specified item is visible, by adjusting the panel's scroll
   * position.
   * 
   * @param item the item whose visibility is to be ensured
   */
  public void ensureVisible(UIObject item) {
    Element scroll = getElement();
    Element element = item.getElement();
    ensureVisibleImpl(scroll, element);
  }

  /**
   * Gets the horizontal scroll position.
   * 
   * @return the horizontal scroll position, in pixels
   */
  public int getHorizontalScrollPosition() {
    return DOM.getElementPropertyInt(getElement(), "scrollLeft");
  }

  /**
   * Gets the vertical scroll position.
   * 
   * @return the vertical scroll position, in pixels
   */
  public int getScrollPosition() {
    return DOM.getElementPropertyInt(getElement(), "scrollTop");
  }

  public void onResize() {
    Widget child = getWidget();
    if ((child != null) && (child instanceof RequiresResize)) {
      ((RequiresResize) child).onResize();
    }
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by {@link #addScrollHandler} instead
   */
  @Deprecated
  public void removeScrollListener(ScrollListener listener) {
    ListenerWrapper.WrappedScrollListener.remove(this, listener);
  }

  /**
   * Scroll to the bottom of this panel.
   */
  public void scrollToBottom() {
    setScrollPosition(DOM.getElementPropertyInt(getElement(), "scrollHeight"));
  }

  /**
   * Scroll to the far left of this panel.
   */
  public void scrollToLeft() {
    setHorizontalScrollPosition(0);
  }

  /**
   * Scroll to the far right of this panel.
   */
  public void scrollToRight() {
    setHorizontalScrollPosition(DOM.getElementPropertyInt(getElement(),
        "scrollWidth"));
  }

  /**
   * Scroll to the top of this panel.
   */
  public void scrollToTop() {
    setScrollPosition(0);
  }

  /**
   * Sets whether this panel always shows its scroll bars, or only when
   * necessary.
   * 
   * @param alwaysShow <code>true</code> to show scroll bars at all times
   */
  public void setAlwaysShowScrollBars(boolean alwaysShow) {
    DOM.setStyleAttribute(getElement(), "overflow", alwaysShow ? "scroll"
        : "auto");
  }

  /**
   * Sets the object's height. This height does not include decorations such as
   * border, margin, and padding.
   * 
   * @param height the object's new height, in absolute CSS units (e.g. "10px",
   *          "1em" but not "50%")
   */
  @Override
  public void setHeight(String height) {
    super.setHeight(height);
  }

  /**
   * Sets the horizontal scroll position.
   * 
   * @param position the new horizontal scroll position, in pixels
   */
  public void setHorizontalScrollPosition(int position) {
    DOM.setElementPropertyInt(getElement(), "scrollLeft", position);
  }

  /**
   * Sets the vertical scroll position.
   * 
   * @param position the new vertical scroll position, in pixels
   */
  public void setScrollPosition(int position) {
    DOM.setElementPropertyInt(getElement(), "scrollTop", position);
  }

  /**
   * Sets the object's size. This size does not include decorations such as
   * border, margin, and padding.
   * 
   * @param width the object's new width, in absolute CSS units (e.g. "10px",
   *          "1em", but not "50%")
   * @param height the object's new height, in absolute CSS units (e.g. "10px",
   *          "1em", but not "50%")
   */
  @Override
  public void setSize(String width, String height) {
    super.setSize(width, height);
  }

  /**
   * Sets the object's width. This width does not include decorations such as
   * border, margin, and padding.
   * 
   * @param width the object's new width, in absolute CSS units (e.g. "10px",
   *          "1em", but not "50%")
   */
  @Override
  public void setWidth(String width) {
    super.setWidth(width);
  }

  protected Element getContainerElement() {
    return containerElem;
  }

  private native void ensureVisibleImpl(Element scroll, Element e) /*-{
    if (!e)
      return; 

    var item = e;
    var realOffset = 0;
    while (item && (item != scroll)) {
      realOffset += item.offsetTop;
      item = item.offsetParent;
    }

    scroll.scrollTop = realOffset - scroll.offsetHeight / 2;
  }-*/;
}
