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
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.Range;

/**
 * A simple pager that controls the page size.
 */
public class PageSizePager extends AbstractPager {

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
  public PageSizePager(final int increment) {
    this.increment = increment;
    initWidget(layout);
    layout.setCellPadding(0);
    layout.setCellSpacing(0);

    // Show more button.
    showMoreButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // view should be non-null, but we check defensively.
        HasRows view = getView();
        if (view != null) {
          Range range = view.getVisibleRange();
          int pageSize = Math.min(range.getLength() + increment,
              view.getRowCount() + (view.isRowCountExact() ? 0 : increment));
          view.setVisibleRange(range.getStart(), pageSize);
        }
      }
    });
    showLessButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // view should be non-null, but we check defensively.
        HasRows view = getView();
        if (view != null) {
          Range range = view.getVisibleRange();
          int pageSize = Math.max(range.getLength() - increment, increment);
          view.setVisibleRange(range.getStart(), pageSize);
        }
      }
    });

    // Add the buttons to the pager.
    layout.setWidget(0, 0, showLessButton);
    layout.setText(0, 1, " | ");
    layout.setWidget(0, 2, showMoreButton);

    // Hide the buttons by default.
    setView(null);
  }

  @Override
  public void setPageSize(int pageSize) {
    super.setPageSize(pageSize);
  }

  @Override
  public void setView(HasRows view) {
    // Hide the buttons if the view is null. If the view is non-null, the
    // buttons will be displayed in onRangeOrRowCountChanged().
    if (view == null) {
      showLessButton.setVisible(false);
      showMoreButton.setVisible(false);
    }
    super.setView(view);
  }

  @Override
  protected void onRangeOrRowCountChanged() {
    // Assumes a page start index of 0.
    HasRows view = getView();
    int pageSize = view.getVisibleRange().getLength();
    boolean hasLess = pageSize > increment;
    boolean hasMore = !view.isRowCountExact() || pageSize < view.getRowCount();
    showLessButton.setVisible(hasLess);
    showMoreButton.setVisible(hasMore);
    layout.setText(0, 1, (hasLess && hasMore) ? " | " : "");
  }

  /**
   * Visible for testing.
   */
  boolean isShowLessButtonVisible() {
    return showLessButton.isVisible();
  }

  /**
   * Visible for testing.
   */
  boolean isShowMoreButtonVisible() {
    return showMoreButton.isVisible();
  }
}
