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
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Demonstrates how to use {@link Messages}.
 */
public class MessagesExampleController {
  private static final ErrorMessages ERRORS = (ErrorMessages) GWT.create(ErrorMessages.class);

  public final TextBox txtArg1 = new TextBox();
  public final TextBox txtArg2 = new TextBox();
  public final TextBox txtArg3 = new TextBox();
  public final Label lblFormattedMessage = new Label();
  public final Label lblMessageTemplate = new Label();

  private String prevArg1;
  private String prevArg2;
  private String prevArg3;
  private final MessagesExampleConstants constants;

  public MessagesExampleController(MessagesExampleConstants constants) {
    this.constants = constants;

    String messageTemplate = ERRORS.permissionDenied("{0}", "{1}", "{2}");
    lblMessageTemplate.setText(messageTemplate);

    KeyboardListenerAdapter listener = new KeyboardListenerAdapter() {
      public void onKeyUp(Widget sender, char keyCode, int modifiers) {
        maybeRefreshFormattedMessage();
      }
    };
    txtArg1.addKeyboardListener(listener);
    txtArg2.addKeyboardListener(listener);
    txtArg3.addKeyboardListener(listener);

    txtArg1.setText("amelie");
    txtArg2.setText("guest");
    txtArg3.setText("/secure/blueprints.xml");

    maybeRefreshFormattedMessage();
  }

  public MessagesExampleConstants getConstants() {
    return constants;
  }

  private void maybeRefreshFormattedMessage() {
    String arg1 = txtArg1.getText().trim();
    String arg2 = txtArg2.getText().trim();
    String arg3 = txtArg3.getText().trim();

    if (arg1.equals(prevArg1)) {
      if (arg2.equals(prevArg2)) {
        if (arg3.equals(prevArg3)) {
          // Nothing has changed.
          return;
        }
      }
    }

    prevArg1 = arg1;
    prevArg2 = arg2;
    prevArg3 = arg3;

    String formattedMessage = ERRORS.permissionDenied(arg1, arg2, arg3);
    lblFormattedMessage.setText(formattedMessage);
  }
}
