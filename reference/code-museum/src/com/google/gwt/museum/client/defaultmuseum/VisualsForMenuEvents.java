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

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.EventReporter;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple test for suggest box events.
 */
@SuppressWarnings("deprecation")
public class VisualsForMenuEvents extends AbstractIssue {

  HorizontalPanel report = new HorizontalPanel();

  @Override
  public Widget createIssue() {
    VerticalPanel p = new VerticalPanel();

    createMenu(p);
    report.setBorderWidth(3);

    report.setCellWidth(report.getWidget(0), "300px");

    p.add(report);
    return p;
  }

  @Override
  public String getInstructions() {
    return "Open and close menu items and see that you get close events";
  }

  @Override
  public String getSummary() {
    return "Menu event tests";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  void createMenu(Panel p) {
    final EventReporter<Object, PopupPanel> reporter = new EventReporter<Object, PopupPanel>(
        report);
    Command command = new Command() {

      public void execute() {
        reporter.report("menu item selected");
      }

    };

    MenuBar main = new MenuBar();
    main.setTitle("Main");
    p.add(main);
    MenuBar barA = new MenuBar();
    barA.setTitle("A");
    CloseHandler<PopupPanel> handler = new CloseHandler<PopupPanel>() {

      public void onClose(CloseEvent<PopupPanel> event) {
        reporter.report("closed popup belonging to Main");
      }

    };
    main.addCloseHandler(handler);
    barA.addItem("a1", command);
    barA.addItem("a2", command);

    handler = new CloseHandler<PopupPanel>() {

      public void onClose(CloseEvent<PopupPanel> event) {
        reporter.report("closed popup belonging to A");
      }

    };
    barA.addCloseHandler(handler);
    MenuBar barB = new MenuBar();
    barB.setTitle("B");
    barB.addItem("b1", command);
    barB.addItem("b2", command);

    handler = new CloseHandler<PopupPanel>() {

      public void onClose(CloseEvent<PopupPanel> event) {
        reporter.report("closed popup belonging to B");
      }

    };
    barB.addCloseHandler(handler);
    MenuBar barC = new MenuBar();
    barC.addItem("c1", command);
    barC.addItem("c2", command);

    handler = new CloseHandler<PopupPanel>() {

      public void onClose(CloseEvent<PopupPanel> event) {
        reporter.report("closed popup belonging to c");
      }

    };
    barC.addCloseHandler(handler);
    barC.setTitle("C");

    handler = new CloseHandler<PopupPanel>() {

      public void onClose(CloseEvent<PopupPanel> event) {
        reporter.report("closed popup belonging to B");
      }

    };

    main.addItem("A", barA);
    barA.addItem("b", barB);
    barB.addItem("c", barC);
  }
}