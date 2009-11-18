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

import static com.google.gwt.core.client.prefetch.RunAsyncCode.runAsyncCode;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.prefetch.Prefetcher;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.sample.showcase.client.content.i18n.CwConstantsExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwConstantsWithLookupExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwDateTimeFormat;
import com.google.gwt.sample.showcase.client.content.i18n.CwDictionaryExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwMessagesExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwNumberFormat;
import com.google.gwt.sample.showcase.client.content.i18n.CwPluralFormsExample;
import com.google.gwt.sample.showcase.client.content.lists.CwListBox;
import com.google.gwt.sample.showcase.client.content.lists.CwMenuBar;
import com.google.gwt.sample.showcase.client.content.lists.CwStackPanel;
import com.google.gwt.sample.showcase.client.content.lists.CwSuggestBox;
import com.google.gwt.sample.showcase.client.content.lists.CwTree;
import com.google.gwt.sample.showcase.client.content.other.CwAnimation;
import com.google.gwt.sample.showcase.client.content.other.CwCookies;
import com.google.gwt.sample.showcase.client.content.panels.CwAbsolutePanel;
import com.google.gwt.sample.showcase.client.content.panels.CwDecoratorPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwDisclosurePanel;
import com.google.gwt.sample.showcase.client.content.panels.CwDockPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwFlowPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwHorizontalPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwHorizontalSplitPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwTabPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwVerticalPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwVerticalSplitPanel;
import com.google.gwt.sample.showcase.client.content.popups.CwBasicPopup;
import com.google.gwt.sample.showcase.client.content.popups.CwDialogBox;
import com.google.gwt.sample.showcase.client.content.tables.CwFlexTable;
import com.google.gwt.sample.showcase.client.content.tables.CwGrid;
import com.google.gwt.sample.showcase.client.content.text.CwBasicText;
import com.google.gwt.sample.showcase.client.content.text.CwRichText;
import com.google.gwt.sample.showcase.client.content.widgets.CwBasicButton;
import com.google.gwt.sample.showcase.client.content.widgets.CwCustomButton;
import com.google.gwt.sample.showcase.client.content.widgets.CwDatePicker;
import com.google.gwt.sample.showcase.client.content.widgets.CwFileUpload;
import com.google.gwt.sample.showcase.client.content.widgets.CwHyperlink;
import com.google.gwt.sample.showcase.client.content.widgets.CwRadioButton;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * <h3>CSS Style Rules</h3> <ul class="css"> <li>.sc-ContentWidget { Applied to
 * the entire widget }</li> <li>.sc-ContentWidget-tabBar { Applied to the TabBar
 * }</li> <li>.sc-ContentWidget-deckPanel { Applied to the DeckPanel }</li> <li>
 * .sc-ContentWidget-name { Applied to the name }</li> <li>
 * .sc-ContentWidget-description { Applied to the description }</li> </ul>
 */
