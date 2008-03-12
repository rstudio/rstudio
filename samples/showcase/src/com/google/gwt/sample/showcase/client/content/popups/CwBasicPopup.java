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
package com.google.gwt.sample.showcase.client.content.popups;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.Showcase;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 * 
 * @gwt.CSS .gwt-PopupPanel
 * @gwt.CSS html>body .gwt-PopupPanel
 * @gwt.CSS * html .gwt-PopupPanel
 */
public class CwBasicPopup extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   * 
   * @gwt.SRC
   */
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwBasicPopupClickOutsideInstructions();

    String cwBasicPopupDescription();

    String cwBasicPopupInstructions();

    String cwBasicPopupName();

    String cwBasicPopupShowButton();
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
  public CwBasicPopup(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwBasicPopupDescription();
  }

  @Override
  public String getName() {
    return constants.cwBasicPopupName();
  }

  /**
   * Initialize this example.
   * 
   * @gwt.SRC
   */
  @Override
  public Widget onInitialize() {
    // Create a basic popup widget
    final PopupPanel simplePopup = new PopupPanel(true);
    simplePopup.ensureDebugId("cwBasicPopup-simplePopup");
    simplePopup.setWidth("128px");
    simplePopup.setWidget(new HTML(
        constants.cwBasicPopupClickOutsideInstructions()));

    // Create a button to show the popup
    Button openButton = new Button(constants.cwBasicPopupShowButton(),
        new ClickListener() {
          public void onClick(Widget sender) {
            // Reposition the popup relative to the button
            int left = sender.getAbsoluteLeft() + 10;
            int top = sender.getAbsoluteTop() + 10;
            simplePopup.setPopupPosition(left, top);

            // Show the popup
            simplePopup.show();
          }
        });

    // Create a popup to show the full size image
    Image jimmyFull = Showcase.images.jimmy().createImage();
    final PopupPanel imagePopup = new PopupPanel(true);
    imagePopup.ensureDebugId("cwBasicPopup-imagePopup");
    imagePopup.setWidget(jimmyFull);
    jimmyFull.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        imagePopup.hide();
      }
    });

    // Add an image thumbnail
    Image jimmyThumb = Showcase.images.jimmyThumb().createImage();
    jimmyThumb.ensureDebugId("cwBasicPopup-thumb");
    jimmyThumb.addStyleName("cw-BasicPopup-thumb");
    jimmyThumb.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        imagePopup.center();
      }
    });

    // Add the widgets to a panel
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.setSpacing(5);
    vPanel.add(openButton);
    vPanel.add(new HTML("<br><br><br>" + constants.cwBasicPopupInstructions()));
    vPanel.add(jimmyThumb);

    // Return the panel
    return vPanel;
  }
}
