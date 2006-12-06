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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Demonstrates the various button widgets.
 */
public class Buttons extends Sink {

  public static SinkInfo init() {
    return new SinkInfo("Buttons",
      "GWT supports all the myriad types of buttons that exist in HTML.  "
        + "Here are a few for your viewing pleasure.") {
      public Sink createInstance() {
        return new Buttons();
      }
    };
  }

  private Button disabledButton = new Button("Disabled Button");
  private CheckBox disabledCheck = new CheckBox("Disabled Check");
  private Button normalButton = new Button("Normal Button");
  private CheckBox normalCheck = new CheckBox("Normal Check");
  private VerticalPanel panel = new VerticalPanel();
  private RadioButton radio0 = new RadioButton("group0", "Choice 0");
  private RadioButton radio1 = new RadioButton("group0", "Choice 1");
  private RadioButton radio2 = new RadioButton("group0", "Choice 2 (Disabled)");
  private RadioButton radio3 = new RadioButton("group0", "Choice 3");

  public Buttons() {
    HorizontalPanel hp;

    panel.add(hp = new HorizontalPanel());
    hp.setSpacing(8);
    hp.add(normalButton);
    hp.add(disabledButton);

    panel.add(hp = new HorizontalPanel());
    hp.setSpacing(8);
    hp.add(normalCheck);
    hp.add(disabledCheck);

    panel.add(hp = new HorizontalPanel());
    hp.setSpacing(8);
    hp.add(radio0);
    hp.add(radio1);
    hp.add(radio2);
    hp.add(radio3);

    disabledButton.setEnabled(false);
    disabledCheck.setEnabled(false);
    radio2.setEnabled(false);

    panel.setSpacing(8);
    initWidget(panel);
  }

  public void onShow() {
  }
}
