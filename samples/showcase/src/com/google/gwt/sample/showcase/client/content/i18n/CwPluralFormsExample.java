/*
 * Copyright 2009 Google Inc.
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
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

/**
 * Example file.
 */
@ShowcaseRaw({"PluralMessages.java", "PluralMessages_fr.properties"})
public class CwPluralFormsExample extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwPluralFormsExampleArg0Label();

    String cwPluralFormsExampleDescription();

    String cwPluralFormsExampleFormattedLabel();

    String cwPluralFormsExampleLinkText();

    String cwPluralFormsExampleName();
  }

  /**
   * The {@link TextBox} where the user enters argument 0.
   */
  @ShowcaseData
  private TextBox arg0Box = null;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private CwConstants constants;

  /**
   * The {@link Label} used to display the message.
   */
  @ShowcaseData
  private Label formattedMessage = null;

  /**
   * Indicates whether or not we have loaded the {@link PluralMessages} java
   * source yet.
   */
  private boolean javaLoaded = false;

  /**
   * The widget used to display {@link PluralMessages} java source.
   */
  private HTML javaWidget = null;

  /**
   * The plural messages used in this example.
   */
  @ShowcaseData
  private PluralMessages pluralMessages = null;

  /**
   * Indicates whether or not we have loaded the {@link ErrorMessages}
   * properties source yet.
   */
  private boolean propertiesLoaded = false;

  /**
   * The widget used to display {@link ErrorMessages} properties source.
   */
  private HTML propertiesWidget = null;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwPluralFormsExample(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwPluralFormsExampleDescription();
  }

  @Override
  public String getName() {
    return constants.cwPluralFormsExampleName();
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
    // Create the internationalized error messages
    pluralMessages = GWT.create(PluralMessages.class);

    // Use a FlexTable to layout the content
    FlexTable layout = new FlexTable();
    FlexCellFormatter formatter = layout.getFlexCellFormatter();
    layout.setCellSpacing(5);

    // Add a link to the source code of the Interface
    HTML link = new HTML(" <a href=\"javascript:void(0);\">PluralMessages</a>");
    link.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        selectTab(2);
      }
    });
    HorizontalPanel linkPanel = new HorizontalPanel();
    linkPanel.setSpacing(3);
    linkPanel.add(new HTML(constants.cwPluralFormsExampleLinkText()));
    linkPanel.add(link);
    layout.setWidget(0, 0, linkPanel);
    formatter.setColSpan(0, 0, 2);

    // Add argument 0
    arg0Box = new TextBox();
    arg0Box.setText("13");
    layout.setHTML(2, 0, constants.cwPluralFormsExampleArg0Label());
    layout.setWidget(2, 1, arg0Box);

    // Add the formatted message
    formattedMessage = new Label();
    layout.setHTML(5, 0, constants.cwPluralFormsExampleFormattedLabel());
    layout.setWidget(5, 1, formattedMessage);
    formatter.setVerticalAlignment(5, 0, HasVerticalAlignment.ALIGN_TOP);

    // Add handlers to all of the argument boxes
    KeyUpHandler keyUpHandler = new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        updateMessage();
      }
    };
    arg0Box.addKeyUpHandler(keyUpHandler);

    // Return the layout Widget
    updateMessage();
    return layout;
  }

  @Override
  public void onInitializeComplete() {
    addMessagesTab();
  }

  @Override
  public void onSelection(SelectionEvent<Integer> event) {
    super.onSelection(event);

    int tabIndex = event.getSelectedItem().intValue();
    if (!javaLoaded && tabIndex == 2) {
      // Load PluralMessages.java
      javaLoaded = true;
      String className = PluralMessages.class.getName();
      className = className.substring(className.lastIndexOf(".") + 1);
      requestSourceContents(ShowcaseConstants.DST_SOURCE_RAW + className
          + ".java.html", javaWidget, null);
    } else if (!propertiesLoaded && tabIndex == 3) {
      // Load ErrorMessages.properties
      propertiesLoaded = true;
      String className = PluralMessages.class.getName();
      className = className.substring(className.lastIndexOf(".") + 1);
      requestSourceContents(ShowcaseConstants.DST_SOURCE_RAW + className
          + "_fr.properties.html", propertiesWidget, null);
    }
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwPluralFormsExample.class, new RunAsyncCallback() {

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
   * Add a tab to this example to show the {@link PluralMessages} source code.
   */
  private void addMessagesTab() {
    // Add a tab to show the interface
    javaWidget = new HTML();
    add(javaWidget, "PluralMessages.java");

    // Add a tab to show the properties
    propertiesWidget = new HTML();
    add(propertiesWidget, "PluralMessages_fr.properties");
  }

  /**
   * Update the formatted message.
   */
  @ShowcaseSource
  private void updateMessage() {
    try {
      int count = Integer.parseInt(arg0Box.getText().trim());
      formattedMessage.setText(pluralMessages.treeCount(count));
    } catch (NumberFormatException e) {
      // Ignore.
    }
  }
}
