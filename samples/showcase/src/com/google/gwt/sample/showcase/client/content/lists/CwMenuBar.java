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
package com.google.gwt.sample.showcase.client.content.lists;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle({
    ".gwt-MenuBar", ".gwt-MenuBarPopup", "html>body .gwt-MenuBarPopup",
    "* html .gwt-MenuBarPopup"})
public class CwMenuBar extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwMenuBarDescription();

    String cwMenuBarEditCategory();

    String[] cwMenuBarEditOptions();

    String cwMenuBarFileCategory();

    String[] cwMenuBarFileOptions();

    String[] cwMenuBarFileRecents();

    String[] cwMenuBarGWTOptions();

    String cwMenuBarHelpCategory();

    String[] cwMenuBarHelpOptions();

    String cwMenuBarName();

    String[] cwMenuBarPrompts();
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwMenuBar(CwConstants constants) {
    super(constants.cwMenuBarName(), constants.cwMenuBarDescription(), true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a command that will execute on menu item selection
    Command menuCommand = new Command() {
      private int curPhrase = 0;
      private final String[] phrases = constants.cwMenuBarPrompts();

      public void execute() {
        Window.alert(phrases[curPhrase]);
        curPhrase = (curPhrase + 1) % phrases.length;
      }
    };

    // Create a menu bar
    MenuBar menu = new MenuBar();
    menu.setAutoOpen(true);
    menu.setWidth("500px");
    menu.setAnimationEnabled(true);

    // Create a sub menu of recent documents
    MenuBar recentDocsMenu = new MenuBar(true);
    String[] recentDocs = constants.cwMenuBarFileRecents();
    for (int i = 0; i < recentDocs.length; i++) {
      recentDocsMenu.addItem(recentDocs[i], menuCommand);
    }

    // Create the file menu
    MenuBar fileMenu = new MenuBar(true);
    fileMenu.setAnimationEnabled(true);
    menu.addItem(new MenuItem(constants.cwMenuBarFileCategory(), fileMenu));
    String[] fileOptions = constants.cwMenuBarFileOptions();
    for (int i = 0; i < fileOptions.length; i++) {
      if (i == 3) {
        fileMenu.addSeparator();
        fileMenu.addItem(fileOptions[i], recentDocsMenu);
        fileMenu.addSeparator();
      } else {
        fileMenu.addItem(fileOptions[i], menuCommand);
      }
    }

    // Create the edit menu
    MenuBar editMenu = new MenuBar(true);
    menu.addItem(new MenuItem(constants.cwMenuBarEditCategory(), editMenu));
    String[] editOptions = constants.cwMenuBarEditOptions();
    for (int i = 0; i < editOptions.length; i++) {
      editMenu.addItem(editOptions[i], menuCommand);
    }

    // Create the GWT menu
    MenuBar gwtMenu = new MenuBar(true);
    menu.addItem(new MenuItem("GWT", true, gwtMenu));
    String[] gwtOptions = constants.cwMenuBarGWTOptions();
    for (int i = 0; i < gwtOptions.length; i++) {
      gwtMenu.addItem(gwtOptions[i], menuCommand);
    }

    // Create the help menu
    MenuBar helpMenu = new MenuBar(true);
    menu.addSeparator();
    menu.addItem(new MenuItem(constants.cwMenuBarHelpCategory(), helpMenu));
    String[] helpOptions = constants.cwMenuBarHelpOptions();
    for (int i = 0; i < helpOptions.length; i++) {
      helpMenu.addItem(helpOptions[i], menuCommand);
    }

    // Return the menu
    menu.ensureDebugId("cwMenuBar");
    return menu;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwMenuBar.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
