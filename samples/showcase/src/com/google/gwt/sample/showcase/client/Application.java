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
package com.google.gwt.sample.showcase.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

/**
 * <p>
 * A generic application that includes a title bar, main menu, content area, and
 * some external links at the top.
 * </p>
 * <h3>CSS Style Rules</h3>
 * 
 * <ul class="css">
 * 
 * <li>.Application { Applied to the entire Application }</li>
 * 
 * <li>.Application-top { The top portion of the Application }</li>
 * 
 * <li>.Application-title { The title widget }</li>
 * 
 * <li>.Application-links { The main external links }</li>
 * 
 * <li>.Application-options { The options widget }</li>
 * 
 * <li>.Application-menu { The main menu }</li>
 * 
 * <li>.Application-content-wrapper { The scrollable element around the content }</li>
 * 
 * </ul>
 */
public class Application extends Composite implements ResizeHandler,
    HasSelectionHandlers<TreeItem> {
  /**
   * Images used in the {@link Application}.
   */
  public interface ApplicationImages extends Tree.Resources {
    /**
     * An image indicating a leaf.
     * 
     * @return a prototype of this image
     */
    @Source("noimage.png")
    ImageResource treeLeaf();
  }

  /**
   * The base style name.
   */
  public static final String DEFAULT_STYLE_NAME = "Application";

  /**
   * The panel that contains the menu and content.
   */
  private HorizontalPanel bottomPanel;

  /**
   * The decorator around the content.
   */
  private DecoratorPanel contentDecorator;

  /**
   * The main wrapper around the content and content title.
   */
  private Grid contentLayout;

  /**
   * The wrapper around the content.
   */
  private SimplePanel contentWrapper;

  /**
   * The panel that holds the main links.
   */
  private HorizontalPanel linksPanel;

  /**
   * The main menu.
   */
  private Tree mainMenu;

  /**
   * The last known width of the window.
   */
  private int windowWidth = -1;

  /**
   * The panel that contains the title widget and links.
   */
  private FlexTable topPanel;

  /**
   * Constructor.
   */
  public Application() {
    // Setup the main layout widget
    FlowPanel layout = new FlowPanel();
    initWidget(layout);

    // Setup the top panel with the title and links
    createTopPanel();
    layout.add(topPanel);

    // Add the main menu
    bottomPanel = new HorizontalPanel();
    bottomPanel.setWidth("100%");
    bottomPanel.setSpacing(0);
    bottomPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
    layout.add(bottomPanel);
    createMainMenu();
    bottomPanel.add(mainMenu);

    // Setup the content layout
    contentLayout = new Grid(2, 1);
    contentLayout.setCellPadding(0);
    contentLayout.setCellSpacing(0);
    contentDecorator = new DecoratorPanel();
    contentDecorator.setWidget(contentLayout);
    contentDecorator.addStyleName(DEFAULT_STYLE_NAME + "-content-decorator");
    bottomPanel.add(contentDecorator);
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      bottomPanel.setCellHorizontalAlignment(contentDecorator,
          HasHorizontalAlignment.ALIGN_LEFT);
      contentDecorator.getElement().setAttribute("align", "LEFT");
    } else {
      bottomPanel.setCellHorizontalAlignment(contentDecorator,
          HasHorizontalAlignment.ALIGN_RIGHT);
      contentDecorator.getElement().setAttribute("align", "RIGHT");
    }
    CellFormatter formatter = contentLayout.getCellFormatter();

    // Add the content title
    setContentTitle(new HTML("Content"));
    formatter.setStyleName(0, 0, DEFAULT_STYLE_NAME + "-content-title");

    // Add the content wrapper
    contentWrapper = new SimplePanel();
    contentLayout.setWidget(1, 0, contentWrapper);
    formatter.setStyleName(1, 0, DEFAULT_STYLE_NAME + "-content-wrapper");
    setContent(null);

    // Add a window resize handler
    Window.addResizeHandler(this);
  }

  /**
   * Add a link to the top of the page.
   * 
   * @param link the widget to add to the mainLinks
   */
  public void addLink(Widget link) {
    if (linksPanel.getWidgetCount() > 0) {
      linksPanel.add(new HTML("&nbsp;|&nbsp;"));
    }
    linksPanel.add(link);
  }

  public HandlerRegistration addSelectionHandler(
      SelectionHandler<TreeItem> handler) {
    return mainMenu.addSelectionHandler(handler);
  }

  /**
   * @return the {@link Widget} in the content area
   */
  public Widget getContent() {
    return contentWrapper.getWidget();
  }

  /**
   * @return the content title widget
   */
  public Widget getContentTitle() {
    return contentLayout.getWidget(0, 0);
  }

  /**
   * @return the main menu.
   */
  public Tree getMainMenu() {
    return mainMenu;
  }

  /**
   * @return the {@link Widget} used as the title
   */
  public Widget getTitleWidget() {
    return topPanel.getWidget(0, 0);
  }

  public void onResize(ResizeEvent event) {
    onWindowResized(event.getWidth(), event.getHeight());
  }

  public void onWindowResized(int width, int height) {
    if (width == windowWidth || width < 1) {
      return;
    }
    windowWidth = width;
    onWindowResizedImpl(width);
  }

  /**
   * Set the {@link Widget} to display in the content area.
   * 
   * @param content the content widget
   */
  public void setContent(Widget content) {
    if (content == null) {
      contentWrapper.setWidget(new HTML("&nbsp;"));
    } else {
      contentWrapper.setWidget(content);
    }
  }

  /**
   * Set the title of the content area.
   * 
   * @param title the content area title
   */
  public void setContentTitle(Widget title) {
    contentLayout.setWidget(0, 0, title);
  }

  /**
   * Set the {@link Widget} to use as options, which appear to the right of the
   * title bar.
   * 
   * @param options the options widget
   */
  public void setOptionsWidget(Widget options) {
    topPanel.setWidget(1, 1, options);
  }

  /**
   * Set the {@link Widget} to use as the title bar.
   * 
   * @param title the title widget
   */
  public void setTitleWidget(Widget title) {
    topPanel.setWidget(1, 0, title);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    onWindowResized(Window.getClientWidth(), Window.getClientHeight());
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    windowWidth = -1;
  }

  protected void onWindowResizedImpl(int width) {
    int menuWidth = mainMenu.getOffsetWidth();
    int contentWidth = Math.max(width - menuWidth - 30, 1);
    int contentWidthInner = Math.max(contentWidth - 10, 1);
    bottomPanel.setCellWidth(mainMenu, menuWidth + "px");
    bottomPanel.setCellWidth(contentDecorator, contentWidth + "px");
    contentLayout.getCellFormatter().setWidth(0, 0, contentWidthInner + "px");
    contentLayout.getCellFormatter().setWidth(1, 0, contentWidthInner + "px");
  }

  /**
   * Create the main menu.
   */
  private void createMainMenu() {
    // Setup the main menu
    ApplicationImages treeImages = GWT.create(ApplicationImages.class);
    mainMenu = new Tree(treeImages);
    mainMenu.setAnimationEnabled(true);
    mainMenu.addStyleName(DEFAULT_STYLE_NAME + "-menu");
  }

  /**
   * Create the panel at the top of the page that contains the title and links.
   */
  private void createTopPanel() {
    boolean isRTL = LocaleInfo.getCurrentLocale().isRTL();
    topPanel = new FlexTable();
    topPanel.setCellPadding(0);
    topPanel.setCellSpacing(0);
    topPanel.setStyleName(DEFAULT_STYLE_NAME + "-top");
    FlexCellFormatter formatter = topPanel.getFlexCellFormatter();

    // Setup the links cell
    linksPanel = new HorizontalPanel();
    topPanel.setWidget(0, 0, linksPanel);
    formatter.setStyleName(0, 0, DEFAULT_STYLE_NAME + "-links");
    if (isRTL) {
      formatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_LEFT);
    } else {
      formatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);
    }
    formatter.setColSpan(0, 0, 2);

    // Setup the title cell
    setTitleWidget(null);
    formatter.setStyleName(1, 0, DEFAULT_STYLE_NAME + "-title");

    // Setup the options cell
    setOptionsWidget(null);
    formatter.setStyleName(1, 1, DEFAULT_STYLE_NAME + "-options");
    if (isRTL) {
      formatter.setHorizontalAlignment(1, 1, HasHorizontalAlignment.ALIGN_LEFT);
    } else {
      formatter.setHorizontalAlignment(1, 1, HasHorizontalAlignment.ALIGN_RIGHT);
    }

    // Align the content to the top
    topPanel.getRowFormatter().setVerticalAlign(0,
        HasVerticalAlignment.ALIGN_TOP);
    topPanel.getRowFormatter().setVerticalAlign(1,
        HasVerticalAlignment.ALIGN_TOP);
  }
}
