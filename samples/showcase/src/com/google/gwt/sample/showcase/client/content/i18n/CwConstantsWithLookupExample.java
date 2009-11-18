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
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseConstants;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

import java.util.MissingResourceException;

/**
 * Example file.
 */
@ShowcaseRaw( {"ColorConstants.java", "ColorConstants.properties"})
public class CwConstantsWithLookupExample extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwConstantsWithLookupExampleDescription();

    String cwConstantsWithLookupExampleLinkText();

    String cwConstantsWithLookupExampleMethodName();

    String cwConstantsWithLookupExampleName();

    String cwConstantsWithLookupExampleNoInput();

    String cwConstantsWithLookupExampleNoMatches();

    String cwConstantsWithLookupExampleResults();
  }

  /**
   * A {@link TextBox} where the user can select a color to lookup.
   */
  @ShowcaseData
  private TextBox colorBox = null;

  /**
   * The {@link ColorConstants} that map colors to values.
   */
  private ColorConstants colorConstants = null;

  /**
   * A {@link TextBox} where the results of the lookup are displayed.
   */
  @ShowcaseData
  private TextBox colorResultsBox = null;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private CwConstants constants;

  /**
   * Indicates whether or not we have loaded the {@link ExampleConstants} java
   * source yet.
   */
  private boolean javaLoaded = false;

  /**
   * The widget used to display {@link ExampleConstants} java source.
   */
  private HTML javaWidget = null;

  /**
   * Indicates whether or not we have loaded the {@link ExampleConstants}
   * properties source yet.
   */
  private boolean propertiesLoaded = false;

  /**
   * The widget used to display {@link ExampleConstants} properties source.
   */
  private HTML propertiesWidget = null;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwConstantsWithLookupExample(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwConstantsWithLookupExampleDescription();
  }

  @Override
  public String getName() {
    return constants.cwConstantsWithLookupExampleName();
  }

  @Override
  public boolean hasStyle() {
    return false;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create the internationalized constants
    colorConstants = GWT.create(ColorConstants.class);

    // Use a FlexTable to layout the content
    FlexTable layout = new FlexTable();
    FlexCellFormatter formatter = layout.getFlexCellFormatter();
    layout.setCellSpacing(5);

    // Add a link to the source code of the Interface
    HTML link = new HTML(" <a href=\"javascript:void(0);\">ColorConstants</a>");
    link.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        selectTab(2);
      }
    });
    HorizontalPanel linkPanel = new HorizontalPanel();
    linkPanel.setSpacing(3);
    linkPanel.add(new HTML(constants.cwConstantsWithLookupExampleLinkText()));
    linkPanel.add(link);
    layout.setWidget(0, 0, linkPanel);
    formatter.setColSpan(0, 0, 2);

    // Add a field so the user can type a color
    colorBox = new TextBox();
    colorBox.setText("red");
    colorBox.setWidth("17em");
    layout.setHTML(1, 0, constants.cwConstantsWithLookupExampleMethodName());
    layout.setWidget(1, 1, colorBox);

    // Show the last name
    colorResultsBox = new TextBox();
    colorResultsBox.setEnabled(false);
    colorResultsBox.setWidth("17em");
    layout.setHTML(2, 0, constants.cwConstantsWithLookupExampleResults());
    layout.setWidget(2, 1, colorResultsBox);

    // Add a handler to update the color as the user types a lookup value
    colorBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        updateColor();
      }
    });

    // Return the layout Widget
    updateColor();
    return layout;
  }

  @Override
  public void onInitializeComplete() {
    addConstantsTab();
  }

  @Override
  public void onSelection(SelectionEvent<Integer> event) {
    super.onSelection(event);

    int tabIndex = event.getSelectedItem().intValue();
    if (!javaLoaded && tabIndex == 2) {
      // Load ErrorMessages.java
      javaLoaded = true;
      String className = ColorConstants.class.getName();
      className = className.substring(className.lastIndexOf(".") + 1);
      requestSourceContents(ShowcaseConstants.DST_SOURCE_RAW + className
          + ".java.html", javaWidget, null);
    } else if (!propertiesLoaded && tabIndex == 3) {
      // Load ErrorMessages.properties
      propertiesLoaded = true;
      String className = ColorConstants.class.getName();
      className = className.substring(className.lastIndexOf(".") + 1);
      requestSourceContents(ShowcaseConstants.DST_SOURCE_RAW + className
          + ".properties.html", propertiesWidget, null);
    }
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwConstantsWithLookupExample.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  @Override
  protected void setRunAsyncPrefetches() {
    prefetchInternationalization();
  }

  /**
   * Add a tab to this example to show the messages interface.
   */
  private void addConstantsTab() {
    // Add a tab to show the interface
    javaWidget = new HTML();
    add(javaWidget, "ColorConstants.java");

    // Add a tab to show the properties
    propertiesWidget = new HTML();
    add(propertiesWidget, "ColorConstants.properties");
  }

  /**
   * Lookup the color based on the value the user entered.
   */
  @ShowcaseSource
  private void updateColor() {
    String key = colorBox.getText().trim();
    if (key.equals("")) {
      colorResultsBox.setText(constants.cwConstantsWithLookupExampleNoInput());
    } else {
      try {
        String color = colorConstants.getString(key);
        colorResultsBox.setText(color);
      } catch (MissingResourceException e) {
        colorResultsBox.setText(constants.cwConstantsWithLookupExampleNoMatches());
      }
    }
  }
}
