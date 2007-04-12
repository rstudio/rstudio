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
package com.google.gwt.sample.kitchensink.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Demonstrates the various text widgets.
 */
public class Text extends Sink {

  public static SinkInfo init() {
    return new SinkInfo(
      "Text",
      "GWT includes the standard complement of text-entry widgets, each of which "
        + "supports keyboard and selection events you can use to control text entry.  "
        + "In particular, notice that the selection range for each widget is "
        + "updated whenever you press a key.  "
        + "This can be a bit tricky on some browsers, but the GWT class library "
        + "takes care of the plumbing for you automatically.") {
      public Sink createInstance() {
        return new Text();
      }
    };
  }

  private PasswordTextBox passwordText = new PasswordTextBox();
  private TextArea textArea = new TextArea();
  private TextBox textBox = new TextBox();

  public Text() {
    VerticalPanel panel = new VerticalPanel();
    panel.setSpacing(8);
    panel.add(new HTML("Normal text box:"));
    panel.add(createTextThing(textBox));
    panel.add(new HTML("Password text box:"));
    panel.add(createTextThing(passwordText));
    panel.add(new HTML("Text area:"));
    panel.add(createTextThing(textArea));
    panel.add(new HTML("Rich text area:"));
    panel.add(createRichText());
    initWidget(panel);
  }

  public void onShow() {
  }

  private Widget createRichText() {
    RichTextArea area = new RichTextArea();
    RichTextToolbar tb = new RichTextToolbar(area);

    VerticalPanel p = new VerticalPanel();
    p.add(tb);
    p.add(area);
    area.setWidth("100%");
    return p;
  }

  private Widget createTextThing(final TextBoxBase textBox) {
    HorizontalPanel p = new HorizontalPanel();
    p.setSpacing(4);

    p.add(textBox);

    final HTML echo = new HTML();
    p.add(new Button("select all", new ClickListener() {
      public void onClick(Widget sender) {
        textBox.selectAll();
        textBox.setFocus(true);
        updateText(textBox, echo);
      }
    }));

    p.add(echo);
    textBox.addKeyboardListener(new KeyboardListenerAdapter() {
      public void onKeyUp(Widget sender, char keyCode, int modifiers) {
        updateText(textBox, echo);
      }
    });

    textBox.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        updateText(textBox, echo);
      }
    });

    return p;
  }

  private void updateText(TextBoxBase text, HTML echo) {
    echo.setHTML("Text: " + text.getText() + "<br>" + "Selection: "
      + text.getCursorPos() + ", " + text.getSelectionLength());
  }
}
