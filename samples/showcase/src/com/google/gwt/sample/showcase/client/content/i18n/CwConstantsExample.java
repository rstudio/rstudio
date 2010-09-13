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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;

/**
 * Example file.
 */
@ShowcaseRaw({"ExampleConstants.java", "ExampleConstants.properties"})
public class CwConstantsExample extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwConstantsExampleDescription();

    String cwConstantsExampleLinkText();

    String cwConstantsExampleName();
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
  public CwConstantsExample(CwConstants constants) {
    super(constants.cwConstantsExampleName(),
        constants.cwConstantsExampleDescription(), false,
        "ExampleConstants.java", "ExampleConstants.properties");
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create the internationalized constants
    ExampleConstants exampleConstants = GWT.create(ExampleConstants.class);

    // Use a FlexTable to layout the content
    FlexTable layout = new FlexTable();
    FlexCellFormatter formatter = layout.getFlexCellFormatter();
    layout.setCellSpacing(5);

    // Add a link to the source code of the Interface
    final String rawFile = getSimpleName(ExampleConstants.class);
    Anchor link = new Anchor(rawFile);
    link.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        fireRawSourceRequest(rawFile + ".java");
      }
    });
    HorizontalPanel linkPanel = new HorizontalPanel();
    linkPanel.setSpacing(3);
    linkPanel.add(new HTML(constants.cwConstantsExampleLinkText()));
    linkPanel.add(link);
    layout.setWidget(0, 0, linkPanel);
    formatter.setColSpan(0, 0, 2);

    // Show the first name
    TextBox firstNameBox = new TextBox();
    firstNameBox.setText("Amelie");
    firstNameBox.setWidth("17em");
    layout.setHTML(1, 0, exampleConstants.firstName());
    layout.setWidget(1, 1, firstNameBox);

    // Show the last name
    TextBox lastNameBox = new TextBox();
    lastNameBox.setText("Crutcher");
    lastNameBox.setWidth("17em");
    layout.setHTML(2, 0, exampleConstants.lastName());
    layout.setWidget(2, 1, lastNameBox);

    // Create a list box of favorite colors
    ListBox colorBox = new ListBox();
    Map<String, String> colorMap = exampleConstants.colorMap();
    for (Map.Entry<String, String> entry : colorMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      colorBox.addItem(value, key);
    }
    layout.setHTML(3, 0, exampleConstants.favoriteColor());
    layout.setWidget(3, 1, colorBox);

    // Return the layout Widget
    return layout;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwConstantsExample.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
