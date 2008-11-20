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
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DisclosureEvent;
import com.google.gwt.user.client.ui.DisclosureHandler;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Testing disclosure events.
 */
@SuppressWarnings("deprecation")
public class VisualsForDisclosurePanelEvents extends AbstractIssue {

  VerticalPanel report = new VerticalPanel();

  @Override
  public Widget createIssue() {
    VerticalPanel p = new VerticalPanel();

    p.add(createDisclosurePanel("disclose 1"));
    p.add(createDisclosurePanel("disclose 2"));
    p.add(report);
    report("reporting");
    return p;
  }

  @Override
  public String getInstructions() {
    return "Click on disclosure panel, see the expected Open and Close events firing";
  }

  @Override
  public String getSummary() {
    return "DisclosurePanel event tests";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  DisclosurePanel createDisclosurePanel(final String name) {
    DisclosurePanel widget = new DisclosurePanel();
    widget.setContent(new HTML("content for " + name));
    widget.setTitle(name);
    widget.setHeader(new CheckBox(name));
    class MyHandler implements DisclosureHandler, OpenHandler<DisclosurePanel>,
        CloseHandler<DisclosurePanel> {

      public void onClose(CloseEvent<DisclosurePanel> event) {
        report(event);
      }

      public void onClose(DisclosureEvent event) {
        report(name + "close");
      }

      public void onOpen(DisclosureEvent event) {
        report(name + "open");
      }

      public void onOpen(OpenEvent<DisclosurePanel> event) {
        report(event);
      }
    }
    MyHandler handler = new MyHandler();
    widget.addCloseHandler(handler);
    widget.addOpenHandler(handler);
    widget.addEventHandler(handler);
    return widget;
  }

  private void report(GwtEvent<?> event) {
    String title = ((UIObject) event.getSource()).getTitle();
    report(title + " fired " + event.toDebugString());
  }

  // will be replaced by logging
  private void report(String s) {
    report.insert(new Label(s), 0);
    if (report.getWidgetCount() == 10) {
      report.remove(9);
    }
  }
}
