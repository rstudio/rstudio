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
package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class PopupPanelExample implements EntryPoint {

  private static class MyPopup extends PopupPanel {

    public MyPopup() {
      // PopupPanel's constructor takes 'auto-hide' as its boolean parameter.
      // If this is set, the panel closes itself automatically when the user
      // clicks outside of it.
      super(true);

      // PopupPanel is a SimplePanel, so you have to set it's widget property to
      // whatever you want its contents to be.
      setWidget(new Label("Click outside of this popup to close it"));
    }
  }

  public void onModuleLoad() {
    Button b1 = new Button("Click me to show popup");
    b1.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // Instantiate the popup and show it.
        new MyPopup().show();
      }
    });

    RootPanel.get().add(b1);

    Button b2 = new Button("Click me to show popup partway across the screen");

    b2.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // Create the new popup.
        final MyPopup popup = new MyPopup();
        // Position the popup 1/3rd of the way down and across the screen, and
        // show the popup. Since the position calculation is based on the
        // offsetWidth and offsetHeight of the popup, you have to use the
        // setPopupPositionAndShow(callback) method. The alternative would
        // be to call show(), calculate the left and top positions, and
        // call setPopupPosition(left, top). This would have the ugly side
        // effect of the popup jumping from its original position to its
        // new position.
        popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
          public void setPosition(int offsetWidth, int offsetHeight) {
            int left = (Window.getClientWidth() - offsetWidth) / 3;
            int top = (Window.getClientHeight() - offsetHeight) / 3;
            popup.setPopupPosition(left, top);
          }
        });
      }
    });

    RootPanel.get().add(b2);
  }
}
