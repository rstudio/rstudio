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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogBoxExample implements EntryPoint, ClickListener {

  private static class MyDialog extends DialogBox {

    public MyDialog() {
      // Set the dialog box's caption.
      setText("My First Dialog");

      // DialogBox is a SimplePanel, so you have to set it's widget property to
      // whatever you want its contents to be.
      Button ok = new Button("OK");
      ok.addClickListener(new ClickListener() {
        public void onClick(Widget sender) {
          MyDialog.this.hide();
        }
      });
      setWidget(ok);
    }
  }

  public void onModuleLoad() {
    Button b = new Button("Click me");
    b.addClickListener(this);

    RootPanel.get().add(b);
  }

  public void onClick(Widget sender) {
    // Instantiate the dialog box and show it.
    new MyDialog().show();
  }
}
