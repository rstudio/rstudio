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
package com.google.gwt.sample.showcase.client.content.cell;

import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;

/**
 * A custom implementation of
 * {@link com.google.gwt.view.client.PagingListView.Pager} that automatically
 * increases the range every time the scroll bar reaches the bottom.
 *
 * @param <T> the data type of the list view
 */
public class ShowMorePagerPanel<T> extends AbstractPager<T> {

  /**
   * The label that shows the current range.
   */
  private final HTML label = new HTML();

  /**
   * The last scroll position.
   */
  private int lastScrollPos = 0;

  /**
   * Construct a new {@link ShowMorePagerPanel}. Presumably the
   * {@link ScrollPanel} wraps the listView, but it isn't strictly necessary.
   *
   * @param listView the list view to page
   * @param scrollable the {@link ScrollPanel} to respond to
   */
  public ShowMorePagerPanel(
      final PagingListView<T> listView, final ScrollPanel scrollable) {
    super(listView);
    final int initialPageSize = listView.getRange().getLength();
    initWidget(label);
    onRangeOrSizeChanged(listView);

    // Handle scroll events.
    scrollable.addScrollHandler(new ScrollHandler() {
      public void onScroll(ScrollEvent event) {
        // If scrolling up, ignore the event.
        int oldScrollPos = lastScrollPos;
        lastScrollPos = scrollable.getScrollPosition();
        if (oldScrollPos >= lastScrollPos) {
          return;
        }

        int maxScrollTop = scrollable.getWidget().getOffsetHeight()
            - scrollable.getOffsetHeight();
        if (lastScrollPos >= maxScrollTop) {
          // We are near the end, so increase the page size.
          int newPageSize = Math.min(
              listView.getRange().getLength() + initialPageSize,
              listView.getDataSize());
          listView.setRange(0, newPageSize);
        }
      }
    });
  }

  @Override
  public void onRangeOrSizeChanged(PagingListView<T> listView) {
    // Update the label.
    Range range = listView.getRange();
    int start = range.getStart();
    int end = start + range.getLength();
    label.setText(start + " - " + end + " : " + listView.getDataSize());

    super.onRangeOrSizeChanged(listView);
  }
}
