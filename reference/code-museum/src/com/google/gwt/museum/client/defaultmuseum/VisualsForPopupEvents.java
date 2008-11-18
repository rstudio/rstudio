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

package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple tree used to quickly exercise tree behavior.
 */
public class VisualsForPopupEvents extends AbstractIssue {
  static int xPos = 0;

  @Override
  public Widget createIssue() {
    VerticalPanel panel = new VerticalPanel();
    createButton(true, true, panel);
    createButton(true, false, panel);
    createButton(false, true, panel);
    createButton(false, false, panel);
    return panel;
  }

  @Override
  public String getInstructions() {
    return "Open and close each popup, check for the closing text in the window title";
  }

  @Override
  public String getSummary() {
    return "popup event visual test";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  private void createButton(boolean autoHide, boolean modal, VerticalPanel panel) {
    final String text = " popup " + (modal ? " modal " : " non-modal ")
        + (autoHide ? "auto hide" : " persistent");
    panel.add(new HTML("<h2>" + text + "</h2>"));
    final PopupPanel p = new PopupPanel(autoHide, modal);
    p.setTitle(text);
    Button b = new Button("show", new ClickHandler() {
      public void onClick(ClickEvent event) {
        p.setPopupPosition(200,
            ((Button) event.getSource()).getAbsoluteTop() - 10);
        p.show();
      }
    });
    panel.add(b);
    p.setWidget(new Button("hide me", new ClickHandler() {
      public void onClick(ClickEvent event) {
        p.hide();
      }
    }));

    p.addCloseHandler(new CloseHandler<PopupPanel>() {
      public void onClose(CloseEvent<PopupPanel> event) {
        Window.setTitle("closing popup '" + p.getTitle() + "'. autohide:"
            + event.isAutoClosed());
      }
    });
  }
}