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
package com.google.gwt.sample.showcase.client.content.i18n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.shared.BidiFormatter;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
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
@ShowcaseRaw({"BlogMessages.java", "BlogMessages.properties"})
public class CwBidiFormatting extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwBidiFormattingArg0Label();

    String cwBidiFormattingArg1Label();

    String cwBidiFormattingArg2Label();

    String cwBidiFormattingBidiFormattedLabel();

    String cwBidiFormattingDescription();

    String cwBidiFormattingLinkText();

    String cwBidiFormattingName();

    String cwBidiFormattingNonbidiFormattedLabel();

    String cwBidiFormattingTemplateLabel();
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
   * A {@link com.google.gwt.i18n.shared.BidiFormatter} instance used for
   * bidi-formatting of user input.
   */
  @ShowcaseData
  private BidiFormatter bidiFormatter =
      BidiFormatter.getInstanceForCurrentLocale();

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * The blog messages used in this example.
   */
  @ShowcaseData
  private BlogMessages blogMessages = null;

  /**
   * The {@link HTML} used to display the message.
   */
  @ShowcaseData
  private HTML message = null;

  /**
   * The {@link HTML} used to display the bidi formatted message.
   */
  @ShowcaseData
  private HTML bidiFormattedMessage = null;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwBidiFormatting(CwConstants constants) {
    super(constants.cwBidiFormattingName(),
        constants.cwBidiFormattingDescription(), false, "BlogMessages.java",
        "BlogMessages.properties");
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create the internationalized blog messages
    blogMessages = GWT.create(BlogMessages.class);

    // Use a FlexTable to layout the content
    FlexTable layout = new FlexTable();
    FlexCellFormatter formatter = layout.getFlexCellFormatter();
    layout.setCellSpacing(5);

    // Add a link to the source code of the Interface
    final String rawFile = getSimpleName(BlogMessages.class);
    Anchor link = new Anchor(rawFile);
    link.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        fireRawSourceRequest(rawFile + ".java");
      }
    });
    HorizontalPanel linkPanel = new HorizontalPanel();
    linkPanel.setSpacing(3);
    linkPanel.add(new HTML(constants.cwBidiFormattingLinkText()));
    linkPanel.add(link);
    layout.setWidget(0, 0, linkPanel);
    formatter.setColSpan(0, 0, 2);

    // Show the template for reference
    String template = blogMessages.userComment("{0}", "{1}", "{2}");
    layout.setHTML(1, 0, constants.cwBidiFormattingTemplateLabel());
    layout.setHTML(1, 1, template);

    // Add argument 0
    arg0Box = new TextBox();
    // Using an initial value whose direction is opposite the locale's direction
    // demonstrates the need for and effect of bidi formatting.
    arg0Box.setText(
        LocaleInfo.getCurrentLocale().isRTL() ? "Tom Bombadil" : "תומר גרין");
    layout.setHTML(2, 0, constants.cwBidiFormattingArg0Label());
    layout.setWidget(2, 1, arg0Box);

    // Add argument 1
    arg1Box = new TextBox();
    arg1Box.setText("16");
    layout.setHTML(3, 0, constants.cwBidiFormattingArg1Label());
    layout.setWidget(3, 1, arg1Box);

    // Add argument 2
    arg2Box = new TextBox();
    // Using an initial value whose direction is opposite the locale's direction
    // demonstrates the need for and effect of bidi formatting.
    arg2Box.setText(LocaleInfo.getCurrentLocale().isRTL()
        ? "How deep is your love?" : "כמה חול יש בחוף?");
    layout.setHTML(4, 0, constants.cwBidiFormattingArg2Label());
    layout.setWidget(4, 1, arg2Box);

    // Add the unformatted message
    message = new HTML();
    layout.setHTML(5, 0, constants.cwBidiFormattingNonbidiFormattedLabel());
    layout.setWidget(5, 1, message);
    formatter.setVerticalAlignment(5, 0, HasVerticalAlignment.ALIGN_TOP);

    // Add the bidi formatted message
    bidiFormattedMessage = new HTML();
    layout.setHTML(6, 0, constants.cwBidiFormattingBidiFormattedLabel());
    layout.setWidget(6, 1, bidiFormattedMessage);
    formatter.setVerticalAlignment(6, 0, HasVerticalAlignment.ALIGN_TOP);

    // Add handlers to all of the argument boxes
    KeyUpHandler keyUpHandler = new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        updateMessages();
      }
    };
    arg0Box.addKeyUpHandler(keyUpHandler);
    arg1Box.addKeyUpHandler(keyUpHandler);
    arg2Box.addKeyUpHandler(keyUpHandler);

    // Return the layout Widget
    updateMessages();

    return layout;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwBidiFormatting.class, new RunAsyncCallback() {

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
  private void updateMessages() {
    String arg0 = arg0Box.getText().trim();
    String arg1 = arg1Box.getText().trim();
    String arg2 = arg2Box.getText().trim();
    message.setText(blogMessages.userComment(arg0, arg1, arg2));

    bidiFormattedMessage.setHTML(
        blogMessages.userComment(bidiFormatter.spanWrap(arg0),
        // arg1 is intended to be an unsigned number, so bidi formatting is not
        // needed. However, HTML escaping is a must to avoid an XSS attack hole!
            SafeHtmlUtils.htmlEscape(arg1), bidiFormatter.spanWrap(arg2)));
  }
}
