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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
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
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Showcase implements EntryPoint {
  /**
   * The type passed into the
   * {@link com.google.gwt.sample.showcase.generator.ShowcaseGenerator}.
   */
  private static final class GeneratorInfo {
  }

  /**
   * A special version of the ToggleButton that cannot be clicked if down. If
   * one theme button is pressed, all of the others are depressed.
   */
  private static class ThemeButton extends ToggleButton {
    private static List<ThemeButton> allButtons = null;

    private String theme;

    public ThemeButton(String theme) {
      super();
      this.theme = theme;
      addStyleName("sc-ThemeButton-" + theme);

      // Add this button to the static list
      if (allButtons == null) {
        allButtons = new ArrayList<ThemeButton>();
        setDown(true);
      }
      allButtons.add(this);
    }

    public String getTheme() {
      return theme;
    }

    @Override
    protected void onClick() {
      if (!isDown()) {
        // Raise all of the other buttons
        for (ThemeButton button : allButtons) {
          if (button != this) {
            button.setDown(false);
          }
        }

        // Fire the click listeners
        super.onClick();
      }
    }
  }

  /**
   * The static images used throughout the Showcase.
   */
  public static final ShowcaseImages images = (ShowcaseImages) GWT.create(ShowcaseImages.class);

  /**
   * The current style theme.
   */
  public static String CUR_THEME = ShowcaseConstants.STYLE_THEMES[0];

  /**
   * Convenience method for getting the document's head element.
   * 
   * @return the document's head element
   */
  private static native HeadElement getHeadElement() /*-{
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
   * A small widget used to determine when a new style sheet has finished
   * loading. The widget has a natural width of 0px, but when any GWT.css style
   * sheet is loaded, the width changes to 5px. We use a Timer to check the
   * width until the style sheet loads.
   */
  private Label styleTester;

  /**
   * The timer that uses the styleTester to determine when the new GWT style
   * sheet has loaded.
   */
  private Timer styleTesterTimer = new Timer() {
    @Override
    public void run() {
      styleTester.setVisible(false);
      styleTester.setVisible(true);
      if (styleTester.getOffsetWidth() > 0) {
        RootPanel.getBodyElement().getStyle().setProperty("display", "none");
        RootPanel.getBodyElement().getStyle().setProperty("display", "");
      } else {
        schedule(25);
      }
    }
  };

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    // Generate the source code and css for the examples
    GWT.create(GeneratorInfo.class);

    // Create a widget to test when style sheets are loaded
    styleTester = new HTML("<div class=\"topLeftInner\"></div>");
    styleTester.setStyleName("gwt-DecoratorPanel");
    styleTester.getElement().getStyle().setProperty("position", "absolute");
    styleTester.getElement().getStyle().setProperty("visibility", "hidden");
    styleTester.getElement().getStyle().setProperty("display", "inline");
    styleTester.getElement().getStyle().setPropertyPx("padding", 0);
    styleTester.getElement().getStyle().setPropertyPx("top", 0);
    styleTester.getElement().getStyle().setPropertyPx("left", 0);
    RootPanel.get().add(styleTester);

    // Create the constants
    ShowcaseConstants constants = (ShowcaseConstants) GWT.create(ShowcaseConstants.class);

    // Create the application
    app = new Application();
    setupTitlePanel(constants);
    setupMainLinks(constants);
    setupOptionsPanel();
    setupMainMenu(constants);
    RootPanel.get().add(app);

    // Swap out the style sheets for the RTL versions if needed. We need to do
    // this after the app is loaded because the app will setup the layout based
    // on the width of the main menu, which is defined in the style sheet. If
    // we swap the style sheets first, the app may load without any style sheet
    // to define the main menu width, because the RTL version is still being
    // loaded. Note that we are basing the layout on the width defined in the
    // LTR version, so both versions should use the same width for the main nav
    // menu.
    updateStyleSheets();

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
   * Create a new {@link LinkElement} that links to a style sheet and append it
   * to the head element.
   * 
   * @param href the path to the style sheet
   */
  private void loadStyleSheet(String href) {
    LinkElement linkElem = Document.get().createLinkElement();
    linkElem.setRel("stylesheet");
    linkElem.setType("text/css");
    linkElem.setHref(href);
    getHeadElement().appendChild(linkElem);
  }

  /**
   * Create the main links at the top of the application.
   * 
   * @param constants the constants with text
   */
  private void setupMainLinks(ShowcaseConstants constants) {
    // Link to GWT Homepage
    app.addLink(new HTML("<a href=\"" + ShowcaseConstants.GWT_HOMEPAGE + "\">"
        + constants.mainLinkHomepage() + "</a>"));

    // Link to More Examples
    app.addLink(new HTML("<a href=\"" + ShowcaseConstants.GWT_EXAMPLES + "\">"
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
   */
  private void setupOptionsPanel() {
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
    final HorizontalPanel styleWrapper = new HorizontalPanel();
    vPanel.add(styleWrapper);
    for (int i = 0; i < ShowcaseConstants.STYLE_THEMES.length; i++) {
      final ThemeButton button = new ThemeButton(
          ShowcaseConstants.STYLE_THEMES[i]);
      styleWrapper.add(button);
      button.addClickListener(new ClickListener() {
        public void onClick(Widget sender) {
          // Update the current theme
          CUR_THEME = button.getTheme();

          // Reload the current tab, loading the new theme if necessary
          TabBar bar = ((TabBar) app.getContentTitle());
          bar.selectTab(bar.getSelectedTab());

          // Load the new style sheets
          updateStyleSheets();
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

  /**
   * Update the style sheets to reflect the current theme and direction.
   */
  private void updateStyleSheets() {
    // Generate the names of the style sheets to include
    String gwtStyleSheet = "gwt/" + CUR_THEME + "/" + CUR_THEME + ".css";
    String showcaseStyleSheet = CUR_THEME + "/Showcase.css";
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      gwtStyleSheet = gwtStyleSheet.replace(".css", "_rtl.css");
      showcaseStyleSheet = showcaseStyleSheet.replace(".css", "_rtl.css");
    }

    // Remove existing style sheets
    HeadElement headElem = getHeadElement();
    NodeList<Node> children = headElem.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.getItem(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element elem = Element.as(node);
        if (elem.getTagName().equalsIgnoreCase("link")
            && elem.getPropertyString("rel").equalsIgnoreCase("stylesheet")) {
          String href = elem.getPropertyString("href");
          // If the style sheet is already loaded, we keep it and set
          // gwtStyleSheet to null so that we do not load it below.  The same
          // applies to showcaseStyleSheet.
          if (gwtStyleSheet != null && href.contains(gwtStyleSheet)) {
            gwtStyleSheet = null;
          } else if (showcaseStyleSheet != null
              && href.contains(showcaseStyleSheet)) {
            showcaseStyleSheet = null;
          } else {
            headElem.removeChild(elem);
            i--;
          }
        }
      }
    }

    // Add the new style sheets
    String modulePath = GWT.getModuleBaseURL();
    if (gwtStyleSheet != null) {
      styleTesterTimer.schedule(25);
      loadStyleSheet(modulePath + gwtStyleSheet);
    }
    if (showcaseStyleSheet != null) {
      loadStyleSheet(modulePath + showcaseStyleSheet);
    }
  }
}
