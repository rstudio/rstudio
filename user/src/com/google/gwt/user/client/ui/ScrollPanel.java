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
import com.google.gwt.user.client.Event;

/**
 * A simple panel that wraps its contents in a scrollable area.
 */
public class ScrollPanel extends SimplePanel implements SourcesScrollEvents {

  private ScrollListenerCollection scrollListeners;

  /**
   * Creates an empty scroll panel.
   */
  public ScrollPanel() {
    setAlwaysShowScrollBars(false);
    sinkEvents(Event.ONSCROLL);
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

  public void addScrollListener(ScrollListener listener) {
    if (scrollListeners == null) {
      scrollListeners = new ScrollListenerCollection();
    }
    scrollListeners.add(listener);
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
    return DOM.getIntAttribute(getElement(), "scrollLeft");
  }

  /**
   * Gets the vertical scroll position.
   * 
   * @return the vertical scroll position, in pixels
   */
  public int getScrollPosition() {
    return DOM.getIntAttribute(getElement(), "scrollTop");
  }

  public void onBrowserEvent(Event event) {
    if (DOM.eventGetType(event) == Event.ONSCROLL) {
      if (scrollListeners != null) {
        scrollListeners.fireScroll(this, getHorizontalScrollPosition(),
            getScrollPosition());
      }
    }
  }

  public void removeScrollListener(ScrollListener listener) {
    if (scrollListeners != null) {
      scrollListeners.remove(listener);
    }
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
   * Sets the horizontal scroll position.
   * 
   * @param position the new horizontal scroll position, in pixels
   */
  public void setHorizontalScrollPosition(int position) {
    DOM.setIntAttribute(getElement(), "scrollLeft", position);
  }

  /**
   * Sets the vertical scroll position.
   * 
   * @param position the new vertical scroll position, in pixels
   */
  public void setScrollPosition(int position) {
    DOM.setIntAttribute(getElement(), "scrollTop", position);
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
