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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;

/**
 * Buttons default to type=submit in WebKit (Safari and Chrome) and IE8 (in IE8
 * mode).
 */
public class Issue3962 extends AbstractIssue {
  private FormPanel form;

  @Override
  public Widget createIssue() {
    form = new FormPanel();
    form.addSubmitHandler(new SubmitHandler() {
      public void onSubmit(SubmitEvent event) {
        Window.alert("Form is being submitted.");
        event.cancel();
      }
    });
    form.setWidget(new Button("Submit"));
    return form;
  }

  @Override
  public String getInstructions() {
    return "Click the button, it should have no effect.";
  }

  @Override
  public String getSummary() {
    return "In IE8 (in IE8 mode) and WebKit (Safari and Chrome), buttons default"
        + " to type submit.";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
