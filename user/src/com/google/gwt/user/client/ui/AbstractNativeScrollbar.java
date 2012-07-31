/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.HasScrollHandlers;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

/**
 * Abstract parent class for scrollbars implemented using the native browser
 * scrollbars.
 */
public abstract class AbstractNativeScrollbar extends Widget implements HasScrollHandlers {

  private static int nativeHeight = -1;
  private static int nativeWidth = -1;
  private static boolean nativeRtl = false;

  /**
   * Get the height of a native horizontal scrollbar.
   * 
   * <p>
   * This method assumes that all native scrollbars on the page have the same
   * height.
   * 
   * @return the height in pixels
   */
  public static int getNativeScrollbarHeight() {
    maybeRecalculateNativeScrollbarSize();
    return nativeHeight;
  }

  /**
   * Get the width of a native vertical scrollbar.
   * 
   * <p>
   * This method assumes that all native vertical scrollbars on the page have
   * the same width.
   * 
   * @return the height in pixels
   */
  public static int getNativeScrollbarWidth() {
    maybeRecalculateNativeScrollbarSize();
    return nativeWidth;
  }

  /**
   * Check whether or not the native vertical scrollbar is aligned on the left
   * side of the scrollable element in RTL mode.
   * 
   * @return true if left aligned, false if not
   */
  public static boolean isScrollbarLeftAlignedInRtl() {
    maybeRecalculateNativeScrollbarSize();
    return nativeRtl;
  }

  /**
   * Recalculate the height and width of a native scrollbar.
   */
  private static void maybeRecalculateNativeScrollbarSize() {
    // Check if the size has already been calculated.
    if (nativeHeight > -1) {
      return;
    }

    // Create a scrollable element and attach it to the body.
    Element scrollable = Document.get().createDivElement();
    scrollable.getStyle().setPosition(Position.ABSOLUTE);
    scrollable.getStyle().setTop(-1000.0, Unit.PX);
    scrollable.getStyle().setLeft(-1000.0, Unit.PX);
    scrollable.getStyle().setHeight(100.0, Unit.PX);
    scrollable.getStyle().setWidth(100.0, Unit.PX);
    scrollable.getStyle().setOverflow(Overflow.SCROLL);
    scrollable.getStyle().setProperty("direction", "rtl");
    Document.get().getBody().appendChild(scrollable);

    // Add some content.
    Element content = Document.get().createDivElement();
    content.setInnerText("content");
    scrollable.appendChild(content);

    // Measure the height and width.
    nativeHeight = scrollable.getOffsetHeight() - scrollable.getClientHeight();
    nativeWidth = scrollable.getOffsetWidth() - scrollable.getClientWidth();
    nativeRtl = (content.getAbsoluteLeft() > scrollable.getAbsoluteLeft());

    // Detach the scrollable element.
    scrollable.removeFromParent();
  }

  public HandlerRegistration addScrollHandler(ScrollHandler handler) {
    // Sink the event on the scrollable element, not the root element.
    Event.sinkEvents(getScrollableElement(), Event.ONSCROLL);
    return addHandler(handler, ScrollEvent.getType());
  }

  /**
   * Get the scrollable element.
   * 
   * @return the scrollable element
   */
  protected abstract Element getScrollableElement();

  @Override
  protected void onAttach() {
    super.onAttach();

    /*
     * Attach the event listener in onAttach instead of onLoad so users cannot
     * accidentally override it.
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
}
