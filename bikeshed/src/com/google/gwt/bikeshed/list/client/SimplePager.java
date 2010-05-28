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
package com.google.gwt.bikeshed.list.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.PagingListView;

/**
 * A pager for controlling a {@link PagingListView} that only supports simple
 * page navigation.
 * 
 * @param <T> the type of the PagingListView being controlled
 */
public class SimplePager<T> extends AbstractPager<T> {

  /**
   * The location of the text relative to the paging buttons.
   */
  public static enum TextLocation {
    LEFT, RIGHT, CENTER;
  }

  /**
   * A ClientBundle that provides images for this widget.
   */
  public static interface Resources extends ClientBundle {

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

  private static Resources DEFAULT_RESOURCES;

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  private final Image firstPage;
  private final Label label = new Label();
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
   * Construct a {@link SimplePager}.
   * 
   * @param view the {@link PagingListView} to page
   */
  public SimplePager(PagingListView<T> view) {
    this(view, TextLocation.CENTER);
  }

  /**
   * Construct a {@link SimplePager} with the specified text location.
   * 
   * @param view the {@link PagingListView} to page
   * @param location the location of the text relative to the buttons
   */
  public SimplePager(PagingListView<T> view, TextLocation location) {
    this(view, location, getDefaultResources());
  }

  /**
   * Construct a {@link SimplePager} with the specified resources.
   * 
   * @param view the {@link PagingListView} to page
   * @param location the location of the text relative to the buttons
   * @param resources the {@link Resources} to use
   */
  public SimplePager(PagingListView<T> view, TextLocation location,
      Resources resources) {
    super(view);
    this.resources = resources;
    this.style = resources.simplePagerStyle();
    this.style.ensureInjected();

    // Create the buttons.
    firstPage = new Image(resources.simplePagerFirstPage());
    lastPage = new Image(resources.simplePagerLastPage());
    nextPage = new Image(resources.simplePagerNextPage());
    prevPage = new Image(resources.simplePagerPreviousPage());

    // Add handlers.
    firstPage.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        firstPage();
      }
    });
    lastPage.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        lastPage();
      }
    });
    nextPage.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        nextPage();
      }
    });
    prevPage.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        previousPage();
      }
    });

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
    layout.add(lastPage);
    if (location == TextLocation.LEFT) {
      layout.add(label);
    }

    // Add style names to the cells.
    firstPage.getElement().getParentElement().addClassName(style.button());
    prevPage.getElement().getParentElement().addClassName(style.button());
    label.getElement().getParentElement().addClassName(style.pageDetails());
    nextPage.getElement().getParentElement().addClassName(style.button());
    lastPage.getElement().getParentElement().addClassName(style.button());
  }

  @Override
  public void onRangeOrSizeChanged(PagingListView<T> listView) {
    super.onRangeOrSizeChanged(listView);
    label.setText(createText());

    // Update the prev and first buttons.
    boolean hasPrev = hasPreviousPage();
    if (hasPrev && prevDisabled) {
      prevDisabled = false;
      firstPage.setResource(resources.simplePagerFirstPage());
      prevPage.setResource(resources.simplePagerPreviousPage());
      firstPage.getElement().getParentElement().removeClassName(
          style.disabledButton());
      prevPage.getElement().getParentElement().removeClassName(
          style.disabledButton());
    } else if (!hasPrev && !prevDisabled) {
      prevDisabled = true;
      firstPage.setResource(resources.simplePagerFirstPageDisabled());
      prevPage.setResource(resources.simplePagerPreviousPageDisabled());
      firstPage.getElement().getParentElement().addClassName(
          style.disabledButton());
      prevPage.getElement().getParentElement().addClassName(
          style.disabledButton());
    }

    // Update the next and last buttons.
    if (isRangeLimited()) {
      boolean hasNext = hasNextPage();
      if (hasNext && nextDisabled) {
        nextDisabled = false;
        nextPage.setResource(resources.simplePagerNextPage());
        lastPage.setResource(resources.simplePagerLastPage());
        nextPage.getElement().getParentElement().removeClassName(
            style.disabledButton());
        lastPage.getElement().getParentElement().removeClassName(
            style.disabledButton());
      } else if (!hasNext && !nextDisabled) {
        nextDisabled = true;
        nextPage.setResource(resources.simplePagerNextPageDisabled());
        lastPage.setResource(resources.simplePagerLastPageDisabled());
        nextPage.getElement().getParentElement().addClassName(
            style.disabledButton());
        lastPage.getElement().getParentElement().addClassName(
            style.disabledButton());
      }
    }
  }

  /**
   * Get the text to display in the pager that reflects the state of the pager.
   * 
   * @return the text
   */
  protected String createText() {
    // Default text is 1 based.
    PagingListView<T> view = getPagingListView();
    int pageStart = view.getPageStart() + 1;
    int pageSize = view.getPageSize();
    int dataSize = view.getDataSize();
    int endIndex = Math.min(dataSize, pageStart + pageSize - 1);
    endIndex = Math.max(pageStart, endIndex);
    return pageStart + "-" + endIndex + " of " + dataSize;
  }
}
