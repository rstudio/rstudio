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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.Range;

/**
 * A pager for controlling a {@link HasRows} that only supports simple page
 * navigation.
 */
public class SimplePager extends AbstractPager {

  /**
   * A ClientBundle that provides images for this widget.
   */
  public static interface Resources extends ClientBundle {

    /**
     * The image used to skip ahead multiple pages.
     */
    ImageResource simplePagerFastForward();

    /**
     * The disabled "fast forward" image.
     */
    ImageResource simplePagerFastForwardDisabled();

    /**
     * The image used to go to the first page.
     */
    ImageResource simplePagerFirstPage();

    /**
     * The disabled first page image.
     */
    ImageResource simplePagerFirstPageDisabled();

    /**
     * The image used to go to the last page.
     */
    ImageResource simplePagerLastPage();

    /**
     * The disabled last page image.
     */
    ImageResource simplePagerLastPageDisabled();

    /**
     * The image used to go to the next page.
     */
    ImageResource simplePagerNextPage();

    /**
     * The disabled next page image.
     */
    ImageResource simplePagerNextPageDisabled();

    /**
     * The image used to go to the previous page.
     */
    ImageResource simplePagerPreviousPage();

    /**
     * The disabled previous page image.
     */
    ImageResource simplePagerPreviousPageDisabled();

    /**
     * The styles used in this widget.
     */
    @Source("SimplePager.css")
    Style simplePagerStyle();
  }

  /**
   * Styles used by this widget.
   */
  public static interface Style extends CssResource {

    /**
     * Applied to buttons.
     */
    String button();

    /**
     * Applied to disabled buttons.
     */
    String disabledButton();

    /**
     * Applied to the details text.
     */
    String pageDetails();
  }

  /**
   * The location of the text relative to the paging buttons.
   */
  public static enum TextLocation {
    CENTER, LEFT, RIGHT;
  }

  private static int DEFAULT_FAST_FORWARD_ROWS = 1000;
  private static Resources DEFAULT_RESOURCES;

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  private final Image fastForward;

  private final int fastForwardRows;

  private final Image firstPage;

  /**
   * We use an {@link HTML} so we can embed the loading image.
   */
  private final HTML label = new HTML();

  private final Image lastPage;

  /**
   * Set to true when the next and last buttons are disabled.
   */
  private boolean nextDisabled;

  private final Image nextPage;

  /**
   * Set to true when the prev and first buttons are disabled.
   */
  private boolean prevDisabled;

  private final Image prevPage;

  /**
   * The {@link Resources} used by this widget.
   */
  private final Resources resources;

  /**
   * The {@link Style} used by this widget.
   */
  private final Style style;

  /**
   * Construct a {@link SimplePager} with the default text location.
   */
  public SimplePager() {
    this(TextLocation.CENTER);
  }

  /**
   * Construct a {@link SimplePager} with the specified text location.
   *
   * @param location the location of the text relative to the buttons
   */
  @UiConstructor
  // Hack for Google I/O demo
  public SimplePager(TextLocation location) {
    this(location, getDefaultResources(), true, DEFAULT_FAST_FORWARD_ROWS,
        false);
  }