public abstract class ContentWidget extends LazyPanel implements
    SelectionHandler<Integer> {
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
  private static String loadingImage;

  private static <T> List<T> list(T... elems) {
    List<T> list = new ArrayList<T>(elems.length);
    for (T elem : elems) {
      list.add(elem);
    }
    return list;
  }

  /**
   * The tab bar of options.
   */
  protected TabBar tabBar = null;

  /**
   * An instance of the constants.
   */
  private final CwConstants constants;

  /**
   * The deck panel with the contents.
   */
  private DeckPanel deckPanel = null;

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
   * Whether the demo widget has been initialized.
   */
  private boolean widgetInitialized;

  /**
   * Whether the demo widget is (asynchronously) initializing.
   */
  private boolean widgetInitializing;

  /**
   * A vertical panel that holds the demo widget once it is initialized.
   */
  private VerticalPanel widgetVpanel;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public ContentWidget(CwConstants constants) {
    this.constants = constants;
    tabBar = new TabBar();
  }

  /**
   * Add an item to this content widget. Should not be called before
   * {@link #onInitializeComplete} has been called.
   * 
   * @param w the widget to add
   * @param tabText the text to display in the tab
   */
  public void add(Widget w, String tabText) {
    tabBar.addTab(tabText);
    deckPanel.add(w);
  }

  @Override
  public void ensureWidget() {
    super.ensureWidget();
    ensureWidgetInitialized(widgetVpanel);
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
   * When the widget is first initialized, this method is called. If it returns
   * a Widget, the widget will be added as the first tab. Return null to disable
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

  public void onSelection(SelectionEvent<Integer> event) {
    // Show the associated widget in the deck panel
    int tabIndex = event.getSelectedItem().intValue();
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

  protected abstract void asyncOnInitialize(final AsyncCallback<Widget> callback);

  /**
   * Initialize this widget by creating the elements that should be added to the
   * page.
   */
  @Override
  protected final Widget createWidget() {
    deckPanel = new DeckPanel();

    setStyleName(DEFAULT_STYLE_NAME);

    // Add a tab handler
    tabBar.addSelectionHandler(this);

    // Create a container for the main example
    widgetVpanel = new VerticalPanel();
    add(widgetVpanel, constants.contentWidgetExample());

    // Add the name
    HTML nameWidget = new HTML(getName());
    nameWidget.setStyleName(DEFAULT_STYLE_NAME + "-name");
    widgetVpanel.add(nameWidget);

    // Add the description
    HTML descWidget = new HTML(getDescription());
    descWidget.setStyleName(DEFAULT_STYLE_NAME + "-description");
    widgetVpanel.add(descWidget);

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

    return deckPanel;
  }

  @Override
  protected void onLoad() {
    ensureWidget();

    // Select the first tab
    if (getTabBar().getTabCount() > 0) {
      tabBar.selectTab(0);
    }
  }

  protected void prefetchInternationalization() {
    Prefetcher.prefetch(list(runAsyncCode(CwNumberFormat.class),
        runAsyncCode(CwDateTimeFormat.class),
        runAsyncCode(CwMessagesExample.class),
        runAsyncCode(CwPluralFormsExample.class),
        runAsyncCode(CwConstantsExample.class),
        runAsyncCode(CwConstantsWithLookupExample.class),
        runAsyncCode(CwDictionaryExample.class)));
  }

  protected void prefetchListsAndMenus() {
    Prefetcher.prefetch(list(runAsyncCode(CwListBox.class),
        runAsyncCode(CwSuggestBox.class), runAsyncCode(CwTree.class),
        runAsyncCode(CwMenuBar.class), runAsyncCode(CwStackPanel.class)));
  }

  protected void prefetchOther() {
    Prefetcher.prefetch(list(runAsyncCode(CwAnimation.class),
        runAsyncCode(CwCookies.class)));
  }

  protected void prefetchPanels() {
    Prefetcher.prefetch(list(runAsyncCode(CwDecoratorPanel.class),
        runAsyncCode(CwFlowPanel.class), runAsyncCode(CwHorizontalPanel.class),
        runAsyncCode(CwVerticalPanel.class),
        runAsyncCode(CwAbsolutePanel.class), runAsyncCode(CwDockPanel.class),
        runAsyncCode(CwDisclosurePanel.class), runAsyncCode(CwTabPanel.class),
        runAsyncCode(CwHorizontalSplitPanel.class),
        runAsyncCode(CwVerticalSplitPanel.class)));
  }

  protected void prefetchPopups() {
    Prefetcher.prefetch(list(runAsyncCode(CwBasicPopup.class),
        runAsyncCode(CwDialogBox.class)));
  }

  protected void prefetchTables() {
    Prefetcher.prefetch(list(runAsyncCode(CwGrid.class),
        runAsyncCode(CwFlexTable.class)));
  }

  protected void prefetchTextInput() {
    Prefetcher.prefetch(list(runAsyncCode(CwBasicText.class),
        runAsyncCode(CwRichText.class)));
  }

  protected void prefetchWidgets() {
    Prefetcher.prefetch(list(runAsyncCode(CwRadioButton.class),
        runAsyncCode(CwBasicButton.class), runAsyncCode(CwCustomButton.class),
        runAsyncCode(CwFileUpload.class), runAsyncCode(CwDatePicker.class),
        runAsyncCode(CwHyperlink.class)));
  }

  /**
   * Load the contents of a remote file into the specified widget.
   * 
   * @param url a partial path relative to the module base URL
   * @param target the target Widget to place the contents
   * @param callback the callback when the call completes
   */
  protected void requestSourceContents(String url, final HTML target,
      final RequestCallback callback) {
    // Show the loading image
    if (loadingImage == null) {
      loadingImage = "<img src=\"" + GWT.getModuleBaseURL()
          + "images/loading.gif\">";
    }
    target.setDirection(HasDirection.Direction.LTR);
    DOM.setStyleAttribute(target.getElement(), "textAlign", "left");
    target.setHTML("&nbsp;&nbsp;" + loadingImage);

    // Request the contents of the file
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        GWT.getModuleBaseURL() + url);
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
    try {
      builder.send();
    } catch (RequestException e) {
      realCallback.onError(null, e);
    }
  }

  /**
   * Start prefetches for this widget that are likely to show up after this
   * widget is clicked.
   */
  protected abstract void setRunAsyncPrefetches();

  /**
   * Ensure that the demo widget has been initialized. Note that initialization
   * can fail if there is a network failure.
   */
  private void ensureWidgetInitialized(final VerticalPanel vPanel) {
    if (widgetInitializing || widgetInitialized) {
      return;
    }

    widgetInitializing = true;

    asyncOnInitialize(new AsyncCallback<Widget>() {
      public void onFailure(Throwable reason) {
        widgetInitializing = false;
        Window.alert("Failed to download code for this widget (" + reason + ")");
      }

      public void onSuccess(Widget result) {
        widgetInitializing = false;
        widgetInitialized = true;

        Widget widget = result;
        if (widget != null) {
          vPanel.add(widget);
        }
        onInitializeComplete();
      }
    });

    setRunAsyncPrefetches();
  }
}
