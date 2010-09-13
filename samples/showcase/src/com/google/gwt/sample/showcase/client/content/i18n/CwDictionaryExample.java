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
package com.google.gwt.sample.showcase.client.content.i18n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Set;

/**
 * Example file.
 */
@ShowcaseStyle(".cw-DictionaryExample")
public class CwDictionaryExample extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwDictionaryExampleDescription();

    String cwDictionaryExampleLinkText();

    String cwDictionaryExampleName();
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
  public CwDictionaryExample(CwConstants constants) {
    super(constants.cwDictionaryExampleName(),
        constants.cwDictionaryExampleDescription(), true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a vertical panel to layout the contents
    VerticalPanel layout = new VerticalPanel();

    // Show the HTML variable that defines the dictionary
    HTML source = new HTML(
        "<pre>var userInfo = {\n" + "&nbsp;&nbsp;name: \"Amelie Crutcher\",\n"
            + "&nbsp;&nbsp;timeZone: \"EST\",\n"
            + "&nbsp;&nbsp;userID: \"123\",\n"
            + "&nbsp;&nbsp;lastLogOn: \"2/2/2006\"\n" + "};</pre>\n");
    source.getElement().setDir("ltr");
    source.getElement().getStyle().setProperty("textAlign", "left");
    layout.add(new HTML(constants.cwDictionaryExampleLinkText()));
    layout.add(source);

    // Create the Dictionary of data
    FlexTable userInfoGrid = new FlexTable();
    CellFormatter formatter = userInfoGrid.getCellFormatter();
    Dictionary userInfo = Dictionary.getDictionary("userInfo");
    Set<String> keySet = userInfo.keySet();
    int columnCount = 0;
    for (String key : keySet) {
      // Get the value from the set
      String value = userInfo.get(key);

      // Add a column with the data
      userInfoGrid.setHTML(0, columnCount, key);
      formatter.addStyleName(0, columnCount, "cw-DictionaryExample-header");
      userInfoGrid.setHTML(1, columnCount, value);
      formatter.addStyleName(1, columnCount, "cw-DictionaryExample-data");

      // Go to the next column
      columnCount++;
    }
    layout.add(new HTML("<br><br>"));
    layout.add(userInfoGrid);

    // Return the layout Widget
    return layout;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwDictionaryExample.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