  /**
   * Construct a {@link SimplePager} with the specified resources.
   *
   * @param location the location of the text relative to the buttons
   * @param resources the {@link Resources} to use
   * @param showFastForwardButton if true, show a fast-forward button that
   *          advances by a larger increment than a single page
   * @param fastForwardRows the number of rows to jump when fast forwarding
   * @param showLastPageButton if true, show a button to go the the last page
   */
  public SimplePager(TextLocation location, Resources resources,
      boolean showFastForwardButton, final int fastForwardRows,
      boolean showLastPageButton) {
    this.resources = resources;
    this.fastForwardRows = fastForwardRows;
    this.style = resources.simplePagerStyle();
    this.style.ensureInjected();

    // Create the buttons.
    firstPage = new Image(resources.simplePagerFirstPage());
    firstPage.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        firstPage();
      }
    });
    nextPage = new Image(resources.simplePagerNextPage());
    nextPage.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        nextPage();
      }
    });
    prevPage = new Image(resources.simplePagerPreviousPage());
    prevPage.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        previousPage();
      }
    });
    if (showLastPageButton) {
      lastPage = new Image(resources.simplePagerLastPage());
      lastPage.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          lastPage();
        }
      });
    } else {
      lastPage = null;
    }
    if (showFastForwardButton) {
      fastForward = new Image(resources.simplePagerFastForward());
      fastForward.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          setPage(getPage() + getFastForwardPages());
        }
      });
    } else {
      fastForward = null;
    }

    // Construct the widget.
    HorizontalPanel layout = new HorizontalPanel();
    layout.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    initWidget(layout);
    if (location == TextLocation.RIGHT) {
      layout.add(label);
    }
    layout.add(firstPage);
    layout.add(prevPage);
    if (location == TextLocation.CENTER) {
      layout.add(label);
    }
    layout.add(nextPage);
    if (showFastForwardButton) {
      layout.add(fastForward);
    }
    if (showLastPageButton) {
      layout.add(lastPage);
    }
    if (location == TextLocation.LEFT) {
      layout.add(label);
    }

    // Add style names to the cells.
    firstPage.getElement().getParentElement().addClassName(style.button());
    prevPage.getElement().getParentElement().addClassName(style.button());
    label.getElement().getParentElement().addClassName(style.pageDetails());
    nextPage.getElement().getParentElement().addClassName(style.button());
    if (showFastForwardButton) {
      fastForward.getElement().getParentElement().addClassName(style.button());
    }
    if (showLastPageButton) {
      lastPage.getElement().getParentElement().addClassName(style.button());
    }

    // Disable the buttons by default.
    setView(null);
  }

  @Override
  public void firstPage() {
    super.firstPage();
  }

  @Override
  public int getPage() {
    return super.getPage();
  }

  @Override
  public int getPageCount() {
    return super.getPageCount();
  }

  @Override
  public boolean hasNextPage() {
    return super.hasNextPage();
  }

  @Override
  public boolean hasNextPages(int pages) {
    return super.hasNextPages(pages);
  }

  @Override
  public boolean hasPage(int index) {
    return super.hasPage(index);
  }

  @Override
  public boolean hasPreviousPage() {
    return super.hasPreviousPage();
  }

  @Override
  public boolean hasPreviousPages(int pages) {
    return super.hasPreviousPages(pages);
  }

  @Override
  public void lastPage() {
    super.lastPage();
  }

  @Override
  public void lastPageStart() {
    super.lastPageStart();
  }

  @Override
  public void nextPage() {
    super.nextPage();
  }

  @Override
  public void previousPage() {
    super.previousPage();
  }

  @Override
  public void setPage(int index) {
    super.setPage(index);
  }

  @Override
  public void setPageSize(int pageSize) {
    super.setPageSize(pageSize);
  }

  @Override
  public void setPageStart(int index) {
    super.setPageStart(index);
  }

  @Override
  public void setView(HasRows view) {
    // Enable or disable all buttons.
    boolean disableButtons = (view == null);
    setFastForwardDisabled(disableButtons);
    setNextPageButtonsDisabled(disableButtons);
    setPrevPageButtonsDisabled(disableButtons);
    super.setView(view);
  }

  /**
   * Let the page know that the table is loading. Call this method to clear all
   * data from the table and hide the current range when new data is being
   * loaded into the table.
   */
  public void startLoading() {
    getView().setRowCount(0, true);
    label.setHTML("");
  }

  /**
   * Get the text to display in the pager that reflects the state of the pager.
   *
   * @return the text
   */
  protected String createText() {
    // Default text is 1 based.
    NumberFormat formatter = NumberFormat.getFormat("#,###");
    HasRows view = getView();
    Range range = view.getVisibleRange();
    int pageStart = range.getStart() + 1;
    int pageSize = range.getLength();
    int dataSize = view.getRowCount();
    int endIndex = Math.min(dataSize, pageStart + pageSize - 1);
    endIndex = Math.max(pageStart, endIndex);
    boolean exact = view.isRowCountExact();
    return formatter.format(pageStart) + "-" + formatter.format(endIndex)
        + (exact ? " of " : " of over ") + formatter.format(dataSize);
  }

  @Override
  protected void onRangeOrRowCountChanged() {
    HasRows view = getView();
    label.setText(createText());

    // Update the prev and first buttons.
    setPrevPageButtonsDisabled(!hasPreviousPage());

    // Update the next and last buttons.
    if (isRangeLimited() || !view.isRowCountExact()) {
      setNextPageButtonsDisabled(!hasNextPage());
      setFastForwardDisabled(!hasNextPages(getFastForwardPages()));
    }
  }

  /**
   * Check if the next button is disabled. Visible for testing.
   */
  boolean isNextButtonDisabled() {
    return nextDisabled;
  }

  /**
   * Check if the previous button is disabled. Visible for testing.
   */
  boolean isPreviousButtonDisabled() {
    return prevDisabled;
  }

  /**
   * Get the number of pages to fast forward based on the current page size.
   *
   * @return the number of pages to fast forward
   */
  private int getFastForwardPages() {
    int pageSize = getPageSize();
    return pageSize > 0 ? fastForwardRows / pageSize : 0;
  }

  /**
   * Enable or disable the fast forward button.
   *
   * @param disabled true to disable, false to enable
   */
  private void setFastForwardDisabled(boolean disabled) {
    if (fastForward == null) {
      return;
    }
    if (disabled) {
      fastForward.setResource(resources.simplePagerFastForwardDisabled());
      fastForward.getElement().getParentElement().addClassName(
          style.disabledButton());
    } else {
      fastForward.setResource(resources.simplePagerFastForward());
      fastForward.getElement().getParentElement().removeClassName(
          style.disabledButton());
    }
  }

  /**
   * Enable or disable the next page buttons.
   *
   * @param disabled true to disable, false to enable
   */
  private void setNextPageButtonsDisabled(boolean disabled) {
    if (disabled == nextDisabled) {
      return;
    }

    nextDisabled = disabled;
    if (disabled) {
      nextPage.setResource(resources.simplePagerNextPageDisabled());
      nextPage.getElement().getParentElement().addClassName(
          style.disabledButton());
      if (lastPage != null) {
        lastPage.setResource(resources.simplePagerLastPageDisabled());
        lastPage.getElement().getParentElement().addClassName(
            style.disabledButton());
      }
    } else {
      nextPage.setResource(resources.simplePagerNextPage());
      nextPage.getElement().getParentElement().removeClassName(
          style.disabledButton());
      if (lastPage != null) {
        lastPage.setResource(resources.simplePagerLastPage());
        lastPage.getElement().getParentElement().removeClassName(
            style.disabledButton());
      }
    }
  }

  /**
   * Enable or disable the previous page buttons.
   *
   * @param disabled true to disable, false to enable
   */
  private void setPrevPageButtonsDisabled(boolean disabled) {
    if (disabled == prevDisabled) {
      return;
    }

    prevDisabled = disabled;
    if (disabled) {
      firstPage.setResource(resources.simplePagerFirstPageDisabled());
      firstPage.getElement().getParentElement().addClassName(
          style.disabledButton());
      prevPage.setResource(resources.simplePagerPreviousPageDisabled());
      prevPage.getElement().getParentElement().addClassName(
          style.disabledButton());
    } else {
      firstPage.setResource(resources.simplePagerFirstPage());
      firstPage.getElement().getParentElement().removeClassName(
          style.disabledButton());
      prevPage.setResource(resources.simplePagerPreviousPage());
      prevPage.getElement().getParentElement().removeClassName(
          style.disabledButton());
    }
  }
}
