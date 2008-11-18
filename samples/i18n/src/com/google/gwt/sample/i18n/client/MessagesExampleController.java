/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Demonstrates how to use {@link com.google.gwt.i18n.client.Messages}.
 */
public class MessagesExampleController {
  private static final ErrorMessages ERRORS = GWT.create(ErrorMessages.class);
  private static final PluralMessages PLURALS = GWT.create(PluralMessages.class);

  public final TextBox txtArg1 = new TextBox();
  public final TextBox txtArg2 = new TextBox();
  public final TextBox txtArg3 = new TextBox();
  public final Label lblFormattedMessage = new Label();
  public final Label lblMessageTemplate = new Label();

  public final TextBox pluralCount = new TextBox();
  public final Label lblPluralMessage = new Label();

  private String prevArg1;
  private String prevArg2;
  private String prevArg3;
  private String prevCount;
  private final MessagesExampleConstants constants;

  public MessagesExampleController(MessagesExampleConstants constants) {
    this.constants = constants;

    String messageTemplate = ERRORS.permissionDenied("{0}", "{1}", "{2}");
    lblMessageTemplate.setText(messageTemplate);

    KeyUpHandler handler = new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        maybeRefreshFormattedMessage();
      }
    };
    txtArg1.addKeyUpHandler(handler);
    txtArg2.addKeyUpHandler(handler);
    txtArg3.addKeyUpHandler(handler);
    pluralCount.addKeyUpHandler(handler);

    txtArg1.setText("amelie");
    txtArg2.setText("guest");
    txtArg3.setText("/secure/blueprints.xml");

    pluralCount.setText("13");

    maybeRefreshFormattedMessage();
  }

  public MessagesExampleConstants getConstants() {
    return constants;
  }

  private void maybeRefreshFormattedMessage() {
    String arg1 = txtArg1.getText().trim();
    String arg2 = txtArg2.getText().trim();
    String arg3 = txtArg3.getText().trim();

    // Check if the permission denied message should be regenerated.
    if (!arg1.equals(prevArg1)
      || !arg2.equals(prevArg2)
      || !arg3.equals(prevArg3)) {
      prevArg1 = arg1;
      prevArg2 = arg2;
      prevArg3 = arg3;

      String formattedMessage = ERRORS.permissionDenied(arg1, arg2, arg3);
      lblFormattedMessage.setText(formattedMessage);
    }

    String count = pluralCount.getText().trim();

    // Check if the plurals message should be regenerated.
    if (!count.equals(prevCount) && count.trim().length() > 0) {
      prevCount = count;

      try {
        String formattedMessage = PLURALS.treeCount(Integer.valueOf(count));
        lblPluralMessage.setText(formattedMessage);
      } catch (NumberFormatException e) {
        // Ignore bogus numbers
      }
    }
  }
}
