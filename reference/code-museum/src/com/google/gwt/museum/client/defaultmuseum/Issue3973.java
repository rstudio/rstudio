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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;

/**
 * When a {@link FormPanel} does not use a synthesized hidden iframe and is
 * being submit by means other than {@link FormPanel#submit()}, it doesn't fire
 * submit events.
 */
public class Issue3973 extends AbstractIssue {
  private FormPanel form;

  @Override
  public Widget createIssue() {
    form = new FormPanel("_blank");
    form.setAction("http://www.google.com/search");
    form.addSubmitHandler(new SubmitHandler() {
      public void onSubmit(SubmitEvent event) {
        Window.alert("Did you see me?");
        event.cancel();
      }
    });
    TextBox box = new TextBox();
    box.setName("q");
    form.setWidget(box);
    return form;
  }

  @Override
  public String getInstructions() {
    return "Enter some text and press the ENTER key, it should show an alert. It shouldn't open Google within a new window/tab!";
  }

  @Override
  public String getSummary() {
    return "FormPanel doesn't hook events when not using a synthesized hidden iframe.";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
