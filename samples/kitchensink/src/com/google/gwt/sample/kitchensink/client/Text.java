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
package com.google.gwt.sample.kitchensink.client;

import com.google.gwt.user.client.DOM;
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
        "<h2>Basic and Rich Text</h2>"
            + "<p>GWT includes the standard complement of text-entry widgets, each of which "
            + "supports keyboard and selection events you can use to control text entry.  "
            + "In particular, notice that the selection range for each widget is "
            + "updated whenever you press a key.</p>"
            + "<p>Also notice the rich-text area to the right. This is supported on "
            + "all major browsers, and will fall back gracefully to the level of "
            + "functionality supported on each.</p>") {

      public Sink createInstance() {
        return new Text();
      }

      public String getColor() {
        return "#2fba10";
      }
    };
  }

  private PasswordTextBox passwordText = new PasswordTextBox();
  private TextArea textArea = new TextArea();
  private TextBox textBox = new TextBox();

  public Text() {
    TextBox readOnlyTextBox = new TextBox();
    readOnlyTextBox.setReadOnly(true);
    readOnlyTextBox.setText("read only");
    readOnlyTextBox.setWidth("20em");

    VerticalPanel vp = new VerticalPanel();
    vp.setSpacing(8);
    vp.add(new HTML("Normal text box:"));
    vp.add(createTextThing(textBox));
    vp.add(readOnlyTextBox);
    vp.add(new HTML("Password text box:"));
    vp.add(createTextThing(passwordText));
    vp.add(new HTML("Text area:"));
    vp.add(createTextThing(textArea));

    textArea.setVisibleLines(5);

    Widget richText = createRichText();

    HorizontalPanel hp = new HorizontalPanel();
    hp.add(vp);
    hp.add(richText);
    hp.setCellHorizontalAlignment(vp, HorizontalPanel.ALIGN_LEFT);
    hp.setCellHorizontalAlignment(richText, HorizontalPanel.ALIGN_RIGHT);

    initWidget(hp);
    hp.setWidth("100%");
  }

  public void onShow() {
  }

  private Widget createRichText() {
    RichTextArea area = new RichTextArea();
    RichTextToolbar tb = new RichTextToolbar(area);

    VerticalPanel p = new VerticalPanel();
    p.add(tb);
    p.add(area);

    area.setHeight("14em");
    area.setWidth("100%");
    tb.setWidth("100%");
    p.setWidth("100%");
    DOM.setStyleAttribute(p.getElement(), "margin-right", "4px");
    return p;
  }

  private Widget createTextThing(final TextBoxBase textBox) {
    HorizontalPanel p = new HorizontalPanel();
    p.setSpacing(4);

    p.add(textBox);

    final HTML echo = new HTML();

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

    textBox.setWidth("20em");
    updateText(textBox, echo);
    return p;
  }

  private void updateText(TextBoxBase text, HTML echo) {
    echo.setHTML("Selection: " + text.getCursorPos() + ", "
        + text.getSelectionLength());
  }
}
