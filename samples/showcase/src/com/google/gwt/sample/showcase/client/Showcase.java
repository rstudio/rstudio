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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.LocaleInfo;
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
import com.google.gwt.sample.showcase.client.content.widgets.CwDatePicker;
import com.google.gwt.sample.showcase.client.content.widgets.CwFileUpload;
import com.google.gwt.sample.showcase.client.content.widgets.CwHyperlink;
import com.google.gwt.sample.showcase.client.content.widgets.CwRadioButton;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

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

        // Fire the click handlers
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
  static String CUR_THEME = ShowcaseConstants.STYLE_THEMES[0];

  /**
   * Get the URL of the page, without an hash of query string.
   * 
   * @return the location of the page
   */
  private static native String getHostPageLocation()
  /*-{
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
  private Application app = new Application();

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
    // Generate the source code and css for the examples
    GWT.create(GeneratorInfo.class);

    // Create the constants
    ShowcaseConstants constants = (ShowcaseConstants) GWT.create(ShowcaseConstants.class);

    // Swap out the style sheets for the RTL versions if needed.
    updateStyleSheets();

    // Create the application
    setupTitlePanel(constants);
    setupMainLinks(constants);
    setupOptionsPanel();
    setupMainMenu(constants);

    // Setup a history handler to reselect the associate menu item
    final ValueChangeHandler<String> historyHandler = new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
        TreeItem item = itemTokens.get(event.getValue());
        if (item == null) {
          item = app.getMainMenu().getItem(0).getChild(0);
        }

        // Select the associated TreeItem
        app.getMainMenu().setSelectedItem(item, false);
        app.getMainMenu().ensureSelectedItemVisible();

        // Show the associated ContentWidget
        displayContentWidget(itemWidgets.get(item));
      }
    };
    History.addValueChangeHandler(historyHandler);

    // Add a handler that sets the content widget when a menu item is selected
    app.addSelectionHandler(new SelectionHandler<TreeItem>() {
      public void onSelection(SelectionEvent<TreeItem> event) {
        TreeItem item = event.getSelectedItem();
        ContentWidget content = itemWidgets.get(item);
        if (content != null && !content.equals(app.getContent())) {
          History.newItem(getContentWidgetToken(content));
        }
      }
    });

    // Show the initial example
    if (History.getToken().length() > 0) {
      History.fireCurrentHistoryState();
    } else {
      // Use the first token available
      TreeItem firstItem = app.getMainMenu().getItem(0).getChild(0);
      app.getMainMenu().setSelectedItem(firstItem, false);
      app.getMainMenu().ensureSelectedItemVisible();
      displayContentWidget(itemWidgets.get(firstItem));
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
   * Get the style name of the reference element defined in the current GWT
   * theme style sheet.
   * 
   * @param prefix the prefix of the reference style name
   * @return the style name
   */
  private String getCurrentReferenceStyleName(String prefix) {
    String gwtRef = prefix + "-Reference-" + CUR_THEME;
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      gwtRef += "-rtl";
    }
    return gwtRef;
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
    setupMainMenuOption(catWidgets, new CwDatePicker(constants),
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
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      vPanel.getElement().setAttribute("align", "left");
    } else {
      vPanel.getElement().setAttribute("align", "right");
    }
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
    localeBox.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
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
      button.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
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

    // Find existing style sheets that need to be removed
    boolean styleSheetsFound = false;
    final HeadElement headElem = StyleSheetLoader.getHeadElement();
    final List<Element> toRemove = new ArrayList<Element>();
    NodeList<Node> children = headElem.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.getItem(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element elem = Element.as(node);
        if (elem.getTagName().equalsIgnoreCase("link")
            && elem.getPropertyString("rel").equalsIgnoreCase("stylesheet")) {
          styleSheetsFound = true;
          String href = elem.getPropertyString("href");
          // If the correct style sheets are already loaded, then we should have
          // nothing to remove.
          if (!href.contains(gwtStyleSheet)
              && !href.contains(showcaseStyleSheet)) {
            toRemove.add(elem);
          }
        }
      }
    }

    // Return if we already have the correct style sheets
    if (styleSheetsFound && toRemove.size() == 0) {
      return;
    }

    // Detach the app while we manipulate the styles to avoid rendering issues
    RootPanel.get().remove(app);

    // Remove the old style sheets
    for (Element elem : toRemove) {
      headElem.removeChild(elem);
    }

    // Load the GWT theme style sheet
    String modulePath = GWT.getModuleBaseURL();
    Command callback = new Command() {
      /**
       * The number of style sheets that have been loaded and executed this
       * command.
       */
      private int numStyleSheetsLoaded = 0;

      public void execute() {
        // Wait until all style sheets have loaded before re-attaching the app
        numStyleSheetsLoaded++;
        if (numStyleSheetsLoaded < 2) {
          return;
        }

        // Different themes use different background colors for the body
        // element, but IE only changes the background of the visible content
        // on the page instead of changing the background color of the entire
        // page. By changing the display style on the body element, we force
        // IE to redraw the background correctly.
        RootPanel.getBodyElement().getStyle().setProperty("display", "none");
        RootPanel.getBodyElement().getStyle().setProperty("display", "");
        RootPanel.get().add(app);
      }
    };
    StyleSheetLoader.loadStyleSheet(modulePath + gwtStyleSheet,
        getCurrentReferenceStyleName("gwt"), callback);

    // Load the showcase specific style sheet after the GWT theme style sheet so
    // that custom styles supercede the theme styles.
    StyleSheetLoader.loadStyleSheet(modulePath + showcaseStyleSheet,
        getCurrentReferenceStyleName("Application"), callback);
  }
}
