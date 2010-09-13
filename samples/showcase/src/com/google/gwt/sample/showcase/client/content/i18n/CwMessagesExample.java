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
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseRaw({"ErrorMessages.java", "ErrorMessages.properties"})
public class CwMessagesExample extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwMessagesExampleArg0Label();

    String cwMessagesExampleArg1Label();

    String cwMessagesExampleArg2Label();

    String cwMessagesExampleDescription();

    String cwMessagesExampleFormattedLabel();

    String cwMessagesExampleLinkText();

    String cwMessagesExampleName();

    String cwMessagesExampleTemplateLabel();
  }

  /**
   * The {@link TextBox} where the user enters argument 0.
   */
  @ShowcaseData
  private TextBox arg0Box = null;

  /**
   * The {@link TextBox} where the user enters argument 1.
   */
  @ShowcaseData
  private TextBox arg1Box = null;

  /**
   * The {@link TextBox} where the user enters argument 2.
   */
  @ShowcaseData
  private TextBox arg2Box = null;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * The error messages used in this example.
   */
  @ShowcaseData
  private ErrorMessages errorMessages = null;

  /**
   * The {@link HTML} used to display the message.
   */
  @ShowcaseData
  private HTML formattedMessage = null;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwMessagesExample(CwConstants constants) {
    super(constants.cwMessagesExampleName(),
        constants.cwMessagesExampleDescription(), false, "ErrorMessages.java",
        "ErrorMessages.properties");
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create the internationalized error messages
    errorMessages = GWT.create(ErrorMessages.class);

    // Use a FlexTable to layout the content
    FlexTable layout = new FlexTable();
    FlexCellFormatter formatter = layout.getFlexCellFormatter();
    layout.setCellSpacing(5);

    // Add a link to the source code of the Interface
    final String rawFile = getSimpleName(ErrorMessages.class);
    Anchor link = new Anchor(rawFile);
    link.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        fireRawSourceRequest(rawFile + ".java");
      }
    });
    HorizontalPanel linkPanel = new HorizontalPanel();
    linkPanel.setSpacing(3);
    linkPanel.add(new HTML(constants.cwMessagesExampleLinkText()));
    linkPanel.add(link);
    layout.setWidget(0, 0, linkPanel);
    formatter.setColSpan(0, 0, 2);

    // Show the template for reference
    String template = errorMessages.permissionDenied("{0}", "{1}", "{2}");
    layout.setHTML(1, 0, constants.cwMessagesExampleTemplateLabel());
    layout.setHTML(1, 1, template);

    // Add argument 0
    arg0Box = new TextBox();
    arg0Box.setText("amelie");
    layout.setHTML(2, 0, constants.cwMessagesExampleArg0Label());
    layout.setWidget(2, 1, arg0Box);

    // Add argument 1
    arg1Box = new TextBox();
    arg1Box.setText("guest");
    layout.setHTML(3, 0, constants.cwMessagesExampleArg1Label());
    layout.setWidget(3, 1, arg1Box);

    // Add argument 2
    arg2Box = new TextBox();
    arg2Box.setText("/secure/blueprints.xml");
    layout.setHTML(4, 0, constants.cwMessagesExampleArg2Label());
    layout.setWidget(4, 1, arg2Box);

    // Add the formatted message
    formattedMessage = new HTML();
    layout.setHTML(5, 0, constants.cwMessagesExampleFormattedLabel());
    layout.setWidget(5, 1, formattedMessage);
    formatter.setVerticalAlignment(5, 0, HasVerticalAlignment.ALIGN_TOP);

    // Add handlers to all of the argument boxes
    KeyUpHandler keyUpHandler = new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        updateMessage();
      }
    };
    arg0Box.addKeyUpHandler(keyUpHandler);
    arg1Box.addKeyUpHandler(keyUpHandler);
    arg2Box.addKeyUpHandler(keyUpHandler);

    // Return the layout Widget
    updateMessage();
    return layout;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwMessagesExample.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  /**
   * Update the formatted message.
   */
  @ShowcaseSource
  private void updateMessage() {
    String arg0 = arg0Box.getText().trim();
    String arg1 = arg1Box.getText().trim();
    String arg2 = arg2Box.getText().trim();
    formattedMessage.setText(errorMessages.permissionDenied(arg0, arg1, arg2));
  }
}
