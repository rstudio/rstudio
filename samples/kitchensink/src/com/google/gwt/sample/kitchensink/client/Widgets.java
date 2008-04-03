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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Demonstrates the various button widgets.
 */
public class Widgets extends Sink implements Command {

  public static SinkInfo init(final Sink.Images images) {
    return new SinkInfo("Widgets", "<h2>Basic Widgets</h2>" +
      "<p>GWT has all sorts of the basic widgets you would expect from any " +
      "toolkit.</p><p>Below, you can see various kinds of buttons, check boxes, " +
      "radio buttons, and menus.</p>") {

      @Override
      public Sink createInstance() {
        return new Widgets(images);
      }

      @Override
      public String getColor() {
        return "#bf2a2a";
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
  private PushButton pushButton;
  private ToggleButton toggleButton;

  public Widgets(Sink.Images images) {
    pushButton = new PushButton(images.gwtLogo().createImage());
    toggleButton = new ToggleButton(images.gwtLogo().createImage());

    HorizontalPanel hp;

    panel.add(createMenu());

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

    panel.add(hp = new HorizontalPanel());
    hp.setSpacing(8);
    hp.add(pushButton);
    hp.add(toggleButton);

    disabledButton.setEnabled(false);
    disabledCheck.setEnabled(false);
    radio2.setEnabled(false);

    panel.setSpacing(8);
    initWidget(panel);
  }

  public MenuBar createMenu() {
    MenuBar menu = new MenuBar();
    menu.setAutoOpen(true);

    MenuBar subMenu = new MenuBar(true);
    subMenu.addItem("<code>Code</code>", true, this);
    subMenu.addItem("<strike>Strikethrough</strike>", true, this);
    subMenu.addItem("<u>Underlined</u>", true, this);

    MenuBar menu0 = new MenuBar(true);
    menu0.addItem("<b>Bold</b>", true, this);
    menu0.addItem("<i>Italicized</i>", true, this);
    menu0.addItem("More", true, subMenu);
    MenuBar menu1 = new MenuBar(true);
    menu1.addItem("<font color='#FF0000'><b>Apple</b></font>", true, this);
    menu1.addItem("<font color='#FFFF00'><b>Banana</b></font>", true, this);
    menu1.addItem("<font color='#FFFFFF'><b>Coconut</b></font>", true, this);
    menu1.addItem("<font color='#8B4513'><b>Donut</b></font>", true, this);
    MenuBar menu2 = new MenuBar(true);
    menu2.addItem("Bling", this);
    menu2.addItem("Ginormous", this);
    menu2.addItem("<code>w00t!</code>", true, this);

    menu.addItem(new MenuItem("Style", menu0));
    menu.addItem(new MenuItem("Fruit", menu1));
    menu.addItem(new MenuItem("Term", menu2));

    menu.setWidth("100%");

    return menu;
  }

  public void execute() {
    Window.alert("Thank you for selecting a menu item.");
  }

  @Override
  public void onShow() {
  }
}
