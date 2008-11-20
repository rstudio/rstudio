/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class TextBoxExample implements EntryPoint {

  public void onModuleLoad() {
    // Make some text boxes. The password text box is identical to the text
    // box, except that the input is visually masked by the browser.
    PasswordTextBox ptb = new PasswordTextBox();
    TextBox tb = new TextBox();

    // TODO(ECC) must be tested.
    tb.addKeyPressHandler(new KeyPressHandler() {

      public void onKeyPress(KeyPressEvent event) {
        if (!Character.isDigit(event.getCharCode())) {
          ((TextBox) event.getSource()).cancelKey();
        }
      }
    });

    // Let's make an 80x50 text area to go along with the other two.
    TextArea ta = new TextArea();
    ta.setCharacterWidth(80);
    ta.setVisibleLines(50);

    // Add them to the root panel.
    VerticalPanel panel = new VerticalPanel();
    panel.add(tb);
    panel.add(ptb);
    panel.add(ta);
    RootPanel.get().add(panel);
  }
}