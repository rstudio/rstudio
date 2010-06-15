/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.logging.client;

import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

/**
 * A simple popup to show log messages
 */
public class BasicLoggingPopup extends DialogBox {
  private VerticalPanel v;

  // TODO(unnurg): Make this into a popup that is less intrusive to the 
  // running of the application.
  public BasicLoggingPopup() {
    super(false, false);
    ScrollPanel s = new ScrollPanel();
    v = new VerticalPanel();
    s.setWidget(v);
    s.setAlwaysShowScrollBars(true);
    s.setHeight("100px");
    super.setWidget(s);
    setText("Logging");
    Button ok = new Button("OK");
    ok.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
         hide();
      }
    });
    v.add(ok);
    show();
  }
  
  @Override
  public void add(Widget w) {
    v.add(w);
  }
  
  @Override
  public void setWidget(Widget w) {
    v.clear();
    v.add(w);
  }
  

}
