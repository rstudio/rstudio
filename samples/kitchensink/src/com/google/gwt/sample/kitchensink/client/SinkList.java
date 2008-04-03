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

import com.google.gwt.sample.kitchensink.client.Sink.SinkInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.i18n.client.LocaleInfo;

import java.util.ArrayList;

/**
 * The left panel that contains all of the sinks, along with a short description
 * of each.
 */
public class SinkList extends Composite {

  private class MouseLink extends Hyperlink {

    private int index;

    public MouseLink(String name, int index) {
      super(name, name);
      this.index = index;
      sinkEvents(Event.MOUSEEVENTS);
    }

    @Override
    public void onBrowserEvent(Event event) {
      switch (DOM.eventGetType(event)) {
        case Event.ONMOUSEOVER:
          mouseOver(index);
          break;

        case Event.ONMOUSEOUT:
          mouseOut(index);
          break;
      }

      super.onBrowserEvent(event);
    }
  }

  private HorizontalPanel list = new HorizontalPanel();
  private ArrayList<SinkInfo> sinks = new ArrayList<SinkInfo>();

  private int selectedSink = -1;

  public SinkList(Sink.Images images) {
    initWidget(list);
    list.add(images.gwtLogo().createImage());
    setStyleName("ks-List");
  }

  public void addSink(final SinkInfo info) {
    String name = info.getName();
    int index = list.getWidgetCount() - 1;

    MouseLink link = new MouseLink(name, index);
    list.add(link);
    sinks.add(info);

    list.setCellVerticalAlignment(link, HorizontalPanel.ALIGN_BOTTOM);
    styleSink(index, false);
  }

  public SinkInfo find(String sinkName) {
    for (int i = 0; i < sinks.size(); ++i) {
      SinkInfo info = sinks.get(i);
      if (info.getName().equals(sinkName)) {
        return info;
      }
    }

    return null;
  }

  public void setSinkSelection(String name) {
    if (selectedSink != -1) {
      styleSink(selectedSink, false);
    }

    for (int i = 0; i < sinks.size(); ++i) {
      SinkInfo info = sinks.get(i);
      if (info.getName().equals(name)) {
        selectedSink = i;
        styleSink(selectedSink, true);
        return;
      }
    }
  }

  private void colorSink(int index, boolean on) {
    String color = "";
    if (on) {
      color = sinks.get(index).getColor();
    }

    Widget w = list.getWidget(index + 1);
    DOM.setStyleAttribute(w.getElement(), "backgroundColor", color);
  }

  private void mouseOut(int index) {
    if (index != selectedSink) {
      colorSink(index, false);
    }
  }

  private void mouseOver(int index) {
    if (index != selectedSink) {
      colorSink(index, true);
    }
  }

  private void styleSink(int index, boolean selected) {
    Widget w = list.getWidget(index + 1);

    String primaryStyleName = "ks-SinkItem";

    if (LocaleInfo.getCurrentLocale().isRTL()) {
      primaryStyleName += "-rtl";
    }

    w.setStylePrimaryName(primaryStyleName);

    if (index == 0) {
      w.addStyleDependentName("first");
    } else {
      w.removeStyleDependentName("first");
    }

    if (selected) {
      w.addStyleDependentName("selected");
    } else {
      w.removeStyleDependentName("selected");
    }

    colorSink(index, selected);
  }
}
