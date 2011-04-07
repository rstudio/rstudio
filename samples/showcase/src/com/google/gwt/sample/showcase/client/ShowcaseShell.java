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
package com.google.gwt.sample.showcase.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.TreeViewModel;

import java.util.Date;
import java.util.List;

/**
 * Application shell for Showcase sample.
 */
public class ShowcaseShell extends ResizeComposite {

  interface ShowcaseShellUiBinder extends UiBinder<Widget, ShowcaseShell> {
  }

  /**
   * The callback used when retrieving source code.
   */
  private class CustomCallback implements ContentWidget.Callback<String> {

    private int id;

    public CustomCallback() {
      id = ++nextCallbackId;
    }

    public void onError() {
      if (id == nextCallbackId) {
        contentSource.setHTML("Cannot find resource", Direction.LTR);
      }
    }

    public void onSuccess(String value) {
      if (id == nextCallbackId) {
        contentSource.setHTML(value, Direction.LTR);
      }
    }
  }

  /**
   * The text color of the selected tab.
   */
  private static final String SELECTED_TAB_COLOR = "#333333";

  /**
   * The unique ID assigned to the next callback.
   */
  private static int nextCallbackId = 0;

  private static ShowcaseShellUiBinder uiBinder = GWT.create(
      ShowcaseShellUiBinder.class);

  /**
   * The panel that holds the content.
   */
  @UiField
  SimpleLayoutPanel contentPanel;

  /**
   * The container around the links at the top of the app.
   */
  @UiField
  TableElement linkCell;

  /**
   * A drop box used to change the locale.
   */
  @UiField
  ListBox localeBox;

  /**
   * The container around locale selection.
   */
  @UiField
  TableCellElement localeSelectionCell;

  /**
   * The main menu used to navigate to examples.
   */
  @UiField(provided = true)
  CellTree mainMenu;

  /**
   * The button used to show the example.
   */
  @UiField
  Anchor tabExample;

  /**
   * The button used to show the source CSS style.
   */
  @UiField
  Anchor tabStyle;

  /**
   * The button used to show the source code.
   */
  @UiField
  Anchor tabSource;

  /**
   * The list of available source code.
   */
  @UiField
  ListBox tabSourceList;

  /**
   * The current {@link ContentWidget} being displayed.
   */
  private ContentWidget content;

  /**
   * The handler used to handle user requests to view raw source.
   */
  private HandlerRegistration contentSourceHandler;

  /**
   * The widget that holds CSS or source code for an example.
   */
  private HTML contentSource = new HTML();

  /**
   * The html used to show a loading icon.
   */
  private final String loadingHtml;

  /**
   * Construct the {@link ShowcaseShell}.
   *
   * @param treeModel the treeModel that backs the main menu
   */
  public ShowcaseShell(TreeViewModel treeModel) {
    AbstractImagePrototype proto = AbstractImagePrototype.create(
        Showcase.images.loading());
    loadingHtml = proto.getHTML();

    // Create the cell tree.
    mainMenu = new CellTree(treeModel, null);
    mainMenu.setAnimationEnabled(true);
    mainMenu.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    mainMenu.ensureDebugId("mainMenu");

    // Initialize the ui binder.
    initWidget(uiBinder.createAndBindUi(this));
    initializeLocaleBox();
    contentSource.getElement().getStyle().setBackgroundColor("#eee");
    contentSource.getElement().getStyle().setMargin(10.0, Unit.PX);
    contentSource.getElement().getStyle().setProperty(
        "border", "1px solid #c3c3c3");
    contentSource.getElement().getStyle().setProperty("padding", "10px 2px");

    // In RTL mode, we need to set some attributes.
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      localeSelectionCell.setAlign("left");
      linkCell.setPropertyString("align", "left");
    }

