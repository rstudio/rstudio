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
package com.google.gwt.sample.showcase.client.content.other;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collection;
import java.util.Date;

/**
 * Example file.
 */
public class CwCookies extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwCookiesDeleteCookie();

    String cwCookiesDescription();

    String cwCookiesExistingLabel();

    String cwCookiesInvalidCookie();

    String cwCookiesName();

    String cwCookiesNameLabel();

    String cwCookiesSetCookie();

    String cwCookiesValueLabel();
  }

  /**
   * The timeout before a cookie expires, in milliseconds. Current one day.
   */
  @ShowcaseData
  private static final int COOKIE_TIMEOUT = 1000 * 60 * 60 * 24;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * A {@link TextBox} that holds the name of the cookie.
   */
  @ShowcaseData
  private TextBox cookieNameBox = null;

  /**
   * A {@link TextBox} that holds the value of the cookie.
   */
  @ShowcaseData
  private TextBox cookieValueBox = null;

  /**
   * The {@link ListBox} containing existing cookies.
   */
  @ShowcaseData
  private ListBox existingCookiesBox = null;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwCookies(CwConstants constants) {
    super(constants.cwCookiesName(), constants.cwCookiesDescription(), false);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create the panel used to layout the content
    Grid mainLayout = new Grid(3, 3);

    // Display the existing cookies
    existingCookiesBox = new ListBox();
    Button deleteCookieButton = new Button(constants.cwCookiesDeleteCookie());
    deleteCookieButton.addStyleName("sc-FixedWidthButton");
    mainLayout.setHTML(
        0, 0, "<b>" + constants.cwCookiesExistingLabel() + "</b>");
    mainLayout.setWidget(0, 1, existingCookiesBox);
    mainLayout.setWidget(0, 2, deleteCookieButton);

    // Display the name of the cookie
    cookieNameBox = new TextBox();
    mainLayout.setHTML(1, 0, "<b>" + constants.cwCookiesNameLabel() + "</b>");
    mainLayout.setWidget(1, 1, cookieNameBox);

    // Display the name of the cookie
    cookieValueBox = new TextBox();
    Button setCookieButton = new Button(constants.cwCookiesSetCookie());
    setCookieButton.addStyleName("sc-FixedWidthButton");
    mainLayout.setHTML(2, 0, "<b>" + constants.cwCookiesValueLabel() + "</b>");
    mainLayout.setWidget(2, 1, cookieValueBox);
    mainLayout.setWidget(2, 2, setCookieButton);

    // Add a handler to set the cookie value
    setCookieButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        String name = cookieNameBox.getText();
        String value = cookieValueBox.getText();
        Date expires = new Date((new Date()).getTime() + COOKIE_TIMEOUT);

        // Verify the name is valid
        if (name.length() < 1) {
          Window.alert(constants.cwCookiesInvalidCookie());
          return;
        }

        // Set the cookie value
        Cookies.setCookie(name, value, expires);
        refreshExistingCookies(name);
      }
    });

    // Add a handler to select an existing cookie
    existingCookiesBox.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        updateExstingCookie();
      }
    });

    // Add a handler to delete an existing cookie
    deleteCookieButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        int selectedIndex = existingCookiesBox.getSelectedIndex();
        if (selectedIndex > -1
            && selectedIndex < existingCookiesBox.getItemCount()) {
          String cookieName = existingCookiesBox.getValue(selectedIndex);
          Cookies.removeCookie(cookieName);
          existingCookiesBox.removeItem(selectedIndex);
          updateExstingCookie();
        }
      }
    });

    // Return the main layout
    refreshExistingCookies(null);
    return mainLayout;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwCookies.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  /**
   * Refresh the list of existing cookies.
   *
   * @param selectedCookie the cookie to select by default
   */
  @ShowcaseSource
  private void refreshExistingCookies(String selectedCookie) {
    // Clear the existing cookies
    existingCookiesBox.clear();

    // Add the cookies
    int selectedIndex = 0;
    Collection<String> cookies = Cookies.getCookieNames();
    for (String cookie : cookies) {
      existingCookiesBox.addItem(cookie);
      if (cookie.equals(selectedCookie)) {
        selectedIndex = existingCookiesBox.getItemCount() - 1;
      }
    }

    // Select the index of the selectedCookie. Use a ScheduledCommand to give
    // the options time to register in Opera.
    final int selectedIndexFinal = selectedIndex;
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        // Select the default cookie
        if (selectedIndexFinal < existingCookiesBox.getItemCount()) {
          existingCookiesBox.setSelectedIndex(selectedIndexFinal);
        }

        // Display the selected cookie value
        updateExstingCookie();
      }
    });
  }

  /**
   * Retrieve the value of the existing cookie and put it into to value label.
   */
  @ShowcaseSource
  private void updateExstingCookie() {
    // Cannot update if there are no items
    if (existingCookiesBox.getItemCount() < 1) {
      cookieNameBox.setText("");
      cookieValueBox.setText("");
      return;
    }

    int selectedIndex = existingCookiesBox.getSelectedIndex();
    String cookieName = existingCookiesBox.getValue(selectedIndex);
    String cookieValue = Cookies.getCookie(cookieName);
    cookieNameBox.setText(cookieName);
    cookieValueBox.setText(cookieValue);
  }
}
