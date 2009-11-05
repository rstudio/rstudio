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
package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootPanel;

public class DialogBoxExample implements EntryPoint, ClickHandler {

  private static class MyDialog extends DialogBox {

    public MyDialog() {
      // Set the dialog box's caption.
      setText("My First Dialog");

      // Enable animation.
      setAnimationEnabled(true);

      // Enable glass background.
      setGlassEnabled(true);

      // DialogBox is a SimplePanel, so you have to set its widget property to
      // whatever you want its contents to be.
      Button ok = new Button("OK");
      ok.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          MyDialog.this.hide();
        }
      });
      setWidget(ok);
    }
  }

  public void onModuleLoad() {
    Button b = new Button("Click me");
    b.addClickHandler(this);

    RootPanel.get().add(b);
  }

  public void onClick(ClickEvent event) {
    // Instantiate the dialog box and show it.
    new MyDialog().show();
  }
}
