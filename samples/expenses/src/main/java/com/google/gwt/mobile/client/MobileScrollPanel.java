/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.mobile.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * A panel implementation that behaves like a
 * {@link com.google.gwt.user.client.ui.ScrollPanel ScrollPanel} by default,
 * but switches to a manual drag-scroll implementation on browsers that support
 * touch events.
 * 
 * TODO(jgw): Implement scroll events.
 * TODO(jgw): This is widgetry that doesn't belong in this package.
 * TODO(jgw): Consider rolling it directly into ScrollPanel. Maybe.
 */
public class MobileScrollPanel extends SimplePanel implements RequiresResize {

  private Element container;
  private Scroller scroller;

  public MobileScrollPanel() {
    container = Document.get().createDivElement();
    getElement().appendChild(container);
    getElement().getStyle().setOverflow(Overflow.AUTO);

    // Only turn on the touch-scroll implementation if we're on a touch device.
    if (TouchHandler.supportsTouch()) {
      scroller = new Scroller(getElement(), container);
      scroller.setMomentum(true);
    }
  }

  public void onResize() {
  }

  /**
   * Sets the horizontal scroll position.
   * 
   * @param left the horizontal scroll position, in pixels
   */
  public void setScrollLeft(int left) {
    if (scroller != null) {
      scroller.setContentOffset(left, scroller.getContentOffsetY());
    } else {
      getElement().setScrollLeft(left);
    }
  }

  /**
   * Sets the vertical scroll position.
   * 
   * @param top the vertical scroll position, in pixels
   */
  public void setScrollTop(int top) {
    if (scroller != null) {
      scroller.setContentOffset(scroller.getContentOffsetX(), top);
    } else {
      getElement().setScrollTop(top);
    }
  }

  @Override
  protected com.google.gwt.user.client.Element getContainerElement() {
    return container.cast();
  }
}
