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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.sample.kitchensink.client.Sink.SinkInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Application that demonstrates all of the built-in widgets.
 */
public class KitchenSink implements EntryPoint, HistoryListener {

  protected SinkList list = new SinkList();
  private SinkInfo curInfo;
  private Sink curSink;
  private HTML description = new HTML();
  private VerticalPanel panel = new VerticalPanel();

  public void onHistoryChanged(String token) {
    // Find the SinkInfo associated with the history context. If one is
    // found, show it (It may not be found, for example, when the user mis-
    // types a URL, or on startup, when the first context will be "").
    SinkInfo info = list.find(token);
    if (info == null) {
      showInfo();
      return;
    }
    show(info, false);
  }

  public void onModuleLoad() {
    // Load all the sinks.
    loadSinks();

    panel.add(list);
    panel.add(description);
    panel.setWidth("100%");

    description.setStyleName("ks-Info");

    History.addHistoryListener(this);
    RootPanel.get().add(panel);

    // Show the initial screen.
    String initToken = History.getToken();
    if (initToken.length() > 0) {
      onHistoryChanged(initToken);
    } else {
      showInfo();
    }
  }

  public void show(SinkInfo info, boolean affectHistory) {
    // Don't bother re-displaying the existing sink. This can be an issue
    // in practice, because when the history context is set, our
    // onHistoryChanged() handler will attempt to show the currently-visible
    // sink.
    if (info == curInfo) {
      return;
    }
    curInfo = info;

    // Remove the old sink from the display area.
    if (curSink != null) {
      curSink.onHide();
      panel.remove(curSink);
    }

    // Get the new sink instance, and display its description in the
    // sink list.
    curSink = info.getInstance();
    list.setSinkSelection(info.getName());
    description.setHTML(info.getDescription());

    // If affectHistory is set, create a new item on the history stack. This
    // will ultimately result in onHistoryChanged() being called. It will call
    // show() again, but nothing will happen because it will request the exact
    // same sink we're already showing.
    if (affectHistory) {
      History.newItem(info.getName());
    }

    // Change the description background color.
    DOM.setStyleAttribute(description.getElement(), "backgroundColor",
        info.getColor());

    // Display the new sink.
    panel.add(curSink);
    panel.setCellHorizontalAlignment(curSink, VerticalPanel.ALIGN_CENTER);
    curSink.onShow();
  }

  /**
   * Adds all sinks to the list. Note that this does not create actual instances
   * of all sinks yet (they are created on-demand). This can make a significant
   * difference in startup time.
   */
  protected void loadSinks() {
    list.addSink(Info.init());
    list.addSink(Widgets.init());
    list.addSink(Panels.init());
    list.addSink(Lists.init());
    list.addSink(Text.init());
    list.addSink(Popups.init());
  }

  private void showInfo() {
    show(list.find("Intro"), false);
  }
}
