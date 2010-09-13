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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.Showcase;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle({
    ".gwt-PopupPanel", "html>body .gwt-PopupPanel", "* html .gwt-PopupPanel",
    ".gwt-DecoratedPopupPanel", "html>body .gwt-DecoratedPopupPanel",
    "* html .gwt-DecoratedPopupPanel"})
public class CwBasicPopup extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwBasicPopupClickOutsideInstructions();

    String cwBasicPopupDescription();

    String cwBasicPopupInstructions();

    String cwBasicPopupName();

    String cwBasicPopupShowButton();
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwBasicPopup(CwConstants constants) {
    super(constants.cwBasicPopupName(), constants.cwBasicPopupDescription(),
        true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a basic popup widget
    final DecoratedPopupPanel simplePopup = new DecoratedPopupPanel(true);
    simplePopup.ensureDebugId("cwBasicPopup-simplePopup");
    simplePopup.setWidth("150px");
    simplePopup.setWidget(
        new HTML(constants.cwBasicPopupClickOutsideInstructions()));

    // Create a button to show the popup
    Button openButton = new Button(
        constants.cwBasicPopupShowButton(), new ClickHandler() {
          public void onClick(ClickEvent event) {
            // Reposition the popup relative to the button
            Widget source = (Widget) event.getSource();
            int left = source.getAbsoluteLeft() + 10;
            int top = source.getAbsoluteTop() + 10;
            simplePopup.setPopupPosition(left, top);

            // Show the popup
            simplePopup.show();
          }
        });

    // Create a popup to show the full size image
    Image jimmyFull = new Image(Showcase.images.jimmy());
    final PopupPanel imagePopup = new PopupPanel(true);
    imagePopup.setAnimationEnabled(true);
    imagePopup.ensureDebugId("cwBasicPopup-imagePopup");
    imagePopup.setWidget(jimmyFull);
    jimmyFull.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        imagePopup.hide();
      }
    });

    // Add an image thumbnail
    Image jimmyThumb = new Image(Showcase.images.jimmyThumb());
    jimmyThumb.ensureDebugId("cwBasicPopup-thumb");
    jimmyThumb.addStyleName("cw-BasicPopup-thumb");
    jimmyThumb.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
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

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwBasicPopup.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
