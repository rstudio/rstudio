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
import com.google.gwt.user.client.ui.TextBox;

import java.util.MissingResourceException;

/**
 * Demonstrates {@link com.google.gwt.i18n.client.ConstantsWithLookup}.
 */
public class ConstantsWithLookupExampleController {

  private static final String DEFAULT_INPUT = "red";
  private static final ColorConstants COLORS = GWT.create(ColorConstants.class);

  public final TextBox txtInput = new TextBox();
  public final TextBox txtResult = new TextBox();
  private String prevText;
  private final ConstantsWithLookupExampleConstants constants;

  public ConstantsWithLookupExampleController(
      final ConstantsWithLookupExampleConstants constants) {

    this.constants = constants;
    txtResult.setText(constants.noInputResult());
    txtResult.setReadOnly(true);

    txtInput.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        maybeRefreshLookup(constants);
      }
    });

    txtInput.setText(DEFAULT_INPUT);
    maybeRefreshLookup(constants);
  }

  public ConstantsWithLookupExampleConstants getConstants() {
    return constants;
  }

  private void maybeRefreshLookup(
      final ConstantsWithLookupExampleConstants constants) {
    final String currText = txtInput.getText().trim();
    if (!currText.equals(prevText)) {
      prevText = currText;
      if ("".equals(currText)) {
        txtResult.setText(constants.noInputResult());
      } else {
        try {
          String color = COLORS.getString(currText);
          txtResult.setText(color);
        } catch (MissingResourceException e) {
          txtResult.setText(constants.noMatchingResult());
        }
      }
    }
  }
}
