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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.HasRows;

/**
 * A pager for controlling a {@link CellTable} that uses a native scrollbar.
 */
public class ScrollbarPager extends AbstractPager {

  /**
   * Number of units advanced by clicking the down arrow on the scrollbar.
   */
  private final int jump;
  private final ScrollPanel panel;
  private final HTML spacer = new HTML();
  private CellTable<?> table;

  public ScrollbarPager() {
    this.jump = getScrollPageAmount();
    this.panel = new ScrollPanel();

    panel.setPixelSize(50, 50);
    DOM.setStyleAttribute(panel.getElement(), "overflow", "auto");

    VerticalPanel p = new VerticalPanel();
    p.add(spacer);
    p.add(panel);

    initWidget(p);

    panel.addScrollHandler(new ScrollHandler() {
      public void onScroll(ScrollEvent event) {
        if (table != null) {
          int position = panel.getScrollPosition() / jump;
          setPageStart(position);
        }
      }
    });
  }

  @Override
  public void setDisplay(HasRows view) {
    // TODO(jlabanca): ScrollbarPager should work with any HasRows.
    throw new UnsupportedOperationException(
        "view must be an instance of CellTable.");
  }

  public void setView(CellTable<?> view) {
    table = view;
    super.setDisplay(view);
  }

  @Override
  protected void onRangeOrRowCountChanged() {
    int pageSize = table.getVisibleRange().getLength();
    int dataSize = table.getRowCount();

    int height = table.getBodyHeight();
    int spaceAbove = table.getHeaderHeight();
    spacer.getElement().getStyle().setHeight(spaceAbove, Unit.PX);
    panel.setSize("50px", "" + height + "px");

    int h = Math.max(height + 1, jump * (dataSize - pageSize) + height);
    panel.setWidget(
        new HTML("<div id='scroll-contents' style='width: 1px; height: " + h
            + "px;'></div>"));
  }

  /**
   * Returns the number of units of scrolling caused by clicking on the
   * scrollbar's up/down buttons, based on the browser user agent.
   */
  private native int getScrollPageAmount() /*-{
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf("webkit") != -1) {
      return 40;
    }
    if (ua.indexOf("gecko") != -1) {
      return 14;
    }
    return 40; // not tested
  }-*/;
}
