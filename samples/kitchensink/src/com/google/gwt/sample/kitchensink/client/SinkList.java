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

import com.google.gwt.sample.kitchensink.client.Sink.SinkInfo;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.ArrayList;

/**
 * The left panel that contains all of the sinks, along with a short description
 * of each.
 */
public class SinkList extends Composite {

  private VerticalPanel list = new VerticalPanel();
  private ArrayList sinks = new ArrayList();
  private int selectedSink = -1;

  public SinkList() {
    initWidget(list);
    setStyleName("ks-List");
  }

  public void addSink(final SinkInfo info) {
    String name = info.getName();
    Hyperlink link = new Hyperlink(name, name);
    link.setStyleName("ks-SinkItem");

    list.add(link);
    sinks.add(info);
  }

  public SinkInfo find(String sinkName) {
    for (int i = 0; i < sinks.size(); ++i) {
      SinkInfo info = (SinkInfo) sinks.get(i);
      if (info.getName().equals(sinkName)) {
        return info;
      }
    }

    return null;
  }

  public void setSinkSelection(String name) {
    if (selectedSink != -1) {
      list.getWidget(selectedSink).removeStyleName("ks-SinkItem-selected");
    }
    
    for (int i = 0; i < sinks.size(); ++i) {
      SinkInfo info = (SinkInfo) sinks.get(i);
      if (info.getName().equals(name)) {
        selectedSink = i;
        list.getWidget(selectedSink).addStyleName("ks-SinkItem-selected");
        return;
      }
    }
  }
}
