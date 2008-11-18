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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CompositeExample implements EntryPoint {

  /**
   * A composite of a TextBox and a CheckBox that optionally enables it.
   */
  private static class OptionalTextBox extends Composite implements
      ClickHandler {

    private TextBox textBox = new TextBox();
    private CheckBox checkBox = new CheckBox();

    /**
     * Constructs an OptionalTextBox with the given caption on the check.
     * 
     * @param caption the caption to be displayed with the check box
     */
    public OptionalTextBox(String caption) {
      // Place the check above the text box using a vertical panel.
      VerticalPanel panel = new VerticalPanel();
      panel.add(checkBox);
      panel.add(textBox);

      // Set the check box's caption, and check it by default.
      checkBox.setText(caption);
      checkBox.setChecked(true);
      checkBox.addClickHandler(this);

      // All composites must call initWidget() in their constructors.
      initWidget(panel);

      // Give the overall composite a style name.
      setStyleName("example-OptionalCheckBox");
    }

    public void onClick(ClickEvent event) {
      if (event.getSource() == checkBox) {
        // When the check box is clicked, update the text box's enabled state.
        textBox.setEnabled(checkBox.isChecked());
      }
    }

    /**
     * Sets the caption associated with the check box.
     * 
     * @param caption the check box's caption
     */
    public void setCaption(String caption) {
      // Note how we use the use composition of the contained widgets to provide
      // only the methods that we want to.
      checkBox.setText(caption);
    }

    /**
     * Gets the caption associated with the check box.
     * 
     * @return the check box's caption
     */
    public String getCaption() {
      return checkBox.getText();
    }
  }

  public void onModuleLoad() {
    // Create an optional text box and add it to the root panel.
    OptionalTextBox otb = new OptionalTextBox("Check this to enable me");
    RootPanel.get().add(otb);
  }
}
