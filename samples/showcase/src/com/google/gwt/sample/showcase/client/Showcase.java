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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.sample.showcase.client.Application.ApplicationListener;
import com.google.gwt.sample.showcase.client.content.i18n.CwConstantsExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwConstantsWithLookupExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwDateTimeFormat;
import com.google.gwt.sample.showcase.client.content.i18n.CwDictionaryExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwMessagesExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwNumberFormat;
import com.google.gwt.sample.showcase.client.content.lists.CwListBox;
import com.google.gwt.sample.showcase.client.content.lists.CwMenuBar;
import com.google.gwt.sample.showcase.client.content.lists.CwStackPanel;
import com.google.gwt.sample.showcase.client.content.lists.CwSuggestBox;
import com.google.gwt.sample.showcase.client.content.lists.CwTree;
import com.google.gwt.sample.showcase.client.content.other.CwAnimation;
import com.google.gwt.sample.showcase.client.content.other.CwCookies;
import com.google.gwt.sample.showcase.client.content.other.CwFrame;
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
import com.google.gwt.sample.showcase.client.content.widgets.CwCheckBox;
import com.google.gwt.sample.showcase.client.content.widgets.CwCustomButton;
import com.google.gwt.sample.showcase.client.content.widgets.CwFileUpload;
import com.google.gwt.sample.showcase.client.content.widgets.CwHyperlink;
import com.google.gwt.sample.showcase.client.content.widgets.CwRadioButton;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Showcase implements EntryPoint {
  /**
   * The static images used throughout the Showcase.
   */
  public static final ShowcaseImages images = (ShowcaseImages) GWT.create(ShowcaseImages.class);

  /**
   * The images to cache, such as background images. These images will be added
   * to the page and hidden, forcing the browser to cache them.
   */
  private static final String[] CACHED_IMAGES = {
      "bg_headergradient.png", "bg_listgradient.png", "bg_stackpanel.png",
      "bg_tab_selected.png", "corner.png", "hborder.png", "loading.gif",
      "vborder.png", "ie6/corner_dialog_topleft.png",
      "ie6/corner_dialog_topright.png", "ie6/hborder_blue_shadow.png",
      "ie6/hborder_gray_shadow.png", "ie6/vborder_blue_shadow.png",
      "ie6/vborder_gray_shadow.png"};

  /**
   * Link to GWT homepage.
   */
  private static final String GWT_HOMEPAGE = "http://code.google.com/webtoolkit/";

  /**
   * Link to GWT examples page.
   */
  private static final String GWT_EXAMPLES = GWT_HOMEPAGE + "examples/";

  /**
   * The available style themes that the user can select.
   */
  private static final String[] STYLE_THEMES = {"default", "chrome", "black"};
  
  /**
   * Convenience method for getting the document's head element.
   * 
   * @return the document's head element
   */
  private static native Element getHeadElement() /*-{
    return $doc.getElementsByTagName("head")[0];
  }-*/;

  /**
   * Get the URL of the page, without an hash of query string.
   * 
   * @return the location of the page
   */
  private static native String getHostPageLocation() /*-{
    var s = $doc.location.href;
  
    // Pull off any hash.
    var i = s.indexOf('#');
    if (i != -1)
      s = s.substring(0, i);
  
    // Pull off any query string.
    i = s.indexOf('?');
    if (i != -1)
      s = s.substring(0, i);
  
    // Ensure a final slash if non-empty.
    return s;
  }-*/;

  /**
   * The {@link Application}.
   */
  private Application app;

  /**
   * A mapping of history tokens to their associated menu items.
   */
  private Map<String, TreeItem> itemTokens = new HashMap<String, TreeItem>();

  /**
   * A mapping of menu items to the widget display when the item is selected.
   */
  private Map<TreeItem, ContentWidget> itemWidgets = new HashMap<TreeItem, ContentWidget>();

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    // Create the constants
    ShowcaseConstants constants = (ShowcaseConstants) GWT.create(ShowcaseConstants.class);

    // Create the application
    app = new Application();
    setupTitlePanel(constants);
    setupMainLinks(constants);
    setupOptionsPanel(constants);
    setupMainMenu(constants);
    RootPanel.get().add(app);

    // Swap out the style sheets for the RTL versions if needed.  We need to do
    // this after the app is loaded because the app will setup the layout based
    // on the width of the main menu, which is defined in the style sheet.  If
    // we swap the style sheets first, the app may load without any style sheet
    // to define the main menu width, because the RTL version is still being
    // loaded.  Note that we are basing the layout on the width defined in the
    // LTR version, so both versions should use the same width for the main nav
    // menu.
    includeStyleSheets();

    // Add an listener that sets the content widget when a menu item is selected
    app.setListener(new ApplicationListener() {
      public void onMenuItemSelected(TreeItem item) {
        ContentWidget content = itemWidgets.get(item);
        if (content != null) {
          History.newItem(getContentWidgetToken(content));
        }
      }
    });

    // Setup a history listener to reselect the associate menu item
    HistoryListener historyListener = new HistoryListener() {
      public void onHistoryChanged(String historyToken) {
        TreeItem item = itemTokens.get(historyToken);
        if (item != null) {
          // Select the item in the tree
          if (!item.equals(app.getMainMenu().getSelectedItem())) {
            app.getMainMenu().setSelectedItem(item, false);
            app.getMainMenu().ensureSelectedItemVisible();
          }

          // Show the associated widget
          displayContentWidget(itemWidgets.get(item));
        }
      }
    };
    History.addHistoryListener(historyListener);

    // Show the initial example
    String initToken = History.getToken();
    if (initToken.length() > 0) {
      historyListener.onHistoryChanged(initToken);
    } else {
      // Use the first token available
      TreeItem firstItem = app.getMainMenu().getItem(0).getChild(0);
      ContentWidget firstContent = itemWidgets.get(firstItem);
      historyListener.onHistoryChanged(getContentWidgetToken(firstContent));
    }

    // Cache images as needed
    cacheImages();
  }

  /**
   * Cache the images used in the background.
   */
  private void cacheImages() {
    for (int i = 0; i < CACHED_IMAGES.length; i++) {
      Image image = new Image("images/" + CACHED_IMAGES[i]);
      RootPanel.get().add(image);
      image.setVisible(false);
    }
  }

  /**
   * Set the content to the {@link ContentWidget}.
   * 
   * @param content the {@link ContentWidget} to display
   */
  private void displayContentWidget(ContentWidget content) {
    if (content != null) {
      app.setContent(content);
      app.setContentTitle(content.getTabBar());
    }
  }

  /**
   * Get the token for a given content widget.
   * 
   * @return the content widget token.
   */
  private String getContentWidgetToken(ContentWidget content) {
    String className = content.getClass().getName();
    className = className.substring(className.lastIndexOf('.') + 1);
    return className;
  }

  /**
   * Add the stylesheets to the page, loading one at a time.
   */
  private void includeStyleSheets() {
    // Do nothing if we are in LTR
    if (!LocaleInfo.getCurrentLocale().isRTL()) {
      return;
    }

    // Remove existing style sheets
    Element headElem = getHeadElement();
    int numChildren = DOM.getChildCount(headElem);
    for (int i = 0; i < numChildren; i++) {
      Element elem = DOM.getChild(headElem, i);
      if (DOM.getElementProperty(elem, "tagName").equalsIgnoreCase("link")
          && DOM.getElementProperty(elem, "rel").equalsIgnoreCase("stylesheet")) {
        // Remove the existing link
        String href = DOM.getElementProperty(elem, "href");
        DOM.removeChild(headElem, elem);

        // Add the style tag to the page
        href = href.replaceAll(".css", ".rtl.css");
        Element styleElem = DOM.createElement("link");
        DOM.setElementProperty(styleElem, "rel", "stylesheet");
        DOM.setElementProperty(styleElem, "type", "text/css");
        DOM.setElementProperty(styleElem, "href", href);
        DOM.insertChild(headElem, styleElem, i);
      }
    }
  }

  /**
   * Create the main links at the top of the application.
   * 
   * @param constants the constants with text
   */
  private void setupMainLinks(ShowcaseConstants constants) {
    // Link to GWT Homepage
    app.addLink(new HTML("<a href=\"" + GWT_HOMEPAGE + "\">"
        + constants.mainLinkHomepage() + "</a>"));

    // Link to More Examples
    app.addLink(new HTML("<a href=\"" + GWT_EXAMPLES + "\">"
        + constants.mainLinkExamples() + "</a>"));
  }

  /**
   * Setup all of the options in the main menu.
   * 
   * @param constants the constant values to use
   */
  private void setupMainMenu(ShowcaseConstants constants) {
    Tree mainMenu = app.getMainMenu();

    // Widgets
    TreeItem catWidgets = mainMenu.addItem(constants.categoryWidgets());
    setupMainMenuOption(catWidgets, new CwCheckBox(constants),
        images.catWidgets());
    setupMainMenuOption(catWidgets, new CwRadioButton(constants),
        images.catWidgets());
    setupMainMenuOption(catWidgets, new CwBasicButton(constants),
        images.catWidgets());
    setupMainMenuOption(catWidgets, new CwCustomButton(constants),
        images.catWidgets());
    setupMainMenuOption(catWidgets, new CwFileUpload(constants),
        images.catWidgets());
    setupMainMenuOption(catWidgets, new CwHyperlink(constants),
        images.catWidgets());

    // Lists
    TreeItem catLists = mainMenu.addItem(constants.categoryLists());
    setupMainMenuOption(catLists, new CwListBox(constants), images.catLists());
    setupMainMenuOption(catLists, new CwSuggestBox(constants),
        images.catLists());
    setupMainMenuOption(catLists, new CwTree(constants), images.catLists());
    setupMainMenuOption(catLists, new CwMenuBar(constants), images.catLists());
    setupMainMenuOption(catLists, new CwStackPanel(constants),
        images.catLists());

    // Text
    TreeItem catText = mainMenu.addItem(constants.categoryTextInput());
    setupMainMenuOption(catText, new CwBasicText(constants),
        images.catTextInput());
    setupMainMenuOption(catText, new CwRichText(constants),
        images.catTextInput());

    // Popups
    TreeItem catPopup = mainMenu.addItem(constants.categoryPopups());
    setupMainMenuOption(catPopup, new CwBasicPopup(constants),
        images.catPopups());
    setupMainMenuOption(catPopup, new CwDialogBox(constants),
        images.catPopups());

    // Panels
    TreeItem catPanels = mainMenu.addItem(constants.categoryPanels());
    setupMainMenuOption(catPanels, new CwDecoratorPanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwFlowPanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwHorizontalPanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwVerticalPanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwAbsolutePanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwDockPanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwDisclosurePanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwTabPanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwHorizontalSplitPanel(constants),
        images.catPanels());
    setupMainMenuOption(catPanels, new CwVerticalSplitPanel(constants),
        images.catPanels());

    // Tables
    TreeItem catTables = mainMenu.addItem(constants.categoryTables());
    setupMainMenuOption(catTables, new CwGrid(constants), images.catTables());
    setupMainMenuOption(catTables, new CwFlexTable(constants),
        images.catTables());

    // Internationalization
    TreeItem catI18N = mainMenu.addItem(constants.categoryI18N());
    setupMainMenuOption(catI18N, new CwNumberFormat(constants),
        images.catI18N());
    setupMainMenuOption(catI18N, new CwDateTimeFormat(constants),
        images.catI18N());
    setupMainMenuOption(catI18N, new CwMessagesExample(constants),
        images.catI18N());
    setupMainMenuOption(catI18N, new CwConstantsExample(constants),
        images.catI18N());
    setupMainMenuOption(catI18N, new CwConstantsWithLookupExample(constants),
        images.catI18N());
    setupMainMenuOption(catI18N, new CwDictionaryExample(constants),
        images.catI18N());

    // Other
    TreeItem catOther = mainMenu.addItem(constants.categoryOther());
    setupMainMenuOption(catOther, new CwAnimation(constants), images.catOther());
    setupMainMenuOption(catOther, new CwFrame(constants), images.catOther());
    setupMainMenuOption(catOther, new CwCookies(constants), images.catOther());
  }

  /**
   * Add an option to the main menu.
   * 
   * @param parent the {@link TreeItem} that is the option
   * @param content the {@link ContentWidget} to display when selected
   * @param image the icon to display next to the {@link TreeItem}
   */
  private void setupMainMenuOption(TreeItem parent, ContentWidget content,
      AbstractImagePrototype image) {
    // Create the TreeItem
    TreeItem option = parent.addItem(image.getHTML() + " " + content.getName());

    // Map the item to its history token and content widget
    itemWidgets.put(option, content);
    itemTokens.put(getContentWidgetToken(content), option);
  }

  /**
   * Create the options that appear next to the title.
   * 
   * @param constants the constant values to use
   */
  private void setupOptionsPanel(ShowcaseConstants constants) {
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    app.setOptionsWidget(vPanel);
    
    // Add the option to change the locale
    final ListBox localeBox = new ListBox();
    String currentLocale = LocaleInfo.getCurrentLocale().getLocaleName();
    if (currentLocale.equals("default")) {
      currentLocale = "en";
    }
    String[] localeNames = LocaleInfo.getAvailableLocaleNames();
    for (String localeName : localeNames) {
      if (!localeName.equals("default")) {
        String nativeName = LocaleInfo.getLocaleNativeDisplayName(localeName);
        localeBox.addItem(nativeName, localeName);
        if (localeName.equals(currentLocale)) {
          localeBox.setSelectedIndex(localeBox.getItemCount() - 1);
        }
      }
    }
    localeBox.addChangeListener(new ChangeListener() {
      public void onChange(Widget sender) {
        String localeName = localeBox.getValue(localeBox.getSelectedIndex());
        Window.open(getHostPageLocation() + "?locale=" + localeName, "_self",
            "");
      }
    });
    HorizontalPanel localeWrapper = new HorizontalPanel();
    localeWrapper.add(images.locale().createImage());
    localeWrapper.add(localeBox);
    vPanel.add(localeWrapper);
    
    // Add the option to change the style
    HorizontalPanel styleWrapper = new HorizontalPanel();
    vPanel.add(styleWrapper);
    for (int i = 0; i < STYLE_THEMES.length; i++) {
      String theme = STYLE_THEMES[i];
      PushButton button = new PushButton();
      button.addStyleName("sc-ThemeButton-" + theme);
      styleWrapper.add(button);
      button.addClickListener(new ClickListener() {
        public void onClick(Widget sender) {
          Window.alert("Additional styles coming soon...");
        }
      });
    }
  }
  
  /**
   * Create the title bar at the top of the application.
   * 
   * @param constants the constant values to use
   */
  private void setupTitlePanel(ShowcaseConstants constants) {
    // Get the title from the internationalized constants
    String pageTitle = "<h1>" + constants.mainTitle() + "</h1><h2>"
        + constants.mainSubTitle() + "</h2>";

    // Add the title and some images to the title bar
    HorizontalPanel titlePanel = new HorizontalPanel();
    titlePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    titlePanel.add(images.gwtLogo().createImage());
    titlePanel.add(new HTML(pageTitle));
    app.setTitleWidget(titlePanel);
  }
}
