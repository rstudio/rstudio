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

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.user.client.HTTPRequest;
import com.google.gwt.user.client.ResponseTextHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * <p>
 * A widget used to show GWT examples in the ContentPanel. It includes a tab bar
 * with options to view the example, view the source, or view the css style
 * rules.
 * </p>
 * <p>
 * This {@link Widget} uses a lazy initialization mechanism so that the content
 * is not rendered until the onInitialize method is called, which happens the
 * first time the {@link Widget} is added to the page.. The data in the source
 * and css tabs are loaded using an RPC call to the server.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.sc-ContentWidget { Applied to the entire widget }</li>
 * <li>.sc-ContentWidget-tabBar { Applied to the TabBar }</li>
 * <li>.sc-ContentWidget-deckPanel { Applied to the DeckPanel }</li>
 * <li>.sc-ContentWidget-name { Applied to the name }</li>
 * <li>.sc-ContentWidget-description { Applied to the description }</li>
 * </ul>
 */
public abstract class ContentWidget extends Composite implements TabListener {
  /**
   * The constants used in this Content Widget.
   */
  public static interface CwConstants extends Constants {
    String contentWidgetExample();

    String contentWidgetSource();

    String contentWidgetStyle();
  }

  /**
   * The default style name.
   */
  private static final String DEFAULT_STYLE_NAME = "sc-ContentWidget";

  /**
   * The static loading image displayed when loading CSS or source code.
   */
  private static Image loadingImage;

  /**
   * An instance of the constants.
   * 
   * @gwt.DATA
   */
  private CwConstants constants;

  /**
   * The deck panel with the contents.
   */
  private DeckPanel deckPanel = null;

  /**
   * A boolean indicating whether or not this widget has been initialized.
   */
  private boolean initialized = false;

  /**
   * A boolean indicating whether or not the RPC request for the source code has
   * been sent.
   */
  private boolean sourceLoaded = false;

  /**
   * The widget used to display source code.
   */
  private HTML sourceWidget = null;

  /**
   * A boolean indicating whether or not the RPC request for the style code has
   * been sent.
   */
  private boolean styleLoaded = false;

  /**
   * The widget used to display css style.
   */
  private HTML styleWidget = null;

  /**
   * The tab bar of options.
   */
  private TabBar tabBar = null;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public ContentWidget(CwConstants constants) {
    this.constants = constants;
    tabBar = new TabBar();
    deckPanel = new DeckPanel();
    initWidget(deckPanel);
    setStyleName(DEFAULT_STYLE_NAME);
  }

  /**
   * Add an item to this content widget.
   * 
   * @param w the widget to add
   * @param tabText the text to display in the tab
   */
  public void add(Widget w, String tabText) {
    tabBar.addTab(tabText);
    deckPanel.add(w);
  }

  /**
   * Get the description of this example.
   * 
   * @return a description for this example
   */
  public abstract String getDescription();

  /**
   * Get the name of this example to use as a title.
   * 
   * @return a name for this example
   */
  public abstract String getName();

  /**
   * @return the tab bar
   */
  public TabBar getTabBar() {
    return tabBar;
  }

  /**
   * Returns true if this widget has a source section.
   * 
   * @return true if source tab available
   */
  public boolean hasSource() {
    return true;
  }

  /**
   * Returns true if this widget has a style section.
   * 
   * @return true if style tab available
   */
  public boolean hasStyle() {
    return true;
  }

  /**
   * Initialize this widget by creating the elements that should be added to the
   * page.
   */
  public final void initialize() {
    if (initialized == false) {
      initialized = true;

      // Add a tab listener
      tabBar.addTabListener(this);

      // Create a container for the main example
      final VerticalPanel vPanel = new VerticalPanel();
      add(vPanel, constants.contentWidgetExample());

      // Add the name
      HTML nameWidget = new HTML(getName());
      nameWidget.setStyleName(DEFAULT_STYLE_NAME + "-name");
      vPanel.add(nameWidget);

      // Add the description
      HTML descWidget = new HTML(getDescription());
      descWidget.setStyleName(DEFAULT_STYLE_NAME + "-description");
      vPanel.add(descWidget);

      // Add source code tab
      if (hasSource()) {
        sourceWidget = new HTML();
        add(sourceWidget, constants.contentWidgetSource());
      } else {
        sourceLoaded = true;
      }

      // Add style tab
      if (hasStyle()) {
        styleWidget = new HTML();
        add(styleWidget, constants.contentWidgetStyle());
      } else {
        styleLoaded = true;
      }

      // Initialize the widget and add it to the page
      Widget widget = onInitialize();
      if (widget != null) {
        vPanel.add(widget);
      }
      onInitializeComplete();
    }
  }

  public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
    return true;
  }

  /**
   * When the widget is first initialize, this method is called. If it returns a
   * Widget, the widget will be added as the first tab. Return null to disable
   * the first tab.
   * 
   * @return the widget to add to the first tab
   */
  public abstract Widget onInitialize();

  /**
   * Called when initialization has completed and the widget has been added to
   * the page.
   */
  public void onInitializeComplete() {
  }

  public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
    // Show the associated widget in the deck panel
    deckPanel.showWidget(tabIndex);

    // Send an RPC request to load the content of the tab
    String tabHTML = getTabBar().getTabHTML(tabIndex);
    if (!sourceLoaded && tabHTML.equals(constants.contentWidgetSource())) {
      sourceLoaded = true;
      requestFileContents("source/" + this.getClass().getName() + ".html",
          sourceWidget, "Source code not available.");
    } else if (!styleLoaded && tabHTML.equals(constants.contentWidgetStyle())) {
      styleLoaded = true;
      requestFileContents("style/" + this.getClass().getName() + ".html",
          styleWidget, "Style not available.");
    }
  }

  /**
   * Select a tab.
   * 
   * @param index the tab index
   */
  public void selectTab(int index) {
    tabBar.selectTab(index);
  }

  @Override
  protected void onLoad() {
    // Initialize this widget if we haven't already
    initialize();

    // Select the first tab
    if (getTabBar().getTabCount() > 0) {
      tabBar.selectTab(0);
    }
  }

  /**
   * Load the contents of a remote file into the specified widget.
   * 
   * @param url the URL of the file
   * @param target the target Widget to place the contents
   * @param errorMsg the error message to display if the request fails
   */
  protected void requestFileContents(String url, final HTML target,
      final String errorMsg) {
    // Show the loading image
    if (loadingImage == null) {
      loadingImage = new Image("images/loading.gif");
    }
    target.setHTML("&nbsp;&nbsp;" + loadingImage.toString());

    // Request the contents of the file
    HTTPRequest.asyncGet(url, new ResponseTextHandler() {
      public void onCompletion(String responseText) {
        if (responseText.length() > 0) {
          target.setHTML(responseText);
        } else {
          target.setHTML(errorMsg);
        }
      }
    });
  }
}
