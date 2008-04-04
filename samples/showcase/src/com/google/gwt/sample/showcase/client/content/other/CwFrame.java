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
package com.google.gwt.sample.showcase.client.content.other;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
public class CwFrame extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   * 
   * @gwt.SRC
   */
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwFrameDescription();

    String cwFrameName();

    String cwFrameSetLocation();
  }

  /**
   * An instance of the constants.
   * 
   * @gwt.DATA
   */
  private CwConstants constants;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwFrame(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwFrameDescription();
  }

  @Override
  public String getName() {
    return constants.cwFrameName();
  }
  
  @Override
  public boolean hasStyle() {
    return false;
  }

  /**
   * Initialize this example.
   * 
   * @gwt.SRC
   */
  @Override
  public Widget onInitialize() {
    // Create a new frame
    final Frame frame = new Frame("GWT-default.css");
    frame.setSize("700px", "300px");
    frame.ensureDebugId("cwFrame");

    // Create a form to set the location of the frame
    final TextBox locationBox = new TextBox();
    locationBox.setText(frame.getUrl());
    Button setLocationButton = new Button(constants.cwFrameSetLocation());
    HorizontalPanel optionsPanel = new HorizontalPanel();
    optionsPanel.setSpacing(8);
    optionsPanel.add(locationBox);
    optionsPanel.add(setLocationButton);

    // Change the location when the user clicks the button
    setLocationButton.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        frame.setUrl(locationBox.getText());
      }
    });

    // Change the location when the user presses enter
    locationBox.addKeyboardListener(new KeyboardListenerAdapter() {
      @Override
      public void onKeyPress(Widget sender, char keyCode, int modifiers) {
        if (keyCode == KEY_ENTER) {
          frame.setUrl(locationBox.getText());
        }
      }
    });

    // Add everything to a panel and return it
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(optionsPanel);
    vPanel.add(frame);
    return vPanel;
  }
}
