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
package com.google.gwt.user.cellview.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.PagingListView.Pager;

/**
 * A simple {@link Pager} that controls the page size.
 * 
 * @param <T> the data type of list items
 */
public class PageSizePager<T> extends Composite implements Pager<T> {

  /**
   * The increment by which to grow or shrink the page size.
   */
  private final int increment;

  /**
   * The main layout widget.
   */
  private final FlexTable layout = new FlexTable();

  // TODO(jlabanca): I18N button text.
  private final Anchor showMoreButton = new Anchor("Show More");
  private final Anchor showLessButton = new Anchor("Show Less");

  @UiConstructor
  public PageSizePager(final PagingListView<T> listView, final int increment) {
    this.increment = increment;
    initWidget(layout);
    layout.setCellPadding(0);
    layout.setCellSpacing(0);

    // Show more button.
    showMoreButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Range range = listView.getRange();
        int pageSize = Math.min(range.getLength() + increment,
            listView.getDataSize());
        listView.setRange(range.getStart(), pageSize);
      }
    });
    showLessButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Range range = listView.getRange();
        int pageSize = Math.max(range.getLength() - increment, increment);
        listView.setRange(range.getStart(), pageSize);
      }
    });

    // Add the buttons to the pager.
    layout.setWidget(0, 0, showLessButton);
    layout.setText(0, 1, " | ");
    layout.setWidget(0, 2, showMoreButton);

    // Update the button state.
    listView.setPager(this);
    onRangeOrSizeChanged(listView);
  }

  public void onRangeOrSizeChanged(PagingListView<T> listView) {
    // Assumes a page start index of 0.
    int pageSize = listView.getRange().getLength();
    boolean hasLess = pageSize > increment;
    boolean hasMore = pageSize < listView.getDataSize();
    showLessButton.setVisible(hasLess);
    showMoreButton.setVisible(hasMore);
    layout.setText(0, 1, (hasLess && hasMore) ? " | " : "");
  }
}
