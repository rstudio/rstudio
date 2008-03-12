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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeImages;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

/**
 * <p>
 * A generic application that includes a title bar, main menu, content area, and
 * some external links at the top.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.Application { Applied to the entire Application }</li>
 * <li>.Application-top { The top portion of the Application }</li>
 * <li>.Application-title { The title widget }</li>
 * <li>.Application-links { The main external links }</li>
 * <li>.Application-menu { The main menu }</li>
 * <li>.Application-menu-title { The title above the main menu }</li>
 * <li>.Application-menu-wrapper { The scrollable element around the main menu }</li>
 * <li>.Application-content-wrapper { The scrollable element around the content }</li>
 * </ul>
 */
public class Application extends Composite implements WindowResizeListener {
  /**
   * Images used in the {@link Application}.
   */
  public interface ApplicationImages extends TreeImages {
    /**
     * An image indicating a leaf.
     * 
     * @gwt.resource noimage.png
     * @return a prototype of this image
     */
    AbstractImagePrototype treeLeaf();
  }

  /**
   * A listener to handle events from the Application.
   */
  public interface ApplicationListener {
    /**
     * Fired when a menu item is selected.
     * 
     * @param item the item that was selected
     */
    void onMenuItemSelected(com.google.gwt.user.client.ui.TreeItem item);
  }

  /**
   * The base style name.
   */
  public static final String DEFAULT_STYLE_NAME = "Application";

  /**
   * The wrapper around the content.
   */
  private ScrollPanel contentWrapper;

  /**
   * The main wrapper around the menu and content.
   */
  private FlexTable contentLayout;

  /**
   * The panel that holds the main links.
   */
  private HorizontalPanel linksPanel;

  /**
   * The {@link ApplicationListener}.
   */
  private ApplicationListener listener = null;

  /**
   * The main menu.
   */
  private Tree mainMenu;

  /**
   * The wrapper around the main menu.
   */
  private ScrollPanel mainMenuWrapper;

  /**
   * The panel that contains the title widget and links.
   */
  private Grid topPanel;

  /**
   * Constructor.
   */
  public Application() {
    // Setup the main layout
    VerticalPanel layout = new VerticalPanel();
    initWidget(layout);
    layout.setSize("100%", "100%");

    // Setup the top panel with the title and links
    topPanel = createTopPanel();
    layout.add(topPanel);

    // Create a DecoratorPanel to hold the menu and content
    DecoratorPanel mainDecorator = new DecoratorPanel();
    layout.add(mainDecorator);
    layout.setCellHorizontalAlignment(mainDecorator,
        HasHorizontalAlignment.ALIGN_CENTER);
    layout.setCellVerticalAlignment(mainDecorator,
        HasVerticalAlignment.ALIGN_MIDDLE);

    // Setup the content layout
    contentLayout = new FlexTable();
    contentLayout.setCellPadding(0);
    contentLayout.setCellSpacing(0);
    mainDecorator.setWidget(contentLayout);
    FlexCellFormatter formatter = contentLayout.getFlexCellFormatter();

    // Add the main menu title
    setMainMenuTitle("Main Menu");
    formatter.setStyleName(0, 0, DEFAULT_STYLE_NAME + "-menu-title");

    // Add the main menu
    contentLayout.setWidget(1, 0, createMainMenu());
    formatter.setStyleName(1, 0, DEFAULT_STYLE_NAME + "-menu-wrapper");

    // Add the content title
    setContentTitle(new HTML("Content"));
    formatter.setStyleName(0, 1, DEFAULT_STYLE_NAME + "-content-title");

    // Add the content wrapper
    contentWrapper = new ScrollPanel();
    contentLayout.setWidget(1, 1, contentWrapper);
    formatter.setStyleName(1, 1, DEFAULT_STYLE_NAME + "-content-wrapper");
    setContent(null);
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
    return contentLayout.getWidget(0, 1);
  }

  /**
   * @return the main menu.
   */
  public Tree getMainMenu() {
    return mainMenu;
  }

  /**
   * @return the title above the main menu
   */
  public String getMainMenuTitle() {
    return contentLayout.getHTML(0, 0);
  }

  /**
   * @return the {@link Widget} used as the title
   */
  public Widget getTitleWidget() {
    return topPanel.getWidget(0, 0);
  }

  public void onWindowResized(int width, int height) {
    // Set the height of the main layout
    getWidget().setHeight(height + "px");

    // Set the size of the main wrappers
    int contentTitleHeight = DOM.getElementPropertyInt(
        contentLayout.getCellFormatter().getElement(0, 1), "offsetHeight");
    int bottomHeight = height - topPanel.getOffsetHeight() - 50
        - contentTitleHeight;
    int bottomWidth = width - 50;
    mainMenuWrapper.setSize("200px", bottomHeight + "px");
    contentWrapper.setSize((bottomWidth - 200) + "px", bottomHeight + "px");
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
    contentLayout.setWidget(0, 1, title);
  }

  /**
   * Set the {@link ApplicationListener}.
   * 
   * @param listener the listener
   */
  public void setListener(ApplicationListener listener) {
    this.listener = listener;
  }

  /**
   * Set the title of the main menu.
   * 
   * @param title the main menu title
   */
  public void setMainMenuTitle(String title) {
    contentLayout.setHTML(0, 0, title);
  }

  /**
   * Set the {@link Widget} to use as the title bar.
   * 
   * @param title the title widget
   */
  public void setTitleWidget(Widget title) {
    topPanel.setWidget(0, 0, title);
  }

  @Override
  protected void onLoad() {
    Window.addWindowResizeListener(this);
    onWindowResized(Window.getClientWidth(), Window.getClientHeight());
  }

  @Override
  protected void onUnload() {
    Window.removeWindowResizeListener(this);
  }

  /**
   * Create the main menu.
   * 
   * @return the main menu
   */
  private Widget createMainMenu() {
    // Setup the main menu
    ApplicationImages treeImages = GWT.create(ApplicationImages.class);
    mainMenu = new Tree(treeImages);
    mainMenu.addStyleName(DEFAULT_STYLE_NAME + "-menu");
    mainMenu.addTreeListener(new TreeListener() {
      public void onTreeItemSelected(TreeItem item) {
        if (listener != null) {
          listener.onMenuItemSelected(item);
        }
      }

      public void onTreeItemStateChanged(TreeItem item) {
      }
    });

    // Add a wrapper around the menu
    mainMenuWrapper = new ScrollPanel(mainMenu);
    return mainMenuWrapper;
  }

  /**
   * Create the panel at the top of the page that contains the title and links.
   * 
   * @return the top panel
   */
  private Grid createTopPanel() {
    Grid grid = new Grid(1, 2);
    grid.setStyleName(DEFAULT_STYLE_NAME + "-top");
    grid.getRowFormatter().setVerticalAlign(0, HasVerticalAlignment.ALIGN_TOP);

    // Setup the title cell
    CellFormatter formatter = grid.getCellFormatter();
    formatter.setStyleName(0, 0, DEFAULT_STYLE_NAME + "-title");

    // Setup the links cell
    linksPanel = new HorizontalPanel();
    grid.setWidget(0, 1, linksPanel);
    formatter.setStyleName(0, 1, DEFAULT_STYLE_NAME + "-links");
    formatter.setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_RIGHT);

    // Return the panel
    return grid;
  }
}
