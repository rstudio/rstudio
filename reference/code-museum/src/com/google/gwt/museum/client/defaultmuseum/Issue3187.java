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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.Widget;

/**
 * {@link FileUpload} onChange event fires correctly in all browsers.
 */
public class Issue3187 extends AbstractIssue {
  @Override
  public Widget createIssue() {
    FileUpload fileUpload = new FileUpload();
    fileUpload.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        Window.alert("Value Changed");
      }
    });
    return fileUpload;
  }

  @Override
  public String getInstructions() {
    return "Change the file path and verify an alert dialog appears.  In "
        + "Opera, the onChange event should only be fired when the box is "
        + "blurred.";
  }

  @Override
  public String getSummary() {
    return "FileUpload supports change events";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
