/*
 * Copyright 2009 Google Inc.
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

package com.google.gwt.museum.client.common;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Simple logging class.
 */
@SuppressWarnings("deprecation")
public class SimpleLogger extends Composite {

  private VerticalPanel panel = new VerticalPanel();

  public SimpleLogger() {
    initWidget(panel);
  }

  public String getInfo(Object sender) {
    if (sender instanceof HasText) {
      return ((HasText) sender).getText();
    } else if (sender instanceof UIObject
        && ((UIObject) sender).getTitle() != null) {
      return ((UIObject) sender).getTitle();
    } else if (sender instanceof HasHTML) {
      return ((HasHTML) sender).getHTML();
    } else {
      return sender.toString();
    }
  }

  public void report(String s) {
    panel.insert(new Label(s), 0);
    if (panel.getWidgetCount() == 10) {
      panel.remove(9);
    }
  }

  public void report(GwtEvent<?> event) {
    report(getInfo(event.getSource()) + " fired " + event.toDebugString());
  }

}