    // Handle events from the tabs.
    tabExample.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        showExample();
      }
    });
    tabStyle.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        showSourceStyles();
      }
    });
    tabSource.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        showSourceFile();
      }
    });
    tabSourceList.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        showSourceFile();
      }
    });

    // Default to no content.
    contentPanel.ensureDebugId("contentPanel");
    setContent(null);
  }

  /**
   * Returns the currently displayed content. (Used by tests.)
   */
  public ContentWidget getContent() {
    return content;
  }

  /**
   * Get the main menu used to select examples.
   *
   * @return the main menu
   */
  public CellTree getMainMenu() {
    return mainMenu;
  }

  /**
   * Set the content to display.
   *
   * @param content the content
   */
  public void setContent(final ContentWidget content) {
    // Clear the old handler.
    if (contentSourceHandler != null) {
      contentSourceHandler.removeHandler();
      contentSourceHandler = null;
    }

    this.content = content;
    if (content == null) {
      tabExample.setVisible(false);
      tabStyle.setVisible(false);
      tabSource.setVisible(false);
      tabSourceList.setVisible(false);
      contentPanel.setWidget(null);
      return;
    }

    // Setup the options bar.
    tabExample.setVisible(true);
    tabStyle.setVisible(content.hasStyle());
    tabSource.setVisible(true);

    /*
     * Show the list of raw source files if there are any. We need to add at
     * least one option to the list for crawlability. If we do not, HtmlUnit
     * innerHtml will close the select tag in the open tag (ie, use a forward
     * slash instead of a separate close tag) which most browsers parse
     * incorrectly.
     */
    tabSourceList.clear();
    tabSourceList.addItem("Example");
    List<String> rawFilenames = content.getRawSourceFilenames();
    if (rawFilenames.size() > 0) {
      String text = tabSource.getText();
      if (!text.endsWith(":")) {
        tabSource.setText(text + ":");
      }
      tabSourceList.setVisible(true);
      for (String filename : rawFilenames) {
        tabSourceList.addItem(filename);
      }
      tabSourceList.setSelectedIndex(0);
    } else {
      String text = tabSource.getText();
      if (text.endsWith(":")) {
        tabSource.setText(text.substring(0, text.length() - 1));
      }
      tabSourceList.setVisible(false);
    }

    // Handle user requests for raw source.
    contentSourceHandler = content.addValueChangeHandler(
        new ValueChangeHandler<String>() {
          public void onValueChange(ValueChangeEvent<String> event) {
            // Select the file in the list box.
            String filename = event.getValue();
            int index = content.getRawSourceFilenames().indexOf(filename);
            tabSourceList.setSelectedIndex(index + 1);

            // Show the file.
            showSourceFile();
          }
        });

    // Show the widget.
    showExample();
  }

  /**
   * Initialize the {@link ListBox} used for locale selection.
   */
  private void initializeLocaleBox() {
    final String cookieName = LocaleInfo.getLocaleCookieName();
    final String queryParam = LocaleInfo.getLocaleQueryParam();
    if (cookieName == null && queryParam == null) {
      // if there is no way for us to affect the locale, don't show the selector
      localeSelectionCell.getStyle().setDisplay(Display.NONE);
      return;
    }
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
      @SuppressWarnings("deprecation")
      public void onChange(ChangeEvent event) {
        String localeName = localeBox.getValue(localeBox.getSelectedIndex());
        if (cookieName != null) {
          // expire in one year
          Date expires = new Date();
          expires.setYear(expires.getYear() + 1);
          Cookies.setCookie(cookieName, localeName, expires);
        }
        if (queryParam != null) {
          UrlBuilder builder = Location.createUrlBuilder().setParameter(
              queryParam, localeName);
          Window.Location.replace(builder.buildString());
        } else {
          // If we are using only cookies, just reload
          Window.Location.reload();
        }
      }
    });
  }

  /**
   * Show a example.
   */
  private void showExample() {
    if (content == null) {
      return;
    }

    // Set the highlighted tab.
    tabExample.getElement().getStyle().setColor(SELECTED_TAB_COLOR);
    tabStyle.getElement().getStyle().clearColor();
    tabSource.getElement().getStyle().clearColor();

    contentPanel.setWidget(content);
  }

  /**
   * Show a source file based on the selection in the source list.
   */
  private void showSourceFile() {
    if (content == null) {
      return;
    }

    // Set the highlighted tab.
    tabExample.getElement().getStyle().clearColor();
    tabStyle.getElement().getStyle().clearColor();
    tabSource.getElement().getStyle().setColor(SELECTED_TAB_COLOR);

    contentSource.setHTML(loadingHtml, Direction.LTR);
    contentPanel.setWidget(new ScrollPanel(contentSource));
    if (!tabSourceList.isVisible() || tabSourceList.getSelectedIndex() == 0) {
      // If the source list isn't visible or the first item is selected, load
      // the source for the example.
      content.getSource(new CustomCallback());
    } else {
      // Load a raw file.
      String filename = tabSourceList.getItemText(
          tabSourceList.getSelectedIndex());
      content.getRawSource(filename, new CustomCallback());
    }
  }

  /**
   * Show the source CSS style.
   */
  private void showSourceStyles() {
    if (content == null) {
      return;
    }

    // Set the highlighted tab.
    tabExample.getElement().getStyle().clearColor();
    tabStyle.getElement().getStyle().setColor(SELECTED_TAB_COLOR);
    tabSource.getElement().getStyle().clearColor();

    contentSource.setHTML(loadingHtml, Direction.LTR);
    contentPanel.setWidget(new ScrollPanel(contentSource));
    content.getStyle(new CustomCallback());
  }
}
