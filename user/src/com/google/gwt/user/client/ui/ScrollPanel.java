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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.touch.client.TouchScroller;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * A simple panel that wraps its contents in a scrollable area.
 */
@SuppressWarnings("deprecation")
public class ScrollPanel extends SimplePanel implements SourcesScrollEvents,
    RequiresResize, ProvidesResize, HasScrolling {

  private final Element containerElem;
  private final Element scrollableElem;

  /**
   * The scroller used to support touch events.
   */
  private TouchScroller touchScroller;

  /**
   * Creates an empty scroll panel.
   */
  public ScrollPanel() {
    this.scrollableElem = getElement();
    this.containerElem = Document.get().createDivElement().cast();
    scrollableElem.appendChild(containerElem);
    initialize();
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

  /**
   * Creates an empty scroll panel using the specified root, scrollable, and
   * container elements.
   * 
   * @param root the root element of the Widget
   * @param scrollable the scrollable element, which can be the same as the root
   *          element
   * @param container the container element that holds the child
   */
  protected ScrollPanel(Element root, Element scrollable, Element container) {
    super(root);
    this.scrollableElem = scrollable;
    this.containerElem = container;
    initialize();
  }

  public HandlerRegistration addScrollHandler(ScrollHandler handler) {
    /*
     * Sink the event on the scrollable element, which may not be the root
     * element.
     */
    Event.sinkEvents(getScrollableElement(), Event.ONSCROLL);
    return addHandler(handler, ScrollEvent.getType());
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
    Element scroll = getScrollableElement();
    Element element = item.getElement();
    ensureVisibleImpl(scroll, element);
  }

  /**
   * Gets the horizontal scroll position.
   * 
   * @return the horizontal scroll position, in pixels
   */
  public int getHorizontalScrollPosition() {
    return getScrollableElement().getScrollLeft();
  }

  public int getMaximumHorizontalScrollPosition() {
    return ScrollImpl.get().getMaximumHorizontalScrollPosition(getScrollableElement());
  }

  public int getMaximumVerticalScrollPosition() {
    return getScrollableElement().getScrollHeight() - getScrollableElement().getClientHeight();
  }

  public int getMinimumHorizontalScrollPosition() {
    return ScrollImpl.get().getMinimumHorizontalScrollPosition(getScrollableElement());
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
    return getScrollableElement().getScrollTop();
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
    setVerticalScrollPosition(getMaximumVerticalScrollPosition());
  }

  /**
   * Scroll to the far left of this panel.
   */
  public void scrollToLeft() {
    setHorizontalScrollPosition(getMinimumHorizontalScrollPosition());
  }

  /**
   * Scroll to the far right of this panel.
   */
  public void scrollToRight() {
    setHorizontalScrollPosition(getMaximumHorizontalScrollPosition());
  }

  /**
   * Scroll to the top of this panel.
   */
  public void scrollToTop() {
    setVerticalScrollPosition(getMinimumVerticalScrollPosition());
  }

  /**
   * Sets whether this panel always shows its scroll bars, or only when
   * necessary.
   * 
   * @param alwaysShow <code>true</code> to show scroll bars at all times
   */
  public void setAlwaysShowScrollBars(boolean alwaysShow) {
    getScrollableElement().getStyle().setOverflow(alwaysShow ? Overflow.SCROLL : Overflow.AUTO);
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
    getScrollableElement().setScrollLeft(position);
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
    getScrollableElement().setScrollTop(position);
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

  /**
   * Get the scrollable element. That is the element with its overflow set to
   * 'auto' or 'scroll'.
   * 
   * @return the scrollable element
   */
  protected Element getScrollableElement() {
    return scrollableElem;
  }

  @Override
  protected void onAttach() {
    super.onAttach();

    /*
     * Attach the event listener in onAttach instead of onLoad so users cannot
     * accidentally override it. If the scrollable element is the same as the
     * root element, then we set the event listener twice (once in
     * super.onAttach() and once here), which is fine.
     */
    Event.setEventListener(getScrollableElement(), this);
  }

  @Override
  protected void onDetach() {
    /*
     * Detach the event listener in onDetach instead of onUnload so users cannot
     * accidentally override it.
     */
    Event.setEventListener(getScrollableElement(), null);

    super.onDetach();
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

  /**
   * Initialize the widget.
   */
  private void initialize() {
    setAlwaysShowScrollBars(false);

    // Prevent IE standard mode bug when a AbsolutePanel is contained.
    scrollableElem.getStyle().setPosition(Position.RELATIVE);
    containerElem.getStyle().setPosition(Position.RELATIVE);

    // Hack to account for the IE6/7 scrolling bug described here:
    //   http://stackoverflow.com/questions/139000/div-with-overflowauto-and-a-100-wide-table-problem
    scrollableElem.getStyle().setProperty("zoom", "1");
    containerElem.getStyle().setProperty("zoom", "1");

    // Enable touch scrolling.
    setTouchScrollingDisabled(false);

    // Initialize the scrollable element.
    ScrollImpl.get().initialize(scrollableElem, containerElem);
  }
}
