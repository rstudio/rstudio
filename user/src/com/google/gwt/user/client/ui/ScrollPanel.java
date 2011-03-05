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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.touch.client.TouchScroller;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * A simple panel that wraps its contents in a scrollable area.
 */
@SuppressWarnings("deprecation")
public class ScrollPanel extends SimplePanel implements SourcesScrollEvents,
    RequiresResize, ProvidesResize, HasScrolling {

  /**
   * Implementation of this widget.
   */
  static class Impl {
    /**
     * Get the maximum horizontal scroll position.
     * 
     * @param scrollable the scrollable element
     * @return the maximum scroll position
     */
    public int getMaximumHorizontalScrollPosition(Element scrollable) {
      return scrollable.getScrollWidth() - scrollable.getClientWidth();
    }

    /**
     * Get the minimum horizontal scroll position.
     * 
     * @param scrollable the scrollable element
     * @return the minimum scroll position
     */
    public int getMinimumHorizontalScrollPosition(Element scrollable) {
      return 0;
    }
  }

  /**
   * Firefox scrolls in the negative direction in RTL mode.
   */
  static class ImplRtlReversed extends Impl {
    @Override
    public int getMaximumHorizontalScrollPosition(Element scrollable) {
      return isRtl(scrollable) ? 0 : super
          .getMaximumHorizontalScrollPosition(scrollable);
    }

    @Override
    public int getMinimumHorizontalScrollPosition(Element scrollable) {
      return isRtl(scrollable) ? scrollable.getClientWidth()
          - scrollable.getScrollWidth() : 0;
    }

    /**
     * Check if the specified element has an RTL direction. We can't base this
     * on the current locale because the user can modify the direction at the
     * DOM level.
     * 
     * @param scrollable the scrollable element
     * @return true if the direction is RTL, false if LTR
     */
    private native boolean isRtl(Element scrollable) /*-{
      var computedStyle = $doc.defaultView.getComputedStyle(scrollable, null);
      return computedStyle.getPropertyValue('direction') == 'rtl';
    }-*/;
  }

  private static Impl impl;

  private Element containerElem;

  /**
   * The scroller used to support touch events.
   */
  private TouchScroller touchScroller;
  
  /**
   * Creates an empty scroll panel.
   */
  public ScrollPanel() {
    if (impl == null) {
      impl = GWT.create(Impl.class);
    }
    
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

    // Enable touch scrolling.
    setTouchScrollingDisabled(false);
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

  public int getMaximumHorizontalScrollPosition() {
    return impl.getMaximumHorizontalScrollPosition(getElement());
  }

  public int getMaximumVerticalScrollPosition() {
    return getElement().getScrollHeight() - getElement().getClientHeight();
  }

  public int getMinimumHorizontalScrollPosition() {
    return impl.getMinimumHorizontalScrollPosition(getElement());
  }

  public int getMinimumVerticalScrollPosition() {
    return 0;
  }

  /**
   * Gets the vertical scroll position.
   * 
   * @return the vertical scroll position, in pixels
   * @deprecated as of GWT 2.3, replaced by {@link #getVerticalScrollPosition()}
   */
  @Deprecated
  public int getScrollPosition() {
    return DOM.getElementPropertyInt(getElement(), "scrollTop");
  }

  public int getVerticalScrollPosition() {
    return getScrollPosition();
  }

  /**
   * Check whether or not touch based scrolling is disabled. This method always
   * returns false on devices that do not support touch scrolling.
   * 
   * @return true if disabled, false if enabled
   */
  public boolean isTouchScrollingDisabled() {
    return touchScroller == null;
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
   * @deprecated as of GWT 2.3, replaced by
   *             {@link #setVerticalScrollPosition(int)}
   */
  @Deprecated
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
   * Set whether or not touch scrolling is disabled. By default, touch scrolling
   * is enabled on devices that support touch events.
   * 
   * @param isDisabled true to disable, false to enable
   * @return true if touch scrolling is enabled and supported, false if disabled
   *         or not supported
   */
  public boolean setTouchScrollingDisabled(boolean isDisabled) {
    if (isDisabled == isTouchScrollingDisabled()) {
      return isDisabled;
    }

    if (isDisabled) {
      // Detach the touch scroller.
      touchScroller.setTargetWidget(null);
      touchScroller = null;
    } else {
      // Attach a new touch scroller.
      touchScroller = TouchScroller.createIfSupported(this);
    }
    return isTouchScrollingDisabled();
  }

  public void setVerticalScrollPosition(int position) {
    setScrollPosition(position);
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

  @Override
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
