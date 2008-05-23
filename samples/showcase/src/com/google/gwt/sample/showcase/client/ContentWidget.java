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

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.Map;

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
   * A mapping of themes to style definitions.
   */
  private Map<String, String> styleDefs = null;

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
        styleDefs = new HashMap<String, String>();
        styleWidget = new HTML();
        add(styleWidget, constants.contentWidgetStyle());
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

    // Load the source code
    String tabHTML = getTabBar().getTabHTML(tabIndex);
    if (!sourceLoaded && tabHTML.equals(constants.contentWidgetSource())) {
      sourceLoaded = true;
      String className = this.getClass().getName();
      className = className.substring(className.lastIndexOf(".") + 1);
      requestSourceContents(ShowcaseConstants.DST_SOURCE_EXAMPLE + className
          + ".html", sourceWidget, null);
    }

    // Load the style definitions
    if (hasStyle() && tabHTML.equals(constants.contentWidgetStyle())) {
      final String theme = Showcase.CUR_THEME;
      if (styleDefs.containsKey(theme)) {
        styleWidget.setHTML(styleDefs.get(theme));
      } else {
        styleDefs.put(theme, "");
        RequestCallback callback = new RequestCallback() {
          public void onError(Request request, Throwable exception) {
            styleDefs.put(theme, "Style not available.");
          }

          public void onResponseReceived(Request request, Response response) {
            styleDefs.put(theme, response.getText());
          }
        };

        String srcPath = ShowcaseConstants.DST_SOURCE_STYLE + theme;
        if (LocaleInfo.getCurrentLocale().isRTL()) {
          srcPath += "_rtl";
        }
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf(".") + 1);
        requestSourceContents(srcPath + "/" + className + ".html", styleWidget,
            callback);
      }
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
   * @param url the URL of the file relative to the source directory in public
   * @param target the target Widget to place the contents
   * @param callback the callback when the call completes
   */
  protected void requestSourceContents(String url, final HTML target,
      final RequestCallback callback) {
    // Show the loading image
    if (loadingImage == null) {
      loadingImage = new Image("images/loading.gif");
    }
    target.setDirection(HasDirection.Direction.LTR);
    DOM.setStyleAttribute(target.getElement(), "textAlign", "left");
    target.setHTML("&nbsp;&nbsp;" + loadingImage.toString());

    // Request the contents of the file
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
    RequestCallback realCallback = new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        target.setHTML("Cannot find resource");
        if (callback != null) {
          callback.onError(request, exception);
        }
      }

      public void onResponseReceived(Request request, Response response) {
        target.setHTML(response.getText());
        if (callback != null) {
          callback.onResponseReceived(request, response);
        }
      }
    };
    builder.setCallback(realCallback);

    // Send the request
    Request request = null;
    try {
      request = builder.send();
    } catch (RequestException e) {
      realCallback.onError(request, e);
    }
  }
}
